package com.pingshow.amper;

import java.util.List;
import java.util.Map;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

public class CountryRatesAdapter extends BaseAdapter{

	private List<Map<String, Object>> itemList;
	private Context mContext;
	
	public CountryRatesAdapter(Context context, List<Map<String, Object>> list)
	{
		mContext=context;
		itemList=list;
	}
	
	@Override
	public int getCount() {
		return itemList.size();
	}

	@Override
	public Object getItem(int arg0) {
		return itemList.get(arg0);
	}

	@Override
	public long getItemId(int position) {
		return position;
	}

	class ViewHolder {
		TextView countryName;
		TextView rate;
	}
	
	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		Map<String, Object> map=null;
		map = itemList.get(position);
		
		ViewHolder holder;
		
		if (convertView == null) {
			holder = new ViewHolder();
			convertView = View.inflate(mContext, R.layout.rate_item_cell, null);
			
			holder.countryName = (TextView) convertView.findViewById(R.id.name);
			holder.rate = (TextView) convertView.findViewById(R.id.rate);
			convertView.setTag(holder);
		} else {
			holder = (ViewHolder) convertView.getTag();
		}

		holder.countryName.setText((String)map.get("name"));
		holder.rate.setText((String)map.get("rate"));
		
		return convertView;
	}
}
