package italo.vaffapp.app.util;

import android.graphics.drawable.Drawable;

/**
 * Created by iarmenti on 2/9/15.
 */
public class App {
    public final String text;
    public final String package_name;
    public final Drawable icon;
    public App(String text, String package_name, Drawable icon) {
        this.text = text;
        this.package_name = package_name;
        this.icon = icon;
    }

    public String getPackageName(){
        return this.package_name;
    }
    @Override
    public String toString() {
        return text;
    }
}
