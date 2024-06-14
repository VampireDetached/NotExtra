package com.example.myapplication;

import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.myapplication.ui.theme.HttpPostRequest;

import org.json.JSONException;
import org.json.JSONObject;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.CookieJar;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.TimeUnit;
import java.util.Scanner;



public class SignUpActivity extends AppCompatActivity
{
    private OkHttpClient client = new OkHttpClient();
    String server_url;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.sign_up);

        //获取全局变量
        SharedPreferences sharedPreferences = this.getSharedPreferences("prefs", Context.MODE_PRIVATE);
        server_url = sharedPreferences.getString("server_url", "http://10.0.2.2:8000");
        Log.i("SignUpActivity debug",server_url);
    }

    public void onSignUpClick(View view) throws JSONException, IOException {
//        获取用户名和密码
        EditText usernameText = findViewById(R.id.username);
        EditText passwordText = findViewById(R.id.password);
        EditText password_confirmText = findViewById(R.id.password_confirm);
        String username = usernameText.getText().toString();
        String password = passwordText.getText().toString();
        String passwordconfirm = password_confirmText.getText().toString();
        if(!password.equals(passwordconfirm)){
            // 提示两次密码不一致
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("提示");  // 设置对话框标题
            builder.setMessage("两次输入的密码不一致");  // 设置对话框消息内容
            // 创建并显示对话框
            AlertDialog dialog = builder.create();
            dialog.show();
        } else if (username.equals("") || password.equals("")) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("提示");  // 设置对话框标题
            builder.setMessage("用户名和密码不能为空");  // 设置对话框消息内容
            // 创建并显示对话框
            AlertDialog dialog = builder.create();
            dialog.show();

        } else
        {
            new Thread(){

                @Override
                public void run() {
                    HttpURLConnection connection=null;
                    try {
                        URL url = new URL(server_url+"sign_up/");
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
                            if (jsonObject.has("status")) {
                                String status = jsonObject.getString("status");
                                if(status.equals("success"))
                                {
                                    runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            // 创建对话框
                                            AlertDialog.Builder builder = new AlertDialog.Builder(SignUpActivity.this);
                                            builder.setTitle("注册结果");
                                            builder.setMessage("注册成功");

                                            // 设置对话框关闭监听器
                                            builder.setOnDismissListener(new DialogInterface.OnDismissListener() {
                                                @Override
                                                public void onDismiss(DialogInterface dialog) {
                                                    // 对话框关闭后调用 finish()
                                                    finish();
                                                }
                                            });

                                            builder.show(); // 显示对话框
                                        }
                                    });
                                }
                                else if(status.equals("refuse"))
                                {
                                    String refusereason = jsonObject.getString("msg");
                                    runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            // 创建对话框
                                            AlertDialog.Builder builder = new AlertDialog.Builder(SignUpActivity.this);
                                            builder.setTitle("注册结果");
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
