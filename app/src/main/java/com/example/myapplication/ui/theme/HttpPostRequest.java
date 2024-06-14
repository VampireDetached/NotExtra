package com.example.myapplication.ui.theme;

import android.os.AsyncTask;
import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class HttpPostRequest extends AsyncTask<String, Void, String> {

    @Override
    protected String doInBackground(String... params) {
        String urlString = params[0];
        String postData = params[1];
        String response = "";

        try {
            // 创建 URL 对象
            URL url = new URL(urlString);

            // 创建 HttpURLConnection 对象
            HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.setRequestMethod("POST");
            urlConnection.setReadTimeout(10000); // 读取超时时间
            urlConnection.setConnectTimeout(15000); // 连接超时时间
            urlConnection.setDoOutput(true); // 允许输出

            // 设置请求体
            byte[] postDataBytes = postData.getBytes(StandardCharsets.UTF_8);
            urlConnection.setFixedLengthStreamingMode(postDataBytes.length);
            urlConnection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded;charset=UTF-8");

            // 发送 POST 数据
            try (OutputStream os = urlConnection.getOutputStream()) {
                os.write(postDataBytes);
            }

            // 获取服务器响应
            BufferedReader reader = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                response += line;
            }

            // 关闭连接
            urlConnection.disconnect();

        } catch (IOException e) {
            Log.e("HTTP POST Request", "Error: " + e.getMessage());
        }

        return response;
    }

    @Override
    protected void onPostExecute(String result) {
        super.onPostExecute(result);
        // 在请求完成后处理返回的结果，例如更新 UI 或执行其他操作
        Log.d("HTTP POST Response", result);
    }
}
