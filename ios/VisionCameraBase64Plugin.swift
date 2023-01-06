
import Foundation

@objc(VisionCameraBase64Plugin)
public class VisionCameraBase64Plugin: NSObject, FrameProcessorPluginBase {
    private static let context = CIContext(options: nil)
    @objc
    public static func callback(_ frame: Frame!, withArgs _: [Any]!) -> Any! {
        guard let imageBuffer = CMSampleBufferGetImageBuffer(frame.buffer) else {
          print("Failed to get CVPixelBuffer!")
          return nil
        }
        let ciImage = CIImage(cvPixelBuffer: imageBuffer)

        guard let cgImage = context.createCGImage(ciImage, from: ciImage.extent) else {
          print("Failed to create CGImage!")
          return nil
        }
        let image = UIImage(cgImage: cgImage)
        let imageData = image.jpegData(compressionQuality: 100)
        return imageData?.base64EncodedString() ?? ""
    }
}
