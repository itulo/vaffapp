package italo.vaffapp.app.util;

import android.app.Activity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import italo.vaffapp.app.databases.Insult;

/**
 * Helper class for providing sample content for user interfaces created by
 * Android template wizards.
 * <p/>
 * TODO: Replace all uses of this class before publishing your app.
 */
public class InsultContent {

    /**
     * An array of sample (dummy) items.
     */
    public static List<Insult> ITEMS = new ArrayList<Insult>();

    /**
     * A map of sample (dummy) items, by ID.
     */
    public static Map<String, Insult> ITEM_MAP = new HashMap<String, Insult>();

    public static void setupInsults(Activity act) { //could be also setupItems
        if (ITEMS.size() == 0) {
            ITEMS = SharedMethods.loadInsults(act);
        }
        if (ITEM_MAP.size() == 0) {
            for (Insult tmp : ITEMS) {
                ITEM_MAP.put(Integer.toString(tmp.getId()), tmp);
            }
        }
    }
}
