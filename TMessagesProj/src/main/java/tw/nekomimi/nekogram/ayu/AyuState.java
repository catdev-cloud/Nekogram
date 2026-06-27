package tw.nekomimi.nekogram.ayu;

public class AyuState {
    private static final AyuStateVariable allowReadPacket = new AyuStateVariable();

    public static void setAllowReadPacket(boolean val, int resetAfter) {
        allowReadPacket.val = val;
        allowReadPacket.resetAfter = resetAfter;
    }

    public static boolean getAllowReadPacket() {
        return AyuConfig.sendReadPackets || allowReadPacket.process();
    }
}
