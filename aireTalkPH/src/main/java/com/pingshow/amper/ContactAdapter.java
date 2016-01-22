package com.pingshow.amper;

import android.content.Context;
import android.database.Cursor;
import android.graphics.drawable.Drawable;
import android.provider.ContactsContract;
import android.provider.ContactsContract.CommonDataKinds;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.Filterable;
import android.widget.ImageView;
import android.widget.TextView;

import com.pingshow.amper.contacts.ContactsQuery;

public class ContactAdapter extends CursorAdapter implements Filterable{

	private ContactsQuery cq;
	Context mContext;
	private Drawable empty;
	private DQCurrency currency;
	
	public static final String[] CONTACTS_PROJECTION = new String[] {
		"_id","contact_id","display_name","data1"
    };
//	"_id","contact_id","display_name","data1"

	public ContactAdapter(Context context, Cursor c, ContactsQuery _cq) {
		super(context, c);
		cq=_cq;
		mContext=context;
		currency=new DQCurrency(context);
		empty=context.getResources().getDrawable(R.drawable.bighead);
	}

	public final class ViewBinder {
		public TextView vDisplayName;
		public TextView vAddress;
		public ImageView vPhoto;
		public TextView vCost;
	}

	@Override
	public void bindView(final View view, final Context context, final Cursor c) 
	{
		ViewBinder holder = new ViewBinder();
		
		if (c.isClosed() || c==null) return;
		
		holder.vDisplayName = (TextView) view.findViewById(R.id.displayname);
		holder.vAddress= (TextView) view.findViewById(R.id.address);
		holder.vPhoto= (ImageView) view.findViewById(R.id.photo);
		holder.vCost=(TextView) view.findViewById(R.id.cost);
		
		boolean bCallLogView=false;
		long contactId=-1;
		String address="";
		int cc=c.getColumnCount();
		if (cc==4)//CommonDataKinds.Phone.CONTACT_ID)
		{
			contactId=c.getLong(c.getColumnIndex("contact_id"));
			address=c.getString(c.getColumnIndex("data1"));
		}
		else if (cc==10)//Call Log
		{
			address=c.getString(2);
			contactId=c.getLong(3);
			bCallLogView=true;
			
		}else{
			contactId=c.getLong(c.getColumnIndex(ContactsContract.Contacts._ID));
			address=cq.getPrimaryNumberByContactId(contactId);
		}
		
		String displayname=c.getString(c.getColumnIndex("display_name"));
		holder.vDisplayName.setText(displayname);
		
		if (contactId>0)
			holder.vPhoto.setImageDrawable(cq.getPhotoById(mContext, contactId, false));
		else
			holder.vPhoto.setImageDrawable(empty);
		
		if (bCallLogView)
		{
			String tFormat = DateUtils.formatDateTime(mContext,
					c.getLong(4), DateUtils.FORMAT_SHOW_DATE | DateUtils.FORMAT_SHOW_TIME | DateUtils.FORMAT_CAP_AMPM);
			int sec=c.getInt(5);
			if (sec>0)
			{
				float cost=c.getFloat(6);
				if (cost<0)
					holder.vCost.setText(R.string.free);
				else
					holder.vCost.setText(String.format("%1$s $%2$.3f",currency.translate("USD"), cost));
				holder.vCost.setVisibility(View.VISIBLE);
			}
			else
				holder.vCost.setVisibility(View.INVISIBLE);
			String duration=context.getResources().getString(R.string.call_duration)+" "+DateUtils.formatElapsedTime(sec);
			holder.vAddress.setText(tFormat+" "+duration);
		}else{
			holder.vCost.setVisibility(View.INVISIBLE);
			holder.vAddress.setText(address);
		}
		view.setTag(address);
	}

	@Override
	public View newView(Context context, Cursor cursor, ViewGroup parent) {
		return LayoutInflater.from(context).inflate(R.layout.contact_cell, null);
	}

}
