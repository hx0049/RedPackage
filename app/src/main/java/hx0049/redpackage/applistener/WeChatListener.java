package hx0049.redpackage.applistener;

import android.annotation.TargetApi;
import android.app.Notification;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Parcelable;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import hx0049.redpackage.service.QHBListenerService;
import hx0049.redpackage.utils.AccessibilityHelper;
import hx0049.redpackage.utils.NotifyHelper;

/**
 * Created by hx on 2016/12/14.
 */

public class WeChatListener extends BaseAppListener {
    /**
     * 微信的包名
     */
    public static final String WECHAT_PACKAGENAME = "com.tencent.mm";
    /**
     * 微信包信息
     */
    private PackageInfo mWechatPackageInfo;
    /**
     * 红包消息的关键字
     */
    private static final String HONGBAO_TEXT_KEY = "[微信红包]";

    private static final String BUTTON_CLASS_NAME = "android.widget.Button";

    /** 不能再使用文字匹配的最小版本号 */
    private static final int USE_ID_MIN_VERSION = 700;// 6.3.8 对应code为680,6.3.9对应code为700



    public static final int WX_MODE_0 = 0;//自动抢
    public static final int WX_MODE_1 = 1;//抢单聊红包,群聊红包只通知
    public static final int WX_MODE_2 = 2;//抢群聊红包,单聊红包只通知
    public static final int WX_MODE_3 = 3;//通知手动抢

    public static final int WX_AFTER_OPEN_HONGBAO = 0;//拆红包
    public static final int WX_AFTER_OPEN_SEE = 1; //看大家手气
    public static final int WX_AFTER_OPEN_NONE = 2; //静静地看着

    public static final int WX_AFTER_GET_GOHOME = 0; //返回桌面
    public static final int WX_AFTER_GET_NONE = 1;

    private static final int WINDOW_NONE = 0;
    private static final int WINDOW_LUCKYMONEY_RECEIVEUI = 1;
    private static final int WINDOW_LUCKYMONEY_DETAIL = 2;
    private static final int WINDOW_LAUNCHER = 3;
    private static final int WINDOW_OTHER = -1;

    private int mCurrentWindow = WINDOW_NONE;

    private boolean isReceivingHongbao;



    private QHBListenerService service;

    @Override
    public void onCreateAppListener(QHBListenerService service) {
        Log.d("----------------微信", "onCreateAppListener: ");
        this.service = service;

        updatePackageInfo();

        IntentFilter filter = new IntentFilter();
        filter.addDataScheme("package");
        filter.addAction("android.intent.action.PACKAGE_ADDED");
        filter.addAction("android.intent.action.PACKAGE_REPLACED");
        filter.addAction("android.intent.action.PACKAGE_REMOVED");
        getContext().registerReceiver(broadcastReceiver, filter);
    }

    @Override
    public void onReceiveAppListener(AccessibilityEvent event) {
        Log.d("------------------", "onReceiveAppListener: ");
        int eventType = event.getEventType();
        switch (eventType) {
            //通知栏事件
            case AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED:
                Log.d("------------------", "通知栏事件: ");
                Parcelable data = event.getParcelableData();
                if (data != null && !(data instanceof Notification)) {
                    return;
                }
                List<CharSequence> texts = event.getText();
                if (texts != null) {
                    String text = String.valueOf(texts);
                    notificationEvent(text, (Notification) data);
                    Log.d("------------------微信", "调用通知栏事件:onReceiveAppListener ");
                }
                break;
            case AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED:
                Log.d("------------------微信", "准备打开红包: ");
                openHongBao(event);
                break;
            case AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED:
                if(mCurrentWindow != WINDOW_LAUNCHER) { //不在聊天界面或聊天列表，不处理
                    return;
                }

                if(isReceivingHongbao) {
                    Log.d("------------------", "准备处理聊天页面红包: ");
                    handleChatListHongBao();
                }else{
                    Log.d("------------------", "状态为未收到红包: ");
                }
                break;

        }

    }

    /**
     * 通知栏事件
     */
    private void notificationEvent(String ticker, Notification nf) {
        Log.d("------------------微信", "通知栏事件");
        String text = ticker;
        int index = text.indexOf(":");
        if (index != -1) {
            text = text.substring(index + 1);
        }
        text = text.trim();
        if (text.contains(HONGBAO_TEXT_KEY)) { //红包消息
            newHongBaoNotification(nf);
            Log.d("------------------微信", "通知栏事件: 收到新红包 ");
        }
    }

    @Override
    public void onStopAppListener() {
        Log.d("-----------微信", "onStopAppListener: ");
        try {
            getContext().unregisterReceiver(broadcastReceiver);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public Context getContext() {
        return service.getApplicationContext();
    }

    private BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            //更新安装包信息
            updatePackageInfo();
        }
    };

