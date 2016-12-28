package hx0049.redpackage;

import android.annotation.TargetApi;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

import hx0049.redpackage.ui.ComAdapter;
import hx0049.redpackage.ui.ComRecyclerViewHolder;

public class MainActivity extends AppCompatActivity {

    public static final String ACTION_QIANGHONGBAO_SERVICE_DISCONNECT = "com.codeboy.qianghongbao.ACCESSBILITY_DISCONNECT";
    public static final String ACTION_QIANGHONGBAO_SERVICE_CONNECT = "com.codeboy.qianghongbao.ACCESSBILITY_CONNECT";

    public static final String ACTION_NOTIFY_LISTENER_SERVICE_DISCONNECT = "com.codeboy.qianghongbao.NOTIFY_LISTENER_DISCONNECT";
    public static final String ACTION_NOTIFY_LISTENER_SERVICE_CONNECT = "com.codeboy.qianghongbao.NOTIFY_LISTENER_CONNECT";

    private RecyclerView rvShow;
    public static long number = 0;
    public List<String> data;
    ComAdapter<String> adapter;
    private static boolean isNotificationListenerOpen;
    private static boolean isRobRedPackageServiceOpen;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        init();

        IntentFilter filter = new IntentFilter();
        filter.addAction(ACTION_QIANGHONGBAO_SERVICE_CONNECT);
        filter.addAction(ACTION_QIANGHONGBAO_SERVICE_DISCONNECT);
        filter.addAction(ACTION_NOTIFY_LISTENER_SERVICE_DISCONNECT);
        filter.addAction(ACTION_NOTIFY_LISTENER_SERVICE_CONNECT);
        registerReceiver(qhbConnectReceiver, filter);
    }


    @Override
    protected void onResume() {
        super.onResume();
//        if(QHBListenerService.isRunning()){
//
//        }else{
//            openAccessibilityServiceSettings();
//        }
    }



    private void init() {
        rvShow = (RecyclerView) findViewById(R.id.rv_show);
        data = new ArrayList<>();
        adapter = new ComAdapter<>(this);
        adapter.setData(data)
                .setItemView(android.R.layout.simple_spinner_item)
                .setShowItem(new ComAdapter.ShowItem() {
                    @Override
                    public void show(ComRecyclerViewHolder viewHolder, Object object) {
                        viewHolder.setText(android.R.id.text1, (String) object);
                    }
                })
                .setHeadView(R.layout.head_view)
                .setShowHead(new ComAdapter.ShowHead() {
                    @Override
                    public void show(ComRecyclerViewHolder viewHolder) {
                        viewHolder.getView(R.id.tv1).setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                addNotice("【点击按钮】--> 修改红包服务");
                                openAccessibilityServiceSettings();
                            }
                        });
                        viewHolder.getView(R.id.tv2).setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                addNotice("【点击按钮】--> 修改通知栏监听状态");
                                openNotificationServiceSettings();
                            }
                        });

                        if (isRobRedPackageServiceOpen) {
                            viewHolder.setText(R.id.tv1,"关闭红包服务");
                        } else {
                            viewHolder.setText(R.id.tv1,"打开红包服务");
                        }
                        if (isNotificationListenerOpen) {
                            viewHolder.setText(R.id.tv2,"关闭通知栏监听");
                        } else {
                            viewHolder.setText(R.id.tv2,"打开通知栏监听");
                        }
                    }
                })
                .loadVertical(rvShow);
        addNotice("【---打开抢红包软件---】");
        addNotice("  请打开红包服务和通知栏监听...");
    }

    private BroadcastReceiver qhbConnectReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (isFinishing()) {
                return;
            }
            String action = intent.getAction();
            Log.d("MainActivity", "receive-->" + action);
            if (ACTION_QIANGHONGBAO_SERVICE_CONNECT.equals(action)) {
                addNotice("  抢红包服务已经连接...");
                isRobRedPackageServiceOpen = true;
            } else if (ACTION_QIANGHONGBAO_SERVICE_DISCONNECT.equals(action)) {
                openAccessibilityServiceSettings();
                addNotice("  抢红包服务已断开连接...");
                isRobRedPackageServiceOpen = false;
            } else if (ACTION_NOTIFY_LISTENER_SERVICE_CONNECT.equals(action)) {
                addNotice("  通知栏服务监听已经启动...");
                isNotificationListenerOpen = true;
            } else if (ACTION_NOTIFY_LISTENER_SERVICE_DISCONNECT.equals(action)) {
                addNotice("  通知栏服务监听已经断开...");
                isNotificationListenerOpen = false;
            }
        }
    };

    public void addNotice(String notice) {
        try {
            number++;
            adapter.addData(number + " " + notice);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 打开辅助服务的设置
     */
    private void openAccessibilityServiceSettings() {
        try {
            Intent intent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
            startActivity(intent);
            addNotice("  提示：请开启【抢红包】服务...");
            if(!isRobRedPackageServiceOpen) {
                Toast.makeText(this, "找到抢红包软件，然后开启即可", Toast.LENGTH_LONG).show();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 打开通知栏设置
     */
    @TargetApi(Build.VERSION_CODES.LOLLIPOP_MR1)
    private void openNotificationServiceSettings() {
        try {
            Intent intent = new Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS);
            startActivity(intent);
            if(!isNotificationListenerOpen) {
                Toast.makeText(this, "找到抢红包软件，然后开启即可", Toast.LENGTH_LONG).show();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        try {
            unregisterReceiver(qhbConnectReceiver);
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

}
