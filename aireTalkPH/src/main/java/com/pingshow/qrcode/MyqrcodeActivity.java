package com.pingshow.qrcode;

import android.app.Activity;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.Base64;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageView;

import com.google.zxing.WriterException;
import com.pingshow.amper.Log;
import com.pingshow.amper.MyPreference;
import com.pingshow.amper.R;
import com.pingshow.util.MCrypt;

public class MyqrcodeActivity extends Activity {
	private MyPreference mPref;
	private ImageView iv;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.san_myqrcode);
		iv = (ImageView) findViewById(R.id.myqrcodeview);
		initData();	
		((ImageView)findViewById(R.id.cancel_san)).setOnClickListener(new OnClickListener()
		{
			@Override
			public void onClick(View v) {
				finish();
			}
		});
	}
	private void initData() {
		mPref = new MyPreference(this);
		if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.FROYO) {
			try {
				String address = mPref.read("myPhoneNumber", "----");
				String nickname = mPref.read("myNickname", "");
				int idx = Integer.parseInt(mPref.read("myID", "0"), 16);
				String qrContent = "AddAireFriend," + address + "," + idx + ","
						+ nickname;
				// li*** encrypt
				Log.d("qr preXN= " + qrContent);
				byte[] bytes = MCrypt.encrypt(qrContent.getBytes());
				qrContent = Base64.encodeToString(bytes, Base64.DEFAULT);
				Log.d("qr postXN= " + qrContent);

				DisplayMetrics displaymetrics = new DisplayMetrics();
				getWindowManager().getDefaultDisplay().getMetrics(
						displaymetrics);
				int width = displaymetrics.widthPixels;
				int height = displaymetrics.heightPixels;
				if (width > height)
					width = height;
				Bitmap qrCodeBitmap = EncodingHandler.createQRCode(qrContent,
						width * 3 / 5);
				// Dialog qrCode = new Dialog(SearchDialog.this);
				// View viewto = View.inflate(SearchDialog.this,
				// R.layout.san_myqrcode, null);
				// ImageView iv = (ImageView)
				// viewto.findViewById(R.id.myqrcodeview);
				// iv.setImageBitmap(qrCodeBitmap);

				// qrCode.setTitle(nickname);
				// qrCode.setCanceledOnTouchOutside(true);
				// qrCode.setContentView(viewto);
				//
				// qrCode.show();
				iv.setImageBitmap(qrCodeBitmap);
			} catch (WriterException e) {
				Log.e("build qr !@#$ " + e.getMessage());
			}
		}
		
		}
	
}
