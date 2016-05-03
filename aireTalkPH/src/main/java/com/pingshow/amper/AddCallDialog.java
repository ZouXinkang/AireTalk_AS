package com.pingshow.amper;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.app.Activity;
import android.content.AsyncQueryHandler;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.ContactsContract;
import android.text.SpannableString;
import android.text.TextUtils;
import android.text.style.ImageSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.pingshow.amper.contacts.ContactsOnline;
import com.pingshow.amper.contacts.ContactsQuery;
import com.pingshow.amper.db.AmpUserDB;
import com.pingshow.network.MyNet;
import com.pingshow.util.AsyncImageLoader;
import com.pingshow.util.AsyncImageLoader.ImageCallback;
import com.pingshow.util.MCrypt;
import com.pingshow.voip.DialerActivity;

public class AddCallDialog extends Activity {

    final int limit = 3;
    private UserItemAdapter gridAdapter;
    private List<Map<String, String>> amperList;
    private List<String> contactsList;
    private AsyncImageLoader asyncImageLoader;
    private int numColumns = 3;
    private GridView resultGridView;
    private ListView Contacts_LV;
    private AmpUserDB mADB;
    private ContactsQuery cq;
    private int mCount = 0;
    private int mCount2 = 0;
    private ArrayList<String> excludeList;
    private QueryContactHandler mContactQueryHandler;
    private Cursor mContactCursor = null;
    private ContactAdapter mContactCursorAdapter;
    private int selectionMode = 0;
    private MyPreference mPref;
    private float mDensity = 1.0f;

    Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            Toast.makeText(AddCallDialog.this, msg.obj.toString(), Toast.LENGTH_SHORT).show();
        }
    };
    private LinearLayout enter_phonenumber;
    private TextView tv_select_country;
    private EditText et_iso;
    private EditText et_number;
    private Button bt_add;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.add_call_dialog);

        mDensity = getResources().getDisplayMetrics().density;

        WindowManager.LayoutParams lp = getWindow().getAttributes();
        lp.flags |= WindowManager.LayoutParams.FLAG_DIM_BEHIND;
        lp.dimAmount = 0.5f;
        getWindow().setAttributes(lp);

        mADB = new AmpUserDB(this);
        mADB.open();

        ((ImageView) findViewById(R.id.close)).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                setResult(RESULT_CANCELED);
                finish();
            }
        });

        mADB = new AmpUserDB(this);
        mADB.open();

        numColumns = 3;

        cq = new ContactsQuery(this);

        amperList = new ArrayList<Map<String, String>>();
        contactsList = new ArrayList<String>();

        gridAdapter = new UserItemAdapter(this);

        //jack init container
        resultGridView = (GridView) findViewById(R.id.pickup);
        resultGridView.setNumColumns(numColumns);
        Contacts_LV = (ListView) findViewById(R.id.addressbook);
        enter_phonenumber = (LinearLayout) findViewById(R.id.enter_phonenumber);
        //iso
        et_iso = (EditText) findViewById(R.id.et_iso);
        //电话号码
        et_number = (EditText) findViewById(R.id.et_number);

        //jack 初始化输入号码界面
        tv_select_country = (TextView) findViewById(R.id.tv_select_country);
        tv_select_country.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(AddCallDialog.this, CountryCodeActivity.class);
                AddCallDialog.this.startActivityForResult(intent, 0);
            }
        });


        LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View v = inflater.inflate(R.layout.inflate_credit, null, false);
        Contacts_LV.addHeaderView(v);

        mPref = new MyPreference(this);
        float credit = mPref.readFloat("Credit", 0);
        TextView tv = (TextView) v.findViewById(R.id.credit);
        if (tv != null) tv.setText(String.format(getString(R.string.credit), credit));

        //excludeList=(ArrayList<String>)getIntent().getSerializableExtra("Exclude");

        resultGridView.setAdapter(gridAdapter);
        resultGridView.setOnItemClickListener(onChooseUser);

        ((ImageView) findViewById(R.id.done)).setOnClickListener(new OnClickListener() {
                                                                     public void onClick(View v) {
                                                                         int count = 0;
                                                                         if (selectionMode == 0) {
                                                                             for (int i = 0; i < amperList.size() && count < 3; i++) {
                                                                                 Map<String, String> map = amperList.get(i);
                                                                                 if (map.get("checked").equals("1")) {
                                                                                     DialerActivity.addingList.add(map.get("idx"));
                                                                                     count++;
                                                                                 }
                                                                             }
                                                                         } else if (selectionMode == 1) {
                                                                             float credit = mPref.readFloat("Credit", 0);
                                                                             if (credit < 0.010) {
                                                                                 Intent it = new Intent(AddCallDialog.this, CommonDialog.class);
                                                                                 it.putExtra("msgContent", getString(R.string.credit_not_enough));
                                                                                 it.putExtra("numItems", 1);
                                                                                 it.putExtra("ItemCaption0", getString(R.string.done));
                                                                                 it.putExtra("ItemResult0", RESULT_OK);
                                                                                 startActivity(it);
                                                                                 return;
                                                                             }

                                                                             for (int i = 0; i < contactsList.size() && count < 3; i++) {
                                                                                 String address = contactsList.get(i);
                                                                                 DialerActivity.addingList2.add(address);
                                                                                 Log.d("AddCallDialog  "+ address);
                                                                                 count++;
                                                                             }
                                                                         }else if (selectionMode ==2){
                                                                             // TODO: 2016/4/11 添加电话号码加入多方会议
                                                                             String phoneNumber = et_number.getText().toString().trim();
                                                                             String iso = et_iso.getText().toString().trim();
                                                                             if (TextUtils.isEmpty(phoneNumber) || TextUtils.isEmpty(iso)) {
                                                                                 Toast.makeText(AddCallDialog.this, AddCallDialog.this.getResources().getString(R.string.edit_input_empty), Toast.LENGTH_SHORT).show();
                                                                                 return;
                                                                             } else if (!iso.startsWith("+")) {
                                                                                 Toast.makeText(AddCallDialog.this, AddCallDialog.this.getResources().getString(R.string.iso_input_invalid), Toast.LENGTH_SHORT).show();
                                                                                 return;
                                                                             }

                                                                             String globalnumber = iso + phoneNumber;
                                                                             //拼接完成的电话号码
                                                                             Log.d("AddCallDialog  "+globalnumber);
                                                                             // TODO: 2016/4/11 关闭页面,发送电话号码加入多方会议
//                                                                             DialerActivity.addPhoneToConference(globalnumber);
                                                                             DialerActivity.addingList2.add(globalnumber);
                                                                             // TODO: 2016/4/11 添加成功
                                                                             Toast.makeText(AddCallDialog.this, AddCallDialog.this.getString(R.string.add_successful), Toast.LENGTH_SHORT).show();
                                                                         }

                                                                         finish();
                                                                     }
                                                                 }
        );

        // TODO: 2016/4/11 进入这个页面默认选择的pager
        ((ToggleButton) findViewById(R.id.address)).setChecked(false);
        ((ToggleButton) findViewById(R.id.address)).setEnabled(true);
        ((ToggleButton) findViewById(R.id.call)).setChecked(false);
        ((ToggleButton) findViewById(R.id.call)).setEnabled(true);

        ((ToggleButton) findViewById(R.id.users)).setEnabled(false);
        ((ToggleButton) findViewById(R.id.users)).setChecked(true);
        Contacts_LV.setVisibility(View.GONE);
        enter_phonenumber.setVisibility(View.GONE);
        resultGridView.setVisibility(View.VISIBLE);
        selectionMode = 0;

        ((ToggleButton) findViewById(R.id.users)).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                ((ToggleButton) findViewById(R.id.address)).setChecked(false);
                ((ToggleButton) findViewById(R.id.address)).setEnabled(true);
                ((ToggleButton) findViewById(R.id.call)).setChecked(false);
                ((ToggleButton) findViewById(R.id.call)).setEnabled(true);

                ((ToggleButton) findViewById(R.id.users)).setEnabled(false);
                Contacts_LV.setVisibility(View.GONE);
                enter_phonenumber.setVisibility(View.GONE);
                resultGridView.setVisibility(View.VISIBLE);
                selectionMode = 0;

                //隐藏键盘
                InputMethodManager imanager = (InputMethodManager) AddCallDialog.this
                        .getSystemService(Context.INPUT_METHOD_SERVICE);
                imanager.hideSoftInputFromWindow(et_iso.getWindowToken(), 0);
                imanager.hideSoftInputFromWindow(et_number.getWindowToken(), 0);
            }
        });

        ((ToggleButton) findViewById(R.id.address)).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                ((ToggleButton) findViewById(R.id.users)).setChecked(false);
                ((ToggleButton) findViewById(R.id.users)).setEnabled(true);
                ((ToggleButton) findViewById(R.id.call)).setChecked(false);
                ((ToggleButton) findViewById(R.id.call)).setEnabled(true);

                ((ToggleButton) findViewById(R.id.address)).setEnabled(false);
                Contacts_LV.setVisibility(View.VISIBLE);
                resultGridView.setVisibility(View.GONE);
                enter_phonenumber.setVisibility(View.GONE);
                selectionMode = 1;

                //隐藏键盘
                InputMethodManager imanager = (InputMethodManager) AddCallDialog.this
                        .getSystemService(Context.INPUT_METHOD_SERVICE);
                imanager.hideSoftInputFromWindow(et_iso.getWindowToken(), 0);
                imanager.hideSoftInputFromWindow(et_number.getWindowToken(), 0);
            }
        });

        //jack 添加输入电话号码加入会议
        ((ToggleButton) findViewById(R.id.call)).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                ((ToggleButton) findViewById(R.id.users)).setChecked(false);
                ((ToggleButton) findViewById(R.id.users)).setEnabled(true);
                ((ToggleButton) findViewById(R.id.address)).setChecked(false);
                ((ToggleButton) findViewById(R.id.address)).setEnabled(true);

                ((ToggleButton) findViewById(R.id.call)).setEnabled(false);
                Contacts_LV.setVisibility(View.GONE);
                resultGridView.setVisibility(View.GONE);

                enter_phonenumber.setVisibility(View.VISIBLE);
                selectionMode = 2;
            }
        });

        mHandler.post(mFetchFriends);
        mHandler.postDelayed(new Runnable() {
            public void run() {
                onContactQuery();
            }
        }, 1000);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == 1) {
            String iso = data.getStringExtra("iso");
            String country = data.getStringExtra("country");
            et_iso.setText("+" + iso);
            tv_select_country.setText(this.getString(R.string.select_country).split(":")[0] + ":" + country);
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    public void onContactQuery() {
        if (mContactCursor != null && !mContactCursor.isClosed())
            mContactCursor.close();

        if (mContactQueryHandler == null)
            mContactQueryHandler = new QueryContactHandler(getContentResolver());

        mContactQueryHandler.startQuery(0, null,
                ContactsContract.Contacts.CONTENT_URI,
                new String[]{"_id", "display_name"},
                ContactsContract.Contacts.HAS_PHONE_NUMBER + "=1", null,
                ContactsContract.Contacts.LAST_TIME_CONTACTED + " desc");
    }

    private class QueryContactHandler extends AsyncQueryHandler {
        public QueryContactHandler(ContentResolver cr) {
            super(cr);
        }

        @Override
        protected void onQueryComplete(int token, Object cookie, Cursor c) {
            try {
                mContactCursor = c;

                if (mContactCursorAdapter == null) {
                    mContactCursorAdapter = new ContactAdapter(AddCallDialog.this, mContactCursor, cq);
                    Contacts_LV.setAdapter(mContactCursorAdapter);
                    Contacts_LV.setOnItemClickListener(OnContactClickListener);
                } else {
                    mContactCursorAdapter.changeCursor(mContactCursor);
                }
            } catch (Exception e) {
                Log.e("addcall onQueryComplete !@#$ " + e.getMessage());
            }
        }

        @Override
        protected void onInsertComplete(int token, Object cookie, Uri uri) {
            onContactQuery();
        }

        @Override
        protected void onUpdateComplete(int token, Object cookie, int result) {
            onContactQuery();
        }

        @Override
        protected void onDeleteComplete(int token, Object cookie, int result) {
            onContactQuery();
        }
    }

    private OnItemClickListener OnContactClickListener = new OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            try {
                ImageView checkbox = (ImageView) view.findViewById(R.id.checked);
                String address = ((TextView) view.findViewById(R.id.address)).getText().toString();
                if (checkbox.getVisibility() == View.VISIBLE) {
                    checkbox.setVisibility(View.GONE);
                    contactsList.remove(address);
                    mCount2--;
                } else {
                    if (mCount2 >= limit) return;
                    checkbox.setVisibility(View.VISIBLE);
                    contactsList.add(address);
                    mCount2++;
                }
            } catch (Exception e) {
            }
        }
    };

    OnItemClickListener onChooseUser = new OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> parent, View view,
                                int position, long id) {
            Map<String, String> map = amperList.get(position);
            if (map.get("checked").equals("0")) {
                if (mCount >= limit) return;
                map.put("checked", "1");
                mCount++;
            } else {
                map.put("checked", "0");
                mCount--;
            }

            gridAdapter.notifyDataSetInvalidated();
        }
    };


    Runnable mFetchFriends = new Runnable() {
        public void run() {
            amperList.clear();
            Cursor c = mADB.fetchAllByTime();
            if (c != null && c.moveToFirst()) {
                do {
                    String address = c.getString(1);
                    if (address.startsWith("[<GROUP>]")) continue;
                    int idx = c.getInt(3);
                    if (idx < 50) continue;
                    if (DialerActivity.memberList != null)//alec Exclude some users
                    {
                        boolean found = false;
                        try {
                            for (Map<String, Object> a : DialerActivity.memberList) {
                                if (Integer.parseInt((String) a.get("idx")) == idx) {
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
                        disName = getString(R.string.unknown_person);

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
                    gridAdapter.notifyDataSetChanged();
                }
            });
        }
    };

    @Override
    protected void onDestroy() {
        if (mADB != null && mADB.isOpen())
            mADB.close();
        amperList.clear();
        contactsList.clear();
        System.gc();
        System.gc();
        super.onDestroy();
    }

    class foundViewHolder {
        TextView friendName;
        ImageView photoimage;
        ImageView checked;
    }

    public class UserItemAdapter extends BaseAdapter {
        Context icontext;

        public UserItemAdapter(Context context) {
            icontext = context;
            asyncImageLoader = new AsyncImageLoader(context);
        }

        @Override
        public int getCount() {
            int count = amperList.size();
            return count;
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

            foundViewHolder holder;

            if (convertView == null) {
                holder = new foundViewHolder();
                convertView = View.inflate(icontext, R.layout.user_add_cell, null);

                holder.photoimage = (ImageView) convertView.findViewById(R.id.photo);
                holder.friendName = (TextView) convertView.findViewById(R.id.friendname);
                holder.checked = (ImageView) convertView.findViewById(R.id.checked);
                convertView.setTag(holder);
            } else {
                holder = (foundViewHolder) convertView.getTag();
            }

            holder.photoimage.setTag(imagePath);
            Drawable cachedImage = asyncImageLoader.loadDrawable(imagePath, new ImageCallback() {
                public void imageLoaded(Drawable imageDrawable, String path) {
                    ImageView imageViewByTag = null;
                    imageViewByTag = (ImageView) resultGridView.findViewWithTag(path);
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
                Drawable d = getResources().getDrawable(R.drawable.online_light);
                d.setBounds(0, 0, (int) (13.f * mDensity), (int) (13.f * mDensity));
                SpannableString spannable = new SpannableString("*" + disname);
                ImageSpan icon = new ImageSpan(d, ImageSpan.ALIGN_BOTTOM);
                spannable.setSpan(icon, 0, 1, SpannableString.SPAN_EXCLUSIVE_EXCLUSIVE);
                holder.friendName.setText(spannable);
            }

            String checked = map.get("checked");
            if (checked.equals("1"))
                holder.checked.setVisibility(View.VISIBLE);
            else
                holder.checked.setVisibility(View.INVISIBLE);

            return convertView;
        }
    }
}
