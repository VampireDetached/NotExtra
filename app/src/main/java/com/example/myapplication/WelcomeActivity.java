package com.example.myapplication;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

public class WelcomeActivity extends AppCompatActivity {
    String TAG = "WelcomeActivity debug";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.welcome);

        //使用SharedPreferences保存全局变量
        SharedPreferences sharedPreferences = getSharedPreferences("prefs", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();

        if (isEmulator()) {
            editor.putString("server_url", "http://10.0.2.2:8000/");
            Log.i(TAG, "1");
        } else {
            editor.putString("server_url", "http://59.66.139.24:8000/");
            Log.i(TAG, "2");
        }
        editor.apply();

        String user_id = sharedPreferences.getString("user_id", "0");
        String auth_id = sharedPreferences.getString("auth_id", "");
        if(!user_id.equals("0"))
        {
            Intent intent = new Intent(WelcomeActivity.this, HomeActivity.class);
            startActivity(intent);
        }
    }

    public void onSignUpViewClick(View view) {
        // 创建 Intent 对象，指定从 WelcomeActivity 到 DetailActivity 的跳转
        Intent intent = new Intent(this, SignUpActivity.class);
        // 启动新的 Activity
        startActivity(intent);
    }

    public void onLogInViewClick(View view) {
        // 创建 Intent 对象，指定从 WelcomeActivity 到 DetailActivity 的跳转
        Intent intent = new Intent(this, LogInActivity.class);
        // 启动新的 Activity
        startActivity(intent);
    }

    public boolean isEmulator() {
        String brand = Build.BRAND;
        String device = Build.DEVICE;
        String model = Build.MODEL;
        String product = Build.PRODUCT;
        return (brand.startsWith("generic") || brand.equalsIgnoreCase("google") ||
                device.startsWith("generic") || model.contains("Emulator") ||
                product.contains("sdk") || product.contains("emulator") ||
                product.contains("simulator"));

    }
}
