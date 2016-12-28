package hx0049.redpackage.service;

import android.annotation.TargetApi;
import android.content.Intent;
import android.os.Build;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;

import static hx0049.redpackage.MainActivity.ACTION_NOTIFY_LISTENER_SERVICE_CONNECT;
import static hx0049.redpackage.MainActivity.ACTION_NOTIFY_LISTENER_SERVICE_DISCONNECT;

/**
 * Created by hx on 2016/12/14.
 */

@TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
public class MyNotificationService extends NotificationListenerService {

    private static final String TAG = "MyNotificationService";
    public static MyNotificationService notificationService;

    @Override
    public void onCreate() {
        super.onCreate();
        if(Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            onListenerConnected();
        }
    }

    @Override
    public void onListenerConnected() {
        super.onListenerConnected();
        notificationService = this;
        //发送广播，已经连接上了
        Intent intent = new Intent(ACTION_NOTIFY_LISTENER_SERVICE_CONNECT);
        sendBroadcast(intent);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        notificationService = null;
        //发送广播，断开连接了
        Intent intent = new Intent(ACTION_NOTIFY_LISTENER_SERVICE_DISCONNECT);
        sendBroadcast(intent);
    }

    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        super.onNotificationPosted(sbn);
        QHBListenerService.handlerNotificationPosted(sbn.getPackageName(),sbn.getNotification());
    }

    @Override
    public void onNotificationRemoved(StatusBarNotification sbn) {
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            super.onNotificationRemoved(sbn);
        }
    }
    public static boolean isNotificationRunning(){
        if(notificationService == null){
            return false;
        }
        return true;
    }

}
