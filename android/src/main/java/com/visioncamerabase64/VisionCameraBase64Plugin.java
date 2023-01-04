package com.visioncamerabase64;

import android.annotation.SuppressLint;
import android.graphics.Bitmap;
import android.util.Base64;
import androidx.camera.core.ImageProxy;
import com.mrousavy.camera.frameprocessor.FrameProcessorPlugin;
import java.io.ByteArrayOutputStream;

  public class VisionCameraBase64Plugin extends FrameProcessorPlugin {

  @Override
  public Object callback(ImageProxy image, Object[] params) {
    // TODO: image format and quality must come from params
    Bitmap.CompressFormat imageFormat = Bitmap.CompressFormat.PNG;
    int quality = 100;

    @SuppressLint("UnsafeOptInUsageError")
    Bitmap bitmap = BitmapUtils.getBitmap(image);
    return bitmapToBase64(bitmap, imageFormat, quality);
  }

  /** Converts a bitmap to base64 format string */
  public static String bitmapToBase64(Bitmap bitmap, Bitmap.CompressFormat format, int quality)
  {
    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    bitmap.compress(format, quality, outputStream);

    return Base64.encodeToString(outputStream.toByteArray(), Base64.DEFAULT);
  }

  VisionCameraBase64Plugin() {
    super("frameToBase64");
  }
  }
