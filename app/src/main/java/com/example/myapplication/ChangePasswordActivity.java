package com.example.myapplication;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONObject;

import java.io.BufferedWriter;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Scanner;

public class ChangePasswordActivity extends AppCompatActivity {

    String user_id,auth_id,server_url;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.change_password);

        //获取全局变量
        SharedPreferences sharedPreferences = this.getSharedPreferences("prefs", Context.MODE_PRIVATE);
        user_id = sharedPreferences.getString("user_id", "0");
        auth_id = sharedPreferences.getString("auth_id", "");
        server_url = sharedPreferences.getString("server_url", "http://10.0.2.2:8000");

    }

    public void onConfirmChangeClick(View view) {
        EditText old_pw_text = findViewById(R.id.old_pw);
        EditText new_pw_text = findViewById(R.id.new_pd);
        EditText new_pw_confirm_text = findViewById(R.id.new_pd_confirm);
        String old_pw = old_pw_text.getText().toString();
        String new_pw = new_pw_text.getText().toString();
        String new_pw_confirm = new_pw_confirm_text.getText().toString();
        if (!new_pw.equals(new_pw_confirm)) {
            // 提示两次密码不一致
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("提示");  // 设置对话框标题
            builder.setMessage("两次输入的新密码不一致");  // 设置对话框消息内容
            // 创建并显示对话框
            AlertDialog dialog = builder.create();
            dialog.show();
        } else if (new_pw.equals("")) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("提示");  // 设置对话框标题
            builder.setMessage("密码不能为空");  // 设置对话框消息内容
            // 创建并显示对话框
            AlertDialog dialog = builder.create();
            dialog.show();

        } else {
            new Thread() {

                @Override
                public void run() {
                    HttpURLConnection connection = null;
                    try {
                        URL url = new URL(server_url+"change_password/");
                        connection = (HttpURLConnection) url.openConnection();
                        connection.setConnectTimeout(3000);
                        connection.setReadTimeout(3000);
                        //设置请求方式
                        connection.setRequestMethod("POST");
                        connection.setDoInput(true);
                        connection.setDoOutput(true);
                        connection.connect();

                        String body = "user_id="+user_id+"&auth_id="+auth_id+"&old_password="+old_pw+"&new_password="+new_pw;
                        BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(connection.getOutputStream(), "UTF-8"));
                        writer.write(body);
                        writer.close();
                        int responseCode = connection.getResponseCode();
                        if (responseCode == HttpURLConnection.HTTP_OK) {
                            InputStream inputStream = connection.getInputStream();
                            Scanner scanner = new Scanner(inputStream).useDelimiter("\\A");
                            String responseStr = scanner.hasNext() ? scanner.next() : "";
                            JSONObject jsonObject = new JSONObject(responseStr);
                            if (jsonObject.has("status")) {
                                String status = jsonObject.getString("status");
                                if (status.equals("success")) {
                                    runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            // 创建对话框
                                            AlertDialog.Builder builder = new AlertDialog.Builder(ChangePasswordActivity.this);
                                            builder.setTitle("修改密码结果");
                                            builder.setMessage("修改密码成功");

                                            // 设置对话框关闭监听器
                                            builder.setOnDismissListener(new DialogInterface.OnDismissListener() {
                                                @Override
                                                public void onDismiss(DialogInterface dialog) {
                                                    // 对话框关闭后调用 finish()
                                                    finish();
                                                    Intent intent = new Intent(getApplicationContext(), HomeActivity.class);
                                                    startActivity(intent);
                                                }
                                            });

                                            builder.show(); // 显示对话框
                                        }
                                    });
                                } else if (status.equals("refuse")) {
                                    String refusereason = jsonObject.getString("msg");
                                    runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            // 创建对话框
                                            AlertDialog.Builder builder = new AlertDialog.Builder(ChangePasswordActivity.this);
                                            builder.setTitle("修改密码错误");
                                            builder.setMessage(refusereason);

                                            builder.show(); // 显示对话框
                                        }
                                    });
                                }
                            }
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }.start();
        }
    }
}
