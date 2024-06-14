package com.example.myapplication;

import android.content.ContentResolver;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.AssetFileDescriptor;
import android.database.Cursor;
import android.media.MediaPlayer;
import android.net.Uri;
import android.provider.OpenableColumns;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextClock;
import android.widget.TextView;
import android.widget.VideoView;
import android.os.Handler;


import org.json.JSONObject;

import java.io.BufferedWriter;
import java.io.IOException;

import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Scanner;
import java.util.Vector;
import java.util.logging.LogRecord;

public class NoteBlock extends RelativeLayout {

    String TAG = "data:---->";
    Context context;
    String user_id,auth_id,server_url;
    String note_code;
    private View content;
    private int topMargin;
    private int blockType;
    boolean menucall = false;
    Vector<NoteBlock> noteBlockList;
    EditText textView;
    ImageView imageView;

    ImageView playButton;
    MediaPlayer mediaPlayer;

    TextView audioTitle;
    SeekBar seekBar;

    LinearLayout audioComponent;

    int number;

    public NoteBlock(Context fathercontext, int type, Vector<NoteBlock> nb, String nc, int pos) {
        super(fathercontext);
        context = fathercontext;
        noteBlockList = nb;
        note_code = nc;
        number = pos;
        init(type);
    }

    public NoteBlock(Context fathercontext, AttributeSet attrs, int type, Vector<NoteBlock> nb, String nc, int pos) {
        super(fathercontext, attrs);
        context = fathercontext;
        noteBlockList = nb;
        number = pos;
        init(type);
    }

    public NoteBlock(Context fathercontext, AttributeSet attrs, int defStyleAttr, int type, Vector<NoteBlock> nb, String nc, int pos) {
        super(fathercontext, attrs, defStyleAttr);
        context = fathercontext;
        noteBlockList = nb;
        note_code = nc;
        number = pos;
        init(type);
    }

    private void init(int type) {
        SharedPreferences sharedPreferences = context.getSharedPreferences("prefs", Context.MODE_PRIVATE);
        user_id = sharedPreferences.getString("user_id", "0");
        auth_id = sharedPreferences.getString("auth_id", "");
        server_url = sharedPreferences.getString("server_url","http://10.0.2.2:8000/");
        Log.i(TAG,"number = "+number);
        blockType = type;
        Log.i(TAG,"noteblock type="+type);
        if (type == 1) {
            content = new EditText(context);
        }
        else if (type == 2) {
            content = new ImageView(context);
        }
        else if (type == 3){
            content = new TextClock(context);
        }
        setId(View.generateViewId()); // 生成唯一ID
    }

