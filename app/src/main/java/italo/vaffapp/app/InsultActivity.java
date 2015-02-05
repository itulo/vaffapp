package italo.vaffapp.app;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ArgbEvaluator;
import android.app.AlarmManager;
import android.graphics.Typeface;
import android.os.SystemClock;
import android.support.v7.app.ActionBarActivity;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.content.Intent;

import italo.vaffapp.app.databases.DatabaseHandler;
import italo.vaffapp.app.databases.Insult;
import italo.vaffapp.app.util.SharedMethods;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import com.facebook.*;
import com.facebook.widget.*;
import com.facebook.AppEventsLogger;
import android.content.ClipboardManager;
import android.content.ClipData;
import android.app.AlertDialog;
import android.content.DialogInterface;

import java.util.List;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Parcelable;

import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.ConnectionResult;
import com.vungle.publisher.VunglePub;
import com.vungle.publisher.AdConfig;
import com.vungle.publisher.Orientation;

import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;
import android.app.PendingIntent;
import android.content.Context;
import android.widget.Toast;

import com.flurry.android.FlurryAgent;

import android.animation.ObjectAnimator;
import android.graphics.Color;


public class InsultActivity extends ActionBarActivity {
    private static ArrayList<Insult> insults = null;
    private UiLifecycleHelper uiHelper;
    private Session.StatusCallback callback = null;
    private TextView insult;
    private TextView insult_desc;
    private TextView insult_eng;
    private String region;
    private String DEFAULT_ENG = "Not translatable";

    private static int rand_index;
    private static byte[] occurrences;
    private static short generated_n = 0;

    // for condividi
    private List<Intent> targetedShareIntents;
    private List<String> diff_app;
    private Intent sharingIntent;

    private Speaker speaker;

    final VunglePub vunglePub = VunglePub.getInstance();
    private short time_for_ad_1 = 30;

    private boolean SEND_STATS_FLURRY = true;
    private static short pronunciated_n = 0;

    private int pref_language;

