package com.pingshow.airecenter;

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
import android.media.ToneGenerator;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.ContactsContract;
import android.provider.ContactsContract.CommonDataKinds;
import android.text.Editable;
import android.text.SpannableString;
import android.text.TextWatcher;
import android.text.style.ImageSpan;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FilterQueryProvider;
import android.widget.FrameLayout;
import android.widget.GridView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;

import com.pingshow.airecenter.R;
import com.pingshow.airecenter.contacts.ContactsOnline;
import com.pingshow.airecenter.contacts.ContactsQuery;
import com.pingshow.airecenter.db.AmpUserDB;
import com.pingshow.airecenter.view.DigitTextView;
import com.pingshow.util.AsyncImageLoader;
import com.pingshow.util.MyTelephony;
import com.pingshow.util.AsyncImageLoader.ImageCallback;

public class SelectUserActivity extends Activity implements FilterQueryProvider{
	
	private static SelectUserActivity theSelectUser;
	private UserItemAdapter gridAdapter;
	private List<Map<String, String>> amperList;
	private List<Map<String, String>> orgList;
	private List<String> excludeList;
	private AsyncImageLoader asyncImageLoader;
	private MyPreference mPref;
	private GridView mGridView;
	private AmpUserDB mADB;
	private ContactsQuery cq;
	private int mCount=0;
	private float mDensity=1.0f;
	private int mLimit=1;
	public String number="";
	private DigitTextView tNumberField;
	
	private QueryContactHandler mContactQueryHandler;
	private ContactAdapter mContactCursorAdapter;
	protected Cursor mContactCursor = null;
	private ListView Contacts_LV;

	private int spinISOcpos = -1; // spinner position = null
	private String spinISOitem;
	private String spinISOccode; // + 86
	private Spinner spinnerISO;
	
	private int funcPage=0;
	
	final int keyarray[]={R.id.key0,R.id.key1,R.id.key2,R.id.key3,R.id.key4,R.id.key5,R.id.key6,
			R.id.key7,R.id.key8,R.id.key9};
	private ToneGenerator tg=null;
	
	Handler mHandler = new Handler(){
		@Override
		public void handleMessage(Message msg) {
			super.handleMessage(msg);
		}
	};
	
	public static SelectUserActivity instance () {
		return theSelectUser;
	}
	
