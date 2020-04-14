package com.citms.gdsb;

import android.app.Application;
import com.uuzuche.lib_zxing.activity.ZXingLibrary;

public class App extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        ZXingLibrary.initDisplayOpinion(this);

//        IntentFilter filter = new IntentFilter(Intent.ACTION_TIME_TICK);
//        MyBroadcastReceiver receiver = new MyBroadcastReceiver();
//        registerReceiver(receiver, filter);
//
//        Intent i = new Intent(this, MyService.class);
//        this.startService(i);
    }

}
