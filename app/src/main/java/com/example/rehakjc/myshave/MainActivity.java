package com.example.rehakjc.myshave;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Parcelable;
import android.provider.MediaStore;
import android.view.KeyEvent;
import android.view.View;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.Toast;
import android.util.Log;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * <h1> My Shave of the day</h1>
 * This is the Android web client for the MSOTD web app.
 * This application serves as a native wrapper for the web app.
 * This code is heavily influenced by floryn90's Android-File_Chooser app
 * found at 'https://github.com/OpenGeeksMe/Android-File-Chooser'
 * @author James Rehak
 */


public class MainActivity extends Activity {

    private WebView myWebView;
    private WebSettings webSettings;

    // For Image upload
    private static final int INPUT_FILE_REQUEST_CODE = 1;
    private static final int FILECHOOSER_RESULTCODE = 1;
    private ValueCallback<Uri> mUploadMessage;
    private Uri mCapturedImageURI = null;
    private ValueCallback<Uri[]> mFilePathCallback;
    private String mCameraPhotoPath;

    // For error handling
    private static final String TAG = MainActivity.class.getSimpleName();


    /**
     * Initializes main activity, webview.
     * @param savedInstanceState last used Instance State of web app.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        myWebView = (WebView) findViewById(R.id.webView);
        setWebSettings();
        myWebView.setWebViewClient(new WebViewClient());
        myWebView.setWebChromeClient(new MyWebChromeCLient());
        activateHardwareAcceleration();

        myWebView.loadUrl("http://mysotd.herokuapp.com/");
    }



    /**
     * Called when activity exits.
     * @param requestCode integer request code for identifying what this result came from
     * @param resultCode integer result code returned from child activity though its set result
     * @param data Intent data, which can return result data to the caller.
     */
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {

            if (requestCode != INPUT_FILE_REQUEST_CODE || mFilePathCallback == null) {
                super.onActivityResult(requestCode, resultCode, data);
                return;
            }

            Uri[] results = null;

            // Check that the response is a good one
            if (resultCode == Activity.RESULT_OK) {
                if (data == null) {
                    // If there is not data, then we may have taken a photo
                    if (mCameraPhotoPath != null) {
                        results = new Uri[]{Uri.parse(mCameraPhotoPath)};
                    }
                } else {
                    String dataString = data.getDataString();
                    if (dataString != null) {
                        results = new Uri[]{Uri.parse(dataString)};
                    }
                }
            }

            mFilePathCallback.onReceiveValue(results);
            mFilePathCallback = null;

        } else if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.KITKAT) {
            if (requestCode != FILECHOOSER_RESULTCODE || mUploadMessage == null) {
                super.onActivityResult(requestCode, resultCode, data);
                return;
            }

            if (requestCode == FILECHOOSER_RESULTCODE) {

                if (null == this.mUploadMessage) {
                    return;

                }

                Uri result = null;

                try {
                    if (resultCode != RESULT_OK) {

                        result = null;

                    } else {

                        // retrieve from the private variable if the intent is null
                        result = data == null ? mCapturedImageURI : data.getData();
                    }
                } catch (Exception e) {
                    Toast.makeText(getApplicationContext(), "activity :" + e,
                            Toast.LENGTH_LONG).show();
                }

                mUploadMessage.onReceiveValue(result);
                mUploadMessage = null;

            }
        }

        return;
    }



    /**
     * Set Web Settings for web view client.
     */
    private void setWebSettings() {
        webSettings = myWebView.getSettings();
        webSettings.setAppCacheEnabled(true);
        webSettings.setCacheMode(webSettings.LOAD_DEFAULT);
        webSettings.setJavaScriptEnabled(true);
        webSettings.setLoadWithOverviewMode(true);
        webSettings.setAllowFileAccess(true);
    }



    /**
     * Web View Client Class
     */
    public class WebViewClient extends android.webkit.WebViewClient {
        //ProgressDialog progressDialog;

        /**
         * should Override Url Loading to prevent opening new pages outside of web app
         * @param view web view.
         * @param url of webapp.
         * @return always true.
         */
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            view.loadUrl(url);
            return true;

        }
    }


    /**
     * Activate Hardware Acceleration, if SDK version supports it.
     */
    private void activateHardwareAcceleration() {
        //if SDK version is greater of 19 then activate hardware acceleration otherwise activate software acceleration
        if (Build.VERSION.SDK_INT >= 19) {
            myWebView.setLayerType(View.LAYER_TYPE_HARDWARE, null);
        }
        else if(Build.VERSION.SDK_INT >=11 && Build.VERSION.SDK_INT < 19) {
            myWebView.setLayerType(View.LAYER_TYPE_SOFTWARE, null);
        }
    }



    /**
     * Create Image File
     * @return imageFile
     * @throws IOException
     */
    private File createImageFile() throws IOException {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES);
        File imageFile = File.createTempFile(
                imageFileName,  /* prefix */
                ".jpg",         /* suffix */
                storageDir      /* directory */
        );
        return imageFile;
    }



    /**
     * On Key Down event has occurred. Used to enable system UI back button.
     * @param keyCode
     * @param event
     * @return
     */
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        // Check if the key event was the Back button and if there's history
        if ((keyCode == KeyEvent.KEYCODE_BACK) && myWebView.canGoBack()) {
            myWebView.goBack();
            return true;
        }
        // If there's no web page history, exit the activity
        return super.onKeyDown(keyCode, event);
    }



    /**
     * Web Chrome Client Class
     * defined for image capture functionality.
     */
    public class MyWebChromeCLient extends WebChromeClient {

        /**
         * Tells web view client to show a file chooser, including image capture for Lollipop
         * @param view web view
         * @param filePath to image
         * @param fileChooserParams file chooser parameters
         * @return true cancels request
         */
        public boolean onShowFileChooser(WebView view, ValueCallback<Uri[]> filePath, WebChromeClient.FileChooserParams fileChooserParams) {
            // Double check that we don't have any existing callbacks
            if (mFilePathCallback != null) {
                mFilePathCallback.onReceiveValue(null);
            }
            mFilePathCallback = filePath;

            Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
                // Create the File where the photo should go
                File photoFile = null;
                try {
                    photoFile = createImageFile();
                    takePictureIntent.putExtra("PhotoPath", mCameraPhotoPath);
                } catch (IOException ex) {
                    // Error occurred while creating the File
                    Log.e(TAG, "Unable to create Image File", ex);
                }

                // Continue only if the File was successfully created
                if (photoFile != null) {
                    mCameraPhotoPath = "file:" + photoFile.getAbsolutePath();
                    takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT,
                            Uri.fromFile(photoFile));
                } else {
                    takePictureIntent = null;
                }
            }

            Intent contentSelectionIntent = new Intent(Intent.ACTION_GET_CONTENT);
            contentSelectionIntent.addCategory(Intent.CATEGORY_OPENABLE);
            contentSelectionIntent.setType("image/*");

            Intent[] intentArray;
            if (takePictureIntent != null) {
                intentArray = new Intent[]{takePictureIntent};
            } else {
                intentArray = new Intent[0];
            }

            Intent chooserIntent = new Intent(Intent.ACTION_CHOOSER);
            chooserIntent.putExtra(Intent.EXTRA_INTENT, contentSelectionIntent);
            chooserIntent.putExtra(Intent.EXTRA_TITLE, "Image Chooser");
            chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, intentArray);

            startActivityForResult(chooserIntent, INPUT_FILE_REQUEST_CODE);

            return true;

        }



        /**
         * Open File Chooser dialog for Honeycomb and greater.
         * @param uploadMsg
         * @param acceptType
         */
        public void openFileChooser(ValueCallback<Uri> uploadMsg, String acceptType) {

            mUploadMessage = uploadMsg;
            // Create AndroidExampleFolder at sdcard
            // Create AndroidExampleFolder at sdcard

            File imageStorageDir = new File(
                    Environment.getExternalStoragePublicDirectory(
                            Environment.DIRECTORY_PICTURES)
                    , "AndroidExampleFolder");

            if (!imageStorageDir.exists()) {
                // Create AndroidExampleFolder at sdcard
                imageStorageDir.mkdirs();
            }

            // Create camera captured image file path and name
            File file = new File(
                    imageStorageDir + File.separator + "IMG_"
                            + String.valueOf(System.currentTimeMillis())
                            + ".jpg");
            Log.d("File", "File: " + file);
            mCapturedImageURI = Uri.fromFile(file);

            // Camera capture image intent
            final Intent captureIntent = new Intent(
                    android.provider.MediaStore.ACTION_IMAGE_CAPTURE);

            captureIntent.putExtra(MediaStore.EXTRA_OUTPUT, mCapturedImageURI);

            Intent i = new Intent(Intent.ACTION_GET_CONTENT);
            i.addCategory(Intent.CATEGORY_OPENABLE);
            i.setType("image/*");

            // Create file chooser intent
            Intent chooserIntent = Intent.createChooser(i, "Image Chooser");

            // Set camera intent to file chooser
            chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS
                    , new Parcelable[] { captureIntent });

            // On select image call onActivityResult method of activity
            startActivityForResult(chooserIntent, FILECHOOSER_RESULTCODE);

        }

    } // End Web Chrome Client

}// End Main Activity
