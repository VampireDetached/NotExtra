package com.example.myapplication;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class FileUtils {

    public static void copyInputStreamToFile(InputStream inputStream, File file) throws IOException {
        try (FileOutputStream outputStream = new FileOutputStream(file)) {
            byte[] buffer = new byte[4096];
            int length;
            while ((length = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, length);
            }
        }
    }
}
