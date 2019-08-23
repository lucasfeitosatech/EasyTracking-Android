package lucasfeitosa.tech.easytracking.activity;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.PointF;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.TextView;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Array;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import lucasfeitosa.tech.easytracking.R;
import lucasfeitosa.tech.easytracking.util.RestClient;
import lucasfeitosa.tech.easytracking.view.PointsOverlayView;
import lucasfeitosa.tech.easytracking.view.QRCodeReaderView;
import okhttp3.ResponseBody;
import okio.BufferedSink;
import okio.Okio;
import retrofit2.Response;
import rx.Observable;
import rx.Observer;
import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Func1;
import rx.schedulers.Schedulers;

public class QRCodeReaderActivity extends AppCompatActivity implements ActivityCompat.OnRequestPermissionsResultCallback, QRCodeReaderView.OnQRCodeReadListener {

    private static final int MY_PERMISSION_REQUEST_CAMERA = 0;
    private ViewGroup mainLayout;

    private TextView resultTextView;
    private QRCodeReaderView qrCodeReaderView;
    private CheckBox flashlightCheckBox;
    private CheckBox enableDecodingCheckBox;
    private PointsOverlayView pointsOverlayView;
    private static final String TAG = "teste";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_qr_reader);

        mainLayout = (ViewGroup) findViewById(R.id.main_layout);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED) {
            initQRCodeReaderView();
        } else {
            requestCameraPermission();
        }
    }
    @Override protected void onResume() {
        super.onResume();

        if (qrCodeReaderView != null) {
            qrCodeReaderView.startCamera();
        }
    }

    @Override protected void onPause() {
        super.onPause();

        if (qrCodeReaderView != null) {
            qrCodeReaderView.stopCamera();
        }
    }

    @Override
    public void onQRCodeRead(String text, byte[] rawData, PointF[] points) {

        Log.d(TAG, "onQRCodeRead: " + rawData.length);
        qrCodeReaderView.stopCamera();
        short x = getXfromQRResult(rawData);
        String id = getIdFromQRResult(rawData).toLowerCase();
        downloadRedundancy(id);

    }

    public short getXfromQRResult(byte[] rawData){
        byte[] xByteArray = new byte[2];
        xByteArray[0] = rawData[0];
        xByteArray[1] = rawData[1];
        ByteBuffer wrapped = ByteBuffer.wrap(xByteArray); // big-endian by default
        short num = wrapped.getShort();
        Log.d(TAG, "onQRCodeRead: " + num);
        return num;
    }

    public String getIdFromQRResult(byte[] rawData){


        StringBuilder hex = new StringBuilder();
        for(int i=2;i <34;i++){
            byte b = rawData[i];
            String s1 = String.format("%8s", Integer.toBinaryString(b & 0xFF)).replace(' ', '0');
            //Log.d(TAG, "getIdFromQRResult: " + binaryToHex(s1));
            hex.append(binaryToHex(s1));
        }

        return hex.toString();
    }
    public static String binaryToHex(String binary) {
        return String.format("%02X", Long.parseLong(binary,2)) ;
    }

    public String addPadding(String mByte,int size){
        int dif = size - mByte.length();
        String rByte = "";
        if( dif != 0) {
            for(int i=0;i<dif;i++){
                rByte = rByte.concat("0");
            }
        }
        rByte = rByte.concat(mByte);
        return rByte;
    }

    public void downloadRedundancy(String id){
        RestClient.get()
                .downloadRedundancy("http://lucasfeitosa.online/red/" + id + ".red")
                .flatMap(responseBody -> {
                    try {
                        Log.d(TAG, "downloadRedundancy: " + responseBody.string().length());
                        return Observable.just(responseBody.string());
                    } catch (IOException e) {
                        return Observable.error(e);
                    }
                })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe();
    }

    @Override public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                                     @NonNull int[] grantResults) {
        if (requestCode != MY_PERMISSION_REQUEST_CAMERA) {
            return;
        }

        if (grantResults.length == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            Snackbar.make(mainLayout, "Camera permission was granted.", Snackbar.LENGTH_SHORT).show();

            initQRCodeReaderView();
        } else {
            Snackbar.make(mainLayout, "Camera permission request was denied.", Snackbar.LENGTH_SHORT)
                    .show();
        }
    }

    private void requestCameraPermission() {
        if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.CAMERA)) {
            Snackbar.make(mainLayout, "Camera access is required to display the camera preview.",
                    Snackbar.LENGTH_INDEFINITE).setAction("OK", new View.OnClickListener() {
                @Override public void onClick(View view) {
                    ActivityCompat.requestPermissions(QRCodeReaderActivity.this, new String[] {
                            Manifest.permission.CAMERA
                    }, MY_PERMISSION_REQUEST_CAMERA);
                }
            }).show();
        } else {
            Snackbar.make(mainLayout, "Permission is not available. Requesting camera permission.",
                    Snackbar.LENGTH_SHORT).show();
            ActivityCompat.requestPermissions(this, new String[] {
                    Manifest.permission.CAMERA
            }, MY_PERMISSION_REQUEST_CAMERA);
        }
    }

    private void initQRCodeReaderView() {
        View content = getLayoutInflater().inflate(R.layout.content_decoder, mainLayout, true);

        qrCodeReaderView = (QRCodeReaderView) content.findViewById(R.id.qrdecoderview);
        resultTextView = (TextView) content.findViewById(R.id.result_text_view);
        flashlightCheckBox = (CheckBox) content.findViewById(R.id.flashlight_checkbox);
        enableDecodingCheckBox = (CheckBox) content.findViewById(R.id.enable_decoding_checkbox);
        pointsOverlayView = (PointsOverlayView) content.findViewById(R.id.points_overlay_view);

        qrCodeReaderView.setAutofocusInterval(2000L);
        qrCodeReaderView.setOnQRCodeReadListener(this);
        qrCodeReaderView.setBackCamera();
        flashlightCheckBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean isChecked) {
                qrCodeReaderView.setTorchEnabled(isChecked);
            }
        });
        enableDecodingCheckBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean isChecked) {
                qrCodeReaderView.setQRDecodingEnabled(isChecked);
            }
        });
        qrCodeReaderView.startCamera();
    }
}
