package com.google.zxing.binaryqrcode.binary;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.ChecksumException;
import com.google.zxing.FormatException;
import com.google.zxing.NotFoundException;
import com.google.zxing.Result;
import com.google.zxing.ResultPoint;
import com.google.zxing.common.DecoderResult;
import com.google.zxing.common.DetectorResult;
import com.google.zxing.qrcode.decoder.Decoder;
import com.google.zxing.qrcode.detector.Detector;

/**
 * This implementation can detect and decode QR Codes in an image into bytes.
 *
 * @author Thomas Skjolberg
 */
public class BinaryQRCodeReader{

  private final Decoder decoder = new Decoder();

  protected Decoder getDecoder() {
    return decoder;
  }

  public Result decode(BinaryBitmap image) throws NotFoundException, ChecksumException, FormatException
  {
      DetectorResult detectorResult = new Detector(image.getBlackMatrix()).detect();
      ResultPoint[] points;

      DecoderResult decode = decoder.decode(detectorResult.getBits());
      points = detectorResult.getPoints();
      Result result = new Result(decode.getText(), decode.getRawBytes(), points, BarcodeFormat.QR_CODE);

      return result;
  }

  public void reset() {
    // do nothing
  }


}
