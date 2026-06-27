package tw.nekomimi.nekogram.ayu;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.text.TextUtils;

import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.FileLog;
import org.telegram.messenger.MessageObject;
import org.telegram.tgnet.SerializedData;
import org.telegram.tgnet.TLRPC;

import java.util.ArrayList;
import java.util.List;

public class AyuMessageStore extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "ayu_messages.db";
    private static final int DATABASE_VERSION = 1;

    private static volatile AyuMessageStore instance;

    public static AyuMessageStore getInstance() {
        if (instance == null) {
            synchronized (AyuMessageStore.class) {
                if (instance == null) {
                    instance = new AyuMessageStore(ApplicationLoader.applicationContext);
                }
            }
        }
        return instance;
    }

    private AyuMessageStore(android.content.Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE saved_messages (account_id INTEGER NOT NULL, channel_id INTEGER NOT NULL, message_id INTEGER NOT NULL, dialog_id INTEGER NOT NULL, date INTEGER NOT NULL, edit_date INTEGER NOT NULL, text TEXT, data BLOB, deleted INTEGER NOT NULL DEFAULT 0, deleted_date INTEGER NOT NULL DEFAULT 0, PRIMARY KEY(account_id, channel_id, message_id))");
        db.execSQL("CREATE TABLE saved_edits (account_id INTEGER NOT NULL, channel_id INTEGER NOT NULL, message_id INTEGER NOT NULL, dialog_id INTEGER NOT NULL, edit_date INTEGER NOT NULL, captured_date INTEGER NOT NULL, text TEXT, data BLOB)");
        db.execSQL("CREATE INDEX idx_saved_messages_dialog ON saved_messages(account_id, dialog_id)");
        db.execSQL("CREATE INDEX idx_saved_edits_dialog ON saved_edits(account_id, dialog_id)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
    }

    private static long computeChannelId(TLRPC.Message message) {
        if (message.peer_id instanceof TLRPC.TL_peerChannel) {
            return message.peer_id.channel_id;
        }
        return 0;
    }

    private static String extractText(TLRPC.Message message) {
        if (!TextUtils.isEmpty(message.message)) {
            return message.message;
        }
        if (message.media != null) {
            return "[" + message.media.getClass().getSimpleName() + "]";
        }
        return "";
    }

    private static byte[] serialize(TLRPC.Message message) {
        try {
            SerializedData data = new SerializedData(message.getObjectSize());
            message.serializeToStream(data);
            byte[] result = data.toByteArray();
            data.cleanup();
            return result;
        } catch (Exception e) {
            FileLog.e(e);
            return null;
        }
    }

    public void onSeenMessages(int accountId, List<TLRPC.Message> messages) {
        if (messages == null || messages.isEmpty()) {
            return;
        }
        SQLiteDatabase db;
        try {
            db = getWritableDatabase();
        } catch (Exception e) {
            FileLog.e(e);
            return;
        }
        for (TLRPC.Message message : messages) {
            if (!(message instanceof TLRPC.TL_message)) {
                continue;
            }
            if (message.id <= 0) {
                continue;
            }
            try {
                long channelId = computeChannelId(message);
                long dialogId = MessageObject.getDialogId(message);

                boolean wantDeleted = AyuConfig.saveDeletedMessageFor(accountId, dialogId);
                boolean wantEdited = AyuConfig.saveEditedMessageFor(accountId, dialogId);
                if (!wantDeleted && !wantEdited) {
                    continue;
                }

                long previousEditDate = -1;
                Cursor cursor = db.rawQuery(
                        "SELECT edit_date, text, data, date, dialog_id FROM saved_messages WHERE account_id=? AND channel_id=? AND message_id=?",
                        new String[]{String.valueOf(accountId), String.valueOf(channelId), String.valueOf(message.id)});
                byte[] previousData = null;
                String previousText = null;
                int previousDate = 0;
                long previousDialogId = dialogId;
                if (cursor.moveToFirst()) {
                    previousEditDate = cursor.getLong(0);
                    previousText = cursor.getString(1);
                    previousData = cursor.getBlob(2);
                    previousDate = cursor.getInt(3);
                    previousDialogId = cursor.getLong(4);
                }
                cursor.close();

                boolean edited = previousEditDate != -1 && message.edit_date > previousEditDate;
                if (edited && wantEdited) {
                    ContentValues editValues = new ContentValues();
                    editValues.put("account_id", accountId);
                    editValues.put("channel_id", channelId);
                    editValues.put("message_id", message.id);
                    editValues.put("dialog_id", previousDialogId);
                    editValues.put("edit_date", previousEditDate);
                    editValues.put("captured_date", System.currentTimeMillis() / 1000);
                    editValues.put("text", previousText);
                    editValues.put("data", previousData);
                    db.insert("saved_edits", null, editValues);
                }

                byte[] data = serialize(message);
                ContentValues values = new ContentValues();
                values.put("account_id", accountId);
                values.put("channel_id", channelId);
                values.put("message_id", message.id);
                values.put("dialog_id", dialogId);
                values.put("date", message.date);
                values.put("edit_date", message.edit_date);
                values.put("text", extractText(message));
                values.put("data", data);
                values.put("deleted", 0);
                values.put("deleted_date", 0);
                db.insertWithOnConflict("saved_messages", null, values, SQLiteDatabase.CONFLICT_REPLACE);
            } catch (Exception e) {
                FileLog.e(e);
            }
        }
    }

    public void markDeleted(int accountId, long channelId, ArrayList<Integer> ids) {
        if (ids == null || ids.isEmpty()) {
            return;
        }
        SQLiteDatabase db;
        try {
            db = getWritableDatabase();
        } catch (Exception e) {
            FileLog.e(e);
            return;
        }
        try {
            StringBuilder inClause = new StringBuilder();
            for (int i = 0; i < ids.size(); i++) {
                if (i > 0) {
                    inClause.append(",");
                }
                inClause.append(ids.get(i).intValue());
            }
            long now = System.currentTimeMillis() / 1000;
            db.execSQL("UPDATE saved_messages SET deleted=1, deleted_date=" + now
                    + " WHERE account_id=" + accountId
                    + " AND channel_id=" + channelId
                    + " AND message_id IN (" + inClause + ")");
        } catch (Exception e) {
            FileLog.e(e);
        }
    }

    public List<Long> getDialogsWithSaved(int accountId) {
        ArrayList<Long> result = new ArrayList<>();
        try {
            SQLiteDatabase db = getReadableDatabase();
            Cursor cursor = db.rawQuery(
                    "SELECT DISTINCT dialog_id FROM saved_messages WHERE account_id=? AND deleted=1 "
                            + "UNION SELECT DISTINCT dialog_id FROM saved_edits WHERE account_id=?",
                    new String[]{String.valueOf(accountId), String.valueOf(accountId)});
            while (cursor.moveToNext()) {
                result.add(cursor.getLong(0));
            }
            cursor.close();
        } catch (Exception e) {
            FileLog.e(e);
        }
        return result;
    }

    public List<AyuSavedRecord> getDeletedRecords(int accountId, long dialogId) {
        ArrayList<AyuSavedRecord> result = new ArrayList<>();
        try {
            SQLiteDatabase db = getReadableDatabase();
            Cursor cursor = db.rawQuery(
                    "SELECT message_id, date, deleted_date, text FROM saved_messages WHERE account_id=? AND dialog_id=? AND deleted=1 ORDER BY deleted_date DESC, message_id DESC",
                    new String[]{String.valueOf(accountId), String.valueOf(dialogId)});
            while (cursor.moveToNext()) {
                AyuSavedRecord record = new AyuSavedRecord();
                record.messageId = cursor.getInt(0);
                record.date = cursor.getInt(1);
                record.eventDate = cursor.getLong(2);
                record.text = cursor.getString(3);
                record.deleted = true;
                result.add(record);
            }
            cursor.close();
        } catch (Exception e) {
            FileLog.e(e);
        }
        return result;
    }

    public List<AyuSavedRecord> getEditedRecords(int accountId, long dialogId) {
        ArrayList<AyuSavedRecord> result = new ArrayList<>();
        try {
            SQLiteDatabase db = getReadableDatabase();
            Cursor cursor = db.rawQuery(
                    "SELECT message_id, edit_date, captured_date, text FROM saved_edits WHERE account_id=? AND dialog_id=? ORDER BY captured_date DESC",
                    new String[]{String.valueOf(accountId), String.valueOf(dialogId)});
            while (cursor.moveToNext()) {
                AyuSavedRecord record = new AyuSavedRecord();
                record.messageId = cursor.getInt(0);
                record.date = cursor.getInt(1);
                record.eventDate = cursor.getLong(2);
                record.text = cursor.getString(3);
                record.deleted = false;
                result.add(record);
            }
            cursor.close();
        } catch (Exception e) {
            FileLog.e(e);
        }
        return result;
    }

    public static class AyuSavedRecord {
        public int messageId;
        public int date;
        public long eventDate;
        public String text;
        public boolean deleted;
    }
}
