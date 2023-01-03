package com.visioncamerabase64;

import androidx.camera.core.ImageProxy;
import com.mrousavy.camera.frameprocessor.FrameProcessorPlugin;

public class Base64FrameProcessorPlugin extends FrameProcessorPlugin {

  @Override
  public Object callback(ImageProxy image, Object[] params) {
    // code goes here
    return null;
  }

  Base64FrameProcessorPlugin() {
    super("frameToBase64");
  }
}