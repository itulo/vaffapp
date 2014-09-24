package italo.vaffapp.app;

import android.app.AlarmManager;
import android.app.Notification;
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import com.facebook.*;
import com.facebook.widget.*;
import android.content.ClipboardManager;
import android.content.ClipData;
import android.app.AlertDialog;
import android.content.DialogInterface;

import java.util.List;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Parcelable;

//import com.appnext.appnextsdk.Appnext;

import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.ConnectionResult;
import com.jirbo.adcolony.*;


import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationCompat.Builder;
import android.support.v4.app.TaskStackBuilder;
import android.app.PendingIntent;
import android.app.NotificationManager;
import android.content.Context;

import com.flurry.android.FlurryAgent;


public class InsultActivity extends ActionBarActivity {
    private static ArrayList<Insult> insults = null;
    private UiLifecycleHelper uiHelper;
    private Session.StatusCallback callback = null;
    private TextView insult;
    private TextView insult_desc;
    private String region;

    private static int rand_index;
    private static byte[] occurrences;
    private static short generated_n = 0;

    // for condividi
    private List<Intent> targetedShareIntents;
    private List<String> diff_app;
    private Intent sharingIntent;

    //private Appnext appnext;

    private Speaker speaker;

    private AdColonyVideoAd adcolonyad;
    private short time_for_ad_1 = 30;
    private short time_for_ad_2 = 90;

    private boolean SEND_STATS_FLURRY = false;
    private static short pronunciated_n = 0;

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

        // https://github.com/AdColony/AdColony-Android-SDK/wiki/API-Details#configure-activity-activity-string-client_options-string-app_id-string-zone_ids-
        AdColony.configure(this, "version:3.0,store:google", "app916d076c2a05451fb5", "vzad48f059dc8d48b8af");
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
        AdColony.resume(this);
        adcolonyad = new AdColonyVideoAd();
        setRegionNameInTitle();
        checkGooglePlayServicesVersion();

        scheduleNotification();
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
        AdColony.pause();
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
        if ( SEND_STATS_FLURRY && (generated_n%10 == 0 || pronunciated_n%10 == 0) )
            FlurryAgent.logEvent("onStop()", flurry_stats);

