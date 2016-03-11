package com.pingshow.amper;

import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.pingshow.amper.bean.Person;

import java.util.List;

public class ConferenceContactAdapter extends BaseAdapter{


	private final PickupActivity context;
	private final List<Person> contacts;

	public ConferenceContactAdapter(PickupActivity context, List<Person> contacts) {
		this.context = context;
		this.contacts = contacts;
	}

	@Override
	public int getCount() {
		return contacts.size();
	}

	@Override
	public Object getItem(int position) {
		return contacts.get(position);
	}

	@Override
	public long getItemId(int position) {
		return position;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		final ViewHolder holder;
		if (convertView == null) {
			holder = new ViewHolder();
			convertView = View.inflate(context, R.layout.conference_contact_cell, null);
			holder.vDisplayName = (TextView) convertView.findViewById(R.id.displayname);
			holder.vAddress = (TextView) convertView.findViewById(R.id.address);
			holder.vPhoto = (ImageView) convertView.findViewById(R.id.photo);
			holder.vCost = (TextView) convertView.findViewById(R.id.cost);
			holder.vChecked = (ImageView) convertView.findViewById(R.id.checked);

			convertView.setTag(holder);
		} else {
			holder = (ViewHolder) convertView.getTag();
		}

		Person person = contacts.get(position);
		holder.vDisplayName.setText(person.getName());
		holder.vPhoto.setImageDrawable(person.getPhoto());
		holder.vAddress.setText(person.getAddress());
		holder.vCost.setVisibility(View.INVISIBLE);

		if (person.getChecked()==0){
			holder.vChecked.setVisibility(View.GONE);
		}else{
			holder.vChecked.setVisibility(View.VISIBLE);
		}


		return convertView;
	}

	public class ViewHolder {
		public TextView vDisplayName;
		public TextView vAddress;
		public ImageView vPhoto;
		public TextView vCost;
		public ImageView vChecked;
	}
}
