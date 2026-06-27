package tw.nekomimi.nekogram.ayu;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;

import org.telegram.messenger.MessagesController;
import org.telegram.messenger.UserObject;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.Components.UItem;
import org.telegram.ui.Components.UniversalAdapter;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import tw.nekomimi.nekogram.settings.BaseNekoSettingsActivity;

public class AyuDeletedHistoryActivity extends BaseNekoSettingsActivity {

    private static final int DIALOG_BASE_ID = 1000;
    private static final int MESSAGE_BASE_ID = 100000;

    private final long dialogId;
    private final ArrayList<Long> dialogList = new ArrayList<>();
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());

    public AyuDeletedHistoryActivity() {
        this.dialogId = 0;
    }

    public AyuDeletedHistoryActivity(Bundle args) {
        super(args);
        this.dialogId = args != null ? args.getLong("dialog_id", 0) : 0;
    }

    private String resolveDialogName(long id) {
        MessagesController controller = MessagesController.getInstance(currentAccount);
        if (id > 0) {
            TLRPC.User user = controller.getUser(id);
            if (user != null) {
                return UserObject.getUserName(user);
            }
        } else {
            TLRPC.Chat chat = controller.getChat(-id);
            if (chat != null && !TextUtils.isEmpty(chat.title)) {
                return chat.title;
            }
        }
        return "Dialog " + id;
    }

    private String formatDate(long seconds) {
        if (seconds <= 0) {
            return "";
        }
        return dateFormat.format(new Date(seconds * 1000L));
    }

    @Override
    protected void fillItems(ArrayList<UItem> items, UniversalAdapter adapter) {
        if (dialogId == 0) {
            fillDialogList(items);
        } else {
            fillMessages(items);
        }
    }

    private void fillDialogList(ArrayList<UItem> items) {
        dialogList.clear();
        List<Long> dialogs = AyuMessageStore.getInstance().getDialogsWithSaved(currentAccount);
        if (dialogs.isEmpty()) {
            items.add(UItem.asShadow("No saved deleted or edited messages yet."));
            return;
        }
        items.add(UItem.asHeader("Chats with saved history"));
        int index = 0;
        for (Long id : dialogs) {
            dialogList.add(id);
            items.add(UItem.asButton(DIALOG_BASE_ID + index, resolveDialogName(id)));
            index++;
        }
        items.add(UItem.asShadow(null));
    }

    private void fillMessages(ArrayList<UItem> items) {
        List<AyuMessageStore.AyuSavedRecord> deleted = AyuMessageStore.getInstance().getDeletedRecords(currentAccount, dialogId);
        List<AyuMessageStore.AyuSavedRecord> edited = AyuMessageStore.getInstance().getEditedRecords(currentAccount, dialogId);

        int id = MESSAGE_BASE_ID;

        if (!deleted.isEmpty()) {
            items.add(UItem.asHeader("Deleted messages"));
            for (AyuMessageStore.AyuSavedRecord record : deleted) {
                String title = TextUtils.isEmpty(record.text) ? "(no text)" : record.text;
                String subtitle = "Deleted " + formatDate(record.eventDate);
                items.add(TextDetailSettingsCellFactory.of(id++, title, subtitle));
            }
            items.add(UItem.asShadow(null));
        }

        if (!edited.isEmpty()) {
            items.add(UItem.asHeader("Edited messages"));
            for (AyuMessageStore.AyuSavedRecord record : edited) {
                String title = TextUtils.isEmpty(record.text) ? "(no text)" : record.text;
                String subtitle = "Previous version, edited " + formatDate(record.eventDate);
                items.add(TextDetailSettingsCellFactory.of(id++, title, subtitle));
            }
            items.add(UItem.asShadow(null));
        }

        if (deleted.isEmpty() && edited.isEmpty()) {
            items.add(UItem.asShadow("No saved history for this chat."));
        }
    }

    @Override
    protected void onItemClick(UItem item, View view, int position, float x, float y) {
        int id = item.id;
        if (dialogId == 0 && id >= DIALOG_BASE_ID && id < DIALOG_BASE_ID + dialogList.size()) {
            Bundle args = new Bundle();
            args.putLong("dialog_id", dialogList.get(id - DIALOG_BASE_ID));
            presentFragment(new AyuDeletedHistoryActivity(args));
        }
    }

    @Override
    protected String getActionBarTitle() {
        if (dialogId == 0) {
            return "Deleted and edited messages";
        }
        return resolveDialogName(dialogId);
    }
}
