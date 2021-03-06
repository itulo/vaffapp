package italo.vaffapp.app.util;

import android.app.Activity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import italo.vaffapp.app.common.CommonMethods;
import italo.vaffapp.app.entity.Insult;

/**
 * Helper class for providing all insults in a list - used by Master Detail flow
 */
public class InsultContent {

    /**
     * An array of sample insult items.
     */
    public static List<Insult> ITEMS = new ArrayList<Insult>();

    /**
     * A map of sample insult items, by ID.
     */
    public static Map<String, Insult> ITEM_MAP = new HashMap<String, Insult>();

    public static void setupInsults(Activity act) {
        if (ITEMS.size() == 0) {
            ITEMS = CommonMethods.loadInsults(act);
        }
        if (ITEM_MAP.size() == 0) {
            for (Insult tmp : ITEMS) {
                ITEM_MAP.put(Integer.toString(tmp.getId()), tmp);
            }
        }
    }
}
