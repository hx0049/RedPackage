package hx0049.redpackage.utils;

import android.app.KeyguardManager;
import android.app.PendingIntent;
import android.content.Context;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.PowerManager;
import android.os.Vibrator;

import java.util.Calendar;

import hx0049.redpackage.service.QHBListenerService;

/**
 * <p>Created 16/2/5 下午9:48.</p>
 * <p><a href="mailto:codeboy2013@gmail.com">Email:codeboy2013@gmail.com</a></p>
 * <p><a href="http://www.happycodeboy.com">LeonLee Blog</a></p>
 *
 * @author LeonLee
 */
public class NotifyHelper {

    private static Vibrator sVibrator;
    private static KeyguardManager sKeyguardManager;
    private static PowerManager sPowerManager;

    /**
     * 播放声音
     */
    public static void sound(Context context) {
        try {
            MediaPlayer player = MediaPlayer.create(context,
                    Uri.parse("file:///system/media/audio/ui/camera_click.ogg"));
            player.start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 振动
     */
    public static void vibrator(Context context) {
        if (sVibrator == null) {
            sVibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
        }
        sVibrator.vibrate(new long[]{100, 10, 100, 1000}, -1);
    }

    /**
     * 是否为夜间
     */
    public static boolean isNightTime() {
        Calendar cal = Calendar.getInstance();
        int hour = cal.get(Calendar.HOUR_OF_DAY);
        if (hour >= 23 || hour < 7) {
            return true;
        }
        return false;
    }

    public static KeyguardManager getKeyguardManager(Context context) {
        if (sKeyguardManager == null) {
            sKeyguardManager = (KeyguardManager) context.getSystemService(Context.KEYGUARD_SERVICE);
        }
        return sKeyguardManager;
    }

    public static PowerManager getPowerManager(Context context) {
        if (sPowerManager == null) {
            sPowerManager = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        }
        return sPowerManager;
    }

    /**
     * 是否为锁屏或黑屏状态
     */
    public static boolean isLockScreen(Context context) {
        KeyguardManager km = getKeyguardManager(context);

        return km.inKeyguardRestrictedInputMode() || !isScreenOn(context);
    }

    public static boolean isScreenOn(Context context) {
        PowerManager pm = getPowerManager(context);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT_WATCH) {
            return pm.isInteractive();
        } else {
            return pm.isScreenOn();
        }
    }

    /**
     * 播放效果、声音与震动
     */
    public static void playEffect(Context context) {
        //夜间模式，不处理
        if (NotifyHelper.isNightTime() && true) {
            return;
        }

        if (true) {
            sound(context);
        }
        if (true) {
            vibrator(context);
        }
    }

    public static void unLockScreenAndExitWeak(QHBListenerService service) {
        //得到键盘锁管理器对象
        KeyguardManager km = (KeyguardManager) service.getSystemService(Context.KEYGUARD_SERVICE);
        //参数是LogCat里用的Tag
        KeyguardManager.KeyguardLock kl = km.newKeyguardLock("unLock");
        //解锁
        kl.disableKeyguard();
        //获取电源管理器对象
        PowerManager pm = (PowerManager) service.getSystemService(Context.POWER_SERVICE);
        //获取PowerManager.WakeLock对象,后面的参数|表示同时传入两个值,最后的是LogCat里用的Tag
        PowerManager.WakeLock wl = pm.newWakeLock(PowerManager.ACQUIRE_CAUSES_WAKEUP | PowerManager.SCREEN_DIM_WAKE_LOCK, "bright");
        //点亮屏幕
        wl.acquire();
        //释放
        wl.release();
    }

    /**
     * 显示通知
     */
    public static void showNotify(Context context, String title, PendingIntent pendingIntent) {

    }

    /**
     * 执行PendingIntent事件
     */
    public static void send(PendingIntent pendingIntent) {
        try {
            pendingIntent.send();
        } catch (PendingIntent.CanceledException e) {
            e.printStackTrace();
        }
    }
}
