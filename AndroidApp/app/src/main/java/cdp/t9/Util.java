package cdp.t9;

import java.util.UUID;

public class Util {
    private final static char[] hexArray = "0123456789ABCDEF".toCharArray();
    public static String bytesToHex(byte[] bytes) {
        char[] hexChars = new char[bytes.length * 2];
        for ( int j = 0; j < bytes.length; j++ ) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = hexArray[v >>> 4];
            hexChars[j * 2 + 1] = hexArray[v & 0x0F];
        }
        return new String(hexChars);
    }

    public static String bytesToHex(byte bytedata) {
        char[] hexChars = new char[2];

        int v = bytedata & 0xFF;
        hexChars[0] = hexArray[v >>> 4];
        hexChars[1] = hexArray[v & 0x0F];

        return new String(hexChars);
    }

    public static String unHex(String arg) {
        String str = "";
        for (int i = 0; i < arg.length(); i += 3) {
            String s = arg.substring(i, (i + 2));
            int decimal = Integer.parseInt(s, 16);
            str = str + (char) decimal;
        }
        return str;
    }

    public final static UUID uuidGenericAccess = UUID.fromString("00001801-0000-1000-8000-00805f9b34fb");
    public final static UUID uuidGenericAttribute = UUID.fromString("00001801-0000-1000-8000-00805f9b34fb");
}
