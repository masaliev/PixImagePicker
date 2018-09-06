package com.fxn;

import android.graphics.Bitmap;

public class Utility {
    public static Bitmap getScaledBitmap(int maxWidth, Bitmap rotatedBitmap) {
        try {

            int nh = (int) (rotatedBitmap.getHeight() * (512.0 / rotatedBitmap.getWidth()));
            return Bitmap.createScaledBitmap(rotatedBitmap, maxWidth, nh, true);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
