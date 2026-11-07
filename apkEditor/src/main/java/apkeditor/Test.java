package apkeditor;

import android.content.ContentResolver;
import android.content.Context;
import android.provider.Settings;

public class Test {
    public String test(Context ctx) {
        ContentResolver r = ctx.getContentResolver();
        String id = Settings.Secure.getString(r, "android_id");
        return Utils.stringAdd1(id);
    }

    public void testStack() {
        Utils.printCallStack();
    }
}
