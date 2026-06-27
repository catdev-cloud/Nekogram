package tw.nekomimi.nekogram.ayu;

import android.app.Activity;
import android.content.SharedPreferences;

import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.MessagesController;
import org.telegram.tgnet.TLRPC;

public class AyuConfig {
    private static final Object sync = new Object();

    private static SharedPreferences preferences;
    private static SharedPreferences.Editor editor;

    public static boolean sendReadPackets;
    public static boolean sendOnlinePackets;
    public static boolean sendUploadProgress;
    public static boolean sendOfflinePacketAfterOnline;

    public static boolean saveDeletedMessages;
    public static boolean saveEditedMessages;
    public static boolean saveForBots;

    public static boolean showGhostToggleInDrawer;

    private static boolean configLoaded;

    static {
        loadConfig();
    }

    public static void loadConfig() {
        synchronized (sync) {
            if (configLoaded) {
                return;
            }

            preferences = ApplicationLoader.applicationContext.getSharedPreferences("ayuconfig", Activity.MODE_PRIVATE);
            editor = preferences.edit();

            sendReadPackets = preferences.getBoolean("sendReadPackets", true);
            sendOnlinePackets = preferences.getBoolean("sendOnlinePackets", true);
            sendUploadProgress = preferences.getBoolean("sendUploadProgress", true);
            sendOfflinePacketAfterOnline = preferences.getBoolean("sendOfflinePacketAfterOnline", false);

            saveDeletedMessages = preferences.getBoolean("saveDeletedMessages", true);
            saveEditedMessages = preferences.getBoolean("saveEditedMessages", true);
            saveForBots = preferences.getBoolean("saveForBots", true);

            showGhostToggleInDrawer = preferences.getBoolean("showGhostToggleInDrawer", false);

            configLoaded = true;
        }
    }

    private static void putBoolean(String key, boolean value) {
        if (editor != null) {
            editor.putBoolean(key, value).apply();
        }
    }

    public static boolean isGhostModeActive() {
        return !sendReadPackets && !sendOnlinePackets && !sendUploadProgress && sendOfflinePacketAfterOnline;
    }

    public static void setGhostMode(boolean enabled) {
        sendReadPackets = !enabled;
        sendOnlinePackets = !enabled;
        sendUploadProgress = !enabled;
        sendOfflinePacketAfterOnline = enabled;

        putBoolean("sendReadPackets", sendReadPackets);
        putBoolean("sendOnlinePackets", sendOnlinePackets);
        putBoolean("sendUploadProgress", sendUploadProgress);
        putBoolean("sendOfflinePacketAfterOnline", sendOfflinePacketAfterOnline);
    }

    public static void toggleGhostMode() {
        setGhostMode(!isGhostModeActive());
    }

    public static void toggleSendReadPackets() {
        sendReadPackets = !sendReadPackets;
        putBoolean("sendReadPackets", sendReadPackets);
    }

    public static void toggleSendOnlinePackets() {
        sendOnlinePackets = !sendOnlinePackets;
        putBoolean("sendOnlinePackets", sendOnlinePackets);
    }

    public static void toggleSendUploadProgress() {
        sendUploadProgress = !sendUploadProgress;
        putBoolean("sendUploadProgress", sendUploadProgress);
    }

    public static void toggleSendOfflinePacketAfterOnline() {
        sendOfflinePacketAfterOnline = !sendOfflinePacketAfterOnline;
        putBoolean("sendOfflinePacketAfterOnline", sendOfflinePacketAfterOnline);
    }

    public static void toggleSaveDeletedMessages() {
        saveDeletedMessages = !saveDeletedMessages;
        putBoolean("saveDeletedMessages", saveDeletedMessages);
    }

    public static void toggleSaveEditedMessages() {
        saveEditedMessages = !saveEditedMessages;
        putBoolean("saveEditedMessages", saveEditedMessages);
    }

    public static void toggleSaveForBots() {
        saveForBots = !saveForBots;
        putBoolean("saveForBots", saveForBots);
    }

    public static void toggleShowGhostToggleInDrawer() {
        showGhostToggleInDrawer = !showGhostToggleInDrawer;
        putBoolean("showGhostToggleInDrawer", showGhostToggleInDrawer);
    }

    public static String getDeletedMark() {
        if (preferences == null) {
            return "deleted";
        }
        return preferences.getString("deletedMarkText", "deleted");
    }

    public static boolean saveDeletedMessageFor(int accountId, long dialogId) {
        if (!saveDeletedMessages) {
            return false;
        }
        TLRPC.User user = MessagesController.getInstance(accountId).getUser(dialogId);
        if (user == null) {
            return true;
        }
        return !user.bot || saveForBots;
    }

    public static boolean saveEditedMessageFor(int accountId, long dialogId) {
        if (!saveEditedMessages) {
            return false;
        }
        TLRPC.User user = MessagesController.getInstance(accountId).getUser(dialogId);
        if (user == null) {
            return true;
        }
        return !user.bot || saveForBots;
    }
}
