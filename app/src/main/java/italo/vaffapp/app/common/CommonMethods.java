package italo.vaffapp.app.common;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.Drawable;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.flurry.android.FlurryAgent;

import java.util.ArrayList;
import java.util.TreeMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import italo.vaffapp.app.R;
import italo.vaffapp.app.database.DatabaseHandler;
import italo.vaffapp.app.entity.Insult;
import italo.vaffapp.app.util.App;
import italo.vaffapp.app.util.Speaker;

/**
 * Created by iarmenti on 11/27/14.
 */
public class CommonMethods {
    // for sharing
    public static int SHARE_REQUEST = 1; // to be used in onActivityResult
    private static Intent sharingIntent;
    private static String hid_link = "http://adf.ly/ssss4";
    private static String vaffapp_link = "https://play.google.com/store/apps/details?id=italo.vaffapp.app";
    private static String vf_hashtag = " #VaffApp"; // one space before on purpose

    private static Speaker speaker;

    private static final int UNBLOCK_INSULTS = 20; // insults to unlock everytime sharing is done 3 times

    /* on* methods */
    public static void onStart(Context c) {
        //initialize TextToSpeech objects in Speaker
        speaker = new Speaker(c);

        FlurryAgent.setLogEnabled(false);
        FlurryAgent.init(c, c.getString(R.string.flurry_id));
        FlurryAgent.onStartSession(c);
    }

    public static void onPause() {
        speaker.onPause();
    }

    public static void onStop(Activity a) {
        FlurryAgent.onEndSession(a);
    }

    public static void sendFlurry(String name, Map<String, String> flurry_stats) {
        FlurryAgent.logEvent(name, flurry_stats);
    }

    public static void sendEventFlurry(String name){
        FlurryAgent.logEvent(name);
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

    /* fetch all insults from database */
    public static ArrayList<Insult> loadInsults(Activity act) {
        ArrayList<Insult> insults = null;

        DatabaseHandler db = new DatabaseHandler(act.getApplicationContext());
        db.openDataBase();
        insults = db.getAllInsults();
        db.close();

        Toast.makeText(act, insults.size() + " " + act.getString(R.string.n_insulti), Toast.LENGTH_SHORT).show();

        return insults;
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
            case 1: region = "Molise"; break;
            case 2: region = "Abruzzo"; break;
            case 3: region = "Basilicata"; break;
            case 4: region = "Calabria"; break;
            case 5: region = "Campania"; break;
            case 6: region = "Emilia Romagna"; break;
            case 7: region = "Friuli Venezia Giulia"; break;
            case 8: region = "Lazio"; break;
            case 9: region = "Liguria"; break;
            case 10: region = "Lombardia"; break;
            case 11: region = "Marche"; break;
            case 12: region = "Piemonte"; break;
            case 13: region = "Puglia"; break;
            case 14: region = "Sardegna"; break;
            case 15: region = "Sicilia"; break;
            case 16: region = "Toscana"; break;
            case 17: region = "Trentino Alto Adige"; break;
            case 18: region = "Umbria"; break;
            case 19: region = "Valle d'Aosta"; break;
            case 20: region = "Veneto"; break;
            case 21: region = "Nazionale"; break;
            default: region = ""; break;
        }

        return region;
    }

    /* methods used to share! */
    public static void share(final Activity a, final Insult insult) {
        List<ResolveInfo> diff_app = checkPresenceOfApps(a);

        if (diff_app.size() > 0) {
            preChoiceMenu(diff_app, a, insult);
        } else {
            sharingIntent.putExtra(Intent.EXTRA_TEXT, insult + vf_hashtag);
            a.startActivity(Intent.createChooser(sharingIntent, a.getString(R.string.choice1)));
        }
    }

    // check for presence of Facebook, Messenger, Twitter, WhatsApp, Hangaout, and Viber apps (these will be treated differently)
    public static List<ResolveInfo> checkPresenceOfApps(Activity a) {
        List<ResolveInfo> diff_app = new ArrayList<ResolveInfo>();
        sharingIntent = new Intent(Intent.ACTION_SEND);

        sharingIntent.setType("text/plain");
        PackageManager pm = a.getPackageManager();
        List<ResolveInfo> activityList = pm.queryIntentActivities(sharingIntent, 0);

        for (final ResolveInfo app : activityList) {
            String packageName = app.activityInfo.packageName;
            // facebook.katana is FB app, facebook.orca is the messenger
            if ( packageName.contains("com.twitter.android")
                    || packageName.contains("com.facebook.orca") || packageName.contains("com.whatsapp")
                    || packageName.contains("google.android.talk") || packageName.contains("com.viber")
                    || packageName.contains("com.android.mms") || packageName.contains("org.telegram.messenger")) {
                diff_app.add(app);
                continue;
            }
            // skip these
            if (packageName.contains("com.android.bluetooth") || packageName.contains("flipboard.app")
                    || packageName.contains("com.sec.android.widgetapp.diotek.smemo") || packageName.contains("com.google.android.apps.docs")) {
                continue;
            }
        }
        return diff_app;
    }

