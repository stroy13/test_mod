package com.gmail.heagoo.sqliteutil.util;


public class HexUtil {
    public static String bytesToHexString(byte[] src, int offset, int len) {
        StringBuilder stringBuilder = new StringBuilder("");
        if (src == null || src.length <= 0) {
            return null;
        }
        int v = src[offset] & 255;
        String hv = Integer.toHexString(v);
        if (hv.length() < 2) {
            stringBuilder.append(0);
        }
        stringBuilder.append(hv);
        for (int i = offset + 1; i < offset + len; i++) {
            stringBuilder.append(" ");
            int v2 = src[i] & 255;
            String hv2 = Integer.toHexString(v2);
            if (hv2.length() < 2) {
                stringBuilder.append(0);
            }
            stringBuilder.append(hv2);
        }
        return stringBuilder.toString();
    }

    public static byte[] hexStringToBytes(String hexString) {
        if (hexString == null || hexString.equals("")) {
            return null;
        }
        String hexString2 = hexString.toUpperCase();
        int length = hexString2.length() / 2;
        char[] hexChars = hexString2.toCharArray();
        byte[] d = new byte[length];
        for (int i = 0; i < length; i++) {
            int pos = i * 2;
            d[i] = (byte) ((charToByte(hexChars[pos]) << 4) | charToByte(hexChars[pos + 1]));
        }
        return d;
    }

    private static byte charToByte(char c) {
        return (byte) "0123456789ABCDEF".indexOf(c);
    }
}
