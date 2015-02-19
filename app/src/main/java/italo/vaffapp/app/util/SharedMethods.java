package italo.vaffapp.app.util;

import android.app.Activity;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;

import italo.vaffapp.app.R;

/**
 * Created by iarmenti on 11/27/14.
 */
public class SharedMethods {

    /* set vaffapp icon in ActionBar - Needed from Android 5
      Used by: SimpleEula, MainActivity
     */
    public static void setIconInActionBar(ActionBarActivity act) {
        ActionBar ab = act.getSupportActionBar();

        ab.setDisplayShowHomeEnabled(true);
        ab.setLogo(R.drawable.ic_launcher);
        ab.setDisplayUseLogoEnabled(true);
    }

    /* get this app's info
    Used by: SimpleEula, MainActivity
     */
    public static PackageInfo getPackageInfo(Activity a) {
        PackageInfo pi = null;
        try {
            pi = a.getPackageManager().getPackageInfo(a.getPackageName(), PackageManager.GET_ACTIVITIES);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        return pi;
    }
}
