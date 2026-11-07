package com.gmail.heagoo.apkeditor;

import android.graphics.Bitmap;

public class ImageThumbnailInfo {
    public String detailInfo;
    public Bitmap thumbnail;

    public ImageThumbnailInfo(Bitmap thumbnail, String info) {
        this.thumbnail = thumbnail;
        this.detailInfo = info;
    }
}
