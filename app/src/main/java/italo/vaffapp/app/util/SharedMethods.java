package italo.vaffapp.app.util;

import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;

import italo.vaffapp.app.R;

/**
 * Created by iarmenti on 11/27/14.
 */
public class SharedMethods {

    /* set vaffapp icon in ActionBar
      Needed from Android 5 */
    public static void setIconInActionBar(ActionBarActivity act) {
        ActionBar ab = act.getSupportActionBar();

        ab.setDisplayShowHomeEnabled(true);
        ab.setLogo(R.drawable.ic_launcher);
        ab.setDisplayUseLogoEnabled(true);
    }
}
