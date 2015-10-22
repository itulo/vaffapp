package italo.vaffapp.app.util;

import android.app.Notification;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

// solution by https://gist.github.com/BrandonSmith/6679223

public class NotificationPublisher extends BroadcastReceiver {

    public static String NOTIFICATION_ID = "vaffapp-notification-id";
    public static String NOTIFICATION = "vaffapp-notification";

    public void onReceive(Context context, Intent intent) {

        NotificationManager notificationManager = (NotificationManager)context.getSystemService(Context.NOTIFICATION_SERVICE);

        Notification notification = intent.getParcelableExtra(NOTIFICATION);
        int id = intent.getIntExtra(NOTIFICATION_ID, 0);
        notificationManager.notify(id, notification);
    }
}
