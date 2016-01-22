package com.pingshow.amper;

import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.pingshow.amper.db.AmpUserDB;
import com.pingshow.amper.db.TimeLineDB;
import com.pingshow.amper.db.TimeLineFollowDB;
import com.pingshow.amper.view.WebPhotoView;
import com.pingshow.network.MyNet;
import com.pingshow.network.NetInfo;
import com.pingshow.util.MyUtil;

public class TimeLineFollows extends Activity {
	
	private MyPreference mPref;
	private TimeLineDB mTLDB;
	private AmpUserDB mADB;
	private TimeLineFollowDB mTLF;
	private ListView mList;
	private FollowsAdapter followsAdapter;
	
	private String mDisplayname;
	private String myNickname;
	private int mIdx;
	private int myIdx;
	private int mPostId;
	private int mExtId;
	private Handler mHandler=new Handler();
	
	private String mContent;
	
	private ArrayList<Map<String, Object>> TalkList=new ArrayList<Map<String, Object>>();
	
	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.timeline_follow);
        mPref=new MyPreference(this);
        
        mADB = new AmpUserDB(this);
		mADB.open(true);
		
        mTLDB = new TimeLineDB(this);
        mTLDB.open(true);
        
        mTLF = new TimeLineFollowDB(this);
        mTLF.open();
        
        mIdx=getIntent().getIntExtra("Idx", 0);
        mPostId=getIntent().getIntExtra("postId", 0);
        mDisplayname=getIntent().getStringExtra("displayname");
        
        myNickname=mPref.read("myNickname","");
        
        try{
        	myIdx=Integer.parseInt(mPref.read("myID","0"),16);
        }catch(Exception e){
        	myIdx=mIdx;
        }
        
        ((TextView)findViewById(R.id.sendee)).setText(mDisplayname);
        
        ((ImageView)findViewById(R.id.cancel)).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				finish();
			}
		});
        
        ((Button)findViewById(R.id.sendmsg)).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				EditText edit = ((EditText)findViewById(R.id.content));
				mContent=((EditText)findViewById(R.id.content)).getText().toString();
				if (mContent.length()>0)
				{
					mHandler.post(popupProgressDialog);
					new Thread(doPostTimelineFollows).start();
					((EditText)findViewById(R.id.content)).setText("");
					InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
					imm.hideSoftInputFromWindow(edit.getWindowToken(), 0);
				}
			}
		});
        
        mList=(ListView)findViewById(R.id.follows);
        
        mHandler.postDelayed(new Runnable(){
        	public void run()
        	{
        		arrageFollows();
                followsAdapter=new FollowsAdapter(TimeLineFollows.this);
                mList.setAdapter(followsAdapter);
        	}
        }, 100);
	}
	
	private void arrageFollows()
	{
		TalkList.clear();
		Cursor c=mTLF.fetch(mPostId);
        do{
        	try{
        		Map<String, Object> map = new HashMap<String, Object>();
        		map.put("ext_id", c.getInt(0));
        		map.put("writer", ""+c.getInt(2));
    			map.put("name", c.getString(3));
    			map.put("text", c.getString(5));
    			TalkList.add(map);
        	}catch(Exception e){}
        }while(c.moveToNext());
        
        c.close();
        
        String likeList=mTLDB.getLikeList(mPostId);
        if (likeList!=null)
        {
        	String [] likes=likeList.split(";");
        	for (String writer: likes)
        	{
        		try{
        			Map<String, Object> map = new HashMap<String, Object>();
        			
            		int idx=Integer.parseInt(writer);
            		String name=mADB.getNicknameByIdx(idx);
            		if (name.length()==0)
            			name=mTLF.getNameByIdx(idx);
            		map.put("ext_id", 0);
            		map.put("writer", ""+idx);//not removable
        			map.put("name", name);
        			map.put("text", getString(R.string.likes_it));
        			TalkList.add(map);
        		}catch(Exception e){}
        	}
        }
        
        c.close();
	}
	
	ProgressDialog progress;
	Runnable popupProgressDialog=new Runnable()
	{
		@Override
		public void run() {
			try{
				progress = ProgressDialog.show(TimeLineFollows.this, "", getString(R.string.in_progress), true, true);
			}catch(Exception e){}
		}
	};
	
	Runnable dismissProgressDialog=new Runnable()
	{
		@Override
		public void run() {
			try{
				if (progress!=null)
				{
					if (progress.isShowing()){
						progress.dismiss();
					}
				}
			}catch(Exception e){}
		}
	};
	
	Runnable doPostTimelineFollows=new Runnable()
	{
		public void run()
		{
			String Return="";
			int c=0;
			do{
				try{
					MyNet net = new MyNet(TimeLineFollows.this);
					Return = net.doPost("timelinepostfollow.php","follow="+mPostId+
							"&writer="+myIdx+
							"&name="+URLEncoder.encode(myNickname,"UTF-8")+
							"&text="+URLEncoder.encode(mContent,"UTF-8")+
							"&pms=0"
							,null);
				}catch(Exception e){}
				if (Return.length()>5) break;
				MyUtil.Sleep(500);
			}while(c++<3);
			
			if (Return.startsWith("Done="))
			{
				int post_id=Integer.parseInt(Return.substring(5));
				
				mTLF.insert(post_id, mPostId, myIdx, myNickname, 0, new Date().getTime(), mContent, null, 0);
				mHandler.postDelayed(new Runnable(){
					public void run()
					{
						arrageFollows();
						followsAdapter.notifyDataSetInvalidated();
					}
				}, 100);
			}
			
			mHandler.post(dismissProgressDialog);
		}
	};
	
	@Override
	protected void onDestroy() {
		if (mTLDB !=null && mTLDB.isOpen())
			mTLDB.close();
		if (mTLF !=null && mTLF.isOpen())
			mTLF.close();
		if (mADB !=null && mADB.isOpen())
			mADB.close();
		
		System.gc();
		System.gc();
		super.onDestroy();
	}
	
	class ViewHolder {
		TextView vName;
		TextView vContent;
		WebPhotoView vPhoto;
		ImageView vRemove;
	}
	
	public class FollowsAdapter extends BaseAdapter {
		private Context mContext;

		public FollowsAdapter(Context context) {
			mContext = context;
		}

		public int getCount() {
			return TalkList.size();
		}

		public Object getItem(int position) {
			return TalkList.get(position);
		}

		public long getItemId(int position) {
			return position;
		}

		public View getView(int position, View convertView, ViewGroup parent) {
			ViewHolder holder;
			Map<String, Object> map = TalkList.get(position);
			if (convertView==null) {
				holder = new ViewHolder();
				convertView = View.inflate(mContext, R.layout.inflate_follow, null);
				holder.vName = (TextView) convertView.findViewById(R.id.displayname);
				holder.vContent = (TextView) convertView.findViewById(R.id.content);
				holder.vPhoto = (WebPhotoView) convertView.findViewById(R.id.photo);
				holder.vRemove = (ImageView) convertView.findViewById(R.id.remove);
				convertView.setTag(holder);
			}
			else{
				holder = (ViewHolder) convertView.getTag();
			}
			Integer ext_id=(Integer)map.get("ext_id");
			int idx=Integer.parseInt((String)map.get("writer"));
			holder.vRemove.setEnabled(idx==myIdx && ext_id!=0);
			holder.vRemove.setAlpha((idx==myIdx && ext_id!=0)?1f:0.4f);
			holder.vRemove.setTag(ext_id);
			holder.vRemove.setOnClickListener(new OnClickListener(){
				@Override
				public void onClick(View v) {
					Integer postId=(Integer)v.getTag();
					removePost(postId);
				};
			});
			String displayname=(String)map.get("name");
			
			holder.vPhoto.setURL("http://"+AireJupiter.myLocalPhpServer+"/onair/profiles/thumbs/photo_"+idx+".jpg");
			holder.vPhoto.setTag(R.string.displayname, displayname);
			holder.vPhoto.setTag(R.string.add, idx);
			holder.vPhoto.setOnClickListener(new OnClickListener(){
				@Override
				public void onClick(View v) {
					Integer idx=(Integer)v.getTag(R.string.add);
					String displayname=(String)v.getTag(R.string.displayname);
					String address=mADB.getAddressByIdx(idx);
					Intent i = new Intent(mContext, TimeLine.class);
					i.putExtra("displayname", displayname);
					i.putExtra("address", address);
					i.putExtra("Idx", idx);
					mContext.startActivity(i);
				}
			});
			
			holder.vName.setTag(R.string.displayname, displayname);
			holder.vName.setTag(R.string.add, idx);
			holder.vName.setOnClickListener(new OnClickListener(){
				@Override
				public void onClick(View v) {
					Integer idx=(Integer)v.getTag(R.string.add);
					String displayname=(String)v.getTag(R.string.displayname);
					String address=mADB.getAddressByIdx(idx);
					Intent i = new Intent(mContext, TimeLine.class);
					i.putExtra("displayname", displayname);
					i.putExtra("address", address);
					i.putExtra("Idx", idx);
					mContext.startActivity(i);
				}
			});
			
			
			holder.vName.setText(displayname);
			holder.vContent.setText((String)map.get("text"));
			
			return convertView;
		}
	}
	
	Runnable removePostThread=new Runnable()
	{
		public void run()
		{
			String Return="";
			int c=0;
			do{
				try{
					MyNet net = new MyNet(TimeLineFollows.this);
					Return = net.doPost("timelineremovefollow.php","id="+myIdx+"&postid="+mExtId, null);
				}catch(Exception e){}
				if (Return.length()>0) break;
				MyUtil.Sleep(500);
			}while(c++<2);
		}
	};
	
	private void removePost(int postId)
	{
		mExtId=postId;
		
		Intent it = new Intent(this, CommonDialog.class);
		String title=getString(R.string.delete_post_confirm);
		it.putExtra("msgContent", title);
		it.putExtra("numItems", 2);
		it.putExtra("ItemCaption0", getString(R.string.no));
		it.putExtra("ItemResult0", Activity.RESULT_CANCELED);
		it.putExtra("ItemCaption1", getString(R.string.yes));
		it.putExtra("ItemResult1", Activity.RESULT_OK);
		startActivityForResult(it, 3);		
	}
	
	private void executeRemove()
	{
		if (!new NetInfo(this).isConnected())
			return;
		
		mTLF.remove(mExtId);
		new Thread(removePostThread).start();
		
		arrageFollows();
		followsAdapter.notifyDataSetInvalidated();
	}
	
	public void onActivityResult(int requestCode, int resultCode, Intent data) { 
	    if (resultCode == RESULT_OK) {
	    	if (requestCode == 3)
	    	{
	    		executeRemove();
	    	}
	    }
	}
}
