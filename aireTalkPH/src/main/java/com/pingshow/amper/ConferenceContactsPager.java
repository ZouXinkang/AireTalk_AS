package com.pingshow.amper;

import android.content.AsyncQueryHandler;
import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.database.MergeCursor;
import android.net.Uri;
import android.os.Build;
import android.provider.ContactsContract;
import android.text.format.DateUtils;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.ProgressBar;

import com.pingshow.amper.bean.Person;
import com.pingshow.amper.contacts.ContactsQuery;
import com.pingshow.amper.db.AireCallLogDB;


import java.util.ArrayList;
import java.util.List;

/**
 * Created by jack on 2016/3/2.
 */
public class ConferenceContactsPager extends ConferenceBasePager {

    private ListView mListView;
    private AireCallLogDB mCLDB;
    private ConferenceContactAdapter mContactCursorAdapter;

    private QueryContactHandler mContactQueryHandler;
    private MyPreference mPref;
    private int sortMethod = 1;//jack 2.4.51排序目前没上
    //phoneCall集合
    private int mCount = 0 ;

    private DQCurrency currency = new DQCurrency(context);

    protected Cursor mContactCursor = null;

    private ContactsQuery cq;
    private ProgressBar pb_wait;

    public ConferenceContactsPager(Context context) {
        super((PickupActivity) context);
    }

    private List<Person> contacts = new ArrayList<Person>();

    @Override
    public View initView() {
        view = View.inflate(context, R.layout.conference_container, null);
        mListView = (ListView) view.findViewById(R.id.lv_listview);
        pb_wait = (ProgressBar) view.findViewById(R.id.pb_wait);
        pb_wait.setVisibility(View.VISIBLE);
        mListView.setOnItemClickListener(OnContactClickListener);
        mCLDB = new AireCallLogDB(context);
        mCLDB.open();

        return view;
    }

    @Override
    public void initData() {
        //jack query
        cq = new ContactsQuery(context);
        mPref = new MyPreference(context);
        sortMethod = mPref.readInt("ContactsSortMethod", 1);
        if (contacts.size() == 0)
            onContactQuery();

    }

    @Override
    public void releaseSrc() {
        if (mContactCursor != null && !mContactCursor.isClosed()) {
            try {
                if (Build.VERSION.SDK_INT < 14) {
                    mContactCursor.close();
                    mContactCursor = null;
                }
            } catch (Exception e) {
                Log.e("airecall onDestroy !@#$ " + e.getMessage());
            }
        }

        if (mCLDB != null && mCLDB.isOpen())
            mCLDB.close();
        System.gc();
        System.gc();
    }

    @Override
    public void destory() {
        try {
            if (Build.VERSION.SDK_INT < 14) {
                mContactCursor.close();
                mContactCursor = null;
            }
        } catch (Exception e) {
            Log.e("airecall onDestroy !@#$ " + e.getMessage());
        }

    if (mCLDB != null && mCLDB.isOpen())
            mCLDB.close();
        for (Person p : contacts){
            p.setChecked(0);
        }
        if (mContactCursorAdapter!=null)
        mContactCursorAdapter.notifyDataSetChanged();

        mCount=0;
        System.gc();
        System.gc();
    }

    private void onContactQuery() {
        if (mContactQueryHandler == null)
            mContactQueryHandler = new QueryContactHandler(context.getContentResolver());

        if (sortMethod == 0) {
            mContactQueryHandler.startQuery(0, null,
                    ContactsContract.Contacts.CONTENT_URI,
                    new String[]{"_id", "display_name"},
                    ContactsContract.Contacts.HAS_PHONE_NUMBER + "=1", null,
                    ContactsContract.Contacts.DISPLAY_NAME +
                            " COLLATE LOCALIZED ASC");
        } else {
            mContactQueryHandler.startQuery(0, null,
                    ContactsContract.Contacts.CONTENT_URI,
                    new String[]{"_id", "display_name"},
                    ContactsContract.Contacts.HAS_PHONE_NUMBER + "=1", null,
                    ContactsContract.Contacts.LAST_TIME_CONTACTED + " desc");
        }
    }

