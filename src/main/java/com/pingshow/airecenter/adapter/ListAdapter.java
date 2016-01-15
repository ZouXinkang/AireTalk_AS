package com.pingshow.airecenter.adapter;

import java.util.List;

import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

import com.pingshow.airecenter.Log;
import com.pingshow.airecenter.holder.BaseHolder;

/**
 * 
 * 对Adapter进行了简单的封装
 * 
 * @author Li
 *
 * @param <M> 数据模型
 * @param <H> ViewHolder
 */
public abstract class ListAdapter<M,H extends BaseHolder<M>> extends BaseAdapter {

	protected List<M> list;

	public ListAdapter(List<M> list){
		this.list = list;
	}
	
	@Override
	public int getCount() {
		return getList() == null ? 0 : getList().size();
	}

	@Override
	public Object getItem(int position) {
		return null;
	}

	@Override
	public long getItemId(int position) {
		return 0;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		H holder = null;
		if(convertView == null){
			try {
				holder = initHolder();
				convertView = holder.contentView;
				convertView.setTag(holder);
			} catch (Exception e) {
				Log.e("实例化ViewHolder出错！Msg: "+ e.getMessage());
				return null;
			}
		}else{
			holder = (H)convertView.getTag();
		}
		holder.position = position;
		holder.setView(list.get(position));
		handleView(position, holder);
		return convertView;
	}
	
	/**
	 * 根据位置特殊处理holder的方法
	 * @param position
	 * @param view
	 */
	public  void handleView(int position,H holder){
		
	}
	/**
	 * 需要在这里初始化ViewHolder
	 * @return
	 */
	public abstract H initHolder();
	
	/**
	 * 获取数据模型
	 * @return
	 */
	public List<M> getList() {
		return list;
	}
	/**
	 * 改变数据，这里会调用notifyDataSetChanged()
	 * @param list
	 */
	public void changeList(List<M> list) {
		this.list = list;
		notifyDataSetChanged();
	}

}