	protected void onCreate(Bundle bundle) {
	    super.onCreate(bundle);
	    requestWindowFeature(Window.FEATURE_NO_TITLE);
	    setContentView(R.layout.select_users);
	    theSelectUser = this;
	    
	    boolean xlarge=(findViewById(R.id.large)!=null);
	    
	    mDensity = getResources().getDisplayMetrics().density;
	    WindowManager.LayoutParams lp = getWindow().getAttributes();
	    lp.flags|=WindowManager.LayoutParams.FLAG_DIM_BEHIND;
		lp.dimAmount = 0.5f;
		lp.width=(int)(mDensity*(xlarge?920:720));
		lp.height=(int)(mDensity*(xlarge?620:500));
		getWindow().setAttributes(lp);
	    
	    mADB = new AmpUserDB(this);
		mADB.open();

		mPref = new MyPreference(this);
		
		mLimit=getIntent().getIntExtra("limit", 1);
		excludeList=getIntent().getExtras().getStringArrayList("exclude");
	    
	    ((Button)findViewById(R.id.func_contact)).setOnClickListener(new OnClickListener() {
    		public void onClick(View v)
    		{
    			funcPage=0;
    			loadFuncPage(funcPage);
    		}
	    });
	    
	    ((Button)findViewById(R.id.func_addressbook)).setOnClickListener(new OnClickListener() {
    		public void onClick(View v)
    		{
    			funcPage=1;
    			loadFuncPage(funcPage);
    		}
	    });
	    
	    ((Button)findViewById(R.id.func_number)).setOnClickListener(new OnClickListener() {
    		public void onClick(View v)
    		{
    			funcPage=2;
    			loadFuncPage(funcPage);
    		}
	    });
	    
	    ((Button)findViewById(R.id.cancel)).setOnClickListener(new OnClickListener() {
    		public void onClick(View v)
    		{
    			setResult(RESULT_CANCELED);
    			finish();
    		}
	    });
	    
	    ((EditText)findViewById(R.id.keyword)).addTextChangedListener (new TextWatcher() {
	        @Override
	        public void afterTextChanged (Editable s) {
	        	onFilterUserQuery(s.toString());
	        	
	        	if (s.toString().length()==0)
	        	{
	        		((ImageView)findViewById(R.id.clear)).setVisibility(View.GONE);
	        		((EditText)findViewById(R.id.keyword)).setPadding((int)(16.f*mDensity), (int)(6.f*mDensity),(int)(16.f*mDensity), (int)(6.f*mDensity));
	        	}
	        	else{
	        		((ImageView)findViewById(R.id.clear)).setVisibility(View.VISIBLE);
	        		((EditText)findViewById(R.id.keyword)).setPadding((int)(56.f*mDensity), (int)(6.f*mDensity),(int)(16.f*mDensity), (int)(6.f*mDensity));
	        	}
	        }

	        @Override
	        public void onTextChanged (CharSequence s, int start, int before, int count) {
	        }

	        @Override
	        public void beforeTextChanged (CharSequence s, int start, int count, int after) {
	        }
	    });
		
		((ImageView)findViewById(R.id.clear)).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				((EditText)findViewById(R.id.keyword)).setText("");
				onFilterUserQuery("");
			}
		});
    	
        ((Button)findViewById(R.id.done)).setOnClickListener(new OnClickListener() {
    		public void onClick(View v)
    		{
    			if (funcPage==0)
    			{
    				ArrayList<String> idxList = new ArrayList<String>();
    				ArrayList<String> addressList = new ArrayList<String>();
	    			int count=0;
	    			for (int i=0;i<amperList.size();i++)
	    			{
	    				Map<String, String> map = amperList.get(i);
	    				if (map.get("checked").equals("1"))
	    				{
	    					int idx=Integer.parseInt(map.get("idx"));
	    					if (idx<50) continue;
	    					String address=map.get("address");
	    					idxList.add(""+idx);
	    					addressList.add(address);
	    					count++;
	    				}
	    			}
	    			if (count>0)
	    			{
						if (idxList.size()>0 && idxList.size()<=mLimit)
						{
							Intent it=new Intent();
							it.putExtra("type", 0);
			    			it.putExtra("idxList", idxList);
			    			it.putExtra("addressList", addressList);
							setResult(RESULT_OK, it);
							finish();
						}
	    			}
    			}
    			else if (funcPage==2)
    			{
    				String _number = number;
    				if (_number.length() > 7) {
        				Log.d("SelPh# insert Call1 " + _number);
        				Intent it = new Intent();
        				it.putExtra("type", 1);
    	    			it.putExtra("result", _number);
    					setResult(RESULT_OK, it);
    					finish();
    				}
    			}
    		}
    	});
        
        amperList = new ArrayList<Map<String, String>>();
        orgList = new ArrayList<Map<String, String>>();
		cq = new ContactsQuery(this);
	    gridAdapter = new UserItemAdapter(this);
	    
	    mGridView = (GridView)findViewById(R.id.friends);
	    
	    mGridView.setAdapter(gridAdapter);
	    mGridView.setOnItemClickListener(onChooseUser);
	    
	    mHandler.post(mFetchFriends);
	    
	    
	    Contacts_LV=(ListView)findViewById(R.id.phonebook);
	    onContactQuery();
	    
	    tNumberField=(DigitTextView)findViewById(R.id.number_to_call);

        //tml|yang*** siplist country
        spinnerISO = (Spinner) findViewById(R.id.selectiso);
        String[] listISOs = getResources().getStringArray(R.array.phone_code_list);
        ArrayAdapter<String> spinISOAdapter = new ArrayAdapter<String>(SelectUserActivity.this,
                android.R.layout.simple_spinner_item, listISOs);
        String isosp = mPref.read("iso", "");
        spinISOccode = mPref.read("spinISOccode", ""); // +86
        spinISOcpos = mPref.readInt("spinISOpos", 0);
        
        spinISOAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerISO.setAdapter(spinISOAdapter);
        spinnerISO.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
        	public void onItemSelected(AdapterView<?> adapterView, View view,
        			int position, long id) {
        		String spinISOccode_prev = mPref.read("spinISOccode", "");
        		
	        	spinISOitem = (String) adapterView.getItemAtPosition(position);
	        	if (spinISOcpos != position) {
		        	spinISOcpos = position;
		        	if (spinISOcpos > 0) {
			        	spinISOccode = "+" + spinISOitem.split("\\+")[1];
		        	} else {
		        		spinISOccode = "";
		        	}
		        	mPref.write("spinISOccode", spinISOccode);
		        	mPref.write("spinISOpos", spinISOcpos);
	        		Log.i("AireCall spinnerISO=" + spinISOcpos + " " + spinISOitem + " (" + spinISOccode + ")");
	        	}

    			tNumberField.setHint(spinISOccode);
	    		if (number.length() == 0 || tNumberField.length() == 0) {
	    			number = "";
	    			return;
	    		}
	    		
	    		if (tNumberField.getText().toString().startsWith(spinISOccode_prev)) {
		    		number = spinISOccode + tNumberField.getText().toString().substring(spinISOccode_prev.length());
		    		tNumberField.setText(number);
	    		} else {
	    			if (number.contains("+")) number = number.replace("+", "");
		    		number = spinISOccode + tNumberField.getText();
		    		tNumberField.setText(number);
	    		}
        	}

        	public void onNothingSelected(AdapterView<?> view) {
        		//Log.i("spinnerISO " + view.getClass().getName());
        	}
        });
        
    	if (spinISOccode != null && spinISOcpos > -1) {
    		spinnerISO.setSelection(spinISOcpos);
    	} else if (isosp.trim().length() != 0) {
	        for (int i = 0; i < MyTelephony.COUNTRIES.length; i++) {
		        if (MyTelephony.COUNTRIES[i][0].toLowerCase().equals(isosp)) {
		        	spinISOccode = MyTelephony.COUNTRIES[i][MyTelephony.COUNTRIES[i].length - 1];
		        }
	        }
	        for (int i = 0; i < listISOs.length; i++) {
		        if (listISOs[i].contains(spinISOccode)) {
			        spinnerISO.setSelection(i);
		        }
	        }
        } else {
        	spinnerISO.setSelection(0);
        }
    	
    	((ImageView) findViewById(R.id.cleariso)).setOnClickListener(new OnClickListener(){
        	public void onClick(View v) {
                spinnerISO.setSelection(0);
    		}
        });
        //***tml
	    
	    tg=new ToneGenerator(-1,75);
	    for(int i=0;i<10;i++)
	    {
	    	Button key=(Button)findViewById(keyarray[i]);
	    	key.setId(i);
	    	key.setOnClickListener(new OnClickListener() {
	    		public void onClick(View v) {
	    			number+=v.getId();
	    			tNumberField.setText(number);
	    			tg.startTone(ToneGenerator.TONE_DTMF_0+v.getId(), 150);
		        	//tml*** siplist country
		        	if (tNumberField.length() != 0) {
		        		String spinISOccode = mPref.read("spinISOccode", "");
		        		if (spinISOccode.length() != 0 && !(tNumberField.getText().toString().startsWith(spinISOccode))) {
		        			Log.d("AireCall insert iso " + spinISOccode);
		        			number = spinISOccode + number;
		        			tNumberField.setText(spinISOccode + tNumberField.getText().toString());
		        		}
		        	}
	    		}}
	    	);
	    	if (i==0)
	    	{
	    		key.setOnLongClickListener(new OnLongClickListener() {
	        		@Override
	    			public boolean onLongClick(View v) {
//	        			number = "+" + number;
	    				//tml*** siplist country
	    				if (tNumberField.length() == 0) {
	    					number = mPref.read("spinISOccode", "");
	    					if (number.equals("")) {
			        			number = "+" + number;
	    					}
	    				} else {
	    					if (number.startsWith("+")) return true;
			        		number = "+" + number;
	    				}
	        			tNumberField.setText(number);
		    			tg.startTone(ToneGenerator.TONE_DTMF_0, 400);
	        			return true;
	    			}}
	        	);
	    	}
	    }
        
        ((ImageButton)findViewById(R.id.del)).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				if (number.equals(mPref.read("spinISOccode", ""))) return;  //tml*** siplist country
				if (number.length()>0)
				{
					number=number.substring(0,number.length()-1);
					tNumberField.setText(number);
				}
			}
		});
        ((ImageButton)findViewById(R.id.del)).setOnLongClickListener(new OnLongClickListener() {
			@Override
			public boolean onLongClick(View v) {
				number="";
				tNumberField.setText(number);
				return false;
			}
		});
        ((Button)findViewById(R.id.keyStar)).setOnClickListener(new OnClickListener() {
    		public void onClick(View v) {
    			number+="*";
    			tNumberField.setText(number);
    			tg.startTone(ToneGenerator.TONE_DTMF_S, 250);
    		}}
    	);
    	((Button)findViewById(R.id.keyHash)).setOnClickListener(new OnClickListener() {
    		public void onClick(View v) {
    			number+="#";
    			tNumberField.setText(number);
    			tg.startTone(ToneGenerator.TONE_DTMF_S, 250);
    		}}
    	);
	}
	
	private void onFilterUserQuery(String keyword) {
		
		if (amperList!=null)
		{
			amperList.clear();
		}
		
		for (Map<String, String> map: orgList)
		{
			if (keyword.length()==0 || ((String)map.get("displayName")).toLowerCase().contains(keyword.toLowerCase()))
			{
				amperList.add(map);
			}
		}
		
		gridAdapter.notifyDataSetChanged();
	}
	
	private void loadFuncPage(int page)
	{
		if (page==0)
		{
		    ((FrameLayout)findViewById(R.id.keypad)).setVisibility(View.GONE);
		    ((FrameLayout)findViewById(R.id.addressbook)).setVisibility(View.GONE);
		    ((FrameLayout)findViewById(R.id.contacts)).setVisibility(View.VISIBLE);
		}
		else if (page==1)
		{
			((FrameLayout)findViewById(R.id.keypad)).setVisibility(View.GONE);
		    ((FrameLayout)findViewById(R.id.contacts)).setVisibility(View.GONE);
		    ((FrameLayout)findViewById(R.id.addressbook)).setVisibility(View.VISIBLE);
		}
		else if (page==2)
		{
		    ((FrameLayout)findViewById(R.id.contacts)).setVisibility(View.GONE);
		    ((FrameLayout)findViewById(R.id.addressbook)).setVisibility(View.GONE);
		    ((FrameLayout)findViewById(R.id.keypad)).setVisibility(View.VISIBLE);
		}
	}
	
	public void onContactQuery() 
	{
		if (mContactQueryHandler == null)
			mContactQueryHandler = new QueryContactHandler(getContentResolver());
		
		mContactQueryHandler.startQuery(0, null,
				ContactsContract.Contacts.CONTENT_URI,
				new String[]{"_id","display_name"},
				ContactsContract.Contacts.HAS_PHONE_NUMBER + "=1", null,
				ContactsContract.Contacts.LAST_TIME_CONTACTED+" desc");
	}
	
	private class QueryContactHandler extends AsyncQueryHandler {
		public QueryContactHandler(ContentResolver cr) {
			super(cr);
		}

		@Override
		protected void onQueryComplete(int token, Object cookie, Cursor c) {
			
			try{
				if (mContactCursor!=null) mContactCursor.close();
				
				mContactCursor = c;
				
				if (mContactCursorAdapter == null) {
					mContactCursorAdapter = new ContactAdapter(SelectUserActivity.this, mContactCursor, cq);
					mContactCursorAdapter.setFilterQueryProvider(SelectUserActivity.this);
					Contacts_LV.setAdapter(mContactCursorAdapter);
					Contacts_LV.setOnItemClickListener(OnContactClickListener);
		        } else {
		        	mContactCursorAdapter.changeCursor(mContactCursor);
		        }
			}catch(Exception e){}
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
	
    private OnItemClickListener OnContactClickListener = new OnItemClickListener()
	{
		@Override  
		public void onItemClick(AdapterView<?> parent, View view, int position, long id) 
		{
			number=(String)view.getTag();
			number=number.replace(" ", "");
			if (funcPage==1)
			{
				Intent it=new Intent();
				it.putExtra("type", 1);
    			it.putExtra("result", number);
				setResult(RESULT_OK, it);
				finish();
			}
		}
	};
	
	OnItemClickListener onChooseUser=new OnItemClickListener() {
		@Override
		public void onItemClick(AdapterView<?> parent, View view,
				int position, long id) {
			Map<String, String> map = amperList.get(position);
			if (map.get("checked").equals("0"))
			{
				if (mCount>=mLimit) return;
				map.put("checked", "1");
				mCount++;
				//tml*** alpha ui
				if (mLimit == 1) {
					mHandler.postDelayed(new Runnable() {
						public void run() {
							((Button)findViewById(R.id.done)).performClick();
						}
					}, 300);
				}
			}else{
				map.put("checked", "0");
				mCount--;
			}
			
			gridAdapter.notifyDataSetInvalidated();
		}
	};
	
	
	Runnable mFetchFriends=new Runnable(){
		public void run()
		{
			amperList.clear();
			orgList.clear();
			Cursor c = mADB.fetchAllByTime();
			if (c!=null && c.moveToFirst())
			{
				do {
					String address = c.getString(1);
					if (address.startsWith("[<GROUP>]")) continue;
					int idx=c.getInt(3);
					if (idx<50) continue;
					boolean found=false;
					if (excludeList!=null)
					{
						for (String exc: excludeList)
						{
							if (address.equals(exc))
							{
								found=true;
								break;
							}
						}
					}
					if (found) continue;
					
					long contactId = cq.getContactIdByNumber(address);
					String disName="";
					String userphotoPath = Global.SdcardPath_inbox + "photo_" + idx + "b.jpg";
//					File f = new File(userphotoPath);
//					if (!f.exists()) userphotoPath=null;
					//tml*** userphoto fix
					if (!new File(userphotoPath).exists())
					{
						userphotoPath = Global.SdcardPath_inbox + "photo_" + idx + ".jpg";
						if (!new File(userphotoPath).exists())
							userphotoPath=null;
					}
					if (userphotoPath == null) {
						Log.w("null pic! " + address + " path=" + userphotoPath);
					}
					
					if (contactId>0)
						disName=cq.getNameByContactId(contactId);
					else
						disName = c.getString(4);
					
					if (disName==null || disName.length()==0)
						disName=String.valueOf((R.string.unknown_person));
					
					HashMap<String, String> map = new HashMap<String, String>();
					
					map.put("displayName", disName);
					map.put("address", address);
					map.put("idx", idx+"");
					map.put("checked", "0");
					map.put("imagePath", userphotoPath);
					
					amperList.add(map);
					orgList.add(map);
				}while(c.moveToNext());
				
				c.close();
			}
			
			mHandler.post(new Runnable(){
				public void run(){
					gridAdapter.notifyDataSetChanged();
				}
			});
		}
	};
	
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
			int count=amperList.size();
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
			
			Map<String, String> map=null;
			
			try{
				map = amperList.get(position);
			}catch(Exception e){
				return convertView;
			}
			
			String imagePath = map.get("imagePath");
			
			foundViewHolder holder;

			if (convertView == null) {
				holder = new foundViewHolder();
				convertView = View.inflate(icontext, R.layout.user_tiny_cell, null);
				
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
					ImageView imageViewByTag=null;
					imageViewByTag = (ImageView) mGridView.findViewWithTag(path);
					if (imageViewByTag != null && imageDrawable!=null) {
						imageViewByTag.setImageDrawable(imageDrawable);
					}
				}
			});
			
			if (cachedImage != null && imagePath!=null)
				holder.photoimage.setImageDrawable(cachedImage);
			else
				holder.photoimage.setImageResource(R.drawable.bighead);
			
			String disname = map.get("displayName");
			holder.friendName.setText(disname);
			
			String address=map.get("address");
			int status=ContactsOnline.getContactOnlineStatus(address);
			if (status>0)
			{
				Drawable d=getResources().getDrawable(R.drawable.online_light);
				d.setBounds(0, 0, (int)(20.f*mDensity), (int)(20.f*mDensity));
				SpannableString spannable = new SpannableString("*"+disname);
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
	
	void hideKeyboard()
	{
		InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
		imm.hideSoftInputFromWindow(((EditText)findViewById(R.id.keyword)).getWindowToken(), 0);
	}

	@Override
	public void onDestroy() {
		hideKeyboard();
		if (mADB != null && mADB.isOpen())
			mADB.close();
		amperList.clear();
		orgList.clear();
		theSelectUser = null;
		System.gc();
		System.gc();
		super.onDestroy();
	}

	@Override
	public Cursor runQuery(CharSequence constraint) {
		try{
			Cursor cursor;
			
	    	if (constraint == null || constraint.length () == 0)
	        {
	    		cursor = getContentResolver().query(ContactsContract.Contacts.CONTENT_URI,
						new String[]{"_id","display_name"},
						ContactsContract.Contacts.HAS_PHONE_NUMBER + "=1", null,
						ContactsContract.Contacts.LAST_TIME_CONTACTED+" desc");
	        }
	        else
	        {
	        	String key=constraint.toString();
	        	cursor = getContentResolver().query(CommonDataKinds.Phone.CONTENT_URI,
	        			ContactAdapter.CONTACTS_PROJECTION,
						CommonDataKinds.Phone.NUMBER+" LIKE '%"+ key + "%'",
						null, CommonDataKinds.Phone.LAST_TIME_CONTACTED+" desc");
	        }
	    	
			mContactCursor = cursor;
	
	        return mContactCursor;
		}catch(Exception e)
		{
			return mContactCursor;
		}
	}
	
	public void closeSelectUser () {
		finish();
	}
}