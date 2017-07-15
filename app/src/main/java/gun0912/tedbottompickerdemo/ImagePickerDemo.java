/*
 * Copyright (C) 2016 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package gun0912.tedbottompickerdemo;

import android.Manifest;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.annotation.NonNull;
import android.support.annotation.StringRes;
import android.support.design.widget.BottomSheetBehavior;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.FileProvider;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.google.android.cameraview.AspectRatio;
import com.google.android.cameraview.CameraView;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;
import java.util.Set;

import gun0912.tedbottompicker.GridSpacingItemDecoration;
import gun0912.tedbottompicker.TedBottomPicker;
import gun0912.tedbottompicker.adapter.ImageGalleryAdapter;


/**
 * This demo app saves the taken picture to a constant file.
 * $ adb pull /sdcard/Android/data/com.google.android.cameraview.demo/files/Pictures/picture.jpg
 */
public class ImagePickerDemo extends AppCompatActivity implements
        ActivityCompat.OnRequestPermissionsResultCallback,
        AspectRatioFragment.Listener {

    private static final String TAG = "ImagePickerDemo";

    private static final int REQUEST_CAMERA_PERMISSION = 1;

    private static final String FRAGMENT_DIALOG = "dialog";

    final private int REQUEST_CODE_ASK_MULTIPLE_PERMISSIONS = 124;


    private static final int[] FLASH_OPTIONS = {
            CameraView.FLASH_AUTO,
            CameraView.FLASH_OFF,
            CameraView.FLASH_ON,
    };

    private static final int[] FLASH_ICONS = {
            R.drawable.ic_flash_auto,
            R.drawable.ic_flash_off,
            R.drawable.ic_flash_on,
    };

    private static final int[] FLASH_TITLES = {
            R.string.flash_auto,
            R.string.flash_off,
            R.string.flash_on,
    };

    private int mCurrentFlash;

    private CameraView mCameraView;

    private Handler mBackgroundHandler;
    private RecyclerView galleryView;

    private ImageGalleryAdapter imageGalleryAdapter;

    private Uri cameraImageUri;


    ArrayList<Uri> selectedUriList;
    ArrayList<Uri> tempUriList;

    static final int REQ_CODE_CAMERA = 1;
    static final int REQ_CODE_GALLERY = 2;
    static final String EXTRA_CAMERA_IMAGE_URI = "camera_image_uri";
    static final String EXTRA_CAMERA_SELECTED_IMAGE_URI = "camera_selected_image_uri";

    TedBottomPicker bottomSheetDialogFragment;

    private BottomSheetBehavior bottomSheetBehavior;
    private FrameLayout bottomSheetView;

    private String pickerType;
    Uri selectedUri;
    private View.OnClickListener mOnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            switch (v.getId()) {
                case R.id.take_picture:
                    if (mCameraView != null) {
                        mCameraView.takePicture();
                    }
                    break;
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.image_picker_demo);
        mCameraView = (CameraView) findViewById(R.id.camera);
        bottomSheetView = (FrameLayout) findViewById(R.id.container);

        // setRecyclerView();
        if (mCameraView != null) {
            mCameraView.addCallback(mCallback);
        }

        if (getIntent() != null) {
            pickerType = getIntent().getStringExtra("picker");
        }
        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.take_picture);
        if (fab != null) {
            fab.setOnClickListener(mOnClickListener);
        }
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayShowTitleEnabled(false);
        }

    }

    @Override
    protected void onResume() {
        super.onResume();
        if (checkPermission()) {
            mCameraView.start();
            if (bottomSheetDialogFragment == null || !bottomSheetDialogFragment.isVisible()) {

                if (pickerType.equalsIgnoreCase("single"))
                    showBottomPicker();
                else if (pickerType.equalsIgnoreCase("multi")) {
                    showMultiBottomPicker();
                }


            }

        } else {
            requestPermission();
        }
    }

    private void showMultiBottomPicker() {
        bottomSheetDialogFragment = new TedBottomPicker.Builder(ImagePickerDemo.this)
                .setOnMultiImageSelectedListener(
                        new TedBottomPicker.OnMultiImageSelectedListener() {
                            @Override
                            public void onImagesSelected(ArrayList<Uri> uriList) {
                                selectedUriList = uriList;
                                Toast.makeText(ImagePickerDemo.this, "length " + uriList.size(),
                                        Toast.LENGTH_SHORT).show();
                                // showUriList(uriList);
                            }
                        })
                .setPeekHeight(getResources().getDisplayMetrics().heightPixels / 2)
                //.setPeekHeight(300)
                .showCameraTile(false)
                // .showGalleryTile(true)
                .setCompleteButtonText("Done")
                .setEmptySelectionText("No Select")
                .setSelectedUriList(selectedUriList)
                .create();
        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        ft.add(R.id.container, bottomSheetDialogFragment);
        ft.commitAllowingStateLoss();

        bottomSheetBehavior = BottomSheetBehavior.from(bottomSheetView);
        bottomSheetBehavior.setBottomSheetCallback(new BottomSheetBehavior.BottomSheetCallback() {
            @Override
            public void onStateChanged(@NonNull View bottomSheet, int newState) {
                if (newState == BottomSheetBehavior.STATE_COLLAPSED)
                    bottomSheetDialogFragment.setStateOpen(false);
                else
                    bottomSheetDialogFragment.setStateOpen(true);
            }

            @Override
            public void onSlide(@NonNull View bottomSheet, float slideOffset) {

            }
        });
    }

    private void showBottomPicker() {
        bottomSheetDialogFragment = new TedBottomPicker.Builder(ImagePickerDemo.this)
                .setOnImageSelectedListener(new TedBottomPicker.OnImageSelectedListener() {
                    @Override
                    public void onImageSelected(Uri uri) {
                        imagePreview(uri);
                        Toast.makeText(ImagePickerDemo.this, "length ",
                                Toast.LENGTH_SHORT).show();
                    }
                })
                .setPeekHeight(getResources().getDisplayMetrics().heightPixels / 2)
                //.setPeekHeight(300)
                .showCameraTile(false)
                // .showGalleryTile(true)
                .setCompleteButtonText("Done")
                .setEmptySelectionText("No Select")
                .setSelectedUriList(selectedUriList)
                .create();
        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        ft.add(R.id.container, bottomSheetDialogFragment);
        ft.commitAllowingStateLoss();

        bottomSheetBehavior = BottomSheetBehavior.from(bottomSheetView);
        bottomSheetBehavior.setBottomSheetCallback(new BottomSheetBehavior.BottomSheetCallback() {
            @Override
            public void onStateChanged(@NonNull View bottomSheet, int newState) {
                if (newState == BottomSheetBehavior.STATE_COLLAPSED)
                    bottomSheetDialogFragment.setStateOpen(false);
                else
                    bottomSheetDialogFragment.setStateOpen(true);
            }

            @Override
            public void onSlide(@NonNull View bottomSheet, float slideOffset) {

            }
        });


        //
    }

    @Override
    protected void onPause() {
        if (mCameraView != null)
            mCameraView.stop();
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mBackgroundHandler != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
                mBackgroundHandler.getLooper().quitSafely();
            } else {
                mBackgroundHandler.getLooper().quit();
            }
            mBackgroundHandler = null;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.aspect_ratio:
                FragmentManager fragmentManager = getSupportFragmentManager();
                if (mCameraView != null
                        && fragmentManager.findFragmentByTag(FRAGMENT_DIALOG) == null) {
                    final Set<AspectRatio> ratios = mCameraView.getSupportedAspectRatios();
                    final AspectRatio currentRatio = mCameraView.getAspectRatio();
                    AspectRatioFragment.newInstance(ratios, currentRatio)
                            .show(fragmentManager, FRAGMENT_DIALOG);
                }
                return true;
            case R.id.switch_flash:
                if (mCameraView != null) {
                    mCurrentFlash = (mCurrentFlash + 1) % FLASH_OPTIONS.length;
                    item.setTitle(FLASH_TITLES[mCurrentFlash]);
                    item.setIcon(FLASH_ICONS[mCurrentFlash]);
                    mCameraView.setFlash(FLASH_OPTIONS[mCurrentFlash]);
                }
                return true;
            case R.id.switch_camera:
                if (mCameraView != null) {
                    int facing = mCameraView.getFacing();
                    mCameraView.setFacing(facing == CameraView.FACING_FRONT ?
                            CameraView.FACING_BACK : CameraView.FACING_FRONT);
                }
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onAspectRatioSelected(@NonNull AspectRatio ratio) {
        if (mCameraView != null) {
            Toast.makeText(this, ratio.toString(), Toast.LENGTH_SHORT).show();
            mCameraView.setAspectRatio(ratio);
        }
    }

    private Handler getBackgroundHandler() {
        if (mBackgroundHandler == null) {
            HandlerThread thread = new HandlerThread("background");
            thread.start();
            mBackgroundHandler = new Handler(thread.getLooper());
        }
        return mBackgroundHandler;
    }

    private CameraView.Callback mCallback
            = new CameraView.Callback() {

        @Override
        public void onCameraOpened(CameraView cameraView) {
            Log.d(TAG, "onCameraOpened");
        }

        @Override
        public void onCameraClosed(CameraView cameraView) {
            Log.d(TAG, "onCameraClosed");
        }

        @Override
        public void onPictureTaken(CameraView cameraView, final byte[] data) {
            Log.d(TAG, "onPictureTaken " + data.length);
            Toast.makeText(getApplicationContext(), getString(R.string.picture_taken),
                    Toast.LENGTH_SHORT)
                    .show();

            getBackgroundHandler().post(new Runnable() {
                @Override
                public void run() {
                    String timeStamp = new SimpleDateFormat("yyyyMMddHHmmss",
                            Locale.getDefault()).format(
                            new Date());
                    String imageFileName = "JPEG_" + timeStamp + "_";
                    File file = new File(getExternalFilesDir(Environment.DIRECTORY_PICTURES),
                            imageFileName);

                    OutputStream os = null;
                    Uri photoURI = FileProvider.getUriForFile(getApplicationContext(),
                            getApplicationContext().getPackageName() + ".provider", file);

                    bottomSheetDialogFragment.onActivityResultCamera(photoURI);
                    try {
                        os = new FileOutputStream(file);
                        os.write(data);
                        os.close();
                    } catch (IOException e) {
                        Log.w(TAG, "Cannot write to " + file, e);
                    } finally {
                        if (os != null) {
                            try {
                                os.close();
                            } catch (IOException e) {
                                // Ignore
                            }
                        }
                    }
                }
            });
        }

    };

    public static class ConfirmationDialogFragment extends DialogFragment {

        private static final String ARG_MESSAGE = "message";
        private static final String ARG_PERMISSIONS = "permissions";
        private static final String ARG_REQUEST_CODE = "request_code";
        private static final String ARG_NOT_GRANTED_MESSAGE = "not_granted_message";

        public static ConfirmationDialogFragment newInstance(@StringRes int message,
                                                             String[] permissions, int requestCode, @StringRes int notGrantedMessage) {
            ConfirmationDialogFragment fragment = new ConfirmationDialogFragment();
            Bundle args = new Bundle();
            args.putInt(ARG_MESSAGE, message);
            args.putStringArray(ARG_PERMISSIONS, permissions);
            args.putInt(ARG_REQUEST_CODE, requestCode);
            args.putInt(ARG_NOT_GRANTED_MESSAGE, notGrantedMessage);
            fragment.setArguments(args);
            return fragment;
        }

        @NonNull
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            final Bundle args = getArguments();
            return new AlertDialog.Builder(getActivity())
                    .setMessage(args.getInt(ARG_MESSAGE))
                    .setPositiveButton(android.R.string.ok,
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    String[] permissions = args.getStringArray(ARG_PERMISSIONS);
                                    if (permissions == null) {
                                        throw new IllegalArgumentException();
                                    }
                                    ActivityCompat.requestPermissions(getActivity(),
                                            permissions, args.getInt(ARG_REQUEST_CODE));
                                }
                            })
                    .setNegativeButton(android.R.string.cancel,
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    Toast.makeText(getActivity(),
                                            args.getInt(ARG_NOT_GRANTED_MESSAGE),
                                            Toast.LENGTH_SHORT).show();
                                }
                            })
                    .create();
        }

    }

    private void setRecyclerView() {
        GridLayoutManager gridLayoutManager = new GridLayoutManager(getApplicationContext(), 3);
        galleryView.setLayoutManager(gridLayoutManager);
        galleryView.addItemDecoration(
                new GridSpacingItemDecoration(gridLayoutManager.getSpanCount(), 10, false));
    }


    private void requestPermission() {

        ActivityCompat.requestPermissions(ImagePickerDemo.this,
                new String[]{Manifest.permission.CAMERA,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE},
                REQUEST_CODE_ASK_MULTIPLE_PERMISSIONS);

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[],
                                           int[] grantResults) {
        switch (requestCode) {

            case REQUEST_CODE_ASK_MULTIPLE_PERMISSIONS:

                if (grantResults.length > 0) {
                    boolean CameraPermission = grantResults[0] == PackageManager.PERMISSION_GRANTED;
                    boolean StoragePermission =
                            grantResults[1] == PackageManager.PERMISSION_GRANTED;
                    if (CameraPermission && StoragePermission) {
                        // Permission Granted
                        mCameraView.start();
                        showBottomPicker();

                        Toast.makeText(ImagePickerDemo.this, "Permission Granted",
                                Toast.LENGTH_LONG).show();
                    } else {
                        Toast.makeText(ImagePickerDemo.this, "Permission Denied",
                                Toast.LENGTH_LONG).show();

                    }
                }

                break;
        }
    }

    public boolean checkPermission() {
        int FirstPermissionResult = ContextCompat.checkSelfPermission(getApplicationContext(),
                Manifest.permission.CAMERA);
        int SecondPermissionResult = ContextCompat.checkSelfPermission(getApplicationContext(),
                Manifest.permission.WRITE_EXTERNAL_STORAGE);

        return FirstPermissionResult == PackageManager.PERMISSION_GRANTED &&
                SecondPermissionResult == PackageManager.PERMISSION_GRANTED;
    }


    public void imagePreview(final Uri uri) {
        final Dialog dialog = new Dialog(this, android.R.style.Theme_Light);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setCancelable(false);
        dialog.setContentView(R.layout.image_preview);

        ImageView previewImage = (ImageView) dialog.findViewById(R.id.img_preview_image);
        Button saveButton = (Button) dialog.findViewById(R.id.btn_image_preview_save);
        Button cancelButton = (Button) dialog.findViewById(R.id.btn_image_preview_canel);

        Glide.with(this).load(uri).into(previewImage);
        saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });
        cancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });
        dialog.show();
    }
}
