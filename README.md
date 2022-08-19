### This project is a (working) stub for personal use that I've decided to share

# Send Telegram Message for Wear OS 3
A simple message initiator that allows you to send a text message
to any of your Telegram recent chats on Wear OS 3 (Galaxy Watch 4 ecc...).

It's useful especially now that you can't send a new Telegram
message from scratch ([Google Assistant doesn't work](https://support.google.com/assistant/thread/142052062?hl=en) anymore with Telegram
and the [Telegram Wear OS app](https://telegram.org/blog/android-wear-2-0) has been retired).

![Screenshot of the app](screenshot.png)

This client uses a combo of TdLib and TdLights and it's based on
the work of the [DainoGram](https://github.com/daino-selvatico/DainoGram) project.

What's working:
- Authentication
- Reloading the app without the needs of retrieving another auth. code
- Sending a message to a contact on recent chats
- Pressing the lower Galaxy Watch 4 button manually refreshes the chats

Issues/limitations:
- Sometimes it crashes during the load of the chats, but when they are loaded
successfully, they work
- Errors are not handled (such as network down, authorization removed from
Telegram session, message not sent correctly, etc.)
- It may leak memory, but who cares (as it works!)?
- If it hangs, clear data and login again from scratch


