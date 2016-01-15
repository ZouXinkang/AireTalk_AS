package com.pingshow.airecenter;

import java.net.URLEncoder;

import android.app.Activity;
import android.content.AsyncQueryHandler;
import android.content.ContentResolver;
import android.content.Intent;
import android.database.Cursor;
import android.database.MergeCursor;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.provider.ContactsContract;
import android.provider.ContactsContract.CommonDataKinds;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.view.View.OnTouchListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.FilterQueryProvider;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;

import com.pingshow.airecenter.R;
import com.pingshow.airecenter.contacts.ContactsQuery;
import com.pingshow.airecenter.db.AireCallLogDB;
import com.pingshow.airecenter.map.LocationUpdate;
import com.pingshow.airecenter.view.DigitTextView;
import com.pingshow.network.MyNet;
import com.pingshow.util.MyTelephony;
import com.pingshow.util.MyUtil;
import com.pingshow.voip.AireVenus;
import com.pingshow.voip.DialerActivity;

public class AireCallPage extends Page implements FilterQueryProvider{
	public static final boolean OverrideShowAireCall = false;  //tml|sw*** no airecall in china

	private MyPreference mPref;
	private ContactsQuery cq;
	private QueryContactHandler mContactQueryHandler;
	private ContactAdapter mContactCursorAdapter;
	protected MergeCursor mContactCursor = null;
	private ListView Contacts_LV;
	
	private DigitTextView tNumberField;
	static String StoredNumber="";
	public String number="";
	private boolean keypad_voice=true;
	private Handler mHandler=new Handler();
	private boolean keysTouched = false;
	
	private AireCallLogDB mCLDB;
	public static long CallLogRowId=-1;
	public static boolean isMobileNumber=true;
	public static int cIndex;
	public static String cIso="us";
	public static float previousCredit;
	
	private DQCurrency currency;
	
	private String numberToCheck;
	private String unsureString;
	private String countryName="";
	
	private int selectedClass=0;
	
	final int keyarray[]={R.id.key0,R.id.key1,R.id.key2,R.id.key3,R.id.key4,R.id.key5,R.id.key6,
			R.id.key7,R.id.key8,R.id.key9};
	private ToneGenerator tg=null;
	
	static AireCallPage instance=null;
	private View layout=null;
	private Thread qureyRateThread;
	
	private int spinISOcpos = -1; // spinner position = null
	private String spinISOitem;
	private String spinISOccode; // + 86
	private Spinner spinnerISO;
	
	static public void updateCredit(float credit)
	{
		if (instance!=null)
		{
			Button c=(Button)instance.layout.findViewById(R.id.credit);
	        if (c!=null) c.setText(String.format(MainActivity._this.getString(R.string.credit), credit));
		
	        instance.onContactQuery();
		}
	}
	
	static public AireCallPage getInstance()
	{
		return instance;
	}
	
