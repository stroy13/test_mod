package apkeditor;

import android.content.Context;
import android.util.Log;
import android.widget.Toast;
import java.lang.reflect.Field;
import java.util.Date;

public class Utils {
    private static final String TAG = "APKEDITOR";

    public static void showToast(Context ctx, String msg) {
        Toast.makeText(ctx, msg, 1).show();
    }

    public static void log(String msg) {
        Log.d(TAG, msg);
    }

    public static void dumpValue(Object obj) {
        if (obj != null) {
            log("Values of " + obj + ":");
            getAllValues(obj, 1, 3);
        } else {
            log("null");
        }
    }

    public static String stringAdd1(String str) {
        if (str != null && !str.equals("")) {
            char c = str.charAt(str.length() - 1);
            return str.substring(0, str.length() - 1) + ((char) (c + 1));
        }
        return str;
    }

    public static String generateImei(int offset) {
        long imei = offset + 35806501910400L;
        int checksum = imei_checksum(imei);
        return String.valueOf((10 * imei) + checksum);
    }

    private static int imei_checksum(long imei) {
        int[] sum = new int[15];
        for (int i = 1; i <= 14; i++) {
            sum[i] = (int) (imei % 10);
            if (i % 2 != 0) {
                sum[i] = sum[i] * 2;
            }
            if (sum[i] >= 10) {
                sum[i] = (sum[i] % 10) + (sum[i] / 10);
            }
            imei /= 10;
        }
        int check = 0;
        for (int i2 : sum) {
            check += i2;
        }
        int i3 = check * 9;
        return i3 % 10;
    }

    private static String getValueString(Object value) {
        if (value instanceof String[]) {
            String[] strArray = (String[]) value;
            StringBuilder sb = new StringBuilder();
            sb.append("String[]={");
            for (int i = 0; i < strArray.length; i++) {
                sb.append("" + i + ":" + strArray[i] + ", ");
            }
            sb.append("}");
            return sb.toString();
        }
        if (!(value instanceof Integer[])) {
            return value == null ? "null" : value.toString();
        }
        Integer[] intArray = (Integer[]) value;
        StringBuilder sb2 = new StringBuilder();
        sb2.append("Integer[]={");
        for (int i2 = 0; i2 < intArray.length; i2++) {
            sb2.append("" + i2 + ":" + intArray[i2] + ", ");
        }
        sb2.append("}");
        return sb2.toString();
    }

    private static String getPadding(int level) {
        String[] buffers = {"", "  ", "    ", "      ", "        ", "          ", "            ", "              ", "                ", "                  ", "                    "};
        if (level < buffers.length) {
            return buffers[level];
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < level; i++) {
            sb.append("  ");
        }
        return sb.toString();
    }

    private static void getAllValues(Object obj, int level, int maxlevel) {
        Field[] fields = obj.getClass().getDeclaredFields();
        for (int i = 0; i < fields.length; i++) {
            if (!fields[i].isAccessible()) {
                fields[i].setAccessible(true);
            }
            try {
                Object value = fields[i].get(obj);
                log(getPadding(level) + "Name: " + fields[i].getName() + ", Value: " + getValueString(value));
                if (level < maxlevel && value != null && !isBasicType(value)) {
                    getAllValues(value, level + 1, maxlevel);
                }
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            } catch (IllegalArgumentException e2) {
                e2.printStackTrace();
            }
        }
    }

    private static boolean isBasicType(Object param) {
        return (param instanceof Integer) || (param instanceof String) || (param instanceof Double) || (param instanceof Float) || (param instanceof Long) || (param instanceof Boolean) || (param instanceof Date) || (param instanceof Integer[]) || (param instanceof String[]) || (param instanceof Double[]) || (param instanceof Float[]) || (param instanceof Long[]) || (param instanceof Boolean[]) || (param instanceof Date[]);
    }

    public static void printCallStack() {
        printCallStack(null);
    }

    public static void printCallStack(String tag) {
        if (tag != null) {
            log("Stack at " + tag + ": ");
        } else {
            log("Stack:");
        }
        Throwable ex = new Throwable();
        StackTraceElement[] stackElements = ex.getStackTrace();
        if (stackElements != null) {
            for (StackTraceElement stackTraceElement : stackElements) {
                log("\t" + stackTraceElement.toString());
            }
        }
    }
}
