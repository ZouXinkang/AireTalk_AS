package com.pingshow.amper;

import android.content.Context;
import android.database.Cursor;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.ImageView;
import android.widget.TextView;

public class StudioGroupsAdapter extends CursorAdapter {
	
	private int curSel=-1;
	private Drawable lockDrawable;
	
	public StudioGroupsAdapter(Context context, Cursor c) {
		super(context, c);
		lockDrawable=context.getResources().getDrawable(R.drawable.lock);
	}

	public final class ViewBinder {
		public TextView vDisplayName;
		public TextView vAuthor;
		public TextView vHot;
		public ImageView vLock;
	}
	
	public void setSel(int sel)
	{
		curSel=sel;
	}
	
	public int getSel()
	{
		return curSel;
	}

	@Override
	public void bindView(final View view, final Context context, final Cursor c) {
		ViewBinder holder = null;
		holder = new ViewBinder();
		
		holder.vDisplayName = (TextView) view.findViewById(R.id.displayname);
		holder.vAuthor = (TextView) view.findViewById(R.id.author);
		holder.vHot = (TextView) view.findViewById(R.id.hot);
		holder.vLock = (ImageView) view.findViewById(R.id.lock);
		String displayname=c.getString(1);
		String Author="";
		try{
			int id=displayname.indexOf(" (by ");
			if (id>0)
			{
				Author=displayname.substring(id+5, displayname.indexOf(")", displayname.length()-1));
				displayname=displayname.substring(0, id);
			}
		}catch(Exception e){
			displayname=c.getString(1);
			Author="123";
		}
		
		holder.vDisplayName.setText(displayname);
		holder.vAuthor.setText(Author);
		int hot=c.getInt(3);
		holder.vHot.setText(hot+"");
		
		view.setTag(c.getString(2));
		
		int lock=c.getInt(4);
		if (lock>0)
			holder.vLock.setImageDrawable(lockDrawable);
		else
			holder.vLock.setImageDrawable(null);
		
		if (curSel==c.getPosition())
			view.setBackgroundColor(0x4030C0C0);
		else
			view.setBackgroundColor(0x00FFFFFF);
	}
	
	@Override
	public View newView(Context context, Cursor cursor, ViewGroup parent) {
		return LayoutInflater.from(context).inflate(R.layout.studio_group_cell, null);
	}
}