    public int getType()
    {
        return blockType;
    }
    // 设置内容模块
    public void setContent(int type, String text, Uri uri , int toppos) {
        LayoutInflater inflater = LayoutInflater.from(getContext());
        if (type == 1) { // TextView
            content = inflater.inflate(R.layout.note_text, this, false);
            textView = content.findViewById(R.id.note_text);
            textView.setText(text);
            RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(
                    RelativeLayout.LayoutParams.MATCH_PARENT, // 宽度为 MATCH_PARENT
                    RelativeLayout.LayoutParams.WRAP_CONTENT  // 高度为 WRAP_CONTENT，根据内容自动调整
            );

            // 设置顶边距
            layoutParams.topMargin = toppos;
            topMargin = toppos;
            content.setLayoutParams(layoutParams);


            Log.i(TAG, "top pos" + toppos);
            textView.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    showPopupMenu(1);
                    return true;
                }
            });




        }
        else if (type == 2) { // ImageView
            Log.i(TAG,"start set image:"+uri+" pos:"+toppos);
            content = inflater.inflate(R.layout.note_image, this, false);
            imageView = content.findViewById(R.id.note_image);
            imageView.setImageURI(uri);
            RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(
                    RelativeLayout.LayoutParams.MATCH_PARENT, // 宽度为 MATCH_PARENT
                    RelativeLayout.LayoutParams.WRAP_CONTENT  // 高度为 WRAP_CONTENT，根据内容自动调整
            );

            // 设置顶边距
            layoutParams.topMargin = toppos;
            topMargin = toppos;
            content.setLayoutParams(layoutParams);


            imageView.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    showPopupMenu(2);
                    return true;
                }
            });
        }
        else if (type == 3) {
            Log.i(TAG,"start set audio");
            content = inflater.inflate(R.layout.note_audio, this, false);
            audioComponent = content.findViewById(R.id.audio_component);
            audioTitle = content.findViewById(R.id.audio_title);
            playButton = content.findViewById(R.id.play_button);
            seekBar = content.findViewById(R.id.seekBar);
            Runnable runnable;
            Handler handler =  new Handler() ;
            mediaPlayer = new MediaPlayer();
            Log.i(TAG,"0");


            RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(
                    RelativeLayout.LayoutParams.MATCH_PARENT, // 宽度为 MATCH_PARENT
                    RelativeLayout.LayoutParams.WRAP_CONTENT  // 高度为 WRAP_CONTENT，根据内容自动调整
            );
            Log.i(TAG,"2");
            // 设置顶边距
            layoutParams.topMargin = toppos;
            topMargin = toppos;
            content.setLayoutParams(layoutParams);
            Log.i(TAG,String.valueOf(uri));
            try {
                ContentResolver contentResolver = getContext().getContentResolver();
                String fileName = "audio";
                if (String.valueOf(uri).startsWith("content"))
                {
                    AssetFileDescriptor afd = contentResolver.openAssetFileDescriptor(uri, "r");
                    if (afd != null) {
                        mediaPlayer.setDataSource(afd.getFileDescriptor(), afd.getStartOffset(), afd.getLength());
                        afd.close();
                    }
                    try (Cursor cursor = contentResolver.query(uri, null, null, null, null)) {
                        if (cursor != null && cursor.moveToFirst()) {
                            int nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                            fileName = cursor.getString(nameIndex);
                        }
                    }
                }
                else
                {
                    mediaPlayer.setDataSource(String.valueOf(uri));
                    int lastSlashIndex = String.valueOf(uri).lastIndexOf('/');
                    fileName = String.valueOf(uri).substring(lastSlashIndex + 1);
                }


                mediaPlayer.prepare();



                audioTitle.setText(fileName);
                Log.i(TAG,"2.5");
            } catch (IOException e) {
                e.printStackTrace();
            }

            Log.i(TAG,"3");

            mediaPlayer.setOnCompletionListener(mp -> playButton.setImageResource(R.drawable.play_button));


            Log.i(TAG,"3.3");

            playButton.setImageResource(R.drawable.play_button);
            Log.i(TAG,"3.5");
            playButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (mediaPlayer.isPlaying()) {
                        mediaPlayer.pause();
                        playButton.setImageResource(R.drawable.play_button);
                    } else {
                        mediaPlayer.start();
                        playButton.setImageResource(R.drawable.pause_button);
                        updateSeekBar(seekBar, handler, mediaPlayer);
                    }
                }
            });

            Log.i(TAG,"4");
            seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    if (fromUser) {
                        mediaPlayer.seekTo(progress);
                    }
                }

                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {}

                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {}
            });
            Log.i(TAG,"4.3");
            seekBar.setMax(mediaPlayer.getDuration());

            audioComponent.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    showPopupMenu(3);
                    return true;
                }
            });

        }
        // 如果需要，可以添加视频播放组件的逻辑
        this.addView(content);
        Log.i(TAG, "height" + getH());
        Log.i(TAG,"5");
    }

