package italo.vaffapp.app;

import android.support.v7.app.ActionBarActivity;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.content.Intent;

import android.util.Log;

import android.content.SharedPreferences;
import java.util.Locale;
import android.content.res.Configuration;
import android.app.PendingIntent;
import android.app.AlarmManager;
import android.content.Context;
import android.widget.Button;


public class MainActivity extends ActionBarActivity {

    private int pref_language;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.container, new PlaceholderFragment())
                    .commit();
        }

        new SimpleEula(this).show();
        setLanguage(false);
    }

    /* change the UI's language: Italiano <-> English
     @params
      savelanguage: set to true to save the new language
    */
    private void setLanguage(boolean savelanguage){
        int new_lang = -1;
        Configuration config = new Configuration();

        // get language from shared preferences
        SharedPreferences settings = getPreferences(MODE_PRIVATE);
        // 0 = Italian (default), 1 = English
        pref_language = settings.getInt("language", LanguageOptions.ITALIANO);

        switch( pref_language ) {
            case LanguageOptions.ENGLISH: config.locale = Locale.ENGLISH; break;
            case LanguageOptions.ITALIANO:
            default: config.locale = Locale.ITALIAN; break;
        }

        getResources().updateConfiguration(config, null);

        if (savelanguage){

            switch( pref_language ) {
                case LanguageOptions.ENGLISH: new_lang = LanguageOptions.ITALIANO; break;
                case LanguageOptions.ITALIANO: new_lang = LanguageOptions.ENGLISH; break;
            }
            SharedPreferences.Editor editor = settings.edit();
            editor.putInt("language", new_lang);
            // this has to be 'commit' NOT 'apply'
            // commit writes the preference back in memory soon and we need it soon after we restart the app
            editor.commit();

            //restart app
            Intent mStartActivity = new Intent(this, MainActivity.class);
            int mPendingIntentId = 123456;
            PendingIntent mPendingIntent = PendingIntent.getActivity(this, mPendingIntentId, mStartActivity, PendingIntent.FLAG_CANCEL_CURRENT);
            AlarmManager mgr = (AlarmManager)getSystemService(Context.ALARM_SERVICE);
            mgr.set(AlarmManager.RTC, System.currentTimeMillis() + 100, mPendingIntent);
            System.exit(0);
        }
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
            setLanguage(true);
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
        startActivity(intent);
    }

    /* Go to SendInsultActivity */
    public void startSendInsultActivity(View view){
        Intent intent = new Intent(this, SendInsultActivity.class);
        intent.putExtra("pref_language", pref_language);
        startActivity(intent);
    }
}
