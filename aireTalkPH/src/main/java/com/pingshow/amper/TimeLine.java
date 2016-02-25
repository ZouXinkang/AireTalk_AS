package com.pingshow.amper;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Date;

import android.app.Activity;
import android.content.AsyncQueryHandler;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.drawable.AnimationDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.provider.ContactsContract.CommonDataKinds;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.pingshow.amper.db.AmpUserDB;
import com.pingshow.amper.db.TimeLineDB;
import com.pingshow.amper.db.TimeLineFollowDB;
import com.pingshow.network.MyNet;
import com.pingshow.util.ImageUtil;
import com.pingshow.util.MyUtil;

public class TimeLine extends Activity {
	
	private MyPreference mPref;
	private ListView mList;
	private ImageView mWaitView;
	private AmpUserDB mADB;
	private TimeLineDB mTL;
	private TimeLineFollowDB mTLF;
	private AnimationDrawable waitAnim;
	
	private QueryThreadHandler mThreadQueryHandler;
	public TimeLineAdapter mCursorAdapter;
	public Cursor mCursor;
	private Handler mHandler=new Handler();
	
	private String mDisplayname;
	private String mAddress;
	private int mIdx;
	private int myIdx=0;
	private boolean bMySelf=false;
	private int lastCount=-1;
	private int curCount=0;
	private int limit=50;
	private boolean bFirstTime=true;
	
	static private TimeLine instance=null;
	
