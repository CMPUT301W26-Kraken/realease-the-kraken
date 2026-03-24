package com.example.releasethekraken.util;

import android.graphics.Bitmap;
import android.graphics.Color;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;

/**
 * Utility class to generate QR code Bitmaps from strings.
 */
public class QRCodeGenerator {

    /**
     * Generates a QR code Bitmap for the given text.
     *
     * @param text   The content to encode in the QR code.
     * @param width  The width of the Bitmap.
     * @param height The height of the Bitmap.
     * @return A Bitmap containing the QR code, or null if generation fails.
     */
    public static Bitmap generateQRCode(String text, int width, int height) {
        try {
            BitMatrix bitMatrix = new MultiFormatWriter().encode(text, BarcodeFormat.QR_CODE, width, height);
            Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);
            for (int x = 0; x < width; x++) {
                for (int y = 0; y < height; y++) {
                    bitmap.setPixel(x, y, bitMatrix.get(x, y) ? Color.BLACK : Color.WHITE);
                }
            }
            return bitmap;
        } catch (WriterException e) {
            e.printStackTrace();
            return null;
        }
    }
}
