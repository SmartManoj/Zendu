package it.wear.sendtgmsg;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

public class ChatListAdapter extends RecyclerView.Adapter<ChatListAdapter.RecyclerViewHolder> {

    private ArrayList<ChatItem> dataSource = new ArrayList<ChatItem>();

    public interface AdapterCallback {
        void onItemClicked(String title, Long chatId);
    }

    private AdapterCallback callback;
    private String drawableIcon;
    private Context context;

    public ChatListAdapter(Context context, ArrayList<ChatItem> dataArgs, AdapterCallback callback) {
        this.context = context;
        this.dataSource = dataArgs;
        this.callback = callback;
    }

    @NonNull
    @Override
    public RecyclerViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.chat_item, parent, false);

        return new RecyclerViewHolder(view);
    }

    public static class RecyclerViewHolder extends RecyclerView.ViewHolder {
        RelativeLayout menuContainer;
        TextView menuItem;
        ImageView menuIcon;

        public RecyclerViewHolder(View view) {
            super(view);
            menuContainer = view.findViewById(R.id.menu_container);
            menuItem = view.findViewById(R.id.menu_item);
            menuIcon = view.findViewById(R.id.menu_icon);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerViewHolder holder, int position) {
        try {
            ChatItem data_provider = dataSource.get(position);
            holder.menuItem.setText(data_provider.getText());
        }catch (Exception e) {
            return;
        }

        holder.menuIcon.setImageResource(R.drawable.ic_baseline_person_24);
        holder.menuContainer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View v) {
                if (callback != null) {
                    ChatItem item = dataSource.get(holder.getAbsoluteAdapterPosition());
                    callback.onItemClicked(item.getText(), item.getId());
                }
            }
        });
    }

    @Override
    public int getItemCount() {
        return dataSource.size();
    }
}

class ChatItem {
    private String text;
    private long id;

    public ChatItem(String text, long id) {
        this.text = text;
        this.id = id;
    }

    public String getText() {
        return text;
    }

    public long getId() {
        return id;
    }
}