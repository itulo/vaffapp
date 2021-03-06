package italo.vaffapp.app.util;

import 	android.app.Activity;
import android.content.pm.PackageInfo;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;

import italo.vaffapp.app.R;
import italo.vaffapp.app.common.CommonMethods;
import italo.vaffapp.app.common.CommonSharedPrefsMethods;

// from: http://www.donnfelker.com/android-a-simple-eula-for-your-android-apps/

public class SimpleEula {
    private String EULA_AGREEMENT = "ENGLISH BELOW\n" +
            "    Contratto di licenza con l'utente finale (EULA):\n" +
            "    Il software oggetto del presente EULA e l'autore della VaffApp, di seguito denominato autore.\n" +
            "    Questo EULA è un accordo vincolante legalmente tra il Licenziatario e l'autore in base al quale l'autore permette l'uso del Software concesso in\n" +
            "    licenza al licenziatario. Per qualsiasi chiarimento riguardo questo EULA vi preghiamo di contattare vaffapp@gmail.com.\n" +
            "    Qualsiasi installazione, copia, accesso o utilizzo del Software concesso in licenza costituisce accettazione e promessa di aderire a tutti i termini e condizioni del presente EULA.\n" +
            "    Condizioni di utilizzo:\n" +
            "    - l'autore declina ogni responsabilità se la VaffApp viene utilizzata per insultare una persona;\n" +
            "    - l'autore non fornisce alcuna garanzia;\n" +
            "    - è vietata la traduzione e la riproduzione su altre piattaforme;\n" +
            "    - l'autore non è obbligato dal presente EULA a fornire al Licenziatario alcun servizio di supporto tecnico relativo al Software concesso in licenza;\n" +
            "    L'autore si riserva tutti i diritti non espressamente concessi al Licenziatario dal presente EULA. I diritti concessi al Licenziatario sono\n" +
            "    limitati ai diritti di proprietà intellettuale della VaffApp.\n" +
            "    Il presente EULA potrà essere modificato.\n\n" +
            "    ENGLISH:\n" +
            "    End User License Agreement:\n" +
            "    This EULA regulates the usage of the VaffApp app:\n" +
            "    - the author declines any responsibility when the VaffApp is used to insult a person;\n" +
            "    - the VaffApp has no guarantees;\n" +
            "    - it is forbidden the translation and reproduction of the VaffApp on other software platforms;\n" +
            "    - the author doesn't provide any kind of support for the VaffApp;\n" +
            "    Except for the rights explicitly granted in this License, the author retains all right, title and interest (including all intellectual property rights).\n" +
            "    This EULA may be modified in the future.\n" +
            "    For any clarification please contact vaffapp@gmail.com";

    private String EULA_PREFIX = "eula_";
    private Activity mActivity;

    public SimpleEula(Activity context) {
        mActivity = context;
    }

    public void show() {
        PackageInfo versionInfo = CommonMethods.getPackageInfo(mActivity);

        // the eulaKey changes every time you increment the version number in the AndroidManifest.xml
        final String eulaKey = EULA_PREFIX + versionInfo.versionCode;
        final String app_ver = CommonSharedPrefsMethods.getString("app_ver", "");
        if ( !app_ver.equals(eulaKey) ){

            // Show the Eula                                        // it's in the title, 'v' is needed
            String title = mActivity.getString(R.string.app_name) + " v" + versionInfo.versionName;

            //Includes the updates as well so users know what changed.
            String message = EULA_AGREEMENT;

            AlertDialog.Builder builder = new AlertDialog.Builder(mActivity)
                    .setTitle(title)
                    .setMessage(message)
                    .setPositiveButton(android.R.string.ok, new Dialog.OnClickListener() {

                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            // Mark this version as read.
                            CommonSharedPrefsMethods.putString("app_ver", eulaKey);
                            dialogInterface.dismiss();
                        }
                    })
                    .setNegativeButton(android.R.string.cancel, new Dialog.OnClickListener() {

                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            // Close the activity as they have declined the EULA
                            mActivity.finish();
                        }

                    });
            builder.create().show();
        }
    }

}
