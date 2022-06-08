package com.xiaosi.css;

import android.Manifest;
import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.xiaosi.css.network.HttpUtils;
import com.xiaosi.css.network.OkHttpUtil;
import com.xiaosi.css.network.callback.OkHttpRequestCallBack;
import com.xiaosi.css.utils.PathHelper;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import okhttp3.Response;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    private Button btnPhoto, btnSelect;
    private Intent intent;
    private final int CAMERA = 1;//事件枚举(可以自定义)
    private final int CHOOSE = 2;//事件枚举(可以自定义)
    private final String postUrl = "http://119.91.104.48:8900/upload";//接收上传图片的地址
    String photoPath = "";//要上传的图片路径
    private final int permissionCode = 100;//权限请求码

    String[] permissions = new String[]{
            Manifest.permission.CAMERA,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.ACCESS_NETWORK_STATE,
            Manifest.permission.ACCESS_WIFI_STATE,
            Manifest.permission.INTERNET
    };
    AlertDialog alertDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        //6.0才用动态权限
        if (Build.VERSION.SDK_INT >= 23) {
            checkPermission();
        }

        btnPhoto = findViewById(R.id.btnPhoto);
        btnSelect = findViewById(R.id.btnSelect);
        btnPhoto.setOnClickListener(this);
        btnSelect.setOnClickListener(this);
    }

    //检查权限
    private void checkPermission() {
        List<String> permissionList = new ArrayList<>();
        for (int i = 0; i < permissions.length; i++) {
            if (ContextCompat.checkSelfPermission(this, permissions[i]) != PackageManager.PERMISSION_GRANTED) {
                permissionList.add(permissions[i]);
            }
        }
        if (permissionList.size() <= 0) {
            //说明权限都已经通过，可以做你想做的事情去

        } else {
            //存在未允许的权限
            ActivityCompat.requestPermissions(this, permissions, permissionCode);
        }
    }

    //授权后回调函数
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        boolean haspermission = false;
        if (permissionCode == requestCode) {
            for (int i = 0; i < grantResults.length; i++) {
                if (grantResults[i] == -1) {
                    haspermission = true;
                }
            }
            if (haspermission) {
                //跳转到系统设置权限页面，或者直接关闭页面，不让他继续访问
                permissionDialog();
            } else {
                //全部权限通过，可以进行下一步操作
            }
        }
    }

    //打开手动设置应用权限
    private void permissionDialog() {
        if (alertDialog == null) {
            alertDialog = new AlertDialog.Builder(this)
                    .setTitle("提示信息")
                    .setMessage("当前应用缺少必要权限，该功能暂时无法使用。如若需要，请单击【确定】按钮前往设置中心进行权限授权。")
                    .setPositiveButton("设置", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            cancelPermissionDialog();
                            Uri packageURI = Uri.parse("package:" + getPackageName());
                            Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, packageURI);
                            startActivity(intent);
                        }
                    })
                    .setNegativeButton("取消", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            cancelPermissionDialog();
                        }
                    })
                    .create();
        }
        alertDialog.show();
    }

    //用户取消授权
    private void cancelPermissionDialog() {
        alertDialog.cancel();
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {

            //拍照按钮事件
            case R.id.btnPhoto:
                //方法一：这样拍照只能取到缩略图（不清晰）
                //intent = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
                //startActivityForResult(intent, CAMERA);


                //方法二：指定加载路径图片路径（保存原图，清晰）
                String SD_PATH = Environment.getExternalStorageDirectory().getPath() + "/xiaosi/";
                SimpleDateFormat format = new SimpleDateFormat("yyyyMMddHHmmss");
                String fileName = format.format(new Date(System.currentTimeMillis())) + ".JPEG";
                photoPath = SD_PATH + fileName;
                File file = new File(photoPath);
                if (!file.getParentFile().exists()) {
                    file.getParentFile().mkdirs();
                }

                //兼容7.0以上的版本
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    try {
                        ContentValues values = new ContentValues(1);
                        values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpg");
                        values.put(MediaStore.Images.Media.DATA, photoPath);
                        Uri tempuri = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
                        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                        if (tempuri != null) {
                            intent.putExtra(MediaStore.EXTRA_OUTPUT, tempuri);
                            intent.putExtra(MediaStore.EXTRA_VIDEO_QUALITY, 1);
                        }
                        startActivityForResult(intent, CAMERA);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                } else {
                    intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                    Uri uri = Uri.fromFile(file);
                    intent.putExtra(MediaStore.EXTRA_OUTPUT, uri); //指定拍照后的存储路径，保存原图
                    startActivityForResult(intent, CAMERA);
                }
                break;

            //选择按钮事件
            case R.id.btnSelect:
                intent = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                startActivityForResult(intent, CHOOSE);
                break;
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            // 调用照相机拍照
            case CAMERA:
                if (resultCode == RESULT_OK) {
                    //对应方法一：图片未保存，需保存文件到本地
//                    Bundle bundle = data.getExtras();
//                    Bitmap bitmap = (Bitmap) bundle.get("data");
//                    String savePath;
//                    String SD_PATH = Environment.getExternalStorageDirectory().getPath() + "/拍照上传示例/";
//                    SimpleDateFormat format = new SimpleDateFormat("yyyyMMddHHmmss");
//                    String fileName = format.format(new Date(System.currentTimeMillis())) + ".JPEG";
//                    if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
//                        savePath = SD_PATH;
//                    } else {
//                        Toast.makeText(MainActivity.this, "保存失败！", Toast.LENGTH_SHORT).show();
//                        return;
//                    }
//                    photoPath = savePath + fileName;
//                    File file = new File(photoPath);
//                    try {
//                        if (!file.exists()) {
//                            file.getParentFile().mkdirs();
//                            file.createNewFile();
//                        }
//                        FileOutputStream stream = new FileOutputStream(file);
//                        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream);
//                        Toast.makeText(MainActivity.this, "保存成功,位置:" + file.getAbsolutePath(), Toast.LENGTH_SHORT).show();
//                    } catch (IOException e) {
//                        e.printStackTrace();
//                    }

                    //对应方法二：图片已保存，只需读取就行了
                    try {
                        FileInputStream stream = new FileInputStream(photoPath);
                        Bitmap bitmap = BitmapFactory.decodeStream(stream);

                        //预览图片
                        ImageView image = findViewById(R.id.imageView);
                        image.setImageBitmap(bitmap);
                        //上传图片（Android 4.0 之后不能在主线程中请求HTTP请求）
                        File file = new File(photoPath);
                        OkHttpRequestCallBack callBack = null;
                        if (file.exists()) {
                            new Thread(() -> {
                                OkHttpUtil.getInstance().upLoadFile("upload", photoPath, new OkHttpRequestCallBack() {
                                    @Override
                                    public void onReqSuccess(Response response) throws IOException {
                                        String str = response.body().string();
                                        if (str.equals("Ok")) {
                                            Log.d("TAG", "上传成功:" + str);
                                        }
                                    }

                                    @Override
                                    public void onReqFailed(IOException e) {
                                        Log.d("TAG", "onReqFailed: 上传失败" + e.toString());
                                    }
                                });
                                System.out.println(callBack); //OkHttp
                                //HttpUtils.uploadFile(postUrl,photoPath,"image");  //HttpUrlConnection
                            }).start();
                        }
                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                    }
                }
                break;
            // 选择图片库的图片
            case CHOOSE:
                if (resultCode == RESULT_OK) {
                    try {
                        Uri uri = data.getData();
                        photoPath = PathHelper.getRealPathFromUri(MainActivity.this, uri);
                        Bitmap bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), uri);

                        //压缩图片
                        bitmap = scaleBitmap(bitmap, (float) 0.5);

                        //预览图片
                        ImageView image = findViewById(R.id.imageView);
                        image.setImageBitmap(bitmap);

                        //上传图片（Android 4.0 之后不能在主线程中请求HTTP请求）
                        File file = new File(photoPath);
                        if (file.exists()) {
                            new Thread(new Runnable() {
                                @Override
                                public void run() {
                                    HttpUtils.uploadFile(postUrl, photoPath, "image");
                                }
                            }).start();
                        }

                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                break;
        }
    }

    //压缩图片
    public Bitmap scaleBitmap(Bitmap origin, float ratio) {
        if (origin == null) {
            return null;
        }
        int width = origin.getWidth();
        int height = origin.getHeight();
        Matrix matrix = new Matrix();
        matrix.preScale(ratio, ratio);
        Bitmap newBM = Bitmap.createBitmap(origin, 0, 0, width, height, matrix, false);
        return newBM;
    }
}