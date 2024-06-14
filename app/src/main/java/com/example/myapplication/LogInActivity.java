package com.example.myapplication;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Scanner;

import okhttp3.OkHttpClient;


public class LogInActivity extends AppCompatActivity
{
    private OkHttpClient client = new OkHttpClient();
    String server_url;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.log_in);

        //获取全局变量
        SharedPreferences sharedPreferences = this.getSharedPreferences("prefs", Context.MODE_PRIVATE);
        server_url = sharedPreferences.getString("server_url", "http://10.0.2.2:8000/");
    }

    public void onLogInClick(View view) throws JSONException, IOException {
//        获取用户名和密码
        EditText usernameText = findViewById(R.id.log_in_username);
        EditText passwordText = findViewById(R.id.log_in_password);
        String username = usernameText.getText().toString();
        String password = passwordText.getText().toString();
        new Thread(){
            @Override
            public void run() {

                HttpURLConnection connection=null;
                try {
                    URL url = new URL(server_url+"log_in/");
                    connection = (HttpURLConnection) url.openConnection();
                    connection.setConnectTimeout(3000);
                    connection.setReadTimeout(3000);
                    //设置请求方式
                    connection.setRequestMethod("POST");
                    connection.setDoInput(true);
                    connection.setDoOutput(true);
                    connection.connect();

                    String body = "username="+username+"&password="+password;
                    BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(connection.getOutputStream(), "UTF-8"));
                    writer.write(body);
                    writer.close();
                    int responseCode = connection.getResponseCode();
                    if(responseCode == HttpURLConnection.HTTP_OK){
                        InputStream inputStream = connection.getInputStream();
                        Scanner scanner = new Scanner(inputStream).useDelimiter("\\A");
                        String responseStr = scanner.hasNext() ? scanner.next() : "";
                        JSONObject jsonObject = new JSONObject(responseStr);

                        if (jsonObject.has("status"))
                        {
                            String status = jsonObject.getString("status");
                            if(status.equals("refuse"))
                            {
                                String msg = jsonObject.getString("msg");
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        // 创建对话框
                                        AlertDialog.Builder builder = new AlertDialog.Builder(LogInActivity.this);
                                        builder.setTitle("登录失败");
                                        builder.setMessage(msg);

                                        builder.show(); // 显示对话框
                                    }
                                });
                                return;
                            }
                        }

                        //使用SharedPreferences保存全局变量
                        SharedPreferences sharedPreferences = getSharedPreferences("prefs", Context.MODE_PRIVATE);
                        SharedPreferences.Editor editor = sharedPreferences.edit();
                        if(jsonObject.has("user_id"))
                        {
                            String user_id = jsonObject.getString("user_id");
                            editor.putString("user_id", user_id);
                        }
                        if(jsonObject.has("auth_id"))
                        {
                            String auth_id = jsonObject.getString("auth_id");
                            editor.putString("auth_id", auth_id);
                        }
                        editor.apply();

                        // 启动新的 Activity
                        Intent intent = new Intent(LogInActivity.this, HomeActivity.class);
                        startActivity(intent);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }.start();
    }



}
