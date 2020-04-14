package com.citms.gdsb;

import android.annotation.TargetApi;
import android.app.*;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Build;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import com.citms.gdsb.utils.Constant;
import com.citms.gdsb.utils.HttpClient;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * 类MyService的实现描述：TODO 类实现描述 
 * @author DELL 2020/1/7 8:57
 */
public class MyService extends Service {

    private int one;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        new Thread(new Runnable() {
            @Override
            public void run() {
                while (true){
                    try {
                        getNotifyInfo();
                        showNotify("通知标题","通知内容");
                    } catch (Exception e) {
                        e.printStackTrace();
                    }finally {
                        one++;
                        try {
                            Thread.sleep(10 * 1000);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        }).start();
    }

    private void getNotifyInfo() throws JSONException {
        String result = HttpClient.doGet(Constant.NOTIFY_WARN_URL);
        if(result != null){
            JSONObject jsonObject = new JSONObject(result);
            String code = jsonObject.getString("code");
            if("0".equals(code)){
                Log.i("infomationHaha", one + ",result = "+result);
            }
        }

        SharedPreferences sharedPreferences= getSharedPreferences(Constant.PREFERENCE_NAME, Context.MODE_PRIVATE);
        Long userId = sharedPreferences.getLong(Constant.PREFERENCE_USER_ID,0L);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        Intent intent = new Intent(Intent.ACTION_TIME_TICK);
        sendBroadcast(intent);
        super.onDestroy();
    }

    @TargetApi(Build.VERSION_CODES.O)
    private void showNotify(String title, String content){
        Context context = MyService.this;
        String CHANNEL_ID = "channel_id";   //通道渠道id
        String  CHANEL_NAME = "chanel_name"; //通道渠道名称
        NotificationChannel channel = null;
        if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.O){
            //创建 通知通道  channelid和channelname是必须的（自己命名就好）
            channel = new NotificationChannel(CHANNEL_ID, CHANEL_NAME, NotificationManager.IMPORTANCE_DEFAULT);
            channel.enableLights(true);//是否在桌面icon右上角展示小红点
            channel.setLightColor(Color.GREEN);//小红点颜色
            channel.setShowBadge(false); //是否在久按桌面图标时显示此渠道的通知
        }

        Intent intent = new Intent(this,MyWebViewActivity.class);//即跳转到本类
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK| Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);//关键的一步，设置启动模式
        PendingIntent pendingIntent = PendingIntent.getActivity(this,0,intent,PendingIntent.FLAG_UPDATE_CURRENT);

        Notification notification;
        //获取Notification实例   获取Notification实例有很多方法处理    在此我只展示通用的方法（虽然这种方式是属于api16以上，但是已经可以了，毕竟16以下的Android机很少了，如果非要全面兼容可以用）
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
            //向上兼容 用Notification.Builder构造notification对象
            notification = new Notification.Builder(context,CHANNEL_ID)
                    .setAutoCancel(true)
                    .setContentTitle(title)
                    .setContentText(content)
                    .setWhen(System.currentTimeMillis())
                    .setSmallIcon(R.drawable.ic_launcher)
                    .setColor(Color.parseColor("#FEDA26"))
                    .setLargeIcon(BitmapFactory
                            .decodeResource(context.getResources(),R.mipmap.ic_launcher))
                    .setTicker(context.getResources().getString(R.string.app_name))
                    .setContentIntent(pendingIntent)
                    .build();
        }else {
            //向下兼容 用NotificationCompat.Builder构造notification对象
            notification = new NotificationCompat.Builder(context)
                    .setAutoCancel(true)
                    .setContentTitle(title)
                    .setContentText(content)
                    .setWhen(System.currentTimeMillis())
                    .setSmallIcon(R.drawable.ic_launcher)
                    .setColor(Color.parseColor("#FEDA26"))
                    .setLargeIcon(BitmapFactory.decodeResource(context.getResources(),R.mipmap.ic_launcher))
                    .setTicker(context.getResources().getString(R.string.app_name))
                    .setContentIntent(pendingIntent)
                    .build();
        }


        //发送通知
        int  notifiId=1;
        //创建一个通知管理器
        NotificationManager notificationManager= (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.O){
            notificationManager.createNotificationChannel(channel);
        }
        notificationManager.notify(notifiId,notification);

    }
}
