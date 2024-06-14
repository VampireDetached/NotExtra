package com.example.myapplication;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.android.flexbox.FlexboxLayout;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedWriter;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Scanner;

public class ChangeTagsActivity extends AppCompatActivity {
    String user_id,auth_id,server_url,note_id;
    JSONArray user_tags,note_tags;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.change_tags);

        //获取全局变量
        SharedPreferences sharedPreferences = this.getSharedPreferences("prefs", Context.MODE_PRIVATE);
        user_id = sharedPreferences.getString("user_id", "0");
        auth_id = sharedPreferences.getString("auth_id", "");
        server_url = sharedPreferences.getString("server_url", "http://10.0.2.2:8000/");

        Intent intent = getIntent();
        note_id = intent.getStringExtra("note_id");

        getTags();
    }

    public void getTags()
    {
        new Thread(){

            @Override
            public void run() {
                HttpURLConnection connection=null;
                try {
                    String urlString = server_url+"get_tags/?user_id=" + user_id + "&auth_id=" + auth_id+"&note_id="+note_id;
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
                                user_tags = jsonObject.getJSONArray("user_tags");
                                note_tags = jsonObject.getJSONArray("note_tags");
                                Log.i("tagslog", String.valueOf(note_tags.length()));
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        try {
                                            setNoteTags();
                                            setUserTags();

                                        } catch (JSONException e) {
                                            throw new RuntimeException(e);
                                        }
                                    }
                                });
                            }
                            else if(status.equals("refuse"))
                            {
                                Toast.makeText(ChangeTagsActivity.this, "请求失败", Toast.LENGTH_SHORT).show();
                            }
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }.start();
    }

    public void setUserTags() throws JSONException {
        FlexboxLayout user_tags_layout = findViewById(R.id.user_tags_container);
        user_tags_layout.removeAllViews();  // Clear any existing views

        int backgroundColor1 = ContextCompat.getColor(this, R.color.gray);
        int backgroundColor2 = ContextCompat.getColor(this, R.color.blue);
        int textColor = ContextCompat.getColor(this, R.color.black);

        for (int i = 0; i < user_tags.length(); i++) {
            String tags = user_tags.getString(i);

            TextView tagsView = new TextView(this);
            tagsView.setText(tags);
            tagsView.setTextSize(18f);
            tagsView.setPadding(20, 10, 20, 10);
            tagsView.setTextColor(textColor);
            tagsView.setTag("old");

            // Set margins for the tags
            FlexboxLayout.LayoutParams params = new FlexboxLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    100
            );
            params.setMargins(10, 10, 10, 10);  // Margins around each tag
            tagsView.setLayoutParams(params);

            GradientDrawable background = new GradientDrawable();
            if(containsString(note_tags,tags))
                background.setColor(backgroundColor2);
            else
                background.setColor(backgroundColor1);
            background.setCornerRadius(50f);

            tagsView.setBackground(background);

            // 点击事件
            tagsView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    String tag = tagsView.getText().toString();
                    new_note_tag(tag,tagsView);
                }
            });

            // 长按事件
            tagsView.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    String tag = tagsView.getText().toString();
                    showTagMenu(v,tag,"user");
                    return true;
                }
            });

            user_tags_layout.addView(tagsView);
        }

    }

    public void setNoteTags() throws JSONException {
        FlexboxLayout note_tags_layout = findViewById(R.id.note_tags_container);
        note_tags_layout.removeAllViews();  // Clear any existing views

        int backgroundColor = ContextCompat.getColor(this, R.color.blue);
        int textColor = ContextCompat.getColor(this, R.color.black);

        for (int i = 0; i < note_tags.length(); i++) {
            String tags = note_tags.getString(i);

            TextView tagsView = new TextView(this);
            tagsView.setText(tags);
            tagsView.setTextSize(18f);
            tagsView.setPadding(20, 10, 20, 10);
            tagsView.setTextColor(textColor);

            // Set margins for the tags
            FlexboxLayout.LayoutParams params = new FlexboxLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    100
            );
            params.setMargins(10, 10, 10, 10);  // Margins around each tag
            tagsView.setLayoutParams(params);

            GradientDrawable background = new GradientDrawable();
            background.setColor(backgroundColor);  // Background color of the tag
            background.setCornerRadius(50f);  // Corner radius to make it look like a capsule

            tagsView.setBackground(background);

            // 长按事件
            tagsView.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    String tag = tagsView.getText().toString();
                    showTagMenu(v,tag,"note");
                    return true;
                }
            });

            note_tags_layout.addView(tagsView);
        }
    }

    public void new_note_tag(String tag, TextView tagsView)
    {
        new Thread(){
            @Override
            public void run() {

                HttpURLConnection connection=null;
                try {
                    URL url = new URL(server_url+"change_note_tag/");
                    connection = (HttpURLConnection) url.openConnection();
                    connection.setConnectTimeout(3000);
                    connection.setReadTimeout(3000);
                    //设置请求方式
                    connection.setRequestMethod("POST");
                    connection.setDoInput(true);
                    connection.setDoOutput(true);
                    connection.connect();

                    String body = "user_id="+user_id+"&auth_id="+auth_id+"&tag="+tag+"&operation="+"add"+"&note_id="+note_id;
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
                            if(status.equals("success"))
                            {
//                                runOnUiThread(new Runnable() {
//                                    @Override
//                                    public void run() {
//                                        GradientDrawable background = new GradientDrawable();
//                                        background.setColor(ContextCompat.getColor(ChangeTagsActivity.this, R.color.blue));
//                                        background.setCornerRadius(50f);
//                                        tagsView.setBackground(background);
//
//                                        note_tags.put(tag);
//                                        try {
//                                            setNoteTags();
//                                        } catch (JSONException e) {
//                                            throw new RuntimeException(e);
//                                        }
//                                    }
//                                });
                                getTags();
                            }
                            if(status.equals("refuse"))
                            {
                                String msg = jsonObject.getString("msg");
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        Toast.makeText(ChangeTagsActivity.this,msg, Toast.LENGTH_SHORT).show();
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

    public void delete_note_tag(String tag)
    {
        new Thread(){
            @Override
            public void run() {

                HttpURLConnection connection=null;
                try {
                    URL url = new URL(server_url+"change_note_tag/");
                    connection = (HttpURLConnection) url.openConnection();
                    connection.setConnectTimeout(3000);
                    connection.setReadTimeout(3000);
                    //设置请求方式
                    connection.setRequestMethod("POST");
                    connection.setDoInput(true);
                    connection.setDoOutput(true);
                    connection.connect();

                    String body = "user_id="+user_id+"&auth_id="+auth_id+"&tag="+tag+"&operation="+"delete"+"&note_id="+note_id;
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
                            if(status.equals("success"))
                            {
                                getTags();
                            }
                            if(status.equals("refuse"))
                            {
                                String msg = jsonObject.getString("msg");
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        Toast.makeText(ChangeTagsActivity.this,msg, Toast.LENGTH_SHORT).show();
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

    public void delete_user_tag(String tag)
    {
        new Thread(){
            @Override
            public void run() {

                HttpURLConnection connection=null;
                try {
                    URL url = new URL(server_url+"change_user_tag/");
                    connection = (HttpURLConnection) url.openConnection();
                    connection.setConnectTimeout(3000);
                    connection.setReadTimeout(3000);
                    //设置请求方式
                    connection.setRequestMethod("POST");
                    connection.setDoInput(true);
                    connection.setDoOutput(true);
                    connection.connect();

                    String body = "user_id="+user_id+"&auth_id="+auth_id+"&tag="+tag+"&operation="+"delete";
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
                            if(status.equals("success"))
                            {
                                getTags();
                            }
                            if(status.equals("refuse"))
                            {
                                String msg = jsonObject.getString("msg");
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        Toast.makeText(ChangeTagsActivity.this,msg, Toast.LENGTH_SHORT).show();
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

    public void new_user_tag(String tag)
    {
        new Thread(){
            @Override
            public void run() {

                HttpURLConnection connection=null;
                try {
                    URL url = new URL(server_url+"change_user_tag/");
                    connection = (HttpURLConnection) url.openConnection();
                    connection.setConnectTimeout(3000);
                    connection.setReadTimeout(3000);
                    //设置请求方式
                    connection.setRequestMethod("POST");
                    connection.setDoInput(true);
                    connection.setDoOutput(true);
                    connection.connect();

                    String body = "user_id="+user_id+"&auth_id="+auth_id+"&tag="+tag+"&operation="+"add";
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
                            if(status.equals("success"))
                            {
//                                runOnUiThread(new Runnable() {
//                                    @Override
//                                    public void run() {
//                                        user_tags.put(tag);
//                                        try {
//                                            setUserTags();
//                                        } catch (JSONException e) {
//                                            throw new RuntimeException(e);
//                                        }
//                                    }
//                                });
                                getTags();
                            }
                            else if(status.equals("refuse"))
                            {
                                String msg = jsonObject.getString("msg");
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {

                                        Toast.makeText(ChangeTagsActivity.this, msg, Toast.LENGTH_SHORT).show();
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

    public void showTagMenu(View view,String tag,String type) {
        PopupMenu popupMenu = new PopupMenu(this, view);
        popupMenu.getMenuInflater().inflate(R.menu.tag_menu, popupMenu.getMenu());

        popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                switch (item.getItemId()) {
                    case R.id.delete_tag:
                        // 创建AlertDialog.Builder对象
                        AlertDialog.Builder builder = new AlertDialog.Builder(ChangeTagsActivity.this);
                        // 设置对话框标题
                        builder.setTitle("确认");
                        // 设置对话框内容
                        builder.setMessage("确定要删除该标签吗？");
                        // 设置确定按钮
                        builder.setPositiveButton("确定", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                if(type.equals("note"))
                                    delete_note_tag(tag);
                                else if(type.equals("user"))
                                    delete_user_tag(tag);
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
                        return true;
                    default:
                        return false;
                }
            }
        });

        popupMenu.show();
    }

    public boolean containsString(JSONArray jsonArray, String targetString) {
        for (int i = 0; i < jsonArray.length(); i++) {
            try {
                String str = jsonArray.getString(i);
                if (str.equals(targetString)) {
                    return true;
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        return false;
    }

    public void onNewUserTagClick(View view) {
        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_TEXT);

        new AlertDialog.Builder(this)
                .setTitle("新建标签")
                .setMessage("请输入标签名")
                .setView(input)
                .setPositiveButton("确认", (dialog, which) -> {
                    String tag = input.getText().toString();
                    new_user_tag(tag);
                })
                .setNegativeButton("取消", (dialog, which) -> dialog.cancel())
                .show();
    }
}
