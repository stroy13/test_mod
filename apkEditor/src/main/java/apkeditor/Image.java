package apkeditor;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

public class Image {
    public Bitmap bitmap;

    public Image(Context context, int id) {
        this.bitmap = BitmapFactory.decodeResource(context.getResources(), id);
    }

    public Image(Context context, String name, float scaleW, float scaleH) throws IOException {
        InputStream is = new FileInputStream(name);
        this.bitmap = BitmapFactory.decodeStream(is);
        is.close();
    }

    public static void free(Image image) {
        try {
            if (image.bitmap != null) {
                if (!image.bitmap.isRecycled()) {
                    image.bitmap.recycle();
                    if (image.bitmap.isRecycled()) {
                        image.bitmap = null;
                    }
                } else {
                    image.bitmap = null;
                }
            } else {
                image.bitmap = null;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public int getWidth() {
        return this.bitmap.getWidth();
    }

    public int getHeight() {
        return this.bitmap.getHeight();
    }

    public int[] getPixel(int[] array, int x, int y) {
        this.bitmap.getPixels(array, 0, getWidth(), 0, 0, getWidth(), getHeight());
        return array;
    }

    public Bitmap CreateImage(int[] array, int w, int h) {
        Bitmap image = Bitmap.createBitmap(array, w, h, Bitmap.Config.ARGB_4444);
        return image;
    }

    private Bitmap setScaleSize(Bitmap bitmap, float arg0, float arg1) {
        Matrix matrix = new Matrix();
        matrix.postScale(arg0, arg1);
        return Bitmap.createBitmap(bitmap, 0, 0, getWidth(), getHeight(), matrix, true);
    }
}
