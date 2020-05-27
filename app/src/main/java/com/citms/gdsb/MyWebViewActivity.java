package com.citms.gdsb;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.PendingIntent;
import android.content.ClipData;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v4.app.ActivityCompat;
import android.view.KeyEvent;
import android.webkit.*;
import android.widget.Toast;
import com.citms.gdsb.aes.AESUtils;
import com.citms.gdsb.nfc.BaseNfcActivity;
import com.citms.gdsb.utils.CheckPermissionUtils;
import com.citms.gdsb.utils.Constant;
import com.uuzuche.lib_zxing.activity.CaptureActivity;
import com.uuzuche.lib_zxing.activity.CodeUtils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

public class MyWebViewActivity extends BaseNfcActivity {

    private final static int    FILE_CHOOSER_RESULT_CODE = 10000;
    private final static int    SCAN_CHOOSER_RESULT_CODE = 20000;
    private final static String    AES_KEY = "b13df01d7efd4d4d";//aes加密的key
    //    public static final String DEFAULT_URL = "http://192.168.20.72:40046/index.html?v=";//现场
        public static final String               DEFAULT_URL              = "http://192.168.7.18:8080/index.html?v=";
//    public static final  String DEFAULT_URL              = "http://192.168.77.169:8080/index.html?v=";

