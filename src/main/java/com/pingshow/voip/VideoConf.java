package com.pingshow.voip;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import android.app.Activity;
import android.content.Intent;
import android.hardware.Camera;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.pingshow.airecenter.AireJupiter;
import com.pingshow.airecenter.Log;
import com.pingshow.airecenter.MyPreference;
import com.pingshow.airecenter.R;
import com.pingshow.video.capture.AndroidVideoApi9JniWrapper;
import com.pingshow.video.capture.hwconf.AndroidCameraConfiguration;

public class VideoConf {
	public static boolean EN_VC = false;

	public static int maxDisplays = 3;
	
//	private static int _width=640;
//	private static int _height=480;
	private static int _width=640;
	private static int _height=480;
	private static final int _fps=15;
	
	private ArrayList<byte[]> mOutputQueue=new ArrayList<byte[]>();
	public List<byte[]> [] mInputQueue;
	private Activity mDialer=null;
	
	private boolean Running=false;
	private SurfaceView mSurface;
	private MyPreference mPref;
	private boolean isRecording;
	
	private static VideoConf instance=null;
	private P2P p2p;
	private int numOfStreams=0;
	private int frames=0;
	private int last_frameCount=0;
	
	public VideoConf(Activity dialer, MyPreference pref)
	{
//		mDialer=dialer;
//		mPref=pref;
//		
//		mSurface=(SurfaceView)mDialer.findViewById(R.id.preview_1);
//		
//		new Thread(new Runnable(){
//			public void run()
//			{
//				startSocket();
//				mHandler.postDelayed(checkLayout, 3000);
//			}
//		}).start();
//		
//		instance=this;
	}
	
	public void start()
	{
//		Log.e("tmlvc startingCamera");
//		mHandler.post(startCamera);
	}
	
	private boolean startSocket()
	{
//		Log.e("tmlvc startingSocket");
//		p2p=new P2P();
//		int myIdx=0;
//		try{
//			myIdx=Integer.parseInt(mPref.read("myID","0"),16);
//		}catch(Exception e){}
//		
//		Log.e("tmlvc startSocket p2p.init=" + _width + "x" + _height);
//		p2p.init(""+myIdx, _width, _height);
		return true;
	}
	
	void disconnect()
	{
//		if (p2p!=null)
//			p2p.release();
//		p2p=null;
	}
	
	private Camera cam=null;
	Runnable startCamera=new Runnable()
	{
		public void run()
		{
//			if (cam==null)
//			{
//				if (memberList==null) return;
//				if (memberList.size()==0) return;
//				
////				((RelativeLayout)mDialer.findViewById(R.id.video_preview)).setVisibility(View.VISIBLE);
//				RelativeLayout preview = (RelativeLayout)mDialer.findViewById(R.id.video_preview);
//				preview.setVisibility(View.VISIBLE);
//				
//				//tml*** vc resize self
////				preview.post(new Runnable() {
////					@Override
////					public void run() {
////						LinearLayout view = (LinearLayout) mDialer.findViewById(R.id.topVWin_holder);
////						int selfH = view.getHeight();
////						int selfW = (int) ((double) selfH * (double) 1.33);
////						Log.e("tmlvc startCamera preview selfWH=" + selfW + "x" + selfH);
////						setSizePreview(view, selfW, selfH);
////					}
////				});
//				
//				Log.d("tmlvc startCamera....");
//				
//				int cameraId=1;
//				if(AndroidCameraConfiguration.retrieveCameras().length>0)
//					cameraId=cameraId%AndroidCameraConfiguration.retrieveCameras().length;
//				else
//					cameraId=0;
//				
//				int r=0;
//				String Brand=Build.BRAND.toLowerCase();
//				String Model=Build.MODEL.toLowerCase();
//				//camera rotation per device
//				if (Model.contains("k200") || Model.contains("s82"))//s802
//				{
//					r=0; //S802
//				}else if (Model.contains("a9"))//a9
//					r=90;
//				else if (Brand.contains("amazon"))//kindle
//					r=90;
//				
//				mSurface.getHolder().setKeepScreenOn(true);
//				mSurface.getHolder().setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
//				
//				AndroidVideoApi9JniWrapper.vc=VideoConf.this;
//				cam=(Camera) AndroidVideoApi9JniWrapper.startRecording(cameraId, _width, _height, _fps, r, 0);
//				
//				isRecording=true;
//				
//				arrangeDisplay();
//				
//				if (cam!=null)
//				{
//					try {
//						cam.setPreviewDisplay(mSurface.getHolder());
//					} catch (Exception exc) {
//						exc.printStackTrace();
//					} catch (IncompatibleClassChangeError e){
//					}
//				}
//			}
//			
//			if (isRecording)
//			{
//				new Thread(generateDSPThread).start();
//			}
		}
	};
	
