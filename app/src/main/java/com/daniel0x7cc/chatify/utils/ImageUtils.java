package com.daniel0x7cc.chatify.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Environment;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Created by daniel0x7cc on 5/21/17.
 */

public class ImageUtils {
    public static File scaleImageFile(Context context, final Uri uri, String fileNamePrefix, int reqBounds) {
        FileOutputStream os = null;
        InputStream is = null;

        try {
            boolean isPNG = false;
            if (uri.getPath().endsWith(".png")) {
                isPNG = true;
            }

            // First decode with inJustDecodeBounds=true to check dimensions
            is = context.getContentResolver().openInputStream(uri);
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            BitmapFactory.decodeStream(is, null, options);
            if (is != null) {
                is.close();
            }

            // Calculate inSampleSize
            options.inSampleSize = calculateInSampleSize(options, reqBounds, reqBounds);

            // Decode bitmap with inSampleSize set
            options.inJustDecodeBounds = false;
            is = context.getContentResolver().openInputStream(uri);
            Bitmap bitmap = BitmapFactory.decodeStream(is, null, options);
            if (is != null) {
                is.close();
            }

            bitmap = adjustOrientation(uri, bitmap);

            // Save new bitmap on temp file
            File tmpFile;
            if (isPNG) {
                File storageDir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES);
                tmpFile = File.createTempFile(fileNamePrefix, ".png", storageDir);
                os = new FileOutputStream(tmpFile);
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, os);

            } else {
                File storageDir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES);
                tmpFile = File.createTempFile(fileNamePrefix, ".jpg", storageDir);
                os = new FileOutputStream(tmpFile);
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, os);
            }

            File originalFile = new File(uri.getPath());
            if (originalFile.length() == 0 || !originalFile.exists() || tmpFile.length() < originalFile.length()) {
                return tmpFile;
            } else {
                return originalFile;
            }
        } catch (Exception e) {
            LogUtils.e("Erro ao reduzir escala da imagem.", e);
            try {
                return new File(uri.getPath());
            } catch (Exception ex) {
                LogUtils.e("Erro ao reduzir escala da imagem.", ex);
                return null;
            }

        } finally {
            if (os != null) {
                try {
                    os.close();
                } catch (IOException ignore) {}
            }
            if (is != null) {
                try {
                    is.close();
                } catch (IOException ignore) {}
            }
        }
    }

    private static int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {
        // Raw height and width of image
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {
            final int halfHeight = height / 2;
            final int halfWidth = width / 2;

            // Calculate the largest inSampleSize value that is a power of 2 and keeps both
            // height and width larger than the requested height and width.
            while ((halfHeight / inSampleSize) >= reqHeight
                    && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2;
            }
        }

        return inSampleSize;
    }

    private static Bitmap adjustOrientation(Uri uri, Bitmap bitmap) {
        try {
            // Workaround because ExifInterface cant open file from URI created by FileProvider
            String path = uri.getPath();
            final String fileProviderPathSufix = "/external_files";
            if (path.startsWith(fileProviderPathSufix)) {
                path = Environment.getExternalStorageDirectory()
                        + path.substring(fileProviderPathSufix.length());
            }

            ExifInterface ei = new ExifInterface(path);
            int orientation = ei.getAttributeInt(ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_UNDEFINED);
            switch (orientation) {
                case ExifInterface.ORIENTATION_ROTATE_90:
                    bitmap = rotateImage(bitmap, 90);
                    break;

                case ExifInterface.ORIENTATION_ROTATE_180:
                    bitmap = rotateImage(bitmap, 180);
                    break;

                case ExifInterface.ORIENTATION_ROTATE_270:
                    bitmap = rotateImage(bitmap, 270);
                    break;

                case ExifInterface.ORIENTATION_NORMAL:
                default:
                    break;
            }
        } catch (Exception e) {
            LogUtils.e("Erro ao ajustar orianteção do Bitmap.", e);
        }
        return bitmap;
    }

    private static Bitmap rotateImage(Bitmap source, float angle) {
        try {
            Matrix matrix = new Matrix();
            matrix.postRotate(angle);
            return Bitmap.createBitmap(source, 0, 0, source.getWidth(), source.getHeight(),
                    matrix, true);
        } catch (Exception e) {
            LogUtils.e("Erro ao rotacionar Bitmap.", e);
        }
        return source;
    }
}
