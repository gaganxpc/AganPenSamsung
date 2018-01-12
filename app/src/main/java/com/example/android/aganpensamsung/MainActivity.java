package com.example.android.aganpensamsung;

import android.Manifest;
import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.CursorLoader;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.RectF;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.RadioGroup;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.samsung.android.sdk.SsdkUnsupportedException;
import com.samsung.android.sdk.pen.Spen;
import com.samsung.android.sdk.pen.SpenSettingEraserInfo;
import com.samsung.android.sdk.pen.SpenSettingPenInfo;
import com.samsung.android.sdk.pen.SpenSettingTextInfo;
import com.samsung.android.sdk.pen.document.SpenInvalidPasswordException;
import com.samsung.android.sdk.pen.document.SpenNoteDoc;
import com.samsung.android.sdk.pen.document.SpenObjectImage;
import com.samsung.android.sdk.pen.document.SpenObjectStroke;
import com.samsung.android.sdk.pen.document.SpenObjectTextBox;
import com.samsung.android.sdk.pen.document.SpenPageDoc;
import com.samsung.android.sdk.pen.document.SpenUnsupportedTypeException;
import com.samsung.android.sdk.pen.document.SpenUnsupportedVersionException;
import com.samsung.android.sdk.pen.document.textspan.SpenFontSizeSpan;
import com.samsung.android.sdk.pen.document.textspan.SpenLineSpacingParagraph;
import com.samsung.android.sdk.pen.document.textspan.SpenTextParagraphBase;
import com.samsung.android.sdk.pen.document.textspan.SpenTextSpanBase;
import com.samsung.android.sdk.pen.engine.SpenColorPickerListener;
import com.samsung.android.sdk.pen.engine.SpenControlBase;
import com.samsung.android.sdk.pen.engine.SpenSurfaceView;
import com.samsung.android.sdk.pen.engine.SpenTextChangeListener;
import com.samsung.android.sdk.pen.engine.SpenTouchListener;
import com.samsung.android.sdk.pen.settingui.SpenSettingEraserLayout;
import com.samsung.android.sdk.pen.settingui.SpenSettingPenLayout;
import com.samsung.android.sdk.pen.settingui.SpenSettingTextLayout;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import static android.graphics.BitmapFactory.decodeFile;

public class MainActivity extends AppCompatActivity {

    private SpenNoteDoc mSpenNoteDoc;
    private SpenPageDoc mSpenPageDoc;
    private SpenSurfaceView mSpenSurfaceView;
    private SpenSettingPenLayout mPenSettingView;
    private SpenSettingEraserLayout mEraserSettingView;
    private SpenSettingTextLayout mTextSettingView;
    private Button mPenBtn;
    private Button mEraserBtn;
    private Button mUndoBtn;
    private Button mRedoBtn;
    private Button mBgBtn;
    private Button mImgObjBtn;
    private Button mTextObjBtn;
    private Button mStrokeObjBtn;
    private Button mSaveFileBtn;
    private Button mLoadFileBtn;
    private Button mShapeObjRecogBtn;
    private Button mShapeLineObjBtn;
    private File mFilePath;
    private boolean mIsDiscard = false;
    Rect mScreenRect;
    private int mToolType = SpenSurfaceView.TOOL_SPEN;
    public static final int SDK_VERSION = Build.VERSION.SDK_INT;
    private static final int ADD_BACKGROUND_REQUEST = 1;
    private final int REQUEST_CODE_SELECT_IMAGE_BACKGROUND = 100;
    private final int MODE_PEN = 0;
    private final int MODE_IMG_OBJ = 2;
    private final int MODE_TEXT_OBJ = 3;
    private final int MODE_STROKE_OBJ = 4;
    private int mMode = MODE_PEN;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Shape Object Recognition
        mShapeObjRecogBtn = (Button) findViewById(R.id.btn_shaperecog);
        mShapeObjRecogBtn.setOnClickListener(mShapeObjRecogBtnClickListener);

        // Init filepath
        String filePath = Environment.getExternalStorageDirectory().getAbsolutePath() + "/SPen/";
        mFilePath = new File(filePath);

        // Save Load
        mSaveFileBtn = (Button) findViewById(R.id.btn_save);
        mSaveFileBtn.setOnClickListener(mSaveFileBtnClickListener);
        mLoadFileBtn = (Button) findViewById(R.id.btn_load);
        mLoadFileBtn.setOnClickListener(mLoadFileBtnClickListener);

        // Undo and Redo
        // Undo and Redo
        mUndoBtn = (Button) findViewById(R.id.btn_undo);
        mUndoBtn.setOnClickListener(undoNredoBtnClickListener);
        mUndoBtn.setEnabled(mSpenPageDoc.isUndoable());
        mRedoBtn = (Button) findViewById(R.id.btn_redo);
        mRedoBtn.setOnClickListener(undoNredoBtnClickListener);
        mRedoBtn.setEnabled(mSpenPageDoc.isRedoable());
        // Background
        mBgBtn = (Button) findViewById(R.id.btn_background);
        mBgBtn.setOnClickListener(mBgBtnClickListener);
        // Image Object
        mImgObjBtn = (Button) findViewById(R.id.btn_image);
        mImgObjBtn.setOnClickListener(mImgObjBtnClickListener);
        // Initialize Spen
        boolean isSpenFeatureEnabled = false;
        Spen spenPackage = new Spen();
        try {
            spenPackage.initialize(this);
            isSpenFeatureEnabled =
                    spenPackage.isFeatureEnabled(Spen.DEVICE_PEN);
        } catch (SsdkUnsupportedException e) {
            Toast.makeText(this, "This device does not support Spen.",
                    Toast.LENGTH_SHORT).show();
            e.printStackTrace();
            finish();
        } catch (Exception e1) {
            Toast.makeText(this, "Cannot initialize Pen.",
                    Toast.LENGTH_SHORT).show();
            e1.printStackTrace();
            finish();
        }