	private List<Integer> p2pSession=new ArrayList<Integer>();
	public void addP2PMember(int idx)
	{
//		p2pSession.add(idx);
//		int index = p2pSession.indexOf(idx);
//		Log.e("tmlvc 510 p2pSession.ADD [" + index + "] = " + idx);
	}
	
	long timelast = 0;
	Runnable generateDSPThread=new Runnable()
	{
		public void run()
		{
//			Log.e("tmlvc BEGIN generateDSPThread");
//			for (int i=0;i<memberList.size();i++)
//			{
//				int idx=Integer.parseInt((String)memberList.get(i).get("idx"));
//				Log.e("tmlvc DSPThread idx [" + i + "/" + memberList.size() + "] = " + idx);
//				if (idx>50){
//					boolean exists=false;
//					for (Integer s:p2pSession)
//					{
//						if (idx==s)
//						{
//							Log.e("tmlvc DSPThread exists! idx=" + idx);
//							exists=true;
//							break;
//						}
//					}
//					if (!exists)
//					{
//						String dsp=p2p.generateDSP(""+idx);
//						if (dsp==null)
//							Running=true;
//						else if (AireJupiter.getInstance()!=null)
//						{
//							AireJupiter.getInstance().tcpSocket().sendSDP(idx, dsp);
//							Sleep(3000);
//							Log.e("tmlvc DSPThread !exists sendSDP idx/dsp=" + idx + "/" + dsp);
//						}
//					}
//					else{
//						p2p.startEncoder(0);
//						Running=true;
//					}
//				}
//			}
		}
	};
	
	public List<Map<String,Object>> memberList;
	public void assign(List<Map<String,Object>> member)
	{
//		memberList=member;
	}
	
	public void capture(byte[] data)
	{
//		frames++;
//		if (!Running) return;
//		//if ((frames%3)<2) return;
//		if (p2p!=null) p2p.sendYUV(data);  //send videoconf preview out
	}
	
	public boolean isRecording()
	{
		return isRecording;
	}
	
	public void startCapture()
	{
//		mHandler.postDelayed(new Runnable(){
//			public void run()
//			{
//				Running=true;
//			}
//		}, 1000);
	}
	
	public void stop()
	{
//		Log.e("tmlvc stoppingCamera");
//		Running=false;
//		
//		if (isRecording)
//		{
//			AndroidVideoApi9JniWrapper.stopRecording(cam);
//			isRecording=false;
//			if (cam!=null)
//			{
//				cam.release();
//				cam=null;
//			}
//			
//			((RelativeLayout)mDialer.findViewById(R.id.video_preview)).setVisibility(View.INVISIBLE);
//			
//			Log.d("tmlvc Camera stopped....");
//		}
//		mOutputQueue.clear();
//		
//		arrangeDisplay();
	}
	
	public void quit()
	{
//		Running=false;
//		mHandler.removeCallbacks(checkLayout);
//		
//		stop();
//		Sleep(100);
//		disconnect();
//		
//		p2pSession.clear();
//		
//		instance=null;
	}
	
