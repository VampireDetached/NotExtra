package com.example.myapplication;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextWatcher;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.Toolbar;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import com.squareup.picasso.BuildConfig;
import com.squareup.picasso.Picasso;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Scanner;
import java.util.Vector;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class EditNoteActivity extends AppCompatActivity {

    static String TAG = "data:---->";
    SharedPreferences sharedPreferences;
    String user_id,auth_id,server_url;
    private static final int REQUEST_CODE_PERMISSIONS = 1;
    private static final String[] REQUIRED_PERMISSIONS = {
            android.Manifest.permission.READ_MEDIA_IMAGES,
            android.Manifest.permission.READ_MEDIA_VIDEO,
            android.Manifest.permission.READ_MEDIA_AUDIO,
            android.Manifest.permission.CAMERA
    };
    // 获取父布局
    RelativeLayout parentLayout;
    ConstraintLayout background;
    Context that=this;
    JSONObject originjsonObject;
    String note_text="";
    Uri image_uri;
    File outputImage;
    Uri audio_uri;

    int curfocus;
    String title_text="";

    String currentPhotoPath;
    String note_id;

    String note_code;
    public Vector<NoteBlock> noteBlocks = new Vector<>();
//    int topMargin = 0;
    boolean newnote;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.edit_note);

        if (!allPermissionsGranted()) {
            ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS);
        }

        //获取全局变量
        sharedPreferences = this.getSharedPreferences("prefs", Context.MODE_PRIVATE);
        user_id = sharedPreferences.getString("user_id", "0");
        auth_id = sharedPreferences.getString("auth_id", "");
        server_url = sharedPreferences.getString("server_url","http://10.0.2.2:8000/");

        // 获取传递过来的 Intent
        Intent intent = getIntent();
        // 从 Intent 中提取 note_id
        note_id = intent.getStringExtra("note_id");
        Log.i(TAG,"note_id:"+note_id);
        // 创建一个Handler来处理网络操作的结果
        Handler handler = new Handler(Looper.getMainLooper());

        Thread networkThread = new Thread(new Runnable() {
            @Override
            public void run() {
                if (Objects.equals(note_id, "0")) {
                    Log.i(TAG, "newnote");
                    newnote = true;

                    HttpURLConnection connection = null;
                    try {
                        URL url = new URL(server_url + "new_note/");
                        connection = (HttpURLConnection) url.openConnection();
                        connection.setConnectTimeout(3000);
                        connection.setReadTimeout(3000);
                        // 设置请求方式
                        connection.setRequestMethod("POST");
                        connection.setDoInput(true);
                        connection.setDoOutput(true);
                        connection.connect();

                        String body = "title=" + title_text + "&user_id=" + user_id + "&auth_id=" + auth_id;
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
                                    note_code = jsonObject.getString("note_id");
                                    Log.i(TAG, "Create note ok" + note_code);
                                }
                            }
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                else {
                    Log.i(TAG, "oldnote");
                    newnote = false;

                    HttpURLConnection connection = null;
                    try {
                        URL url = new URL(server_url + "view_note/?" + "note_id=" + note_id + "&user_id=" + user_id + "&auth_id=" + auth_id);
                        connection = (HttpURLConnection) url.openConnection();
                        connection.setConnectTimeout(3000);
                        connection.setReadTimeout(3000);
                        // 设置请求方式
                        connection.setRequestMethod("GET");
                        connection.setDoInput(true);
                        connection.connect();
                        Log.i(TAG, "oldnote1");
                        int responseCode = connection.getResponseCode();
                        if (responseCode == HttpURLConnection.HTTP_OK) {
                            InputStream inputStream = connection.getInputStream();
                            Scanner scanner = new Scanner(inputStream).useDelimiter("\\A");
                            String responseStr = scanner.hasNext() ? scanner.next() : "";
                            JSONObject jsonObject = new JSONObject(responseStr);
                            Log.i(TAG, "oldnote2");
                            if (jsonObject.has("status")) {
                                String status = jsonObject.getString("status");
                                Log.i(TAG, status);
                                if (status.equals("success")) {
                                    note_code = jsonObject.getString("note_id");
                                    title_text = jsonObject.getString("title");
                                    originjsonObject = jsonObject;
                                    Log.i(TAG, "oldnote4");
                                }
                            }
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

                // 在主线程上处理结果
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        Log.i(TAG, "start 0");
                        // 在这里进行任何需要在主线程上运行的操作
                    }
                });
            }
        });

        networkThread.start();
        try {
            networkThread.join(); // 等待网络线程完成
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        Log.i(TAG,"start 0");
        background = findViewById(R.id.background);
        EditText title = findViewById(R.id.title);
        TextView time = findViewById(R.id.timelabel);
        TextView tag = findViewById(R.id.taglabel);
        ImageView button_text = findViewById(R.id.button_text);
        ImageView button_image = findViewById(R.id.button_image);
        ImageView button_audio = findViewById(R.id.button_audio);
        ImageView button_ai = findViewById(R.id.button_ai);

        title.setText(title_text);
        Log.i(TAG,"start 1");
        background.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                curfocus = noteBlocks.size();
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    View currentFocus = getCurrentFocus();
                    if (currentFocus instanceof EditText) {
                        currentFocus.clearFocus();
                        InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
                        if (imm != null) {
                            imm.hideSoftInputFromWindow(currentFocus.getWindowToken(), 0);
                        }
                    }
                }
                return true;
            }
        });


        Log.i(TAG,"start 2");
        parentLayout = findViewById(R.id.parent_layout);

        title.setSingleLine(true);
        title.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (!hasFocus) {
                    new Thread(){
                        @Override
                        public void run() {

                            HttpURLConnection connection=null;
                            try {
                                URL url = new URL(server_url+"change_title/");
                                connection = (HttpURLConnection) url.openConnection();
                                connection.setConnectTimeout(3000);
                                connection.setReadTimeout(3000);
                                //设置请求方式
                                connection.setRequestMethod("POST");
                                connection.setDoInput(true);
                                connection.setDoOutput(true);
                                connection.connect();

                                String body = "note_id="+note_code+"&title="+title.getText().toString()+"&user_id=" + user_id + "&auth_id=" + auth_id;
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
                                            note_code = jsonObject.getString("note_id");
                                            Log.i(TAG,"Change title ok:"+title.getText().toString());
                                        }
                                    }
                                }
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    }.start();
                }
                else {

                }
            }
        });
        Log.i(TAG,"start 3");


        // 假设您的布局中有一个RelativeLayout作为父布局

        button_text.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.i(TAG,"clicked");
                View currentFocus = getCurrentFocus();
                if (currentFocus instanceof EditText) {
                    currentFocus.clearFocus();
                    InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
                    if (imm != null) {
                        imm.hideSoftInputFromWindow(currentFocus.getWindowToken(), 0);
                    }
                }
                NoteBlock noteBlock = new NoteBlock(that, 1, noteBlocks, note_code, curfocus);

                // 设置内容（假设setContent方法的第一个参数1代表文本类型）
                if (noteBlocks.isEmpty())
                    noteBlock.setContent(1, note_text, image_uri,  0);

                else
                    noteBlock.setContent(1, note_text, image_uri,  noteBlocks.elementAt(curfocus-1).topMargin()+noteBlocks.elementAt(curfocus-1).getH());

                int noteBlockId = View.generateViewId();
                noteBlock.setId(noteBlockId);


                // 将NoteBlock添加到父布局中
                parentLayout.addView(noteBlock);
                noteBlocks.add(curfocus,noteBlock);
                Log.i("AAAAA", String.valueOf(noteBlocks.size()));


                EditText tV = noteBlock.textView;
                tV.post(new Runnable() {
                    @Override
                    public void run() {
                        int blockH = tV.getHeight();
                        for (int i =curfocus; i < noteBlocks.size(); i++) {
                            noteBlocks.get(i).setTopMarigin(noteBlocks.get(i).topMargin() + blockH);
                            noteBlocks.get(i).number = noteBlocks.get(i).number+1;
                            noteBlocks.get(i).requestLayout();
                            Log.i("LLL", String.valueOf(blockH));
                        }

                    }
                });

                tV.addTextChangedListener(new TextWatcher() {
                    int originHeight;
                    @Override
                    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                        originHeight = tV.getHeight();
                    }

                    @Override
                    public void onTextChanged(CharSequence s, int start, int before, int count) {
                        // 动态调整宽度
                        // 计算行数
                        int lineCount = tV.getLineCount();
                        // 获取单行高度
                        int lineHeight = tV.getLineHeight();
                        // 计算总高度
                        int desiredHeight = lineCount * lineHeight;
                        if (desiredHeight != originHeight)
                        {
                            int deltaHeight = desiredHeight - originHeight;
                            // 设置 textView 的高度
                            tV.setHeight(desiredHeight);

                            for (int i = noteBlocks.indexOf(noteBlock) + 1; i < noteBlocks.size(); i++) {
                                Log.i(TAG,"Lines ="+lineCount+" Change height of "+i+" deltaHeight= "+deltaHeight+" now topmargin = "+(noteBlocks.get(i).topMargin() + deltaHeight));
                                noteBlocks.get(i).setTopMarigin(noteBlocks.get(i).topMargin() + deltaHeight);
                            }
                        }
                    }

                    @Override
                    public void afterTextChanged(Editable s) {

                    }

                });
                tV.setOnFocusChangeListener(new View.OnFocusChangeListener() {
                    @Override
                    public void onFocusChange(View v, boolean hasFocus) {
                        if (!hasFocus) {
                            noteBlock.menucall = false;
                            new Thread(){
                                @Override
                                public void run() {

                                    HttpURLConnection connection=null;
                                    try {
                                        URL url = new URL(server_url+"change_content/");
                                        connection = (HttpURLConnection) url.openConnection();
                                        connection.setConnectTimeout(3000);
                                        connection.setReadTimeout(3000);
                                        //设置请求方式
                                        connection.setRequestMethod("POST");
                                        connection.setDoInput(true);
                                        connection.setDoOutput(true);
                                        connection.connect();

                                        String body = "note_id="+note_code+"&user_id=" + user_id + "&auth_id=" + auth_id + "&type=text" + "&order=" + String.valueOf(noteBlock.number +1) + "&operation=edit" + "&detail=" + tV.getText().toString() ;
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
                                                    Log.i(TAG,"Change text ok");
                                                }
                                            }
                                        }
                                    } catch (Exception e) {
                                        e.printStackTrace();
                                    }
                                }
                            }.start();
                        }
                        else {

                        }
                    }
                });
                tV.setOnTouchListener(new View.OnTouchListener() {
                    @Override
                    public boolean onTouch(View v, MotionEvent event) {
                        // 当视图被触摸时，执行以下代码
                        if (event.getAction() == MotionEvent.ACTION_DOWN) {
                            // 如果是按下事件
                            Log.i(TAG,"focus reset");
                            curfocus=noteBlock.number+1;
                            Log.i("LLL", String.valueOf(curfocus));
                        }
                        // 返回 true 表示消费了该事件，不会向下传递
                        return false;
                    }
                });


                Log.i(TAG, "height "+noteBlock.getH());
                curfocus = noteBlock.number+1;
                new Thread(){
                    @Override
                    public void run() {

                        HttpURLConnection connection=null;
                        try {
                            URL url = new URL(server_url+"change_content/");
                            connection = (HttpURLConnection) url.openConnection();
                            connection.setConnectTimeout(3000);
                            connection.setReadTimeout(3000);
                            //设置请求方式
                            connection.setRequestMethod("POST");
                            connection.setDoInput(true);
                            connection.setDoOutput(true);
                            connection.connect();

                            String body = "note_id="+note_code+"&user_id=" + user_id + "&auth_id=" + auth_id + "&type=text" + "&order=" + String.valueOf(noteBlock.number +1) + "&operation=add" + "&detail=" + tV.getText().toString() ;
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
                                        Log.i(TAG,"Add text ok");
                                    }
                                }
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }.start();

            }
        });


        button_image.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                Log.e("a","start trans");
                button_image.getViewTreeObserver().removeOnGlobalLayoutListener(this);
            }
        });

        button_image.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                View currentFocus = getCurrentFocus();
                if (currentFocus instanceof EditText) {
                    currentFocus.clearFocus();
                    InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
                    if (imm != null) {
                        imm.hideSoftInputFromWindow(currentFocus.getWindowToken(), 0);
                    }
                }
                AlertDialog.Builder builder = new AlertDialog.Builder(that);
                builder.setTitle("选择图片")
                        .setItems(new CharSequence[]{"拍照", "从相册选择"}, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                switch (which) {
                                    case 0:
                                        Log.i(TAG,"photo1");
                                        outputImage = new File(getExternalCacheDir(), "output_image.jpg");
                                        try {
                                            if (outputImage.exists()) {
                                                outputImage.delete();
                                            }
                                        } catch (Exception e) {
                                            e.printStackTrace();
                                        }
                                        Log.i(TAG,"photo2");
                                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                                            //大于等于版本24（7.0）的场合
//                                            image_uri = FileProvider.getUriForFile(that,"com.limxtop.research.fileprovider", outputImage);
                                            image_uri = FileProvider.getUriForFile(Objects.requireNonNull(getApplicationContext()), "com.squareup.picasso.provider", outputImage);
                                        } else {
                                            //小于android 版本7.0（24）的场合
                                            image_uri = Uri.fromFile(outputImage);
                                        }

                                        Log.i(TAG,"photo3");
                                        //启动相机程序
                                        Intent intent_photo = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                                        //MediaStore.ACTION_IMAGE_CAPTURE = android.media.action.IMAGE_CAPTURE
                                        intent_photo.putExtra(MediaStore.EXTRA_OUTPUT, image_uri);
                                        startActivityForResult(intent_photo, 3);
                                        Log.i(TAG,"photo4>");
                                        break;
                                    case 1:
                                        // 从相册选择图片
                                        Intent intent_image = new Intent(Intent.ACTION_PICK, null);
                                        intent_image.setDataAndType(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, "image/*");
                                        startActivityForResult(intent_image, 1);
                                        break;
                                }
                            }
                        }).show();
            }
        });

        button_audio.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // 动态创建NoteBlock实例
                View currentFocus = getCurrentFocus();
                if (currentFocus instanceof EditText) {
                    currentFocus.clearFocus();
                    InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
                    if (imm != null) {
                        imm.hideSoftInputFromWindow(currentFocus.getWindowToken(), 0);
                    }
                }
                Log.i(TAG,"audio clicked");
                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                intent.setType("audio/*");
                Log.i(TAG,"audio intent starting");
                startActivityForResult(intent, 2);
            }
        });

        button_ai.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // 创建一个AlertDialog构建器
                AlertDialog.Builder builder = new AlertDialog.Builder(EditNoteActivity.this);
                builder.setTitle("AI文本生成");

                LayoutInflater inflater = EditNoteActivity.this.getLayoutInflater();

