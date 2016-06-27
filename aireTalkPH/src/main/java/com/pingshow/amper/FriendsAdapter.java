package com.pingshow.amper;

import android.content.Context;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.pingshow.amper.bean.FriendsEntity;
import com.pingshow.amper.contacts.ContactsOnline;
import com.pingshow.amper.view.indexableListView.IndexBarAdapter;
import com.pingshow.util.AsyncImageLoader;


/**
 * Created by jack on 2016/5/20.
 */

public class FriendsAdapter extends IndexBarAdapter<FriendsEntity> {
    private UsersNewActivity mContext;
    private AsyncImageLoader asyncImageLoader;
    private final MyPreference mPref;
    private boolean largeScreen;
    private float mDensity;
    private View convertView;

    public FriendsAdapter(Context context, boolean largeScreen, float mDensity) {
        this.mContext = (UsersNewActivity)context;
        asyncImageLoader = new AsyncImageLoader(context);
        mPref = new MyPreference(context);

        //屏幕相关
        this.largeScreen = largeScreen;
        this.mDensity = mDensity;
    }

    public boolean getLargeScreen(){
        return largeScreen;
    }

    @Override
    public TextView onCreateTitleViewHolder(ViewGroup parent) {
            View view = LayoutInflater.from(mContext).inflate(R.layout.item_tv_title_friends, parent, false);
            return (TextView) view.findViewById(R.id.tv_title);

    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent) {
        View itemView = LayoutInflater.from(mContext).inflate(R.layout.item_friends_details, parent, false);

        return new FriendsViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, FriendsEntity friendsEntity) {
        FriendsViewHolder myHolder = (FriendsViewHolder) holder;
        //显示图像
        DisplayImageOptions options = new DisplayImageOptions.Builder()
                .showImageForEmptyUri(R.drawable.empty)
                .showImageOnFail(R.drawable.empty)
                .cacheInMemory(true) //
                .cacheOnDisk(true)
                .build();
        if (!TextUtils.isEmpty(friendsEntity.getName())) {
            if (friendsEntity.getName().equals("☆")) {
                if (friendsEntity.getDisplayName().equals(mContext.getString(R.string.new_friends))) {
                    myHolder.photoimage.setImageDrawable(mContext.getResources().getDrawable(R.drawable.default_fmessage));
                    myHolder.friendName.setText(friendsEntity.getDisplayName());
                }else if (friendsEntity.getDisplayName().equals(mContext.getString(R.string.saved_groups))){
                    myHolder.photoimage.setImageDrawable(mContext.getResources().getDrawable(R.drawable.default_chatroom));
                    myHolder.friendName.setText(friendsEntity.getDisplayName());
                }
                myHolder.online.setVisibility(View.GONE);
//             convertView.setBackgroundResource(R.drawable.null_draw);

            }else{
                android.util.Log.d("FriendsAdapter", "path: "+friendsEntity.getImagePath());
                ImageLoader.getInstance().displayImage("file://" + friendsEntity.getImagePath(), myHolder.photoimage, options);
                //设置分隔
                myHolder.separator.setVisibility(View.GONE);
                //设置用户名
                myHolder.friendName.setText(friendsEntity.getDisplayName());
                //设置安防管家
                setSecurityiam(myHolder, friendsEntity.getAddress());
                //设置个人状态
                setFriendsStatus(myHolder, friendsEntity.getAddress(), friendsEntity.getActual(), friendsEntity.getMood());
            }
        }
    }


    /**
     * 设置好友状态
     *
     * @param myHolder
     * @param address  地址
     * @param myActual
     * @param mood     心情
     */
    private void setFriendsStatus(FriendsViewHolder myHolder, String address, String myActual, String mood) {
        int actual = Integer.parseInt(myActual);
        if (actual == 1) {
            myHolder.online.setVisibility(View.VISIBLE);
            int status = ContactsOnline.getContactOnlineStatus(address);
            if (status > 0)
                myHolder.online.setImageResource(R.drawable.online);
            else
                myHolder.online.setImageResource(R.drawable.offline);
            myHolder.mood.setText(mood);

//            convertView.setBackgroundResource(R.drawable.null_draw);
        } else {
            myHolder.online.setVisibility(View.INVISIBLE);
            myHolder.mood.setText("");
//            convertView.setBackgroundResource(R.drawable.lightblue_draw);
        }
//        myHolder.photoimage.setBackgroundResource(R.drawable.empty);
    }

    /**
     * 设置安防管家
     *
     * @param myHolder
     * @param address
     */
    private void setSecurityiam(FriendsViewHolder myHolder, String address) {
        //tml*** beta ui, security
        int maxSuvei = Global.MAX_SUVS;
        boolean securityman = false;
        for (int i = 0; i < maxSuvei; i++) {
            String thisaddress;
            if ((thisaddress = mPref.read("Suvei" + i)) != null) {
                if (thisaddress.equals(address)) {
                    //tml*** suv onoff alert
                    String addr = mPref.read("SuveiON" + i);
                    boolean suvON = addr.equals(address);
//						Log.i("securityiam=" + address + " on:" + suvON);
                    if (suvON) {
                        myHolder.securityiam.setImageResource(R.drawable.security_mark);
                        if (largeScreen) {
                            myHolder.securityiam.setPadding(0, 0,
                                    (int) (15 * mDensity), (int) (15 * mDensity));
                        } else {
                            myHolder.securityiam.setPadding(0, 0,
                                    (int) (10 * mDensity), (int) (10 * mDensity));
                        }

                    } else {
                        myHolder.securityiam.setImageResource(R.drawable.security_mark2);
                        if (largeScreen) {
                            myHolder.securityiam.setPadding((int) (5 * mDensity), 0,
                                    (int) (25 * mDensity), (int) (25 * mDensity));
                        } else {
                            myHolder.securityiam.setPadding((int) (5 * mDensity), 0,
                                    (int) (20 * mDensity), (int) (20 * mDensity));
                        }
                    }

                    myHolder.securityiam.setVisibility(View.VISIBLE);
                    securityman = true;
                    break;
                }
            }
        }
        if (!securityman) {
            myHolder.securityiam.setVisibility(View.GONE);
        }
        Log.d("设置安防管家");
    }

    class FriendsViewHolder extends ViewHolder {
        private ImageView photoimage;
        private TextView friendName;
        private TextView separator;
        private ImageView delete;
        private ImageView securityiam;
        private TextView mask;
        private ImageView online;
        private TextView mood;
        private TextView tv_unread;
//        private TextView tv_block;//黑名单
//        private TextView tv_delete;//删除

        public FriendsViewHolder(View view) {
            super(view);
            photoimage = (ImageView) view.findViewById(R.id.photo);
            friendName = (TextView) view.findViewById(R.id.friendname);
            separator = (TextView) view.findViewById(R.id.separator);
//            delete = (ImageView) view.findViewById(R.id.delete);
            securityiam = (ImageView) view.findViewById(R.id.security_iam);  //tml*** beta ui, security
//            mask = (TextView) view.findViewById(R.id.mask);
            online = (ImageView) view.findViewById(R.id.online);
            mood = (TextView) view.findViewById(R.id.mood);
            tv_unread = (TextView) view.findViewById(R.id.tv_unread);
//            tv_block = view.findViewById(R.id.tv_block);
//            tv_delete = view.findViewById(R.id.tv_delete);

        }
    }
}
