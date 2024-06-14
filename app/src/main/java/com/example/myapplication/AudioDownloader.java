package com.example.myapplication;

import android.content.Context;
import android.net.Uri;
import android.os.Environment;
import android.util.Log;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class AudioDownloader {

    private static final String TAG = "data:---->";

    public static void downloadAudio(Context context, String server_uri, String audioUrl, Uri target) {
        Log.i(TAG,"0");
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(server_uri)
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        Log.i(TAG,server_uri);
        ApiService apiService = retrofit.create(ApiService.class);
        Call<ResponseBody> call = apiService.downloadAudioFile(audioUrl);
        Log.i(TAG,audioUrl);
        call.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (response.isSuccessful()) {
                    Log.i(TAG,"on saving");
                    saveAudioToFile(context, response.body(), target);
                } else {
                    Log.e(TAG, "Download failed");
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                Log.e(TAG, "Download failed: " + t.getMessage());
            }
        });
    }

    private static void saveAudioToFile(Context context, ResponseBody body, Uri target) {
        try {
            File audioFile = new File(context.getExternalFilesDir(Environment.DIRECTORY_MUSIC), "audio.mp3");
            FileOutputStream outputStream = new FileOutputStream(audioFile);
            outputStream.write(body.bytes());
            outputStream.close();
            Log.d(TAG, "Audio file saved: " + audioFile.getAbsolutePath());

            // 将本地文件的 URI 存储在 target 中
            Uri audioUri = Uri.fromFile(audioFile);
            if (audioUri != null) {
                target = audioUri;
                Log.d(TAG, "Audio URI: " + target.toString());
            } else {
                Log.e(TAG, "Failed to get local audio URI");
            }
        } catch (IOException e) {
            Log.e(TAG, "Save audio file failed: " + e.getMessage());
        }
    }
}
