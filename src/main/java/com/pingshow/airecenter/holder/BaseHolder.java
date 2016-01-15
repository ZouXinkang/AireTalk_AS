package com.pingshow.airecenter.holder;

import android.view.View;

/**
 * 
 * 所有view的超类
 * @author Li
 *
 * @param <M> 数据模型
 */
public abstract class BaseHolder<M>{
	
	/**
	 * 当前holder所有View的共同父View。
	 */
	public View contentView;
	/**
	 * 当前holder的位置
	 */
	public int position;
	/**
	 * 通过数据模型设置view
	 * @param model
	 */
	public abstract void setView(M model);
}
