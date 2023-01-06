package com.visioncamerabase64;

/*
 * Copyright 2020 Google LLC. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import android.annotation.TargetApi;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.media.Image;
import android.media.Image.Plane;
import android.os.Build.VERSION_CODES;
import androidx.annotation.Nullable;
import android.util.Log;
import androidx.annotation.RequiresApi;
import androidx.camera.core.ExperimentalGetImage;
import androidx.camera.core.ImageProxy;
import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;

/** Utils functions for bitmap conversions. */
public class BitmapUtils {
  private static final String TAG = "BitmapUtils";

  /** Converts NV21 format byte buffer to bitmap. */
  @Nullable
  public static Bitmap getBitmap(ByteBuffer data, FrameMetadata metadata) {
    data.rewind();
    byte[] imageInBuffer = new byte[data.limit()];
    data.get(imageInBuffer, 0, imageInBuffer.length);
    try {
      YuvImage image =
        new YuvImage(
          imageInBuffer, ImageFormat.NV21, metadata.getWidth(), metadata.getHeight(), null);
      ByteArrayOutputStream stream = new ByteArrayOutputStream();
      image.compressToJpeg(new Rect(0, 0, metadata.getWidth(), metadata.getHeight()), 80, stream);

      Bitmap bmp = BitmapFactory.decodeByteArray(stream.toByteArray(), 0, stream.size());

      stream.close();
      return rotateBitmap(bmp, metadata.getRotation(), false, false);
    } catch (Exception e) {
      Log.e("VisionProcessorBase", "Error: " + e.getMessage());
    }
    return null;
  }

  /** Converts a YUV_420_888 image from CameraX API to a bitmap. */
  @RequiresApi(VERSION_CODES.LOLLIPOP)
  @Nullable
  @ExperimentalGetImage
  public static Bitmap getBitmap(ImageProxy image) {
    FrameMetadata frameMetadata =
      new FrameMetadata.Builder()
        .setWidth(image.getWidth())
        .setHeight(image.getHeight())
        .setRotation(image.getImageInfo().getRotationDegrees())
        .build();

    ByteBuffer nv21Buffer =
      yuv420ThreePlanesToNV21(image.getImage().getPlanes(), image.getWidth(), image.getHeight());
    return getBitmap(nv21Buffer, frameMetadata);
  }

  /** Rotates a bitmap if it is converted from a bytebuffer. */
  private static Bitmap rotateBitmap(
    Bitmap bitmap, int rotationDegrees, boolean flipX, boolean flipY) {
    Matrix matrix = new Matrix();

    // Rotate the image back to straight.
    matrix.postRotate(rotationDegrees);

    // Mirror the image along the X or Y axis.
    matrix.postScale(flipX ? -1.0f : 1.0f, flipY ? -1.0f : 1.0f);
    Bitmap rotatedBitmap =
      Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);

    // Recycle the old bitmap if it has changed.
    if (rotatedBitmap != bitmap) {
      bitmap.recycle();
    }
    return rotatedBitmap;
  }

  @RequiresApi(VERSION_CODES.KITKAT)
  private static ByteBuffer yuv420ThreePlanesToNV21(
    Plane[] yuv420888planes, int width, int height) {
    int imageSize = width * height;
    byte[] out = new byte[imageSize + 2 * (imageSize / 4)];

    if (areUVPlanesNV21(yuv420888planes, width, height)) {
      // Copy the Y values.
      yuv420888planes[0].getBuffer().get(out, 0, imageSize);

      ByteBuffer uBuffer = yuv420888planes[1].getBuffer();
      ByteBuffer vBuffer = yuv420888planes[2].getBuffer();
      // Get the first V value from the V buffer, since the U buffer does not contain it.
      vBuffer.get(out, imageSize, 1);
      // Copy the first U value and the remaining VU values from the U buffer.
      uBuffer.get(out, imageSize + 1, 2 * imageSize / 4 - 1);
    } else {
      // Fallback to copying the UV values one by one, which is slower but also works.
      // Unpack Y.
      unpackPlane(yuv420888planes[0], width, height, out, 0, 1);
      // Unpack U.
      unpackPlane(yuv420888planes[1], width, height, out, imageSize + 1, 2);
      // Unpack V.
      unpackPlane(yuv420888planes[2], width, height, out, imageSize, 2);
    }

    return ByteBuffer.wrap(out);
  }

  /** Checks if the UV plane buffers of a YUV_420_888 image are in the NV21 format. */
  @RequiresApi(VERSION_CODES.KITKAT)
  private static boolean areUVPlanesNV21(Plane[] planes, int width, int height) {
    int imageSize = width * height;

    ByteBuffer uBuffer = planes[1].getBuffer();
    ByteBuffer vBuffer = planes[2].getBuffer();

    // Backup buffer properties.
    int vBufferPosition = vBuffer.position();
    int uBufferLimit = uBuffer.limit();

    // Advance the V buffer by 1 byte, since the U buffer will not contain the first V value.
    vBuffer.position(vBufferPosition + 1);
    // Chop off the last byte of the U buffer, since the V buffer will not contain the last U value.
    uBuffer.limit(uBufferLimit - 1);

    // Check that the buffers are equal and have the expected number of elements.
    boolean areNV21 =
      (vBuffer.remaining() == (2 * imageSize / 4 - 2)) && (vBuffer.compareTo(uBuffer) == 0);

    // Restore buffers to their initial state.
    vBuffer.position(vBufferPosition);
    uBuffer.limit(uBufferLimit);

    return areNV21;
  }

  /**
   * Unpack an image plane into a byte array.
   *
   * <p>The input plane data will be copied in 'out', starting at 'offset' and every pixel will be
   * spaced by 'pixelStride'. Note that there is no row padding on the output.
   */
  @TargetApi(VERSION_CODES.KITKAT)
  private static void unpackPlane(
    Plane plane, int width, int height, byte[] out, int offset, int pixelStride) {
    ByteBuffer buffer = plane.getBuffer();
    buffer.rewind();

    // Compute the size of the current plane.
    // We assume that it has the aspect ratio as the original image.
    int numRow = (buffer.limit() + plane.getRowStride() - 1) / plane.getRowStride();
    if (numRow == 0) {
      return;
    }
    int scaleFactor = height / numRow;
    int numCol = width / scaleFactor;

    // Extract the data in the output buffer.
    int outputPos = offset;
    int rowStart = 0;
    for (int row = 0; row < numRow; row++) {
      int inputPos = rowStart;
      for (int col = 0; col < numCol; col++) {
        out[outputPos] = buffer.get(inputPos);
        outputPos += pixelStride;
        inputPos += plane.getPixelStride();
      }
      rowStart += plane.getRowStride();
    }
  }
}
