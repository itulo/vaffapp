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

import italo.vaffapp.app.databases.Insult;
import italo.vaffapp.app.util.SharedMethods;
import italo.vaffapp.app.util.SharedPrefsMethods;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import com.facebook.AppEventsLogger;
import android.content.ClipboardManager;
import android.content.ClipData;

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

import android.animation.ObjectAnimator;
import android.graphics.Color;


public class InsultActivity extends ActionBarActivity {
    private static ArrayList<Insult> insults = null;
    private TextView insult;
    private TextView insult_desc;
    private TextView insult_eng;
    private String region;
    private String DEFAULT_ENG = "Not translatable";

    private static int rand_index;
    private static byte[] occurrences;
    private static short generated_n = 0;

    private static VunglePub vunglePub = null;
    private short time_for_ad_1 = 30;

    private static short pronunciated_n = 0;

    private int pref_language;

    private int shared_insults; // # of times a person shares an insult

    static ArrayList<String> notification_titles = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_insult);
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                .add(R.id.container, new PlaceholderFragment())
                .commit();
        }

        SharedMethods.onCreate(this, savedInstanceState);

        if ( vunglePub == null ) {
            vunglePub = VunglePub.getInstance();
            // https://github.com/Vungle/vungle-resources/blob/master/English/Android/3.2.x/android-dev-guide.md
            vunglePub.init(this, "italo.vaffapp.app");
        }

        Intent mIntent = getIntent();
        pref_language = mIntent.getIntExtra("pref_language", 0);

        SharedPrefsMethods.setupSharedPrefsMethods(this);
        shared_insults = SharedPrefsMethods.getInt("shared_insults", 0);

        showInstructionsIfFirstTime();
    }

    // 2. configure a callback handler that's invoked when the share dialog closes and control returns to the calling app
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        SharedMethods.onActivityResult(requestCode, resultCode, data);

        // when a user shares and then the program returns to the VaffApp
        if (requestCode == SharedMethods.SHARE_REQUEST) {
            // I have to comment the instruction below, it works only for Twitter
            // all the others app return always RESULT_CANCELLED 0 or RESULT_OK -1 (Facebook)
            //if (resultCode == RESULT_OK) {
            increaseSharedInsult();
        }
    }

    // 3. configure other methods on uiHelper to handle Activity lifecycle callbacks correctly
    @Override
    protected void onResume() {
        super.onResume();
        SharedMethods.onResume();
        vunglePub.onResume();
        setRegionNameInTitle();
        AppEventsLogger.activateApp(this);  // to track in FB
        checkGooglePlayServicesVersion();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        SharedMethods.onSaveInstanceState(outState);
    }

    @Override
    public void onPause() {
        super.onPause();
        SharedMethods.onPause();
        vunglePub.onPause();
        scheduleNotification();
        AppEventsLogger.deactivateApp(this);    // to track in FB
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        SharedMethods.onDestroy();
    }

    public void onStop(){
        super.onStop();
        // Flurry send how many insults generated
        Map<String, String> flurry_stats = new HashMap<String, String>();
        flurry_stats.put("Amount Insults generated", String.valueOf(generated_n));
        flurry_stats.put("Amount insults pronunciated", String.valueOf(pronunciated_n));
        // send stats if number of generated insults is >= 10
        if ( generated_n >= 10 )
            SharedMethods.sendFlurry("onStop()", flurry_stats);

        SharedMethods.onStop(this);
    }

    public void onStart(){
        super.onStart();
        SharedMethods.onStart(getApplicationContext());

        if ( insults == null ) {
            nextInsult();
            Toast.makeText(this, insults.size()+" "+getString(R.string.n_insulti), Toast.LENGTH_SHORT).show();
        } else{
            getTextviews();
            setTextviews();
        }
        if ( pref_language == LanguageOptions.ITALIANO )
            SharedMethods.hideEngTextView(insult_eng);
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
        SharedMethods.speakInsult(insult.getText().toString());

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
        SharedMethods.speakDesc(insult_desc.getText().toString());
    }

    public void speakEng(View v){
        pronunciated_n++;
        SharedMethods.speakEnglish(insult_eng.getText().toString());
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
            if ( vunglePub.isAdPlayable() ) {
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
        region = SharedMethods.getRegionFromId(insults.get(rand_index).getRegionId());
        getSupportActionBar().setTitle(region);
    }

    public void loadInsults(){
        insults = SharedMethods.loadInsults(this);
        occurrences = new byte[insults.size()];
    }


    public void share(View view){
        // Checks presence of apps Facebook, Messenger, Twitter, Hangout, Whatsapp and Viber
        // if not, shows a dialog box with all apps able to receive the insult
        SharedMethods.share(this, insults.get(rand_index));
    }

    public void increaseSharedInsult(){
        shared_insults++;

        SharedMethods.checkSharedInsults(this, getString(R.string.share_reward), shared_insults);

        SharedPrefsMethods.putInt("shared_insults", shared_insults);
    }

    // Notification stuff
    // solution by https://gist.github.com/BrandonSmith/6679223
    // this will show a notification after 2 days (notification is deleted if the phone is rebooted)
    private void scheduleNotification(){
        if ( notification_titles == null ) {
            notification_titles = new ArrayList<String>();
            notification_titles.add(getString(R.string.notif_title_1));
            notification_titles.add(getString(R.string.notif_title_2));
            notification_titles.add(getString(R.string.notif_title_3));
        }
        // notification in two days time
        int DELAY = 24*60*60*1000;
        int insult_idx = generateRandomIdx();
        Random rand = new Random();
        String title = notification_titles.get(rand.nextInt(notification_titles.size()));

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this);
        builder.setContentTitle(title);
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

    private void showInstructionsIfFirstTime(){
        SharedPrefsMethods.setupSharedPrefsMethods(this);
        int instructions_showed = SharedPrefsMethods.getInt("instruction_showed", 0);

        if ( instructions_showed == 0 ){
            SharedMethods.showDialog(this, getString(R.string.share_instructions_title), getString(R.string.share_instructions_msg));
            SharedPrefsMethods.putInt("instruction_showed", 1);
        }
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
