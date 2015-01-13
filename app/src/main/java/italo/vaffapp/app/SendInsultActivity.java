package italo.vaffapp.app;

import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

import java.util.HashMap;
import java.util.Map;

import android.view.View;
import android.widget.TextView;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.RadioButton;
import android.widget.Button;

import italo.vaffapp.app.mail.GMailSender;
import italo.vaffapp.app.util.SharedMethods;

import android.os.AsyncTask;

import android.graphics.Color;

import android.util.Log;

import android.accounts.AccountManager;
import android.accounts.Account;

import com.flurry.android.FlurryAgent;
import com.google.android.gms.auth.GoogleAuthUtil;
import android.text.TextUtils;

import android.widget.CheckBox;

public class SendInsultActivity extends ActionBarActivity {
    final String vaffapp_email = "vaffapp@gmail.com";
    private String email_msg = "";
    private boolean anonymous = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_send_insult);

        SharedMethods.setIconInActionBar(this);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        // comment so it doesn't show 'Settings'
        //getMenuInflater().inflate(R.menu.send_insult, menu);
        return true;
    }

    public void onStart(){
        super.onStart();
        FlurryAgent.onStartSession(this, getString(R.string.flurry_id));
    }

    public void onStop(){
        super.onStop();
        FlurryAgent.onEndSession(this);
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

    public void sendFeedback(View view){
        Button button_manda;

        if ( checkForm() ) {
            final String identity = (anonymous) ? "Anonymous" : getDeviceEmail();
            email_msg += "\n\nBy " + identity;
            Map<String, String> flurry_stats = new HashMap<String, String>();

            new AsyncTask<Void, Void, Void>() {
                @Override public Void doInBackground(Void... params) {
                    try {
                        GMailSender sender = new GMailSender(vaffapp_email, "kuukausi");
                        sender.sendMail(vaffapp_email,
                                email_msg,
                                vaffapp_email,
                                vaffapp_email);
                    } catch (Exception e) {
                        Log.e("SendInsultActivity","SendMail\n" + e.getMessage());
                    }
                    return null;
                }
            }.execute();

            // Flurry send how many insults generated
            flurry_stats.put("Email", "By "+identity+" "+email_msg);
            FlurryAgent.logEvent("sendFeedback()", flurry_stats);

            // disable button, thank user
            button_manda = (Button)findViewById(R.id.button_manda);
            button_manda.setEnabled(false);
            button_manda.setText("Grazie!");
        }
    }

    // get email of this device (the one registered on google play)
    public String getDeviceEmail(){
        AccountManager mAccountManager = AccountManager.get(this);
        Account[] accounts = mAccountManager.getAccountsByType(GoogleAuthUtil.GOOGLE_ACCOUNT_TYPE);
        String[] names = new String[accounts.length];
        for (int i = 0; i < accounts.length; i++) {
            names[i] = accounts[i].name;
        }
        if (accounts.length > 0 )
            return TextUtils.join(",", names);
        else
            return "vaffapp@gmail.com";
    }

    // check the form but also build the message for the email
    public boolean checkForm(){
        CheckBox cb;
        EditText tmp_edittext;
        TextView tmp_textview;
        RadioButton tmp_radiobutt;
        String str = null;
        email_msg = "";

        // Check anonymous checkbox is selected
        cb = (CheckBox) findViewById(R.id.checkBox1);
        anonymous = cb.isChecked();

        // Check radio group choice
        RadioGroup rg = (RadioGroup)findViewById(R.id.radio_group_choice);
        int choice = rg.getCheckedRadioButtonId();
        if (choice == -1) {
            tmp_textview = (TextView) findViewById(R.id.textview_choice);
            tmp_textview.setTextColor(Color.RED);
            return false;
        }
        tmp_radiobutt = (RadioButton) findViewById(choice);
        email_msg += "regione: " + tmp_radiobutt.getText() + "\n";

        // check edittext(s)
        tmp_edittext = (EditText) findViewById(R.id.edittext_insulto);
        if ( tmp_edittext != null ) {
            str = tmp_edittext.getText().toString();
            if ( str.trim().length() <= 0 ) {
                tmp_textview = (TextView) findViewById(R.id.textview_insulto);
                tmp_textview.setTextColor(Color.RED);
                return false;
            }
        }
        if (str != null)
            email_msg += "'" + str + "',";

        tmp_edittext = (EditText) findViewById(R.id.edittext_descrizione);
        if ( tmp_edittext != null ) {
            str = tmp_edittext.getText().toString();
            if (str.trim().length() <= 0 && !tmp_radiobutt.getText().equals("Commento/Richiesta") ) {
                tmp_textview = (TextView) findViewById(R.id.textview_descrizione);
                tmp_textview.setTextColor(Color.RED);
                return false;
            }
        }
        if (str != null)
            email_msg += "'" + str + "'";

        return true;
    }

}
