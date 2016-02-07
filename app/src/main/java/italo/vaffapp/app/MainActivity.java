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
import java.util.HashMap;
import java.util.Locale;

import android.content.res.Configuration;
import android.app.PendingIntent;
import android.app.AlarmManager;
import android.content.Context;
import android.widget.Button;

import italo.vaffapp.app.util.LanguageOptions;
import italo.vaffapp.app.common.CommonMethods;
import italo.vaffapp.app.common.CommonSharedPrefsMethods;

import java.util.Calendar;
import java.util.Map;

import italo.vaffapp.app.util.IabHelper;
import italo.vaffapp.app.util.IabResult;
import italo.vaffapp.app.util.Inventory;
import italo.vaffapp.app.util.Purchase;
import italo.vaffapp.app.util.SimpleEula;
import italo.vaffapp.app.util.SkuDetails;

import android.widget.ImageView;

import com.vungle.publisher.AdConfig;
import com.vungle.publisher.EventListener;
import com.vungle.publisher.Orientation;
import com.vungle.publisher.VunglePub;


public class MainActivity extends ActionBarActivity {
    // for rewarding
    private final int UNLOCK_INSULTS = 10; // insults to unlock when returning user
    private final int UNLOCK_INSULTS_AD = 40; //insults to unlock when user watches ad

    // in app billing
    private IabHelper mHelper;
    private Inventory inv;
    private static final int RC_REQUEST = 10001;    // (arbitrary) request code for the purchase flow
    private String SKU_ALL_INSULTS_ID = "0";
    private int blocked_insults = -1;

    private String insults_per_region = null;   // string listing each region with how many insults are locked
    private int pref_language;

    private static VunglePub vunglePub = null;
    private final EventListener vungleListener = new EventListener(){

        @Override
        public void onVideoView(boolean isCompletedView, int watchedMillis, int videoDurationMillis) {
            Map<String, String> flurry_stats = new HashMap<String, String>();
            if ( isCompletedView ) {
                flurry_stats.put("Ad", "Played completely");
                // the function below shows a dialog so must be run in the UiThread
                MainActivity.this.runOnUiThread(new Runnable() {
                    public void run() {
                        removeVungleListenerandUnlockInsults();
                    }
                });
            } else {
                flurry_stats.put("Ad", "Not played completely");
                removeVungleListener();
            }
            CommonMethods.sendFlurry("Unlock", flurry_stats);
        }

        @Override
        public void onAdStart() {
            // Called before playing an ad
        }

        @Override
        public void onAdEnd(boolean wasCallToActionClicked) {
            // Called when the user leaves the ad and control is returned to your application
        }

        @Override
        public void onAdPlayableChanged(boolean isAdPlayable) {
            // Called when the playability state changes. if isAdPlayable is true, you can now play an ad.
            // If false, you cannot yet play an ad.
        }

        @Override
        public void onAdUnavailable(String reason) {
            // Called when VunglePub.playAd() was called, but no ad was available to play
        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                .add(R.id.container, new PlaceholderFragment())
                .commit();
        }

        CommonMethods.setIconInActionBar(this);
        CommonSharedPrefsMethods.setupSharedPrefsMethods(this);
        new SimpleEula(this).show();
        setLanguage();
        RewardIfReturn();

        if ( vunglePub == null ) {
            vunglePub = VunglePub.getInstance();
            // https://github.com/Vungle/vungle-resources/blob/master/English/Android/3.2.x/android-dev-guide.md
            vunglePub.init(this, "italo.vaffapp.app");
        }
    }