    private static String link = "http://adf.ly/ssss4";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_insult);
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                .add(R.id.container, new PlaceholderFragment())
                .commit();
        }

        // FB code, UiLifecycleHelper needed to share a post - https://developers.facebook.com/docs/android/share
        // Includes callback in case FB app is not installed!
        // 1. configure the UiLifecycleHelper in onCreate
        uiHelper = new UiLifecycleHelper(this, callback);
        uiHelper.onCreate(savedInstanceState);

        // https://github.com/Vungle/vungle-resources/blob/master/English/Android/3.2.x/android-dev-guide.md
        vunglePub.init(this, "italo.vaffapp.app");

        Intent mIntent = getIntent();
        pref_language = mIntent.getIntExtra("pref_language", 0);
    }

    // 2. configure a callback handler that's invoked when the share dialog closes and control returns to the calling app
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        uiHelper.onActivityResult(requestCode, resultCode, data);
    }

    // 3. configure other methods on uiHelper to handle Activity lifecycle callbacks correctly
    @Override
    protected void onResume() {
        super.onResume();
        uiHelper.onResume();
        vunglePub.onResume();
        setRegionNameInTitle();
        AppEventsLogger.activateApp(this);  // to track in FB
        checkGooglePlayServicesVersion();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        uiHelper.onSaveInstanceState(outState);
    }

    @Override
    public void onPause() {
        super.onPause();
        uiHelper.onPause();
        vunglePub.onPause();
        speaker.onPause();
        scheduleNotification();
        AppEventsLogger.deactivateApp(this);    // to track in FB
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        uiHelper.onDestroy();
    }

    public void onStop(){
        super.onStop();
        // Flurry send how many insults generated
        Map<String, String> flurry_stats = new HashMap<String, String>();
        flurry_stats.put("Amount Insults generated", String.valueOf(generated_n));
        flurry_stats.put("Amount insults pronunciated", String.valueOf(pronunciated_n));
        // send stats if number of generated insults is >= 10
        if ( SEND_STATS_FLURRY && generated_n >= 10 )
            FlurryAgent.logEvent("onStop()", flurry_stats);

        FlurryAgent.onEndSession(this);
    }

    public void onStart(){
        super.onStart();

        //initialize TextToSpeech objects in Speaker
        speaker = new Speaker(getApplicationContext());

        FlurryAgent.onStartSession(this, getString(R.string.flurry_id));

        if ( insults == null ) {
            nextInsult();
            Toast.makeText(this, insults.size()+" "+getString(R.string.n_insulti), Toast.LENGTH_SHORT).show();
        } else{
            getTextviews();
            setTextviews();
        }
        if ( pref_language == LanguageOptions.ITALIANO )
            hideEngTextView();
    }

    // if the UI is in italian, don't show the TextView for the english translation of an insult
    public void hideEngTextView(){
        insult_eng.setVisibility(View.GONE);
    }

    public void checkGooglePlayServicesVersion(){
        int REQUEST_CODE_RECOVER_PLAY_SERVICES = 1001;

        int gps_ver = GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);
        if ( gps_ver != ConnectionResult.SUCCESS )
            GooglePlayServicesUtil.getErrorDialog(gps_ver, this, REQUEST_CODE_RECOVER_PLAY_SERVICES).show();
    }

    public void getTextviews(){
        if ( insult == null )
            insult = (TextView)findViewById(R.id.insult);
        if( insult_desc == null )
            insult_desc = (TextView)findViewById(R.id.insult_desc);
        if ( insult_eng == null)
            insult_eng = (TextView)findViewById(R.id.insult_eng);
    }

    public void setTextviews(){
        if ( insult!=null && insult_desc!=null && insult_eng!=null) {
            insult.setText(insults.get(rand_index).getInsult());
            insult_desc.setText(insults.get(rand_index).getDesc());

            if ( pref_language == LanguageOptions.ENGLISH ) {
                String eng = insults.get(rand_index).getEnglish();
                if (eng.equals("")) {
                    eng = DEFAULT_ENG;
                    insult_eng.setTypeface(null, Typeface.ITALIC);
                } else
                    insult_eng.setTypeface(null, Typeface.NORMAL);
                insult_eng.setText(eng);
            }
        }
    }

    public void speakInsult(View v){
        pronunciated_n++;
        speaker.speakInsult(insult.getText().toString());

        /* This is a trick: copy the insult with description, translation and region)
           to keep sharing on Facebook as before */
        String share;
        ClipboardManager clipb = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
        share = insult.getText()+"\n"+insult_desc.getText()+"\n";
        if (pref_language == LanguageOptions.ENGLISH && !insult_eng.getText().toString().equals(DEFAULT_ENG))
            share += insult_eng.getText()+"\n";
        share += "("+region+")";
        clipb.setPrimaryClip(ClipData.newPlainText(getString(R.string.title_activity_insulto), share));
    }

    public void speakDesc(View v){
        pronunciated_n++;
        speaker.speakInsult(insult_desc.getText().toString());
    }

    public void speakEng(View v){
        pronunciated_n++;
        speaker.speakEnglish(insult_eng.getText().toString());
    }

    /* fade away text, get new insult, fade in text*/
    public void showInsult(View v){
        if (insult != null && insult_desc != null && insult_eng != null){
            AnimatorSet fadeaway = new AnimatorSet();
            fadeaway.playTogether(ObjectAnimator.ofObject(insult, "textColor", new ArgbEvaluator(), Color.GRAY, Color.WHITE),
                            ObjectAnimator.ofObject(insult_desc, "textColor", new ArgbEvaluator(), Color.GRAY, Color.WHITE),
                            ObjectAnimator.ofObject(insult_eng, "textColor", new ArgbEvaluator(), Color.GRAY, Color.WHITE));
            fadeaway.setDuration(100);
            fadeaway.addListener(new AnimatorListenerAdapter() {
                                 public void onAnimationEnd(Animator animation) {
                                     nextInsult();

                                     AnimatorSet fadein = new AnimatorSet();
                                     fadein.playTogether(ObjectAnimator.ofObject(insult, "textColor", new ArgbEvaluator(), Color.WHITE, Color.GRAY),
                                             ObjectAnimator.ofObject(insult_desc, "textColor", new ArgbEvaluator(), Color.WHITE, Color.GRAY),
                                             ObjectAnimator.ofObject(insult_eng, "textColor", new ArgbEvaluator(), Color.WHITE, Color.GRAY));
                                     fadein.setDuration(100);
                                     fadein.start();
                                 }
                             });
            fadeaway.start();
            }
        }

    /* nextInsult
    1. load insults if not loaded yet
    2. generate a random number to show the insult
       there is an array that keeps all generated numbers so far
       if generated a number which was chosen already for 10 times, get the first available
    3. set insult in the view
    4. checks if array of generated numbers is full (all possible numbers have been generated)
       if yes reinitialize and start from scratch
     */
    public void nextInsult(){

        if (insults == null)
            loadInsults();

        rand_index = generateRandomIdx();

        getTextviews();
        setTextviews();
        setRegionNameInTitle();

        occurrences[rand_index] = 1;
        generated_n++;
        if ( generated_n == occurrences.length ) {
            //reinitialize occurrences
            for(int i=0;i<occurrences.length;i++)
                occurrences[i] = 0;
            generated_n = 0;
        }

        if ( generated_n == time_for_ad_1 ){
            if ( vunglePub.isCachedAdAvailable() ) {
                final AdConfig overrideConfig = new AdConfig();
                overrideConfig.setOrientation(Orientation.autoRotate);
                overrideConfig.setSoundEnabled(false);
                vunglePub.playAd(overrideConfig);
            }
            else {
                time_for_ad_1++;
            }
        }
    }

    // generate a random index that hasn't been generated recently
    // writes the idx in the global variable rand_index
    // this is used to get an insult from insults (the ArrayList)
    public int generateRandomIdx(){
        int tmp_ind;
        short retry = 0;
        short MAX_RETRIES = 10;

        Random rand = new Random();
        tmp_ind = rand.nextInt(insults.size());
        while ( occurrences[tmp_ind] == 1 && retry < MAX_RETRIES) {
            tmp_ind = rand.nextInt(insults.size());
            retry++;
        }

        if (retry == MAX_RETRIES){
            for(int i=0;i<occurrences.length;i++){
                if (occurrences[i]==0){
                    // don't set occurrences[i]=1 here, it occurs in showInsult()
                    tmp_ind = i;
                    break;
                }
            }
        }
        return tmp_ind;
    }

    public void setRegionNameInTitle(){
        region = getRegionFromId(insults.get(rand_index).getRegionId());
        getSupportActionBar().setTitle(region);
    }

    public void loadInsults(){
        DatabaseHandler db = new DatabaseHandler(this);
        db.openDataBase();
        insults = db.getAllInsults();
        db.close();

        occurrences = new byte[insults.size()];
    }

    public String getRegionFromId(int region_id){
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
            case 6: region = "Emilia-Romagna"; break;
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
            default: region = ""; break;
        }

        return region;
    }

    public void postToFB() {
        FacebookDialog shareDialog = new FacebookDialog.ShareDialogBuilder(this)
            .setApplicationName(getString(R.string.app_name))
            //.setLink("http://play.google.com/store/apps/details?id=italo.vaffapp.app")
            .build();
        uiHelper.trackPendingDialogCall(shareDialog.present());
    }

    public void insultFriendOnFB() {
        if (insult == null ) {
            return;
        } else {
            ClipboardManager clipb = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
            clipb.setPrimaryClip(ClipData.newPlainText(getString(R.string.title_activity_insulto),
                    insult.getText()+" #vaffapp"));
        }

        // Create Dialog to warn user
        //http://developer.android.com/guide/topics/ui/dialogs.html
        //http://developmentality.wordpress.com/2009/10/31/android-dialog-box-tutorial/
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(getString(R.string.fb_warning_message))
                .setTitle(getString(R.string.fb_warning_title))
                .setPositiveButton(getString(R.string.ok), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        postToFB();
                    }
                });
        // Create the AlertDialog object and return it
        builder.create().show();
    }

    // check for presence of Facebook, Messenger, Twitter and WhatsApp apps (these will be treated differently)
    public void checkPresenceOfApps(View view){
        targetedShareIntents = new ArrayList<Intent>();
        diff_app = new ArrayList<String>();
        sharingIntent = new Intent(Intent.ACTION_SEND);

        sharingIntent.setType("text/plain");
        PackageManager pm = view.getContext().getPackageManager();
        List<ResolveInfo> activityList = pm.queryIntentActivities(sharingIntent, 0);

        for(final ResolveInfo app : activityList) {
            String packageName = app.activityInfo.packageName;
            // facebook.katana is FB app, facebook.orca is the messenger
            if ( packageName.contains("com.facebook.katana") || packageName.contains("com.twitter.android")
                    || packageName.contains("com.facebook.orca") || packageName.contains("com.whatsapp")
                    || packageName.contains("google.android.talk") || packageName.contains("com.viber") ){
                diff_app.add(packageName);

                Intent targetedShareIntent = new Intent(Intent.ACTION_SEND);
                targetedShareIntent.setType("text/plain");
                targetedShareIntent.putExtra(Intent.EXTRA_TEXT, insult.getText()+" #vaffapp");
                targetedShareIntent.setPackage(packageName);
                targetedShareIntents.add(targetedShareIntent);

                continue;
            }
            // skip all others
            else
                continue;
        }
    }

    // 1. show a choice dialog to choose between Twitter, Facebook
    public void preChoiceMenu(){
        // I have to declare this an array, only this way I can modify it later (final statement is necessary)
        final String[] packageNames = {
            "twitter",
            "messenger",
            "whatsapp",
            "hangout",
            "viber"
        };
        final CharSequence[] items = new CharSequence[diff_app.size()];
        for (int i=0; i<diff_app.size();i++) {
            if (diff_app.get(i).contains("com.facebook.katana"))
                items[i] = "Facebook";
            if (diff_app.get(i).contains("com.twitter.android")) {
                items[i] = "Twitter";
                packageNames[0] = diff_app.get(i);
            }
            if (diff_app.get(i).contains("com.facebook.orca")) {
                items[i] = "Messenger";
                packageNames[1] = diff_app.get(i);
            }
            if (diff_app.get(i).contains("com.whatsapp")) {
                items[i] = "WhatsApp";
                packageNames[2] = diff_app.get(i);
            }
            if (diff_app.get(i).contains("google.android.talk")) {
                items[i] = "Hangout";
                packageNames[3] = diff_app.get(i);
            }
            if (diff_app.get(i).contains("com.viber")) {
                items[i] = "Viber";
                packageNames[4] = diff_app.get(i);
            }
        }
        new AlertDialog.Builder(this)
            .setTitle(getString(R.string.choice1))
            .setItems(items, new DialogInterface.OnClickListener()
                {
                @Override
                public void onClick(DialogInterface dialog, int which)
                {
                    String choice = items[which].toString();
                    // Flurry analytics
                    Map<String, String> flurry_stats = new HashMap<String, String>();

                    if ( choice.equals("Facebook") ){
                        flurry_stats.put("Share on", "Facebook");
                        flurry_stats.put("Insult", insult.getText().toString());

                        insultFriendOnFB();
                    }

                    Intent targetedShareIntent = new Intent(Intent.ACTION_SEND);
                    targetedShareIntent.setType("text/plain");
                    targetedShareIntent.putExtra(Intent.EXTRA_TEXT, insult.getText()+" #vaffapp\n\n--"+link);
                    if ( choice.equals("Twitter") ){
                        flurry_stats.put("Share on", "Twitter");
                        flurry_stats.put("Insult", insult.getText().toString());
                        // can't share a link on twitter
                        targetedShareIntent.putExtra(Intent.EXTRA_TEXT, insult.getText()+" #vaffapp");
                        targetedShareIntent.setPackage(packageNames[0]);
                        startActivity(targetedShareIntent);
                    }
                    if (choice.equals("Messenger")) {
                        flurry_stats.put("Share on", "Messenger");
                        flurry_stats.put("Insult", insult.getText().toString());

                        targetedShareIntent.setPackage(packageNames[1]);
                        startActivity(targetedShareIntent);
                    }
                    if (choice.equals("WhatsApp")) {
                        flurry_stats.put("Share on", "WhatsApp");
                        flurry_stats.put("Insult", insult.getText().toString());

                        targetedShareIntent.setPackage(packageNames[2]);
                        startActivity(targetedShareIntent);
                    }
                    if (choice.equals("Hangout")) {
                        flurry_stats.put("Share on", "Hangout");
                        flurry_stats.put("Insult", insult.getText().toString());

                        targetedShareIntent.setPackage(packageNames[3]);
                        startActivity(targetedShareIntent);
                    }
                    if (choice.equals("Viber")) {
                        flurry_stats.put("Share on", "Viber");
                        flurry_stats.put("Insult", insult.getText().toString());

                        targetedShareIntent.setPackage(packageNames[4]);
                        startActivity(targetedShareIntent);
                    }

                    if ( SEND_STATS_FLURRY )
                        FlurryAgent.logEvent("Sharing", flurry_stats);
                }
            }).create().show();
    }

    // Checks presence of apps Facebook and Twitter
    // if not, shows a dialog box with all apps able to receive the insult
    public void condividi(View view){
        checkPresenceOfApps(view);

        if ( diff_app.size() > 0 ){
            preChoiceMenu();
        }
        else {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setMessage(getString(R.string.noapp_warning_message))
                    .setTitle(getString(R.string.noapp_warning_title))
                    .setPositiveButton(getString(R.string.ok), new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            //do nothing
                        }
                    });
            // Create the AlertDialog object and return it
            builder.create().show();
        }
    }

    // Notification stuff
    // solution by https://gist.github.com/BrandonSmith/6679223
    // this will show a notification after 2 days (notification is deleted if the phone is rebooted)
    private void scheduleNotification(){
        // notification in two days time
        int DELAY = 24*60*60*1000;
        int insult_idx = generateRandomIdx();

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this);
        builder.setContentTitle("");
        builder.setContentText(insults.get(insult_idx).getInsult());
        builder.setSmallIcon(R.drawable.ic_launcher);
        builder.setAutoCancel(true);

        // what the notification has to do when clicked upon (open insult activity)
        Intent resultIntent = new Intent(this, InsultActivity.class);
        // The stack builder object will contain an artificial back stack for the started Activity.
        // This ensures that navigating backward from the Activity leads out of
        // your application to the Home screen.
        TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);
        stackBuilder.addParentStack(InsultActivity.class);
        stackBuilder.addNextIntent(resultIntent);
        PendingIntent resultPendingIntent = stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);
        builder.setContentIntent(resultPendingIntent);

        Intent notificationIntent = new Intent(this, NotificationPublisher.class);
        notificationIntent.putExtra(NotificationPublisher.NOTIFICATION_ID, 1);
        notificationIntent.putExtra(NotificationPublisher.NOTIFICATION, builder.build());
        PendingIntent pendingIntent = PendingIntent.getBroadcast(this, 0, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        long futureInMillis = SystemClock.elapsedRealtime() + DELAY;
        AlarmManager alarmManager = (AlarmManager)getSystemService(Context.ALARM_SERVICE);
        alarmManager.set(AlarmManager.ELAPSED_REALTIME_WAKEUP, futureInMillis, pendingIntent);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        
        // Inflate the menu; this adds items to the action bar if it is present.
        // comment so it doesn't show 'Settings'
        //getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * A placeholder fragment containing a simple view.
     */
    public static class PlaceholderFragment extends Fragment {

        public PlaceholderFragment() {
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                Bundle savedInstanceState) {
            View rootView = inflater.inflate(R.layout.fragment_main2, container, false);
            return rootView;
        }
    }
}
