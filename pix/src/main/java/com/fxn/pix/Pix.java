package com.fxn.pix;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomSheetBehavior;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.ScaleAnimation;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.fxn.adapters.InstantImageAdapter;
import com.fxn.adapters.MainImageAdapter;
import com.fxn.interfaces.OnSelectionListener;
import com.fxn.interfaces.WorkFinish;
import com.fxn.modals.Img;
import com.fxn.utility.HeaderItemDecoration;
import com.fxn.utility.ImageFetcher;
import com.fxn.utility.PermUtil;
import com.fxn.utility.Utility;

import java.io.File;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashSet;
import java.util.Set;

import io.fotoapparat.Fotoapparat;
import io.fotoapparat.configuration.CameraConfiguration;
import io.fotoapparat.parameter.ScaleType;
import io.fotoapparat.result.BitmapPhoto;
import io.fotoapparat.selector.FlashSelectorsKt;
import io.fotoapparat.selector.FocusModeSelectorsKt;
import io.fotoapparat.selector.LensPositionSelectorsKt;
import io.fotoapparat.selector.SelectorsKt;
import io.fotoapparat.view.CameraView;
import kotlin.Unit;
import kotlin.jvm.functions.Function1;

public class Pix extends AppCompatActivity implements View.OnClickListener {

    private static final String SELECTION = "selection";
    public static final String IMAGE_RESULTS = "image_results";

    private int mMaxImageCount = 1;
    private boolean mIsMultiSelectMode = false;
    private Set<Img> mSelectionList = new HashSet<>();

    private MainImageAdapter mMainImageAdapter;
    private InstantImageAdapter mInstantImageAdapter;

    private Fotoapparat fotoapparat;

    private boolean isBackLensSelected = true;
    private ImageView ivLensSwitcher;
    private ImageView ivFlashSwitcher;
    private int flashDrawable = R.drawable.ic_flash_off_black_24dp;

    private RecyclerView mainRecyclerView, instantRecyclerView;

    private BottomSheetBehavior mBottomSheetBehavior;

    private View statusBarBg, topBar, bottomButtons, sendButton;
    private TextView topBarSelectionCount, sendButtonSelectionCount;

    private View btnSelectionOk, btnSelectionCheck;


    private OnSelectionListener onSelectionListener = new OnSelectionListener() {
        @Override
        public void onClick(Img img) {
            if (mIsMultiSelectMode) {
                if (mSelectionList.contains(img)) {
                    changeImageSelection(img, false);
                } else {
                    if (mMaxImageCount <= mSelectionList.size()) {
                        Toast.makeText(Pix.this, String.format(getResources().getString(R.string.selection_limiter_pix), mSelectionList.size()), Toast.LENGTH_SHORT).show();
                        return;
                    }
                    changeImageSelection(img, true);
                }
                if (mSelectionList.size() == 0) {
                    mIsMultiSelectMode = false;
                }

            } else {
                mSelectionList.add(img);
                returnObjects();
                btnSelectionOk.setVisibility(View.GONE);
                topBarSelectionCount.setVisibility(View.GONE);
            }
        }

        @Override
        public void onLongClick(Img img) {
            if (mMaxImageCount > 1) {
                Utility.vibe(Pix.this, 50);
                mIsMultiSelectMode = true;
                changeImageSelection(img, !mSelectionList.contains(img));
            }

        }
    };

