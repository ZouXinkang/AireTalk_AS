package com.pingshow.amper.view.SwipeMenuLayout;

import android.content.Context;
import android.database.DataSetObserver;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListAdapter;
import android.widget.TextView;
import android.widget.WrapperListAdapter;

import com.pingshow.amper.view.indexableListView.IndexBarAdapter;
import com.pingshow.amper.view.indexableListView.IndexEntity;


/**
 * 
 * @author baoyz
 * @date 2014-8-24
 * 
 */
public class SwipeMenuAdapter extends IndexBarAdapter implements WrapperListAdapter,
		SwipeMenuView.OnSwipeItemClickListener {

	private IndexBarAdapter mAdapter;
	private Context mContext;
	private SwipeMenuListView.OnMenuItemClickListener onMenuItemClickListener;

	public SwipeMenuAdapter(Context context, ListAdapter adapter) {
		mAdapter = (IndexBarAdapter)adapter;
		mContext = context;
	}

	@Override
	public int getCount() {
		return mAdapter.getCount();
	}

	@Override
	public Object getItem(int position) {
		return mAdapter.getItem(position);
	}

	@Override
	public long getItemId(int position) {
		return mAdapter.getItemId(position);
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		Log.d("SwipeMenuAdapter", "mAdapter.getItemViewType(position):" + mAdapter.getItemViewType(position));

		SwipeMenuLayout layout = null;
		if (convertView == null) {
			View contentView = mAdapter.getView(position, convertView, parent);

			if (checkShouldSwipe(position)) return contentView;

			SwipeMenu menu = new SwipeMenu(mContext);
			menu.setViewType(mAdapter.getItemViewType(position));
			createMenu(menu);
			SwipeMenuView menuView = new SwipeMenuView(menu,
					(SwipeMenuListView) parent);
			menuView.setOnSwipeItemClickListener(this);
			SwipeMenuListView listView = (SwipeMenuListView) parent;
			layout = new SwipeMenuLayout(contentView, menuView,
					listView.getCloseInterpolator(),
					listView.getOpenInterpolator());
			layout.setPosition(position);
		} else {

			if (checkShouldSwipe(position)) return convertView;

			layout = (SwipeMenuLayout) convertView;
			layout.closeMenu();
			layout.setPosition(position);
			View view = mAdapter.getView(position, layout.getContentView(),
					parent);
		}
		return layout;
	}

	//检查是否可以侧拉
	private boolean checkShouldSwipe(int position) {
		if (mAdapter.getItemViewType(position)==TYPE_INDEX) {
			return true;
		}
		return false;
	}

	@Override
	protected TextView onCreateTitleViewHolder(ViewGroup parent) {
		return null;
	}

	@Override
	protected ViewHolder onCreateViewHolder(ViewGroup parent) {
		return null;
	}

	@Override
	protected void onBindViewHolder(ViewHolder holder, IndexEntity cityEntity) {

	}

	public void createMenu(SwipeMenu menu) {
		// Test Code
		SwipeMenuItem item = new SwipeMenuItem(mContext);
		item.setTitle("Item 1");
		item.setBackground(new ColorDrawable(Color.GRAY));
		item.setWidth(300);
		menu.addMenuItem(item);

		item = new SwipeMenuItem(mContext);
		item.setTitle("Item 2");
		item.setBackground(new ColorDrawable(Color.RED));
		item.setWidth(300);
		menu.addMenuItem(item);
	}

	@Override
	public void onItemClick(SwipeMenuView view, SwipeMenu menu, int index) {
		if (onMenuItemClickListener != null) {
			onMenuItemClickListener.onMenuItemClick(view.getPosition(), menu,
					index);
		}
	}

	public void setOnMenuItemClickListener(
			SwipeMenuListView.OnMenuItemClickListener onMenuItemClickListener) {
		this.onMenuItemClickListener = onMenuItemClickListener;
	}

	@Override
	public void registerDataSetObserver(DataSetObserver observer) {
		mAdapter.registerDataSetObserver(observer);
	}

	@Override
	public void unregisterDataSetObserver(DataSetObserver observer) {
		mAdapter.unregisterDataSetObserver(observer);
	}

	@Override
	public boolean areAllItemsEnabled() {
		return mAdapter.areAllItemsEnabled();
	}

	@Override
	public boolean isEnabled(int position) {
		return mAdapter.isEnabled(position);
	}

	@Override
	public boolean hasStableIds() {
		return mAdapter.hasStableIds();
	}

	@Override
	public int getItemViewType(int position) {
		return mAdapter.getItemViewType(position);
	}

	@Override
	public int getViewTypeCount() {
		return mAdapter.getViewTypeCount();
	}

	@Override
	public boolean isEmpty() {
		return mAdapter.isEmpty();
	}

	@Override
	public ListAdapter getWrappedAdapter() {
		return mAdapter;
	}

	public IndexBarAdapter getRealAdapter() {
		return mAdapter;
	}
}
