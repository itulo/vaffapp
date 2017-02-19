package italo.vaffapp.app;

import android.graphics.Color;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

import com.flurry.android.FlurryAgent;

import java.util.HashMap;
import java.util.Map;

import italo.vaffapp.app.common.CommonMethods;

public class SendInsultActivity extends ActionBarActivity {
    private String feedback = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_send_insult);

        CommonMethods.setIconInActionBar(this);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu){
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
            final EditText email = (EditText)findViewById(R.id.edittext_email);
            Map<String, String> flurry_stats = new HashMap<String, String>();

            feedback += "\n\nBy " + email.getText().toString();
            // send the feedback to flurry
            flurry_stats.put("Feedback", feedback);
            FlurryAgent.logEvent("User feedback", flurry_stats);

            // disable button, thank user
            button_manda = (Button)findViewById(R.id.button_manda);
            button_manda.setEnabled(false);
            button_manda.setText("Grazie!");
        }
    }

    // check the form but also build the message for the email
    public boolean checkForm(){
        EditText tmp_edittext;
        TextView tmp_textview;
        RadioButton tmp_radiobutt;
        String str = null;
        // the message is in italian: 'new insult suggested from the VaffApp'
        feedback = "Nuovo insulto suggerito tramite la VaffApp:\n\n";

        // Check radio group choice
        RadioGroup rg = (RadioGroup)findViewById(R.id.radio_group_choice);
        int choice = rg.getCheckedRadioButtonId();
        if (choice == -1) {
            tmp_textview = (TextView) findViewById(R.id.textview_choice);
            tmp_textview.setTextColor(Color.RED);
            return false;
        }
        tmp_radiobutt = (RadioButton) findViewById(choice);
        // 'Region:'
        feedback += "Regione: " + tmp_radiobutt.getText() + "\n";

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
            // 'Insult:'
            feedback += "Insulto: '" + str + "'\n";

        tmp_edittext = (EditText) findViewById(R.id.edittext_descrizione);
        if ( tmp_edittext != null ) {
            str = tmp_edittext.getText().toString();
                                                                            //Comment/Request
            if (str.trim().length() <= 0 && !tmp_radiobutt.getText().equals("Commento/Richiesta") ) {
                tmp_textview = (TextView) findViewById(R.id.textview_descrizione);
                tmp_textview.setTextColor(Color.RED);
                return false;
            }
        }
        if (str != null)
            // 'Description:'
            feedback += "Descrizione: '" + str + "'";

        return true;
    }

}
