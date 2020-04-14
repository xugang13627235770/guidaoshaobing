package com.citms.gdsb;

import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/**
 * 类MyBroadcastReceiver的实现描述：TODO 类实现描述 
 * @author DELL 2020/1/7 9:14
 */
public class MyBroadcastReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        boolean isServiceRunning = false;
        if (intent.getAction().equals(Intent.ACTION_TIME_TICK)) {
            //检查Service状态
            ActivityManager manager = (ActivityManager)context.getSystemService(Context.ACTIVITY_SERVICE);
            for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
                if("com.citms.gdsb.LocalService".equals(service.service.getClassName())){
                    isServiceRunning = true;
                }

            }
            if (!isServiceRunning) {
                Intent i = new Intent(context, MyService.class);
                context.startService(i);
            }
        }
    }
}
