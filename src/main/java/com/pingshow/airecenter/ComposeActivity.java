package com.pingshow.airecenter;

import java.io.File;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Date;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.AnimationDrawable;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Vibrator;
import android.provider.MediaStore;
import android.provider.MediaStore.Video;
import android.text.Editable;
import android.text.SpannableString;
import android.text.TextWatcher;
import android.text.style.ImageSpan;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.AnimationUtils;
import android.view.animation.TranslateAnimation;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.pingshow.airecenter.R;
import com.pingshow.airecenter.contacts.ContactsQuery;
import com.pingshow.airecenter.db.AmpUserDB;
import com.pingshow.airecenter.db.SmsDB;
import com.pingshow.codec.VoiceMemoPlayer_NB;
import com.pingshow.codec.VoicePlayer2_MP;
import com.pingshow.network.NetInfo;
import com.pingshow.util.ImageUtil;
import com.pingshow.util.MyUtil;
import com.pingshow.util.ResizeImage;


public class ComposeActivity extends Activity implements OnClickListener{

	private String SrcAudioPath;
	private String SrcImagePath;
	private String SrcVideoPath;
	private int mAttached = 0;
	private SendAgent agent;
	private SendFileAgent fileAgent;
	private Handler mHandler = new Handler();
	private SmsDB mDB;
	private String mMsgText;
	private MyPreference mPrf;
	private EditText mInput, text;
	int changing = 0;
	int cursorPos = 0;
	private Vibrator mVibrator;
	private String beforeS = "";
	private String afterS = "";
	private boolean isSmile = false;
	private int myIdx;

	private VoiceMemoPlayer_NB vmp = null;
	private VoiceMemoPlayer_NB mp2 = null;
	private VoicePlayer2_MP myVP1, myVP2 = null;
	float mDensity = 1.f;
	private Button mSend;
	private ImageView mVoice;
	private ImageView speaker;
	private AnimationDrawable spAnimation;
	private boolean state = true;
	private boolean AnimationDrawablestate = true;
	public static boolean fileUploading = false;
	public static long smsId = 0;
	private String myPhoneNumber;
	private long rowid;
	private RelativeLayout messageitem;
	private boolean showitem=false;
	private boolean isVideo = false;
	private Bitmap videobitmap=null;
	private Bitmap picturebitmap = null;
	private int voicetime = 0;
	private Animation fadein,fadeout;
	private AmpUserDB mADB;
	private boolean largeScreen=false;
	
	private ContactsQuery cq;
	private static ArrayList<String> sendeeList = new ArrayList<String>();
	private ArrayList<String> addressList=new ArrayList<String>();
	private ArrayList<String> rowidList=new ArrayList<String>();
	
