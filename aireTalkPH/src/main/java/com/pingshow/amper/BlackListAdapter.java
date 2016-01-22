package com.pingshow.amper;

import android.content.Context;
import android.database.Cursor;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.pingshow.util.ImageUtil;

public class BlackListAdapter extends CursorAdapter {

	private Context mContext;
	public boolean showDeleteIcon;
	private BitmapDrawable empty;
	private BitmapDrawable group_empty;
	
	public BlackListAdapter(Context context, Cursor c) {
		super(context, c);
		this.mContext = context;
		try{
			empty = new BitmapDrawable(BitmapFactory.decodeResource(mContext.getResources(), R.drawable.bighead));
			group_empty = new BitmapDrawable(BitmapFactory.decodeResource(mContext.getResources(), R.drawable.group_empty));
		}catch (OutOfMemoryError e){
			empty = new BitmapDrawable(BitmapFactory.decodeResource(mContext.getResources(), R.drawable.empty));
		}
	}

	public final class ViewBinder {
		public ImageView vPhoto;
		public TextView vDisplayName;
	}

	@Override
	public void bindView(final View view, final Context context, final Cursor c) {
		ViewBinder holder = null;
		holder = new ViewBinder();
		holder.vPhoto = (ImageView) view.findViewById(R.id.photo);
		holder.vDisplayName = (TextView) view.findViewById(R.id.displayname);
		String displayname=c.getString(2);
		boolean inGroup=c.getString(1).startsWith("[<GROUP>]");//alec
		int idx=c.getInt(3);
		Drawable photo=null;
		String path = Global.SdcardPath_inbox+"photo_"+idx+".jpg";
		photo=ImageUtil.getBitmapAsRoundCorner(path,1,5);//alec
		if (photo==null)
		{
			if (inGroup)
				photo=group_empty;
			else
				photo=empty;
		}
		
		if (inGroup)
			displayname=mContext.getResources().getString(R.string.the_group)+": "+displayname;
			
		holder.vPhoto.setImageDrawable(photo);
		
		if (displayname==null)
			displayname=mContext.getString(R.string.unknown_person);
		
		holder.vDisplayName.setText(displayname);
		
		view.setTag(c.getString(1));
	}
	
	@Override
	public View newView(Context context, Cursor cursor, ViewGroup parent) {
		return LayoutInflater.from(context).inflate(R.layout.blacklist_cell, null);
	}
}