        FlurryAgent.onEndSession(this);
    }

    public void onStart(){
        super.onStart();

        //initialize TextToSpeech objects in Speaker
        speaker = new Speaker(getApplicationContext());

        FlurryAgent.onStartSession(this, "CTMK9MZJN48KNVB3JH5V");

        //appnext = new Appnext(this);
        //appnext.setAppID("a813fa77-433c-4b51-87bb-d6f7b34b4246");

        if ( insults == null ) {
            showInsult(null);
        } else{
            getTextviews();
            setTextviews();
        }
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
    }

    public void setTextviews(){
        if ( insult!=null && insult_desc!=null ) {
            insult.setText(insults.get(rand_index).getInsult());
            insult_desc.setText(insults.get(rand_index).getDesc());
        }
    }

    public void speakInsult(View v){
        pronunciated_n++;
        speaker.speakInsult(insult.getText().toString());
    }

    public void speakDesc(View v){
        pronunciated_n++;
        speaker.speakDesc(insult_desc.getText().toString());
    }

    /* showInsults
    1. load insults if not loaded yet
    2. generate a random number to show the insult
       there is an array that keeps all generated numbers so far
       if generated a number which was chosen already for 10 times, get the first available
    3. set insult in the view
    4. checks if array of generated numbers is full (all possible numbers have been generated)
       if yes reinitialize and start from scratch
     */
    public void showInsult(View view){
        //short retry = 0;
        //short MAX_RETRIES = 10;

        if (insults == null)
            loadInsults();

        /*Random rand = new Random();
        rand_index = rand.nextInt(insults.size());
        while ( occurrences[rand_index] == 1 || retry == MAX_RETRIES) {
            rand_index = rand.nextInt(insults.size());
            retry++;
        }

        if (retry == MAX_RETRIES){
            for(int i=0;i<occurrences.length;i++){
                if (occurrences[i]==0){
                    rand_index = i;
                    break;
                }
            }
        }*/

        generateRandomIdx();

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

        if ( generated_n == time_for_ad_1 || generated_n == time_for_ad_2 ){
            //appnext.addMoreAppsLeft("961d922f-d94d-4d08-a060-ea2d78dd6d20");
            //appnext.showBubble();
            if ( adcolonyad.isReady() ) {
                adcolonyad.show();
            }
            else {
                time_for_ad_1++;
                time_for_ad_2++;
            }
        }
    }

    // generate a random index that hasn't been generated recently
    // writes the idx in the global variable rand_index
    // this is used to get an insult from insults (the ArrayList)
    public void generateRandomIdx(){
        short retry = 0;
        short MAX_RETRIES = 10;

        Random rand = new Random();
        rand_index = rand.nextInt(insults.size());
        while ( occurrences[rand_index] == 1 || retry == MAX_RETRIES) {
            rand_index = rand.nextInt(insults.size());
            retry++;
        }

        if (retry == MAX_RETRIES){
            for(int i=0;i<occurrences.length;i++){
                if (occurrences[i]==0){
                    rand_index = i;
                    break;
                }
            }
        }
    }

    public void setRegionNameInTitle(){
        region = "("+getRegionFromId(insults.get(rand_index).getRegionId())+")";
        getSupportActionBar().setTitle(getString(R.string.title_activity_insulto)+" "+region);
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
            case 1: region = "Molise"; break;
            case 2: region = "Valle d'Aosta"; break;
            case 3: region = "Piemonte"; break;
            case 4: region = "Lombardia"; break;
            case 5: region = "Trentino Alto Adige"; break;
            case 6: region = "Friuli Venezia Giulia"; break;
            case 7: region = "Veneto"; break;
            case 8: region = "Emilia-Romagna"; break;
            case 9: region = "Liguria"; break;
            case 10: region = "Toscana"; break;
            case 11: region = "Lazio"; break;
            case 12: region = "Umbria"; break;
            case 13: region = "Marche"; break;
            case 14: region = "Abruzzo"; break;
            case 15: region = "Campania"; break;
            case 16: region = "Puglia"; break;
            case 17: region = "Basilicata"; break;
            case 18: region = "Calabria"; break;
            case 19: region = "Sicilia"; break;
            case 20: region = "Sardegna"; break;
            default: region = ""; break;
        }

        return region;
    }

    public void postToFB() {
        FacebookDialog shareDialog = new FacebookDialog.ShareDialogBuilder(this)
            .setApplicationName("VaffApp")
            .setLink("http://play.google.com/store/apps/details?id=italo.vaffapp.app")
            .build();
        uiHelper.trackPendingDialogCall(shareDialog.present());
    }

    public void insultFriendOnFB() {
        if (insult == null ) {
            return;
        } else {
            ClipboardManager clipb = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
            clipb.setPrimaryClip(ClipData.newPlainText(getString(R.string.title_activity_insulto),
                    insult.getText()+" -\n"+insult_desc.getText()+"\n"+region));
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
                    || packageName.contains("com.facebook.orca") || packageName.contains("com.whatsapp") ){
                diff_app.add(packageName);
                continue;
            }
            // skip these
            if ( packageName.contains("com.android.bluetooth") || packageName.contains("flipboard.app")
                    || packageName.contains("com.sec.android.widgetapp.diotek.smemo") || packageName.contains("com.google.android.apps.docs") ){
                continue;
            }
            Intent targetedShareIntent = new Intent(Intent.ACTION_SEND);
            targetedShareIntent.setType("text/plain");
            targetedShareIntent.putExtra(Intent.EXTRA_TEXT, insult.getText()+" #vaffapp");
            targetedShareIntent.setPackage(packageName);
            targetedShareIntents.add(targetedShareIntent);
        }
    }

    // 1. show a first choice dialog to choose between Twitter, Facebook and Other
    // 2. if "Other" is chosen, show another dialog with all apps that can share (Whatsapp, Viber, Hangout...)
    public void preChoiceMenu(){
        // I have to declare this an array, only this way I can modify it later (final statement is necessary)
        final String[] packageNames = {
                "twitter",
                "messenger",
                "whatsapp"
        };
        final CharSequence[] items = new CharSequence[diff_app.size()+1];
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
        }
        items[diff_app.size()] = getString(R.string.other);
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
                        targetedShareIntent.putExtra(Intent.EXTRA_TEXT, insult.getText()+" #vaffapp");
                        if ( choice.equals("Twitter") ){
                            flurry_stats.put("Share on", "Twitter");
                            flurry_stats.put("Insult", insult.getText().toString());

                            targetedShareIntent.setPackage(packageNames[0]);
                            startActivity(targetedShareIntent);
                        }
                        if ( choice.equals("Messenger") ){
                            flurry_stats.put("Share on", "Messenger");
                            flurry_stats.put("Insult", insult.getText().toString());

                            targetedShareIntent.setPackage(packageNames[1]);
                            startActivity(targetedShareIntent);
                        }
                        if ( choice.equals("WhatsApp") ){
                            flurry_stats.put("Share on", "WhatsApp");
                            flurry_stats.put("Insult", insult.getText().toString());

                            targetedShareIntent.setPackage(packageNames[2]);
                            startActivity(targetedShareIntent);
                        }

                        if (choice.equals(getString(R.string.other)) ) {
                            flurry_stats.put("Share on", "Other");
                            flurry_stats.put("Insult", insult.getText().toString());

                            Intent chooserIntent = Intent.createChooser(targetedShareIntents.remove(0), getString(R.string.choice2));
                            chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, targetedShareIntents.toArray(new Parcelable[]{}));
                            startActivity(chooserIntent);
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
            sharingIntent.putExtra(Intent.EXTRA_TEXT, insult.getText()+" #vaffapp");
            startActivity(Intent.createChooser(sharingIntent, getString(R.string.choice1)));
        }
    }

    /*public void setupNotification(){
        generateRandomIdx();

        NotificationCompat.Builder mBuilder =
            new NotificationCompat.Builder(this)
            .setSmallIcon(R.drawable.ic_launcher)
            .setContentTitle("")
            .setContentText(insults.get(rand_index).getInsult())
            .setAutoCancel(true);           // cancel when user clicks on notification
        // Creates an explicit intent for an Activity in your app
        Intent resultIntent = new Intent(this, InsultActivity.class);

        //schedules the intent
        //resultIntent.putExtra("EVENT_ALERT_DAYS", );
        //resultIntent.putExtra("EVENT_ALERT_TIME", "2:28");

        // The stack builder object will contain an artificial back stack for the
        // started Activity.
        // This ensures that navigating backward from the Activity leads out of
        // your application to the Home screen.
        TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);
        // Adds the back stack for the Intent (but not the Intent itself)
        stackBuilder.addParentStack(InsultActivity.class);
        // Adds the Intent that starts the Activity to the top of the stack
        stackBuilder.addNextIntent(resultIntent);
        PendingIntent resultPendingIntent = stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);
        mBuilder.setContentIntent(resultPendingIntent);
        NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        // mId allows you to update the notification later on.
        // set mId = 1 because it's the only notification
        //mNotificationManager.notify(1, mBuilder.build());

        // schedule with AlarmManager
        AlarmManager mgr = (AlarmManager)getSystemService(Context.ALARM_SERVICE);

        PendingIntent nPendingIntent = stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);
    }*/

    // Notification stuff
    // solution by https://gist.github.com/BrandonSmith/6679223
    private void scheduleNotification(){
        short DELAY = 5000;
        generateRandomIdx();
        System.out.println("scheduled");

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this);
        builder.setContentTitle("");
        builder.setContentText(insults.get(rand_index).getInsult());
        builder.setSmallIcon(R.drawable.ic_launcher);
        builder.setAutoCancel(true);

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