    /* change the UI's language: Italiano <-> English */
    private void setLanguage() {
        int no_lang = -1;
        Configuration config = new Configuration();

        // 0 = Italian (default), 1 = English
        pref_language = CommonSharedPrefsMethods.getInt("language", no_lang);

        // this 'if' is executed only at the first launch of the app
        if (pref_language == no_lang) {
            Locale current = getResources().getConfiguration().locale;
            if (current == Locale.ITALIAN)
                pref_language = LanguageOptions.ENGLISH;
            else
                pref_language = LanguageOptions.ITALIANO;
            //switchLanguage() is going to switch before saving
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
        CommonSharedPrefsMethods.putInt("language", new_lang);

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

        CommonMethods.onStart(this);

        // hide 'Suggerisci un insulto' button if english UI
        if ( pref_language == LanguageOptions.ENGLISH ) {
            Button button_insultaci = (Button) findViewById(R.id.button_insultaci);
            button_insultaci.setVisibility(View.GONE);
        }

        getCountBlockedInsults();
        checkInAppBilling();
    }

    public void onResume(){
        super.onResume();
        vunglePub.onResume();
    }

    public void onStop() {
        showAdDialogIfNewAppVersion();
        super.onStop();
        CommonMethods.onStop(this);
    }

    public void onPause(){
        super.onPause();
        vunglePub.onPause();
    }

    public void onDestroy(){
        super.onDestroy();
        if (mHelper != null) {
            try { mHelper.dispose();
            } catch (IllegalArgumentException e) {} // 'Service not registered' - happens in emulator
        }
        mHelper = null;
    }

    /* show the ad for VaffAppPro if it is the first time running a new version of the VaffApp */
    public void showAdDialogIfNewAppVersion(){
        PackageInfo versionInfo = CommonMethods.getPackageInfo(this);
        final String current_app_ver = "app"+versionInfo.versionCode;
        String ad_app_ver = CommonSharedPrefsMethods.getString("ad_app_ver", "");
        int days_app_opened = CommonSharedPrefsMethods.getInt("times_app_opened", -1);

        // show VaffAppPro ad anytime the version changes, but not the first time ever the app is used
        if ( !ad_app_ver.equals(current_app_ver) && days_app_opened > 1){
            CommonSharedPrefsMethods.putString("ad_app_ver", current_app_ver);

            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setMessage(getString(R.string.ad_message))
                    .setTitle(getString(R.string.ad_title))
                    .setNegativeButton(getString(R.string.no_thanks), new DialogInterface.OnClickListener() {
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
                            CommonMethods.sendEventFlurry("Open VaffAppPro");
                        }
                    });

            builder.create().show();
        }
    }

    /* If the user comes back a day after, unlock as many insults as UNLOCK_INSULTS */
    private void RewardIfReturn(){
        int last_day_use_def_val = -1;
        Map<String, String> flurry_stats = new HashMap<String, String>();

        int today = Calendar.getInstance().get(Calendar.DATE); //returns the day of the month.

        int last_use = CommonSharedPrefsMethods.getInt("last_day_use", last_day_use_def_val);
        // last_use = 4; // for debug, uncomment and device will unlock insults (as long as today is no the 4th :) )

        int days_app_opened = CommonSharedPrefsMethods.getInt("times_app_opened", 0);
        days_app_opened++;

        // if not null and different from today
        if ( last_use != last_day_use_def_val && last_use != today ) {
            if (days_app_opened > 1) {
                flurry_stats.put("Times", Integer.toString(days_app_opened));
                CommonMethods.sendFlurry("Returning user", flurry_stats);
            }
            CommonMethods.unlockInsults(this, getString(R.string.comeback_reward_title), UNLOCK_INSULTS);
        }
        // save shared prefs
        CommonSharedPrefsMethods.putInt("last_day_use", today);
        CommonSharedPrefsMethods.putInt("times_app_opened", days_app_opened);
    }

    void getCountBlockedInsults(){
        if ( blocked_insults == -1 ) {
            blocked_insults = CommonMethods.getAmountBlockedInsults(this);
            if (blocked_insults <= 0)
                hideButton( (Button)findViewById(R.id.button_buy_insults) );
        }
    }

    void hideButton(Button b){
        b.setVisibility(View.GONE);
    }
    void showButton(Button b){
        b.setVisibility(View.VISIBLE);
    }

    void unlockAllInsults(){
        if ( blocked_insults > 0) {
            CommonMethods.unlockInsults(this, getString(R.string.bought_insults_title), blocked_insults);
            hideButton((Button)findViewById(R.id.button_buy_insults));
        }
    }