    public AireCallPage(View v) {
		Log.e("*** !!! AIRECALLPAGE *** START START !!! ***");
    	keysTouched = false;
    	layout=v;
    	
        mPref=new MyPreference(MainActivity._this);
        cq = new ContactsQuery(MainActivity._this);
        
        mCLDB=new AireCallLogDB(MainActivity._this);
        mCLDB.open();
        
        Contacts_LV=(ListView)layout.findViewById(R.id.contacts);
        tNumberField=(DigitTextView)layout.findViewById(R.id.number_to_call);
        
        tg=new ToneGenerator(-1, 50);
        
        selectedClass=mPref.readInt("SelectedClass",0);
        
        currency=new DQCurrency(MainActivity._this);
        
        final String [] classes={MainActivity._this.getString(R.string.standard_class), MainActivity._this.getString(R.string.premium_class), MainActivity._this.getString(R.string.business_class)};
        ((Button)layout.findViewById(R.id.class_select)).setText(classes[selectedClass]);
        
        ((Button)layout.findViewById(R.id.class_select)).setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View arg0) {
				preServiceY();  //tml*** preAV reg
				selectedClass=(selectedClass+1)%3;
				((Button)arg0).setText(classes[selectedClass]);
				mPref.write("SelectedClass",selectedClass);
				
				if (qureyRateThread!=null && qureyRateThread.isAlive())
					qureyRateThread.interrupt();
				qureyRateThread=new Thread(onQueryRate);
				qureyRateThread.start();
			}
        });
        
        for(int i=0;i<10;i++)
	    {
	    	Button key=(Button)layout.findViewById(keyarray[i]);
	    	key.setId(i);
	    	key.setOnClickListener(new OnClickListener() {
	    		public void onClick(View v) {
					preServiceY();  //tml*** preAV reg
	    			number+=v.getId();
	    			tNumberField.setText(number);
	    			if(keypad_voice)
	    				tg.startTone(ToneGenerator.TONE_DTMF_0+v.getId(), 150);
	    		}}
	    	);
	    	if (i==0)
	    	{
	    		key.setOnLongClickListener(new OnLongClickListener() {
	        		@Override
	    			public boolean onLongClick(View v) {
	    				preServiceY();  //tml*** preAV reg
//	        			number = "+" + number;
	    				//tml*** siplist country
	    				if (tNumberField.length() == 0) {
	    					number = mPref.read("spinISOccode", "");
	    					if (number.equals("")) {
			        			number = "+" + number;
	    					}
	    				} else {
	    					if (number.contains("+")) {
	    						number = number.replace("+", "");
	    					} else {
				        		number = "+" + number;
	    					}
	    				}
	        			tNumberField.setText(number);
		    			if(keypad_voice)
		    				tg.startTone(ToneGenerator.TONE_DTMF_0, 400);
	        			return true;
	    			}}
	        	);
	    	}
	    }
        
        ((ImageButton)layout.findViewById(R.id.del)).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				preServiceY();  //tml*** preAV reg
				if (number.equals(mPref.read("spinISOccode", ""))) return;  //tml*** siplist country
				if (number.length()>0)
				{
					number=number.substring(0,number.length()-1);
					tNumberField.setText(number);
				}
			}
		});
//        ((ImageButton)layout.findViewById(R.id.del)).setOnLongClickListener(new OnLongClickListener() {
//			@Override
//			public boolean onLongClick(View v) {
//				number="";
//				tNumberField.setText(number);
//				return false;
//			}
//		});
        ((Button)layout.findViewById(R.id.clear)).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				preServiceY();  //tml*** preAV reg
				number="";
				tNumberField.setText(number);
			}
		});
        
        ((Button)layout.findViewById(R.id.call)).setOnClickListener(new OnClickListener() {
    		public void onClick(View v) {
    			if (AireVenus.callstate_AV == null) {  //tml*** beta ui
        			makeCall();
    			} else {
    				Log.d("AIRECALL callstate_AV != null, " + AireVenus.callstate_AV);
    			}
    		}}
    	);
        ((Button)layout.findViewById(R.id.keyStar)).setOnClickListener(new OnClickListener() {
    		public void onClick(View v) {
				preServiceY();  //tml*** preAV reg
    			number+="*";
    			tNumberField.setText(number);
    			if(keypad_voice)
    				tg.startTone(ToneGenerator.TONE_DTMF_S, 250);
    		}}
    	);
    	((Button)layout.findViewById(R.id.keyHash)).setOnClickListener(new OnClickListener() {
    		public void onClick(View v) {
				preServiceY();  //tml*** preAV reg
    			number+="#";
    			tNumberField.setText(number);
    			if(keypad_voice)
    				tg.startTone(ToneGenerator.TONE_DTMF_S, 250);
    		}}
    	);
        
        
        tNumberField.addTextChangedListener (new TextWatcher() {
	        @Override
	        public void afterTextChanged (Editable s) {
	        	//tml*** siplist country
	        	if (s.length() != 0) {
	        		String spinISOccode = mPref.read("spinISOccode", "");
	        		if (spinISOccode.length() != 0 && !(s.toString().startsWith(spinISOccode))
	        				&& !(s.toString().contains("+"))) {
	        			Log.d("AireCall insert iso " + spinISOccode);
	        			number = spinISOccode + number;
	        			s.insert(0, spinISOccode);
	        		}
	        	}
	        	number = s.toString();
		        Log.d("AireCall phone# to dial= " + s.toString());
		        //***tml
		        if (mContactCursorAdapter != null)
		        	mContactCursorAdapter.getFilter().filter(s.toString());
	        	if (qureyRateThread != null && qureyRateThread.isAlive())
					qureyRateThread.interrupt();
				qureyRateThread = new Thread(onQueryRate);
				qureyRateThread.start();
	        }

	        @Override
	        public void onTextChanged (CharSequence s, int start, int before, int count) {
	        }

	        @Override
	        public void beforeTextChanged (CharSequence s, int start, int count, int after) {
	        }
	    });
        
        ((Button)layout.findViewById(R.id.buy)).setOnClickListener(new OnClickListener(){
        	public void onClick(View v) {
        		Intent it=new Intent(MainActivity._this, ShoppingActivity.class);
				it.putExtra("launchFromSelf", true);
				MainActivity._this.startActivity(it);
				MainActivity._this.finish();
    		}
        });
        //tml*** beta ui
