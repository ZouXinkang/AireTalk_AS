package com.pingshow.amper;

import java.io.File;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.LinkedList;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.*;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.AnimationSet;
import android.view.animation.TranslateAnimation;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.pingshow.amper.bean.GroupMsg;
import com.pingshow.amper.db.AmpUserDB;
import com.pingshow.amper.db.GroupDB;
import com.pingshow.network.MyNet;
import com.pingshow.util.ImageUtil;
import com.pingshow.util.MyUtil;
import com.pingshow.util.ResizeImage;

public class CreateGroupActivity extends Activity {

    private MyPreference mPref;
    float mDensity = 1.f;
    private boolean largeScreen = false;
    private LinkedList<String> sendeeList = new LinkedList<String>();
    private ImageView photoView;
    private Uri uriOrig = null;
    private String photoPath = null;
    private boolean photoAssigned = false;
    private String groupName;

    private static final int TAKEPHOTO = 20;
    private ProgressDialog progress;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.create_group);

        ((ImageView) findViewById(R.id.cancel))
                .setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        finish();
                    }
                });

        ((Button) findViewById(R.id.pickup))
                .setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Intent it = new Intent(CreateGroupActivity.this,
                                PickupActivity.class);
                        startActivityForResult(it, 108);
                    }
                });

        photoView = (ImageView) findViewById(R.id.photo);
        photoView.setScaleType(ImageView.ScaleType.FIT_CENTER);
        photoView.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                onPickPictureOption();
            }
        });

        ((Button) findViewById(R.id.done))
                .setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (sendeeList == null || sendeeList.size() == 0)
                            return;

                        groupName = ((EditText) findViewById(R.id.nickname))
                                .getText().toString();
                        if (groupName != null) {
                            groupName = groupName.trim();
                            boolean chinese = groupName.toLowerCase().equals(
                                    groupName.toUpperCase());

                            if (groupName.length() < (chinese ? 3 : 6)
                                    || groupName.length() > 30) {
                                Intent int2 = new Intent(
                                        getApplicationContext(),
                                        CommonDialog.class);
                                int2.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                                        | Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS
                                        | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                                int2.putExtra("msgContent",
                                        getString(R.string.nickname_invalid));
                                int2.putExtra("numItems", 1);
                                int2.putExtra("ItemCaption0",
                                        getString(R.string.done));
                                int2.putExtra("ItemResult0", -1);
                                startActivity(int2);
                                return;
                            }
                        } else
                            return;

                        progress = ProgressDialog.show(
                                CreateGroupActivity.this, "",
                                getString(R.string.in_progress), true, true);

                        new Thread(registerGroup, "registerGroup").start();
                    }
                });

        mPref = new MyPreference(this);
        largeScreen = (findViewById(R.id.large) != null);
        mDensity = getResources().getDisplayMetrics().density;
    }

    Runnable registerGroup = new Runnable() {
        public void run() {
            sendeeList.remove("0");
            // TODO: 2016/4/8  jack 2.4.51 原来的请求Php没有将创建的creator放在members里面,850是查询整个members群发,所以为了避免,缺少群发群主
            int myIdx = Integer.parseInt(mPref.read("myIdx"));
            sendeeList.addFirst(myIdx + "");
            String Return = "";
            String members = "";
            for (int i = 0; i < sendeeList.size(); i++) {
                String id = sendeeList.get(i);
                if (i > 0)
                    members += ",";
                members += id;
            }

            android.util.Log.d("新建群组", "创建群组的集合包含本身,作为群主.群成员: " + sendeeList);

            try {
                int c = 0;
                do {
                    MyNet net = new MyNet(CreateGroupActivity.this);
                    Return = net.doPostHttps("create_group.php",
                            "id=" + myIdx + "&members=" + members + "&name="
                                    + URLEncoder.encode(groupName, "UTF-8"),
                            null);
                    android.util.Log.d("新建群组", "新建群组的返回值: " + Return);
                    if (Return.startsWith("Done"))
                        break;
                    MyUtil.Sleep(2000);
                } while (++c < 3);
            } catch (Exception e) {
            }

            int groupidx = 0;
            if (Return.startsWith("Done")) {
                Return = Return.substring(5);
                groupidx = Integer.parseInt(Return);
            }

            if (groupidx == 0) {
                if (progress != null && progress.isShowing())
                    progress.dismiss();
                return;
            }

            GroupDB gdb = new GroupDB(CreateGroupActivity.this);
            gdb.open();

            for (int i = 0; i < sendeeList.size(); i++) {
                int idx = Integer.parseInt(sendeeList.get(i));
                // TODO: 2016/4/6  将创建者加入数据库,rank为0代表创建者
                if ((myIdx + "").equals(sendeeList.get(i))) {
                    gdb.insertGroup(groupidx, groupName, idx, 0);
                } else {
                    gdb.insertGroup(groupidx, groupName, idx, 1);
                }
            }
            gdb.close();

            AmpUserDB mADB = new AmpUserDB(CreateGroupActivity.this);
            mADB.open();
            mADB.insertUser("[<GROUP>]" + groupidx, groupidx + 100000000,
                    groupName);

            // TODO: 2016/4/8 上传群组图片,待查
            if (photoAssigned) {
                File f = new File(photoPath);
                String localPath = Global.SdcardPath_inbox + "photo_"
                        + (groupidx + 100000000) + ".jpg";
                File f2 = new File(localPath);
                f.renameTo(f2);

                try {
                    int count = 0;
                    do {
                        //jack 2.4.51
                        MyNet net = new MyNet(CreateGroupActivity.this);
                        Return = net.doPostAttach("uploadgroupphoto.php",
                                groupidx, 0, localPath, AireJupiter.myPhpServer_default2A); // httppost
                        if (Return.startsWith("Done"))
                            break;
                        MyUtil.Sleep(2000);
                    } while (++count < 3);
                } catch (Exception e) {
                }
            }
            ArrayList<String> addressList = new ArrayList<String>();
            try {
                for (int i = 0; i < sendeeList.size(); i++)
                    addressList.add(mADB.getAddressByIdx(Integer
                            .parseInt(sendeeList.get(i))));
            } catch (Exception e) {
            }

            mADB.close();

            if (progress != null && progress.isShowing())
                progress.dismiss();

            setResult(RESULT_OK);
            finish();
        }
    };

    private void onPickPictureOption() {
        final CharSequence[] items = {
                getResources().getString(R.string.photo_gallery),
                getResources().getString(R.string.takepicture)};
        final CharSequence[] items_noCamera = {getResources().getString(
                R.string.photo_gallery)};
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        if (!AmazonKindle.canHandleCameraIntent(this)) {
            builder.setItems(items_noCamera,
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int item) {
                            if (item == 0)
                                onPickPicture();
                            dialog.dismiss();
                        }
                    });
        } else {
            builder.setItems(items, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int item) {
                    if (item == 0)
                        onPickPicture();
                    else if (item == 1)
                        onTakePicture();
                    dialog.dismiss();
                }
            });
        }
        builder.setTitle(this.getResources().getString(
                R.string.choose_photo_source));
        builder.setNegativeButton(R.string.cancel,
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int item) {

                    }
                });
        AlertDialog alert = builder.create();
        alert.show();
    }

    private void onPickPicture() {
        Intent intent = new Intent(Intent.ACTION_PICK);  //li*** fill picture
        intent.setType("image/*");
        if (AmazonKindle.IsKindle()) {
            String title = getResources().getString(
                    R.string.choose_photo_source);
            startActivityForResult(Intent.createChooser(intent, title), 16);
            return;
        }

        intent.putExtra("crop", "true");
        intent.putExtra("aspectX", 1);
        intent.putExtra("aspectY", 1);
        intent.putExtra("outputX", 240);
        intent.putExtra("outputY", 240);
        //li*** fill picture
        intent.putExtra("scale", true);
        intent.putExtra("scaleUpIfNeeded", true);
        intent.putExtra("return-data", true);
        startActivityForResult(intent, 1);
    }

    public String getPath(Uri uri) {
        if (uri.toString().startsWith("content:")) {
            try {
                String[] projection = {MediaStore.Images.Media.DATA};
                Cursor cursor = managedQuery(uri, projection, null, null, null);
                int column_index = cursor
                        .getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
                if (cursor != null) {
                    cursor.moveToFirst();
                    String path = cursor.getString(column_index);
                    return path;
                }
            } catch (Exception e) {
            }
        } else if (uri.toString().startsWith("file:")) {
            String uriStr = uri.toString();
            return uriStr.substring(uriStr.indexOf("sdcard"));
        }
        return "";
    }

    @SuppressLint("ShowToast")
    private void onTakePicture() {
        if (!AmazonKindle.canHandleCameraIntent(this)) {
            Toast.makeText(this, R.string.take_picture_error,
                    Toast.LENGTH_SHORT).show();
            return;
        }
        try {
            if (AmazonKindle.IsKindle()) {
                Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                photoPath = Global.SdcardPath_sent + "tmp.jpg";
                intent.putExtra(MediaStore.EXTRA_OUTPUT,
                        Uri.fromFile(new File(photoPath)));
                startActivityForResult(intent, 8);
            } else {
                Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                photoPath = Global.SdcardPath_sent + "tmp.jpg";
                intent.putExtra(android.provider.MediaStore.EXTRA_OUTPUT,
                        Uri.fromFile(new File(photoPath)));
                startActivityForResult(intent, 20);
            }
        } catch (Exception e) {
            Toast.makeText(this, R.string.take_picture_error,
                    Toast.LENGTH_SHORT).show();
        }
    }

    private void arrangePickedUsers() {
        if (sendeeList == null || sendeeList.size() == 0)
            return;

        RelativeLayout s = (RelativeLayout) findViewById(R.id.picked);
        s.removeAllViews();

        try {

            String path = mPref.read("myPhotoPath", null);
            if (path == null) {
                int uid = Integer.valueOf(mPref.read("myID", "0"), 16);
                path = Global.SdcardPath_sent + "myself_photo_" + uid + ".jpg";
                if (MyUtil.checkSDCard(this) && (new File(path).exists()))
                    mPref.write("myPhotoPath", path);
                else
                    path = null;
            }

            boolean showMyself = false;

            if (path == null || !MyUtil.checkSDCard(this)
                    || !(new File(path).exists())) {
            } else {
                sendeeList.add("0");
                showMyself = true;
            }

            int count = sendeeList.size();
            int width = (int) ((float) s.getWidth() / mDensity);
            if (width < 0) {
                int w = getWindowManager().getDefaultDisplay().getWidth();
                width = (int) ((float) w / mDensity);
            }
            int p = 5;
            int w = 60;
            if (largeScreen) {
                p = 8;
                w = 90;
            }
            int space = width / (count + 1);
            for (int i = 0; i < count; i++) {
                ImageView a = new ImageView(this);
                a.setBackgroundResource(R.drawable.empty);
                a.setPadding((int) (mDensity * p), (int) (mDensity * p),
                        (int) (mDensity * p), (int) (mDensity * p));
                a.setClickable(true);
                int idx = Integer.parseInt(sendeeList.get(i));
                String userphotoPath = Global.SdcardPath_inbox + "photo_" + idx
                        + ".jpg";

                if (showMyself && i == count - 1)
                    userphotoPath = path;
                Drawable photo = ImageUtil.getBitmapAsRoundCorner(
                        userphotoPath, 1, 4);
                if (photo != null)
                    a.setImageDrawable(photo);
                else
                    a.setImageResource(R.drawable.bighead);

                RelativeLayout.LayoutParams lp = new RelativeLayout.LayoutParams(
                        (int) (mDensity * w), (int) (mDensity * w));
                lp.leftMargin = (int) (mDensity * ((count - i) * space - (w / 2)));
                lp.addRule(RelativeLayout.ALIGN_PARENT_TOP);
                s.addView(a, lp);

                if (i < count - 1) {
                    AnimationSet as = new AnimationSet(false);
                    as.setInterpolator(new AccelerateInterpolator());
                    TranslateAnimation ta = new TranslateAnimation(mDensity
                            * -space * (count - i - 1), 0, 0, 0);
                    ta.setDuration(300 + 50 * (count - i - 1));
                    as.addAnimation(ta);
                    as.setDuration(300 + 50 * (count - i - 1));
                    a.startAnimation(as);
                }
            }
        } catch (Exception e) {
        }
    }

    public static Intent getCropImageIntent(Uri photoUri) {
        Intent intent = new Intent("com.android.camera.action.CROP");
        intent.setDataAndType(photoUri, "image/*");
        intent.putExtra("crop", "true");
        intent.putExtra("aspectX", 1);
        intent.putExtra("aspectY", 1);
        intent.putExtra("outputX", 240);
        intent.putExtra("outputY", 240);
        intent.putExtra("return-data", true);
        return intent;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK) {
            if (requestCode == 108) {
                try {
                    sendeeList.clear();
                    String idxArray = data.getStringExtra("idx");
                    String[] items = idxArray.split(" ");
                    for (int i = 0; i < items.length; i++) {
                        int idx = Integer.parseInt(items[i]);
                        if (idx < 50)
                            continue;
                        sendeeList.add(items[i]);
                    }

                    arrangePickedUsers();
                    if (sendeeList.size() > 0)
                        ((Button) findViewById(R.id.done)).setEnabled(true);
                } catch (Exception e) {
                }
            } else if (requestCode == 8) {
                try {
                    String outFilename = Global.SdcardPath_sent
                            + "temp_group.jpg";
                    ResizeImage
                            .ResizeXY(this, photoPath, outFilename, 320, 100); // tml***
                    // bitmap
                    // quality,
                    // 240->320

                    photoPath = outFilename;
                    photoAssigned = true;

                    Drawable photo = ImageUtil.getBitmapAsRoundCorner(
                            outFilename, 3, 10);// alec
                    if (photo != null) {
                        ((TextView) findViewById(R.id.my_photo_hint))
                                .setVisibility(View.GONE);
                        photoView.setImageDrawable(photo);
                        photoView.setScaleType(ImageView.ScaleType.FIT_CENTER);
                    }

                } catch (Exception e) {
                }
            } else if (requestCode == 16) {
                try {
                    Bitmap bitmap = null;
                    try {
                        Uri selectedImageUri = data.getData(); // URI of the
                        // photo
                        bitmap = MediaStore.Images.Media.getBitmap(
                                getContentResolver(), selectedImageUri);
                    } catch (Exception e) {
                    }

                    if (bitmap != null) {
                        String outFilename = Global.SdcardPath_sent
                                + "temp_group.jpg";
                        ResizeImage.ResizeBitmapXY(this, bitmap, outFilename,
                                320, 100); // tml*** bitmap quality, 240->320

                        photoPath = outFilename;

                        Drawable photo = ImageUtil.getBitmapAsRoundCorner(
                                photoPath, 3, 10);// alec
                        if (photo != null) {
                            TextView hint = (TextView) findViewById(R.id.my_photo_hint);
                            hint.setVisibility(View.GONE);
                            photoView.setImageDrawable(photo);
                            photoView
                                    .setScaleType(ImageView.ScaleType.FIT_CENTER);
                        }
                    }
                } catch (Exception e) {
                }
            } else if (requestCode == TAKEPHOTO) {
                boolean HDSize = false;
                try {
                    BitmapFactory.Options bitmapOptions = new BitmapFactory.Options();
                    bitmapOptions.inJustDecodeBounds = true;
                    bitmapOptions.inPurgeable = true;
                    BitmapFactory.decodeFile(photoPath, bitmapOptions);
                    if (bitmapOptions.outHeight > 1000)
                        HDSize = true;
                } catch (Exception e) {
                } catch (OutOfMemoryError e) {
                }
                if (HDSize) {
                    Bitmap bmp = ImageUtil.loadBitmapSafe(2, photoPath);
                    try {
                        uriOrig = Uri.parse(MediaStore.Images.Media
                                .insertImage(getContentResolver(), bmp, null,
                                        null));
                    } catch (Exception e) {
                    }
                } else {
                    try {
                        uriOrig = Uri.parse(MediaStore.Images.Media
                                .insertImage(getContentResolver(), photoPath,
                                        null, null));
                    } catch (Exception e) {
                    } catch (OutOfMemoryError e) {
                    }
                }
                startActivityForResult(getCropImageIntent(uriOrig), 3);
            } else if (requestCode == 7) {
                Drawable photo = ImageUtil.getBitmapAsRoundCorner(photoPath, 3,
                        10);// alec
                if (photo != null) {
                    ((TextView) findViewById(R.id.my_photo_hint))
                            .setVisibility(View.GONE);
                    photoView.setImageDrawable(photo);
                    photoView.setScaleType(ImageView.ScaleType.FIT_CENTER);
                }
            } else if (requestCode == 1 || requestCode == 3) {
                if (data == null)
                    return;
                String SrcImagePath = "";
                try {
                    Uri uri = null;
                    Bitmap bitmap = data.getParcelableExtra("data");
                    uri = Uri.parse(MediaStore.Images.Media.insertImage(
                            getContentResolver(), bitmap, null, null));
                    SrcImagePath = getPath(uri);

                    String outFilename = Global.SdcardPath_sent
                            + "temp_group.jpg";
                    ResizeImage.ResizeXY(this, SrcImagePath, outFilename, 320,
                            100); // tml*** bitmap quality, 240->320

                    photoPath = outFilename;
                    photoAssigned = true;

                    if (uriOrig != null)
                        getContentResolver().delete(uriOrig, null, null);
                    getContentResolver().delete(uri, null, null);

                    if (requestCode == 3)// taken from camera
                    {
                        Intent it = new Intent(CreateGroupActivity.this,
                                PictureRotationActivity.class);
                        it.putExtra("photoPath", outFilename);
                        startActivityForResult(it, 7);
                    } else {
                        Drawable photo = ImageUtil.getBitmapAsRoundCorner(
                                outFilename, 3, 10);// alec
                        if (photo != null) {
                            ((TextView) findViewById(R.id.my_photo_hint))
                                    .setVisibility(View.GONE);
                            photoView.setImageDrawable(photo);
                            photoView
                                    .setScaleType(ImageView.ScaleType.FIT_CENTER);
                        }
                    }

                } catch (Exception e) {
                }
            }
        }
    }
}