    void playAd(){
        Map<String, String> flurry_stats = new HashMap<String, String>();

        if (vunglePub.isAdPlayable()) {
            flurry_stats.put("Ad", "Play");
            vunglePub.setEventListeners(vungleListener);
            final AdConfig overrideConfig = new AdConfig();
            overrideConfig.setOrientation(Orientation.autoRotate);
            overrideConfig.setIncentivized(true);
            overrideConfig.setSoundEnabled(false);
            vunglePub.playAd(overrideConfig);
        } else {
            flurry_stats.put("Ad", "Not available");
            CommonMethods.showDialog(this, getString(R.string.ad_na_title), getString(R.string.ad_na_msg));
        }
        CommonMethods.sendFlurry("Unlock", flurry_stats);
    }

    void removeVungleListener(){
        vunglePub.removeEventListeners(vungleListener);
    }

    void removeVungleListenerandUnlockInsults(){
        removeVungleListener();
        CommonMethods.unlockInsults(this, getString(R.string.ad_watched), UNLOCK_INSULTS_AD);
    }

    /// IN APP BILLING  METHODS ///
    private void checkInAppBilling(){
        /* in app billing */
        String base64EncodedPublicKey = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAuVoijs7LeeIM18nmYa2gX6iqFGRtzbPJxlsqVJFYjwceyK729qQcNRW0lZQKlvNSO2B5leFqwb86nB3LT57COBjpDvoKUHYDAKdM7RBqCxI2WyD47R+dHROokVwjHJlmo/H3gDCkdZF2idV4HAInWYUC8WwVgG5xv6jqVRp01hRuiSdadtjK+oUAIJPYsm690I+lzJcQTmcdXisC6k/yKXw+OsTRrwhudXeHFAVlBZxS/YteaI7rjgJjPCkheRA5iBdgK75925K4g1w/jNOuuZwkGoxCQjUxmFGSc8EwaKbLrfuJlg715RSQuQT6v/xs+/rDDMvifgFSsxJGO+Sd9QIDAQAB";
        Inventory inv = null;

        mHelper = new IabHelper(this, base64EncodedPublicKey);
        //mHelper.enableDebugLogging(true);
        try {
            mHelper.startSetup(new IabHelper.OnIabSetupFinishedListener() {
                public void onIabSetupFinished(IabResult result) {
                    if (!result.isSuccess()) {
                        // Oh noes, there was a problem.
                        complain("Problem setting up in-app billing: " + result);
                        return;
                    }

                    ArrayList inAppProducts = new ArrayList();
                    inAppProducts.add(SKU_ALL_INSULTS_ID);
                    mHelper.queryInventoryAsync(true, inAppProducts, mGotInventoryListener);
                }
            });
        // Emulator throws it
        }catch (NullPointerException e) { }
    }

    // Listener that's called when we finish querying the items and subscriptions we own
    IabHelper.QueryInventoryFinishedListener mGotInventoryListener = new IabHelper.QueryInventoryFinishedListener() {
        public void onQueryInventoryFinished(IabResult result, Inventory inventory) {
            // Have we been disposed of in the meantime? If so, quit.
            if (mHelper == null) return;

            // Is it a failure?
            if (result.isFailure()) {
                complain("Failed to query inventory: " + result);
                return;
            }
            inv = inventory;
            checkInventory();
        }
    };

    void checkInventory(){
        if ( inv.hasPurchase(SKU_ALL_INSULTS_ID) )
            unlockAllInsults();
    }

