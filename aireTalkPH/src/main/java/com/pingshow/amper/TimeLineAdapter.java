package com.pingshow.amper;

import java.io.File;
import java.util.ArrayList;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.CursorAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.pingshow.amper.db.AmpUserDB;
import com.pingshow.amper.db.TimeLineDB;
import com.pingshow.amper.view.WebPhotoView;
import com.pingshow.network.MyNet;
import com.pingshow.network.NetInfo;
import com.pingshow.util.AsyncImageLoader;
import com.pingshow.util.MyUtil;

public class TimeLineAdapter extends CursorAdapter {

	private Context mContext;
	static public AsyncImageLoader asyncImageLoader=null;
	private ListView mList;
	private AmpUserDB mADB;
	private TimeLineDB mTL;
	private float mDensity=1.0f;
	
	private int myIdx;
	private int currentIdx;
	
	public TimeLineAdapter(Context context, Cursor c, boolean autoRequery, AmpUserDB adb, TimeLineDB tl, ListView listview, int myidx, int current_idx) {
		super(context, c, autoRequery);
		mContext=context;
		mDensity = mContext.getResources().getDisplayMetrics().density;
		if (asyncImageLoader==null) asyncImageLoader = new AsyncImageLoader(context);
		mList = listview;
		mADB = adb;
		mTL = tl;
		this.currentIdx=current_idx;
		this.myIdx=myidx;
	}

	public final class ViewBinder {
		public WebPhotoView vPhoto;
		public TextView vDisplayName;
		public ImageView vRemove;
		public ImageView vImage;
		public TextView vContent;
		public TextView vlikes;
		public ImageView vComment;
		public ImageView vLikeIt;
	}
	
	private void requestNewLayout(ImageView v, Bitmap bm)
	{
		//li*** timeline align
		int w=((TimeLine) mContext).getWindowManager().getDefaultDisplay().getWidth()-(int)(40*mDensity);
		int bw = bm.getWidth();
		w = bw > w ? LayoutParams.MATCH_PARENT : bw; 
		
		int h=(int)((float)bm.getHeight()/bw*w);
		if (h==0) h=LayoutParams.WRAP_CONTENT;
		RelativeLayout.LayoutParams lp=new RelativeLayout.LayoutParams(w, h);
		v.setLayoutParams(lp);
	}
	
	private static class DownloadThreadRunnable implements Runnable {
	     private String filename;
	     private String fromServer;

	     DownloadThreadRunnable(String filename, String fromServer) {
	       this.filename = filename;
	       this.fromServer = fromServer;
	     }

	     public void run() {
	    	 try{
	    		 String remote=filename.substring(filename.lastIndexOf("/")+1);
	    		 MyNet net=new MyNet(AireJupiter.getInstance());
	    		 net.Download("timeline/"+remote, filename, fromServer);
	    	 }catch(Exception e){}
	     }
	  }

