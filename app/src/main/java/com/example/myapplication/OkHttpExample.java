package com.example.myapplication;

import androidx.annotation.NonNull;

import java.io.File;
import java.io.IOException;

import okhttp3.*;

public class OkHttpExample {
    public static final MediaType JSON
            = MediaType.parse("application/json; charset=utf-8");

    OkHttpClient client = new OkHttpClient();

    public void post(String url, RequestBody body) throws IOException {
//        RequestBody body = RequestBody.create(json, JSON);
        Request request = new Request.Builder()
                .url(url)
                .post(body)
                .build();

        // 发送请求
        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) throw new IOException("Unexpected code " + response);

            // 获取响应体
            System.out.println(response.body().string());
        }
    }


    public void uploadFile(String url, File file) throws IOException {
        RequestBody fileBody = RequestBody.create(file, MediaType.parse("application/octet-stream"));
        MultipartBody requestBody = new MultipartBody.Builder()
                .setType(MediaType.parse("application/octet-stream"))
                .addFormDataPart("file", file.getName(), fileBody)
                .build();
        Request request = new Request.Builder()
                .url(url)
                .post(requestBody)
                .build();
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {

            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {

            }
        });
    }
}