    public static void start(final Fragment context, final int requestCode, final int selectionCount) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PermUtil.checkForCamara_WritePermissions(context, new WorkFinish() {
                @Override
                public void onWorkFinish(Boolean check) {
                    Intent i = new Intent(context.getActivity(), Pix.class);
                    i.putExtra(SELECTION, selectionCount);
                    context.startActivityForResult(i, requestCode);
                }
            });
        } else {
            Intent i = new Intent(context.getActivity(), Pix.class);
            i.putExtra(SELECTION, selectionCount);
            context.startActivityForResult(i, requestCode);
        }

    }

    public static void start(Fragment context, int requestCode) {
        start(context, requestCode, 1);
    }

    public static void start(final FragmentActivity context, final int requestCode, final int selectionCount) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PermUtil.checkForCamara_WritePermissions(context, new WorkFinish() {
                @Override
                public void onWorkFinish(Boolean check) {
                    Intent i = new Intent(context, Pix.class);
                    i.putExtra(SELECTION, selectionCount);
                    context.startActivityForResult(i, requestCode);
                }
            });
        } else {
            Intent i = new Intent(context, Pix.class);
            i.putExtra(SELECTION, selectionCount);
            context.startActivityForResult(i, requestCode);
        }
    }

    public static void start(final FragmentActivity context, int requestCode) {
        start(context, requestCode, 1);
    }


    public void returnObjects() {
        ArrayList<String> list = new ArrayList<>();
        for (Img i : mSelectionList) {
            list.add(i.getUrl());
        }
        Intent resultIntent = new Intent();
        resultIntent.putStringArrayListExtra(IMAGE_RESULTS, list);
        setResult(Activity.RESULT_OK, resultIntent);
        finish();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //setupStatusBarHidden
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            Window w = getWindow();
            w.addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION);
            w.addFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);
        }

        Utility.hideStatusBar(this);
        setContentView(R.layout.activity_main_lib);
        initialize();
    }

    private void initialize() {
        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }
        try {
            mMaxImageCount = getIntent().getIntExtra(SELECTION, 1);
        } catch (Exception e) {
            e.printStackTrace();
        }

        setUpCamera();

        ImageView btnTakePicture = findViewById(R.id.takePicture);
        btnTakePicture.setOnClickListener(this);
        ivLensSwitcher = findViewById(R.id.ivLensSwitcher);
        ivLensSwitcher.setOnClickListener(this);
        ivFlashSwitcher = findViewById(R.id.ivFlashSwitcher);
        ivFlashSwitcher.setOnClickListener(this);

        topBar = findViewById(R.id.topbar);
        bottomButtons = findViewById(R.id.bottomButtons);
        statusBarBg = findViewById(R.id.status_bar_bg);

        topBarSelectionCount = findViewById(R.id.selection_count);
        sendButtonSelectionCount = findViewById(R.id.img_count);

        findViewById(R.id.selection_back).setOnClickListener(this);

        btnSelectionOk = findViewById(R.id.selection_ok);
        btnSelectionOk.setVisibility((mMaxImageCount > 1) ? View.GONE : View.VISIBLE);
        btnSelectionOk.setOnClickListener(this);

        btnSelectionCheck = findViewById(R.id.selection_check);
        btnSelectionCheck.setVisibility((mMaxImageCount > 1) ? View.VISIBLE : View.GONE);
        btnSelectionCheck.setOnClickListener(this);

        View mainFrameLayout = findViewById(R.id.mainFrameLayout);
        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT);
        lp.setMargins(0, 0, 0, Utility.getSoftButtonsBarSizePort(this));
        mainFrameLayout.setLayoutParams(lp);

        sendButton = findViewById(R.id.sendButton);
        FrameLayout.LayoutParams layoutParams = (FrameLayout.LayoutParams) sendButton.getLayoutParams();
        layoutParams.setMargins(0, 0, (int) (Utility.convertDpToPixel(16, this)),
                (int) (Utility.convertDpToPixel(174, this)));
        sendButton.setLayoutParams(layoutParams);
        sendButton.setOnClickListener(this);

        setUpInstantRecyclerView();
        setUpMainRecyclerView();

        updateImages();
    }

    @SuppressLint("ClickableViewAccessibility")
    private void setUpCamera() {
        CameraView camera = findViewById(R.id.camera_view);
        fotoapparat = Fotoapparat.with(this).into(camera)
                .previewScaleType(ScaleType.CenterCrop)  // we want the preview to fill the view
                // .photoResolution(ResolutionSelectorsKt.lowestResolution())   // we want to have the biggest photo possible
                .lensPosition(LensPositionSelectorsKt.back())      // we want back camera
                .focusMode(SelectorsKt.firstAvailable(  // (optional) use the first focus mode which is supported by device
                        FocusModeSelectorsKt.continuousFocusPicture(),
                        FocusModeSelectorsKt.autoFocus(),    // in case if continuous focus is not available on device, auto focus will be used
                        FocusModeSelectorsKt.fixed()             // if even auto focus is not available - fixed focus mode will be used
                ))
                //.flash(FlashSelectorsKt.autoRedEye())
                /*.logger(LoggersKt.loggers(            // (optional) we want to log camera events in 2 places at once
                        LoggersKt.logcat(),           // ... in logcat
                        LoggersKt.fileLogger(this)    // ... and to file
                ))*/
                .build();

        camera.setOnTouchListener(new View.OnTouchListener() {

            float mZoom = 0.0f;
            float dist = 0.0f;

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getPointerCount() > 1) {

                    switch (event.getAction() & MotionEvent.ACTION_MASK) {
                        case MotionEvent.ACTION_POINTER_DOWN:
                            dist = Utility.getFingerSpacing(event);
                            break;
                        case MotionEvent.ACTION_MOVE:
                            float maxZoom = 1f;

                            float newDist = Utility.getFingerSpacing(event);
                            if (newDist > dist) {
                                //zoom in
                                if (mZoom < maxZoom)
                                    mZoom = mZoom + 0.01f;
                            } else if (newDist < dist) {
                                //zoom out
                                if (mZoom > 0)
                                    mZoom = mZoom - 0.01f;
                            }
                            dist = newDist;
                            fotoapparat.setZoom(mZoom);
                            break;
                    }
                }
                return true;
            }
        });
        fotoapparat.start();
        fotoapparat.updateConfiguration(CameraConfiguration.builder().flash(FlashSelectorsKt.autoRedEye()).build());
    }

    private void setUpInstantRecyclerView() {
        instantRecyclerView = findViewById(R.id.instantRecyclerView);

        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this);
        linearLayoutManager.setOrientation(LinearLayoutManager.HORIZONTAL);
        instantRecyclerView.setLayoutManager(linearLayoutManager);

        mInstantImageAdapter = new InstantImageAdapter(this);
        mInstantImageAdapter.addOnSelectionListener(onSelectionListener);
        instantRecyclerView.setAdapter(mInstantImageAdapter);
    }

    private void setUpMainRecyclerView() {
        mainRecyclerView = findViewById(R.id.mainRecyclerView);

        mMainImageAdapter = new MainImageAdapter(this);
        mMainImageAdapter.addOnSelectionListener(onSelectionListener);
        mainRecyclerView.setAdapter(mMainImageAdapter);

        GridLayoutManager layoutManager = new GridLayoutManager(this, MainImageAdapter.SPAN_COUNT);
        layoutManager.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
            @Override
            public int getSpanSize(int position) {
                if (mMainImageAdapter.getItemViewType(position) == MainImageAdapter.HEADER) {
                    return MainImageAdapter.SPAN_COUNT;
                }
                return 1;
            }
        });
        mainRecyclerView.setLayoutManager(layoutManager);
        mainRecyclerView.addItemDecoration(new HeaderItemDecoration(this, mainRecyclerView, mMainImageAdapter));
    }

    private void updateImages() {
        mMainImageAdapter.clearList();
        Cursor cursor = Utility.getCursor(Pix.this);
        ArrayList<Img> INSTANTLIST = new ArrayList<>();
        String header = "";
        int limit = 100;
        if (cursor.getCount() < 100) {
            limit = cursor.getCount();
        }
        int date = cursor.getColumnIndex(MediaStore.Images.Media.DATE_TAKEN);
        int data = cursor.getColumnIndex(MediaStore.Images.Media.DATA);
        int contentUrl = cursor.getColumnIndex(MediaStore.Images.Media._ID);
        Calendar calendar;
        for (int i = 0; i < limit; i++) {
            cursor.moveToNext();
            Uri path = Uri.withAppendedPath(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, "" + cursor.getInt(contentUrl));
            calendar = Calendar.getInstance();
            calendar.setTimeInMillis(cursor.getLong(date));
            String dateDifference = Utility.getDateDifference(Pix.this, calendar);
            if (!header.equalsIgnoreCase("" + dateDifference)) {
                header = "" + dateDifference;
                INSTANTLIST.add(new Img("" + dateDifference, "", ""));
            }
            INSTANTLIST.add(new Img("" + header, "" + path, cursor.getString(data)));
        }
        cursor.close();
        new ImageFetcher(Pix.this) {
            @Override
            protected void onPostExecute(ArrayList<Img> imgs) {
                super.onPostExecute(imgs);
                mMainImageAdapter.addImageList(imgs);
            }
        }.execute(Utility.getCursor(Pix.this));
        mInstantImageAdapter.addImageList(INSTANTLIST);
        mMainImageAdapter.addImageList(INSTANTLIST);
        setBottomSheetBehavior();
    }

    private void setBottomSheetBehavior() {
        View bottomSheet = findViewById(R.id.bottom_sheet);
        mBottomSheetBehavior = BottomSheetBehavior.from(bottomSheet);
        mBottomSheetBehavior.setPeekHeight((int) (Utility.convertDpToPixel(194, this)));
        mBottomSheetBehavior.setBottomSheetCallback(new BottomSheetBehavior.BottomSheetCallback() {
            @Override
            public void onStateChanged(@NonNull View bottomSheet, int newState) {
            }

            @Override
            public void onSlide(@NonNull View bottomSheet, float slideOffset) {
                Utility.manipulateVisibility(Pix.this, slideOffset,
                        instantRecyclerView, mainRecyclerView, statusBarBg,
                        topBar, bottomButtons, sendButton, mIsMultiSelectMode);
                if (slideOffset == 1) {
                    mMainImageAdapter.notifyDataSetChanged();
                    sendButton.setVisibility(View.GONE);
                    //  fotoapparat.stop();
                } else if (slideOffset == 0) {

                    mInstantImageAdapter.notifyDataSetChanged();
                    sendButtonSelectionCount.setText(String.valueOf(mSelectionList.size()));
                    fotoapparat.start();
                }
            }
        });
    }

    @Override
    public void onBackPressed() {
        if (mSelectionList.size() > 0) {
            clearImageSelection();
            mIsMultiSelectMode = false;

        } else if (mBottomSheetBehavior.getState() == BottomSheetBehavior.STATE_EXPANDED) {
            mBottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
        } else {
            super.onBackPressed();
        }
    }

    private void clearImageSelection() {
        mMainImageAdapter.clearSelection();
        mInstantImageAdapter.clearSelection();
        mSelectionList.clear();

        checkSelectedImageCount();
    }

    private void changeImageSelection(Img img, boolean selection) {
        if (selection) {
            mSelectionList.add(img);
        } else {
            mSelectionList.remove(img);
        }
        mMainImageAdapter.select(img, selection);
        mInstantImageAdapter.select(img, selection);

        checkSelectedImageCount();
    }

    private void checkSelectedImageCount() {
        if (mSelectionList.size() == 0) {
            topBarSelectionCount.setVisibility(View.GONE);
            btnSelectionOk.setVisibility(View.GONE);
            if (mMaxImageCount > 1) {
                btnSelectionCheck.setVisibility(View.VISIBLE);
            } else {
                btnSelectionCheck.setVisibility(View.GONE);
            }

            if (sendButton.getVisibility() == View.VISIBLE) {
                Animation anim = new ScaleAnimation(
                        1f, 0f, // Start and end values for the X axis scaling
                        1f, 0f, // Start and end values for the Y axis scaling
                        Animation.RELATIVE_TO_SELF, 0.5f, // Pivot point of X scaling
                        Animation.RELATIVE_TO_SELF, 0.5f); // Pivot point of Y scaling
                anim.setFillAfter(true); // Needed to keep the result of the animation
                anim.setDuration(300);
                anim.setAnimationListener(new Animation.AnimationListener() {
                    @Override
                    public void onAnimationStart(Animation animation) {

                    }

                    @Override
                    public void onAnimationEnd(Animation animation) {
                        sendButton.setVisibility(View.GONE);
                        sendButton.clearAnimation();
                    }

                    @Override
                    public void onAnimationRepeat(Animation animation) {

                    }
                });
                sendButton.startAnimation(anim);
            }

        } else {
            topBarSelectionCount.setText(getResources().getString(R.string.pix_selected, mSelectionList.size()));
            sendButtonSelectionCount.setText(String.valueOf(mSelectionList.size()));
            if (mMaxImageCount > 1) {
                topBarSelectionCount.setVisibility(View.VISIBLE);
                btnSelectionOk.setVisibility(View.VISIBLE);
            } else {
                topBarSelectionCount.setVisibility(View.GONE);
                btnSelectionOk.setVisibility(View.GONE);
            }
            btnSelectionCheck.setVisibility(View.GONE);

            if (mBottomSheetBehavior.getState() != BottomSheetBehavior.STATE_EXPANDED && sendButton.getVisibility() == View.GONE) {
                sendButton.setVisibility(View.VISIBLE);
                Animation anim = new ScaleAnimation(
                        0f, 1f, // Start and end values for the X axis scaling
                        0f, 1f, // Start and end values for the Y axis scaling
                        Animation.RELATIVE_TO_SELF, 0.5f, // Pivot point of X scaling
                        Animation.RELATIVE_TO_SELF, 0.5f); // Pivot point of Y scaling
                anim.setFillAfter(true); // Needed to keep the result of the animation
                anim.setDuration(300);
                sendButton.startAnimation(anim);
            }
        }
    }

    private void toggleLensPosition() {
        final CameraConfiguration cameraConfiguration = new CameraConfiguration();
        final ObjectAnimator oa1 = ObjectAnimator.ofFloat(ivLensSwitcher, "scaleX", 1f, 0f).setDuration(150);
        final ObjectAnimator oa2 = ObjectAnimator.ofFloat(ivLensSwitcher, "scaleX", 0f, 1f).setDuration(150);
        oa1.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);
                oa2.start();
            }
        });
        oa1.start();
        if (isBackLensSelected) {
            isBackLensSelected = false;
            fotoapparat.switchTo(LensPositionSelectorsKt.front(), cameraConfiguration);
        } else {
            isBackLensSelected = true;
            fotoapparat.switchTo(LensPositionSelectorsKt.back(), cameraConfiguration);
        }
    }

    private void toggleFlash() {
        final int height = ivFlashSwitcher.getHeight();
        ivFlashSwitcher.animate().translationY(height).setDuration(100).setListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);
                ivFlashSwitcher.setTranslationY(-(height / 2));
                if (flashDrawable == R.drawable.ic_flash_auto_black_24dp) {
                    flashDrawable = R.drawable.ic_flash_off_black_24dp;
                    ivFlashSwitcher.setImageResource(flashDrawable);
                    fotoapparat.updateConfiguration(CameraConfiguration.builder().flash(FlashSelectorsKt.off()).build());
                } else if (flashDrawable == R.drawable.ic_flash_off_black_24dp) {
                    flashDrawable = R.drawable.ic_flash_on_black_24dp;
                    ivFlashSwitcher.setImageResource(flashDrawable);
                    fotoapparat.updateConfiguration(CameraConfiguration.builder().flash(FlashSelectorsKt.on()).build());
                } else {
                    flashDrawable = R.drawable.ic_flash_auto_black_24dp;
                    ivFlashSwitcher.setImageResource(flashDrawable);
                    fotoapparat.updateConfiguration(CameraConfiguration.builder().flash(FlashSelectorsKt.autoRedEye()).build());
                }
                ivFlashSwitcher.animate().translationY(0).setDuration(50).setListener(null).start();
            }
        }).start();
    }

    private void onSelectionCheckClick() {
        topBarSelectionCount.setText(getResources().getString(R.string.pix_tap_to_select));
        sendButtonSelectionCount.setText(String.valueOf(mSelectionList.size()));
        mIsMultiSelectMode = true;
        btnSelectionCheck.setVisibility(View.GONE);
        btnSelectionOk.setVisibility(View.VISIBLE);
        topBarSelectionCount.setVisibility(View.VISIBLE);
    }

    private void takePicture() {
        fotoapparat.takePicture().toBitmap().transform(new Function1<BitmapPhoto, Bitmap>() {
            @Override
            public Bitmap invoke(BitmapPhoto bitmapPhoto) {
                Log.e("my pick transform", bitmapPhoto.toString());
                fotoapparat.stop();
                return Utility.rotate(bitmapPhoto.bitmap, -bitmapPhoto.rotationDegrees);
            }
        }).whenAvailable(new Function1<Bitmap, Unit>() {
            @Override
            public Unit invoke(Bitmap bitmap) {
                if (bitmap != null) {
                    Log.e("my pick", bitmap.toString());
                    synchronized (bitmap) {
                        File photo = Utility.writeImage(bitmap);
                        Log.e("my pick saved", bitmap.toString() + "    ->  " + photo.length() / 1024);
                        mSelectionList.clear();
                        mSelectionList.add(new Img("", "", photo.getAbsolutePath()));
                        returnObjects();

                    }
                }
                return null;
            }
        });
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.ivLensSwitcher) {
            toggleLensPosition();
        } else if (id == R.id.ivFlashSwitcher) {
            toggleFlash();
        } else if (id == R.id.selection_ok) {
            returnObjects();
        } else if (id == R.id.selection_check) {
            onSelectionCheckClick();
        } else if (id == R.id.selection_back) {
            onBackPressed();
        } else if (id == R.id.sendButton) {
            returnObjects();
        } else if (id == R.id.takePicture) {
            takePicture();
        }

    }

    @Override
    protected void onRestart() {
        super.onRestart();
        initialize();
        fotoapparat.start();
    }

    @Override
    protected void onResume() {
        super.onResume();
        fotoapparat.start();
    }

    @Override
    protected void onPause() {
        fotoapparat.stop();
        super.onPause();
    }
}
