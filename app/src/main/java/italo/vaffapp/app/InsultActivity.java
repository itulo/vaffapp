package italo.vaffapp.app;

import android.support.v7.app.ActionBarActivity;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.content.Intent;
import android.util.Log;

import italo.vaffapp.app.databases.DatabaseHandler;
import italo.vaffapp.app.databases.Insult;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.Random;
import java.util.Collection;

import com.facebook.*;
import com.facebook.widget.*;
import android.text.ClipboardManager;
import android.app.AlertDialog;
import android.content.DialogInterface;

import android.widget.LinearLayout;

import java.util.List;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.text.TextUtils;
import android.os.Parcelable;

import com.appnext.appnextsdk.Appnext;

public class InsultActivity extends ActionBarActivity {
    private static ArrayList<Insult> insults = null;
    //private UiLifecycleHelper uiHelper;
    private Session.StatusCallback callback = null;
    // Activity code to flag an incoming activity result is due
// to a new permissions request
    private static final int REAUTH_ACTIVITY_CODE = 100;
    private TextView insult;
    private TextView insult_desc;

    private static int rand_index;
    private static byte[] occurrences;
    private static short generated_n = 0;
    private final short MAX_RETRIES = 10;

    // for condividi
    private List<Intent> targetedShareIntents;
    private List<String> diff_app;
    private Intent sharingIntent;