// 使用自定义布局
                View dialogView = inflater.inflate(R.layout.ai_dialog, null);
                builder.setView(dialogView);

                // 添加一个输入框
                final EditText ai_input = dialogView.findViewById(R.id.ai_input);
                final TextView ai_output = dialogView.findViewById(R.id.ai_output);
                final Button ai_confirm = dialogView.findViewById(R.id.ai_confirm);
                final Button ai_cancel = dialogView.findViewById(R.id.ai_cancel);

                ai_output.setVisibility(View.GONE);
                // 创建并显示AlertDialog
                AlertDialog dialog = builder.create();

                dialog.show();

                ai_confirm.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        final String[] return_output = {"AI助手正在加载中……"};
                        ai_output.setVisibility(View.VISIBLE);
                        ai_output.setText(return_output[0]);
                        new Thread(){

                            @Override
                            public void run() {
                                HttpURLConnection connection=null;
                                try {
                                    String input_text = ai_input.getText().toString();
                                    String urlString = server_url+"ai_text/?user_id=" + user_id + "&auth_id=" + auth_id +"&note_id="+note_id+ "&text=" + input_text;
                                    URL url = new URL(urlString);

                                    // 打开连接
                                    connection = (HttpURLConnection) url.openConnection();
                                    connection.setConnectTimeout(30000);
                                    connection.setReadTimeout(30000);
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

                                                return_output[0] = jsonObject.getString("answer");
                                                runOnUiThread(new Runnable() {
                                                    @Override
                                                    public void run() {
                                                        // 在主线程上设置文本
                                                        ai_output.setText(return_output[0]);
                                                    }
                                                });

                                                Log.i(TAG, return_output[0]);

                                            }
                                            else
                                            {
                                                ai_output.setText("AI助手出错了！");
                                            }
                                        }
                                    }
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            }
                        }.start();
                    }
                });

                ai_cancel.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        dialog.dismiss();
                    }
                });

            }
        });

        Log.i(TAG,"start 4");
        title.setText(title_text);
        // 创建Calendar实例
        Calendar calendar = Calendar.getInstance();

        // 获取小时、分钟和秒
        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        int minute = calendar.get(Calendar.MINUTE);
        int second = calendar.get(Calendar.SECOND);

        // 获取年、月和日
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH) + 1;  // 月份是从0开始计数的
        int day = calendar.get(Calendar.DAY_OF_MONTH);

        // 可以将时间格式化为字符串
        String currentTimeString = DateFormat.format("20yy-MM-dd HH:mm:ss", calendar).toString();
        time.setText(currentTimeString);
        tag.setText("查看标签>");
        tag.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v)
            {
                Intent intent = new Intent(EditNoteActivity.this, ChangeTagsActivity.class);
                intent.putExtra("note_id", note_code);
                startActivity(intent);

            }
        });
        if(!newnote) {
            try {
                String letime = originjsonObject.getString("last_edit_time");
                //JSONArray tags = originjsonObject.getJSONArray("tags");
                time.setText(letime);
//                if(tags.length()!=0){
//                    tag.setText(tags.getString(0)+"等"+tags.length()+"个标签");
//                }
            } catch (JSONException e) {
                throw new RuntimeException(e);
            }

            drawold(0);
        }
        Log.i(TAG,"start 5");

        curfocus = noteBlocks.size();
    }
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.i(TAG,"In activity result");

        if (data!=null)
            super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == 1 && data!=null) {
            // 得到图片的全路径

            image_uri = data.getData();
//                Log.d("data---->", data.toString());
//                Log.d("data---->", data.getData().toString());

            View currentFocus = getCurrentFocus();
            if (currentFocus instanceof EditText) {
                currentFocus.clearFocus();
                InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
                if (imm != null) {
                    imm.hideSoftInputFromWindow(currentFocus.getWindowToken(), 0);
                }
            }
            NoteBlock noteBlock = new NoteBlock(that, 2, noteBlocks, note_code, curfocus);

            // 设置内容（假设setContent方法的第一个参数1代表文本类型）
            if (noteBlocks.isEmpty())
                noteBlock.setContent(2, "", image_uri,  0);
            else
                noteBlock.setContent(2, "", image_uri,  noteBlocks.elementAt(curfocus-1).topMargin()+noteBlocks.elementAt(curfocus-1).getH());

            Log.i(TAG, String.valueOf(noteBlock.topMargin()));
            int noteBlockId = View.generateViewId();
            noteBlock.setId(noteBlockId);


            // 将NoteBlock添加到父布局中
            parentLayout.addView(noteBlock);
            noteBlocks.add(curfocus,noteBlock);

            ImageView imageView = noteBlock.imageView;
            imageView.post(new Runnable() {
                @Override
                public void run() {

                    int blockH = imageView.getHeight();
                    for (int i =curfocus; i < noteBlocks.size(); i++) {
                        Log.i("LLL", String.valueOf(noteBlocks.get(i).topMargin()));
                        noteBlocks.get(i).setTopMarigin(noteBlocks.get(i).topMargin() + blockH);
                        noteBlocks.get(i).number = noteBlocks.get(i).number+1;
                        noteBlocks.get(i).requestLayout();
                        Log.i("LLL", String.valueOf(blockH));
                        Log.i("LLL", String.valueOf(noteBlocks.get(i).topMargin()));
                    }
                    noteBlock.requestLayout();
                }
            });
            String path = uriToBitmap(image_uri, that);
            imageView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    // 当视图被点击时，执行以下代码
                    curfocus=noteBlock.number+1;
                    Log.i("LLL", String.valueOf(curfocus));
                    View currentFocus = getCurrentFocus();
                    if (currentFocus instanceof EditText) {
                        currentFocus.clearFocus();
                        InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
                        if (imm != null) {
                            imm.hideSoftInputFromWindow(currentFocus.getWindowToken(), 0);
                        }
                    }

                }
            });
            curfocus = noteBlock.number+1;
            File file = new File(path);
            OkHttpClient httpClient = new OkHttpClient();
            MediaType mediatype = MediaType.Companion.parse("image/jpg");
            RequestBody fileBody = RequestBody.Companion.create(file, mediatype);
            RequestBody requestBody = new MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart("detail", file.getName(), fileBody)
                    .addFormDataPart("user_id", user_id)
                    .addFormDataPart("auth_id", auth_id)
                    .addFormDataPart("note_id", note_code)
                    .addFormDataPart("type", "image")
                    .addFormDataPart("order", String.valueOf(noteBlock.number+1))
                    .addFormDataPart("operation", "add")
                    .build();
            Request getRequest = new Request.Builder()
                    .url(server_url + "change_content/")
                    .post(requestBody)
                    .build();

            Call call = httpClient.newCall(getRequest);


            call.enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    Log.i(TAG, e.toString());
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    Log.i(TAG, "okHttpPost enqueue: \n onResponse:" + response.toString() + "\n body:" + response.body().string());
                }
            });
        }
        else if (requestCode == 2 && data!=null) {
            // audio
            audio_uri = data.getData();
            Log.d(TAG, "local uri"+data.toString());

            View currentFocus = getCurrentFocus();
            if (currentFocus instanceof EditText) {
                currentFocus.clearFocus();
                InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
                if (imm != null) {
                    imm.hideSoftInputFromWindow(currentFocus.getWindowToken(), 0);
                }
            }
            NoteBlock noteBlock = new NoteBlock(that, 3, noteBlocks, note_code, curfocus);

            // 设置内容（假设setContent方法的第一个参数1代表文本类型）
            if (noteBlocks.isEmpty())
                noteBlock.setContent(3, "", audio_uri,  0);

            else
                noteBlock.setContent(3, "", audio_uri,  noteBlocks.elementAt(curfocus-1).topMargin()+noteBlocks.elementAt(curfocus-1).getH());


            int noteBlockId = View.generateViewId();
            noteBlock.setId(noteBlockId);


            // 将NoteBlock添加到父布局中
            parentLayout.addView(noteBlock);
            noteBlocks.add(curfocus,noteBlock);

            LinearLayout audioComponent= noteBlock.audioComponent;
            audioComponent.post(new Runnable() {
                @Override
                public void run() {
                    int blockH = audioComponent.getHeight();
                    for (int i =curfocus; i < noteBlocks.size(); i++) {
                        noteBlocks.get(i).setTopMarigin(noteBlocks.get(i).topMargin() + blockH);
                        noteBlocks.get(i).number = noteBlocks.get(i).number+1;
                        noteBlocks.get(i).requestLayout();
                        Log.i("LLL", String.valueOf(blockH));
                    }
                }
            });
            audioComponent.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    // 当视图被点击时，执行以下代码
                    curfocus=noteBlock.number+1;
                    Log.i("LLL", String.valueOf(curfocus));
                    View currentFocus = getCurrentFocus();
                    if (currentFocus instanceof EditText) {
                        currentFocus.clearFocus();
                        InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
                        if (imm != null) {
                            imm.hideSoftInputFromWindow(currentFocus.getWindowToken(), 0);
                        }
                    }
                }
            });

            Log.i(TAG,"build success");

            curfocus = noteBlock.number+1;
            File file = createAudioFileFromUri(audio_uri);
            OkHttpClient httpClient = new OkHttpClient();
            MediaType mediatype = null;
            int lastDotIndex = file.getName().lastIndexOf('.');
            String fileExtension = file.getName().substring(lastDotIndex + 1).toLowerCase();

            // 设置 MediaType
            if ("wav".equalsIgnoreCase(fileExtension)) {
                mediatype = MediaType.Companion.parse("audio/wav");
            } else if ("mp3".equalsIgnoreCase(fileExtension)) {
                mediatype = MediaType.Companion.parse("audio/mpeg");
            } else if ("aac".equalsIgnoreCase(fileExtension)) {
                mediatype = MediaType.Companion.parse("audio/aac");
            } else if ("flac".equalsIgnoreCase(fileExtension)) {
                mediatype = MediaType.Companion.parse("audio/flac");
            } else if ("amr".equalsIgnoreCase(fileExtension)) {
                mediatype = MediaType.Companion.parse("audio/amr");
            } else if ("ogg".equalsIgnoreCase(fileExtension)) {
                mediatype = MediaType.Companion.parse("audio/ogg");
            } else if ("aiff".equalsIgnoreCase(fileExtension)) {
                mediatype = MediaType.Companion.parse("audio/aiff");
            } else if ("wma".equalsIgnoreCase(fileExtension)) {
                mediatype = MediaType.Companion.parse("audio/x-ms-wma");
            } else if ("dsd".equalsIgnoreCase(fileExtension)) {
                mediatype = MediaType.Companion.parse("audio/dsd");
            } else if ("m4a".equalsIgnoreCase(fileExtension)) {
                mediatype = MediaType.Companion.parse("audio/mp4");
            } else {
                // 如果文件类型未知，默认为 octet-stream
                mediatype = MediaType.Companion.parse("application/octet-stream");
            }
            RequestBody fileBody = RequestBody.Companion.create(file, mediatype);
            RequestBody requestBody = new MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart("detail", file.getName(), fileBody)
                    .addFormDataPart("user_id", user_id)
                    .addFormDataPart("auth_id", auth_id)
                    .addFormDataPart("note_id", note_code)
                    .addFormDataPart("type", "audio")
                    .addFormDataPart("order", String.valueOf(noteBlock.number+1))
                    .addFormDataPart("operation", "add")
                    .build();
            Request getRequest = new Request.Builder()
                    .url(server_url + "change_content/")
                    .post(requestBody)
                    .build();

            Call call = httpClient.newCall(getRequest);


            call.enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    Log.i(TAG, e.toString());
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    Log.i(TAG, "okHttpPost enqueue: \n onResponse:" + response.toString() + "\n body:" + response.body().string());
                }
            });

        }
        else if (requestCode == 3 ) {
            Log.i(TAG,"photo4");

            View currentFocus = getCurrentFocus();
            if (currentFocus instanceof EditText) {
                currentFocus.clearFocus();
                InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
                if (imm != null) {
                    imm.hideSoftInputFromWindow(currentFocus.getWindowToken(), 0);
                }
            }
            // 将拍摄的图片添加到 NoteBlock 中
            NoteBlock noteBlock = new NoteBlock(that, 2, noteBlocks, note_code, curfocus);
            if (noteBlocks.isEmpty())
                noteBlock.setContent(2, "", image_uri, 0);
            else
                noteBlock.setContent(2, "", image_uri, noteBlocks.elementAt(curfocus-1).topMargin() + noteBlocks.elementAt(curfocus-1).getH());

            int noteBlockId = View.generateViewId();
            noteBlock.setId(noteBlockId);

            parentLayout.addView(noteBlock);
            noteBlocks.add(curfocus,noteBlock);

            // 处理其他逻辑（如调整高度等）
            ImageView imageView = noteBlock.imageView;
            imageView.post(new Runnable() {
                @Override
                public void run() {
                    int blockH = imageView.getHeight();
                    for (int i =curfocus; i < noteBlocks.size(); i++) {
                        noteBlocks.get(i).setTopMarigin(noteBlocks.get(i).topMargin() + blockH);
                        noteBlocks.get(i).number = noteBlocks.get(i).number+1;
                        noteBlocks.get(i).requestLayout();
                        Log.i("LLL", String.valueOf(blockH));
                    }
                }
            });
            imageView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    // 当视图被点击时，执行以下代码
                    curfocus=noteBlock.number+1;
                    Log.i("LLL", String.valueOf(curfocus));
                    View currentFocus = getCurrentFocus();
                    if (currentFocus instanceof EditText) {
                        currentFocus.clearFocus();
                        InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
                        if (imm != null) {
                            imm.hideSoftInputFromWindow(currentFocus.getWindowToken(), 0);
                        }
                    }
                }
            });

            curfocus = noteBlock.number+1;
            File file = outputImage;
            OkHttpClient httpClient = new OkHttpClient();
            MediaType mediatype = MediaType.Companion.parse("image/jpg");
            RequestBody fileBody = RequestBody.Companion.create(file,mediatype);
            Log.i(TAG,"photo working……");
            RequestBody requestBody = new MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart("detail", file.getName(), fileBody)
                    .addFormDataPart("user_id", user_id)
                    .addFormDataPart("auth_id", auth_id)
                    .addFormDataPart("note_id", note_code)
                    .addFormDataPart("type", "image")
                    .addFormDataPart("order", String.valueOf(noteBlock.number +1))
                    .addFormDataPart("operation", "add")
                    .build();
            Request getRequest = new Request.Builder()
                    .url(server_url+"change_content/")
                    .post(requestBody)
                    .build();

            Call call = httpClient.newCall(getRequest);


            call.enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    Log.i(TAG,e.toString());
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    Log.i(TAG, "okHttpPost enqueue: \n onResponse:"+ response.toString() +"\n body:" +response.body().string());
                }
            });

        }

        if (data == null)
        {
            Log.e(TAG,"data is null");
        }
    }
    private void setSupportActionBar(Toolbar toolbar) {
    }
    private boolean allPermissionsGranted() {
        for (String permission : REQUIRED_PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (!allPermissionsGranted()) {
                Toast.makeText(this, "Permissions not granted by the user.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private File createImageFile() throws IOException {
        // 创建唯一的文件名
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(
                imageFileName,  /* 前缀 */
                ".jpg",         /* 后缀 */
                storageDir      /* 目录 */
        );

        // 保存文件路径以便后续使用
        currentPhotoPath = image.getAbsolutePath();
        return image;
    }

    public static String uriToBitmap(Uri uri, Context context) {
        String imagePath = null;
        if (null != uri) {
            if ("file".equals(uri.getScheme())) {
                Log.i(TAG, "path uri 获得图片");
                imagePath = uri.getPath();
            } else if ("content".equals(uri.getScheme())) {
                Log.i(TAG, "content uri 获得图片");
                String[] filePathColumns = {MediaStore.Images.Media.DATA};
                Cursor c = context.getContentResolver().query(uri, filePathColumns, null, null, null);
                if (null != c && c.moveToFirst()) {
                    int columnIndex = c.getColumnIndex(filePathColumns[0]);
                    if (columnIndex != -1) {
                        imagePath = c.getString(columnIndex);
                    }
                    c.close();
                }
            }
        }
        Log.i(TAG, "getResult imagePath " + imagePath);
        return imagePath;
    }


    private File createAudioFileFromUri(Uri audioUri) {
        ContentResolver contentResolver = getContentResolver();
        String audioFilePath = null;
        File audioFile = null;

        try {
            audioFilePath = getExternalFilesDir(Environment.DIRECTORY_MUSIC)
                    + File.separator + "audio_" + new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date()) + ".3gp";
            audioFile = new File(audioFilePath);

            if (!audioFile.exists()) {
                audioFile.createNewFile();
            }

            // Copy audio data from the content resolver to the file
            FileUtils.copyInputStreamToFile(contentResolver.openInputStream(audioUri), audioFile);

        } catch (IOException e) {
            e.printStackTrace();
        }

        return audioFile;
    }

    public Vector<NoteBlock> getNoteBlocks(){
        return noteBlocks;
    }

    public void drawold(int i)
    {
        JSONArray origin_data = null;
        try {
            origin_data = originjsonObject.getJSONArray("contents");
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
        if (i < origin_data.length()) {
            Log.i(TAG, "Start create");
            try {


                final JSONObject origin_block = origin_data.getJSONObject(i);
                //                                        String origin_id = origin_block.getString("content_id");
                //                                        String origin_order = origin_block.getString("order");
                String origin_type = origin_block.getString("type");
                String origin_detail = origin_block.getString("detail");
                NoteBlock noteBlock ;
                int noteBlockId;
                View currentFocus;
                Log.i(TAG, "size="+String.valueOf(noteBlocks.size()));
                switch (origin_type) {
                    case "text":
                        noteBlock = new NoteBlock(that, 1, noteBlocks, note_code, noteBlocks.size());

                        // 设置内容（假设setContent方法的第一个参数1代表文本类型）
                        if (noteBlocks.isEmpty())
                            noteBlock.setContent(1, origin_detail, image_uri, 0);
                        else {
                            switch (noteBlocks.lastElement().getType()) {
                                case 1:
                                    noteBlock.setContent(1, origin_detail, image_uri, noteBlocks.lastElement().topMargin() + noteBlocks.lastElement().textView.getHeight());
                                    break;
                                case 2:
                                    noteBlock.setContent(1, origin_detail, image_uri, noteBlocks.lastElement().topMargin() + noteBlocks.lastElement().imageView.getHeight());
                                    break;
                                case 3:
                                    noteBlock.setContent(1, origin_detail, image_uri, noteBlocks.lastElement().topMargin() + noteBlocks.lastElement().audioComponent.getHeight());
                                    break;
                            }

                        }
//                                    Log.i("")
                        noteBlockId = View.generateViewId();
                        noteBlock.setId(noteBlockId);


                        // 将NoteBlock添加到父布局中
                        parentLayout.addView(noteBlock);
                        noteBlocks.add(noteBlock);

                        EditText tV = noteBlock.textView;
                        tV.addTextChangedListener(new TextWatcher() {
                            int originHeight;

                            @Override
                            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                                originHeight = tV.getHeight();
                            }

                            @Override
                            public void onTextChanged(CharSequence s, int start, int before, int count) {
                                // 动态调整宽度
                                // 计算行数
                                int lineCount = tV.getLineCount();
                                // 获取单行高度
                                int lineHeight = tV.getLineHeight();
                                // 计算总高度
                                int desiredHeight = lineCount * lineHeight;
                                if (desiredHeight != originHeight) {
                                    int deltaHeight = desiredHeight - originHeight;
                                    // 设置 textView 的高度
                                    tV.setHeight(desiredHeight);

                                    for (int i = noteBlocks.indexOf(noteBlock) + 1; i < noteBlocks.size(); i++) {
                                        Log.i(TAG, "Lines =" + lineCount + " Change height of " + i + " deltaHeight= " + deltaHeight + " now topmargin = " + (noteBlocks.get(i).topMargin() + deltaHeight));
                                        noteBlocks.get(i).setTopMarigin(noteBlocks.get(i).topMargin() + deltaHeight);
                                    }
                                }
                            }

                            @Override
                            public void afterTextChanged(Editable s) {

                            }

                        });
                        tV.setOnFocusChangeListener(new View.OnFocusChangeListener() {
                            @Override
                            public void onFocusChange(View v, boolean hasFocus) {
                                if (!hasFocus) {
                                    noteBlock.menucall = false;
                                    new Thread() {
                                        @Override
                                        public void run() {

                                            HttpURLConnection connection = null;
                                            try {
                                                URL url = new URL(server_url + "change_content/");
                                                connection = (HttpURLConnection) url.openConnection();
                                                connection.setConnectTimeout(3000);
                                                connection.setReadTimeout(3000);
                                                //设置请求方式
                                                connection.setRequestMethod("POST");
                                                connection.setDoInput(true);
                                                connection.setDoOutput(true);
                                                connection.connect();

                                                String body = "note_id=" + note_code + "&user_id=" + user_id + "&auth_id=" + auth_id + "&type=text" + "&order=" + String.valueOf(noteBlock.number + 1) + "&operation=edit" + "&detail=" + tV.getText().toString();
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
                                                            Log.i(TAG, "Change text ok");
                                                        }
                                                    }
                                                }
                                            } catch (Exception e) {
                                                e.printStackTrace();
                                            }
                                        }
                                    }.start();
                                }
                                else {
                                }
                            }
                        });
                        tV.setOnTouchListener(new View.OnTouchListener() {
                            @Override
                            public boolean onTouch(View v, MotionEvent event) {
                                // 当视图被触摸时，执行以下代码
                                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                                    // 如果是按下事件
                                    Log.i(TAG,"focus reset");
                                    curfocus=noteBlock.number+1;
                                    Log.i("LLL", String.valueOf(curfocus));
                                }
                                // 返回 true 表示消费了该事件，不会向下传递
                                return false;
                            }
                        });
                        Log.i(TAG, "createtext " + noteBlock.getH());
                        ViewTreeObserver viewTreeObserver = noteBlock.textView.getViewTreeObserver();
                        curfocus = noteBlock.number+1;
                        viewTreeObserver.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                            @Override
                            public void onGlobalLayout() {
                                Log.i(TAG,"finish block"+ i+"height"+noteBlock.getH());
                                drawold(i+1);
                                noteBlock.textView.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                            }
                        });
                        break;
                    case "image":
                        Log.i(TAG, "createimage");
                        currentFocus = getCurrentFocus();
                        if (currentFocus instanceof EditText) {
                            currentFocus.clearFocus();
                            InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
                            if (imm != null) {
                                imm.hideSoftInputFromWindow(currentFocus.getWindowToken(), 0);
                            }
                        }
                        noteBlock = new NoteBlock(that, 2, noteBlocks, note_code, noteBlocks.size());

                        // 设置内容（假设setContent方法的第一个参数1代表文本类型）
                        if (noteBlocks.isEmpty()) {
                            //noteBlock.setContent(2, "", Uri.parse(server_url+image_uri), 0);
//                                        String imageUrl = server_url + image_uri.getPath(); // 获取图片的完整URL
                            Uri fullImageUri = Uri.parse(server_url+origin_detail); // 创建完整的图片URI
                            noteBlock.setContent(2, "", fullImageUri, 0); // 设置noteBlock的图片内容

                        } else {
                            Log.i(TAG,server_url+image_uri);
                            switch (noteBlocks.lastElement().getType()) {
                                case 1:
                                    noteBlock.setContent(2, "", Uri.parse(server_url+origin_detail), noteBlocks.lastElement().topMargin() + noteBlocks.lastElement().textView.getHeight());
                                    break;
                                case 2:
                                    noteBlock.setContent(2, "", Uri.parse(server_url+origin_detail), noteBlocks.lastElement().topMargin() + noteBlocks.lastElement().imageView.getHeight());
                                    break;
                                case 3:
                                    noteBlock.setContent(2, "", Uri.parse(server_url+origin_detail), noteBlocks.lastElement().topMargin() + noteBlocks.lastElement().audioComponent.getHeight());
                                    break;
                            }

                        }
                        Picasso.get()
                                .load(server_url + origin_detail)
                                .into(noteBlock.imageView);
                        Log.i(TAG, String.valueOf(noteBlock.topMargin()));
                        noteBlockId = View.generateViewId();
                        noteBlock.setId(noteBlockId);


                        // 将NoteBlock添加到父布局中
                        parentLayout.addView(noteBlock);
                        noteBlocks.add(noteBlock);

                        ImageView imageView = noteBlock.imageView;
                        imageView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                            int originHeight = imageView.getHeight();

                            @Override
                            public void onGlobalLayout() {
                                int newHeight = imageView.getHeight();
                                if (newHeight != originHeight) {
                                    int deltaHeight = newHeight - originHeight;
                                    originHeight = newHeight;

                                    // 调整其他 noteBlocks 的 topMargin
                                    for (int i = noteBlocks.indexOf(noteBlock) + 1; i < noteBlocks.size(); i++) {
                                        NoteBlock currentNoteBlock = noteBlocks.get(i);
                                        int newTopMargin = currentNoteBlock.topMargin() + deltaHeight;
                                        currentNoteBlock.setTopMarigin(newTopMargin);
                                        Log.i(TAG, "Change height of " + i + " deltaHeight= " + deltaHeight + " now topMargin = " + newTopMargin);
                                    }
                                }
                            }
                        });
                        imageView.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                // 当视图被点击时，执行以下代码
                                curfocus=noteBlock.number+1;
                                Log.i("LLL", String.valueOf(curfocus));
                                View currentFocus = getCurrentFocus();
                                if (currentFocus instanceof EditText) {
                                    currentFocus.clearFocus();
                                    InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
                                    if (imm != null) {
                                        imm.hideSoftInputFromWindow(currentFocus.getWindowToken(), 0);
                                    }
                                }
                            }
                        });
                        Log.i(TAG,"finish image setting");
                        ViewTreeObserver imageviewTreeObserver = noteBlock.imageView.getViewTreeObserver();
                        curfocus = noteBlock.number+1;
                        imageviewTreeObserver.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                            @Override
                            public void onGlobalLayout() {
                                Log.i(TAG,"finish block"+ i+"height"+noteBlock.getH());
                                drawold(i+1);
                                noteBlock.imageView.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                            }
                        });
                        break;
                    case "audio":

                        Log.i(TAG, "createaudio");
                        Log.i(TAG, origin_detail);