    /**
     * 更新微信包信息
     */
    private void updatePackageInfo() {
        try {
            mWechatPackageInfo = getContext().getPackageManager().getPackageInfo(WECHAT_PACKAGENAME, 0);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
    }

    @Override
    public String getPackageName() {
        return WECHAT_PACKAGENAME;
    }

    @Override
    public void onNotificationPost(String tickerText, Notification notification) {
        Log.d("-------------微信", "红包服务调用");
        notificationEvent(tickerText, notification);
    }







    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    private void openHongBao(AccessibilityEvent event) {
        if("com.tencent.mm.plugin.luckymoney.ui.LuckyMoneyReceiveUI".equals(event.getClassName())) {
            mCurrentWindow = WINDOW_LUCKYMONEY_RECEIVEUI;
            //点中了红包，下一步就是去拆红包
            Log.d("--------------微信", "openHongBao: 点中了红包，下一步就是去拆红包");
            handleLuckyMoneyReceive();
        } else if("com.tencent.mm.plugin.luckymoney.ui.LuckyMoneyDetailUI".equals(event.getClassName())) {
            mCurrentWindow = WINDOW_LUCKYMONEY_DETAIL;
            //拆完红包后看详细的纪录界面
            if(WX_AFTER_GET_NONE == WX_AFTER_GET_GOHOME) { //TODO 定死为 啥都不做，以便收到下一次的红包通知
                AccessibilityHelper.performHome(getService());
                Log.d("--------------微信", "openHongBao: 拆完红包后看详细的纪录界面，跳转到桌面");
            }
        } else if("com.tencent.mm.ui.LauncherUI".equals(event.getClassName())) {
            mCurrentWindow = WINDOW_LAUNCHER;
            //在聊天界面,去点中红包
            Log.d("--------------微信", "openHongBao: 在聊天界面,去点中红包");
            handleChatListHongBao();
        } else {
            mCurrentWindow = WINDOW_OTHER;
            Log.d("--------------微信", "openHongBao: 其他情况");
        }
    }

    /** 打开通知栏消息*/
    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    private void newHongBaoNotification(Notification notification) {
        isReceivingHongbao = true;
        //以下是精华，将微信的通知栏消息打开
        Log.d("--------------微信", "newHongBaoNotification: 以下是精华，将微信的通知栏消息打开");
        PendingIntent pendingIntent = notification.contentIntent;
        boolean lock = NotifyHelper.isLockScreen(getContext());

        if(!lock) {
            Log.d("--------------微信", "newHongBaoNotification: 跳转到页面");
            NotifyHelper.send(pendingIntent);
        } else {
            Log.d("--------------微信 锁屏", "newHongBaoNotification");
            NotifyHelper.showNotify(getContext(), String.valueOf(notification.tickerText), pendingIntent);
        }

        if(lock || WX_MODE_0 != WX_MODE_0) {
            NotifyHelper.playEffect(getContext());
            NotifyHelper.unLockScreenAndExitWeak(getService());
            notificationEvent(String.valueOf(notification.tickerText),notification);
            Log.d("--------------微信 ", "newHongBaoNotification 震动并解锁屏幕 ");

        }
    }

    /**
     * 收到聊天里的红包
     * */
    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
    private void handleChatListHongBao() {
        Log.d("--------------微信", "handleChatListHongBao: 收到聊天里的红包 ");
        int mode = WX_MODE_0;//TODO 定死为自动抢红包模式
        if(mode == WX_MODE_3) { //只通知模式
            return;
        }

        AccessibilityNodeInfo nodeInfo = getService().getRootInActiveWindow();
        if(nodeInfo == null) {
            return;
        }

        if(mode != WX_MODE_0) {
            boolean isMember = isMemberChatUi(nodeInfo);
            if(mode == WX_MODE_1 && isMember) {//过滤群聊
                return;
            } else if(mode == WX_MODE_2 && !isMember) { //过滤单聊
                return;
            }
        }

        List<AccessibilityNodeInfo> list = nodeInfo.findAccessibilityNodeInfosByText("领取红包");

        if(list != null && list.isEmpty()) {
            // 从消息列表查找红包
            Log.d("--------------微信", "handleChatListHongBao: 从消息列表查找红包 找到节点了 ");
            AccessibilityNodeInfo node = AccessibilityHelper.findNodeInfosByText(nodeInfo, "[微信红包]");
            if(node != null) {
                isReceivingHongbao = true;
                AccessibilityHelper.performClick(nodeInfo);
            }else{
                Log.d("--------------微信", "handleChatListHongBao: 从消息列表查找红包 没找到红包 ");
            }
        } else if(list != null) {
            Log.d("--------------微信", "handleChatListHongBao: 找到节点了 ");
            if (isReceivingHongbao){
                //最新的红包领起
                AccessibilityNodeInfo node = list.get(list.size() - 1);
                AccessibilityHelper.performClick(node);
                isReceivingHongbao = false;
                Log.d("--------------微信", "handleChatListHongBao: 最新的红包领起 ");
            }
        }
    }

    /**
     * 点击聊天里的红包后，显示的界面
     * */
    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    private void handleLuckyMoneyReceive() {
        Log.d("--------------微信", "handleLuckyMoneyReceive: 点击聊天里的红包后，显示的界面 ");
        AccessibilityNodeInfo nodeInfo = getService().getRootInActiveWindow();
        if(nodeInfo == null) {
            return;
        }

        AccessibilityNodeInfo targetNode = null;

        int event = WX_AFTER_OPEN_HONGBAO;//TODO 定死为拆红包
        int wechatVersion = getWechatVersion();
        if(event == WX_AFTER_OPEN_HONGBAO) { //拆红包
            if (wechatVersion < USE_ID_MIN_VERSION) {
                targetNode = AccessibilityHelper.findNodeInfosByText(nodeInfo, "拆红包");
            } else {
                String buttonId = "com.tencent.mm:id/b43";

                if(wechatVersion == 700) {
                    buttonId = "com.tencent.mm:id/b2c";
                }

                if(buttonId != null) {
                    targetNode = AccessibilityHelper.findNodeInfosById(nodeInfo, buttonId);
                }

                if(targetNode == null) {
                    //分别对应固定金额的红包 拼手气红包
                    AccessibilityNodeInfo textNode = AccessibilityHelper.findNodeInfosByTexts(nodeInfo, "发了一个红包", "给你发了一个红包", "发了一个红包，金额随机");

                    if(textNode != null) {
                        for (int i = 0; i < textNode.getChildCount(); i++) {
                            AccessibilityNodeInfo node = textNode.getChild(i);
                            if (BUTTON_CLASS_NAME.equals(node.getClassName())) {
                                targetNode = node;
                                break;
                            }
                        }
                    }
                }

                if(targetNode == null) { //通过组件查找
                    targetNode = AccessibilityHelper.findNodeInfosByClassName(nodeInfo, BUTTON_CLASS_NAME);
                }
            }
        } else if(event == WX_AFTER_OPEN_SEE) { //看一看
            if(getWechatVersion() < USE_ID_MIN_VERSION) { //低版本才有 看大家手气的功能
                targetNode = AccessibilityHelper.findNodeInfosByText(nodeInfo, "看看大家的手气");
            }
        } else if(event == WX_AFTER_OPEN_NONE) {
            return;
        }

        if(targetNode != null) {
            final AccessibilityNodeInfo n = targetNode;
            //延时抢红包时间设置
            long sDelayTime = 0;
            if(sDelayTime != 0) {
                new Timer().schedule(new TimerTask() {
                    @Override
                    public void run() {
                        AccessibilityHelper.performClick(n);
                    }
                },sDelayTime);
            } else {
                AccessibilityHelper.performClick(n);
            }

        }
    }

    /** 是否为群聊天*/
    private boolean isMemberChatUi(AccessibilityNodeInfo nodeInfo) {
        if(nodeInfo == null) {
            return false;
        }
        String id = "com.tencent.mm:id/ces";
        int wv = getWechatVersion();
        if(wv <= 680) {
            id = "com.tencent.mm:id/ew";
        } else if(wv <= 700) {
            id = "com.tencent.mm:id/cbo";
        }
        String title = null;
        AccessibilityNodeInfo target = AccessibilityHelper.findNodeInfosById(nodeInfo, id);
        if(target != null) {
            title = String.valueOf(target.getText());
        }
        List<AccessibilityNodeInfo> list = nodeInfo.findAccessibilityNodeInfosByText("返回");

        if(list != null && !list.isEmpty()) {
            AccessibilityNodeInfo parent = null;
            for(AccessibilityNodeInfo node : list) {
                if(!"android.widget.ImageView".equals(node.getClassName())) {
                    continue;
                }
                String desc = String.valueOf(node.getContentDescription());
                if(!"返回".equals(desc)) {
                    continue;
                }
                parent = node.getParent();
                break;
            }
            if(parent != null) {
                parent = parent.getParent();
            }
            if(parent != null) {
                if( parent.getChildCount() >= 2) {
                    AccessibilityNodeInfo node = parent.getChild(1);
                    if("android.widget.TextView".equals(node.getClassName())) {
                        title = String.valueOf(node.getText());
                    }
                }
            }
        }


        if(title != null && title.endsWith(")")) {
            return true;
        }
        return false;
    }



    public QHBListenerService getService() {
        return service;
    }
    public int getWechatVersion(){
        if(mWechatPackageInfo == null) {
            return 0;
        }
        return mWechatPackageInfo.versionCode;
    }

}
