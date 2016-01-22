package com.pingshow.amper;

import android.content.Context;
import android.database.Cursor;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.ImageView;
import android.widget.TextView;

public class TransactionListAdapter extends CursorAdapter {

	private Context mContext;
	private DQCurrency currency;
	
	public TransactionListAdapter(Context context, Cursor c) {
		super(context, c);
		this.mContext = context;
		this.currency = new DQCurrency(context);
	}

	public final class ViewBinder {
		public TextView mTitle;
		public TextView mTime;
		public TextView mAmount;
		public ImageView mStatus;
	}

	@Override
	public void bindView(final View view, final Context context, final Cursor c) {
		ViewBinder holder = null;
		holder = new ViewBinder();
		
		holder.mTitle = (TextView) view.findViewById(R.id.title);
		holder.mTime = (TextView) view.findViewById(R.id.time);
		holder.mAmount = (TextView) view.findViewById(R.id.amount);
		holder.mStatus = (ImageView) view.findViewById(R.id.status);
		
		holder.mTitle.setText(c.getString(1));
		holder.mAmount.setText(currency.translate("USD")+" $"+c.getString(3));
		
		int status=c.getInt(7);
		if (status==1)
			holder.mStatus.setImageResource(R.drawable.okay);
		else
			holder.mStatus.setImageResource(R.drawable.gray);
		String tFormat = DateUtils.formatDateTime(mContext,
				c.getLong(2), DateUtils.FORMAT_SHOW_DATE | DateUtils.FORMAT_SHOW_TIME);
		holder.mTime.setText(tFormat);
	}
	
	@Override
	public View newView(Context context, Cursor cursor, ViewGroup parent) {
		return LayoutInflater.from(context).inflate(R.layout.transaction_cell, null);
	}
}
