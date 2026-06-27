package tw.nekomimi.nekogram.ayu;

import org.telegram.messenger.FileLog;
import org.telegram.messenger.Utilities;
import org.telegram.tgnet.TLRPC;

import java.util.ArrayList;
import java.util.List;

public class AyuMessagesController {

    public static void onMessagesSeen(final int accountId, final List<TLRPC.Message> messages) {
        if (messages == null || messages.isEmpty()) {
            return;
        }
        if (!AyuConfig.saveDeletedMessages && !AyuConfig.saveEditedMessages) {
            return;
        }
        try {
            AyuMessageStore.getInstance().onSeenMessages(accountId, messages);
        } catch (Exception e) {
            FileLog.e(e);
        }
    }

    public static void onMessagesDeleted(final int accountId, final long channelId, final ArrayList<Integer> ids) {
        if (!AyuConfig.saveDeletedMessages) {
            return;
        }
        if (ids == null || ids.isEmpty()) {
            return;
        }
        final ArrayList<Integer> copy = new ArrayList<>(ids);
        Utilities.globalQueue.postRunnable(() -> {
            try {
                AyuMessageStore.getInstance().markDeleted(accountId, channelId, copy);
            } catch (Exception e) {
                FileLog.e(e);
            }
        });
    }
}
