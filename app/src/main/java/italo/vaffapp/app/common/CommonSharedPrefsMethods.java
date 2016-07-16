package italo.vaffapp.app.common;

import android.content.Context;
import android.app.Activity;
import android.content.SharedPreferences;

/**
 * Created by iarmenti on 3/18/15.
 *
 * Preferences within an Activity
 * Used in: SimpleEula, MainActivity
 */
public class CommonSharedPrefsMethods {
    final private static String PREFERENCE_LANGUAGE_NAME = "language";
    private static int prefLanguage = -1;
    private static SharedPreferences prefs;

    /* any activity needing shared prefs should call this method in onCreate */
    public static void setupSharedPrefsMethods(Activity a){
        prefs = a.getPreferences(Context.MODE_PRIVATE);
    }

    public static String getString(String var_name, String default_val){
        return prefs.getString(var_name, default_val);
    }

    public static int getInt(String var_name, int default_val){
        return prefs.getInt(var_name, default_val);
    }

    public static void putString(String var_name, String new_value){
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(var_name, new_value);
        // this has to be 'commit' NOT 'apply'
        // commit writes the preference back in memory soon and we need it soon after we restart the app
        editor.commit();
    }

    public static void putInt(String var_name, int new_value){
        SharedPreferences.Editor editor = prefs.edit();
        editor.putInt(var_name, new_value);
        editor.commit();
    }

    public static void clearPrefs(){
        prefs.edit().clear().commit();
    }

    public static void savePrefLanguage(int prefLanguage){
        putInt(PREFERENCE_LANGUAGE_NAME, prefLanguage);
    }

    public static int getPrefLanguage(){
        if (prefLanguage == -1)
            prefLanguage = prefs.getInt(PREFERENCE_LANGUAGE_NAME, prefLanguage);
        return prefLanguage;
    }
}