    // show a first choice dialog to choose between Twitter, Facebook, Messenger, Hangout, Viber, SMS
    public static void preChoiceMenu(List<ResolveInfo> diff_app, final Activity a, final Insult insult) {
        final CharSequence insult_text = insult.getInsult();

        String package_name;
        Drawable icon;
        App app;
        final App[] apps = new App[diff_app.size()];
        PackageManager pm = a.getPackageManager();

        for (int i = 0; i < diff_app.size(); i++) {
            package_name = diff_app.get(i).activityInfo.packageName;
            icon = diff_app.get(i).loadIcon(pm);
            if (package_name.contains("com.twitter.android")) {
                app = new App("Twitter", package_name, icon);
                apps[i] = app;
            }
            if (package_name.contains("com.facebook.orca")) {
                app = new App("Messenger", package_name, icon);
                apps[i] = app;
            }
            if (package_name.contains("com.whatsapp")) {
                app = new App("WhatsApp", package_name, icon);
                apps[i] = app;
            }
            if (package_name.contains("google.android.talk")) {
                app = new App("Hangout", package_name, icon);
                apps[i] = app;
            }
            if (package_name.contains("com.viber")) {
                app = new App("Viber", package_name, icon);
                apps[i] = app;
            }
            if (package_name.contains("org.telegram.messenger")) {
                app = new App("Telegram", package_name, icon);
                apps[i] = app;
            }
            if (package_name.contains("com.android.mms")) {
                app = new App("SMS/Text", package_name, icon);
                apps[i] = app;
            }
        }

        /* show icons of apps */
        // http://stackoverflow.com/questions/3920640/how-to-add-icon-in-alert-dialog-before-each-item
        ListAdapter adapter = new ArrayAdapter<App>(
                a,
                android.R.layout.select_dialog_item,
                android.R.id.text1,
                apps){
            public View getView(int position, View convertView, ViewGroup parent) {
                //User super class to create the View
                View v = super.getView(position, convertView, parent);
                TextView tv = (TextView)v.findViewById(android.R.id.text1);

                //Put the image on the TextView
                tv.setCompoundDrawablesWithIntrinsicBounds(apps[position].icon, null, null, null);

                //Add margin between image and text (support various screen densities)
                int dp5 = (int) (5 * a.getResources().getDisplayMetrics().density + 0.5f);
                tv.setCompoundDrawablePadding(dp5);

                return v;
            }
        };

        new AlertDialog.Builder(a)
                .setTitle(a.getString(R.string.choice1))
                .setAdapter(adapter, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String choice = apps[which].toString();
                        // Flurry analytics
                        Map<String, String> flurry_stats = new HashMap<String, String>();

                        Intent targetedShareIntent = new Intent(Intent.ACTION_SEND);
                        targetedShareIntent.setType("text/plain");
                        targetedShareIntent.putExtra(Intent.EXTRA_TEXT, insult_text + vf_hashtag + "\n\n--" + hid_link);
                        if (choice.equals("Twitter")) {
                            flurry_stats.put("Share on", "Twitter");
                            flurry_stats.put("Insult", insult_text.toString());
                            // can't share a link on twitter
                            targetedShareIntent.putExtra(Intent.EXTRA_TEXT, insult_text + vf_hashtag);

                            targetedShareIntent.setPackage(apps[which].getPackageName());
                            a.startActivityForResult(targetedShareIntent, SHARE_REQUEST);
                        }
                        if (choice.equals("Messenger")) {
                            flurry_stats.put("Share on", "Messenger");
                            flurry_stats.put("Insult", insult_text.toString());
                            // sharing a link on messenger shows also a preview of the website
                            // so it's better to use the original link
                            targetedShareIntent.putExtra(Intent.EXTRA_TEXT, insult_text + vf_hashtag + "\n\n--" + vaffapp_link);
                            targetedShareIntent.setPackage(apps[which].getPackageName());
                            a.startActivityForResult(targetedShareIntent, SHARE_REQUEST);
                        }
                        if (choice.equals("WhatsApp")) {
                            flurry_stats.put("Share on", "WhatsApp");
                            flurry_stats.put("Insult", insult_text.toString());

                            targetedShareIntent.setPackage(apps[which].getPackageName());
                            a.startActivityForResult(targetedShareIntent, SHARE_REQUEST);
                        }
                        if (choice.equals("Hangout")) {
                            flurry_stats.put("Share on", "Hangout");
                            flurry_stats.put("Insult", insult_text.toString());

                            targetedShareIntent.setPackage(apps[which].getPackageName());
                            a.startActivityForResult(targetedShareIntent, SHARE_REQUEST);
                        }
                        if (choice.equals("Viber")) {
                            flurry_stats.put("Share on", "Viber");
                            flurry_stats.put("Insult", insult_text.toString());

                            targetedShareIntent.setPackage(apps[which].getPackageName());
                            a.startActivityForResult(targetedShareIntent, SHARE_REQUEST);
                        }
                        if (choice.equals("Telegram")) {
                            flurry_stats.put("Share on", "Telegram");
                            flurry_stats.put("Insult", insult_text.toString());

                            targetedShareIntent.setPackage(apps[which].getPackageName());
                            a.startActivityForResult(targetedShareIntent, SHARE_REQUEST);
                        }
                        if (choice.equals("SMS/Text")) {
                            flurry_stats.put("Share on", "SMS");
                            flurry_stats.put("Insult", insult_text.toString());

                            targetedShareIntent.setPackage(apps[which].getPackageName());
                            a.startActivityForResult(targetedShareIntent, SHARE_REQUEST);
                        }
                        sendFlurry("Sharing", flurry_stats);
                    }
                }).create().show();
    }
    /* methods used to share! END */


    public static void checkSharedInsults(Activity a, String msg, int shared_insults) {
        if (shared_insults % 3 == 0)
            unlockInsults(a, msg, CommonMethods.UNBLOCK_INSULTS);
    }


    /* Logic around unlocking insults */
    public static void unlockInsults(Activity a, String title, int size){
        ArrayList<Insult> unlocked = null;

        DatabaseHandler db = new DatabaseHandler(a);
        db.openDataBase();
        unlocked = db.unlockInsults(size);
        db.close();

        if ( unlocked != null && unlocked.size() > 0) {
            String text =
                    a.getString(R.string.unlocked) + " " + unlocked.size() + " " + a.getString(R.string.insults) + "\n\n";

            for (Insult temp : unlocked) {
                text += CommonMethods.getRegionFromId(temp.getRegionId()).toUpperCase() + ": " + temp.getInsult() + "\n";
            }

            showDialog(a, title, text);
        }
    }

    public static String getStringInsultsPerRegion(Activity a){
        String StringRegionNInsults = new String();
        int tot_insults = 0;
        DatabaseHandler db = new DatabaseHandler(a);
        db.openDataBase();
        TreeMap<Integer, ArrayList<Integer>> regionNInsults = db.getInsultsPerRegion();
        db.close();

        for (Integer amount_insults : regionNInsults.descendingKeySet()){
            ArrayList<Integer> regions = regionNInsults.get(amount_insults);
            for (Integer region : regions) {
                StringRegionNInsults += getRegionFromId(region) + ": " + amount_insults + "\n";
                tot_insults += amount_insults;
            }
        }
        StringRegionNInsults += "TOT: " + tot_insults;

        return StringRegionNInsults;
    }

    public static int getAmountBlockedInsults(Activity a){
        int blocked = 0;

        DatabaseHandler db = new DatabaseHandler(a);
        db.openDataBase();
        blocked = db.countBlockedInsults();
        db.close();

        return blocked;
    }
    /* Logic around unlocking insults - END */


    public static void showDialog(Activity a, String title, String text){
        AlertDialog.Builder builder = new AlertDialog.Builder(a);
        builder.setMessage(text)
                .setTitle(title)
                .setNeutralButton(a.getString(R.string.ok), null);
        builder.create().show();
    }


    /* speaker methods */
    public static void speakInsult(String s) {
        speaker.speakInsult(s);
    }

    public static void speakDesc(String s) {
        speaker.speakInsult(s);
    }

    public static void speakEnglish(String s) {
        speaker.speakEnglish(s);
    }
     /* speaker methods  - END */
}