	@SuppressLint("NewApi")
	@Override
	public void bindView(final View view, final Context context, final Cursor c) {
		ViewBinder holder=(ViewBinder)view.getTag();
		holder.vPhoto = (WebPhotoView) view.findViewById(R.id.photo);
		holder.vDisplayName = (TextView) view.findViewById(R.id.displayname);
		holder.vImage = (ImageView) view.findViewById(R.id.img);
		holder.vContent = (TextView) view.findViewById(R.id.content);
		
		holder.vlikes = (TextView) view.findViewById(R.id.likes);
		holder.vLikeIt = (ImageView) view.findViewById(R.id.likeit);
		holder.vComment = (ImageView) view.findViewById(R.id.comment);
		
		int post_id=c.getInt(0);
		int idx=c.getInt(2);
		int host=c.getInt(1);
		
		String displayname=mADB.getNicknameByIdx(idx);
		if (displayname==null || displayname.length()==0)
		{
			displayname=c.getString(12);
			if (displayname==null || displayname.length()<2)
				displayname=mContext.getString(R.string.unknown_person);
		}
		
		holder.vRemove=(ImageView) view.findViewById(R.id.remove);
		holder.vRemove.setTag(post_id);
		holder.vRemove.setEnabled(idx==myIdx || myIdx==host);
		holder.vRemove.setAlpha((idx==myIdx || myIdx==host)?1f:0.4f);
		holder.vRemove.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View v) {
				Integer postId=(Integer)v.getTag();
				removePost(postId);
			};
		});
		
		holder.vComment.setTag(R.string.add, post_id);
		holder.vComment.setTag(R.string.contacts, c.getInt(1));
		holder.vComment.setTag(R.string.displayname, displayname);
		holder.vComment.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View v) {
				Integer postId=(Integer)v.getTag(R.string.add);
				Intent it=new Intent(mContext, TimeLineFollows.class);
				it.putExtra("postId", postId);
				Integer idx=(Integer)v.getTag(R.string.contacts);
				it.putExtra("Idx", idx);
				String displayname=(String)v.getTag(R.string.displayname);
				it.putExtra("displayname", displayname);
				mContext.startActivity(it);
			};
		});
		
		holder.vPhoto.setURL("http://"+AireJupiter.myLocalPhpServer+"/onair/profiles/thumbs/photo_"+idx+".jpg");
		holder.vPhoto.setTag(R.string.displayname, displayname);
		holder.vPhoto.setTag(R.string.add, idx);
		holder.vPhoto.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View v) {
				Integer idx=(Integer)v.getTag(R.string.add);
				if (currentIdx==idx) return;
				String displayname=(String)v.getTag(R.string.displayname);
				String address=mADB.getAddressByIdx(idx);
				Intent i = new Intent(mContext, TimeLine.class);
				i.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
				i.putExtra("displayname", displayname);
				i.putExtra("address", address);
				i.putExtra("Idx", idx);
				mContext.startActivity(i);
			}
		});
		
		holder.vDisplayName.setTag(R.string.displayname, displayname);
		holder.vDisplayName.setTag(R.string.add, idx);
		holder.vDisplayName.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View v) {
				Integer idx=(Integer)v.getTag(R.string.add);
				if (currentIdx==idx) return;
				String displayname=(String)v.getTag(R.string.displayname);
				String address=mADB.getAddressByIdx(idx);
				Intent i = new Intent(mContext, TimeLine.class);
				i.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
				i.putExtra("displayname", displayname);
				i.putExtra("address", address);
				i.putExtra("Idx", idx);
				mContext.startActivity(i);
			}
		});
		
		
		holder.vDisplayName.setText(displayname);
		
		TextView numView=((TextView) view.findViewById(R.id.num));
		
		String attaches=c.getString(6);
		
		if (attaches!=null && attaches.length()>0)
		{
			try{
				String imageList="";
//				String [] att=attaches.split("<Z>");
				String [] att = attaches.split("<P>");  //li*** multi timepost fix
				for (String item : att)
				{
					if (item.endsWith(".jpg") || item.endsWith(".mp4") || item.endsWith(".mp3"))
					{
						if (imageList.length()>0)
//							imageList+=("<Z>"+Global.SdcardPath_timeline+item);
							imageList+=("<P>" + Global.SdcardPath_timeline + item);  //li*** multi timepost fix
						else
							imageList=(Global.SdcardPath_timeline+item);
					}
				}
				
//				String [] image=imageList.split("<Z>");
				String [] image = imageList.split("<P>");  //li*** multi timepost fix
				if (image[0].length()>0)
				{
					String filepath=image[0];
					if (filepath.endsWith(".jpg"))
					{
						holder.vImage.setTag(R.string.picmsg, imageList);
						holder.vImage.setTag(filepath);
						
						if (!new File(filepath).exists())
						{
							String server=c.getString(10);
							DownloadThreadRunnable download = new DownloadThreadRunnable(filepath, server);
							new Thread(download).start();
						}
						
						Bitmap cachedImage = asyncImageLoader.loadBitmap(filepath, new AsyncImageLoader.ImageBitmapCallback() {				
							public void imageBitmapLoaded(Bitmap imgBitmap, String path) {
								ImageView v=null;
								v = (ImageView) mList.findViewWithTag(path);
								if (v != null && imgBitmap!=null) {
									v.setVisibility(View.VISIBLE);
									v.setImageBitmap(imgBitmap);
									requestNewLayout(v, imgBitmap);
									
									Animation am = new AlphaAnimation(0,1);
								    am.setDuration(1500);
								    v.setAnimation(am);
								    am.startNow();
								}
							}
						});
						
						if (cachedImage != null)
						{
							holder.vImage.setVisibility(View.VISIBLE);
							holder.vImage.setImageBitmap(cachedImage);
							requestNewLayout(holder.vImage, cachedImage);
						}
						else{
							holder.vImage.setVisibility(View.VISIBLE);
							holder.vImage.setImageResource(R.drawable.blank);
						}
						
						if (image.length>1)
						{
							numView.setVisibility(View.VISIBLE);
							numView.setText(""+image.length);
						}
					}else if (filepath.endsWith(".mp4") || filepath.endsWith(".mp3"))
					{
						String server=c.getString(10);
						String thumbnail=filepath.substring(0, filepath.lastIndexOf("."))+".jpg";
						holder.vImage.setTag(R.string.server_ip, server);
						holder.vImage.setTag(R.string.picmsg, imageList);
						holder.vImage.setTag(thumbnail);
						Bitmap cachedImage = asyncImageLoader.loadBitmap(thumbnail, new AsyncImageLoader.ImageBitmapCallback() {				
							public void imageBitmapLoaded(Bitmap imgBitmap, String path) {
								ImageView v=null;
								v = (ImageView) mList.findViewWithTag(path);
								if (v != null && imgBitmap!=null) {
									v.setVisibility(View.VISIBLE);
									v.setImageBitmap(imgBitmap);
									requestNewLayout(v, imgBitmap);
									
									Animation am = new AlphaAnimation(0,1);
								    am.setDuration(1500);
								    v.setAnimation(am);
								    am.startNow();
								}
							}
						});
						
						if (cachedImage != null)
						{
							holder.vImage.setVisibility(View.VISIBLE);
							holder.vImage.setImageBitmap(cachedImage);
							requestNewLayout(holder.vImage, cachedImage);
						}
						
						numView.setVisibility(View.GONE);
					}
				}
			}catch(Exception e){}
			
			holder.vImage.setOnClickListener(new OnClickListener(){
				@Override
				public void onClick(View v) {
					
					try{
						ArrayList<String> images = new ArrayList<String>();
						String imagesList=(String)v.getTag(R.string.picmsg);
//						String [] imagess=imagesList.split("<Z>");
						String [] imagess = imagesList.split("<P>");  //li*** multi timepost fix
						int onlinePlay=0;
						for (String img:imagess)
						{
							images.add(img);
							if (img.endsWith(".mp4"))
								onlinePlay=1;
							else if (img.endsWith(".mp3"))
								onlinePlay=2;
						}
						if (onlinePlay==0)
						{
							Intent it=new Intent(context, ImageViewer.class);
							it.putStringArrayListExtra("images", images);
							context.startActivity(it);
						}
						else if (onlinePlay==1)
						{
							Intent i = new Intent(mContext, VideoPlayerActivity.class);
							String server=(String)v.getTag(R.string.server_ip);
							String file=imagess[0].substring(imagess[0].lastIndexOf("/")+1);
							i.putExtra("MEDIA", 5);
							i.putExtra("URL", "http://"+server+"/onair/timeline/"+file);
							context.startActivity(i);
						}
						else if (onlinePlay==2)
						{
							Intent i = new Intent(mContext, MusicPlayerActivity.class);
							String server=(String)v.getTag(R.string.server_ip);
							String file=imagess[0].substring(imagess[0].lastIndexOf("/")+1);
							i.putExtra("URL", "http://"+server+"/onair/timeline/"+file);
							String artPath=imagess[0].substring(0, imagess[0].lastIndexOf("."))+".jpg";
							i.putExtra("art", artPath);
							context.startActivity(i);
						}
					}catch(Exception e){}
					
				}
			});
		}
		else
		{
			holder.vImage.setVisibility(View.GONE);
			numView.setVisibility(View.GONE);
		}
		
		String content=c.getString(4);
		if (content!=null && content.length()>0)
		{
			holder.vContent.setVisibility(View.VISIBLE);
			holder.vContent.setText(content);
		}
		else
			holder.vContent.setVisibility(View.GONE);
		
		holder.vLikeIt.setTag(R.string.add, post_id);
		holder.vLikeIt.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View v) {
				Integer doILikeIt=(Integer)v.getTag(R.string.add_contact);
				if (doILikeIt>0)
				{
					Integer postId=(Integer)v.getTag(R.string.add);
					LinearLayout parent=(LinearLayout) v.getParent();
					ImageView likeView=(ImageView)parent.findViewById(R.id.likeit);
					likeView.setImageResource(R.drawable.likeit);
					toggleLikeIt(postId, false);
					v.setTag(R.string.add_contact, 0);
				}
				else{
					Integer postId=(Integer)v.getTag(R.string.add);
					LinearLayout parent=(LinearLayout) v.getParent();
					ImageView likeView=(ImageView)parent.findViewById(R.id.likeit);
					likeView.setImageResource(R.drawable.likeit2);
					toggleLikeIt(postId, true);
					v.setTag(R.string.add_contact, 1);
				}
			}
		});
		
		String likelist=c.getString(7);
		
		if (findMeInLikes(likelist, ""+myIdx))
		{
			holder.vLikeIt.setTag(R.string.add_contact, 1);
			holder.vLikeIt.setImageResource(R.drawable.likeit2);
		}
		else{
			holder.vLikeIt.setTag(R.string.add_contact, 0);
			holder.vLikeIt.setImageResource(R.drawable.likeit);
		}
		int comments=c.getInt(8);
		int likes=0;
		if (likelist!=null && likelist.length()>0)
		{
			String [] likesList=likelist.split(";");
			likes=likesList.length;
		}
		String state="";
		if (comments>0)
			state=mContext.getString(R.string.comments)+comments+" ";
		if (likes>0)
			state=state+mContext.getString(R.string.likes)+likes;
		holder.vlikes.setText(state);
	}
	
	@Override
	public View newView(Context context, Cursor cursor, ViewGroup parent) {
		ViewBinder holder = new ViewBinder();
		View v=LayoutInflater.from(context).inflate(R.layout.inflate_post, null);
		v.setTag(holder);
		return v;
	}
	
	private int mPostId;
	Runnable likeIt=new Runnable()
	{
		public void run()
		{
			String Return="";
			int c=0;
			do{
				try{
					MyNet net = new MyNet(mContext);
					Return = net.doPost("timelinelikeit.php","id="+myIdx+
							"&postid="+mPostId, null);
				}catch(Exception e){}
				if (Return.length()>0) break;
				MyUtil.Sleep(500);
			}while(c++<2);
		}
	};
	Runnable dislikeIt=new Runnable()
	{
		public void run()
		{
			String Return="";
			int c=0;
			do{
				try{
					MyNet net = new MyNet(mContext);
					Return = net.doPost("timelinedislikeit.php","id="+myIdx+
							"&postid="+mPostId, null);
				}catch(Exception e){}
				if (Return.length()>0) break;
				MyUtil.Sleep(500);
			}while(c++<2);
		}
	};
	
	Runnable removePostThread=new Runnable()
	{
		public void run()
		{
			String Return="";
			int c=0;
			do{
				try{
					MyNet net = new MyNet(mContext);
					Return = net.doPost("timelineremove.php","id="+myIdx+"&postid="+mPostId, null);
				}catch(Exception e){}
				if (Return.length()>0) break;
				MyUtil.Sleep(500);
			}while(c++<2);
		}
	};
	
	private void toggleLikeIt(int postId, boolean like)
	{
		if (!new NetInfo(mContext).isConnected())
			return;
		mPostId=postId;
		mTL.setLikeIt(postId, myIdx, like);
		if (like) 
			new Thread(likeIt).start();
		else
			new Thread(dislikeIt).start();
		
		if (TimeLine.getInstance()!=null)
			TimeLine.getInstance().refresh();
	}
	
	private void removePost(int postId)
	{
		mPostId=postId;
		
		Intent it = new Intent(mContext, CommonDialog.class);
		String title=mContext.getString(R.string.delete_post_confirm);
		it.putExtra("msgContent", title);
		it.putExtra("numItems", 2);
		it.putExtra("ItemCaption0", mContext.getString(R.string.no));
		it.putExtra("ItemResult0", Activity.RESULT_CANCELED);
		it.putExtra("ItemCaption1", mContext.getString(R.string.yes));
		it.putExtra("ItemResult1", Activity.RESULT_OK);
		((TimeLine)mContext).startActivityForResult(it, 3);
	}
	
	public void executeRemove()
	{
		if (!new NetInfo(mContext).isConnected())
			return;
		
		mTL.remove(mPostId);
		new Thread(removePostThread).start();
		
		if (TimeLine.getInstance()!=null)
			TimeLine.getInstance().onTimelineQuery();
	}
	
	private boolean findMeInLikes(String src, String idx)
	{
		boolean found=false;
		if (src!=null && src.length()>0)
		{
			String [] sIdxs=src.split(";");
			for (String sIdx:sIdxs)
			{
				if (sIdx.equals(idx))
				{
					found=true;
					break;
				}
			}
		}	
		return found;
	}
}
