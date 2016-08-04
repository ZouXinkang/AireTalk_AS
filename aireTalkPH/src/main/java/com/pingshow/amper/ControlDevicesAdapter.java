package com.pingshow.amper;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.pingshow.amper.bean.SmartDeviceEntity;

import java.util.List;

/**
 * Created by HwH on 2016/7/25.
 */

public class ControlDevicesAdapter extends RecyclerView.Adapter<ControlDevicesAdapter.MyViewHolder> {
    private Context mContext;
    private List<SmartDeviceEntity> mData;
    private LayoutInflater mInfater;

    //监听对象
    private OnButtonClickListener mListener;

    //定义点击事件
    public interface OnButtonClickListener {
//        void onButtonClick(int position);
        void onOpenClick(int position);
        void onCloseClick(int position);
    }

    public void setOnButtonClickListener(OnButtonClickListener listener) {
        this.mListener = listener;
    }

    public ControlDevicesAdapter(Context context, List<SmartDeviceEntity> data) {
        Log.d("ControlDevicesActivity ControlDevicesAdapter 初始化Adapter");
        this.mContext = context;
        this.mData = data;
        mInfater = LayoutInflater.from(context);
    }

    @Override
    public MyViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = mInfater.inflate(R.layout.item_control_device, parent, false);
        return new MyViewHolder(view);
    }

    @Override
    public void onBindViewHolder(MyViewHolder holder, int position) {
        String name = mData.get(position).getName();
        Log.d("ControlDevicesActivity ControlDevicesAdapter 名字: "+name);
        int status = mData.get(holder.getLayoutPosition()).getStatus();
        String str;
        if (status==0) {
            str = String.format(mContext.getString(R.string.smart_home_devices_name_status), name, "<br><font color='red'><b>"+mContext.getString(R.string.smart_home_device_status_close)+ "</b></font>");
        }else{
            str = String.format(mContext.getString(R.string.smart_home_devices_name_status), name, "<br><font color='red'><b>"+mContext.getString(R.string.smart_home_device_status_open)+ "</b></font>");
        }
        holder.mName_tv.setText(Html.fromHtml(str));
        setUpItemEvent(holder);


    }

    //点击事件的处理
    private void setUpItemEvent(final MyViewHolder holder) {

        holder.mSwitch_open_ib.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d("ControlDevicesActivity ControlDevicesAdapter 点击open: " + holder.getLayoutPosition());
                mListener.onOpenClick(holder.getLayoutPosition());
            }
        });

        holder.mSwitch_close_ib.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d("ControlDevicesActivity ControlDevicesAdapter 点击close: " + holder.getLayoutPosition());
                mListener.onCloseClick(holder.getLayoutPosition());

            }
        });
    }

    @Override
    public int getItemCount() {
        return mData.size();
    }

    public class MyViewHolder extends RecyclerView.ViewHolder {

        TextView mName_tv;
        RelativeLayout mItem_bg;
        ImageButton mSwitch_open_ib;
        ImageButton mSwitch_close_ib;

        public MyViewHolder(View itemView) {
            super(itemView);
            mItem_bg = (RelativeLayout) itemView.findViewById(R.id.item_rl_rv);
            mName_tv = (TextView) itemView.findViewById(R.id.tv_name);
            mSwitch_close_ib = (ImageButton) itemView.findViewById(R.id.switch_close_ib);
            mSwitch_open_ib = (ImageButton) itemView.findViewById(R.id.switch_open_ib);

        }
    }
}