    public void buyAllInsults(View v){
        String title = getString(R.string.buy_insults);
        Map<String, String> flurry_stats = new HashMap<String, String>();
        flurry_stats.put("Unlock", "All insults");
        CommonMethods.sendFlurry("Unlock", flurry_stats);

        if ( inv != null ) {
            SkuDetails sku_d = inv.getSkuDetails(SKU_ALL_INSULTS_ID);
            if ( sku_d != null ){
                title += " " + "(" + sku_d.getPrice() + ")";
            }
        }

        if ( insults_per_region == null )
            insults_per_region = CommonMethods.getStringInsultsPerRegion(this);

        AlertDialog.Builder bld = new AlertDialog.Builder(this);
        bld.setTitle(title)
            .setMessage(getString(R.string.msg_unlock_insults) + UNLOCK_INSULTS_AD +' '+ getString(R.string.insults) + getString(R.string.msg_unlock_insults2) + insults_per_region)
            .setNeutralButton(getString(R.string.buy), new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dlg, int id) {
                    Map<String, String> flurry_stats = new HashMap<String, String>();
                    flurry_stats.put("Unlock", "All insults - Buy");
                    CommonMethods.sendFlurry("Unlock", flurry_stats);
                    initiatePurchaseAllInsults();
                }
            })
            .setNegativeButton(getString(R.string.watch_ad), new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dlg, int id) {
                    Map<String, String> flurry_stats = new HashMap<String, String>();
                    flurry_stats.put("Unlock", "Watch ad");
                    CommonMethods.sendFlurry("Unlock", flurry_stats);
                    playAd();
                }
            })
            .setPositiveButton(getString(R.string.no_thanks), null)
            .create()
            .show();
    }

    public void initiatePurchaseAllInsults(){
        if ( !inv.hasPurchase(SKU_ALL_INSULTS_ID) ) {
            // We will be notified of completion via mPurchaseFinishedListener
            mHelper.launchPurchaseFlow(this, SKU_ALL_INSULTS_ID, RC_REQUEST, mPurchaseFinishedListener, "vaffapp");
        }
    }

    // Callback for when a purchase is finished
    IabHelper.OnIabPurchaseFinishedListener mPurchaseFinishedListener = new IabHelper.OnIabPurchaseFinishedListener() {
        public void onIabPurchaseFinished(IabResult result, Purchase purchase) {

            // if we were disposed of in the meantime, quit.
            if (mHelper == null) return;

            Map<String, String> flurry_stats = new HashMap<String, String>();

            if (result.isFailure()) {
                complain("Error purchasing: " + result);
                return;
            }

            if (purchase.getSku().equals(SKU_ALL_INSULTS_ID)) {
                // bought
                // no consumption for this - purchase is forever
                // mHelper.consumeAsync(purchase, mConsumeFinishedListener);
                flurry_stats.put("Unlock", "Purchase all insults");
                unlockAllInsults();
            }

            CommonMethods.sendFlurry("Unlock", flurry_stats);
        }
    };

    // Called when consumption is complete - keeping this function for the future
    IabHelper.OnConsumeFinishedListener mConsumeFinishedListener = new IabHelper.OnConsumeFinishedListener() {
        public void onConsumeFinished(Purchase purchase, IabResult result) {

            // if we were disposed of in the meantime, quit.
            if (mHelper == null) return;

            // We know this is the "gas" sku because it's the only one we consume,
            // so we don't check which sku was consumed. If you have more than one
            // sku, you probably should check...
            if (!result.isSuccess()) {
                //unlockAllInsults();
            }
            else
                complain("Error while consuming: " + result);
        }
    };
    /// IN APP BILLING METHODS END ///

    void complain(String message) {
        Map<String, String> flurry_stats = new HashMap<String, String>();
        flurry_stats.put("Complain", "Something failed: " + message);
        CommonMethods.sendFlurry("Complain", flurry_stats);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (mHelper == null) return;

        // Pass on the activity result to the helper for handling
        if (!mHelper.handleActivityResult(requestCode, resultCode, data)) {
            // not handled, so handle it ourselves (here's where you'd
            // perform any handling of activity results not related to in-app
            // billing...
            super.onActivityResult(requestCode, resultCode, data);
        }
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

    /* Go to InsultDetailActivity */
    public void startInsultDetailActivity(View view) {
        Intent intent = new Intent(this, InsultListActivity.class);
        intent.putExtra("pref_language", pref_language);
        startActivity(intent);
    }
}