	static public void Sleep(int ms) {
//		try {
//			Thread.sleep(ms);
//		} catch (Exception e) {
//		}
	}
	
	void setSize(View v, int w, int h)
	{
//		float mDensity = mDialer.getResources().getDisplayMetrics().density;
//		LinearLayout.LayoutParams lp=(LinearLayout.LayoutParams)v.getLayoutParams();
//		lp.width=(int)(mDensity*w);
//		lp.height=(int)(mDensity*h);
//		v.setLayoutParams(lp);
	}
	
	void setSizePreview(View v, int w, int h) {
////		float mDensity = mDialer.getResources().getDisplayMetrics().density;
//		RelativeLayout.LayoutParams lp = (RelativeLayout.LayoutParams) v.getLayoutParams();
////		int setW = (int) (mDensity * w);
//		int setW = (int) (w);
//		lp.width = setW;
//		v.setLayoutParams(lp);
	}
	
	void setWeight(View v, int weight)
	{
//		LinearLayout.LayoutParams lp=(LinearLayout.LayoutParams)v.getLayoutParams();
//		lp.weight=weight;
//		v.setLayoutParams(lp);
////		((LinearLayout.LayoutParams) v.getLayoutParams()).weight = weight;
//		v.refreshDrawableState();
	}
	
	public void arrangeDisplay()
	{	
//		LinearLayout display=(LinearLayout) mDialer.findViewById(R.id.subVWins);
//		
//		int num=0;
//		if (p2p!=null)
//			num=p2p.numOfStreams();
//		
//		RelativeLayout preview=(RelativeLayout)mDialer.findViewById(R.id.video_preview);
//		
//		if (num>0 || isRecording)
//		{
//			((LinearLayout)mDialer.findViewById(R.id.profile)).setVisibility(View.GONE);
//			((LinearLayout)mDialer.findViewById(R.id.subVWins)).setVisibility(View.VISIBLE);
////			((LinearLayout)mDialer.findViewById(R.id.display_sub0)).setVisibility(View.VISIBLE);
//			((TextView)mDialer.findViewById(R.id.conf_timer)).setVisibility(View.VISIBLE);
//		}else{
//			((LinearLayout)mDialer.findViewById(R.id.profile)).setVisibility(View.VISIBLE);
////			((LinearLayout)mDialer.findViewById(R.id.line1)).setVisibility(View.INVISIBLE);
////			((LinearLayout)mDialer.findViewById(R.id.display_sub0)).setVisibility(View.GONE);
//			((TextView)mDialer.findViewById(R.id.conf_timer)).setVisibility(View.INVISIBLE);
//		}
//		
//		SurfaceView view2, view3, view4, view5;
//		Log.e("tmlvc arrangeDisplay STREAMS = " + num);
//		
//		//tml test, dynamic window builder
////		int [] res_id = {R.id.preview_2, R.id.preview_3, R.id.preview_4, R.id.preview_5};
////		int viewtot = res_id.length;
////		int i = 0;
////		if (num < viewtot) {
////			setListPos(0);
////		} else {
////			setListPos(1);
////		}
////		for (i = 0; i < num; i ++) {
////			Log.e("tmlvc sview [" + i + "] visi");
////			SurfaceView sview = (SurfaceView) mDialer.findViewById(res_id[i]);
////			sview.setVisibility(View.VISIBLE);
////		}
////		for (int i2 = i; i2 < viewtot; i2 ++) {
////			Log.e("tmlvc sview [" + i2 + "] gone");
////			SurfaceView sview = (SurfaceView) mDialer.findViewById(res_id[i]);
////			sview.setVisibility(View.GONE);
////		}
//		
//		switch(num)
//		{
//		case 0:
////			((LinearLayout) mDialer.findViewById(R.id.display_sub1)).setVisibility(View.VISIBLE);
////			((LinearLayout) mDialer.findViewById(R.id.display_sub2)).setVisibility(View.GONE);
//			view2=(SurfaceView)mDialer.findViewById(R.id.preview_2);
//			view2.setVisibility(View.GONE);
//			view3=(SurfaceView)mDialer.findViewById(R.id.preview_3);
//			view3.setVisibility(View.GONE);
//			view4=(SurfaceView)mDialer.findViewById(R.id.preview_4);
//			view4.setVisibility(View.GONE);
////			view5=(SurfaceView)display.findViewById(R.id.preview_5);
////			view5.setVisibility(View.GONE);
////			setSize(preview, 240, 180);
//			break;
//		case 1:
////			((LinearLayout) mDialer.findViewById(R.id.display_sub1)).setVisibility(View.VISIBLE);
////			((LinearLayout) mDialer.findViewById(R.id.display_sub2)).setVisibility(View.GONE);
//			view2=(SurfaceView)mDialer.findViewById(R.id.preview_2);
//			view2.setVisibility(View.VISIBLE);
//			view3=(SurfaceView)mDialer.findViewById(R.id.preview_3);
//			view3.setVisibility(View.GONE);
//			view4=(SurfaceView)mDialer.findViewById(R.id.preview_4);
//			view4.setVisibility(View.GONE);
////			view5=(SurfaceView)display.findViewById(R.id.preview_5);
////			view5.setVisibility(View.GONE);
////			setSize(preview, 240, 180);
////			setSize(view2, 380, 285);
//			break;
//		case 2:
////			((LinearLayout) mDialer.findViewById(R.id.display_sub1)).setVisibility(View.VISIBLE);
////			((LinearLayout) mDialer.findViewById(R.id.display_sub2)).setVisibility(View.GONE);
//			view2=(SurfaceView)mDialer.findViewById(R.id.preview_2);
//			view2.setVisibility(View.VISIBLE);
//			view3=(SurfaceView)mDialer.findViewById(R.id.preview_3);
//			view3.setVisibility(View.VISIBLE);
//			view4=(SurfaceView)mDialer.findViewById(R.id.preview_4);
//			view4.setVisibility(View.GONE);
////			view5=(SurfaceView)display.findViewById(R.id.preview_5);
////			view5.setVisibility(View.GONE);
////			setSize(preview, 240, 180);
////			setSize(view2, 248, -1);
////			setSize(view3, 248, -1);
//			break;
//		case 3:
////			((LinearLayout) mDialer.findViewById(R.id.display_sub1)).setVisibility(View.VISIBLE);
////			((LinearLayout) mDialer.findViewById(R.id.display_sub2)).setVisibility(View.VISIBLE);
//			view2=(SurfaceView)mDialer.findViewById(R.id.preview_2);
//			view2.setVisibility(View.VISIBLE);
//			view3=(SurfaceView)mDialer.findViewById(R.id.preview_3);
//			view3.setVisibility(View.VISIBLE);
//			view4=(SurfaceView)mDialer.findViewById(R.id.preview_4);
//			view4.setVisibility(View.VISIBLE);
////			view5=(SurfaceView)display.findViewById(R.id.preview_5);
////			view5.setVisibility(View.INVISIBLE);
////			setSize(preview, 240, 180);
////			setSize(view2, 248, -1);
////			setSize(view3, 248, -1);
////			setSize(view4, 248, -1);
//			break;
////		case 4:
////			((LinearLayout) mDialer.findViewById(R.id.display_sub1)).setVisibility(View.VISIBLE);
////			((LinearLayout) mDialer.findViewById(R.id.display_sub2)).setVisibility(View.VISIBLE);
////			view2=(SurfaceView)display.findViewById(R.id.preview_2);
////			view2.setVisibility(View.VISIBLE);
////			view3=(SurfaceView)display.findViewById(R.id.preview_3);
////			view3.setVisibility(View.VISIBLE);
////			view4=(SurfaceView)display.findViewById(R.id.preview_4);
////			view4.setVisibility(View.VISIBLE);
////			view5=(SurfaceView)display.findViewById(R.id.preview_5);
////			view5.setVisibility(View.VISIBLE);
//////			setSize(preview, 240, 180);
//////			setSize(view2, 248, -1);
//////			setSize(view3, 248, -1);
//////			setSize(view4, 248, -1);
//////			setSize(view5, 248, -1);
////			break;
//		default:
//			break;
//		}
//		
//		display.requestLayout();
//		
//		if (isRecording)
//		{
//			if (cam!=null)
//			{
//				mHandler.postDelayed(setPreview, 1000);
//			}
//		}
	}
	