//    @Override
//    public boolean onTouchEvent(MotionEvent event) {
//
//        return true;
//    }

    private void showPopupMenu(int type) {
        if(type==1)
        {
            if (content != null) {
                PopupMenu contentPopupMenu = new PopupMenu(getContext(), textView);
                contentPopupMenu.getMenuInflater().inflate(R.menu.note_text_menu, contentPopupMenu.getMenu());

                // 设置PopupMenu的点击事件
                contentPopupMenu.setOnMenuItemClickListener(item -> {
                    switch (item.getItemId()) {
                        case R.id.menu_delete:
                            // 处理删除操作
                            deleteContent();
                            return true;
                        default:
                            return false;
                    }
                });
                contentPopupMenu.show();
            }
        }
        else if(type==2)
        {
            if (content != null) {
                PopupMenu contentPopupMenu = new PopupMenu(getContext(), imageView);
                contentPopupMenu.getMenuInflater().inflate(R.menu.note_image_menu, contentPopupMenu.getMenu());

                // 设置PopupMenu的点击事件
                contentPopupMenu.setOnMenuItemClickListener(item -> {
                    switch (item.getItemId()) {
                        case R.id.menu_delete:
                            // 处理删除操作
                            deleteContent();
                            return true;
                        default:
                            return false;
                    }
                });
                contentPopupMenu.show();
            }
        }
        else if(type==3)
        {
            if (content != null) {
                PopupMenu contentPopupMenu = new PopupMenu(getContext(), content);
                contentPopupMenu.getMenuInflater().inflate(R.menu.note_audio_menu, contentPopupMenu.getMenu());

                // 设置PopupMenu的点击事件
                contentPopupMenu.setOnMenuItemClickListener(item -> {
                    switch (item.getItemId()) {
                        case R.id.menu_delete:
                            // 处理删除操作
                            deleteContent();
                            return true;
                        default:
                            return false;
                    }
                });
                contentPopupMenu.show();
            }
        }
    }

    // 处理菜单操作
    private void editText() {
        // 这里可以添加编辑content的代码
        // 例如，启动一个编辑内容的Activity或Dialog
        textView.setFocusable(true);
        menucall = true;
        textView.performClick();
    }

    private void deleteContent() {
        // 这里可以添加删除content的代码
        // 例如，从视图中移除content，或者从数据源中删除对应的数据
        ViewGroup parent = (ViewGroup) this.getParent();
        if (parent != null) {
            parent.removeView(this);
        }
        new Thread(){
            @Override
            public void run() {

                HttpURLConnection connection=null;
                try {
                    URL url = new URL(server_url+"delete_content/");
                    connection = (HttpURLConnection) url.openConnection();
                    connection.setConnectTimeout(3000);
                    connection.setReadTimeout(3000);
                    //设置请求方式
                    connection.setRequestMethod("POST");
                    connection.setDoInput(true);
                    connection.setDoOutput(true);
                    connection.connect();

                    String body = "note_id=" + note_code +"&user_id=" + user_id + "&auth_id=" + auth_id + "&type=text" + "&order=" + String.valueOf(number +1)  ;
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
                                Log.i(TAG,"delete noteblock ok");
                            }
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }.start();

        noteBlockList.remove(number);
        int deltaHeight = getH();
        for (int i = number; i < noteBlockList.size(); i++) {
            noteBlockList.get(i).setTopMarigin(noteBlockList.get(i).topMargin() - deltaHeight);
            noteBlockList.get(i).number = noteBlockList.get(i).number-1;
        }

    }


    public int getH() {
        return content.getHeight();
    }

    public int topMargin(){
        return topMargin;
    }


    public void setTopMarigin(int topH) {
        RelativeLayout.LayoutParams lp = (LayoutParams) content.getLayoutParams();
        lp.topMargin=topH;
        topMargin = topH;
        content.setLayoutParams(lp);
        return;
    }
    private void updateSeekBar(SeekBar seekBar, Handler handler, MediaPlayer mediaPlayer) {
        seekBar.setProgress(mediaPlayer.getCurrentPosition());
        if (mediaPlayer.isPlaying()) {
            Runnable updater = new Runnable() {
                @Override
                public void run() {
                    updateSeekBar(seekBar, handler, mediaPlayer);
                }
            };
            handler.postDelayed(updater, 1000); // 每秒更新一次
        }
    }
}