        // Create Spen View
        FrameLayout spenViewContainer = (FrameLayout) findViewById(R.id.frame_canvas);
        RelativeLayout spenViewLayout =
                (RelativeLayout) findViewById(R.id.layout_canvas);
// Create PenSettingView
        mPenSettingView = new SpenSettingPenLayout(this, "", spenViewLayout);
// Create EraserSettingView
        mEraserSettingView = new SpenSettingEraserLayout(this, "", spenViewLayout);
// Create TextSettingView
        HashMap<String, String> hashMapFont = new HashMap<String, String>();
        hashMapFont.put("Droid Sans Georgian", "/system/fonts/DroidSansGeorgian.ttf");
        hashMapFont.put("Droid Serif", "/system/fonts/DroidSerif-Regular.ttf");
        hashMapFont.put("Droid Sans", "/system/fonts/DroidSans.ttf");
        hashMapFont.put("Droid Sans Mono", "/system/fonts/DroidSansMono.ttf");
        mTextSettingView = (SpenSettingTextLayout) findViewById(R.id.settingTextLayout);
        mTextSettingView.initialize("", hashMapFont, spenViewLayout);
//Add SettingView to the Container
        spenViewContainer.addView(mPenSettingView);
        spenViewContainer.addView(mEraserSettingView);
// Create SpenSurfaceView
        mSpenSurfaceView = new SpenSurfaceView(this);
        if (null == mSpenSurfaceView) {
            Toast.makeText(this, "Cannot create new SpenSurfaceView.", Toast.LENGTH_SHORT).show();
            finish();
        }
        mSpenSurfaceView.setToolTipEnabled(true);
        spenViewLayout.addView(mSpenSurfaceView);
        mPenSettingView.setCanvasView(mSpenSurfaceView);
        mEraserSettingView.setCanvasView(mSpenSurfaceView);
        mSpenSurfaceView.setColorPickerListener(mColorPickerListener);
        mEraserSettingView.setEraserListener(mEraserListener);
        mSpenSurfaceView.setPreTouchListener(onPreTouchSurfaceViewListener);
        mSpenPageDoc.setHistoryListener(mHistoryListener);
        mSpenSurfaceView.setTouchListener(mPenTouchListener);
        // Text Object
        mTextObjBtn = (Button) findViewById(R.id.btn_text);
        mTextObjBtn.setOnClickListener(mTextObjBtnClickListener);
// Get the dimension of the device screen.
        Display display = getWindowManager().getDefaultDisplay();
        mScreenRect = new Rect();
        display.getRectSize(mScreenRect);
// Create SpenNoteDoc
        try {
            mSpenNoteDoc = new SpenNoteDoc(this, mScreenRect.width(), mScreenRect.height());
        } catch (IOException e) {
            Toast.makeText(this, "Cannot create new NoteDoc", Toast.LENGTH_SHORT).show();
            e.printStackTrace();
            finish();
        } catch (Exception e) {
            e.printStackTrace();
            finish();
        }
// Add a Page to NoteDoc, get an instance, and set it to the member variable.
        mSpenPageDoc = mSpenNoteDoc.appendPage();
        mSpenPageDoc.setBackgroundColor(0xFFD6E6F5);
        mSpenPageDoc.clearHistory();
// Set PageDoc to View
        mSpenSurfaceView.setPageDoc(mSpenPageDoc, true);
// Stroke Object
        mStrokeObjBtn = (Button) findViewById(R.id.btn_stroke);
        mStrokeObjBtn.setOnClickListener(mStrokeObjBtnClickListener);
        // Init setting
        initSettingInfo();
// Register the listener
        mSpenSurfaceView.setColorPickerListener(mColorPickerListener);
        mEraserSettingView.setEraserListener(mEraserListener);
        mSpenSurfaceView.setPreTouchListener(onPreTouchSurfaceViewListener);
        mSpenPageDoc.setHistoryListener(mHistoryListener);
        mSpenSurfaceView.setTouchListener(mPenTouchListener);
        mSpenSurfaceView.setTextChangeListener(mTextChangeListener);
// Set a button
// Pen and Eraser
        mPenBtn = (Button) findViewById(R.id.btn_pen);
        mPenBtn.setOnClickListener(mPenBtnClickListener);
        mEraserBtn = (Button) findViewById(R.id.btn_eraser);
        mEraserBtn.setOnClickListener(mEraserBtnClickListener);
        selectButton(mPenBtn);
// First time run set to pen
        if (!isSpenFeatureEnabled) {
            mToolType = SpenSurfaceView.TOOL_FINGER;
            Toast.makeText(this, "Device does not support Spen. \n You can draw stroke by finger",
                    Toast.LENGTH_SHORT).show();
        } else {
            mToolType = SpenSurfaceView.TOOL_SPEN;
        }
        mSpenSurfaceView.setToolTypeAction(mToolType, SpenSurfaceView.ACTION_STROKE);
    }

    private void initSettingInfo() {
        int mCanvasWidth = mScreenRect.width();
        if (mSpenSurfaceView != null) {
            if (mSpenSurfaceView.getCanvasWidth() < mSpenSurfaceView.getCanvasHeight()) {
                mCanvasWidth = mSpenSurfaceView.getCanvasWidth();
            } else {
                mCanvasWidth = mSpenSurfaceView.getCanvasHeight();
            }
            if (mCanvasWidth == 0) {
                mCanvasWidth = mScreenRect.width();
            }
        }
        // Initialize Pen settings
        SpenSettingPenInfo penInfo = new SpenSettingPenInfo();
        penInfo.color = Color.BLUE;
        penInfo.size = 10;
        mSpenSurfaceView.setPenSettingInfo(penInfo);
        mPenSettingView.setInfo(penInfo);
        // Initialize Eraser settings
        SpenSettingEraserInfo eraserInfo = new SpenSettingEraserInfo();
        eraserInfo.size = 30;
        mSpenSurfaceView.setEraserSettingInfo(eraserInfo);
        mEraserSettingView.setInfo(eraserInfo);
        // Initialize text settings
        SpenSettingTextInfo textInfo = new SpenSettingTextInfo();
        textInfo.size = Math.round(18 * mCanvasWidth / 360);
        mSpenSurfaceView.setTextSettingInfo(textInfo);
        mTextSettingView.setInfo(textInfo);
    }

    /* Listener Pen */
    private final View.OnClickListener mPenBtnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            // When Spen is in stroke (pen) mode
            if (mSpenSurfaceView.getToolTypeAction(mToolType) == SpenSurfaceView.ACTION_STROKE) {
                // If PenSettingView is open, close it.
                if (mPenSettingView.isShown()) {
                    mPenSettingView.setVisibility(View.GONE);
                    // If PenSettingView is not open, open it.
                } else {
                    mPenSettingView.setViewMode(SpenSettingPenLayout.VIEW_MODE_NORMAL);
                    mPenSettingView.setVisibility(View.VISIBLE);
                }
                // If Spen is not in stroke (pen) mode, change it to stroke mode.
            } else {
                int curAction = mSpenSurfaceView.getToolTypeAction(SpenSurfaceView.TOOL_FINGER);
                mSpenSurfaceView.setToolTypeAction(mToolType, SpenSurfaceView.ACTION_STROKE);
                int newAction = mSpenSurfaceView.getToolTypeAction(SpenSurfaceView.TOOL_FINGER);
                if (mToolType == SpenSurfaceView.TOOL_FINGER) {
                    if (curAction != newAction) {
                        mMode = MODE_PEN;
                        selectButton(mPenBtn);
                    }
                } else {
                    mMode = MODE_PEN;
                    selectButton(mPenBtn);
                }
            }
        }
    };
    /* Listener Eraser */
    private final View.OnClickListener mEraserBtnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            // When Spen is in eraser mode
            if (mSpenSurfaceView.getToolTypeAction(mToolType) == SpenSurfaceView.ACTION_ERASER) {
                // If EraserSettingView is open, close it.
                if (mEraserSettingView.isShown()) {
                    mEraserSettingView.setVisibility(View.GONE);
                    // If EraserSettingView is not open, open it.
                } else {
                    mEraserSettingView.setVisibility(View.VISIBLE);
                }
                // If Spen is not in eraser mode, change it to eraser mode.
            } else {
                int MODE_ERASER = 1;
                mMode = MODE_ERASER;
                selectButton(mEraserBtn);
                mSpenSurfaceView.setToolTypeAction(mToolType, SpenSurfaceView.ACTION_ERASER);
            }
        }
    };

    /* Listener pen setting color picker */
    private final SpenColorPickerListener mColorPickerListener = new SpenColorPickerListener() {
        @Override
        public void onChanged(int color, int x, int y) {
            // Set the color from the Color Picker to the setting view.
            if (mPenSettingView != null) {
                SpenSettingPenInfo penInfo = mPenSettingView.getInfo();
                penInfo.color = color;
                mPenSettingView.setInfo(penInfo);
            }else if (mMode == MODE_TEXT_OBJ) {
                SpenSettingTextInfo textInfo = mSpenSurfaceView.getTextSettingInfo();
                textInfo.color = color;
                mTextSettingView.setInfo(textInfo);
            }
        }
    };
    /* Listener eraser setting erase all */
    private final SpenSettingEraserLayout.EventListener mEraserListener = new SpenSettingEraserLayout.EventListener() {
        @Override
        public void onClearAll() {
            // ClearAll button action routines of EraserSettingView
            mSpenPageDoc.removeAllObject();
            mSpenSurfaceView.update();
        }
    };

    /* Listener canvas ontouch */
    private SpenTouchListener onPreTouchSurfaceViewListener = new SpenTouchListener() {
        @Override
        public boolean onTouch(View view, MotionEvent event) {
            // TODO Auto-generated method stub
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                case MotionEvent.ACTION_POINTER_DOWN:
                    enableButton(false);
                    break;
                case MotionEvent.ACTION_CANCEL:
                case MotionEvent.ACTION_UP:
                    enableButton(true);
                    break;
            }
            return false;
        }
    };

    private final View.OnClickListener mShapeObjRecogBtnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            mMode = MODE_PEN;
            selectButton(mShapeObjRecogBtn);
            mSpenSurfaceView.closeControl();
            mSpenSurfaceView.setToolTypeAction(mToolType, SpenSurfaceView.ACTION_RECOGNITION);
        }
    };

    SpenTextChangeListener mTextChangeListener = new SpenTextChangeListener() {
        @Override
        public boolean onSelectionChanged(int arg0, int arg1) {
            return false;
        }
        @Override
        public void onMoreButtonDown(SpenObjectTextBox arg0) {
        }
        @Override
        public void onChanged(SpenSettingTextInfo info, int state) {
            if (mTextSettingView != null) {
                if (state == CONTROL_STATE_SELECTED) {
                    mTextSettingView.setInfo(info);
                }
            }
        }
        @Override
        public void onFocusChanged(boolean gainFocus) {
            if (mTextSettingView != null) {
                if (gainFocus) {
                    // show text setting
                    Log.d("TEXT SHOW", "onFocusChanged: SHOW");
                    mTextSettingView.setVisibility(View.VISIBLE);
                    mMode = MODE_TEXT_OBJ;
                } else {
                    // hide text setting
                    Log.d("TEXT SHOW", "onFocusChanged: GONE");
                    mTextSettingView.setVisibility(View.GONE);
                    mMode = MODE_PEN;
                }
            }
        }
    };

    private final View.OnClickListener mTextObjBtnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            mSpenSurfaceView.closeControl();
            closeSettingView();
            mSpenSurfaceView.setToolTypeAction(mToolType, SpenSurfaceView.ACTION_TEXT);
            mMode = MODE_TEXT_OBJ;
            selectButton(mTextObjBtn);
        }
    };

    private final View.OnClickListener mImgObjBtnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            mSpenSurfaceView.closeControl();
            mMode = MODE_IMG_OBJ;
            selectButton(mImgObjBtn);
            mSpenSurfaceView.setToolTypeAction(mToolType, SpenSurfaceView.ACTION_NONE);
        }
    };

    private final View.OnClickListener mStrokeObjBtnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            mSpenSurfaceView.closeControl();
            mMode = MODE_STROKE_OBJ;
            selectButton(mStrokeObjBtn);
            mSpenSurfaceView.setToolTypeAction(mToolType, SpenSurfaceView.ACTION_NONE);
        }
    };

    //ENABLE DISABLE
    private void enableButton(boolean isEnable) {
        mPenBtn.setEnabled(isEnable);
        mEraserBtn.setEnabled(isEnable);
        mUndoBtn.setEnabled(isEnable && mSpenPageDoc.isUndoable());
        mRedoBtn.setEnabled(isEnable && mSpenPageDoc.isRedoable());
        mBgBtn.setEnabled(isEnable);
        mImgObjBtn.setEnabled(isEnable);
        mTextObjBtn.setEnabled(isEnable);
        mStrokeObjBtn.setEnabled(isEnable);
        mShapeObjRecogBtn.setEnabled(isEnable);
    }
    private void selectButton(View v) {
        // Enable or disable the button according to the current mode.
        mPenBtn.setSelected(false);
        mEraserBtn.setSelected(false);
        mImgObjBtn.setSelected(false);
        mTextObjBtn.setSelected(false);
        mStrokeObjBtn.setSelected(false);
        mShapeObjRecogBtn.setSelected(false);
        v.setSelected(true);
        closeSettingView();
    }
    private void closeSettingView() {
        // Close all the setting views.
        mEraserSettingView.setVisibility(SpenSurfaceView.GONE);
        mPenSettingView.setVisibility(SpenSurfaceView.GONE);
        mTextSettingView.setVisibility(SpenSurfaceView.GONE);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mPenSettingView != null) {
            mPenSettingView.close();
        }
        if (mEraserSettingView != null) {
            mEraserSettingView.close();
        }
        if (mSpenSurfaceView != null) {
            mSpenSurfaceView.close();
            mSpenSurfaceView = null;
        }
        if (mSpenNoteDoc != null) {
            try {
                if (mIsDiscard) {
                    mSpenNoteDoc.discard();
                } else {
                    mSpenNoteDoc.close();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            mSpenNoteDoc = null;
        }
    }

    private final SpenPageDoc.HistoryListener mHistoryListener = new SpenPageDoc.HistoryListener() {
        @Override
        public void onCommit(SpenPageDoc page) {
        }
        @Override
        public void onUndoable(SpenPageDoc page, boolean undoable) {
            // Enable or disable the button according to the availability of undo.
            mUndoBtn.setEnabled(undoable);
        }
        @Override
        public void onRedoable(SpenPageDoc page, boolean redoable) {
            // Enable or disable the button according to the availability of redo.
            mRedoBtn.setEnabled(redoable);
        }
    };

    /* Listener undo redo */
    private final View.OnClickListener undoNredoBtnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (mSpenPageDoc == null) {
                return;
            }
            // Undo button is clicked.
            if (v.equals(mUndoBtn)) {
                if (mSpenPageDoc.isUndoable()) {
                    SpenPageDoc.HistoryUpdateInfo[] userData = mSpenPageDoc.undo();
                    mSpenSurfaceView.updateUndo(userData);
                }
                // Redo button is clicked.
            } else if (v.equals(mRedoBtn)) {
                if (mSpenPageDoc.isRedoable()) {
                    SpenPageDoc.HistoryUpdateInfo[] userData = mSpenPageDoc.redo();
                    mSpenSurfaceView.updateRedo(userData);
                }
            }
        }
    };

    /* Background Listener */
    private final View.OnClickListener mBgBtnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if(hasReadStorePermission()){
                closeSettingView();
                callGalleryForInputImage(REQUEST_CODE_SELECT_IMAGE_BACKGROUND);
            } else {
                requestReadStorePermission();
            }
        }
    };
    private void callGalleryForInputImage(int nRequestCode) {
        // Get an image from Gallery.
        try {
            Intent galleryIntent = new Intent(Intent.ACTION_GET_CONTENT);
            galleryIntent.setType("image/*");
            startActivityForResult(galleryIntent, nRequestCode);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(this, "Cannot find gallery.", Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
    }
    @TargetApi(Build.VERSION_CODES.M)
    public boolean hasReadStorePermission() {
        if (SDK_VERSION < 23) {
            return true;
        }
        return PackageManager.PERMISSION_GRANTED == checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE);
    }
    @TargetApi(Build.VERSION_CODES.M)
    public void requestReadStorePermission() {
        if (SDK_VERSION < 23) {
            return;
        }
        requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, ADD_BACKGROUND_REQUEST);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == ADD_BACKGROUND_REQUEST) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                mBgBtnClickListener.onClick(mBgBtn);
            }else if (requestCode == PEMISSION_REQUEST_CODE) {
                if (grantResults != null ) {
                    for(int i= 0; i< grantResults.length;i++){
                        if(grantResults[i]!= PackageManager.PERMISSION_GRANTED){
                            Toast.makeText(this
                                    ,"permission: " + permissions[i] + " is denied", Toast.LENGTH_SHORT).show();
                        }
                    }
                }
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            if (data == null) {
                Toast.makeText(this, "Cannot find the image", Toast.LENGTH_SHORT).show();
                return;
            }
            // Process background image request.
            if (requestCode == REQUEST_CODE_SELECT_IMAGE_BACKGROUND) {
                // Get the image's URI and set the file path to the background image.
                Uri imageFileUri = data.getData();
                String imagePath = SDKUtils.getRealPathFromURI(this, imageFileUri);
                try {
                    mSpenPageDoc.setBackgroundImageMode(SpenPageDoc.BACKGROUND_IMAGE_MODE_STRETCH);
                    mSpenPageDoc.setVolatileBackgroundImage(decodeFile(imagePath));
                    mSpenSurfaceView.update();
                } catch (IllegalStateException e) {
                    Toast.makeText(this, "Invalid image file", Toast.LENGTH_SHORT).show();
                    e.printStackTrace();
                }
            }
        }
    }

    public static class SDKUtils {
        public static boolean processUnsupportedException(final Activity activity, SsdkUnsupportedException e) {
            e.printStackTrace();
            int errType = e.getType();
            // If the device is not a Samsung device or the device does not support Pen.
            if (errType == SsdkUnsupportedException.VENDOR_NOT_SUPPORTED
                    || errType == SsdkUnsupportedException.DEVICE_NOT_SUPPORTED) {
                Toast.makeText(activity, "This device does not support Spen.", Toast.LENGTH_SHORT).show();
                activity.finish();
            } else if (errType == SsdkUnsupportedException.LIBRARY_NOT_INSTALLED) {
                // If SpenSDK APK is not installed.
                showAlertDialog(activity, "You need to install additional Spen software"
                        + " to use this application."
                        + "You will be taken to the installation screen."
                        + "Restart this application after the software has been installed.", true);
            } else if (errType == SsdkUnsupportedException.LIBRARY_UPDATE_IS_REQUIRED) {
                // SpenSDK APK must be updated.
                showAlertDialog(activity, "You need to update your Spen software to use this application."
                        + " You will be taken to the installation screen."
                        + " Restart this application after the software has been updated.", true);
            } else if (errType == SsdkUnsupportedException.LIBRARY_UPDATE_IS_RECOMMENDED) {
                // Recommended to update SpenSDK APK to a new version available.
                showAlertDialog(activity, "We recommend that you update your Spen software"
                        + " before using this application." + " You will be taken to the installation screen."
                        + " Restart this application after the software has been updated.", false);
                return false; // Procceed to the normal activity process if it is not updated.
            }
            return true;
        }
        private static void showAlertDialog(final Activity activity, String msg, final boolean closeActivity) {
            AlertDialog.Builder dlg = new AlertDialog.Builder(activity);
            dlg.setIcon(activity.getResources().getDrawable(android.R.drawable.ic_dialog_alert));
            dlg.setTitle("Upgrade Notification").setMessage(msg)
                    .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            // Go to the market site and install or update APK.
                            Uri uri = Uri.parse("market://details?id=" + Spen.getSpenPackageName());
                            Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                            activity.startActivity(intent);
                            dialog.dismiss();
                            activity.finish();
                        }
                    }).setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    if (closeActivity) {
                        // Terminate the activity if APK is not installed.
                        activity.finish();
                    }
                    dialog.dismiss();
                }
            }).setOnCancelListener(new DialogInterface.OnCancelListener() {
                @Override
                public void onCancel(DialogInterface dialog) {
                    if (closeActivity) {
                        // Terminate the activity if APK is not installed.
                        activity.finish();
                    }
                }
            }).show();
            dlg = null;
        }
        public static String getRealPathFromURI(Context context, Uri uri) {
            if (Build.VERSION.SDK_INT < 11) {
                return getRealPathFromURI_BelowAPI11(context, uri);
            }
            if (Build.VERSION.SDK_INT < 18) {
                return getRealPathFromURI_API11to18(context, uri);
            }
            if (Build.VERSION.SDK_INT < 21) {
                return getRealPathFromURI_API19to21(context, uri);
            }
            return getRealPathFromURI22(context, uri);
        }
        public static String getRealPathFromURI_API19to21(Context context, Uri uri) {
            Cursor cursor = context.getContentResolver().query(Uri.parse(uri.toString()), null, null, null, null);
            if (cursor != null)
            {
                cursor.moveToNext();
                int columnIndex = cursor.getColumnIndex(MediaStore.MediaColumns.DATA);
                if (columnIndex >= 0)
                    return cursor.getString(columnIndex);
                return getRealPathFromURI_API11to18(context, uri);
            }
            return null;
        }
        @SuppressLint("NewApi")
        public static String getRealPathFromURI22(Context context, Uri uri) {
            String filePath = "";
            if (DocumentsContract.isDocumentUri(context, uri)) {
                String wholeID = DocumentsContract.getDocumentId(uri);
                // Split at colon, use second item in the array
                String id = wholeID.split(":")[1];
                String[] column = { MediaStore.Images.Media.DATA };
                // where id is equal to
                String sel = MediaStore.Images.Media._ID + "=?";
                Cursor cursor = context.getContentResolver().query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, column,
                        sel, new String[] { id }, null);
                if (cursor != null)
                {
                    int columnIndex = cursor.getColumnIndex(column[0]);
                    if (cursor.moveToFirst()) {
                        filePath = cursor.getString(columnIndex);
                    }
                    cursor.close();
                }
                return filePath;
            }
            filePath = getRealPathFromURI_API19to21(context, uri);
            return filePath;
        }
        @SuppressLint("NewApi")
        public static String getRealPathFromURI_API11to18(Context context, Uri contentUri) {
            String[] proj = { MediaStore.Images.Media.DATA };
            String result = null;
            CursorLoader cursorLoader = new CursorLoader(context, contentUri, proj, null, null, null);
            Cursor cursor = cursorLoader.loadInBackground();
            if (cursor != null) {
                int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
                cursor.moveToFirst();
                if (column_index >= 0)
                    result = cursor.getString(column_index);
            }
            return result;
        }
        public static String getRealPathFromURI_BelowAPI11(Context context, Uri contentUri) {
            String[] proj = { MediaStore.Images.Media.DATA };
            Cursor cursor = context.getContentResolver().query(contentUri, proj, null, null, null);
            if (cursor != null)
            {
                int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
                cursor.moveToFirst();
                return cursor.getString(column_index);
            }
            return null;
        }
    }

    private final SpenTouchListener mPenTouchListener = new SpenTouchListener() {
        @Override
        public boolean onTouch(View view, MotionEvent event) {
            if (event.getAction() == MotionEvent.ACTION_UP && event.getToolType(0) == mToolType) {
                // Check if the control is created.
                SpenControlBase control = mSpenSurfaceView.getControl();
                if (control == null) {
                    // When Pen touches the display while it is in Add ObjectImage mode
                    if (mMode == MODE_IMG_OBJ) {
                        // Set a bitmap file to ObjectImage.
                        SpenObjectImage imgObj = new SpenObjectImage();
                        Bitmap imageBitmap = BitmapFactory.decodeResource(MainActivity.this.getResources(),
                                R.mipmap.ic_launcher);
                        imgObj.setImage(imageBitmap);
                        // Set the location to insert ObjectImage and add it to PageDoc.
                        float x = event.getX();
                        float y = event.getY();
                        float panX = mSpenSurfaceView.getPan().x;
                        float panY = mSpenSurfaceView.getPan().y;
                        float zoom = mSpenSurfaceView.getZoomRatio();
                        float imgWidth = imageBitmap.getWidth() * zoom;
                        float imgHeight = imageBitmap.getHeight() * zoom;
                        RectF imageRect = new RectF();
                        imageRect.set((x - imgWidth / 2) / zoom + panX, (y - imgHeight / 2) / zoom + panY,
                                (x + imgWidth / 2) / zoom + panX, (y + imgHeight / 2) / zoom + panY);
                        imgObj.setRect(imageRect, true);
                        mSpenPageDoc.appendObject(imgObj);
                        mSpenSurfaceView.update();
                        imageBitmap.recycle();
                        return true;
                    }else if (mMode == MODE_TEXT_OBJ) {
                        // Set the location to insert ObjectTextBox and add it to PageDoc.
                        SpenObjectTextBox textObj = new SpenObjectTextBox();
                        PointF canvasPos = getCanvasPoint(event);
                        float x = canvasPos.x;
                        float y = canvasPos.y;
                        float textBoxHeight = getTextBoxDefaultHeight(textObj);
                        if ((y + textBoxHeight) > mSpenPageDoc.getHeight()) {
                            y = mSpenPageDoc.getHeight() - textBoxHeight;
                        }
                        RectF rect = new RectF(x, y, x + 350, y + textBoxHeight);
                        textObj.setRect(rect, true);
                        mSpenPageDoc.appendObject(textObj);
                        mSpenPageDoc.selectObject(textObj);
                        mSpenSurfaceView.update();
                    }else if (mMode == MODE_STROKE_OBJ) {
                        // Set the location to insert ObjectStroke and add it to PageDoc.
                        PointF canvasPos = getCanvasPoint(event);
                        float posX = canvasPos.x;
                        int pointSize = 157;
                        PointF[] points = new PointF[pointSize];
                        float[] pressures = new float[pointSize];
                        int[] timestamps = new int[pointSize];
                        for (int i = 0; i < pointSize; i++) {
                            points[i] = new PointF();
                            points[i].x = posX++;
                            points[i].y = (float) (canvasPos.y + Math.sin(.04 * i) * 50);
                            pressures[i] = 1;
                            timestamps[i] = (int) android.os.SystemClock.uptimeMillis();
                        }
                        SpenObjectStroke strokeObj = new SpenObjectStroke(mPenSettingView.getInfo().name, points,
                                pressures, timestamps);
                        strokeObj.setPenSize(mPenSettingView.getInfo().size);
                        strokeObj.setColor(mPenSettingView.getInfo().color);
                        mSpenPageDoc.appendObject(strokeObj);
                        mSpenSurfaceView.update();
                    }
                }
            }
            return false;
        }
    };

        private float getTextBoxDefaultHeight(SpenObjectTextBox textBox) {
            if (textBox == null) {
                return 0;
            }
            float height = 0, lineSpacing = 0, lineSpacePercent = 1.3f;
            float margin = textBox.getTopMargin() + textBox.getBottomMargin();
            ArrayList<SpenTextParagraphBase> pInfo = textBox.getTextParagraph();
            if (pInfo != null) {
                for (SpenTextParagraphBase info : pInfo) {
                    if (info instanceof SpenLineSpacingParagraph) {
                        if (((SpenLineSpacingParagraph) info).getLineSpacingType() ==
                                SpenLineSpacingParagraph.TYPE_PERCENT) {
                            lineSpacePercent = ((SpenLineSpacingParagraph) info).getLineSpacing();
                        } else if (((SpenLineSpacingParagraph) info).getLineSpacingType() ==
                                SpenLineSpacingParagraph.TYPE_PIXEL) {
                            lineSpacing = ((SpenLineSpacingParagraph) info).getLineSpacing();
                        }
                    }
                }
            }
            if (lineSpacing != 0){
                height = lineSpacing + margin;
            } else {
                float fontSize = mSpenPageDoc.getWidth()/20;
                ArrayList<SpenTextSpanBase> sInfo =
                        textBox.findTextSpan(textBox.getCursorPosition(), textBox.getCursorPosition());
                if (sInfo != null) {
                    for (SpenTextSpanBase info : sInfo) {
                        if (info instanceof SpenFontSizeSpan) {
                            fontSize = ((SpenFontSizeSpan) info).getSize();
                            break;
                        }
                    }
                }
                height = fontSize * lineSpacePercent;
            }
            return height;
        }
        private PointF getCanvasPoint(MotionEvent event) {
            float panX = mSpenSurfaceView.getPan().x;
            float panY = mSpenSurfaceView.getPan().y;
            float zoom = mSpenSurfaceView.getZoomRatio();
            return new PointF(event.getX() / zoom + panX, event.getY() / zoom + panY);
        }

    private final View.OnClickListener mSaveFileBtnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if(checkPermission()){
                return;
            }
            mSpenSurfaceView.closeControl();
            closeSettingView();
            saveNoteFile(false);
        }
    };
    private boolean saveNoteFile(final boolean isClose) {
        if (!mFilePath.exists()) {
            if (!mFilePath.mkdirs()) {
                Toast.makeText(this, "Save Path Creation Error", Toast.LENGTH_SHORT).show();
                return false;
            }
        }
        // Prompt Save File dialog to get the file name
        // and get its save format option (note file or image).
        LayoutInflater inflater = (LayoutInflater) this.getSystemService(LAYOUT_INFLATER_SERVICE);
        final View layout = inflater.inflate(R.layout.save_file_dialog, (ViewGroup) findViewById(R.id.layout_root));
        AlertDialog.Builder builderSave = new AlertDialog.Builder(this);
        builderSave.setTitle("Enter file name");
        builderSave.setView(layout);
        final EditText inputPath = (EditText) layout.findViewById(R.id.input_path);
        inputPath.setText("Note");
        builderSave.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                final RadioGroup selectFileExt = (RadioGroup) layout.findViewById(R.id.radioGroup);
                // Set the save directory for the file.
                String saveFilePath = mFilePath.getPath() + '/';
                String fileName = inputPath.getText().toString();
                if (!fileName.equals("")) {
                    saveFilePath += fileName;
                    int checkedRadioButtonId = selectFileExt.getCheckedRadioButtonId();
                    if (checkedRadioButtonId == R.id.radioNote) {
                        saveFilePath += ".spd";
                        saveNoteFile(saveFilePath);
                    } else if (checkedRadioButtonId == R.id.radioImage) {
                        saveFilePath += ".png";
                        captureSpenSurfaceView(saveFilePath);
                    } else {
                    }
                    if (isClose) {
                        finish();
                    }
                } else {
                    Toast.makeText(MainActivity.this, "Invalid filename !!!", Toast.LENGTH_LONG).show();
                }
            }
        });
        builderSave.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (isClose) {
                    finish();
                }
            }
        });
        AlertDialog dlgSave = builderSave.create();
        dlgSave.show();
        return true;
    }
    private boolean saveNoteFile(String strFileName) {
        try {
            // Save NoteDoc
            mSpenNoteDoc.save(strFileName, false);
            Toast.makeText(this, "Save success to " + strFileName, Toast.LENGTH_SHORT).show();
        } catch (IOException e) {
            Toast.makeText(this, "Cannot save NoteDoc file.", Toast.LENGTH_SHORT).show();
            e.printStackTrace();
            return false;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }
    private void captureSpenSurfaceView(String strFileName) {
        // Capture the view
        Bitmap imgBitmap = mSpenSurfaceView.captureCurrentView(true);
        if (imgBitmap == null) {
            Toast.makeText(this, "Capture failed." + strFileName, Toast.LENGTH_SHORT).show();
            return;
        }
        OutputStream out = null;
        try {
            // Create FileOutputStream and save the captured image.
            out = new FileOutputStream(strFileName);
            imgBitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
            // Save the note information.
            mSpenNoteDoc.save(out, false);
            out.close();
            Toast.makeText(this, "Captured images were stored in the file" + strFileName, Toast.LENGTH_SHORT)
                    .show();
        } catch (IOException e) {
            File tmpFile = new File(strFileName);
            if (tmpFile.exists()) {
                tmpFile.delete();
            }
            Toast.makeText(this, "Failed to save the file.", Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        } catch (Exception e) {
            File tmpFile = new File(strFileName);
            if (tmpFile.exists()) {
                tmpFile.delete();
            }
            Toast.makeText(this, "Failed to save the file.", Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
        imgBitmap.recycle();
    }

    private final View.OnClickListener mLoadFileBtnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if(checkPermission()){
                return;
            }
            mSpenSurfaceView.closeControl();
            closeSettingView();
            loadNoteFile();
        }
    };
    private void loadNoteFile() {
        // Load the file list.
        final String fileList[] = setFileList();
        if (fileList == null) {
            return;
        }
        // Prompt Load File dialog.
        new AlertDialog.Builder(this).setTitle("Select file")
                .setItems(fileList, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String strFilePath = mFilePath.getPath() + '/' + fileList[which];
                        try {
                            SpenObjectTextBox.setInitialCursorPos(SpenObjectTextBox.CURSOR_POS_END);
                            // Create NoteDoc with the selected file.
                            SpenNoteDoc tmpSpenNoteDoc = new SpenNoteDoc(MainActivity.this, strFilePath, mScreenRect.width(),
                                    SpenNoteDoc.MODE_WRITABLE, true);
                            mSpenNoteDoc.close();
                            mSpenNoteDoc = tmpSpenNoteDoc;
                            if (mSpenNoteDoc.getPageCount() == 0) {
                                mSpenPageDoc = mSpenNoteDoc.appendPage();
                            } else {
                                mSpenPageDoc = mSpenNoteDoc.getPage(mSpenNoteDoc.getLastEditedPageIndex());
                            }
                            mSpenSurfaceView.setPageDoc(mSpenPageDoc, true);
                            mSpenSurfaceView.update();
                            Toast.makeText(MainActivity.this, "Successfully loaded noteFile.", Toast.LENGTH_SHORT).show();
                        } catch (IOException e) {
                            Toast.makeText(MainActivity.this, "Cannot open this file.", Toast.LENGTH_LONG).show();
                        } catch (SpenUnsupportedTypeException e) {
                            Toast.makeText(MainActivity.this, "This file is not supported.", Toast.LENGTH_LONG).show();
                        } catch (SpenInvalidPasswordException e) {
                            Toast.makeText(MainActivity.this, "This file is locked by a password.", Toast.LENGTH_LONG).show();
                        } catch (SpenUnsupportedVersionException e) {
                            Toast.makeText(MainActivity.this, "This file is the version that does not support.",
                                    Toast.LENGTH_LONG).show();
                        } catch (Exception e) {
                            Toast.makeText(MainActivity.this, "Failed to load noteDoc.", Toast.LENGTH_LONG).show();
                        }
                    }
                }).show();
    }
    private String[] setFileList() {
        // Call the file list under the directory in mFilePath.
        if (!mFilePath.exists()) {
            if (!mFilePath.mkdirs()) {
                Toast.makeText(this, "Save Path Creation Error", Toast.LENGTH_SHORT).show();
                return null;
            }
        }
        // Filter in spd and png files.
        File[] fileList = mFilePath.listFiles(new txtFileFilter());
        if (fileList == null) {
            Toast.makeText(this, "File does not exist.", Toast.LENGTH_SHORT).show();
            return null;
        }
        int i = 0;
        String[] strFileList = new String[fileList.length];
        for (File file : fileList) {
            strFileList[i++] = file.getName();
        }
        return strFileList;
    }
    static class txtFileFilter implements FilenameFilter {
        @Override
        public boolean accept(File dir, String name) {
            return (name.endsWith(".spd") || name.endsWith(".png"));
        }
    }

    private static final int PEMISSION_REQUEST_CODE = 1;
    @TargetApi(Build.VERSION_CODES.M)
    public boolean checkPermission() {
        if (SDK_VERSION < 23) {
            return false;
        }
        List<String> permissionList = new ArrayList<String>(Arrays.asList(Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.READ_EXTERNAL_STORAGE));
        if(PackageManager.PERMISSION_GRANTED == checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)){
            permissionList.remove(Manifest.permission.WRITE_EXTERNAL_STORAGE);
        }
        if(PackageManager.PERMISSION_GRANTED == checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE)){
            permissionList.remove(Manifest.permission.READ_EXTERNAL_STORAGE);
        }
        if(permissionList.size()>0) {
            requestPermissions(permissionList.toArray(new String[permissionList.size()]), PEMISSION_REQUEST_CODE);
            return true;
        }
        return false;
    }

    @Override
    public void onBackPressed() {
        if (mSpenPageDoc.getObjectCount(true) > 0 && mSpenPageDoc.isChanged()) {
            AlertDialog.Builder dlg = new AlertDialog.Builder(this);
            dlg.setIcon(this.getResources().getDrawable(android.R.drawable.ic_dialog_alert));
            dlg.setTitle(this.getResources().getString(R.string.app_name))
                    .setMessage("Do you want to exit after save?")
                    .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            if(checkPermission()){
                                return;
                            }
                            saveNoteFile(true);
                            dialog.dismiss();
                        }
                    }).setNeutralButton("No", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();
                    mIsDiscard = true;
                    finish();
                }
            }).setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();
                }
            }).show();
            dlg = null;
        } else {
            super.onBackPressed();
        }
    }
}
