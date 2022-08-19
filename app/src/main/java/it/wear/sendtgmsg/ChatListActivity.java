package it.wear.sendtgmsg;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.KeyEvent;
import android.widget.Toast;

import it.wear.libsgram.TelegramConfiguration;
import android.app.Activity;

import android.app.RemoteInput;
import androidx.wear.widget.WearableLinearLayoutManager;
import androidx.wear.widget.WearableRecyclerView;

import org.drinkless.td.libcore.telegram.TdApi;

import java.util.ArrayList;
import java.util.NavigableSet;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import it.tdlight.client.GenericResultHandler;
import it.tdlight.client.GenericUpdateHandler;
import it.tdlight.client.Result;
import it.tdlight.client.SimpleTelegramClient;

public class ChatListActivity extends Activity {

    private static final ConcurrentMap<Long, TdApi.Chat> chats = new ConcurrentHashMap<>();
    private static final NavigableSet<OrderedChat> mainChatList = new TreeSet<>();
    private static final int MESSAGE_INPUT_RESULT = 1000;
    private static final String BUNDLE_INPUT = "message_text";
    private int messageIndex = 0;
    private long messageChatId = 0;
    private final ArrayList<ChatItem> chatItems = new ArrayList<>();
    private ChatListAdapter chatAdapter = null;

    static class OrderedChat implements Comparable<OrderedChat> {
        final long chatId;
        final TdApi.ChatPosition position;

        OrderedChat(long chatId, TdApi.ChatPosition position) {
            this.chatId = chatId;
            this.position = position;
        }

        @Override
        public int compareTo(OrderedChat o) {
            if (this.position.order != o.position.order) {
                return o.position.order < this.position.order ? -1 : 1;
            }
            if (this.chatId != o.chatId) {
                return o.chatId < this.chatId ? -1 : 1;
            }
            return 0;
        }

        @Override
        public boolean equals(Object obj) {
            OrderedChat o = (OrderedChat) obj;
            return this.chatId == o.chatId && this.position.order == o.position.order;
        }
    }