//                        AudioDownloader.downloadAudio(this, server_url, origin_detail,audio_uri);
//                        Log.i(TAG, String.valueOf(audio_uri));
                        currentFocus = getCurrentFocus();
                        if (currentFocus instanceof EditText) {
                            currentFocus.clearFocus();
                            InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
                            if (imm != null) {
                                imm.hideSoftInputFromWindow(currentFocus.getWindowToken(), 0);
                            }
                        }
                        noteBlock = new NoteBlock(that, 3, noteBlocks, note_code, noteBlocks.size());

                        // 设置内容（假设setContent方法的第一个参数1代表文本类型）
                        if (noteBlocks.isEmpty())
                            noteBlock.setContent(3, "", Uri.parse(server_url+origin_detail), 0);
                        else {
                            switch (noteBlocks.lastElement().getType()) {
                                case 1:
                                    noteBlock.setContent(3, "", Uri.parse(server_url+origin_detail), noteBlocks.lastElement().topMargin() + noteBlocks.lastElement().textView.getHeight());
                                    break;
                                case 2:
                                    noteBlock.setContent(3, "", Uri.parse(server_url+origin_detail), noteBlocks.lastElement().topMargin() + noteBlocks.lastElement().imageView.getHeight());
                                    break;
                                case 3:
                                    noteBlock.setContent(3, "", Uri.parse(server_url+origin_detail), noteBlocks.lastElement().topMargin() + noteBlocks.lastElement().audioComponent.getHeight());
                                    break;
                            }

                        }

                        noteBlockId = View.generateViewId();
                        noteBlock.setId(noteBlockId);


                        // 将NoteBlock添加到父布局中
                        parentLayout.addView(noteBlock);
                        noteBlocks.add(noteBlock);

                        LinearLayout audioComponent = noteBlock.audioComponent;
                        Log.i(TAG, "build success");
                        audioComponent.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                            int originHeight = audioComponent.getHeight();//

                            @Override
                            public void onGlobalLayout() {
                                int newHeight = audioComponent.getHeight();//
                                if (newHeight != originHeight) {
                                    int deltaHeight = newHeight - originHeight;
                                    originHeight = newHeight;

                                    // 调整其他 noteBlocks 的 topMargin
                                    for (int i = noteBlocks.indexOf(noteBlock) + 1; i < noteBlocks.size(); i++) {
                                        NoteBlock currentNoteBlock = noteBlocks.get(i);
                                        int newTopMargin = currentNoteBlock.topMargin() + deltaHeight;
                                        currentNoteBlock.setTopMarigin(newTopMargin);
                                        Log.i(TAG, "Change height of " + i + " deltaHeight= " + deltaHeight + " now topMargin = " + newTopMargin);
                                    }
                                }
                            }
                        });
                        audioComponent.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                // 当视图被点击时，执行以下代码
                                curfocus=noteBlock.number+1;
                                Log.i("LLL", String.valueOf(curfocus));
                                View currentFocus = getCurrentFocus();
                                if (currentFocus instanceof EditText) {
                                    currentFocus.clearFocus();
                                    InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
                                    if (imm != null) {
                                        imm.hideSoftInputFromWindow(currentFocus.getWindowToken(), 0);
                                    }
                                }
                            }
                        });

                        ViewTreeObserver audioviewTreeObserver = noteBlock.audioComponent.getViewTreeObserver();
                        curfocus = noteBlock.number+1;
                        audioviewTreeObserver.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                            @Override
                            public void onGlobalLayout() {
                                Log.i(TAG,"finish block"+ i+"height"+noteBlock.getH());
                                drawold(i+1);
                                noteBlock.audioComponent.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                            }
                        });
                        break;
                    default:
                        throw new IllegalStateException("Unexpected value: " + origin_type);
                }

            } catch (JSONException e) {
                throw new RuntimeException(e);
            }
        }
        return;
    }

    @Override
    public void onBackPressed() {
        // 设置返回结果
        Intent returnIntent = new Intent();
        setResult(Activity.RESULT_OK, returnIntent);
        Log.i(TAG,"back");
        View currentFocus = getCurrentFocus();
        if (currentFocus instanceof EditText) {
            currentFocus.clearFocus();
        }

        // 调用父类的onBackPressed方法，结束Activity
        super.onBackPressed();
    }

    @Override
    protected void onPause() {
        View currentFocus = getCurrentFocus();
        if (currentFocus instanceof EditText) {
            currentFocus.clearFocus();
        }
        for (int au =0; au<noteBlocks.size();au++)
        {
            if(noteBlocks.get(au).getType()==3 && noteBlocks.get(au).mediaPlayer.isPlaying())
            {
                noteBlocks.get(au).playButton.performClick();
            }
        }
        super.onPause();
    }
}
