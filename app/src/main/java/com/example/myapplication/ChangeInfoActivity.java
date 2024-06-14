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

public class ChangeInfoActivity extends AppCompatActivity {

    String user_id,auth_id,server_url;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.change_info);

        //获取全局变量
        SharedPreferences sharedPreferences = this.getSharedPreferences("prefs", Context.MODE_PRIVATE);
        user_id = sharedPreferences.getString("user_id", "0");
        auth_id = sharedPreferences.getString("auth_id", "");
        server_url = sharedPreferences.getString("server_url", "http://10.0.2.2:8000");

        Intent intent = getIntent();
        String username = intent.getStringExtra("username");
        String motto = intent.getStringExtra("motto");

        EditText new_name = findViewById(R.id.new_name);
        EditText new_motto = findViewById(R.id.new_motto);
        new_name.setText(username);
        new_motto.setText(motto);
    }

    public void onConfirmChangeClick(View view) {
        EditText new_name_text = findViewById(R.id.new_name);
        EditText new_motto_text = findViewById(R.id.new_motto);
        String new_name = new_name_text.getText().toString();
        String new_motto = new_motto_text.getText().toString();

        new Thread() {

            @Override
            public void run() {
                HttpURLConnection connection = null;
                try {
                    URL url = new URL(server_url+"change_info/");
                    connection = (HttpURLConnection) url.openConnection();
                    connection.setConnectTimeout(3000);
                    connection.setReadTimeout(3000);
                    //设置请求方式
                    connection.setRequestMethod("POST");
                    connection.setDoInput(true);
                    connection.setDoOutput(true);
                    connection.connect();

                    String body = "username=" + new_name + "&motto="+ new_motto+"&user_id=" + user_id + "&auth_id=" + auth_id;
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
                                        AlertDialog.Builder builder = new AlertDialog.Builder(ChangeInfoActivity.this);
                                        builder.setTitle("修改用户信息结果");
                                        builder.setMessage("修改用户信息成功");

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
                                        AlertDialog.Builder builder = new AlertDialog.Builder(ChangeInfoActivity.this);
                                        builder.setTitle("修改用户信息失败");
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
