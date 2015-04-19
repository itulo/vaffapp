package italo.vaffapp.app;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.pm.PackageInfo;
import android.net.Uri;
import android.support.v7.app.ActionBarActivity;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.content.Intent;

import java.util.ArrayList;
import java.util.Locale;

import android.content.res.Configuration;
import android.app.PendingIntent;
import android.app.AlarmManager;
import android.content.Context;
import android.widget.Button;

import com.flurry.android.FlurryAgent;

import italo.vaffapp.app.databases.DatabaseHandler;
import italo.vaffapp.app.databases.Insult;
import italo.vaffapp.app.util.SharedMethods;
import italo.vaffapp.app.util.SharedPrefsMethods;

import java.util.Calendar;

import italo.vaffapp.app.util.IabHelper;
import italo.vaffapp.app.util.IabResult;
import android.widget.Toast;


public class MainActivity extends ActionBarActivity {

    private final int UNBLOCK_INSULTS = 10; // insults to unblock when returning user
    private int pref_language;
    // in app billing
    IabHelper mHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                .add(R.id.container, new PlaceholderFragment())
                .commit();
        }

        SharedMethods.setIconInActionBar(this);
        SharedPrefsMethods.setupSharedPrefsMethods(this);
        new SimpleEula(this).show();
        setLanguage();
        RewardIfReturn();

        /* in app billing */
        String base64EncodedPublicKey = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAuVoijs7LeeIM18nmYa2gX6iqFGRtzbPJxlsqVJFYjwceyK729qQcNRW0lZQKlvNSO2B5leFqwb86nB3LT57COBjpDvoKUHYDAKdM7RBqCxI2WyD47R+dHROokVwjHJlmo/H3gDCkdZF2idV4HAInWYUC8WwVgG5xv6jqVRp01hRuiSdadtjK+oUAIJPYsm690I+lzJcQTmcdXisC6k/yKXw+OsTRrwhudXeHFAVlBZxS/YteaI7rjgJjPCkheRA5iBdgK75925K4g1w/jNOuuZwkGoxCQjUxmFGSc8EwaKbLrfuJlg715RSQuQT6v/xs+/rDDMvifgFSsxJGO+Sd9QIDAQAB";
        // compute your public key and store it in base64EncodedPublicKey
        mHelper = new IabHelper(this, base64EncodedPublicKey);
        try {
            mHelper.startSetup(new IabHelper.OnIabSetupFinishedListener() {
                public void onIabSetupFinished(IabResult result) {
                    if (!result.isSuccess()) {
                        // Oh noes, there was a problem.
                        //Toast.makeText(getSupportActionBar().getThemedContext(), "Problem setting up In-app Billing: " + result.getMessage(), Toast.LENGTH_LONG).show();
                    }
                    // Hooray, IAB is fully set up!
                    //Toast.makeText(getSupportActionBar().getThemedContext(), "Ole'", Toast.LENGTH_SHORT);
                }
            });
        } catch (NullPointerException e) {
            //Toast.makeText(getSupportActionBar().getThemedContext(), "NullPointerException configuring In-app billing", Toast.LENGTH_SHORT).show();
        }
    }

    /* change the UI's language: Italiano <-> English */
    private void setLanguage() {
        int no_lang = -1;
        Configuration config = new Configuration();

        // 0 = Italian (default), 1 = English
        pref_language = SharedPrefsMethods.getInt("language", no_lang);

        // this 'if' is executed only the first time the app is executed
        if (pref_language == no_lang) {
            Locale current = getResources().getConfiguration().locale;
            if (current == Locale.ITALIAN)
                pref_language = LanguageOptions.ENGLISH;    //switchLanguage() is going to switch before saving
            else
                pref_language = LanguageOptions.ITALIANO;   //switchLanguage() is going to switch before saving

            switchLanguage();
        } else {
            switch (pref_language) {
                case LanguageOptions.ENGLISH:
                    config.locale = Locale.ENGLISH;
                    break;
                case LanguageOptions.ITALIANO:
                default:
                    config.locale = Locale.ITALIAN;
                    break;
            }
            getResources().updateConfiguration(config, null);
        }
    }

    private void switchLanguage(){
        int new_lang = -1;

        switch( pref_language ) {
            case LanguageOptions.ENGLISH: new_lang = LanguageOptions.ITALIANO; break;
            case LanguageOptions.ITALIANO: new_lang = LanguageOptions.ENGLISH; break;
        }
        SharedPrefsMethods.putInt("language", new_lang);

        //restart app
        Intent mStartActivity = new Intent(this, MainActivity.class);
        int mPendingIntentId = 123456;
        PendingIntent mPendingIntent = PendingIntent.getActivity(this, mPendingIntentId, mStartActivity, PendingIntent.FLAG_CANCEL_CURRENT);
        AlarmManager mgr = (AlarmManager)getSystemService(Context.ALARM_SERVICE);
        mgr.set(AlarmManager.RTC, System.currentTimeMillis() + 100, mPendingIntent);
        System.exit(0);
    }

    public void onStart() {
        super.onStart();

        // hide 'Insultaci' button if english UI
        // disable button, thank user
        if ( pref_language == LanguageOptions.ENGLISH ) {
            Button button_insultaci = (Button) findViewById(R.id.button_insultaci);
            button_insultaci.setVisibility(View.GONE);
        }
    }

    public void onStop() {
        //showAdDialogIfNewAppVersion();
        super.onStop();
    }

    public void onDestroy(){
        super.onDestroy();
        if (mHelper != null) mHelper.dispose();
        mHelper = null;
    }

    /* show the ad for VaffAppPro if it is the first time running a new version of the VaffApp */
    public void showAdDialogIfNewAppVersion(){
        PackageInfo versionInfo = SharedMethods.getPackageInfo(this);
        final String current_app_ver = "app"+versionInfo.versionCode;
        String ad_app_ver = SharedPrefsMethods.getString("ad_app_ver", "");

        if ( !ad_app_ver.equals(current_app_ver) ){
            SharedPrefsMethods.putString("ad_app_ver", current_app_ver);

            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setMessage(getString(R.string.ad_message))
                    .setTitle(getString(R.string.ad_title))
                    .setNegativeButton(getString(R.string.ad_no_button), new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            //do nothing
                        }
                    })
                    .setPositiveButton(getString(R.string.ad_ok_button), new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            String appPackageName = "italo.vaffapp.prop.app";
                            try {
                                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + appPackageName)));
                            } catch (android.content.ActivityNotFoundException anfe) {
                                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("http://play.google.com/store/apps/details?id=" + appPackageName)));
                            }
                            // send to Flurry!
                            FlurryAgent.logEvent("show VaffAppPro");
                        }
                    });
            // Create the AlertDialog object and return it
            builder.create().show();
        }
    }

    /* If the user comes back a day after, unblock as many insults as UNBLOCK_INSULTS */
    private void RewardIfReturn(){
        int def_val = -1;
        ArrayList<Insult> unblocked = null;
        int today = Calendar.getInstance().get(Calendar.DATE); //returns the day of the month.

        int last_use = SharedPrefsMethods.getInt("last_day_use", def_val);
        // last_use = 4; // for debug, uncomment and device will unblock insults

        // if not null and different from today
        if ( last_use != def_val && last_use != today ) {
            SharedMethods.unblockInsults(this, getString(R.string.comeback_reward_title), UNBLOCK_INSULTS);
        }
        // save shared prefs
        SharedPrefsMethods.putInt("last_day_use", today);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.language_menu) {
            setLanguage();
            switchLanguage();
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
            View rootView = inflater.inflate(R.layout.fragment_main, container, false);
            return rootView;
        }
    }

    /* Go to InsultActivity */
    public void startInsultActivity(View view){
        Intent intent = new Intent(this, InsultActivity.class);
        intent.putExtra("pref_language", pref_language);
        startActivity(intent);
    }

    /* Go to SendInsultActivity */
    public void startSendInsultActivity(View view){
        Intent intent = new Intent(this, SendInsultActivity.class);
        startActivity(intent);
    }
}
