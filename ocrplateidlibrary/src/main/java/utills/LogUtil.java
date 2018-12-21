//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package utills;

import android.util.Log;

public class LogUtil {
    public static boolean dbug = true;

    public LogUtil() {
    }

    public static void D(String tag, String text) {
        if(dbug) {
            Log.d(tag, text);
        }

    }

    public static void E(String tag, String text) {
        if(dbug) {
            Log.e(tag, text);
        }

    }

    public static void I(String tag, String text) {
        if(dbug) {
            Log.i(tag, text);
        }

    }
}
