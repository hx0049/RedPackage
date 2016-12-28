package hx0049.redpackage.applistener;

import android.app.Notification;
import android.view.accessibility.AccessibilityEvent;

import hx0049.redpackage.service.QHBListenerService;

/**
 * Created by hx on 2016/12/14.
 */

public interface BaseInterface {
     void onCreateAppListener(QHBListenerService service);
     void onReceiveAppListener(AccessibilityEvent event);
     void onStopAppListener();
    String getPackageName();
    void onNotificationPost(String ticker,Notification notification);
}
