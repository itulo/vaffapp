package italo.vaffapp.app;

import android.support.v7.app.ActionBarActivity;
import android.support.v7.app.ActionBar;
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


public class MainActivity extends ActionBarActivity {

    private static final String TAG = "MainActivity";
    private static int ITALIANO = 0;
    private static int ENGLISH = 1;

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
    }

    // this changes the title of the settings: Italiano <-> English
    private void setLanguageOption(Menu menu){
        MenuItem item;
        // get language from shared preferences
        SharedPreferences settings = getPreferences(MODE_PRIVATE);
        // 0 = Italian (default), 1 = English
        int lang = settings.getInt("language",ITALIANO);

        item = menu.findItem(R.id.language_menu);

        if (item == null)
            System.out.println("item null");

        if ( lang == ITALIANO )
            item.setTitle("English");
        if ( lang == ENGLISH )
            item.setTitle("Italiano");

        /* SharedPreferences.Editor editor = settings.edit();
        editor.putInt("language", 1|0);
        // Commit the edits!
        editor.commit(); */
    }

    /*private int getLanguageSetting(){
        // get language from shared preferences
        SharedPreferences settings = getPreferences(MODE_PRIVATE);
        // 0 = Italian (default), 1 = English
        return settings.getInt("language",ITALIANO);
    }*/

    public void onStart() {
        super.onStart();
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        setLanguageOption(menu);
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
        startActivity(intent);
    }
}
