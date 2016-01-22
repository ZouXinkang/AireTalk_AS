package com.pingshow.amper;

import java.util.Date;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.text.SpannableString;
import android.text.format.DateUtils;
import android.text.style.ImageSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.pingshow.amper.contacts.ContactsQuery;
import com.pingshow.amper.db.AmpUserDB;
import com.pingshow.amper.db.SmsDB;
import com.pingshow.util.ImageUtil;

public class MessageThreadAdapter extends CursorAdapter {

	private Context mContext;
	public boolean showDeleteIcon;
	private SmsDB mDB;
	private ContactsQuery cq;
	private AmpUserDB mADB;
	private BitmapDrawable empty;
	private float mDensity=1.0f;
	private boolean largeScreen;
	private float size=24;
	
	public MessageThreadAdapter(Context context, Cursor c, SmsDB db, AmpUserDB ampdb, ContactsQuery _cq, boolean large) {
		super(context, c);
		this.mContext = context;
		largeScreen=large;
		if (largeScreen)
			size=36;
		mDB=db;
		cq=_cq;
		mADB = ampdb;
		try{
			empty = new BitmapDrawable(BitmapFactory.decodeResource(mContext.getResources(), R.drawable.bighead));
		}catch (OutOfMemoryError e){
			empty = new BitmapDrawable(BitmapFactory.decodeResource(mContext.getResources(), R.drawable.empty));
		}
		mDensity = context.getResources().getDisplayMetrics().density;
	}

	public final class ViewBinder {
		public ImageView vPhoto;
		public TextView vDisplayName;
		public TextView vMsgContent;
		public TextView vTime;
		public TextView vUnread;
	}

