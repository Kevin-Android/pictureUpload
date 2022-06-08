package com.xiaosi.css.network;

import android.os.Build;

import androidx.annotation.RequiresApi;

import com.xiaosi.css.network.callback.OkHttpRequestCallBack;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * Author:一条屈依
 * Date:2021/5/31
 * Blog:https://blog.csdn.net/weixin_44758662
 */
public class OkHttpUtil {
    private OkHttpClient client;
    private static final String BASE_URL = "http://119.91.104.48:8900/"; //请求接口根地址
    private static volatile OkHttpUtil mInstance;//单利引用

    public OkHttpUtil() {
        client = new OkHttpClient();
    }

    public static OkHttpUtil getInstance() {
        OkHttpUtil util = mInstance;
        if (util == null) {
            synchronized (OkHttpUtil.class) {
                util = mInstance;
                if (util == null) {
                    util = new OkHttpUtil();
                    mInstance = util;
                }
            }
        }
        return util;
    }

    /**
     * 上传文件
     *
     * @param actionUrl 接口地址
     * @param filePath  本地文件地址
     */
    @RequiresApi(api = Build.VERSION_CODES.O)
    public <T> void upLoadFile(String actionUrl, String filePath, final OkHttpRequestCallBack callBack) {
        //补全请求地址
        String requestUrl = BASE_URL+actionUrl;

        //创建File
        File file = new File(filePath);

        MultipartBody.Builder builder = new MultipartBody.Builder();
        //设置类型
        builder.setType(MultipartBody.FORM);

        builder.addFormDataPart("image", file.getName(), RequestBody.create(file, null));

        RequestBody body = builder.build();
        //创建Request
        final Request request = new Request.Builder().url(requestUrl).post(body).build();
        final Call call = client.newBuilder().writeTimeout(50, TimeUnit.SECONDS).build().newCall(request);
        call.enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                callBack.onReqFailed(e);
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                callBack.onReqSuccess(response);
            }
        });
    }
}