	long msg_smsid;
	long msg_org_smsid;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.compose2);
		this.overridePendingTransition(R.anim.push_left_in, R.anim.push_left_out);
		
		mVibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
		
		largeScreen=(findViewById(R.id.large)!=null);
		
		mADB = new AmpUserDB(ComposeActivity.this);
		mADB.open();
		mPrf = new MyPreference(this);
		
		mDB = new SmsDB(this);
		mDB.open();
		
		cq=new ContactsQuery(this);
		
		myIdx=Integer.parseInt(mPrf.read("myID","0"), 16);

		((ImageView)findViewById(R.id.voicesms)).setOnClickListener(this);
		((ImageView)findViewById(R.id.picturesms)).setOnClickListener(this);
		((ImageView)findViewById(R.id.photosms)).setOnClickListener(this);
		((ImageView)findViewById(R.id.filesms)).setOnClickListener(this);
		((ImageView)findViewById(R.id.location)).setOnClickListener(this);
		((Button) findViewById(R.id.sendmsg)).setOnClickListener(this);
		((ImageView)findViewById(R.id.attachment)).setOnClickListener(this);
		((ImageView) findViewById(R.id.add)).setOnClickListener(this);
		
		messageitem = (RelativeLayout)findViewById(R.id.messageitem);
		speaker = (ImageView) findViewById(R.id.speaker);
		spAnimation = (AnimationDrawable) speaker.getDrawable();
		
		fadein = AnimationUtils.loadAnimation(this, R.anim.push_up_in_fast);
	 	fadeout = AnimationUtils.loadAnimation(this, R.anim.fadeout);

		myPhoneNumber = mPrf.read("myPhoneNumber");
		
		((ImageView)findViewById(R.id.cancel)).setOnClickListener(this);

		((ImageView) findViewById(R.id.smile)).setOnClickListener(this);
		
		mSend = (Button) findViewById(R.id.sendmsg);
		mVoice = (ImageView) findViewById(R.id.voicesms);

		mDensity = getResources().getDisplayMetrics().density;

		ImageView iv = (ImageView) findViewById(R.id.voice);
		iv.setOnClickListener(this);
		ImageView deleteiv = (ImageView) findViewById(R.id.deletefile);
		deleteiv.setOnClickListener(this);
		mInput = (EditText) findViewById(R.id.msginput);
		mInput.addTextChangedListener(new TextWatcher() {
			@Override
			public void afterTextChanged(Editable s) {
				if (s.toString().length()>0)
				{
					if (mSend.getVisibility()==View.INVISIBLE)
					{
						mSend.setVisibility(View.VISIBLE);
						mVoice.setVisibility(View.INVISIBLE);
					}
				}
				else{
					mSend.setVisibility(View.INVISIBLE);
					mVoice.setVisibility(View.VISIBLE);
				}
				if (!isSmile)
					return;
				Smiley sm = new Smiley();

				if (sm.hasSmileys(s.toString()) > 0) {
					SpannableString spannable = new SpannableString(s
							.toString());
					for (int i = 0; i < Smiley.MAXSIZE; i++) {
						for (int j = 0; j < sm.getCount(i); j++) {
							if (i>=75)//gif
							{
								ImageSpan icon = new ImageSpan(
										ComposeActivity.this, R.drawable.em001 + i - 75, ImageSpan.ALIGN_BOTTOM);
								spannable.setSpan(icon, sm.getStart(i, j),
										sm.getEnd(i, j),
										SpannableString.SPAN_EXCLUSIVE_EXCLUSIVE);
							}
							else{
								ImageSpan icon = new ImageSpan(
										ComposeActivity.this, R.drawable.sm01 + i, ImageSpan.ALIGN_BOTTOM);
								spannable.setSpan(icon, sm.getStart(i, j),
										sm.getEnd(i, j),
										SpannableString.SPAN_EXCLUSIVE_EXCLUSIVE);
							}
						}
					}
					mInput.setText(spannable);
					mInput.setSelection(cursorPos);
				}
			}

			@Override
			public void onTextChanged(CharSequence s, int start, int before,
					int count) {
				afterS = s.toString();
				Smiley sm = new Smiley();
				if (!afterS.equals(beforeS)
						&& sm.hasSmileys(afterS.substring(start, start + count)) > 0) {
					isSmile = true;
				} else {
					isSmile = false;
				}
				afterS = "";
				beforeS = "";
			}

			@Override
			public void beforeTextChanged(CharSequence s, int start, int count,
					int after) {
				beforeS = s.toString();
				cursorPos = mInput.getSelectionStart();
				state = false;
			}
		});
		
		arrangePickedUsers();
		
		MyPreference Pref=new MyPreference(this);
		int c=Pref.readInt("composeTip",0);
		if (c<2)
		{
			mHandler.postDelayed(showTooltip, 250);
    		Pref.write("composeTip",++c);
		}
	}
	
	Runnable showTooltip=new Runnable(){
    	public void run()
    	{
    		Intent it=new Intent(ComposeActivity.this,Tooltip.class);
            it.putExtra("Content", getString(R.string.help_multiple_send));
            startActivity(it);
    	}
    };

	@Override
	protected void onDestroy() {
		try {
			if (vmp != null) {
				vmp.stop();
				vmp = null;
			}
			if (myVP1 != null) {  //tml*** new vmsg
				myVP1.stop();
				myVP1 = null;
			}
		} catch (Exception e) {
			vmp = null;
			myVP1 = null;
		}
		if (mDB != null && mDB.isOpen())
			mDB.close();
		if (mADB!=null && mADB.isOpen()) 
			mADB.close();
		
		spAnimation=null;
		
		String draft = mInput.getText().toString().trim();
		if (draft.length() != 0 && !draft.equals(R.string.textinput)) {
			mPrf.write("multiple_draft", draft);
		} else if (draft.length() == 0) {
			mPrf.delect("multiple_draft");
		}
		
		if (null!=SrcAudioPath) {
			File file = new File(SrcAudioPath);
			if (file.exists())
				file.delete();
		}
		if(picturebitmap!=null)
			picturebitmap.recycle();
		
		super.onDestroy();
		System.gc();
		System.gc();
	}

	@Override
	protected void onResume() {
		super.onResume();
		if (mPrf.read("multiple_draft")!=null && state) {
			String draft1 = mPrf.read("multiple_draft");
			mInput.setText(draft1);
			mInput.setSelection(draft1.length());
		}
//		MobclickAgent.onResume(this);
	}

	@Override
	protected void onPause() {
		mHandler.removeCallbacks(showTooltip);
		stopPlayingVoice();
//		MobclickAgent.onPause(this);
		super.onPause();
	}

	final int action_nothing=0;
	final int action_resend=1;
	final int action_save_as=2;
	int item2_action;

	public String getPath(Uri uri) {
		if (uri.toString().startsWith("content:")) {
			Cursor cursor = managedQuery(uri, new String[] { MediaStore.Images.Media.DATA }, null, null, null);
			if (cursor!=null)
			{
				int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
				cursor.moveToFirst();
				String path = cursor.getString(column_index);
				//cursor.close();
				return path;
			}
		} else if (uri.toString().startsWith("file:")) {
			String uriStr = uri.toString();
			return uriStr.substring(uriStr.indexOf("sdcard"));
		}
		return "";
	}
	
	public static boolean videoRecording  = false;
	private void onVideomemo() {
		if (mAttached != 8 && mAttached != 0) {
			Toast.makeText(this, getString(R.string.fileandvideosingle),
					Toast.LENGTH_SHORT).show();
			return;
		}
		isVideo = true;

		Intent mIntent = new Intent(MediaStore.ACTION_VIDEO_CAPTURE);  
		mIntent.putExtra(MediaStore.EXTRA_DURATION_LIMIT, 30);
	    startActivityForResult(mIntent, 30);
	}

	private void onFileTransfer() {
		if (mAttached != 8 && mAttached != 0) {
			Toast.makeText(this, getString(R.string.fileandvideosingle), Toast.LENGTH_SHORT).show();
			return;
		}
		isVideo = false;
		if (MyUtil.checkSDCard(getApplicationContext())) {
			startActivityForResult(new Intent(this, FileBrowerActivity.class),
					20);
		} else {
			Toast.makeText(this, getString(R.string.no_sdcard),
					Toast.LENGTH_SHORT).show();
		}
	}

	private Uri outputFileUri;

	private void onTakePicture() {
		if (mAttached == 8) {
			Toast.makeText(this, getString(R.string.fileandvideosingle),
					Toast.LENGTH_SHORT).show();
			return;
		}
		try{
			Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
			File file = new File(Global.SdcardPath_sent + "tmp.jpg");
			outputFileUri = Uri.fromFile(file);
			intent.putExtra(MediaStore.EXTRA_OUTPUT, outputFileUri);
			startActivityForResult(intent, 3);
			mPrf.write("vociemessaging", true);// take photo not popupDialog
		}catch(Exception e){
			Toast.makeText(this, R.string.take_picture_error, Toast.LENGTH_SHORT);
		}
	}

	private void onPickPicture() {
		if (mAttached == 8) {
			Toast.makeText(this, getString(R.string.fileandvideosingle),
					Toast.LENGTH_SHORT).show();
			return;
		}
		Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
		intent.setType("image/*");
		String title = getResources().getString(R.string.choose_photo_source);
		startActivityForResult(Intent.createChooser(intent, title), 1);
	}

	public static String getRandomName() {
		return ("" + new Date().getTime());
	}
	
	private void arrangePickedUsers()
	{
		if (sendeeList==null || sendeeList.size()==0) return;
		
		RelativeLayout s=(RelativeLayout)findViewById(R.id.picked);
		s.removeAllViews();
		
		try{
			int count=sendeeList.size();
			int width=(int)((float)s.getWidth()/mDensity)-(largeScreen?120:90);
			if (width<0) {
				int w=getWindowManager().getDefaultDisplay().getWidth();
				width = (int)((float)w/mDensity)-95-((TextView)findViewById(R.id.sendto)).getWidth()-(largeScreen?90:60);
			}
			int space=width/count;
			if (largeScreen)
			{
				if (space>110) space=110;
			}else{
				if (space>65) space=65;
			}
			
			for(int i=0;i<count;i++)
			{
				ImageView a=new ImageView(this);
				a.setBackgroundResource(R.drawable.empty);
				if (largeScreen)
					a.setPadding((int)(mDensity*8), (int)(mDensity*8), (int)(mDensity*8), (int)(mDensity*8));
				else
					a.setPadding((int)(mDensity*5), (int)(mDensity*5), (int)(mDensity*5), (int)(mDensity*5));
				a.setClickable(true);
				int idx=Integer.parseInt(sendeeList.get(i));
				String userphotoPath = Global.SdcardPath_inbox + "photo_" + idx + ".jpg";
				Drawable photo=ImageUtil.getBitmapAsRoundCorner(userphotoPath,1,4);
				if (photo!=null)
					a.setImageDrawable(photo);
				else
					a.setImageResource(R.drawable.bighead);
				
				RelativeLayout.LayoutParams lp=null;
				if (largeScreen)
					lp = new RelativeLayout.LayoutParams((int)(mDensity*90), (int)(mDensity*90));
				else
					lp = new RelativeLayout.LayoutParams((int)(mDensity*60), (int)(mDensity*60));
				lp.leftMargin=(int)(mDensity*space)*(count-i-1);
				lp.addRule(RelativeLayout.ALIGN_PARENT_TOP);
				s.addView(a, lp);
				
				if (i<count-1)
				{
					AnimationSet as = new AnimationSet(false);
				    as.setInterpolator(new AccelerateInterpolator());
					TranslateAnimation ta = new TranslateAnimation(mDensity*-space*(count-i-1),0,0,0);
					ta.setDuration(300+50*(count-i-1));
					as.addAnimation(ta);
					as.setDuration(300+50*(count-i-1));
					a.startAnimation(as);
				}
			}
		}catch(Exception e){}
	}
	
	public static void deleteUserInList(int idx)
	{
		if (sendeeList==null) return;
		try{
			for(int i=0;i<sendeeList.size();i++)
			{
				if (idx==Integer.parseInt(sendeeList.get(i)))
				{
					sendeeList.remove(i);
					return;
				}
			}
		}catch(Exception e){}
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == 3) // take photo not popupDialog
			mPrf.write("vociemessaging", false);
		
		if (requestCode == 108) {
			if (resultCode == RESULT_OK) {
				try{
					sendeeList.clear();
					String idxArray=data.getStringExtra("idx");
					String [] items=idxArray.split(" ");
					for(int i=0;i<items.length;i++)
						sendeeList.add(items[i]);
					arrangePickedUsers();
				}catch(Exception e){}
			}
		}
		else if (requestCode == 7) {
			if (resultCode == RESULT_OK) {
				mAttached = 0;
				long lon = mPrf.readLong("longitude", Global.DEFAULT_LON);
				long lat = mPrf.readLong("latitude", Global.DEFAULT_LAT);
				mMsgText = "here I am ("+((float)lat/1000000.f)+","+((float)lon/1000000.f)+")";
				
				addressList.clear();
				rowidList.clear();
				try{
					for (int i=0;i<sendeeList.size();i++)
						addressList.add(mADB.getAddressByIdx(Integer.parseInt(sendeeList.get(i))));
				}catch(Exception e){}
				
				agent=new SendAgent(ComposeActivity.this, myIdx, 0, true);
				
				if (agent.onMultipleSend(addressList, mMsgText, mAttached,
						SrcAudioPath, SrcImagePath))
				{
					addMsgToDatabase(false);
					playSoundTouch();
				}
			}
		}else if (resultCode == RESULT_OK) {
			if (requestCode == 1 || requestCode == 3) {
				if (requestCode == 1) {
					if (null==data.getData()) return;
					Uri selectedImageUri = data.getData();
					SrcImagePath = getPath(selectedImageUri);
				}
				else if (requestCode == 3)
					SrcImagePath = Global.SdcardPath_sent + "tmp.jpg";

				mAttached |= 2;// image
				String filename = Global.SdcardPath_sent + getRandomName() + ".jpg";

				NetInfo ni = new NetInfo(this);
				int result = 0;
				if (ni.netType >= NetInfo.MOBILE_3G)
					result = ResizeImage.Resize(this, SrcImagePath, filename,
							640, 640, 90);
				else
					result = ResizeImage.Resize(this, SrcImagePath, filename,
							360, 360, 60);
				if (result == -1) {
					Toast.makeText(this, R.string.img_error, Toast.LENGTH_SHORT)
							.show();
					return;
				}
				SrcImagePath = filename;

				ShowAttchment(0);
			}
			else if (requestCode == 200) // back from smile activity
			{
				int index = data.getIntExtra("index", 0);
				if(index>=75){ //gif
					mMsgText = (String)SmileyActivity.smiles[index][0];
					addressList.clear();
					rowidList.clear();
					try{
						for (int i=0;i<sendeeList.size();i++)
							addressList.add(mADB.getAddressByIdx(Integer.parseInt(sendeeList.get(i))));
					}catch(Exception e){}
					
					agent=new SendAgent(ComposeActivity.this, myIdx, 0, true);
					
					if (agent.onMultipleSend(addressList, mMsgText, 0, null, null))
					{
						addMsgToDatabase(false);
						playSoundTouch();
					}
					SrcAudioPath = null;
				}else{
					EditText msginput = (EditText) findViewById(R.id.msginput);
					int indexCursor = msginput.getSelectionStart();
					msginput.getText().insert(indexCursor,
							String.valueOf(SmileyActivity.smiles[index][0]));
				}
			} else if (requestCode == 15) {
				mAttached |= 1;
				SrcAudioPath = data.getStringExtra("path");
				voicetime = 60-data.getIntExtra("voicetime", 60);
				ShowAttchment(0);
			}
			else if (requestCode == 20) { // show file attach icon
				SrcVideoPath = data.getStringExtra("filePath");
				mAttached = 8;
				SrcAudioPath = SrcVideoPath;
				ShowAttchment(1);
			} else if (requestCode == 30) { // show video attach icon
				videoRecording = false;
				mAttached = 8;
				Uri selectedImageUri = data.getData();
				SrcVideoPath = getPath(selectedImageUri).toString();
				SrcAudioPath = SrcVideoPath;
				ShowAttchment(0);
			}
		} else if (resultCode == RESULT_CANCELED && requestCode == 40) {
			mSend.setEnabled(true);
			if (null!=SrcVideoPath){
				(new File(SrcVideoPath)).delete();
			}
			SrcVideoPath = null;
			mAttached = 0;
			ShowAttchment(0);
			if (mVoice.getVisibility()==View.INVISIBLE)
			{
				mVoice.setVisibility(View.VISIBLE);
				mSend.setVisibility(View.INVISIBLE);
			}
		}else if (resultCode != RESULT_OK && requestCode == 30) {// record
			if (null!=SrcAudioPath){
				try{
					(new File(SrcAudioPath)).delete();
				}catch(Exception e){}
			}
			SrcAudioPath = null;
			mAttached = 0;
			videoRecording = false;
			ShowAttchment(0);
		}else if (resultCode == RESULT_CANCELED && requestCode == 15) {// voice memo cancelled
			if (null!=SrcAudioPath){
				try{
					(new File(SrcAudioPath)).delete();
				}catch(Exception e){}
			}
			SrcAudioPath = null;
			mAttached = 0;
		}
	}

	private void showVideoBitmap(ImageView imageView) {
		if (SrcVideoPath == null)
			return;
		
		if(Integer.parseInt(Build.VERSION.SDK) >= 8){
			videobitmap = ThumbnailUtils.createVideoThumbnail(new File(
					SrcVideoPath).getAbsolutePath(), Video.Thumbnails.MICRO_KIND);
		}
		if (videobitmap == null)
			videobitmap = BitmapFactory.decodeResource(getResources(), R.drawable.sm70);
		if (Integer.parseInt(Build.VERSION.SDK) >= 8){
			Bitmap bubbleblue = BitmapFactory.decodeResource(ComposeActivity.this.getResources(),
					R.drawable.videosms_play);
			Drawable[] array = new Drawable[2];
			array[1] = new BitmapDrawable(bubbleblue);
			array[0] = new BitmapDrawable(videobitmap);
			LayerDrawable layers= new LayerDrawable(array);
			layers.setLayerInset(0, 0, 0, 0, 0);
			layers.setLayerInset(1, 0, 0, 0, 0);
			layers.setBounds(0, 0, (int)(66.f*mDensity), (int)(66.f*mDensity));
		
			imageView.setImageDrawable(layers);
		}
		else
			imageView.setImageResource(R.drawable.start_play);
		videobitmap=null;
		
	}
	
	private void ShowAttchment(int fileType) {
		String piclength= "0";
		String filelengh = "0";
		String voicelength = "0";
		FrameLayout f = (FrameLayout) findViewById(R.id.attachedframe);
		ImageView ivp = (ImageView) findViewById(R.id.picture);
		ImageView ivv = (ImageView) findViewById(R.id.voice);
		ImageView ivd = (ImageView) findViewById(R.id.video);
		ImageView ivf = (ImageView) findViewById(R.id.file);
		ImageView ivdelete = (ImageView) findViewById(R.id.deletefile);
		TextView smsinfo=(TextView)findViewById(R.id.smsinfo);
		smsinfo.setVisibility(View.GONE);
		ivdelete.setVisibility(View.GONE);
		if (mAttached != 0) {
			if ((mAttached & 2) == 2) {
				File file = new File(SrcImagePath);
				NumberFormat format = DecimalFormat.getInstance();
				format.setMaximumFractionDigits(2);
				piclength = format.format(file.length() / 1024.0).replace(",", "");
				Drawable img=null;
				try{
					img=Drawable.createFromPath(SrcImagePath);
				}catch(OutOfMemoryError e){
					System.gc();
					System.gc();
				}
				if (img!=null)
				{
					ivp.setImageDrawable(img);
					ivp.setVisibility(View.VISIBLE);
				}
			} else
				ivp.setVisibility(View.GONE);
			if ((mAttached & 1) == 1){
				File file = new File(SrcAudioPath);
				NumberFormat format = DecimalFormat.getInstance();
				format.setMaximumFractionDigits(2);
				voicelength = format.format(file.length() / 1024.0).replace(",", "");
				ivv.setVisibility(View.VISIBLE);
			}
			else{
				ivv.setVisibility(View.GONE);
			}
			if (mAttached == 8) {
				File file = new File(SrcAudioPath);
				NumberFormat format = DecimalFormat.getInstance();
				format.setMaximumFractionDigits(2);
				filelengh = format.format(file.length() / 1024.0).replace(",", "");
				if (fileType == 0) {// video
					ivd.setVisibility(View.VISIBLE);
					ivf.setVisibility(View.GONE);
				} else {
					ivd.setVisibility(View.GONE);
					ivf.setVisibility(View.VISIBLE);
				}
				
				showVideoBitmap(ivd);
			} else {
				ivd.setVisibility(View.GONE);
				ivf.setVisibility(View.GONE);
			}
			if (fileType == -1) {
				f.setVisibility(View.GONE);
			} else {
				f.setVisibility(View.VISIBLE);
			}
			smsinfo.setVisibility(View.VISIBLE);
			float piclen = Float.parseFloat(piclength);
			float voicelen = Float.parseFloat(voicelength);
			float filelen = Float.parseFloat(filelengh);
			smsinfo.setText(filelen+voicelen+piclen+ " KB");
			ivdelete.setVisibility(View.VISIBLE);
		} else
			f.setVisibility(View.GONE);
		
		mSend.setVisibility(View.VISIBLE);
		mVoice.setVisibility(View.INVISIBLE);
	}

	private void addMsgToDatabase(boolean isFile) {
		SMS msg = new SMS();
		long now=new Date().getTime();
		
		for (int i=0;i<addressList.size();i++)
		{
			String address=addressList.get(i);
			String nickname="";
			long contactId=cq.getContactIdByNumber(address);
			if (contactId>0)
				nickname=cq.getNameByContactId(contactId);
			else
				nickname=mADB.getNicknameByAddress(address);
			
			msg.displayname = nickname;
			msg.address = address;
			msg.content = mMsgText;
			msg.contactid = contactId;
			msg.read = 1;
			msg.type = 2;
			msg.status = SMS.STATUS_PENING;
			msg.time = now;
			msg.attached = mAttached;
			if ((mAttached & 1) == 1)
				msg.att_path_aud = SrcAudioPath;
			if ((mAttached & 2) == 2)
				msg.att_path_img = SrcImagePath;
			if (mAttached == 8) {
				msg.att_path_aud = SrcAudioPath;
				if (msg.content.startsWith("(fl)")) {
					msg.content = getString(R.string.filememo_send) + " " + msg.content;
				} else {
					msg.content = getString(R.string.video) + " " + msg.content;
					msg.attached = 9;
				}
			}
	
			msg.longitudeE6 = mPrf.readLong("longitude", Global.DEFAULT_LON);
			msg.latitudeE6 = mPrf.readLong("latitude", Global.DEFAULT_LAT);
			
			long rowid = mDB.insertMessage(msg.address, msg.contactid,
					(new Date()).getTime(), 1, msg.status, msg.type, "",
					msg.content, msg.attached, msg.att_path_aud, msg.att_path_img,
					0, msg.longitudeE6, msg.latitudeE6, 0, msg.displayname, null, 0);
			
			mADB.updateLastContactTimeByAddress(msg.address, new Date().getTime());
			
			rowidList.add(rowid+"");
		}
		
		if (isFile) {
			fileAgent.setRowId(rowidList);
			mAttached = 0;
		} else {
			agent.setRowId(rowidList);
			mAttached = 0;
		}

		text = (EditText) findViewById(R.id.msginput);
		text.setText("");

		mHandler.postDelayed(new Runnable() {
			public void run() {
				Toast.makeText(ComposeActivity.this, R.string.msg_is_sent, Toast.LENGTH_SHORT).show();
				finish();
			}
		}, 1500);
	}

	@Override
	public void onBackPressed() {
		if (AnimationDrawablestate) {
			super.onBackPressed();
		} else {
			stopPlayingVoice();
		}
	}

	private void playSoundTouch() {
		if (mPrf.readBoolean("sendVibrator", true)) {
			long[] patern = { 0, 40, 1000 };
			mVibrator.vibrate(patern, -1);
		}
	}

	public void stopPlayingVoice() {
		try {
			if (vmp != null) {
				vmp.stop();
				vmp = null;
				spAnimation.stop();
				speaker.setVisibility(View.GONE);
				AnimationDrawablestate = true;
			}
			if (myVP1 != null) {  //tml*** new vmsg
				myVP1.stop();
				myVP1 = null;
				spAnimation.stop();
				speaker.setVisibility(View.GONE);
				AnimationDrawablestate = true;
			}
		} catch (Exception e) {
		}
	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.add:
			{
				Intent it=new Intent(ComposeActivity.this, PickupActivity.class);
				startActivityForResult(it, 108);
			}
			break;
		case R.id.attachment:
			if (mAttached!=0) return;
			if(!showitem){
				messageitem.startAnimation(fadein);
				messageitem.setVisibility(View.VISIBLE);
				showitem=true;
			}else{
				messageitem.startAnimation(fadeout);
				mHandler.postDelayed(new Runnable() {
					@Override
				    public void run() {
						messageitem.setVisibility(View.GONE);
					}
				},350);
				showitem=false;
			}
			break;
		case R.id.voicesms:
			if(!MyUtil.checkSDCard(getApplicationContext())){
				Toast.makeText(this, getString(R.string.no_sdcard),
						Toast.LENGTH_SHORT).show();
				return;
			}
			messageitem.setVisibility(View.GONE);
			showitem=false;
			if (mAttached == 8) {
				Toast.makeText(ComposeActivity.this,
						getString(R.string.fileandvideosingle),
						Toast.LENGTH_SHORT).show();
				return;
			}
			SrcAudioPath = Global.SdcardPath_sent + getRandomName() + ".amr";
			Intent it = new Intent(ComposeActivity.this,
					VoiceRecordingDialog.class);
			it.putExtra("path", SrcAudioPath);
			startActivityForResult(it, 15);
			break;
		case R.id.picturesms:
			messageitem.setVisibility(View.GONE);
			showitem=false;
			onPickPicture();
			break;
		case R.id.photosms:
			messageitem.setVisibility(View.GONE);
			showitem=false;
			onTakePicture();
			break;
		case R.id.filesms:
			messageitem.setVisibility(View.GONE);
			showitem=false;
			onFileTransfer();
			break;
		case R.id.location:
			messageitem.setVisibility(View.GONE);
			showitem=false;
			it = new Intent(ComposeActivity.this,
					CommonDialog.class);
			it.putExtra("msgContent", getString(R.string.multi_send_location));
			it.putExtra("numItems", 2);
			it.putExtra("ItemCaption0", getString(R.string.cancel));
			it.putExtra("ItemResult0", RESULT_CANCELED);
			it.putExtra("ItemCaption1", getString(R.string.yes));
			it.putExtra("ItemResult1", RESULT_OK);
			startActivityForResult(it, 7);
			break;
		case R.id.smile:
			startActivityForResult(new Intent(ComposeActivity.this,SmileyActivity.class), 200);
			break;
		case R.id.sendmsg:
			v.setEnabled(false);
			if (sendeeList.size()==0)
			{
				v.setEnabled(true);
				return;
			}
			mMsgText = ((EditText) findViewById(R.id.msginput)).getText().toString();
			mPrf.delect("multiple_draft");
			int len = mMsgText.length();
			if (mAttached == 3)
				mMsgText = "(Vm)(iMG)" + (len == 0 ? "" : ("\n" + mMsgText));
			else if ((mAttached & 1) == 1)
				mMsgText = "(Vm)" + (len == 0 ? ""+voicetime : ("\n" + mMsgText));
			else if ((mAttached & 2) == 2)
				mMsgText = "(iMG)" + (len == 0 ? "" : ("\n" + mMsgText));
			else if (mAttached == 8) {
				if (ComposeActivity.fileUploading) {
					Toast.makeText(getApplicationContext(),
							getString(R.string.fileuploading),
							Toast.LENGTH_SHORT).show();
					v.setEnabled(true);
					mVoice.setVisibility(View.VISIBLE);
					mSend.setVisibility(View.INVISIBLE);
					return;
				}
				File file = new File(SrcAudioPath);
				NumberFormat format = DecimalFormat.getInstance();
				format.setMaximumFractionDigits(2);
				String length = format.format(file.length() / 1024.0).replace(",", "");
				if (Double.valueOf(length) > 30960) { // 30M
					Toast.makeText(getApplicationContext(),
							getString(R.string.fileLarge), Toast.LENGTH_SHORT)
							.show();
					v.setEnabled(true);
					mVoice.setVisibility(View.VISIBLE);
					mSend.setVisibility(View.INVISIBLE);
					return;
				}
				if (isVideo)
					mMsgText = "(vdo)" + length
							+ (len == 0 ? " KB" : (" KB\n" + mMsgText));
				else
					mMsgText = "(fl)" + length
							+ (len == 0 ? " KB" : (" KB\n" + mMsgText));
			}
			
	    	addressList.clear();
			rowidList.clear();
			try{
				for (int i=0;i<sendeeList.size();i++)
					addressList.add(mADB.getAddressByIdx(Integer.parseInt(sendeeList.get(i))));
			}catch(Exception e){}

			if (mAttached == 8) {
				if(videobitmap!=null)
					videobitmap.recycle();
				NetInfo myNet = new NetInfo(this);
				if (myNet.netType == NetInfo.MOBILE_OTHER){
					Toast.makeText(getApplicationContext(),getApplicationContext().getResources().getString(R.string.file_othernet), Toast.LENGTH_SHORT).show();
					SrcAudioPath = null;
					SrcVideoPath = null;
					mAttached = 0;
					mSend.setEnabled(true);
					ShowAttchment(0);
					mVoice.setVisibility(View.VISIBLE);
					mSend.setVisibility(View.INVISIBLE);
					return;
				}
				fileAgent = new SendFileAgent(this, myIdx, true);
				
				if (!fileAgent.onMultipleSend(addressList, mMsgText, mAttached,
						SrcAudioPath, SrcImagePath)) {
					mSend.setEnabled(true);
				} else {
					addMsgToDatabase(true);
					playSoundTouch();
				}
				SrcAudioPath = null;
			} else {
				agent=new SendAgent(ComposeActivity.this, myIdx, 0, true);
				
				if (!agent.onMultipleSend(addressList, mMsgText, mAttached,
						SrcAudioPath, SrcImagePath))
					v.setEnabled(true);
				else {
					addMsgToDatabase(false);
					playSoundTouch();
				}

				SrcAudioPath = null;
			}
			break;
		case R.id.cancel:
			finish();
			break;
		case R.id.voice:
			if (mp2 != null && mp2.isPlaying())
				return;
			if (myVP2 != null && myVP2.isPlaying())
				return;
			if (vmp != null)
				return;
			if (myVP1 != null)
				return;
			if (SrcAudioPath != null &&  (new File(SrcAudioPath).length()>0)) {
				try {
					if (SrcAudioPath.endsWith("amr")) {
						mp2 = new VoiceMemoPlayer_NB(ComposeActivity.this);
						mp2.setDataSource(SrcAudioPath);
						mp2.prepare();
						mp2.start();
					} else {
						//tml*** new vmsg
						myVP1 = new VoicePlayer2_MP(ComposeActivity.this, SrcAudioPath);
						myVP1.start();
					}
					speaker.setVisibility(View.VISIBLE);
					spAnimation.start();
					AnimationDrawablestate = false;
				} catch (IOException e) {
					Log.e("ca1 " + e.getMessage());
				} catch (IllegalArgumentException e) {
				} catch (IllegalStateException e) {
				}
			}
			break;
		case R.id.deletefile:
			SrcVideoPath = null;
			SrcImagePath=null;
			SrcAudioPath = null;
			mAttached = 0;
			ShowAttchment(0);
			mSend.setVisibility(View.INVISIBLE);
			mVoice.setVisibility(View.VISIBLE);
			break;
		}
	}
}