	@Override
	public void bindView(final View view, final Context context, final Cursor c) {

		ViewBinder holder = null;

		holder = new ViewBinder();

		holder.vPhoto = (ImageView) view.findViewById(R.id.photo);
		holder.vDisplayName = (TextView) view.findViewById(R.id.displayname);
		holder.vMsgContent = (TextView) view.findViewById(R.id.msgcontent);
		holder.vTime = (TextView) view.findViewById(R.id.time);
		holder.vUnread = (TextView) view.findViewById(R.id.unread);
		String address = c.getString(1);//address
		int count=mDB.getThreadCountByAddress(address);
		int unread=mDB.getUnreadCountByAddress(address);
		if (unread>0)
		{
			holder.vUnread.setText(""+unread);
			holder.vUnread.setVisibility(View.VISIBLE);
		}else
			holder.vUnread.setVisibility(View.INVISIBLE);
		String CountNum="";
		if (count>1) CountNum=" ("+count+")";
		
		long contactid=cq.getContactIdByNumber(address);
		String displayname="";
		if (contactid>0)
			displayname=cq.getNameByContactId(contactid);
		else
			displayname=mADB.getNicknameByAddress(address);

		//zhao
		Drawable photo=null;
		String path="";
		try {
			path = Global.SdcardPath_inbox+"photo_"+mADB.getIdxByAddress(address)+".jpg";
		} catch (NumberFormatException e) {}
		
		photo = ImageUtil.getBitmapAsRoundCorner(path, 1, 5);//alec
		
		if (photo==null)
		{
			try{
				photo=cq.getPhotoById(mContext, contactid, false);
			}catch (OutOfMemoryError e) {}
			if (photo==null)
				photo=empty;
		}
		
		holder.vPhoto.setImageDrawable(photo);
		
		if (displayname==null) displayname=address;
		
		holder.vDisplayName.setText(displayname+CountNum);
		
		String s=c.getString(5);// body
		if (c.getInt(6) == 9 && s.lastIndexOf("KB")+3 == s.length()) {// video uploaded
			s = s.substring(0,s.length() - 1);
		}
		else if (s!=null)
			s=s.replace("\n", " ");
		
		if(s!= null && s.startsWith("[<AGREESHARE>]")){
			String[] res=s.split(",");
			int relation = Integer.valueOf(res[2]);
			s = context.getString(R.string.agree_share_sms,context.getResources().getStringArray(R.array.share_time)[relation-1]);
		}
		if (s!= null && s.startsWith(context.getString(R.string.video)) && s.contains("(vdo)")){
			s = "(vdo)";
        }
		if(s!= null && s.startsWith(context.getString(R.string.file)+" [")){
			s="(fl)";
		}
		if (s.startsWith("here I am ("))
			s="(mAp)";
		else if (s.equals("Missed call"))
			s="(mCl)";
		else if (s.startsWith("(Vm)") && s.length()>4)
			s+='"';
		
		Smiley sm=new Smiley();
		if (sm.hasSmileys(s)>0)
		{
			SpannableString spannable = new SpannableString(s);
			for(int i=0;i<Smiley.MAXSIZE;i++)
			{
				for(int j=0;j<sm.getCount(i);j++)
				{ 
					if(i==(Smiley.MAXSIZE-1)){//picture
						Drawable d = ImageUtil.loadBitmapSafe(c.getString(7),5);
						if(d==null){
							spannable = new SpannableString(context.getString(R.string.notfound_photo));
						}else{
							int start = sm.getStart(i,j);
							int end = sm.getEnd(i,j);
							float r=(float)d.getIntrinsicHeight()/(float)d.getIntrinsicWidth();
							d.setBounds(0, 0, (int)(mDensity*size/r), (int)(mDensity*size));
							//ImageSpan icon = new ImageSpan(rContext, bitmap, ImageSpan.ALIGN_BOTTOM);
							ImageSpan icon = new ImageSpan(d, ImageSpan.ALIGN_BOTTOM);
							spannable.setSpan(icon, start, end, SpannableString.SPAN_EXCLUSIVE_EXCLUSIVE);
						}
					}else{
						ImageSpan icon = null;
						if(i>=66){
							Bitmap bitmap = null;
							if(i == 69){ // video
								try{
									bitmap = BitmapFactory.decodeResource(mContext.getResources(), R.drawable.sm70);
								}catch(OutOfMemoryError e){
									continue;
								}
								icon = new ImageSpan(mContext, bitmap, ImageSpan.ALIGN_BOTTOM);
							}else{
								/* TODO
								if(i == 70)
								{
									Drawable d = context.getResources().getDrawable(R.drawable.sm71);
									d.setBounds(0, 0, d.getIntrinsicWidth()*3/5, d.getIntrinsicHeight()*3/5);
									icon = new ImageSpan(d, ImageSpan.ALIGN_BOTTOM);
								}
								else if(i>=71){
									Drawable d = context.getResources().getDrawable(R.drawable.em64+i-71);
									int h=(int)(mDensity*26.667);
									int w=d.getIntrinsicWidth()*h/d.getIntrinsicHeight();
									d.setBounds(0, 0, w, h);
									icon = new ImageSpan(d, ImageSpan.ALIGN_BOTTOM);
								}
								else*/
								if(i == 66)
								{
									Drawable d = context.getResources().getDrawable(R.drawable.sm67);
									d.setBounds(0, 0, (int)(mDensity*size), (int)(mDensity*size));
									icon = new ImageSpan(d, ImageSpan.ALIGN_BOTTOM);
								}
								else if(i == 71)
								{
									Drawable d = context.getResources().getDrawable(R.drawable.mapview);
									d.setBounds(0, 0, (int)(mDensity*size), (int)(mDensity*size));
									icon = new ImageSpan(d, ImageSpan.ALIGN_BOTTOM);
								}
								else if (i == 72){
									Drawable d = context.getResources().getDrawable(android.R.drawable.sym_call_missed);
									d.setBounds(0, 0, (int)(mDensity*size), (int)(mDensity*size));
									icon = new ImageSpan(d, ImageSpan.ALIGN_BOTTOM);
								}
								else if (i == 73){
									Drawable d = context.getResources().getDrawable(android.R.drawable.sym_call_outgoing);
									d.setBounds(0, 0, (int)(mDensity*size), (int)(mDensity*size));
									icon = new ImageSpan(d, ImageSpan.ALIGN_BOTTOM);
								}
								else if (i == 74){
									Drawable d = context.getResources().getDrawable(android.R.drawable.sym_call_incoming);
									d.setBounds(0, 0, (int)(mDensity*size), (int)(mDensity*size));
									icon = new ImageSpan(d, ImageSpan.ALIGN_BOTTOM);
								}
								else if (i >= 75){
									Drawable d = context.getResources().getDrawable(R.drawable.em001+i-75);
									int h=(int)(mDensity*26.667);
									int w=d.getIntrinsicWidth()*h/d.getIntrinsicHeight();
									d.setBounds(0, 0, w, h);
									icon = new ImageSpan(d, ImageSpan.ALIGN_BOTTOM);
								}
								else{
									BitmapFactory.Options options = new BitmapFactory.Options();
									options.inSampleSize = 3;
									try{
										bitmap = BitmapFactory.decodeResource(mContext.getResources(), R.drawable.sm01+i, options);
									}catch(OutOfMemoryError e){
										continue;
									}
									icon = new ImageSpan(mContext, bitmap, ImageSpan.ALIGN_BOTTOM);
								}
							}
						}else{
							Drawable d = context.getResources().getDrawable(R.drawable.sm01+i);
							if (largeScreen)
								d.setBounds(0, 0, d.getIntrinsicWidth(), d.getIntrinsicHeight());
							else
								d.setBounds(0, 0, d.getIntrinsicWidth()*3/5, d.getIntrinsicHeight()*3/5);
							icon = new ImageSpan(d, ImageSpan.ALIGN_BOTTOM);
						}
						spannable.setSpan(icon, sm.getStart(i,j), sm.getEnd(i,j), SpannableString.SPAN_EXCLUSIVE_EXCLUSIVE);
					}
				}
			}
			holder.vMsgContent.setText(spannable);
		}
		else
			holder.vMsgContent.setText(s);
		
		holder.vTime.setText(ShowBetterTime(c.getLong(3), new Date().getTime()));
		view.setTag(holder);
	}
	
	static public String ShowBetterTime(long when, long now)
	{
		try{
			String s=(String) DateUtils.getRelativeTimeSpanString(when,now,DateUtils.MINUTE_IN_MILLIS,DateUtils.FORMAT_ABBREV_TIME);
			if (s.length()>10) s=s.trim();
			return s;
		}catch(Exception e){}
		return "";
	}

	@Override
	public View newView(Context context, Cursor cursor, ViewGroup parent) {
		return LayoutInflater.from(context).inflate(R.layout.msg_cell, null);
	}
}