    protected void updateDataModel() {
        if (mainChatList.size() < 1) return;

        chatItems.clear();
        int limit = 10;
        synchronized (mainChatList) {
            java.util.Iterator<OrderedChat> iter = mainChatList.iterator();
            for (int i = 0; i < limit && i < mainChatList.size(); i++) {
                long chatId = iter.next().chatId;
                TdApi.Chat chat = chats.get(chatId);
                if (chat != null) {
                    chatItems.add(new ChatItem(chat.title, chatId));
                }
            }
        }
        if (chatAdapter != null) {
            new Handler(Looper.getMainLooper()).post(new Runnable() {
                @Override
                public void run() {
                    chatAdapter.notifyDataSetChanged();
                }
            });
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_chat_list);
        //TextView testo = findViewById(R.id.textView2);
        //Button btn = findViewById(R.id.button2);

        SimpleTelegramClient cli = TelegramConfiguration.getInstance().getClient();

        WearableRecyclerView recyclerView = findViewById(R.id.chats_view);
        recyclerView.setHasFixedSize(true);
        recyclerView.setEdgeItemsCenteringEnabled(true);
        recyclerView.setLayoutManager(new WearableLinearLayoutManager(this));
        chatAdapter = new ChatListAdapter(this, chatItems, new ChatListAdapter.AdapterCallback() {
            @Override
            public void onItemClicked(String title, Long chatId) {
                //Toast.makeText(ChatListActivity.this, String.format("Click su %d", chatId), Toast.LENGTH_SHORT).show();

                Intent remoteInputIntent = new Intent("android.support.wearable.input.action.REMOTE_INPUT");
                RemoteInput[] remoteInputArray = new RemoteInput[1];
                remoteInputArray[0] = new RemoteInput.Builder(BUNDLE_INPUT)
                        .setAllowFreeFormInput(true)
                        .setLabel("Il tuo messaggio per " + title)
                        .setChoices(new CharSequence[] {"OK", "Esco", "Sto arrivando", "Parto adesso", "Sì", "No", "Perfetto"})
                        .build();
                remoteInputIntent.putExtra("android.support.wearable.input.extra.REMOTE_INPUTS", remoteInputArray);
                messageIndex++;
                if (messageIndex > 99) messageIndex = 0;
                messageChatId = chatId;
                startActivityForResult(remoteInputIntent, MESSAGE_INPUT_RESULT + messageIndex);
            }
        });
        recyclerView.setAdapter(chatAdapter);

        //testo.setText("Elenco utenti:");
        /*
        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

            }
        });
        */

        cli.addUpdatesHandler(new GenericUpdateHandler<TdApi.Update>() {
            @Override
            public void onUpdate(TdApi.Update update) {
                switch (update.getConstructor()) {
                    case TdApi.UpdateUser.CONSTRUCTOR: {
                        // New user
                        //TdApi.UpdateUser updateUser = (TdApi.UpdateUser) update;
                        //users.put(updateUser.user.id, updateUser.user);
                        break;
                    }
                    case TdApi.UpdateNewChat.CONSTRUCTOR: {
                        // New chat
                        TdApi.UpdateNewChat updateNewChat = (TdApi.UpdateNewChat) update;
                        TdApi.Chat chat = updateNewChat.chat;
                        synchronized (chat) {
                            int type = chat.type.getConstructor();
                            if (type == TdApi.ChatTypePrivate.CONSTRUCTOR || type == TdApi.ChatTypeBasicGroup.CONSTRUCTOR) {
                                if (chat.title.length() > 0) {
                                    chats.put(chat.id, chat);

                                    TdApi.ChatPosition[] positions = chat.positions;
                                    chat.positions = new TdApi.ChatPosition[0];
                                    setChatPositions(chat, positions);
                                }
                            }
                        }
                        break;
                    }
                    case TdApi.UpdateChatTitle.CONSTRUCTOR: {
                        // New chat title
                        TdApi.UpdateChatTitle updateChat = (TdApi.UpdateChatTitle) update;
                        TdApi.Chat chat = chats.get(updateChat.chatId);
                        synchronized (chat) {
                            chat.title = updateChat.title;
                        }
                        break;
                    }
                    case TdApi.UpdateChatPosition.CONSTRUCTOR: {
                        // Update in the chat position
                        TdApi.UpdateChatPosition updateChat = (TdApi.UpdateChatPosition) update;
                        if (updateChat.position.list.getConstructor() != TdApi.ChatListMain.CONSTRUCTOR) {
                            break;
                        }

                        TdApi.Chat chat = chats.get(updateChat.chatId);
                        if (chat == null) break;
                        synchronized (chat) {
                            int i;
                            for (i = 0; i < chat.positions.length; i++) {
                                if (chat.positions[i].list.getConstructor() == TdApi.ChatListMain.CONSTRUCTOR) {
                                    break;
                                }
                            }
                            TdApi.ChatPosition[] new_positions = new TdApi.ChatPosition[chat.positions.length + (updateChat.position.order == 0 ? 0 : 1) - (i < chat.positions.length ? 1 : 0)];
                            int pos = 0;
                            if (updateChat.position.order != 0) {
                                new_positions[pos++] = updateChat.position;
                            }
                            for (int j = 0; j < chat.positions.length; j++) {
                                if (j != i) {
                                    new_positions[pos++] = chat.positions[j];
                                }
                            }
                            assert pos == new_positions.length;

                            setChatPositions(chat, new_positions);
                            updateDataModel();
                        }
                        break;
                    }
                    case TdApi.UpdateChatLastMessage.CONSTRUCTOR: {
                        // Update in the chat position (due to last message)
                        TdApi.UpdateChatLastMessage updateChat = (TdApi.UpdateChatLastMessage) update;
                        TdApi.Chat chat = chats.get(updateChat.chatId);
                        if (chat == null) break;
                        synchronized (chat) {
                            setChatPositions(chat, updateChat.positions);
                            updateDataModel();
                        }
                        break;
                    }
                    case TdApi.UpdateMessageSendSucceeded.CONSTRUCTOR: {
                        // Message has been sent
                        //TdApi.UpdateMessageSendSucceeded msg = (TdApi.UpdateMessageSendSucceeded) update;
                        runOnUiThread(() -> Toast.makeText(ChatListActivity.this, "Messaggio inviato", Toast.LENGTH_SHORT).show());
                        break;
                    }
                    default:
                }
            }
        });

        cli.send(new TdApi.GetChats(new TdApi.ChatListMain(), 9223372036854775807L, 0, 20), new GenericResultHandler<TdApi.Object>() {
            @Override
            public void onResult(Result<TdApi.Object> result) {

            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == (MESSAGE_INPUT_RESULT + messageIndex) && resultCode == RESULT_OK) {
            Bundle bundle = RemoteInput.getResultsFromIntent(data);
            if (bundle != null) {
                CharSequence charSequence = bundle.getCharSequence(BUNDLE_INPUT);
                if (charSequence != null && charSequence.length() > 0 && messageChatId != 0) {
                    //Toast.makeText(this, charSequence + String.valueOf(messageChatId), Toast.LENGTH_SHORT).show();

                    SimpleTelegramClient cli = TelegramConfiguration.getInstance().getClient();
                    TdApi.InputMessageContent content = new TdApi.InputMessageText(new TdApi.FormattedText(charSequence.toString(), null), false, true);
                    cli.send(new TdApi.SendMessage(messageChatId, 0, 0, null, null, content), new GenericResultHandler<TdApi.Object>() {
                        @Override
                        public void onResult(Result<TdApi.Object> result) {
                            return;
                        }
                    });
                }
            }
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (event.getRepeatCount() == 0) {
            if (keyCode == KeyEvent.KEYCODE_STEM_1 || keyCode == 4) {
                // Do stuff
                Toast.makeText(this, String.format("Tentativo di ricaricamento (%d)", mainChatList.size()), Toast.LENGTH_SHORT).show();
                if (mainChatList.size() > 0) {
                    updateDataModel();
                }
                return true;
            }
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (mainChatList.size() > 0) {
            updateDataModel();
        }
    }

    private static void setChatPositions(TdApi.Chat chat, TdApi.ChatPosition[] positions) {
        synchronized (mainChatList) {
            synchronized (chat) {
                for (TdApi.ChatPosition position : chat.positions) {
                    if (position.list.getConstructor() == TdApi.ChatListMain.CONSTRUCTOR) {
                        boolean isRemoved = mainChatList.remove(new OrderedChat(chat.id, position));
                        assert isRemoved;
                    }
                }

                chat.positions = positions;

                for (TdApi.ChatPosition position : chat.positions) {
                    if (position.list.getConstructor() == TdApi.ChatListMain.CONSTRUCTOR) {
                        boolean isAdded = mainChatList.add(new OrderedChat(chat.id, position));
                        assert isAdded;
                    }
                }
            }
        }
    }

}