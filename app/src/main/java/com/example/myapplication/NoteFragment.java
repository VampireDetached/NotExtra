package com.example.myapplication;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.widget.SearchView;

import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;

import com.google.android.material.navigation.NavigationView;
import com.squareup.picasso.Picasso;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedWriter;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Scanner;
import android.os.Handler;

public class NoteFragment extends Fragment {

    String TAG = "NoteFragment debug";
    String user_id,auth_id,server_url;
    View view;

    DrawerLayout drawerLayout;

    public NoteFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        //获取全局变量
        SharedPreferences sharedPreferences = getActivity().getSharedPreferences("prefs", Context.MODE_PRIVATE);
        user_id = sharedPreferences.getString("user_id", "0");
        auth_id = sharedPreferences.getString("auth_id", "");
        server_url = sharedPreferences.getString("server_url","http://10.0.2.2:8000/");

        view = inflater.inflate(R.layout.note_fragment, container, false);

        getAllNotes();


        // Inflate the layout for this fragment
        return view;
    }

    public void getAllNotes()
    {
        new Thread(){

            @Override
            public void run() {
                HttpURLConnection connection=null;
                try {
                    String urlString = server_url+"all_notes/?user_id=" + user_id + "&auth_id=" + auth_id;
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
                                getActivity().runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        try {
                                            setNotes(jsonObject);
                                        } catch (JSONException e) {
                                            throw new RuntimeException(e);
                                        }
                                    }
                                });
                            }
                            else if(status.equals("refuse"))
                            {
                                Toast.makeText(getContext(), "请求失败", Toast.LENGTH_SHORT).show();
                            }
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }.start();
    }

    public void getNotesByTag(String tag)
    {
        new Thread(){

            @Override
            public void run() {
                HttpURLConnection connection=null;
                try {
                    String urlString = server_url+"get_notes_by_tag/?user_id=" + user_id + "&auth_id=" + auth_id+"&tag="+tag;
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
                                getActivity().runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        try {
                                            setNotes(jsonObject);
                                        } catch (JSONException e) {
                                            throw new RuntimeException(e);
                                        }
                                    }
                                });
                            }
                            else if(status.equals("refuse"))
                            {
                                Toast.makeText(getContext(), "请求失败", Toast.LENGTH_SHORT).show();
                            }
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }.start();
    }

    public void setNotes(JSONObject json) throws JSONException {
        Log.i(TAG,"setNotes");
        LinearLayout notesContainer = view.findViewById(R.id.notes_container);
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
            LinearLayout noteLayout = new LinearLayout(getContext());
            noteLayout.setOrientation(LinearLayout.VERTICAL);
            noteLayout.setPadding(16, 16, 16, 16);
            LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );
            layoutParams.setMargins(0, 0, 0, 16);
            noteLayout.setLayoutParams(layoutParams);

            // Store the note_id in the noteLayout tag
            noteLayout.setTag(note_id);

            // Create and add the title TextView
            TextView titleTextView = new TextView(getContext());
            titleTextView.setText(title);
            titleTextView.setTextSize(20f);
            titleTextView.setPadding(0, 0, 0, 0);
            noteLayout.addView(titleTextView);

            // Create and add the content TextView
            TextView contentTextView = new TextView(getContext());
            contentTextView.setText(content);
            contentTextView.setTextSize(16f);
            contentTextView.setPadding(0,8,0,0);
            noteLayout.addView(contentTextView);

            TextView EditTimeTextView = new TextView(getContext());
            EditTimeTextView.setText(last_edit_time);
            EditTimeTextView.setTextSize(12f);
            EditTimeTextView.setPadding(0,8,0,0);
            noteLayout.addView(EditTimeTextView);

            View divider = new View(getContext());
            divider.setBackgroundColor(ContextCompat.getColor(getContext(), R.color.black));
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
                    String noteId = (String) v.getTag();

                    Intent intent = new Intent(getActivity(), EditNoteActivity.class);
                    // Pass the note_id to the new Activity
                    intent.putExtra("note_id", noteId);
                    // Start the new Activity
//                    startActivity(intent);
                    startActivityForResult(intent,1);
                }
            });

            // 长按事件
            noteLayout.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    String noteId = (String) v.getTag();
                    showPopupMenu(v,noteId);
                    return true;
                }
            });

            // Add the noteLayout to the notesContainer
            notesContainer.addView(noteLayout);
        }

        if (dataArray.length() == 0)
        {
            TextView noNotesTextView = new TextView(getContext());
            noNotesTextView.setText("您还没有笔记");
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

    public void showPopupMenu(View v, String note_id) {
        PopupMenu popupMenu = new PopupMenu(getActivity(), v);
        popupMenu.getMenuInflater().inflate(R.menu.popup_menu, popupMenu.getMenu());

        popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                switch (item.getItemId()) {
                    case R.id.change_tag:
                        Intent intent = new Intent(getActivity(), ChangeTagsActivity.class);
                        intent.putExtra("note_id", note_id);
                        startActivity(intent);
                        return true;
                    case R.id.delete_note:
                        // 创建AlertDialog.Builder对象
                        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                        // 设置对话框标题
                        builder.setTitle("确认");
                        // 设置对话框内容
                        builder.setMessage("确定要删除该笔记吗？");
                        // 设置确定按钮
                        builder.setPositiveButton("确定", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                delete_note( note_id);
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


    public void delete_note( String note_id)
    {
        new Thread(){

            @Override
            public void run() {
                HttpURLConnection connection=null;
                try {
                    URL url = new URL(server_url+"delete_note/");
                    connection = (HttpURLConnection) url.openConnection();
                    connection.setConnectTimeout(3000);
                    connection.setReadTimeout(3000);
                    //设置请求方式
                    connection.setRequestMethod("POST");
                    connection.setDoInput(true);
                    connection.setDoOutput(true);
                    connection.connect();

                    String body = "user_id="+user_id+"&auth_id="+auth_id+"&note_id="+note_id;
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
                                getActivity().runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        Toast.makeText(getActivity(), "笔记已删除", Toast.LENGTH_SHORT).show();

//                                        // 获取父布局
//                                        ViewGroup parentLayout = (ViewGroup) view.getParent();
//                                        // 移除视图
//                                        if (parentLayout != null) {
//                                            parentLayout.removeView(view);
//                                        }
                                        getAllNotes();
                                    }
                                });


                            }
                            else if(status.equals("refuse"))
                            {
                                String refusereason = jsonObject.getString("msg");
                                getActivity().runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {

                                        Toast.makeText(getActivity(), refusereason, Toast.LENGTH_SHORT).show();
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

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Log.i(TAG, "result code="+ resultCode);
        if (requestCode ==1)
            getAllNotes();

    }
}