    private Appnext appnext;

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
        //uiHelper = new UiLifecycleHelper(this, callback);
        //uiHelper.onCreate(savedInstanceState);
    }

    // 2. configure a callback handler that's invoked when the share dialog closes and control returns to the calling app
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        //uiHelper.onActivityResult(requestCode, resultCode, data);
    }

    // 3. configure other methods on uiHelper to handle Activity lifecycle callbacks correctly
    @Override
    protected void onResume() {
        super.onResume();
        //uiHelper.onResume();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        //uiHelper.onSaveInstanceState(outState);
    }

    @Override
    public void onPause() {
        super.onPause();
        //uiHelper.onPause();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        //uiHelper.onDestroy();
    }

    public void onStart(){
        super.onStart();

        appnext = new Appnext(this);
        appnext.setAppID("a813fa77-433c-4b51-87bb-d6f7b34b4246");

        if ( insults == null ) {
            showInsult(null);
        } else{
            getTextviews();
            setTextviews();
        }
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

    /* showInsults
    1. load insults if not loaded yet
    2. generate a random number to show the insult
       there is an array that keeps all generated numbers so far
       if generated a number which was chosen already for 10 times, get the first available
    3. set insult in the view
    4. checks if array of generated numbers is full (all possible numbers have been generated)
       if yes reinitialize and start from scratch
    5. if 10 insults have been showed, show interstitial ad
     */
    public void showInsult(View view){
        short retry = 0;

        if (insults == null)
            loadInsults();

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

        getTextviews();
        setTextviews();
        getSupportActionBar().setTitle("Ti insulto! ("+getRegionFromId(insults.get(rand_index).getRegionId())+")");

        occurrences[rand_index] = 1;
        generated_n++;
        if ( generated_n == occurrences.length ) {
            //reinitialize occurrences
            for(int i=0;i<occurrences.length;i++)
                occurrences[i] = 0;
            generated_n = 0;
        }

        if ( generated_n % 10 == 0 ){
            appnext.addMoreAppsLeft("961d922f-d94d-4d08-a060-ea2d78dd6d20");
            appnext.showBubble();
        }
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
            .setDescription(insult_desc.getText().toString())
            .build();
        //uiHelper.trackPendingDialogCall(shareDialog.present());
    }

    public void checkFBPublishAction(){
        List<String> PERMISSIONS = Arrays.asList("publish_actions");
        Session session = Session.getActiveSession();

        if (session != null && session.isOpened()) {
            List<String> permissions = session.getPermissions();
            if (!isSubsetOf(PERMISSIONS, permissions)) {
                Session.NewPermissionsRequest newPermissionsRequest = new Session.NewPermissionsRequest(
                        this, PERMISSIONS).setRequestCode(REAUTH_ACTIVITY_CODE);
                session.requestNewPublishPermissions(newPermissionsRequest);
                return;
            }
        }
    }

    private boolean isSubsetOf(Collection<String> subset, Collection<String> superset) {
        for (String string : subset) {
            if (!superset.contains(string)) {
                return false;
            }
        }
        return true;
    }

    public void insultFriendOnFB() {
        // place text in clipboard
        if (insult == null ) {
            return;
        } else {
            ClipboardManager clipb = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
            clipb.setText(insult.getText());
        }

        checkFBPublishAction();

        // Create Dialog to warn user
        //http://developer.android.com/guide/topics/ui/dialogs.html
        //http://developmentality.wordpress.com/2009/10/31/android-dialog-box-tutorial/
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage("Facebook impedisce ad ogni app di scrivere nello status. Per fortuna l'insulto è stato copiato nella clipboard: devi solo incollarlo!")
                .setTitle("Mamma, che coglioni")
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        postToFB();
                    }
                });
        // Create the AlertDialog object and return it
        builder.create().show();
    }

    // check for presence of Facebook and Twitter apps (these will be treated differently)
    public void checkPresenceOfApps(View view){
        targetedShareIntents = new ArrayList<Intent>();
        diff_app = new ArrayList<String>();
        sharingIntent = new Intent(Intent.ACTION_SEND);

        sharingIntent.setType("text/plain");
        PackageManager pm = view.getContext().getPackageManager();
        List<ResolveInfo> activityList = pm.queryIntentActivities(sharingIntent, 0);

        for(final ResolveInfo app : activityList) {
            String packageName = app.activityInfo.packageName;
            if ( packageName.contains("facebook.katana") || packageName.contains("twitter") ){
                diff_app.add(packageName);
                continue;
            }
            Intent targetedShareIntent = new Intent(Intent.ACTION_SEND);
            targetedShareIntent.setType("text/plain");
            targetedShareIntent.putExtra(Intent.EXTRA_TEXT, insult.getText());
            targetedShareIntent.setPackage(packageName);
            targetedShareIntents.add(targetedShareIntent);
        }
    }

    public void insultFriendOnFBTemp(String package_name){
        final String pck_nm = package_name;

        // copia insulto
        ClipboardManager clipb = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
        clipb.setText(insult.getText()+" #vaffapp");

        // avvisa che bisogna copiare
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage("Facebook impedisce ad ogni app di scrivere nello status. Per fortuna l'insulto è stato copiato nella clipboard: devi solo incollarlo!")
                .setTitle("Mamma, che coglioni")
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        // apri facebook app
                        Intent targetedShareIntent = new Intent(Intent.ACTION_SEND);
                        targetedShareIntent.setType("text/plain");
                        targetedShareIntent.putExtra(Intent.EXTRA_TEXT, insult.getText()+" #vaffapp");
                        targetedShareIntent.setPackage(pck_nm);
                        startActivity(targetedShareIntent);
                    }
                });
        builder.create().show();
    }

    // 1. show a first choice dialog to choose between Twitter, Facebook and Other
    // 2. if "Other" is chosen, show another dialog with all apps that can share (Whatsapp, Viber, Hangout...)
    public void twoChoiceMenu(){
        // I have to declare this an array, only this way I can modify it later (final statement is necessary)
        final String[] twitterPackageName = { "twitter" };
        final String[] facebookPackageName = { "facebook" };
        final CharSequence[] items = new CharSequence[diff_app.size()+1];
        for (int i=0; i<diff_app.size();i++) {
            if (diff_app.get(i).contains("facebook"))
                facebookPackageName[0] = diff_app.get(i);
                items[i] = "Facebook";
            if (diff_app.get(i).contains("twitter")) {
                items[i] = "Twitter";
                twitterPackageName[0] = diff_app.get(i);
            }
        }
        items[diff_app.size()] = "Altro";
        new AlertDialog.Builder(this)
                .setTitle("Scegli cazzo!")
                .setItems(items, new DialogInterface.OnClickListener()
                {
                    @Override
                    public void onClick(DialogInterface dialog, int which)
                    {
                        String choice = items[which].toString();
                        if ( choice.equals("Facebook") ){
                            //insultFriendOnFB();
                            insultFriendOnFBTemp(facebookPackageName[0]);
                        }
                        if ( choice.equals("Twitter") ){
                            Intent targetedShareIntent = new Intent(Intent.ACTION_SEND);
                            targetedShareIntent.setType("text/plain");
                            targetedShareIntent.putExtra(Intent.EXTRA_TEXT, insult.getText()+" #vaffapp");
                            targetedShareIntent.setPackage(twitterPackageName[0]);
                            startActivity(targetedShareIntent);
                        }
                        if (choice.equals("Altro") ) {
                            Intent chooserIntent = Intent.createChooser(targetedShareIntents.remove(0), "E scegli cazzo!");
                            chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, targetedShareIntents.toArray(new Parcelable[]{}));
                            startActivity(chooserIntent);
                        }
                    }
                }).create().show();
    }

    // Checks presence of apps Facebook and Twitter
    // if not, shows a dialog box with all apps able to receive the insult
    public void condividi(View view){
        checkPresenceOfApps(view);

        if ( diff_app.size() > 0 ){
            twoChoiceMenu();
        }
        else {
            sharingIntent.putExtra(Intent.EXTRA_TEXT, insult.getText());
            startActivity(Intent.createChooser(sharingIntent, "Scegli cazzo!"));
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
