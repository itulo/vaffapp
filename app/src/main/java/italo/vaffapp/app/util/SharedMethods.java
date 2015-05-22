package italo.vaffapp.app.util;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.facebook.Session;
import com.facebook.UiLifecycleHelper;
import com.facebook.widget.FacebookDialog;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import italo.vaffapp.app.App;
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

    /* methods used to share! */
    public static void share(final Activity a, final Insult insult) {
        checkPresenceOfApps(a, insult);

        if (diff_app.size() > 0) {
            preChoiceMenu(a, insult);
        } else {
            sharingIntent.putExtra(Intent.EXTRA_TEXT, insult + vf_hashtag);
            a.startActivity(Intent.createChooser(sharingIntent, a.getString(R.string.choice1)));
        }
    }

    // check for presence of Facebook, Messenger, Twitter, WhatsApp, Hangaout, and Viber apps (these will be treated differently)
    public static void checkPresenceOfApps(Activity a, Insult insult) {
        targetedShareIntents = new ArrayList<Intent>();
        diff_app = new ArrayList<ResolveInfo>();
        sharingIntent = new Intent(Intent.ACTION_SEND);

        sharingIntent.setType("text/plain");
        PackageManager pm = a.getPackageManager();
        List<ResolveInfo> activityList = pm.queryIntentActivities(sharingIntent, 0);

        for (final ResolveInfo app : activityList) {
            String packageName = app.activityInfo.packageName;
            // facebook.katana is FB app, facebook.orca is the messenger
            if (packageName.contains("com.facebook.katana") || packageName.contains("com.twitter.android")
                    || packageName.contains("com.facebook.orca") || packageName.contains("com.whatsapp")
                    || packageName.contains("google.android.talk") || packageName.contains("com.viber") ) {
                diff_app.add(app);
                continue;
            }
            // skip these
            if (packageName.contains("com.android.bluetooth") || packageName.contains("flipboard.app")
                    || packageName.contains("com.sec.android.widgetapp.diotek.smemo") || packageName.contains("com.google.android.apps.docs")) {
                continue;
            }
            Intent targetedShareIntent = new Intent(Intent.ACTION_SEND);
            targetedShareIntent.setType("text/plain");
            targetedShareIntent.putExtra(Intent.EXTRA_TEXT, insult.getInsult() + vf_hashtag);
            targetedShareIntent.setPackage(packageName);
            targetedShareIntents.add(targetedShareIntent);
        }
    }

    // 1. show a first choice dialog to choose between Twitter, Facebook and Other
    // 2. if "Other" is chosen, show another dialog with all apps that can share (Whatsapp, Viber, Hangout...)
    public static void preChoiceMenu(final Activity a, final Insult insult) {
        final CharSequence insult_text = insult.getInsult();

        String package_name;
        Drawable icon;
        App app;
        final App[] apps = new App[diff_app.size()+1];
        PackageManager pm = a.getPackageManager();

        for (int i = 0; i < diff_app.size(); i++) {
            package_name = diff_app.get(i).activityInfo.packageName;
            icon = diff_app.get(i).loadIcon(pm);
            System.out.println("icon is "+icon);
            if (package_name.contains("com.facebook.katana")) {
                app = new App("Facebook", package_name, icon);
                apps[i] = app;
            }
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
        }

        apps[diff_app.size()] = new App(a.getString(R.string.other), "", null);

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

                        if (choice.equals("Facebook")) {
                            flurry_stats.put("Share on", "Facebook");
                            flurry_stats.put("Insult", insult_text.toString());

                            insultFriendOnFB(a, insult);
                        }

                        Intent targetedShareIntent = new Intent(Intent.ACTION_SEND);
                        targetedShareIntent.setType("text/plain");
                        targetedShareIntent.putExtra(Intent.EXTRA_TEXT, insult_text + vf_hashtag);
                        if (choice.equals("Twitter")) {
                            flurry_stats.put("Share on", "Twitter");
                            flurry_stats.put("Insult", insult_text.toString());

                            targetedShareIntent.setPackage(apps[which].getPackageName());
                            a.startActivity(targetedShareIntent);
                        }
                        if (choice.equals("Messenger")) {
                            flurry_stats.put("Share on", "Messenger");
                            flurry_stats.put("Insult", insult_text.toString());

                            targetedShareIntent.setPackage(apps[which].getPackageName());
                            a.startActivity(targetedShareIntent);
                        }
                        if (choice.equals("WhatsApp")) {
                            flurry_stats.put("Share on", "WhatsApp");
                            flurry_stats.put("Insult", insult_text.toString());

                            targetedShareIntent.setPackage(apps[which].getPackageName());
                            a.startActivity(targetedShareIntent);
                        }
                        if (choice.equals("Hangout")) {
                            flurry_stats.put("Share on", "Hangout");
                            flurry_stats.put("Insult", insult_text.toString());

                            targetedShareIntent.setPackage(apps[which].getPackageName());
                            a.startActivity(targetedShareIntent);
                        }
                        if (choice.equals("Viber")) {
                            flurry_stats.put("Share on", "Viber");
                            flurry_stats.put("Insult", insult_text.toString());

                            targetedShareIntent.setPackage(apps[which].getPackageName());
                            a.startActivity(targetedShareIntent);
                        }

                        /*if (choice.equals(a.getString(R.string.other))) {
                            flurry_stats.put("Share on", "Other");
                            flurry_stats.put("Insult", insult_text.toString());

                            Intent chooserIntent = Intent.createChooser(targetedShareIntents.remove(0), a.getString(R.string.choice2));
                            chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, targetedShareIntents.toArray(new Parcelable[]{}));
                            a.startActivity(chooserIntent);
                        }*/
                        //if ( SEND_STATS_FLURRY )
                        //    FlurryAgent.logEvent("Sharing", flurry_stats);
                    }
                }).create().show();
    }

    public static void insultFriendOnFB(final Activity a, final Insult insult) {
        ClipboardManager clipb = (ClipboardManager) a.getSystemService(Context.CLIPBOARD_SERVICE);
        clipb.setPrimaryClip(ClipData.newPlainText(a.getString(R.string.title_activity_insulto),
                insult.getInsult() + vf_hashtag));

        // Create Dialog to warn user
        //http://developer.android.com/guide/topics/ui/dialogs.html
        //http://developmentality.wordpress.com/2009/10/31/android-dialog-box-tutorial/
        AlertDialog.Builder builder = new AlertDialog.Builder(a);
        builder.setMessage(a.getString(R.string.fb_warning_message))
                .setTitle(a.getString(R.string.fb_warning_title))
                .setPositiveButton(a.getString(R.string.ok), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        postToFB(a);
                    }
                });
        // Create the AlertDialog object and return it
        builder.create().show();
    }

    public static void postToFB(final Activity a) {
        FacebookDialog shareDialog = new FacebookDialog.ShareDialogBuilder(a)
                .setApplicationName(a.getString(R.string.app_name))
                        //.setLink("http://play.google.com/store/apps/details?id=italo.vaffapp.prop.app")
                .build();
        uiHelper.trackPendingDialogCall(shareDialog.present());
    }
    /* methods used to share! END */


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


    /* Hide the textview holding the english translation of an insult
       Only when language is english
     */
    public static void hideEngTextView(TextView eng_text_view) {
        eng_text_view.setVisibility(View.GONE);
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
