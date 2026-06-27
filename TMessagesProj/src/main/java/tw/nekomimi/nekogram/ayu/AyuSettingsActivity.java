package tw.nekomimi.nekogram.ayu;

import android.view.View;

import org.telegram.ui.Cells.TextCheckCell;
import org.telegram.ui.Components.UItem;
import org.telegram.ui.Components.UniversalAdapter;

import java.util.ArrayList;

import tw.nekomimi.nekogram.settings.BaseNekoSettingsActivity;

public class AyuSettingsActivity extends BaseNekoSettingsActivity {

    private int ghostModeRow;
    private int sendReadPacketsRow;
    private int sendOnlinePacketsRow;
    private int sendUploadProgressRow;
    private int sendOfflineRow;

    private int saveDeletedRow;
    private int saveEditedRow;
    private int saveForBotsRow;
    private int viewHistoryRow;

    @Override
    protected void fillItems(ArrayList<UItem> items, UniversalAdapter adapter) {
        rowId = 1;

        items.add(UItem.asHeader("Ghost mode"));
        ghostModeRow = rowId++;
        items.add(UItem.asCheck(ghostModeRow, "Ghost mode", "Stop sending read receipts, online status, typing and upload status").setChecked(AyuConfig.isGhostModeActive()));
        sendReadPacketsRow = rowId++;
        items.add(UItem.asCheck(sendReadPacketsRow, "Send read receipts").setChecked(AyuConfig.sendReadPackets));
        sendOnlinePacketsRow = rowId++;
        items.add(UItem.asCheck(sendOnlinePacketsRow, "Send online status").setChecked(AyuConfig.sendOnlinePackets));
        sendUploadProgressRow = rowId++;
        items.add(UItem.asCheck(sendUploadProgressRow, "Send typing and upload status").setChecked(AyuConfig.sendUploadProgress));
        sendOfflineRow = rowId++;
        items.add(UItem.asCheck(sendOfflineRow, "Send offline after going online").setChecked(AyuConfig.sendOfflinePacketAfterOnline));
        items.add(UItem.asShadow(null));

        items.add(UItem.asHeader("Message history"));
        saveDeletedRow = rowId++;
        items.add(UItem.asCheck(saveDeletedRow, "Save deleted messages").setChecked(AyuConfig.saveDeletedMessages));
        saveEditedRow = rowId++;
        items.add(UItem.asCheck(saveEditedRow, "Save edited messages").setChecked(AyuConfig.saveEditedMessages));
        saveForBotsRow = rowId++;
        items.add(UItem.asCheck(saveForBotsRow, "Save messages from bots").setChecked(AyuConfig.saveForBots));
        items.add(UItem.asShadow(null));

        viewHistoryRow = rowId++;
        items.add(UItem.asButton(viewHistoryRow, "Deleted and edited messages"));
        items.add(UItem.asShadow(null));
    }

    @Override
    protected void onItemClick(UItem item, View view, int position, float x, float y) {
        int id = item.id;
        if (id == ghostModeRow) {
            AyuConfig.toggleGhostMode();
            if (view instanceof TextCheckCell) {
                ((TextCheckCell) view).setChecked(AyuConfig.isGhostModeActive());
            }
            listView.adapter.update(true);
        } else if (id == sendReadPacketsRow) {
            AyuConfig.toggleSendReadPackets();
            if (view instanceof TextCheckCell) {
                ((TextCheckCell) view).setChecked(AyuConfig.sendReadPackets);
            }
        } else if (id == sendOnlinePacketsRow) {
            AyuConfig.toggleSendOnlinePackets();
            if (view instanceof TextCheckCell) {
                ((TextCheckCell) view).setChecked(AyuConfig.sendOnlinePackets);
            }
        } else if (id == sendUploadProgressRow) {
            AyuConfig.toggleSendUploadProgress();
            if (view instanceof TextCheckCell) {
                ((TextCheckCell) view).setChecked(AyuConfig.sendUploadProgress);
            }
        } else if (id == sendOfflineRow) {
            AyuConfig.toggleSendOfflinePacketAfterOnline();
            if (view instanceof TextCheckCell) {
                ((TextCheckCell) view).setChecked(AyuConfig.sendOfflinePacketAfterOnline);
            }
        } else if (id == saveDeletedRow) {
            AyuConfig.toggleSaveDeletedMessages();
            if (view instanceof TextCheckCell) {
                ((TextCheckCell) view).setChecked(AyuConfig.saveDeletedMessages);
            }
        } else if (id == saveEditedRow) {
            AyuConfig.toggleSaveEditedMessages();
            if (view instanceof TextCheckCell) {
                ((TextCheckCell) view).setChecked(AyuConfig.saveEditedMessages);
            }
        } else if (id == saveForBotsRow) {
            AyuConfig.toggleSaveForBots();
            if (view instanceof TextCheckCell) {
                ((TextCheckCell) view).setChecked(AyuConfig.saveForBots);
            }
        } else if (id == viewHistoryRow) {
            presentFragment(new AyuDeletedHistoryActivity());
        }
    }

    @Override
    protected String getActionBarTitle() {
        return "AyuGram Features";
    }

    @Override
    protected String getKey() {
        return "ayu";
    }
}
