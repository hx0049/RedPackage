package hx0049.redpackage.service;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.app.Notification;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityManager;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import hx0049.redpackage.applistener.BaseAppListener;
import hx0049.redpackage.applistener.BaseInterface;
import hx0049.redpackage.applistener.WeChatListener;

import static hx0049.redpackage.MainActivity.ACTION_QIANGHONGBAO_SERVICE_CONNECT;
import static hx0049.redpackage.MainActivity.ACTION_QIANGHONGBAO_SERVICE_DISCONNECT;

/**
 * Created by hx on 2016/12/14.
 */

public class QHBListenerService extends AccessibilityService {
    private static final String TAG = "QHBListenerService";
    private static QHBListenerService service;

    private List<BaseAppListener> baseAppList;
    private HashMap<String, BaseAppListener> mBaseAppMap;
    private static final Class[] ACCESSIBILITY_APPS = {
            WeChatListener.class,
    };

    @Override
    public void onCreate() {
        super.onCreate();
        baseAppList = new ArrayList<>();
        mBaseAppMap = new HashMap<>();

        for (Class clazz : ACCESSIBILITY_APPS) {
            try {
                Object object = clazz.newInstance();
                if (object instanceof BaseAppListener) {
                    BaseAppListener listener = (BaseAppListener) object;
                    listener.onCreateAppListener(this);
                    baseAppList.add(listener);
                    mBaseAppMap.put(listener.getPackageName(), listener);
                }
            } catch (InstantiationException e) {
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        }
        Log.d("-------------", "onCreate:  QHBListenerService");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mBaseAppMap.clear();
        for (BaseInterface baseInterface : baseAppList) {
            baseInterface.onStopAppListener();
        }
        baseAppList.clear();

        mBaseAppMap = null;
        baseAppList = null;
        service = null;

        //发送广播，已经断开辅助服务
        Intent intent = new Intent(ACTION_QIANGHONGBAO_SERVICE_DISCONNECT);
        sendBroadcast(intent);
        Log.d("-------------", "onDestroy:  QHBListenerService");

    }

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        service = this;
        //发送广播，已经连接上了
        Intent intent = new Intent(ACTION_QIANGHONGBAO_SERVICE_CONNECT);
        sendBroadcast(intent);
        Toast.makeText(this, "已连接抢红包服务", Toast.LENGTH_SHORT).show();
        Log.d("-------------", "onServiceConnected:  QHBListenerService");
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        String pkn = String.valueOf(event.getPackageName());
        if (baseAppList == null) {
            return;
        }
        for (BaseInterface baseInterface:baseAppList){
            if(pkn.equals(baseInterface.getPackageName())){
                baseInterface.onReceiveAppListener(event);
            }
        }
        Log.d("-------------", "onAccessibilityEvent:  QHBListenerService");
    }

    @Override
    public void onInterrupt() {
        Toast.makeText(this, "中断抢红包服务", Toast.LENGTH_SHORT).show();
    }

    public static boolean isRunning() {
        if (service == null) {
            return false;
        }
        AccessibilityManager manager = (AccessibilityManager) service.getSystemService(Context.ACCESSIBILITY_SERVICE);
        AccessibilityServiceInfo info = service.getServiceInfo();
        if (info == null) {
            return false;
        }
        List<AccessibilityServiceInfo> infoList = manager.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_GENERIC);
        for (AccessibilityServiceInfo accessibilityServiceInfo : infoList) {
            if (accessibilityServiceInfo.getId().equals(info.getId())) {
                return true;
            }
        }
        return false;
    }

    public static void handlerNotificationPosted(String packageName, Notification notification){
        Log.d("-------------", "handlerNotificationPosted:  QHBListenerService   XXXXXXXX" + packageName +"  "+notification.tickerText);
        if(packageName == null && notification == null){
            return;
        }
        if(service == null || service.baseAppList == null){
            return;
        }
        BaseInterface baseInterface = service.mBaseAppMap.get(packageName);
        if(baseInterface == null){
            return;
        }
        baseInterface.onNotificationPost(String.valueOf(notification.tickerText),notification);
    }
}