	private Runnable setPreview=new Runnable(){
		public void run() {
//			try {
//				cam.setPreviewDisplay(mSurface.getHolder());
//			} catch (Exception exc) {
//				exc.printStackTrace();
//			} catch (IncompatibleClassChangeError e){
//			}
		}
	};
	
	private int [] pkts=new int[maxDisplays];
	int alter=0;
	int ticker = 10;
	private Runnable checkLayout=new Runnable(){
		public void run() {
			
//			if ((ticker % 10) == 0) {
//				Log.d("tmlvc self preview @"+(frames-last_frameCount)+" fps");
//				ticker = 0;
//			}
//			ticker++;
//			last_frameCount=frames;
//			
//			if (p2p!=null)
//			{
//				int n=p2p.numOfStreams();
//					if (n!=numOfStreams)
//					{
//						Log.e("tmlvc p2p.numOfStreams CHANGED! " + numOfStreams + ">" + n + " DO arrangeDisplay");
//						numOfStreams=n;
//						arrangeDisplay();
//					}
//					
//					alter++;
//					if ((alter%2)==0)
//					{
//						int msize;
//						if (memberList == null) {
//							msize = -1;
//						} else {
//							msize = memberList.size();
//						}
//						if (n > maxDisplays) {
//							Log.e("tmlvc p2p.numOfStreams > max!! @ " + n + "/" + msize);
//						} else {
//							Log.e("tmlvc p2p.numOfStreams/mList = " + n + "/" + msize);
//						}
////						int []res_id={R.id.preview_2, R.id.preview_3, R.id.preview_4, R.id.preview_5};
//						int []res_id={R.id.preview_2, R.id.preview_3, R.id.preview_4};
//						for (int i = 0; (i < numOfStreams) && (i < maxDisplays); i++)
//						{
//							int display=p2p.getNumOfPackets(i);
//							int changepkts = display - pkts[i];
//							Log.i("tmlvc #pkts v[" + i + "] = " + display + " +" + changepkts);
//							if (i < maxDisplays) {
//								if (pkts[i]>0)
//								{
//									SurfaceView glDisplay=(SurfaceView)mDialer.findViewById(res_id[i]);
//									if (pkts[i]==display && glDisplay.getVisibility()==View.VISIBLE)
//									{
//										Log.d("tmlvc checkLayout v[" + i + "] = View.GONE/INVIS");
//										glDisplay.setVisibility(View.INVISIBLE);
//									}
//									else if (pkts[i]!=display
//											&& (glDisplay.getVisibility() == View.GONE
//													|| glDisplay.getVisibility() == View.INVISIBLE))
//									{
//										Log.d("tmlvc checkLayout v[" + i + "] = View.VISIBLE");
//										glDisplay.setVisibility(View.VISIBLE);
//									} else {
//	//									Log.e("tmlvc checkLayout displayView no change");
//									}
//								}
//								pkts[i]=display;
//							}
//						}
//					}
//				
//			} else {
//				Log.e("tmlvc checkLayout p2p == NULL NULL NULL");
//			}
//			mHandler.postDelayed(checkLayout, 1000);
		}
	};
}
