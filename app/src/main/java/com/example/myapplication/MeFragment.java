package com.example.myapplication;

import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.ParcelFileDescriptor;
import android.provider.MediaStore;
import android.provider.Settings;
import android.support.annotation.RequiresApi;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.squareup.picasso.Picasso;

import org.json.JSONObject;

import java.io.BufferedOutputStream;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.DataOutput;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class MeFragment extends Fragment {

    private static final int REQUEST_CODE_PERMISSIONS = 1;
    private static final String[] REQUIRED_PERMISSIONS = {
            android.Manifest.permission.READ_MEDIA_IMAGES,
            android.Manifest.permission.READ_MEDIA_VIDEO,
            android.Manifest.permission.READ_MEDIA_AUDIO
    };
    static String TAG = "data---->";
    String username;
    String motto;
    String avatar;
    String auth_id;
    String user_id;

    // 创建 BottomSheetDialog
    BottomSheetDialog bottomSheetDialog;
    TextView cancelop;
    ImageView Avatar;

    Uri avatar_uri;
    String server_url;

    public MeFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        if (!allPermissionsGranted()) {
            ActivityCompat.requestPermissions(getActivity(), REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS);
        }

        // 膨胀布局并获取视图对象
        View view = inflater.inflate(R.layout.me_fragment, container, false);

        //获取全局变量
        SharedPreferences sharedPreferences = getActivity().getSharedPreferences("prefs", Context.MODE_PRIVATE);
        user_id = sharedPreferences.getString("user_id", "0");
        auth_id = sharedPreferences.getString("auth_id", "");
        server_url = sharedPreferences.getString("server_url", "http://10.0.2.2:8000");
        final Boolean[] wordschanged = {false, false};

        // 创建 BottomSheetDialog
        bottomSheetDialog = new BottomSheetDialog(requireContext());
        EditText User_name = view.findViewById(R.id.me_username);
        EditText Motto = view.findViewById(R.id.me_motto);
        Avatar = view.findViewById(R.id.me_avatar);

        Avatar.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                Log.e("a","start trans");
                Avatar.getViewTreeObserver().removeOnGlobalLayoutListener(this);
            }
        });

        Avatar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.i(TAG,"start click");
                Intent intent = new Intent(Intent.ACTION_PICK, null);
                intent.setDataAndType(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, "image/*");
                Log.i(TAG,"do click");
                startActivityForResult(intent, 2);
            }
        });

        Avatar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showBottomSheetDialog();
            }
        });

        User_name.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showBottomSheetDialog();
            }
        });

        Motto.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showBottomSheetDialog();
            }
        });


        new Thread(){

            @Override
            public void run() {
                HttpURLConnection connection=null;
                try {
                    String urlString = server_url+"user_info/?user_id=" + user_id + "&auth_id=" + auth_id;
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
                                username = jsonObject.getString("username");
                                motto = jsonObject.getString("motto");
                                avatar = jsonObject.getString("avatar");
                            }
                            else if(status.equals("refuse"))
                            {
                                String refusereason = jsonObject.getString("msg");
                                getActivity().runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        // 创建对话框
                                        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                                        builder.setTitle("请求用户信息异常");
                                        builder.setMessage(refusereason);

                                        builder.show(); // 显示对话框
                                    }
                                });
                            }
                        }
                    }
                    getActivity().runOnUiThread(() -> {
                        User_name.setText(username);
                        Motto.setText(motto);
                        Picasso.get()
                                .load(server_url +avatar)
                                .transform(new CircleTransform())
                                .resize(200,200)
                                .centerCrop()
                                .into(Avatar);
                    });
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }.start();


        // Inflate the layout for this fragment
        return view;


    }
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {

        super.onActivityResult(requestCode, resultCode, data);
        Log.i(TAG, "result code="+ resultCode);
        if (requestCode == 2 && data!=null) {
            // 得到图片的全路径
            avatar_uri = data.getData();
            Log.d("data---->",data.toString());
            Log.d("data---->",data.getData().toString());
            //Avatar.setImageURI(avatar_uri);
            Picasso.get()
                    .load(avatar_uri)
                    .transform(new CircleTransform())
                    .resize(200,200)
                    .centerCrop()
                    .into(Avatar);

            String path = uriToBitmap(avatar_uri,getContext());

            File file = new File(path);

            if(file.exists())
                Log.i(TAG,"exists");
            OkHttpClient httpClient = new OkHttpClient();
            MediaType mediatype = MediaType.Companion.parse("image/jpg");
            RequestBody fileBody = RequestBody.Companion.create(file,mediatype);
            RequestBody requestBody = new MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart("avatar", file.getName(), fileBody)
                    .addFormDataPart("user_id", user_id)
                    .addFormDataPart("auth_id", auth_id)
                    .build();
            Request getRequest = new Request.Builder()
                    .url(server_url+"change_avatar/")
                    .post(requestBody)
                    .build();

            Call call = httpClient.newCall(getRequest);
            Log.i(TAG, path);

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
            if(bottomSheetDialog.isShowing())
                bottomSheetDialog.dismiss();
        }
        if (data == null)
        {
            Log.e("data","data is null");
        }
    }

    private void showBottomSheetDialog() {
        if(!bottomSheetDialog.isShowing())
        {// 设置底部弹出框的布局
            // 显示 BottomSheetDialog
            View bottomSheetView = getLayoutInflater().inflate(R.layout.bottom_sheet_dialog, null);
            bottomSheetDialog.setContentView(bottomSheetView);
            bottomSheetDialog.show();
            cancelop = bottomSheetView.findViewById(R.id.option_cancel);
            cancelop.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    showBottomSheetDialog();
                }
            });
        }
        else
        {
            bottomSheetDialog.dismiss();
        }
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
                if (null != c) {
                    if (c.moveToFirst()) {
                        int columnIndex = c.getColumnIndex(filePathColumns[0]);
                        imagePath = c.getString(columnIndex);
                    }
                    c.close();
                }
            }
        }
        Log.i(TAG, "getResult imagePath " + imagePath);
        return imagePath;
    }


    private boolean allPermissionsGranted() {
        for (String permission : REQUIRED_PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(getActivity(), permission) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (!allPermissionsGranted()) {
                Toast.makeText(getActivity(), "Permissions not granted by the user.", Toast.LENGTH_SHORT).show();
            }
        }
    }



}

