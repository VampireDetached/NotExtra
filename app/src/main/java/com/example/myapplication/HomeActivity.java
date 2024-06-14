package com.example.myapplication;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.example.myapplication.R;
import com.google.android.material.navigation.NavigationView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedWriter;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Scanner;
import java.util.zip.Inflater;

public class HomeActivity extends AppCompatActivity {

    String user_id,auth_id,server_url;
    Fragment selectedFragment = null;
    Fragment last_fragment =null;

    NoteFragment noteFragment=null;
    MeFragment meFragment=null;
    int last_region =1;

    String TAG = "HomeActivity debug";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.home);

        //获取全局变量
        SharedPreferences sharedPreferences = this.getSharedPreferences("prefs", Context.MODE_PRIVATE);
        user_id = sharedPreferences.getString("user_id", "0");
        auth_id = sharedPreferences.getString("auth_id", "");
        server_url = sharedPreferences.getString("server_url", "http://10.0.2.2:8000/");

        BottomNavigationView navView = findViewById(R.id.navigation);


        // Set default fragment
        if (savedInstanceState == null) {
            meFragment = new MeFragment();
            getSupportFragmentManager().beginTransaction().replace(R.id.container, meFragment).commit();
            navView.setSelectedItemId(R.id.navigation_me);
        }

        navView.setOnNavigationItemSelectedListener(new BottomNavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {

                switch (item.getItemId()) {
                    case R.id.navigation_me:
                        if(last_region==1)
                            return false;
                        last_fragment =selectedFragment;
                        if(meFragment ==null) {
                            selectedFragment = new MeFragment();
                            meFragment = (MeFragment) selectedFragment;
                        }
                        else
                            selectedFragment=meFragment;

                        last_region=1;
                        break;
                    case R.id.navigation_note:
                        if(last_region==0)
                            return false;

                        if(last_fragment==null) {
                            Log.i(TAG, "1");
                            selectedFragment = new NoteFragment();
                        }
                        else
                        {
                            Log.i(TAG,"2");
                            selectedFragment = last_fragment;
                        }
                        last_region=0;
                        noteFragment = (NoteFragment) selectedFragment;
                        break;
                }

                if (selectedFragment != null) {
                    getSupportFragmentManager().beginTransaction().replace(R.id.container, selectedFragment).commit();
                }

                return true;
            }
        });

        NavigationView navigationView = findViewById(R.id.nav_view);

        // Set item click listener
        navigationView.setNavigationItemSelectedListener(new NavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                String tag = item.getTitle().toString();
                DrawerLayout drawerLayout = findViewById(R.id.drawer_layout);
                drawerLayout.closeDrawer(GravityCompat.START);

                if(item.getItemId()==0){
                    noteFragment.getAllNotes();
                }
                else {
                    noteFragment.getNotesByTag(tag);
                }
                return true;
            }
        });
    }


    public void onChangePasswordClick(View view)
    {
        Intent intent = new Intent(this, ChangePasswordActivity.class);

        // 启动新的 Activity
        startActivity(intent);
    }

    public void onChangeAvatarClick(View view)
    {
        Log.i(TAG,"start click");
        Intent intent = new Intent(Intent.ACTION_PICK, null);
        intent.setDataAndType(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, "image/*");
        Log.i(TAG,"do click");
        startActivityForResult(intent, 2);
    }

    public void onChangeInfoClick(View view)
    {
        Intent intent = new Intent(this, ChangeInfoActivity.class);
        intent.putExtra("username", meFragment.username);
        intent.putExtra("motto", meFragment.motto);
        // 启动新的 Activity
        startActivity(intent);
    }


    public void onLogOutClick(View view)
    {
        // 创建AlertDialog.Builder对象
        AlertDialog.Builder builder = new AlertDialog.Builder(HomeActivity.this);
        // 设置对话框标题
        builder.setTitle("确认");
        // 设置对话框内容
        builder.setMessage("确定要退出登录吗？");
        // 设置确定按钮
        builder.setPositiveButton("确定", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                new Thread(){
                    @Override
                    public void run() {

                        HttpURLConnection connection=null;
                        try {
                            URL url = new URL(server_url+"log_out/");
                            connection = (HttpURLConnection) url.openConnection();
                            connection.setConnectTimeout(3000);
                            connection.setReadTimeout(3000);
                            //设置请求方式
                            connection.setRequestMethod("POST");
                            connection.setDoInput(true);
                            connection.setDoOutput(true);
                            connection.connect();

                            String body = "user_id="+user_id+"&auth_id="+auth_id;
                            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(connection.getOutputStream(), "UTF-8"));
                            writer.write(body);
                            writer.close();
                            int responseCode = connection.getResponseCode();
                            if(responseCode == HttpURLConnection.HTTP_OK){
                                InputStream inputStream = connection.getInputStream();
                                Scanner scanner = new Scanner(inputStream).useDelimiter("\\A");
                                String responseStr = scanner.hasNext() ? scanner.next() : "";
                                JSONObject jsonObject = new JSONObject(responseStr);

                                SharedPreferences sharedPreferences = getSharedPreferences("prefs", Context.MODE_PRIVATE);
                                SharedPreferences.Editor editor = sharedPreferences.edit();
                                editor.putString("user_id","0");
                                editor.putString("auth_id","");
                                editor.apply();


                                Intent intent = new Intent(HomeActivity.this, WelcomeActivity.class);
                                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                                startActivity(intent);
                                finish(); // 结束当前活动
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }.start();
            }
        });
        // 设置取消按钮
        builder.setNegativeButton("取消", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                dialogInterface.dismiss();
            }
        });
        // 创建并显示对话框
        AlertDialog dialog = builder.create();
        dialog.show();



    }

    public void onNewNoteClick(View view)
    {
        Intent intent = new Intent(this, EditNoteActivity.class);
        intent.putExtra("note_id", "0");
        // 启动新的 Activity
        startActivityForResult(intent,1);
    }

    public void onTagsClick(View view)
    {
        new Thread(){

            @Override
            public void run() {
                HttpURLConnection connection=null;
                try {
                    String urlString = server_url+"get_user_tags/?user_id=" + user_id + "&auth_id=" + auth_id;
                    URL url = new URL(urlString);

                    // 打开连接
                    connection = (HttpURLConnection) url.openConnection();
                    connection.setConnectTimeout(3000);
                    connection.setReadTimeout(3000);
                    // 设置请求方式为GET
                    connection.setRequestMethod("GET");
                    connection.setDoInput(true); // 允许输入流

                    // 不需要设置 setDoOutput(true)，GET 请求不需要请求体
                    connection.connect();
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
                                JSONArray tags = jsonObject.getJSONArray("tags");
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        try {
                                            setTags(tags);
                                        } catch (JSONException e) {
                                            throw new RuntimeException(e);
                                        }
                                    }
                                });
                            }
                            else if(status.equals("refuse"))
                            {
                                Toast.makeText(HomeActivity.this, "请求失败", Toast.LENGTH_SHORT).show();
                            }
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }.start();
    }

    public void setTags(JSONArray tags) throws JSONException {
        DrawerLayout drawerLayout = findViewById(R.id.drawer_layout);
        NavigationView navigationView = findViewById(R.id.nav_view);

        Menu menu = navigationView.getMenu();
        menu.clear();

        MenuItem menuItem = menu.add(Menu.NONE, 0, Menu.NONE, "全部笔记");

        for (int i=0;i<tags.length();i++)
        {
            String tag = tags.getString(i);
            menuItem = menu.add(Menu.NONE, i+1, Menu.NONE, tag);
        }

        drawerLayout.openDrawer(GravityCompat.START);

    }
    public void onSearchNoteClick(View view)
    {
        Intent intent = new Intent(this, SearchNoteActivity.class);
        // 启动新的 Activity
        startActivity(intent);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Log.i(TAG, "result code="+ resultCode);
        if (requestCode ==1)
            noteFragment.getAllNotes();
        else if(requestCode==2)
        {
            meFragment.onActivityResult(requestCode,resultCode,data);
        }

    }

}


