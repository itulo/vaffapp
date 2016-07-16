package italo.vaffapp.app;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.List;

import italo.vaffapp.app.common.CommonMethods;
import italo.vaffapp.app.common.CommonSharedPrefsMethods;
import italo.vaffapp.app.entity.Insult;
import italo.vaffapp.app.util.LanguageOptions;

/**
 * Created by italoarmenti on 13/07/16.
 * From http://hmkcode.com/android-custom-listview-items-row/
 */
public class InsultListAdapter extends ArrayAdapter<Insult> {
    private final Context context;
    private final List<Insult> insultList;

    public InsultListAdapter(Context context, List<Insult> insultList) {

        super(context, R.layout.custom_insult_list, insultList);

        this.context = context;
        this.insultList = insultList;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        // 1. Create inflater
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        // 2. Get rowView from inflater
        View rowView = inflater.inflate(R.layout.custom_insult_list, parent, false);

        // 3. Get the two text view from the rowView
        TextView regionView = (TextView) rowView.findViewById(R.id.region);
        TextView insultView = (TextView) rowView.findViewById(R.id.insult);
        TextView descriptionView = (TextView) rowView.findViewById(R.id.description);

        // 4. Set the text for textView
        Insult insult = insultList.get(position);
        regionView.setText(CommonMethods.getRegionFromId(insult.getRegionId()));
        insultView.setText(insult.getInsult());

        String desc = "";
        int prefLanguage = CommonSharedPrefsMethods.getPrefLanguage();
        if (prefLanguage == LanguageOptions.ENGLISH)
            desc = insult.getEnglish();
        if(prefLanguage == LanguageOptions.ITALIANO)
            desc = insult.getDesc();
        descriptionView.setText(desc);

        // 5. return rowView
        return rowView;
    }
}
