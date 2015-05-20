package italo.vaffapp.app.util;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;

import com.facebook.Session;
import com.facebook.UiLifecycleHelper;

import java.util.ArrayList;
import java.util.List;

import italo.vaffapp.app.R;
import italo.vaffapp.app.databases.DatabaseHandler;
import italo.vaffapp.app.databases.Insult;

import italo.vaffapp.app.Speaker;

/**
 * Created by iarmenti on 11/27/14.
 */
public class SharedMethods {
    // for condividi
    private static List<Intent> targetedShareIntents;
    private static List<ResolveInfo> diff_app;
    private static Intent sharingIntent;
    private static Speaker speaker;
    private static String vf_hashtag = " #VaffApp"; // one space on purpose

    // for FB
    private static UiLifecycleHelper uiHelper;
    private static Session.StatusCallback callback = null;

    /* on* methods */
    public static void onCreate(Activity a, Bundle savedInstanceState) {
        // FB code, UiLifecycleHelper needed to share a post - https://developers.facebook.com/docs/android/share
        // Includes callback in case FB app is not installed!
        // 1. configure the UiLifecycleHelper in onCreate
        uiHelper = new UiLifecycleHelper(a, callback);
        uiHelper.onCreate(savedInstanceState);
    }

    public static void onStart(Context c) {
        //initialize TextToSpeech objects in Speaker
        speaker = new Speaker(c);
    }

    public static void onResume() {
        uiHelper.onResume();
    }

    public static void onActivityResult(int requestCode, int resultCode, Intent data) {
        uiHelper.onActivityResult(requestCode, resultCode, data);
    }

    public static void onSaveInstanceState(Bundle outState) {
        uiHelper.onSaveInstanceState(outState);
    }

    public static void onPause() {
        uiHelper.onPause();
        speaker.onPause();
    }

    public static void onDestroy() {
        uiHelper.onDestroy();
    }

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

    public static String getRegionFromId(int region_id){
        String region;
        switch(region_id){
            /* Edit 15-01-2015: In db I organized regions in alphabetic order so their ids changed
              (before they were in order-ish from north to south) */
            case 1: region = "Molise"; break;
            case 19: region = "Valle d'Aosta"; break;
            case 12: region = "Piemonte"; break;
            case 10: region = "Lombardia"; break;
            case 17: region = "Trentino Alto Adige"; break;
            case 7: region = "Friuli Venezia Giulia"; break;
            case 20: region = "Veneto"; break;
            case 6: region = "Emilia Romagna"; break;
            case 9: region = "Liguria"; break;
            case 16: region = "Toscana"; break;
            case 8: region = "Lazio"; break;
            case 18: region = "Umbria"; break;
            case 11: region = "Marche"; break;
            case 2: region = "Abruzzo"; break;
            case 5: region = "Campania"; break;
            case 13: region = "Puglia"; break;
            case 3: region = "Basilicata"; break;
            case 4: region = "Calabria"; break;
            case 15: region = "Sicilia"; break;
            case 14: region = "Sardegna"; break;
            case 21: region = "Nazionale"; break;
            default: region = ""; break;
        }

        return region;
    }

    /* Logic around unblocking insults */
    public static void unblockInsults(Activity a, String title, int size){
        ArrayList<Insult> unblocked = null;

        DatabaseHandler db = new DatabaseHandler(a);
        db.openDataBase();
        unblocked = db.unblockInsults(size);
        db.close();

        if ( unblocked != null && unblocked.size() > 0) {
            String text =
                    a.getString(R.string.unblocked) + " " + unblocked.size() + " " + a.getString(R.string.insults) + "\n\n";

            for (Insult temp : unblocked) {
                text += SharedMethods.getRegionFromId(temp.getRegionId()).toUpperCase() + ": " + temp.getInsult() + "\n";
            }

            showDialog(a, title, text);
        }
    }

    public static int getAmountBlockedInsults(Activity a){
        int blocked = 0;

        DatabaseHandler db = new DatabaseHandler(a);
        db.openDataBase();
        blocked = db.countBlockedInsults();
        db.close();

        return blocked;
    }
    /* Logic around unblocking insults - END */

    public static void showDialog(Activity a, String title, String text){
        AlertDialog.Builder builder = new AlertDialog.Builder(a);
        builder.setMessage(text)
                .setTitle(title)
                .setNeutralButton(a.getString(R.string.ok), null);
        builder.create().show();
    }
}