	static public TimeLine getInstance()
	{
		return instance;
	}
	
	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.timeline);
        mPref=new MyPreference(this);
        
        mTL = new TimeLineDB(this);
        mTL.open();
        
        mTLF = new TimeLineFollowDB(this);
        mTLF.open();
        
        mADB = new AmpUserDB(this);
        mADB.open();
        
        mIdx=getIntent().getIntExtra("Idx", 0);
        mDisplayname=getIntent().getStringExtra("displayname");
        mAddress=getIntent().getStringExtra("address");
        
		try{
			myIdx=Integer.parseInt(mPref.read("myID","0"),16);
		}catch(Exception e){}

		mWaitView=(ImageView) findViewById(R.id.wait);
		waitAnim=(AnimationDrawable)mWaitView.getDrawable();
		
        bMySelf=(myIdx==mIdx);
        
        mList = (ListView)findViewById(R.id.timeline);
        LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View profile = inflater.inflate(R.layout.inflate_profile, mList, false);
        mList.addHeaderView(profile);
        
        mList.setOnScrollListener(timelineScrollListener);
        
        ((TextView)findViewById(R.id.sendee)).setText(mDisplayname);
        
        Drawable photo=ImageUtil.getBigRoundedUserPhoto(this, mIdx);
        ((ImageView)profile.findViewById(R.id.photo)).setImageDrawable(photo);
        ((TextView)profile.findViewById(R.id.displayname)).setText(mDisplayname);
        String mood;
        if (bMySelf)
        	mood=mPref.read("moodcontent","");
        else
        	mood=mADB.getMoodByAddress(mAddress);
        ((TextView)profile.findViewById(R.id.mood)).setText(mood);
        
        if (bMySelf) 
        	((ImageView)profile.findViewById(R.id.photo)).setOnClickListener(null);
        else
        ((ImageView)profile.findViewById(R.id.photo)).setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View v) {
				if (bMySelf) return;
				String userphotoPath = Global.SdcardPath_inbox + "photo_" + mIdx + "b.jpg";
				File f = new File(userphotoPath);
				if (!f.exists())
				{
					mHandler.post(showWaiting);
					new Thread(new Runnable(){
						public void run()
						{
							String userphotoPath = Global.SdcardPath_inbox + "photo_" + mIdx + "b.jpg";
							String remotefile = "profiles/photo_" + mIdx + ".jpg";
							int success = 0;
							int count = 0;
							do {
								MyNet net = new MyNet(TimeLine.this);
								//bree
//								success = net.Download(remotefile, userphotoPath, AireJupiter.myLocalPhpServer);
								success = net.DownloadUserPhoto(remotefile, userphotoPath);
								if (success==1||success==0)
									break;
								MyUtil.Sleep(500);
							} while (++count < 2);
							
							if (success!=1)
							{
								count=0;
								do {
									MyNet net = new MyNet(TimeLine.this);
									//bree
									success = net.DownloadUserPhoto(remotefile, userphotoPath);
//									success = net.Download(remotefile, userphotoPath, null);
									if (success==1||success==0)
										break;
									MyUtil.Sleep(500);
								} while (++count < 2);
							}
							
							mHandler.post(hideWaiting);
							
							if (success==1)
							{
								File f = new File(userphotoPath);
								if (f.exists())
								{
									Intent i = new Intent(TimeLine.this,MessageDetailActivity.class);
									i.putExtra("imagePath", userphotoPath);
									i.putExtra("displayname", mDisplayname);
									i.putExtra("address", mAddress);
									startActivity(i);
								}
							}
						}
					}).start();
				}
				else
				{
					Intent i = new Intent(TimeLine.this,MessageDetailActivity.class);
					i.putExtra("imagePath", userphotoPath);
					i.putExtra("displayname", mDisplayname);
					i.putExtra("address", mAddress);
					startActivity(i);
				}
			}
		});
        
        ((ImageView)findViewById(R.id.cancel)).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				finish();
			}
		});
        
        ((ImageView)findViewById(R.id.compose)).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				Intent i = new Intent(TimeLine.this, TimeLineCompose.class);
				i.putExtra("displayname", mDisplayname);
				i.putExtra("address", mAddress);
				i.putExtra("Idx", mIdx);
				startActivityForResult(i, 1000);
			}
		});
        
        mHandler.post(showWaiting);
        
        onTimelineQuery();
        
        long last=mPref.readLong("lastTimeLineUpdated:"+mIdx, 0);
        if (new Date().getTime()-last>15000)//15 sec
        {
        	new Thread(doReadTimeline).start();
        }
        
        instance=this;
        
        //tml*** dev control
        if ((mAddress.equals("news_service") && mDisplayname.equals("Hot News")) || mIdx == 4) {
            if (bMySelf) {
            	((ImageView)findViewById(R.id.compose)).setVisibility(View.VISIBLE);
        	} else {
            	((ImageView)findViewById(R.id.compose)).setVisibility(View.GONE);
        	}
        } else if ((mAddress.equals("support") && mDisplayname.equals("Support")) || mIdx == 2) {
            if (bMySelf) {
            	((ImageView)findViewById(R.id.compose)).setVisibility(View.VISIBLE);
        	} else {
            	((ImageView)findViewById(R.id.compose)).setVisibility(View.GONE);
        	}
        }
        //***tml
        
	}
	
	Runnable showWaiting=new Runnable()
	{
		public void run()
		{
			waitAnim.start();
			mWaitView.setVisibility(View.VISIBLE);
		}
	};
	
	Runnable hideWaiting=new Runnable()
	{
		public void run()
		{
			waitAnim.stop();
			mWaitView.setVisibility(View.GONE);
		}
	};
	
	Runnable doReadTimeline=new Runnable()
	{
		public void run()
		{
			if (curCount==lastCount)
			{
				mHandler.post(hideWaiting);
				return;
			}
			lastCount=curCount;
			
			String Return="";
			int c=0;
			do{
				try {
					MyNet net = new MyNet(TimeLine.this);
					int offset=(bFirstTime?0:curCount);
					Return = net.doPost("timelineread.php","id="+mIdx+
							"&visitor="+myIdx+
							"&offset="+offset+
							"&limit="+limit
							, null);
				} catch (Exception e) {
					Log.e("timelineread.php !@#$ " + e.getMessage());
				}
				if (Return.length()>0) break;
				MyUtil.Sleep(500);
			}while(c++<3);
			
			bFirstTime=false;
			
			if (Return.length()>10)
			{
				mPref.writeLong("lastTimeLineUpdated:"+mIdx, new Date().getTime());
				parseTimeLineData(Return);
			}
			mHandler.post(hideWaiting);
		}
	};
	
	public static int parseTimeLineDate(TimeLineDB tldb, TimeLineFollowDB tlfdb, String src, int localHost)
	{
		int processed=0;
		Log.i("parseTimeLineDate: " + src);
		try {
			String [] rows=src.split("<Z>");
			Log.i("parseTimeLineDate rows " + rows.length + " to be parsed");
			for (String row : rows)
			{
				String [] parts=row.split("<W>");
				if (parts.length==0) continue;
				int post_id=0;
				int host=0;
				int writer=0;
				String text="";
				int permission=0;
				String attach=null;
				long time=0;
				int deleted=0;
				String server="";
				String name="";
				
				if (parts.length>0 && parts[0].length()>0)
				{
					try{
						String [] items=parts[0].split("<R>");
						if (items.length>8)
						{
							post_id=Integer.parseInt(items[0]);
							host=Integer.parseInt(items[1]);
							writer=Integer.parseInt(items[2]);
							text=URLDecoder.decode(items[3], "UTF-8");
							permission=Integer.parseInt(items[4]);
							attach=items[5];
							time=Long.parseLong(items[6])*1000;
							deleted=Integer.parseInt(items[7]);
							server=URLDecoder.decode(items[8], "UTF-8");
						}
						if (items.length>9)
						{
							name=URLDecoder.decode(items[9], "UTF-8");
						}
					} catch (NumberFormatException e) {
						Log.e("parseTimeLineDate parseNum1 !@#$ " + e.getMessage());
					} catch (UnsupportedEncodingException e) {
						Log.e("parseTimeLineDate decodeURL1 !@#$ " + e.getMessage());
					} catch (NullPointerException e) {
						Log.e("parseTimeLineDate null1 !@#$ " + e.getMessage());
					}
				}
				
				int follows=0;
				if (parts.length>1 && parts[1].length()>0)
				{
					try{
						String [] fItems=parts[1].split("<X>");
						for (String f : fItems)
						{
							String [] items=f.split("<F>");
							if (items.length>8)
							{
								int p=Integer.parseInt(items[0]);
								int ff=Integer.parseInt(items[1]);
								int w=Integer.parseInt(items[2]);
								String n=URLDecoder.decode(items[3], "UTF-8");
								int pms=Integer.parseInt(items[4]);
								String t=URLDecoder.decode(items[5], "UTF-8");
								String a=items[6];
								int d=Integer.parseInt(items[7]);
								long ti=Long.parseLong(items[8])*1000;
								tlfdb.insert(p,ff,w,n,pms,ti,t,a,d);
								if (d==0) follows++;
							}
						}
					} catch (NumberFormatException e) {
						Log.e("parseTimeLineDate parseNum2 !@#$ " + e.getMessage());
					} catch (UnsupportedEncodingException e) {
						Log.e("parseTimeLineDate decodeURL2 !@#$ " + e.getMessage());
					} catch (NullPointerException e) {
						Log.e("parseTimeLineDate null2 !@#$ " + e.getMessage());
					}
				}
				
				String likeList=null;
				if (parts.length>2 && parts[2].length()>0)
				{
					likeList=parts[2];
				}
				
				long rowid=tldb.insert(post_id, host, localHost, writer, name, permission, time, text, attach, likeList, server, follows, deleted);
				
				if (rowid>0) processed++;
				
				if (attach!=null && attach.length()>5)
				{
					mAttachesToDownload.add(attach);
					mAttachesFrom.add(server);
					new Thread(doDownloadAttached).start();
				}
			}
		} catch (Exception e) {
			Log.e("parseTimeLineDate !@#$ " + e.getMessage());
		}
		
		return processed;
	}
	
	private static ArrayList<String> mAttachesToDownload = new ArrayList<String>();
	private static ArrayList<String> mAttachesFrom = new ArrayList<String>();
	static Runnable doDownloadAttached=new Runnable()
	{
		public void run()
		{
			String toDownload="";
			String fromServer="";
			if (mAttachesToDownload.size()>0)
			{
				toDownload=mAttachesToDownload.remove(0);
				fromServer=mAttachesFrom.remove(0);
			}
			else
				return;
			try {
//				String [] items=toDownload.split("<Z>");
				String [] items = toDownload.split("<P>");  //li*** multi timepost fix
				for (String filename : items)
				{
					if (filename.length()>0)
					{
						if (filename.endsWith(".jpg"))
						{
							File file=new File(Global.SdcardPath_timeline+filename);
							if (!file.exists())
							{
								try{
									MyNet net=new MyNet(AireJupiter.getInstance());
									int success=net.Download("timeline/"+filename, Global.SdcardPath_timeline+filename, fromServer);
									if (success>0)
									{
										if (TimeLine.getInstance()!=null)
											TimeLine.getInstance().refreshAttached();
									}
								} catch (Exception e) {
									Log.e("timeline Download1 !@#$ " + e.getMessage());
								}
							}
						}else{
							String thumbnail = "";
							try {
								thumbnail=filename.substring(0, filename.lastIndexOf("."))+".jpg";
							} catch (IndexOutOfBoundsException e) {
								Log.e("timeline Download !@#$ " + filename + " " + e.getMessage());
								return;
							}
							File file=new File(Global.SdcardPath_timeline+thumbnail);
							if (!file.exists())
							{
								try{
									MyNet net=new MyNet(AireJupiter.getInstance());
									int success=net.Download("timeline/"+thumbnail, Global.SdcardPath_timeline+thumbnail, fromServer);
									if (success>0)
									{
										if (TimeLine.getInstance()!=null)
											TimeLine.getInstance().refreshAttached();
									}
								}catch(Exception e){
									Log.e("timeline Download2 !@#$ " + e.getMessage());
								}
							}
						}
					}
				}
			} catch (Exception e) {
				Log.e("timeline doDownloadAttached !@#$ " + e.getMessage());
			}
		}
	};
	
	
	private void parseTimeLineData(String src)
	{
		TimeLine.parseTimeLineDate(mTL, mTLF, src, mIdx);
		mHandler.post(new Runnable(){
			public void run()
			{
				refresh();
			}
		});	
	}
	
	private void refreshAttached()
	{
		mHandler.post(new Runnable(){
			public void run()
			{
				if (mCursorAdapter!=null)
					mCursorAdapter.notifyDataSetChanged();
			}
		});
	}
	
	public void refresh() {
		//if (mCursor != null && !mCursor.isClosed())
		//	mCursor.close();
		
		mCursor = mTL.fetch(mIdx);

		if (mCursorAdapter != null)
			mCursorAdapter.changeCursor(mCursor);
	}
	
	public void onTimelineQuery() {
		//if (mCursor != null && !mCursor.isClosed())
		//	mCursor.close();
		
		mCursor = mTL.fetch(mIdx);
		
		if (mCursor == null) {
			if (mCursorAdapter != null)
				mCursorAdapter.changeCursor(null);
			return;
		}
		
		long last=mPref.readLong("lastTimeLineUpdated:"+mIdx, 0);
        if (new Date().getTime()-last<600000)//10 min
        {
        	if (mCursor.getCount()>0)
    			mHandler.post(hideWaiting);
        }

		if (mThreadQueryHandler == null)
			mThreadQueryHandler = new QueryThreadHandler(getContentResolver());
		try {
			mThreadQueryHandler.startQuery(0, null, CommonDataKinds.Phone.CONTENT_URI, 
					new String [] {CommonDataKinds.Phone.CONTACT_ID}, 
					CommonDataKinds.Phone.CONTACT_ID + "=0", null,
					null);//alec: let it query useless curser to avoid NullException
		} catch (Exception e) {}
	}
	
	private class QueryThreadHandler extends AsyncQueryHandler {
		public QueryThreadHandler(ContentResolver cr) {
			super(cr);
		}

		@Override
		protected void onQueryComplete(int token, Object cookie, Cursor c) {
			try {
				c = mCursor;
				if (mCursorAdapter == null) {
					mCursorAdapter = new TimeLineAdapter(TimeLine.this, mCursor, false, mADB, mTL, mList, myIdx, mIdx);
					mList.setAdapter(mCursorAdapter);
				} else
					mCursorAdapter.changeCursor(mCursor);
			} catch (Exception e) {
				Log.e("timeline onQueryComplete !@#$ " + e.getMessage());
			}
		}

		@Override
		protected void onInsertComplete(int token, Object cookie, Uri uri) {
			//onTimelineQuery();
		}

		@Override
		protected void onUpdateComplete(int token, Object cookie, int result) {
			//onTimelineQuery();
		}

		@Override
		protected void onDeleteComplete(int token, Object cookie, int result) {
			//onTimelineQuery();
		}
	}
	
	@Override
	protected void onDestroy() {
		if (mCursor != null && !mCursor.isClosed())
			mCursor.close();
		if (mTL != null && mTL.isOpen())
			mTL.close();
		if (mTLF != null && mTLF.isOpen())
			mTLF.close();
		if (mADB != null && mADB.isOpen())
			mADB.close();
		instance=null;
		System.gc();
		System.gc();
		super.onDestroy();
	}
	
	public void onActivityResult(int requestCode, int resultCode, Intent data) { 
	    if (resultCode == RESULT_OK) {
	    	if (requestCode == 1000)
	    	{
	    		onTimelineQuery();
	    	}
	    	else if (requestCode == 3)
	    	{
	    		mCursorAdapter.executeRemove();
	    	}
	    }
	}
	
	private OnScrollListener timelineScrollListener = new OnScrollListener() {
		private int visibleItem;
		public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
			visibleItem=firstVisibleItem+visibleItemCount;
		}
		public void onScrollStateChanged(AbsListView view, int scrollState) {
			isScrollCompleted(scrollState);
		}
		private void isScrollCompleted(int currentScrollState) {
			if (currentScrollState == OnScrollListener.SCROLL_STATE_IDLE) {
				curCount=mCursor.getCount();
				if (visibleItem>=curCount)
				{
					new Thread(doReadTimeline).start();
				}
			}
		}
	};
}
