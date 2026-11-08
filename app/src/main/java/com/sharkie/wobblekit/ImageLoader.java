package com.sharkie.wobblekit;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import java.io.File;

final class ImageLoader {
    private static final int MAX_DIMENSION = 2048;

    static Bitmap load(File file) {
        String path = file.getAbsolutePath();
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(path, options);
        if (options.outWidth <= 0 || options.outHeight <= 0) return null;
        options.inJustDecodeBounds = false;
        options.inSampleSize = WobbleMath.imageSampleSize(options.outWidth, options.outHeight, MAX_DIMENSION);
        return BitmapFactory.decodeFile(path, options);
    }
}
