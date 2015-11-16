package com.example.xiachen.myimageloader;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.widget.ImageView;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * Created by xiachen on 15/11/14.
 */
public class DownloadImgUtils {

    public static boolean downloadImgByUrl(String path, File file) {
        FileOutputStream fos = null;
        InputStream is = null;
        try {
            URL url = new URL(path);
            HttpURLConnection conn = ((HttpURLConnection) url.openConnection());
            is = conn.getInputStream();
            fos = new FileOutputStream(file);
            byte[] buf = new byte[512];
            int len = 0;
            while ((len = is.read(buf)) != -1) {
                fos.write(buf, 0, len);
            }
            fos.flush();
            return true;
        } catch (Exception e) {

        } finally {
            try {
                if (fos != null) {
                    fos.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            try {
                if (is != null) {
                    is.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return false;
    }

    public static Bitmap downloadImgByUrl(String urlStr, ImageView imageView) {

        InputStream is = null;
        try {
            URL url = new URL(urlStr);
            HttpURLConnection conn = ((HttpURLConnection) url.openConnection());
            is = new BufferedInputStream(conn.getInputStream());
            is.mark(is.available());
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            Bitmap bitmap;
            ImageSizeUtil.ImageSize imageViewSize = ImageSizeUtil.getImageViewSize(imageView);
            options.inSampleSize = ImageSizeUtil.caculateInSampleSize(options, imageViewSize.width, imageViewSize.height);
            options.inJustDecodeBounds = false;
            is.reset();
            bitmap = BitmapFactory.decodeStream(is, null, options);
            conn.disconnect();
            return bitmap;
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (is != null) {
                    is.close();
                }

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return null;
    }
}
