package italo.vaffapp.app.util;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;

import italo.vaffapp.app.R;

/**
 * Created by iarmenti on 11/27/14.
 */
public class SharedMethods {

    /* set vaffapp icon in ActionBar - Needed from Android 5
      Used by: SimpleEula, MainActivity
     */
    public static void setIconInActionBar(ActionBarActivity act) {
        ActionBar ab = act.getSupportActionBar();

        ab.setDisplayShowHomeEnabled(true);
        ab.setLogo(R.drawable.ic_launcher);
        ab.setDisplayUseLogoEnabled(true);
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

    public static void showDialog(Activity a, String title, String text){
        AlertDialog.Builder builder = new AlertDialog.Builder(a);
        builder.setMessage(text)
                .setTitle(title)
                .setPositiveButton(a.getString(R.string.ok), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        //do nothing
                    }
                });
        builder.create().show();
    }
}
