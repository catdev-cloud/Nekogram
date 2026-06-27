package tw.nekomimi.nekogram.ayu;

import org.telegram.tgnet.TLObject;
import org.telegram.tgnet.TLRPC;
import org.telegram.tgnet.tl.TL_account;

public class AyuGhost {

    public static boolean shouldDropTyping(TLObject object) {
        if (AyuConfig.sendUploadProgress) {
            return false;
        }
        return object instanceof TLRPC.TL_messages_setTyping
                || object instanceof TLRPC.TL_messages_setEncryptedTyping;
    }

    public static boolean shouldDropRead(TLObject object) {
        if (AyuConfig.sendReadPackets) {
            return false;
        }
        boolean isReadRequest = object instanceof TLRPC.TL_messages_readHistory
                || object instanceof TLRPC.TL_messages_readEncryptedHistory
                || object instanceof TLRPC.TL_messages_readDiscussion
                || object instanceof TLRPC.TL_messages_readMessageContents
                || object instanceof TLRPC.TL_channels_readHistory
                || object instanceof TLRPC.TL_channels_readMessageContents;
        if (!isReadRequest) {
            return false;
        }
        return !AyuState.getAllowReadPacket();
    }

    public static void adjustOnline(TLObject object) {
        if (AyuConfig.sendOnlinePackets) {
            return;
        }
        if (object instanceof TL_account.updateStatus) {
            ((TL_account.updateStatus) object).offline = true;
        }
    }

    public static TLRPC.TL_messages_affectedMessages buildFakeReadResponse() {
        TLRPC.TL_messages_affectedMessages fake = new TLRPC.TL_messages_affectedMessages();
        fake.pts = -1;
        fake.pts_count = 0;
        return fake;
    }
}
