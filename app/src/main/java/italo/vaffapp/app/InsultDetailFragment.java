package italo.vaffapp.app;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBarActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.content.Context;

import com.flurry.android.FlurryAgent;

import italo.vaffapp.app.databases.Insult;
import italo.vaffapp.app.util.InsultContent;
import italo.vaffapp.app.util.SharedMethods;

/**
 * A fragment representing a single Insult detail screen.
 * This fragment is either contained in a {@link InsultListActivity}
 * in two-pane mode (on tablets) or a {@link InsultDetailActivity}
 * on handsets.
 */
public class InsultDetailFragment extends Fragment {
    /**
     * The fragment argument representing the item ID that this fragment
     * represents.
     */
    public static final String ARG_ITEM_ID = "item_id";

    /**
     * The dummy content this fragment is presenting.
     */
    private Insult mItem;

    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public InsultDetailFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        SharedMethods.onCreate(getActivity(), savedInstanceState);

        if (getArguments().containsKey(ARG_ITEM_ID)) {
            // Load the dummy content specified by the fragment
            // arguments. In a real-world scenario, use a Loader
            // to load content from a content provider.
            mItem = InsultContent.ITEM_MAP.get(getArguments().getString(ARG_ITEM_ID));
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        //View rootView = inflater.inflate(R.layout.fragment_insult_detail, container, false);
        // IA - re-use fragment_main2
        View rootView = inflater.inflate(R.layout.fragment_main2, container, false);

        // Show the dummy content as text in a TextView.
        //if (mItem != null) {
        //((TextView) rootView.findViewById(R.id.insult_detail)).setText(mItem.content);
        //setTextviews(mItem);
        //}

        return rootView;
    }

    //IA
    public void onStart() {
        super.onStart();
        Activity a = getActivity();
        int pref_language = -1;

        SharedMethods.onStart(a.getApplicationContext());

        SharedPreferences settings = a.getSharedPreferences(getString(R.string.preferences_file), Context.MODE_PRIVATE);
        pref_language = settings.getInt("language", LanguageOptions.ITALIANO);

        setRegionNameInTitle();
        setTextviews(mItem, pref_language);
        setTextviewsListeners();
        hideButtonAndTextView(pref_language);
        setCondividiOnClick();
    }

    // 2. configure a callback handler that's invoked when the share dialog closes and control returns to the calling app
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        SharedMethods.onActivityResult(requestCode, resultCode, data);
    }

    // 3. configure other methods on uiHelper to handle Activity lifecycle callbacks correctly
    @Override
    public void onResume() {
        super.onResume();
        SharedMethods.onResume();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        SharedMethods.onSaveInstanceState(outState);
    }

    @Override
    public void onPause() {
        super.onPause();
        SharedMethods.onPause();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        SharedMethods.onDestroy();
    }

    //IA
    public void setTextviews(Insult i, int pref_lang) {
        TextView tmp;
        View view = getView();

        tmp = (TextView) view.findViewById(R.id.insult);
        tmp.setText(i.getInsult());

        tmp = (TextView) view.findViewById(R.id.insult_desc);
        tmp.setText(i.getDesc());

        if (pref_lang == LanguageOptions.ENGLISH) {
            tmp = (TextView) view.findViewById(R.id.insult_eng);
            String eng = i.getEnglish();
            if (eng.equals("")) {
                eng = "Not translatable";
                tmp.setTypeface(null, Typeface.ITALIC);
            } else {
                tmp.setTypeface(null, Typeface.NORMAL);
            }
            tmp.setText(eng);
        }
    }

    public void setTextviewsListeners() {
        TextView tmp;
        View view = getView();

        tmp = (TextView) view.findViewById(R.id.insult);
        tmp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SharedMethods.speakInsult(mItem.getInsult());
            }
        });

        tmp = (TextView) view.findViewById(R.id.insult_desc);
        tmp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SharedMethods.speakDesc(mItem.getDesc());
            }
        });

        tmp = (TextView) view.findViewById(R.id.insult_eng);
        tmp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SharedMethods.speakEnglish(mItem.getEnglish());
            }
        });
    }

    //IA
    public void hideButtonAndTextView(int pref_lang) {
        TextView tmpt;
        View view = getView();

        // hide the 'insultati di nuovo'/'other insult' button
        Button tmpb = (Button) view.findViewById(R.id.insultati_ancora_button);
        tmpb.setVisibility(View.GONE);
        // and the fake textview used as separator between buttons
        tmpt = (TextView) view.findViewById(R.id.fake_textview);
        tmpt.setVisibility(View.GONE);

        // if language is italian - hide english translation textview
        if (pref_lang == LanguageOptions.ITALIANO) {
            tmpt = (TextView) view.findViewById(R.id.insult_eng);
            SharedMethods.hideEngTextView(tmpt);
        }
    }

    // IA - does not work gotta do binding programmatically
    public void setCondividiOnClick() {
        Button tmpb = (Button) getView().findViewById(R.id.condividi_button);
        tmpb.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SharedMethods.share(getActivity(), mItem);
            }
        });
    }

    public void setRegionNameInTitle() {
        String region = SharedMethods.getRegionFromId(mItem.getRegionId());
        ((ActionBarActivity) getActivity()).getSupportActionBar().setTitle(region);
    }
}