    private ValueCallback<Uri>   uploadMessage;
    private ValueCallback<Uri[]> uploadMessageAboveL;
    private WebView              webview;
    private PendingIntent        pendingIntent;
    private long exitTime;//返回键退出时间

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.my_web_view);
        initPermission();
        webview = (WebView) findViewById(R.id.web_view);
        //        webview.clearCache(true);
        WebSettings settings = webview.getSettings();
        settings.setUseWideViewPort(true);
        settings.setLoadWithOverviewMode(true);
        settings.setJavaScriptEnabled(true);
        // 1.设置WebChromeClient，重写文件上传回调
        webview.setWebChromeClient(new WebChromeClient() {
            // For Android < 3.0
            public void openFileChooser(ValueCallback<Uri> valueCallback) {
                uploadMessage = valueCallback;
                openImageChooserActivity();
            }

            // For Android  >= 3.0
            public void openFileChooser(ValueCallback valueCallback, String acceptType) {
                uploadMessage = valueCallback;
                openImageChooserActivity();
            }

            //For Android  >= 4.1
            public void openFileChooser(ValueCallback<Uri> valueCallback, String acceptType,
                                        String capture) {
                uploadMessage = valueCallback;
                openImageChooserActivity();
            }

            // For Android >= 5.0
            @Override
            public boolean onShowFileChooser(WebView webView, ValueCallback<Uri[]> filePathCallback,
                                             FileChooserParams fileChooserParams) {
                uploadMessageAboveL = filePathCallback;
                openImageChooserActivity();
                return true;
            }
        });
        //加载本地网页
        webview.addJavascriptInterface(this, "android");//"android" 和h5要一致
        webview.loadUrl(DEFAULT_URL + System.currentTimeMillis());

    }

    // 2.回调方法触发本地选择文件
    private void openImageChooserActivity() {
        startActivityForResult(createDefaultOpenableIntent(), FILE_CHOOSER_RESULT_CODE);
    }

    // Create and return a chooser with the default OPENABLE
    // actions including the camera, camcorder and sound
    // recorder where available.
    private Intent createDefaultOpenableIntent() {
        Intent i = new Intent(Intent.ACTION_GET_CONTENT);
        i.addCategory(Intent.CATEGORY_OPENABLE);
        //        i.setType("*/*");

        Intent chooser = createChooserIntent(createCamcorderIntent(), createImageIntent());
        chooser.putExtra(Intent.EXTRA_INTENT, i);
        return chooser;
    }

    private Intent createChooserIntent(Intent... intents) {
        Intent chooser = new Intent(Intent.ACTION_CHOOSER);
        chooser.putExtra(Intent.EXTRA_INITIAL_INTENTS, intents);
        chooser.putExtra(Intent.EXTRA_TITLE, "文件选择");
        return chooser;
    }

    private Intent createImageIntent() {
        //        Intent i = new Intent(Intent.ACTION_GET_CONTENT);
        //        i.addCategory(Intent.CATEGORY_OPENABLE);
        //        i.setType("image/*");//图片上传
        //        i.setType("file/*");//文件上传
        //        i.setType("*/*");//文件上传
        //        return Intent.createChooser(i, "Image Chooser");
        return new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
    }

    private Intent createCamcorderIntent() {
        return new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
    }

    // 3.选择图片后处理
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (data != null && requestCode == FILE_CHOOSER_RESULT_CODE) {
            if (data.getAction() != null) {//调用照相机
                String filepath = processFile(data);
                Uri[] results = new Uri[] { Uri.fromFile(new File(filepath)) };
                uploadMessageAboveL.onReceiveValue(results);
                uploadMessageAboveL = null;
            } else {//调用图库
                onActivityResultAboveL(requestCode, resultCode, data);
            }
        } else if (data != null && requestCode == SCAN_CHOOSER_RESULT_CODE) {
            Bundle bundle = data.getExtras();
            if (bundle == null) {
                return;
            }
            String result = bundle.getString(CodeUtils.RESULT_STRING);
            webview.loadUrl("javascript:setScanResult('" + result + "')");
        } else {
            //这里uploadMessage跟uploadMessageAboveL在不同系统版本下分别持有了
            //WebView对象，在用户取消文件选择器的情况下，需给onReceiveValue传null返回值
            //否则WebView在未收到返回值的情况下，无法进行任何操作，文件选择器会失效
            if (uploadMessage != null) {
                uploadMessage.onReceiveValue(null);
                uploadMessage = null;
            } else if (uploadMessageAboveL != null) {
                uploadMessageAboveL.onReceiveValue(null);
                uploadMessageAboveL = null;
            }
        }
    }

    private String processFile(Intent data) {
        //获得图片原始的地址   没有 压缩
        Bitmap bi = (Bitmap) data.getExtras().get("data");
        //将图片保存到SD卡中
        FileOutputStream b = null;
        File file = new File(
                Environment.getExternalStorageDirectory().getAbsolutePath() + Constant.PATH);
        //判断文件是否存在
        if (!file.exists()) {
            file.mkdirs();// 创建文件夹
        }
        String filename = file.getPath() + "/" + System.currentTimeMillis() + ".jpg";
        try {
            b = new FileOutputStream(filename);
            //写入文件夹
            bi.compress(Bitmap.CompressFormat.JPEG, 100, b);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } finally {
            try {
                b.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return filename;
    }

    // 4. 选择内容回调到Html页面
    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private void onActivityResultAboveL(int requestCode, int resultCode, Intent intent) {
        if (requestCode != FILE_CHOOSER_RESULT_CODE || uploadMessageAboveL == null)
            return;
        Uri[] results = null;
        if (resultCode == Activity.RESULT_OK) {
            if (intent != null) {
                String dataString = intent.getDataString();
                ClipData clipData = intent.getClipData();
                if (clipData != null) {
                    results = new Uri[clipData.getItemCount()];
                    for (int i = 0; i < clipData.getItemCount(); i++) {
                        ClipData.Item item = clipData.getItemAt(i);
                        results[i] = item.getUri();
                    }
                }
                if (dataString != null) {
                    results = new Uri[] { Uri.parse(dataString) };
                }

            }
        }
        uploadMessageAboveL.onReceiveValue(results);
        uploadMessageAboveL = null;
    }

    //注解方法 注意:doScan()一定要和h5里面的 android.doScan();方法一致
    @JavascriptInterface
    public void doScan() {
        //打开扫描界面扫描条形码或二维码
        Intent openCameraIntent = new Intent(MyWebViewActivity.this, CaptureActivity.class);
        startActivityForResult(openCameraIntent, SCAN_CHOOSER_RESULT_CODE);

    }

    //    /**
    //     * 集成APP时，获取用户信息时调用
    //     */
    //    @JavascriptInterface
    //    public void initUserInfo() {
    //        runOnUiThread(new Runnable() {
    //            @Override
    //            public void run() {
    //                try {
    //                    Map<String, String> map = UserInfo.getUserInfo(MyWebViewActivity.this);
    //                    String result = "";
    //                    if (map != null) {
    //                        JSONObject object = new JSONObject(map);
    //                        result = object.toString();
    //                    } else {
    //                        result = "map is null";
    //                    }
    //                    webview.loadUrl("javascript:setUserInfo('" + result + "')");
    //                } catch (Exception e) {
    //                    webview.loadUrl("javascript:setUserInfo('" + e.getMessage() + "')");
    //                }
    //
    //            }
    //        });
    //    }

    /**
     * 传入userId并将userId保存到本地
     */
    @JavascriptInterface
    public void saveUserIdByPhone(Long userId) {
        //步骤1：创建一个SharedPreferences对象
        SharedPreferences sharedPreferences = getSharedPreferences(Constant.PREFERENCE_NAME,
                Context.MODE_PRIVATE);
        //步骤2： 实例化SharedPreferences.Editor对象
        SharedPreferences.Editor editor = sharedPreferences.edit();
        //步骤3：将获取过来的值放入文件
        editor.putLong(Constant.PREFERENCE_USER_ID, userId);
        //步骤4：提交
        editor.commit();
    }

    /**
     * 初始化权限事件
     */
    private void initPermission() {
        //检查权限
        String[] permissions = CheckPermissionUtils.checkPermission(this);
        if (permissions.length == 0) {
            //权限都申请了
            //是否登录
        } else {
            //申请权限
            ActivityCompat.requestPermissions(this, permissions, 100);
        }
    }

    /**
     * NFC读取标签
     * @param intent
     */
    @Override
    public void onNewIntent(Intent intent) {
        String aesTag = readNfcTag(intent);
        String nfcTag = AESUtils.decryptData(AES_KEY, aesTag);
//        Toast.makeText(this,nfcTag,Toast.LENGTH_LONG).show();
        webview.loadUrl("javascript:processNfcCard('" + nfcTag + "')");
    }

    /**
     * 监听返回键
     */
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if(keyCode == KeyEvent.KEYCODE_BACK){
            String originalUrl = webview.getUrl();
            if(originalUrl.lastIndexOf("#/attendance") == originalUrl.length()-12
               || originalUrl.lastIndexOf("#/case") == originalUrl.length()-6
               || originalUrl.lastIndexOf("#/task") == originalUrl.length()-6){
                webview.loadUrl("javascript:backHome()");
            }
            else if(originalUrl.lastIndexOf("#/nav") == originalUrl.length()-5
                || originalUrl.lastIndexOf("#/") == originalUrl.length()-2){
                exitApp();
            }else if(webview.canGoBack()){
                webview.goBack();
            }
        }
       return false;
    }

    /**
     * 退出app处理
     */
    private void exitApp() {
        if ((System.currentTimeMillis() - exitTime)> 2000) { //System.currentTimeMillis()无论何时调用，肯定大于2000
            exitTime = System.currentTimeMillis();
            Toast.makeText(this, "请再按一次退出程序", Toast.LENGTH_SHORT).show();
        } else {
            //退出应用
            finish();
            System.exit(0);
        }
    }
}