package com.pingshow.amper;

import android.database.Cursor;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Message;
import android.text.SpannableString;
import android.text.style.ImageSpan;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.pingshow.amper.contacts.ContactsOnline;
import com.pingshow.amper.contacts.ContactsQuery;
import com.pingshow.amper.db.AmpUserDB;
import com.pingshow.util.AsyncImageLoader;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by jack on 2016/3/2.
 */
public class ConferenceFriendsPager extends ConferenceBasePager {
    private ListView mListView;
    private FriendsAdapter adapter;

    private float mDensity = 1.0f;
    private int mCount = 0;

    //jack db
    public List<Map<String, String>> amperList = new ArrayList<Map<String, String>>();

    private AmpUserDB mADB;
    private ContactsQuery cq;
    private ArrayList<String> excludeList;

    private AsyncImageLoader asyncImageLoader;

    public ConferenceFriendsPager(PickupActivity context) {
        super(context);

    }

    Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            Toast.makeText(context, msg.obj.toString(), Toast.LENGTH_SHORT).show();
        }
    };

    @Override
    public View initView() {
        view = View.inflate(context, R.layout.conference_container, null);
        mListView = (ListView) view.findViewById(R.id.lv_listview);
        mDensity = context.getResources().getDisplayMetrics().density;

        return view;
    }

    @Override
    public void initData() {
        //init object data
        mADB = new AmpUserDB(context);
        mADB.open();
        cq = new ContactsQuery(context);

        excludeList = (ArrayList<String>) (context.getIntent().getSerializableExtra("Exclude"));

        mListView.setOnItemClickListener(onChooseUser);

        mHandler.post(mFetchFriends);

        if (adapter == null) {
            adapter = new FriendsAdapter(context);
            mListView.setAdapter(adapter);
        } else {
            adapter.notifyDataSetChanged();
        }

    }

    //jack 释放资源
    @Override
    public void releaseSrc() {
        if (mADB != null && mADB.isOpen())
            mADB.close();
        System.gc();
        System.gc();
    }

    //jack
    @Override
    public void destory() {
        if (mADB != null && mADB.isOpen())
            mADB.close();
        amperList.clear();
        mCount=0;
        System.gc();
        System.gc();
    }

    //jack holder
    class FriendsViewHolder {
        TextView friendName;
        ImageView photoimage;
        ImageView checked;
    }

    private class FriendsAdapter extends BaseAdapter {
        public FriendsAdapter(PickupActivity context) {
            asyncImageLoader = new AsyncImageLoader(context);
        }

        @Override
        public int getCount() {
            return amperList.size();
        }

        @Override
        public Object getItem(int position) {
            return position;
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            Map<String, String> map = null;

            try {
                map = amperList.get(position);
            } catch (Exception e) {
                return convertView;
            }

            String imagePath = map.get("imagePath");

            FriendsViewHolder holder;

            if (convertView == null) {
                holder = new FriendsViewHolder();
                convertView = View.inflate(context, R.layout.user_tiny_cell_new, null);

                holder.photoimage = (ImageView) convertView.findViewById(R.id.photo);
                holder.friendName = (TextView) convertView.findViewById(R.id.friendname);
                holder.checked = (ImageView) convertView.findViewById(R.id.checked);
                convertView.setTag(holder);
            } else {
                holder = (FriendsViewHolder) convertView.getTag();
            }

            holder.photoimage.setTag(imagePath);
            Drawable cachedImage = asyncImageLoader.loadDrawable(imagePath, new AsyncImageLoader.ImageCallback() {
                public void imageLoaded(Drawable imageDrawable, String path) {
                    ImageView imageViewByTag = null;
                    imageViewByTag = (ImageView) mListView.findViewWithTag(path);
                    if (imageViewByTag != null && imageDrawable != null) {
                        imageViewByTag.setImageDrawable(imageDrawable);
                    }
                }
            });

            if (cachedImage != null && imagePath != null)
                holder.photoimage.setImageDrawable(cachedImage);
            else
                holder.photoimage.setImageResource(R.drawable.bighead);

            String disname = map.get("displayName");
            holder.friendName.setText(disname);

            String address = map.get("address");
            int status = ContactsOnline.getContactOnlineStatus(address);
            if (status > 0) {
                Drawable d = context.getResources().getDrawable(R.drawable.online_light);
                d.setBounds(0, 0, (int) (20.f * mDensity), (int) (20.f * mDensity));
                SpannableString spannable = new SpannableString("*" + disname);
                ImageSpan icon = new ImageSpan(d, ImageSpan.ALIGN_BOTTOM);
                spannable.setSpan(icon, 0, 1, SpannableString.SPAN_EXCLUSIVE_EXCLUSIVE);
                holder.friendName.setText(spannable);
            }

            String checked = map.get("checked");
            if (checked.equals("1"))
                holder.checked.setVisibility(View.VISIBLE);
            else
                holder.checked.setVisibility(View.GONE);

            return convertView;
        }
    }

    Runnable mFetchFriends = new Runnable() {
        public void run() {
            if (amperList.size()!=0) {
                return;
            }
            Cursor c = mADB.fetchAllByTime();
            if (c != null && c.moveToFirst()) {
                do {
                    String address = c.getString(1);
                    if (address.startsWith("[<GROUP>]")) continue;
                    int idx = c.getInt(3);
                    if (idx < 50) continue;
                    if (excludeList != null)//alec Exclude some users
                    {
                        boolean found = false;
                        try {
                            for (String a : excludeList) {
                                if (Integer.parseInt(a) == idx) {
                                    found = true;
                                    break;
                                }
                            }
                        } catch (Exception e) {
                        }
                        if (found) continue;
                    }
                    long contactId = cq.getContactIdByNumber(address);
                    String disName = "";
                    String userphotoPath = Global.SdcardPath_inbox + "photo_" + idx + ".jpg";
                    File f = new File(userphotoPath);
                    if (!f.exists()) userphotoPath = null;

                    if (contactId > 0)
                        disName = cq.getNameByContactId(contactId);
                    else
                        disName = c.getString(4);

                    if (disName == null || disName.length() == 0)
                        disName = context.getString(R.string.unknown_person);

                    HashMap<String, String> map = new HashMap<String, String>();

                    map.put("displayName", disName);
                    map.put("address", address);
                    map.put("idx", idx + "");
                    map.put("checked", "0");
                    map.put("imagePath", userphotoPath);

                    amperList.add(map);
                } while (c.moveToNext());

                c.close();
            }

            mHandler.post(new Runnable() {
                public void run() {
                    adapter.notifyDataSetChanged();
                }
            });
        }
    };

    OnItemClickListener onChooseUser = new OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> parent, View view,
                                int position, long id) {
            Map<String, String> map = amperList.get(position);
            if (map.get("checked").equals("0")) {
                if (mCount >= 15) return;
                map.put("checked", "1");
                mCount++;
            } else {
                map.put("checked", "0");
                mCount--;
            }
            adapter.notifyDataSetInvalidated();
            context.setAireCall(amperList,mCount);
        }
    };
}
