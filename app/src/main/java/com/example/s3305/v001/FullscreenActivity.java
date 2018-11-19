package com.example.s3305.v001;

import android.annotation.SuppressLint;
import android.graphics.Bitmap;
import android.graphics.RectF;
import android.support.v4.view.MotionEventCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.os.Handler;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.graphics.BitmapFactory;
import com.flir.flironesdk.*;
import java.nio.ByteOrder;
import org.w3c.dom.Text;
import android.util.Log;
import java.nio.ByteBuffer;
import java.util.EnumSet;

/**
 * An example full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 */
public class FullscreenActivity extends AppCompatActivity implements Device.Delegate, FrameProcessor.Delegate{
    /**
     * Whether or not the system UI should be auto-hidden after
     * {@link #AUTO_HIDE_DELAY_MILLIS} milliseconds.
     */
    private static final boolean AUTO_HIDE = true;

    /**
     * If {@link #AUTO_HIDE} is set, the number of milliseconds to wait after
     * user interaction before hiding the system UI.
     */
    private static final int AUTO_HIDE_DELAY_MILLIS = 3000;

    /**
     * Some older devices needs a small delay between UI widget updates
     * and a change of the status and navigation bar.
     */
    private static final int UI_ANIMATION_DELAY = 300;
    private final Handler mHideHandler = new Handler();
    private View mContentView;
    private final Runnable mHidePart2Runnable = new Runnable() {
        @SuppressLint("InlinedApi")
        @Override
        public void run() {
            // Delayed removal of status and navigation bar

            // Note that some of these constants are new as of API 16 (Jelly Bean)
            // and API 19 (KitKat). It is safe to use them, as they are inlined
            // at compile-time and do nothing on earlier devices.
            mContentView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LOW_PROFILE
                    | View.SYSTEM_UI_FLAG_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                    | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
        }
    };
    private View mControlsView;
    private final Runnable mShowPart2Runnable = new Runnable() {
        @Override
        public void run() {
            // Delayed display of UI elements
            ActionBar actionBar = getSupportActionBar();
            if (actionBar != null) {
                actionBar.show();
            }
            mControlsView.setVisibility(View.VISIBLE);
        }
    };
    private boolean mVisible;
    private ImageView t = null;
    private final Runnable mHideRunnable = new Runnable() {
        @Override
        public void run() {
            hide();
        }
    };
    /**
     * Touch listener to use for in-layout UI controls to delay hiding the
     * system UI. This is to prevent the jarring behavior of controls going away
     * while interacting with activity UI.
     */
    private final View.OnTouchListener mDelayHideTouchListener = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View view, MotionEvent motionEvent) {
            if (AUTO_HIDE) {
                delayedHide(AUTO_HIDE_DELAY_MILLIS);
            }
            return false;
        }
    };

    private FrameProcessor frameProcessor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        frameProcessor = new FrameProcessor(this,this,EnumSet.of(RenderedImage.ImageType.BlendedMSXRGBA8888Image));
        setContentView(R.layout.activity_fullscreen);

        mVisible = true;
        mControlsView = findViewById(R.id.fullscreen_content_controls);
        mContentView = findViewById(R.id.fullscreen_content);


        // Set up the user interaction to manually show or hide the system UI.
        mContentView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                toggle();
            }
        });

        // Upon interacting with UI controls, delay any scheduled hide()
        // operations to prevent the jarring behavior of controls going away
        // while interacting with the UI.
        findViewById(R.id.dummy_button).setOnTouchListener(mDelayHideTouchListener);
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);

        // Trigger the initial hide() shortly after the activity has been
        // created, to briefly hint to the user that UI controls
        // are available.
        delayedHide(100);
    }

    private void toggle() {
        if (mVisible) {
            hide();
        } else {
            show();
        }
    }

    private void hide() {
        // Hide UI first
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.hide();
        }
        mControlsView.setVisibility(View.GONE);
        mVisible = false;

        // Schedule a runnable to remove the status and navigation bar after a delay
        mHideHandler.removeCallbacks(mShowPart2Runnable);
        mHideHandler.postDelayed(mHidePart2Runnable, UI_ANIMATION_DELAY);
    }

    @SuppressLint("InlinedApi")
    private void show() {
        // Show the system bar
        mContentView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION);
        mVisible = true;

        // Schedule a runnable to display UI elements after a delay
        mHideHandler.removeCallbacks(mHidePart2Runnable);
        mHideHandler.postDelayed(mShowPart2Runnable, UI_ANIMATION_DELAY);
    }

    /**
     * Schedules a call to hide() in delay milliseconds, canceling any
     * previously scheduled calls.
     */
    private void delayedHide(int delayMillis) {
        mHideHandler.removeCallbacks(mHideRunnable);
        mHideHandler.postDelayed(mHideRunnable, delayMillis);
    }

    Device flirDevice;

    @Override
    protected void onResume(){
        super.onResume();
        Device.startDiscovery(this,this);
    }

    @Override
    protected void onPause(){
        super.onPause();
        Device.stopDiscovery();
    }
    @Override
    public void onTuningStateChanged(Device.TuningState tuningState) {

    }

    @Override
    public void onAutomaticTuningChanged(boolean b) {

    }
    private short[] thermalPixels;
    private int width;
    private int height;
    private int maxX, maxY;
    private double maxTemp, meantTemp;

    @Override
    public void onDeviceConnected(Device device) {
        flirDevice = device;
        device.startFrameStream(new Device.StreamDelegate() {
            @Override
            public void onFrameReceived(Frame frame) {
                frameProcessor.processFrame(frame);
            }
        });
    }
    private void updateThermalImageView(final Bitmap frame){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                ImageView thermalImageView = null;
                thermalImageView.setImageBitmap(frame);
            }
        });
    }
    @Override
    public void onDeviceDisconnected(Device device) {

    }
    private Bitmap thermalBitmap = null;
    @Override
    public void onFrameProcessed(final RenderedImage renderedImage) {
        final Bitmap imageBitmap = Bitmap.createBitmap(renderedImage.width(), renderedImage.height(), Bitmap.Config.ARGB_8888);
        imageBitmap.copyPixelsFromBuffer(ByteBuffer.wrap(renderedImage.pixelData()));
        final ImageView imageView = (ImageView) findViewById(R.id.imageView2);
        final int[] location = new int[2];
        imageView.getLocationOnScreen(location);
        thermalPixels = renderedImage.thermalPixelData();
        width = renderedImage.width() / 3;
        height = renderedImage.height() / 3;
        int centerPixelIndex = width * (height / 2) + (width / 2);
        int[][] centerPixelIndexes = new int[][]{
                {location[0], location[1]}, {location[0] + width, location[1]},
                {location[0] + (width * 2), location[1], location[0] + (width * 3), location[1]},
                {location[0], location[1] + height}, {location[0] + width, location[1] + height},
                {location[0] + (width * 2), location[1] + height, location[0] + (width * 3), location[1] + height},
                {location[0], location[1] + (height * 2)}, {location[0] + width, location[1] + (height * 2)},
                {location[0] + (width * 2), location[1] + (height * 2), location[0] + (width * 3), location[1] + (height * 2)},
                {location[0], location[1] + (height * 3)}, {location[0] + width, location[1] + (height * 3)},
                {location[0] + (width * 2), location[1] + (height * 3), location[0] + (width * 3), location[1] + (height * 3)},
        };
        long startTime = System.nanoTime();
        if (renderedImage.imageType() == RenderedImage.ImageType.VisualJPEGImage) {
            final Bitmap visBitmap = BitmapFactory.decodeByteArray(renderedImage.pixelData(), 0, renderedImage.pixelData().length);

            // we must rotate the raw visual JPEG to match phone/tablet screen
            android.graphics.Matrix mtx = new android.graphics.Matrix();
            mtx.postRotate(90);
            final Bitmap rotatedVisBitmap = Bitmap.createBitmap(visBitmap, 0, 0, visBitmap.getWidth(), visBitmap.getHeight(), mtx, true);
            updateThermalImageView(rotatedVisBitmap);
        } else {
            if (thermalBitmap == null || (renderedImage.width() != thermalBitmap.getWidth() || renderedImage.height() != thermalBitmap.getHeight())) {
                Log.d("THERMALBMP", "Creating thermalBitmap with dimensions: " + renderedImage.width() + "x" + renderedImage.height());
                thermalBitmap = Bitmap.createBitmap(renderedImage.width(), renderedImage.height(), Bitmap.Config.ARGB_8888);

            }
            if (renderedImage.imageType() == RenderedImage.ImageType.ThermalRadiometricKelvinImage) {
                /**
                 * Here is a simple example of showing color for 9 bands of tempuratures:
                 * Below 0 celceus is black
                 * 0-10C is dark blue
                 * 10-20C is light blue
                 * 20-36C is green
                 * 36-40C is dark red (human body)
                 * 40-50C is bright red
                 * 50-60C is orange
                 * 60-100C is yellow
                 * Above 100C is white
                 */
                short[] shortPixels = new short[renderedImage.pixelData().length / 2];
                byte[] argbPixels = new byte[renderedImage.width() * renderedImage.height() * 4];
                // Thermal data is little endian.
                ByteBuffer.wrap(renderedImage.pixelData()).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(shortPixels);
                final byte aPixValue = (byte) 255;
                for (int p = 0; p < shortPixels.length; p++) {
                    int destP = p * 4;
                    int tempInC = (shortPixels[p] - 27315) / 100;
                    byte rPixValue;
                    byte gPixValue;
                    byte bPixValue;
                    if (tempInC < 0) {
                        rPixValue = gPixValue = bPixValue = 0;
                    } else if (tempInC < 10) {
                        rPixValue = gPixValue = 0;
                        bPixValue = 127;
                    } else if (tempInC < 20) {
                        rPixValue = gPixValue = 0;
                        bPixValue = (byte) 255;
                    } else if (tempInC < 36) {
                        rPixValue = bPixValue = 0;
                        gPixValue = (byte) 160;
                    } else if (tempInC < 40) {
                        bPixValue = gPixValue = 0;
                        rPixValue = 127;
                    } else if (tempInC < 50) {
                        bPixValue = gPixValue = 0;
                        rPixValue = (byte) 255;
                    } else if (tempInC < 60) {
                        rPixValue = (byte) 255;
                        gPixValue = (byte) 166;
                        bPixValue = 0;
                    } else if (tempInC < 100) {
                        rPixValue = gPixValue = (byte) 255;
                        bPixValue = 0;
                    } else {
                        bPixValue = rPixValue = gPixValue = (byte) 255;
                    }
                    // alpha always high
                    argbPixels[destP + 3] = aPixValue;
                    // red pixel
                    argbPixels[destP] = rPixValue;
                    argbPixels[destP + 1] = gPixValue;
                    argbPixels[destP + 2] = bPixValue;
                }
                thermalBitmap.copyPixelsFromBuffer(ByteBuffer.wrap(argbPixels));

            } else if (renderedImage.imageType() == RenderedImage.ImageType.ThermalLinearFlux14BitImage) {
                /**
                 * Here is an example of how to apply custom pseudocolor to a 14 bit greyscale image
                 * This example crates a 768-color Black->Green->Aqua->White by linearly mapping
                 * RGB values. Try experimenting with different color mapping approaches.
                 *
                 * This example normalizes the scene linearly. If you want to map colors to temperatures,
                 * use the Radiometic Kelvin image type and do not apply a scale as done below.
                 */

                short[] shortPixels = new short[renderedImage.pixelData().length / 2];
                byte[] argbPixels = new byte[renderedImage.width() * renderedImage.height() * 4];
                // Thermal data is little endian.
                ByteBuffer.wrap(renderedImage.pixelData()).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(shortPixels);
                int minValue = 65535;
                int maxValue = 0;
                for (int p = 0; p < shortPixels.length; p++) {
                    minValue = Math.min(minValue, shortPixels[p]);
                    maxValue = Math.max(maxValue, shortPixels[p]);
                }
                int range = (maxValue - minValue);
                float scale = ((float) 767 / (float) range);
                for (int p = 0; p < shortPixels.length; p++) {
                    int destP = p * 4;

                    short pixelValue = (short) ((shortPixels[p] - minValue) * scale);
                    byte redValue = 0;
                    byte greenValue;
                    byte blueValue = 0;
                    if (pixelValue < 256) {
                        greenValue = (byte) pixelValue;
                    } else if (pixelValue < 512) {
                        greenValue = (byte) 255;
                        blueValue = (byte) (pixelValue - 256);
                    } else {
                        greenValue = (byte) 255;
                        blueValue = (byte) 255;
                        redValue = (byte) (pixelValue - 512);
                    }


                    // alpha always high
                    argbPixels[destP + 3] = (byte) 255;
                    // red pixel
                    argbPixels[destP] = redValue;
                    argbPixels[destP + 1] = greenValue;
                    argbPixels[destP + 2] = blueValue;
                }

                thermalBitmap.copyPixelsFromBuffer(ByteBuffer.wrap(argbPixels));
            }
       /* new Thread(new Runnable() {

            short[] thermalPixels = renderedImage.thermalPixelData();
            int width = renderedImage.width() ;
            int height = renderedImage.height();

            @Override
            public void run() {
                imageView.setImageBitmap(imageBitmap);
                double pixelCMax = 0;
                double pixelCAll = 0;
                int maxIndex = 0;
                int pixelTemp ;
                double [] temp = new double[width * height];
                for (int i = 0;i < width * height; i++){
                    pixelTemp =thermalPixels[i] & 0xffff;
                    temp[i] = (pixelTemp / 100) - 273.15;
                    pixelCMax = pixelCMax < temp[i] ? temp[i] : pixelCMax;
                    if(pixelCMax == temp[i]){
                        maxIndex = i;
                    }
                    pixelCAll += temp[i];
                    meantTemp = pixelCAll / (width * height); //全屏平均溫度
                }
                maxTemp = pixelCMax; //全屏最高溫度
                maxX = maxIndex % width; //最高溫度x座標
                maxY = maxIndex / width; //最高溫度y座標
                TextView textView = (TextView)findViewById(R.id.textView);
                textView.setText("全屏最高溫: "+maxTemp);
            }
        });*/

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    imageView.setImageBitmap(imageBitmap);
                    imageView.getLocationOnScreen(location);
                    double pixelCMax = 0;
                    double pixelCAll = 0;
                    int maxIndex = 0;
                    int pixelTemp = 0;
                    double[] temp = new double[width * height];
                    for (int i = 0; i < width * height; i++) {
                        pixelTemp = thermalPixels[i] & 0xffff;
                        temp[i] = (pixelTemp / 100) - 273.15;
                        pixelCMax = pixelCMax < temp[i] ? temp[i] : pixelCMax;
                        if (pixelCMax == temp[i]) {
                            maxIndex = i;
                        }
                        pixelCAll += temp[i];
                        meantTemp = pixelCAll / (width * height); //全屏平均溫度
                    }
                    maxTemp = pixelCMax; //全屏最高溫度
                    maxX = maxIndex % width; //最高溫度x座標
                    maxY = maxIndex / width; //最高溫度y座標
                    TextView textView = (TextView) findViewById(R.id.textView);
                    textView.setText("全屏最高溫: " + maxTemp + "\n平均溫度: " + meantTemp + "\nX: "
                            + maxX + "\nY: " + maxY);
                }
            });
        }
    }
  /*  public static RectF calcViewScreenLocation (View view){
        int [] location = new int [2];

        view.getLocationOnScreen(location);
        return new RectF(location[0],location[1],location[0]+view.getWidth(),
                location[1]+view.getHeight());
    }

    public static boolean isInViewRange(View view , MotionEvent event){
        float x = event.getRawX();
        float y = event.getRawY();

        RectF rect = calcViewScreenLocation(view);
        return rect.contains(x,y);
    }*/
}