//        ((ImageView) layout.findViewById(R.id.help)).setOnClickListener(new OnClickListener(){
//        	public void onClick(View v) {
//        		mHandler.post(showDialerTooltip);
//    		}
//        });
        //tml|yang*** siplist country
        spinnerISO = (Spinner) layout.findViewById(R.id.selectiso);
        String[] listISOs = MainActivity._this.getResources().getStringArray(R.array.phone_code_list);
        ArrayAdapter<String> spinISOAdapter = new ArrayAdapter<String>(MainActivity._this,
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
	        		preServiceY();
		        	spinISOcpos = position;
		        	if (spinISOcpos > 0) {
			        	spinISOccode = "+" + spinISOitem.split("\\+")[1];
		        	} else {
		        		spinISOccode = "";
		        	}
		        	mPref.write("spinISOccode", spinISOccode);
		        	mPref.write("spinISOpos", spinISOcpos);
	        		Log.i("AireCall spinnerISO=" + spinISOcpos + " " + spinISOitem + " (" + spinISOccode + ")/" + spinISOccode_prev + " > " + number);
	        	}

    			tNumberField.setHint(spinISOccode);
	    		if (number.length() == 0 || tNumberField.length() == 0) {
	    			number = "";
	    			return;
	    		}
	    		
	    		String _tNumberField = tNumberField.getText().toString();
	    		if (_tNumberField.startsWith(spinISOccode_prev) && spinISOccode_prev.length() > 0) {
	    			_tNumberField = _tNumberField.replace(spinISOccode_prev, "");
		    		number = spinISOccode + _tNumberField;
		    		tNumberField.setText(number);
	    		} else if (spinISOccode.length() > 0) {
	    			if (_tNumberField.startsWith(spinISOccode)) {
		    			//do nothing
	    			} else if (_tNumberField.startsWith(spinISOccode.replace("+", ""))) {
		    			number = "+" + _tNumberField;
		    			tNumberField.setText(number);
	    			} else {
		    			if (number.contains("+")) number = number.replace("+", "");
			    		number = spinISOccode + _tNumberField;
			    		tNumberField.setText(number);
	    			}
	    		} else {
	    		}
        	}

        	public void onNothingSelected(AdapterView<?> view) {
        		//Log.i("spinnerISO " + view.getClass().getName());
        	}
        });
        
        if (MainActivity.actionPhoneNumber == null) {
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
            	new Thread(locationUpdate).start();
            }
        } else {
        	spinnerISO.setSelection(0);
        	number = MainActivity.actionPhoneNumber;
        	MainActivity.actionPhoneNumber = null;
        	tNumberField.setText(number);
        }
    	
    	((ImageView) layout.findViewById(R.id.cleariso)).setOnClickListener(new OnClickListener(){
        	public void onClick(View v) {
                spinnerISO.setSelection(0);
    		}
        });
        //***tml
        
        onContactQuery();
        
        float mCredit=mPref.readFloat("Credit",0);
        Button tv=(Button)layout.findViewById(R.id.credit);
        if (tv!=null)
        	tv.setText(String.format(MainActivity._this.getString(R.string.credit), mCredit));
        
        if (mPref.readInt("cSipTip",0)<2)
        	mHandler.postDelayed(showTooltip, 1000);
        
        MainActivity._this.setVolumeControlStream(AudioManager.STREAM_MUSIC);
        
        unsureString = String.format(MainActivity._this.getString(R.string.rapid_rate_unsure),
				currency.getCurrencyCode(),
				currency.getCurrencySymbol());
		((TextView)layout.findViewById(R.id.rate)).setText(unsureString);
        
		
		Intent intent = new Intent(Global.Action_InternalCMD);
		intent.putExtra("Command", Global.CMD_UPDATE_SIP_CREDIT);
		MainActivity._this.sendBroadcast(intent);
        instance=this;
    }
    
    Runnable showTooltip=new Runnable(){
    	public void run()
    	{
    		Intent it=new Intent(MainActivity._this,Tooltip.class);
            it.putExtra("Content", MainActivity._this.getString(R.string.help_aire_free_call));
            MainActivity._this.startActivity(it);
            int c=mPref.readInt("cSipTip",0);
            mPref.write("cSipTip",++c);
    	}
    };
    
    //tml*** beta ui
	Runnable showDialerTooltip = new Runnable() {
    	public void run() {
    		Intent it = new Intent(MainActivity._this, Tooltip.class);
            it.putExtra("Content", MainActivity._this.getString(R.string.airecall_help));
            MainActivity._this.startActivity(it);
    	}
    };
    
    //tml|yang*** siplist country
	Runnable locationUpdate = new Runnable() {
    	public void run() {
			LocationUpdate location = new LocationUpdate(MainActivity._this, mPref);
 	        location.getMyLocFromIpAddress(false);
 	        String[] listISOs = MainActivity._this.getResources().getStringArray(R.array.phone_code_list);
 	        String isosp = mPref.read("iso", "");
 	        
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
    	}
    };

    public void onRestart() {
    	float mCredit=mPref.readFloat("Credit",0);
    	if (layout.findViewById(R.id.credit)!=null)
    		((TextView)layout.findViewById(R.id.credit)).setText(String.format(MainActivity._this.getString(R.string.credit), mCredit));
    	onContactQuery();
    }
    
    private void makeCall()
    {
    	if (DialerActivity.getDialer() != null && DialerActivity.minimized)
		{
			Intent it = new Intent(MainActivity._this, CommonDialog.class);
			it.putExtra("msgContent", MainActivity._this.getString(R.string.in_call));
			it.putExtra("numItems", 1);
			it.putExtra("ItemCaption0", MainActivity._this.getString(R.string.done));
			it.putExtra("ItemResult0", Activity.RESULT_OK);
			MainActivity._this.startActivity(it);
			return;
		}
    	
    	String n = tNumberField.getText().toString();
		if (n.length() == 0 && StoredNumber.length() > 0)
		{
			tNumberField.setText(StoredNumber);
			number = StoredNumber;
			return;
		}

		String _number = number;
		Log.e("AireCall number=" + _number);
    	boolean useAirevoice = mPref.readBoolean("usestanleysip", false);
    	
    	if (_number!=null && _number.length() <= 6 && !useAirevoice)
			return;
    	
    	if (_number!=null && _number.length() <= 3 && useAirevoice)
			return;
    	
    	float mCredit = mPref.readFloat("Credit", 0);
		if (mCredit < 0.010 && !useAirevoice)
		{
			Intent it = new Intent(MainActivity._this, CommonDialog.class);
			it.putExtra("msgContent", MainActivity._this.getString(R.string.credit_not_enough));
			it.putExtra("numItems", 1);
			it.putExtra("ItemCaption0", MainActivity._this.getString(R.string.done));
			it.putExtra("ItemResult0", Activity.RESULT_OK);
			MainActivity._this.startActivity(it);
			return;
		}
		
		previousCredit=mCredit;
		
		if (AireVenus.getLc()!=null && MyUtil.CheckServiceExists(MainActivity._this, "com.pingshow.voip.AireVenus"))
        {
			if (AireVenus.runAsSipAccount==false)
			{
				Log.e("!!! STOPPING AireVenus/ServiceY *** AireCallPage @ voip.makeCall :: !runasSipAcc");
				Intent itx=new Intent(MainActivity._this, AireVenus.class);
				MainActivity._this.stopService(itx);
			}
        }
			
		String globalnumber = _number;

		if (useAirevoice)
		{
			cIndex = 0;
			cIso = "us";
		}
		else if (_number.length() > 7)
		{
			boolean [] isMobile=new boolean[1];
//			globalnumber = MyTelephony.smartAddingPrefix(MainActivity._this, _number, isMobile);
			//tml*** notsmart query
			if (_number.startsWith("011")) _number = _number.substring(3);
			else if (_number.startsWith("00")) _number = _number.substring(2);
			else if (_number.startsWith("0")) _number = _number.substring(1);
			
			if (!_number.startsWith("+")) {
				globalnumber = "+" + _number;
			}
			//***tml
			isMobileNumber = isMobile[0];

			cIndex = MyTelephony.getCountryIndexByNumber(globalnumber);
			cIso = MyTelephony.getCountryIsoByIndex(cIndex);
		}
		Log.d("AireCall globalnumber!!= " + globalnumber + " " + cIndex + " " + cIso + " " + isMobileNumber);
		
		long contact_id = cq.getContactIdByNumber(_number);
		String displayname = globalnumber;
		if (contact_id > 0)
			displayname = cq.getNameByContactId(contact_id);
		CallLogRowId = mCLDB.insert(displayname, globalnumber, contact_id);
		
		if ((globalnumber != null && globalnumber.length() > 7) || useAirevoice)
		{
			AireVenus.setCallType(AireVenus.CALLTYPE_AIRECALL);
			Log.d("AireCall calling " + globalnumber + " " + displayname);
			MakeCall.SipCall(MainActivity._this, globalnumber, displayname, false);
			StoredNumber = _number;
		}
    }
    
    public void onContactQuery() 
	{
		if (mContactQueryHandler == null)
			mContactQueryHandler = new QueryContactHandler(MainActivity._this.getContentResolver());
		
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
				if (mContactCursor!=null){
					try {
						if(Integer.parseInt(Build.VERSION.SDK) < 14)  
						{  
							mContactCursor.close();
						}
					} catch (Exception e) {
						Log.e("error:" + e.toString());
					}
				}
		    	
		    	Cursor [] cursor=new Cursor[2];
				cursor[0] = mCLDB.fetch();
				cursor[1] = c;
				
				mContactCursor= new MergeCursor(cursor);
				
				if (mContactCursorAdapter == null) {
					mContactCursorAdapter = new ContactAdapter(MainActivity._this, mContactCursor, cq);
					mContactCursorAdapter.setFilterQueryProvider(AireCallPage.this);
					Contacts_LV.setAdapter(mContactCursorAdapter);
					Contacts_LV.setOnItemClickListener(OnContactClickListener);
					Contacts_LV.setLongClickable(true);
					Contacts_LV.setOnItemLongClickListener(onRemoveUserLongClick);
		        } else {
		        	mContactCursorAdapter.changeCursor(mContactCursor);
		        }
			}catch(Exception e){
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
    
    private String numberSel;
    private OnItemClickListener OnContactClickListener = new OnItemClickListener()
	{
		@Override  
		public void onItemClick(AdapterView<?> parent, View view, int position, long id) 
		{
			preServiceY();  //tml*** preAV reg
			numberSel = (String) view.getTag();
			
//			numberSel = numberSel.replace(" ", "");
			//tml*** siplist country
			numberSel = MyTelephony.cleanPhoneNumber2(numberSel);
			spinnerISO.setSelection(0);
			number = numberSel;
			
			mHandler.postDelayed(new Runnable() {
				@Override
				public void run() {
					tNumberField.setText(numberSel);
				}
			}, 200);
			//***tml
		}
	};
	
	//tml*** beta ui
	OnItemLongClickListener onRemoveUserLongClick = new OnItemLongClickListener() {
		@Override
		public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
			mCLDB.delete(id);
			onContactQuery();
			return true;
		}
	};

	@Override
	public Cursor runQuery (CharSequence constraint) {
		try{
			Cursor [] cursor=new Cursor[2];
			
	    	if (constraint == null || constraint.length () == 0)
	        {
	    		cursor[1] = MainActivity._this.getContentResolver().query(ContactsContract.Contacts.CONTENT_URI,
						new String[]{"_id","display_name"},
						ContactsContract.Contacts.HAS_PHONE_NUMBER + "=1", null,
						ContactsContract.Contacts.LAST_TIME_CONTACTED+" desc");
	    		
	    		cursor[0] = mCLDB.fetch();
	        }
	        else
	        {
	        	String key=constraint.toString();
	        	cursor[1] = MainActivity._this.getContentResolver().query(CommonDataKinds.Phone.CONTENT_URI,
	        			ContactAdapter.CONTACTS_PROJECTION,
						CommonDataKinds.Phone.NUMBER+" LIKE '%"+ key + "%'",
						null, CommonDataKinds.Phone.LAST_TIME_CONTACTED+" desc");
	        	
	        	if (key.length()>1 && key.startsWith("0"))
	        	{
	        		cursor[0] = mCLDB.fetchLike(key.substring(1));
	        	}
	        	else
	        		cursor[0] = mCLDB.fetchLike(key);
	        }
	    	
			mContactCursor= new MergeCursor(cursor);
	
	        return mContactCursor;
		}catch(Exception e)
		{
			return mContactCursor;
		}
    }
	

	public boolean onKeyDown(int keyCode, KeyEvent event) { 
        if (keyCode>=KeyEvent.KEYCODE_0 && keyCode<=KeyEvent.KEYCODE_9)
        {
        	int num=(keyCode-KeyEvent.KEYCODE_0);
        	if (num==0 && event.isLongPress()) {
//	        	number = "+" + number;
				//tml*** siplist country
				if (tNumberField.length() == 0) {
					number = mPref.read("spinISOccode", "");
					if (number.equals("")) {
	        			number = "+" + number;
					}
				} else {
					if (number.contains("+")) {
						number = number.replace("+", "");
					} else {
		        		number = "+" + number;
					}
				}
				tNumberField.setText(number);
				if(keypad_voice)
					tg.startTone(ToneGenerator.TONE_DTMF_0, 400);
        	} else {
        		if (number.equals("0") && num == 0) {
        			number = "+";
        		} else {
        			number+=(""+num);
        		}
				tNumberField.setText(number);
				if(keypad_voice)
					tg.startTone(ToneGenerator.TONE_DTMF_0+num, 150);
        	}
            return true;
        }
        else if (keyCode==KeyEvent.KEYCODE_STAR)
        {
        	number+="*";
			tNumberField.setText(number);
			if(keypad_voice)
				tg.startTone(ToneGenerator.TONE_DTMF_S, 250);
            return true;
        }
        else if (keyCode==KeyEvent.KEYCODE_DEL)
        {
			if (number.equals(mPref.read("spinISOccode", ""))) return true;  //tml*** siplist country
        	if (number.length()>0)
			{
				number=number.substring(0,number.length()-1);
				tNumberField.setText(number);
			}
            return true;
        }
        else if (keyCode==KeyEvent.KEYCODE_SPACE)
        {
        	makeCall();
        	return true;
        }
        return false; 
	}
	
	private String resultString;
	private Runnable onQueryRate = new Runnable() 
	{
		@Override
		public void run() {
			countryName = "";
			String _number = number;
			if (_number.length() > 8 && _number.length()<=18)  //alex*** phone+, 14 to 18/
			{
				try {
					boolean [] isMobile = new boolean[1];
//					numberToCheck = MyTelephony.smartAddingPrefix(MainActivity._this, _number, isMobile);
					//tml*** notsmart query
					if (_number.startsWith("011")) _number = _number.substring(3);
					else if (_number.startsWith("00")) _number = _number.substring(2);
					else if (_number.startsWith("0")) _number = _number.substring(1);
					if (!_number.startsWith("+")) {
						numberToCheck = "+" + _number;
					} else {
						numberToCheck = _number;
					}
					//***tml
					isMobileNumber = isMobile[0];
					
					if (!numberToCheck.startsWith("+"))
					{
						mHandler.post(new Runnable() {
	                    	public void run() {
			                    ((TextView)layout.findViewById(R.id.rate)).setText(unsureString);
			                    ((TextView)layout.findViewById(R.id.country)).setText(countryName);
	                    	}
	                    });
						return;
					}
				} catch (Exception e) {
				}
				try {
					int count = 0;
					String Return = "";
					
					try {
						do {
							MyNet net = new MyNet(MainActivity._this);
							Log.d("AireCall queryrate < " + numberToCheck + " " + isMobileNumber + " " + selectedClass);
							Return = net.doPostHttps("queryrate.php", "num=" + URLEncoder.encode(numberToCheck,"UTF-8")
									+ "&callplan=" + selectedClass, null);
							if (Return.length() > 0) break;
						} while (++count < 2);
					} catch (Exception e) {
						Log.e("queryrate.php failed "+e.getMessage());
					}
					
					resultString = String.format(MainActivity._this.getString(R.string.rapid_rate_unsure),
							currency.getCurrencyCode(),
							currency.getCurrencySymbol());
					
					if (Return.startsWith("Done"))
					{
						Return = Return.replace("\n", "");
						String[] items = Return.split(",");
						if (items.length > 2)
						{
							float rate = Float.parseFloat(items[1]);
		                    float curr = currency.getCurrencyFromUSD();
		                   
		                    resultString = String.format(MainActivity._this.getString(R.string.rapid_rate),
										currency.getCurrencyCode(),
										currency.getCurrencySymbol(),
										curr*rate);
		                    
		                    cIndex = MyTelephony.getCountryIndexByNumber(numberToCheck);
		                    countryName = MyTelephony.getCountryNameByIndex(AireCallPage.cIndex, MainActivity._this);
							Log.d("AireCall queryrate > " + Return + " > " + cIndex + " " + cIso);
						}
						
						mHandler.post(new Runnable() {
	                    	public void run() {
			                    ((TextView)layout.findViewById(R.id.rate)).setText(resultString);
			                    ((TextView)layout.findViewById(R.id.country)).setText(countryName);
	                    	}
	                    });
					}
				} catch (Exception e) {
					Log.e("onQueryRate failed.");
				}
			} else {
				mHandler.post(new Runnable() {
                	public void run() {
	                    ((TextView)layout.findViewById(R.id.rate)).setText(unsureString);
	                    ((TextView)layout.findViewById(R.id.country)).setText(countryName);
                	}
                });
			}
		}
	};
	//tml*** preAV reg
	public void preServiceY() {
//		if (!keysTouched && AireJupiter.getInstance() != null) {
//			keysTouched = true;
//			new Thread(new Runnable() {
//				@Override
//				public void run() {
//					Log.e("preStart AireVenus/ServiceY from voip.AireCall view!");
//					AireJupiter.getInstance().startServiceY(AireVenus.CALLTYPE_AIRECALL);
//				}
//			}).start();
//		}
	}
	//***tml
	
	@Override
	public void destroy() {
		if (mContactCursor!=null && !mContactCursor.isClosed()) {
			try {
				if(Integer.parseInt(Build.VERSION.SDK) < 14)  
				{  
					mContactCursor.close();
					mContactCursor=null;
				}
			} catch (Exception e) {
				Log.e("error:" + e.toString());
			}
		}
		
		if (mCLDB!=null && mCLDB.isOpen())
			mCLDB.close();
		MainActivity._this.quitPreServiceY();
		System.gc();
		System.gc();
		MainActivity._this.setVolumeControlStream(AudioManager.STREAM_RING);
		instance=null;
	}

}
