package com.example.myapplication;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import android.widget.SearchView;
import androidx.core.content.ContextCompat;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Scanner;

public class SearchNoteActivity extends AppCompatActivity {

    String TAG = "SearchNoteActivity debug";
    String user_id,auth_id,server_url;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.search_note);

        //获取全局变量
        SharedPreferences sharedPreferences = this.getSharedPreferences("prefs", Context.MODE_PRIVATE);
        user_id = sharedPreferences.getString("user_id", "0");
        auth_id = sharedPreferences.getString("auth_id", "");
        server_url = sharedPreferences.getString("server_url", "http://10.0.2.2:8000/");

        // 搜索功能
        SearchView searchView = findViewById(R.id.search_view);
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                // 处理搜索提交事件
                searchNotes(getWindow().getDecorView(),query);
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                // 处理搜索文本变化事件
                return false;
            }
        });
    }

    public void searchNotes(View view, String query)
    {
        new Thread(){

            @Override
            public void run() {
                HttpURLConnection connection=null;
                try {
                    String urlString = server_url+"search_note/?user_id=" + user_id + "&auth_id=" + auth_id + "&keyword="+query;
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
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        try {
                                            setNotes(jsonObject,view);
                                        } catch (JSONException e) {
                                            throw new RuntimeException(e);
                                        }
                                    }
                                });
                            }
                            else if(status.equals("refuse"))
                            {
                                Toast.makeText(view.getContext(), "请求失败", Toast.LENGTH_SHORT).show();
                            }
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }.start();
    }

    public void setNotes(JSONObject json, View view) throws JSONException {
        LinearLayout notesContainer = view.findViewById(R.id.search_notes);
        notesContainer.removeAllViews();
        JSONArray dataArray = json.getJSONArray("data");

        for (int i = 0; i < dataArray.length(); i++) {
            final JSONObject note = dataArray.getJSONObject(i);
            String title = note.getString("title");
            String content = note.getString("abstract");
            String note_id = note.getString("note_id");
            String last_edit_time = "上次修改于 "+note.getString("last_edit_time");

            Log.i(TAG, title);
            Log.i(TAG, content);

            // Create a new LinearLayout for each note
            LinearLayout noteLayout = new LinearLayout(this);
            noteLayout.setOrientation(LinearLayout.VERTICAL);
            noteLayout.setPadding(16, 16, 16, 16);
            LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );
            layoutParams.setMargins(0, 0, 0, 16);
            noteLayout.setLayoutParams(layoutParams);

            noteLayout.setTag(note_id);

            // Create and add the title TextView
            TextView titleTextView = new TextView(this);
            titleTextView.setText(title);
            titleTextView.setTextSize(20f);
            titleTextView.setPadding(0, 0, 0, 0);
            noteLayout.addView(titleTextView);

            // Create and add the content TextView
            TextView contentTextView = new TextView(this);
            contentTextView.setText(content);
            contentTextView.setTextSize(16f);
            contentTextView.setPadding(0,8,0,0);
            noteLayout.addView(contentTextView);

            TextView EditTimeTextView = new TextView(this);
            EditTimeTextView.setText(last_edit_time);
            EditTimeTextView.setTextSize(12f);
            EditTimeTextView.setPadding(0,8,0,0);
            noteLayout.addView(EditTimeTextView);

            View divider = new View(this);
            divider.setBackgroundColor(ContextCompat.getColor(this, R.color.black));
            LinearLayout.LayoutParams dividerParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    1 // Height of the divider
            );
            divider.setLayoutParams(dividerParams);
            noteLayout.addView(divider);

            // Add click listener to noteLayout
            noteLayout.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Log.i(TAG,"onclick");
                    String noteId = (String) v.getTag();
                    Log.i(TAG,"intent");
                    // Handle click event, e.g., open detailed note view
                    Intent intent = new Intent(SearchNoteActivity.this, EditNoteActivity.class);
                    // Pass the note_id to the new Activity
                    intent.putExtra("note_id", noteId);
                    startActivity(intent);
                }
            });

            // Add the noteLayout to the notesContainer
            notesContainer.addView(noteLayout);
        }

        if (dataArray.length() == 0)
        {
            TextView noNotesTextView = new TextView(this);
            noNotesTextView.setText("无搜索结果");
            noNotesTextView.setTextSize(18f);
            noNotesTextView.setGravity(Gravity.CENTER);

            LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );
            layoutParams.topMargin = 128;  // Set margin here
            noNotesTextView.setLayoutParams(layoutParams);

            notesContainer.addView(noNotesTextView);
        }
    }

}