    private class QueryContactHandler extends AsyncQueryHandler {
        public QueryContactHandler(ContentResolver cr) {
            super(cr);
        }

        @Override
        protected void onQueryComplete(int token, Object cookie, Cursor c) {

            try {
                if (mContactCursor != null) {
                    try {
                        if (Build.VERSION.SDK_INT < 14) {
                            mContactCursor.close();
                        }
                    } catch (Exception e) {
                        Log.e("airecall onQueryComplete !@#$ " + e.getMessage());
                    }
                }


                Cursor[] cursor = new Cursor[2];
                cursor[0] = mCLDB.fetch();
                cursor[1] = c;

                mContactCursor = new MergeCursor(cursor);

                //jack 组合数据
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        while (mContactCursor.moveToNext()) {
                            Person person = new Person();
                            boolean bCallLogView = false;
                            long contactId = -1;
                            String address = "";
                            int cc = mContactCursor.getColumnCount();
                            if (cc == 4)//CommonDataKinds.Phone.CONTACT_ID)
                            {
                                contactId = mContactCursor.getLong(mContactCursor.getColumnIndex("contact_id"));
                                address = mContactCursor.getString(mContactCursor.getColumnIndex("data1"));
                            } else if (cc == 10)//Call Log
                            {
                                address = mContactCursor.getString(2);
                                contactId = mContactCursor.getLong(3);
                                bCallLogView = true;

                            } else {
                                contactId = mContactCursor.getLong(mContactCursor.getColumnIndex(ContactsContract.Contacts._ID));
                                address = cq.getPrimaryNumberByContactId(contactId);
                            }
                            int display_name = mContactCursor.getColumnIndex("display_name");
                            String displayname = mContactCursor.getString(display_name);
                            person.setName(displayname);

                            if (contactId > 0)
                                person.setPhoto(cq.getPhotoById(context, contactId, false));
                            else
                                person.setPhoto(context.getResources().getDrawable(R.drawable.bighead));

                            if (bCallLogView) {
                                String tFormat = DateUtils.formatDateTime(context,
                                        mContactCursor.getLong(4), DateUtils.FORMAT_SHOW_DATE | DateUtils.FORMAT_SHOW_TIME | DateUtils.FORMAT_CAP_AMPM);
                                int sec = mContactCursor.getInt(5);
                                if (sec > 0) {
                                    float cost = mContactCursor.getFloat(6);
                                    if (cost < 0)
                                        person.setCost(context.getResources().getString(R.string.free));
                                    else
                                        person.setCost(String.format("%1$s $%2$.3f", currency.translate("USD"), cost));
                                }

                                String duration = context.getResources().getString(R.string.call_duration) + " " + DateUtils.formatElapsedTime(sec);
                                person.setAddress(tFormat + " " + duration);

                            } else {
                                person.setAddress(address);

                            }
                            person.setChecked(0);//未点击
                            contacts.add(person);
                        }
                        context.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                pb_wait.setVisibility(View.GONE);
                                if (mContactCursorAdapter == null) {
                                    mContactCursorAdapter = new ConferenceContactAdapter(context, contacts);
                                    mListView.setAdapter(mContactCursorAdapter);
                                    mListView.setOnItemClickListener(OnContactClickListener);
                                } else {
                                    mContactCursorAdapter.notifyDataSetChanged();
                                }
                            }
                        });
                    }
                }).start();

            } catch (Exception e) {
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


    private AdapterView.OnItemClickListener OnContactClickListener = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            Person person = contacts.get(position);
            if (person.getChecked() == 0) {
                person.setChecked(1);
                mCount++;
            } else {
                person.setChecked(0);
                mCount--;
            }
            mContactCursorAdapter.notifyDataSetChanged();
            context.setPhoneCall(contacts,mCount);
        }
    };
}
