package com.pingshow.amper;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import android.app.Activity;
import android.content.AsyncQueryHandler;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.MergeCursor;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.ContactsContract;
import android.provider.ContactsContract.CommonDataKinds;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.view.View.OnTouchListener;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.inputmethod.InputMethodManager;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FilterQueryProvider;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;

import com.pingshow.amper.contacts.ContactsQuery;
import com.pingshow.amper.db.AireCallLogDB;
import com.pingshow.amper.map.LocationUpdate;
import com.pingshow.amper.view.MyHorizontalScrollView;
import com.pingshow.amper.view.MyHorizontalScrollView.SizeCallback;
import com.pingshow.util.MyTelephony;
import com.pingshow.util.MyUtil;
import com.pingshow.voip.AireVenus;

public class SipCallActivity extends Activity implements FilterQueryProvider{
	private MyPreference mPref;
	private ContactsQuery cq;
	private QueryContactHandler mContactQueryHandler;
	private ContactAdapter mContactCursorAdapter;
	protected MergeCursor mContactCursor = null;
	private ListView Contacts_LV;
	private FrameLayout mKeypad;
	private EditText tNumberField;
	static String StoredNumber="";
	public String number="";
	private boolean keypad_voice=true;
	private Handler mHandler=new Handler();
	private boolean keysTouched = false;
	
	private MyHorizontalScrollView scrollView;
	private View menu;
	private View main_page;
	
	private AireCallLogDB mCLDB;
	public static boolean callingOut=false;
	public static boolean isMobileNumber=true;
	public static int cIndex;
	public static String cIso="us";
	public static float previousCredit;
	
	private int sortMethod=1;
	static private List<String> orgList;

	private int spinISOcpos = -1; // spinner position = null
	private String spinISOitem;
	private String spinISOccode; // + 86
	private Spinner spinnerISO;
	private int iSel = 0;
	
	private String actionPhoneNumber;
	
	private static ArrayList<Long> CallLogRowIdList = new ArrayList<Long>();
	
	final int keyarray[]={R.id.key0,R.id.key1,R.id.key2,R.id.key3,R.id.key4,R.id.key5,R.id.key6,
			R.id.key7,R.id.key8,R.id.key9};
	private ToneGenerator tg=null;
	
	static SipCallActivity instance=null;
	static public void updateCredit(float credit)
	{
		if (instance!=null)
		{
			TextView tv=(TextView)instance.main_page.findViewById(R.id.credit);
	        if (tv!=null) tv.setText(String.format(instance.getString(R.string.credit), credit));
	        
	        instance.onContactQuery();
		}
	}
	
	static public SipCallActivity getInstance()
	{
		return instance;
	}
	
	static public long getCallLogRowId()
	{
		if (CallLogRowIdList.size()==0)
			return -1;
		return CallLogRowIdList.get(0);
	}
	
	static public long getLatestCallLogRowId()
	{
		if (CallLogRowIdList.size()==0)
			return -1;
		return CallLogRowIdList.get(CallLogRowIdList.size()-1);
	}
	
	static public void popCallLogRowId()
	{
		if (CallLogRowIdList.size()==0)
			return;
		CallLogRowIdList.remove(0);
	}
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.overridePendingTransition(R.anim.freeze, R.anim.freeze);
    	keysTouched = false;
		neverSayNeverDie(SipCallActivity.this);  //tml|bj*** neverdie/
        
        mPref=new MyPreference(SipCallActivity.this);
		
        LayoutInflater inflater = LayoutInflater.from(this);
        scrollView = (MyHorizontalScrollView) inflater.inflate(R.layout.main_slide_page, null);
        setContentView(scrollView);
        sortMethod = mPref.readInt("ContactsSortMethod", 1);
        //xwf
        menu = inflater.inflate(R.layout.airecall_menu, null);
        main_page = inflater.inflate(R.layout.keypad_page, null);
        
        ImageView btnSlide = (ImageView) main_page.findViewById(R.id.icon);
        btnSlide.setOnClickListener(new ClickListenerForScrolling(scrollView, menu));
        
        final View[] children = new View[] { menu, main_page };

        int scrollToViewIdx = 1;
        scrollView.initViews(children,scrollToViewIdx, new SizeCallbackForMenu(btnSlide));
        
        arrangeLayout.run();
        
        Intent intent = new Intent(Global.Action_InternalCMD);
		intent.putExtra("Command", Global.CMD_UPDATE_SIP_CREDIT);
		sendBroadcast(intent);
        
        instance=this;
    }
    
    Runnable arrangeLayout=new Runnable()
    {
    	public void run()
    	{
            cq = new ContactsQuery(SipCallActivity.this);
            
            mCLDB=new AireCallLogDB(SipCallActivity.this);
            mCLDB.open();
            
            LeftMenuAdapter menuAdapter=new LeftMenuAdapter(SipCallActivity.this);
            ((ListView)menu.findViewById(R.id.list)).setAdapter(menuAdapter);
            ((ListView)menu.findViewById(R.id.list)).setOnItemClickListener(onClickMenuListener);
            
            Contacts_LV=(ListView)main_page.findViewById(R.id.contacts);
            tNumberField=(EditText)main_page.findViewById(R.id.number_to_call);
            mKeypad=(FrameLayout)main_page.findViewById(R.id.keypad);
            //xwf
            mKeypad.setVisibility(View.INVISIBLE);
            tNumberField.setOnTouchListener(new OnTouchListener() {
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    v.onTouchEvent(event);
                    InputMethodManager imm = (InputMethodManager)v.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
                    if (imm != null) {
                        imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
                    }                
                    return true;
                }
            });
            
            tg=new ToneGenerator(-1, 50);

            //tml*** search airecall
            ((ImageView) findViewById(R.id.clearkeyword)).setOnClickListener(new OnClickListener() {
    			@Override
    			public void onClick(View v) {
                    InputMethodManager imm = (InputMethodManager) main_page.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
                    if (imm != null) {
                        imm.hideSoftInputFromWindow(main_page.getWindowToken(), 0);
                    }
    				((EditText) findViewById(R.id.searchkeyword)).setText("");
    				((EditText) findViewById(R.id.searchkeyword)).setVisibility(View.GONE);
    				((ImageView) findViewById(R.id.clearkeyword)).setVisibility(View.GONE);
                    stayHereR();
    			}
    		});

    		((EditText) findViewById(R.id.searchkeyword)).addTextChangedListener (new TextWatcher() {
    	        @Override
    	        public void afterTextChanged (Editable s) {
    	        	if (mContactCursorAdapter != null)
    	        		mContactCursorAdapter.getFilter().filter(s.toString());
    	        }
    	        @Override
    	        public void onTextChanged (CharSequence s, int start, int before, int count) {}
    	        @Override
    	        public void beforeTextChanged (CharSequence s, int start, int count, int after) {}
    	    });
            //***tml
            
            ((Button)main_page.findViewById(R.id.bFafauser)).setOnClickListener(new OnClickListener() {
            	@Override
            	public void onClick(View v) {
            		startActivity(new Intent(SipCallActivity.this, UsersActivity.class));
            		finish();
            	}
            });
            
            ((Button)main_page.findViewById(R.id.bMessage)).setOnClickListener(new OnClickListener() {
    			@Override
    			public void onClick(View v) {
    				startActivity(new Intent(SipCallActivity.this, MessageActivity.class));
    				finish();
    			}
    		});
            ((Button)main_page.findViewById(R.id.bAireCall)).setOnClickListener(new OnClickListener() {
    			@Override
    			public void onClick(View v) {
    				if (mKeypad.getVisibility() == View.INVISIBLE) {
        				preServiceY();  //tml*** preAV reg
    					mKeypad.setVisibility(View.VISIBLE);
    					Animation anim = AnimationUtils.loadAnimation(SipCallActivity.this, R.anim.push_up_in);
    					mKeypad.startAnimation(anim);
    				} else {
    					mKeypad.setVisibility(View.INVISIBLE);
    					stayHere();
    				}
    			}
    		});
            
            ((Button)main_page.findViewById(R.id.bSearch)).setOnClickListener(new OnClickListener() {
    			@Override
    			public void onClick(View v) {
    				startActivity(new Intent(SipCallActivity.this, PublicWalkieTalkie.class));
    				finish();
    			}
    		});
    		//tml*** beta ui, conference
            ((Button)main_page.findViewById(R.id.bConference)).setOnClickListener(new OnClickListener() {
            	@Override
            	public void onClick(View v) {
    				Intent it = new Intent(SipCallActivity.this, PickupActivity.class);
    				it.putExtra("conference", true);
    				startActivity(it);
            		finish();
            	}
            });
            //tml*** temp alpha ui, CX/
            ((Button)main_page.findViewById(R.id.bSetting)).setOnClickListener(new OnClickListener() {
    			@Override
    			public void onClick(View v) {
    				startActivity(new Intent(SipCallActivity.this, SettingActivity.class));
    				finish();
    			}
    		});
            //tml*** beta ui2
            if (mPref.read("iso", "cn").equals("cn")) {
            	((Button) main_page.findViewById(R.id.bSearch)).setVisibility(View.GONE);
            	((Button) main_page.findViewById(R.id.bSetting)).setVisibility(View.GONE);
            } else {
            	((Button) main_page.findViewById(R.id.bSearch)).setVisibility(View.GONE);
            	((Button) main_page.findViewById(R.id.bSetting)).setVisibility(View.GONE);
            }
            boolean largeScreen = (main_page.findViewById(R.id.large) != null);
            if (largeScreen) {
            	((Button) main_page.findViewById(R.id.bSetting)).setVisibility(View.GONE);
            }
            
            for(int i=0;i<10;i++)
    	    {
    	    	ImageButton key=(ImageButton)main_page.findViewById(keyarray[i]);
    	    	key.setId(i);
    	    	key.setOnClickListener(new OnClickListener() {
    	    		public void onClick(View v) {
    					preServiceY();  //tml*** preAV reg
    					//tml*** search airecall
    					if (((EditText) findViewById(R.id.searchkeyword)).getVisibility() == View.VISIBLE) {
    						((EditText) findViewById(R.id.searchkeyword)).setText("");
    						((EditText) findViewById(R.id.searchkeyword)).setVisibility(View.GONE);
    						((ImageView) findViewById(R.id.clearkeyword)).setVisibility(View.GONE);
    		                stayHereR();
    					}
    	    			insertNumber(""+v.getId());
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
        					//tml*** search airecall
        					if (((EditText) findViewById(R.id.searchkeyword)).getVisibility() == View.VISIBLE) {
        						((EditText) findViewById(R.id.searchkeyword)).setText("");
        						((EditText) findViewById(R.id.searchkeyword)).setVisibility(View.GONE);
        						((ImageView) findViewById(R.id.clearkeyword)).setVisibility(View.GONE);
        		                stayHereR();
        					}
//    	        			insertNumber("+");
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
            
            ((ImageButton)main_page.findViewById(R.id.del)).setOnClickListener(new OnClickListener() {
    			@Override
    			public void onClick(View v) {
    				preServiceY();  //tml*** preAV reg
    				if (number.equals(mPref.read("spinISOccode", ""))) return;  //tml*** siplist country
    				number=tNumberField.getText().toString();
    				if (number.length()>0)
    				{
    					try{
    						int pos=tNumberField.getSelectionStart();
        					if (pos==0) return;
        					if (pos==number.length())
        					{
        						number=number.substring(0,number.length()-1);
        						tNumberField.setText(number);
            					tNumberField.setSelection(pos-1);
        					}else{
        						String t1=number;
        						String t2=number;
        						number=t1.substring(0, pos-1)+t2.substring(pos,t2.length());
        						tNumberField.setText(number);
            					tNumberField.setSelection(pos-1);
        					}
    					}catch(Exception e){}
    				}
    			}
    		});
            ((ImageButton)main_page.findViewById(R.id.del)).setOnLongClickListener(new OnLongClickListener() {
    			@Override
    			public boolean onLongClick(View v) {
    				preServiceY();  //tml*** preAV reg
    				number="";
    				tNumberField.setText(number);
    				return false;
    			}
    		});
            ((ImageView)main_page.findViewById(R.id.close)).setOnClickListener(new OnClickListener() {
    			@Override
    			public void onClick(View v) {
    				preServiceY();  //tml*** preAV reg
    				mKeypad.setVisibility(View.INVISIBLE);
    				stayHere();
    			}
    		});
            
            ((ImageButton)main_page.findViewById(R.id.call)).setOnClickListener(makeCallClick);
            
            ((ImageButton)main_page.findViewById(R.id.keyStar)).setOnClickListener(new OnClickListener() {
        		public void onClick(View v) {
    				preServiceY();  //tml*** preAV reg
					//tml*** search airecall
					if (((EditText) findViewById(R.id.searchkeyword)).getVisibility() == View.VISIBLE) {
						((EditText) findViewById(R.id.searchkeyword)).setText("");
						((EditText) findViewById(R.id.searchkeyword)).setVisibility(View.GONE);
						((ImageView) findViewById(R.id.clearkeyword)).setVisibility(View.GONE);
		                stayHereR();
					}
        			insertNumber("*");
        			if(keypad_voice)
        				tg.startTone(ToneGenerator.TONE_DTMF_S, 250);
        		}}
        	);
        	((ImageButton)main_page.findViewById(R.id.keyHash)).setOnClickListener(new OnClickListener() {
        		public void onClick(View v) {
    				preServiceY();  //tml*** preAV reg
					//tml*** search airecall
					if (((EditText) findViewById(R.id.searchkeyword)).getVisibility() == View.VISIBLE) {
						((EditText) findViewById(R.id.searchkeyword)).setText("");
						((EditText) findViewById(R.id.searchkeyword)).setVisibility(View.GONE);
						((ImageView) findViewById(R.id.clearkeyword)).setVisibility(View.GONE);
		                stayHereR();
					}
        			insertNumber("#");
        			if(keypad_voice)
        				tg.startTone(ToneGenerator.TONE_DTMF_S, 250);
        		}}
        	);
            //xwf
            Contacts_LV.setOnScrollListener(new OnScrollListener(){
    			@Override
    			public void onScroll(AbsListView view, int firstVisibleItem,
    				int visibleItemCount, int totalItemCount) {
    			}
    			@Override
    			public void onScrollStateChanged(AbsListView view, int scrollState) {
    				if (mKeypad.getVisibility()==View.VISIBLE){
    					mKeypad.setVisibility(View.INVISIBLE);
    					stayHere();
    				}
    			}
            });
            
            tNumberField.addTextChangedListener (new TextWatcher() {
    	        @Override
    	        public void afterTextChanged (Editable s) {
    	        	//tml*** siplist country
    	        	if (s.length() != 0) {
    	        		String spinISOccode = mPref.read("spinISOccode", "");
    	        		int check0 = spinISOccode.length();
    	        		boolean check1 = s.toString().startsWith(spinISOccode);
    	        		Log.d("AireCall current=" + s.toString() + " " + check0 + " " + check1);
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
    	        	if (mContactCursorAdapter!=null)
    	        		mContactCursorAdapter.getFilter().filter(s.toString());
    	        }

    	        @Override
    	        public void onTextChanged (CharSequence s, int start, int before, int count) {
    	        }

    	        @Override
    	        public void beforeTextChanged (CharSequence s, int start, int count, int after) {
    	        }
    	    });

            //tml|yang*** siplist country
            spinnerISO = (Spinner) findViewById(R.id.selectiso);
            String[] listISOs = getResources().getStringArray(R.array.phone_code_list);
            ArrayAdapter<String> spinISOAdapter = new ArrayAdapter<String>(SipCallActivity.this,
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
    	        	spinISOcpos = position;
    	        	if (spinISOcpos > 0) {
    		        	spinISOccode = "+" + spinISOitem.split("\\+")[1];
    	        	} else {
    	        		spinISOccode = "";
    	        	}
    	        	mPref.write("spinISOccode", spinISOccode);
    	        	mPref.write("spinISOpos", spinISOcpos);
            		Log.i("AireCall spinnerISO=" + spinISOcpos + " " + spinISOitem + " (" + spinISOccode + ")/" + spinISOccode_prev + " > " + number);

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
    	    		tNumberField.setSelection(number.length());
            	}

            	public void onNothingSelected(AdapterView<?> view) {
            		//Log.i("spinnerISO " + view.getClass().getName());
            	}
            });
            
            if (actionPhoneNumber == null) {
            	if (spinISOccode != null && spinISOcpos > -1) {
    	        	iSel = spinISOcpos;
    	        	spinnerISO.post(new Runnable () {
    					@Override
    					public void run() {
        			        spinnerISO.setSelection(iSel);
    					}
    	        	});
            	} else if (isosp.trim().length() != 0) {
        	        for (int i = 0; i < MyTelephony.COUNTRIES.length; i++) {
        		        if (MyTelephony.COUNTRIES[i][0].toLowerCase().equals(isosp)) {
        		        	spinISOccode = MyTelephony.COUNTRIES[i][MyTelephony.COUNTRIES[i].length - 1];
        		        }
        	        }
        	        for (int i = 0; i < listISOs.length; i++) {
        		        if (listISOs[i].contains(spinISOccode)) {
        		        	iSel = i;
        		        	spinnerISO.post(new Runnable () {
        						@Override
        						public void run() {
        	    			        spinnerISO.setSelection(iSel);
        						}
        		        	});
        		        }
        	        }
                } else {
                	mHandler.post(locationUpdate);
                }
            }
        	
        	((ImageView) findViewById(R.id.cleariso)).setOnClickListener(new OnClickListener(){
            	public void onClick(View v) {
                    spinnerISO.setSelection(0);
        		}
            });
            //***tml
            ((ImageView)main_page.findViewById(R.id.buy)).setOnClickListener(new OnClickListener(){
            	public void onClick(View v) {
            		try {
    					Class.forName("android.util.Base64");
    				}catch (ClassNotFoundException e) {
    					return;
    				}
            		Intent intnet=new Intent(SipCallActivity.this, PurchaseActivity.class);
            		intnet.putExtra("pushIn", true);
            		startActivity(intnet);
        		}
            });
           
            mPref.write("LastPage", 4);
            onContactQuery();
            
            float mCredit=mPref.readFloat("Credit",0);
            TextView tv=(TextView)main_page.findViewById(R.id.credit);
            if (tv!=null)
            {
            	tv.setOnClickListener(new OnClickListener(){
                	public void onClick(View v) {
                		try {
        					Class.forName("android.util.Base64");
        				}catch (ClassNotFoundException e) {
        					return;
        				}
                		Intent intnet=new Intent(SipCallActivity.this, PurchaseActivity.class);
                		intnet.putExtra("pushIn", true);
                		startActivity(intnet);
            		}
                });
            	tv.setText(String.format(getString(R.string.credit), mCredit));
            }
            
            if (mPref.readInt("cSipTip",0)<2)
            	mHandler.postDelayed(showTooltip, 1000);
    	}
    };
    
    OnClickListener makeCallClick = new OnClickListener() {
		public void onClick(View v) {
			makeCall(null, null);
			v.setOnClickListener(null);
			
			mHandler.postDelayed(reenableCallBtn, 3000);
		}
    };
    
    Runnable reenableCallBtn=new Runnable()
    {
    	public void run()
    	{
    		((ImageButton)main_page.findViewById(R.id.call)).setOnClickListener(makeCallClick);
    	}
    };

    //tml|yang*** siplist country
	Runnable locationUpdate = new Runnable() {
    	public void run() {
			LocationUpdate location = new LocationUpdate(SipCallActivity.this, mPref);
 	        location.getMyLocFromIpAddress();
 	        String[] listISOs = getResources().getStringArray(R.array.phone_code_list);
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
//    		spinISOselected.setText(spinISOccode);
    	}
    };
    void insertNumber(String id)
    {
    	number=tNumberField.getText().toString();
    	try{
	    	int len=number.length();
	    	if (len>18) return;
	    	if (len==0)
	    	{
	    		number+=id;
	    		tNumberField.setText(number);
//	    		tNumberField.setSelection(1, 1);
	    		tNumberField.setSelection(tNumberField.length(), tNumberField.length());  //tml*** siplist country
	    	}
	    	else{
	    		int pos=tNumberField.getSelectionStart();
		    	if (pos==0)
		    	{
		    		number=id+number;
		    		tNumberField.setText(number);
		    		tNumberField.setSelection(1, 1);
		    	}
		    	else if (pos>0)
		    	{
		    		String t1=number;
		        	String t2=number;
		        	number=t1.substring(0,pos)+id+t2.substring(pos, t2.length());  //tml*** siplist country
		        	tNumberField.setText(number);
		    		tNumberField.setSelection(pos+1, pos+1);  //tml*** siplist country
		    	}
	    	}
    	}catch(Exception e){}
    }
    
    public void setSelectedAddress(String address)
    {
    	number=address;
    	tNumberField.setText(address);
    	scrollView.smoothScrollTo(menu.getMeasuredWidth(), 0);
		scrollView.menuOut = false;
		mKeypad.setVisibility(View.VISIBLE);
		try{
			tNumberField.setSelection(number.length());
		}catch(Exception e){}
    }
    
    Runnable showTooltip=new Runnable(){
    	public void run()
    	{
    		Intent it=new Intent(SipCallActivity.this,Tooltip.class);
            it.putExtra("Content", getString(R.string.help_aire_free_call));
            startActivity(it);
            int c=mPref.readInt("cSipTip",0);
            mPref.write("cSipTip",++c);
    	}
    };

    //tml*** phone intent
    @Override
    protected void onNewIntent(Intent intent) {
    	//required to update NEW intents
        super.onNewIntent(intent);
        setIntent(intent);
    }
    
    @Override
	protected void onResume() {
        //tml*** phone intent
		if (getIntent().getData() != null) {
            String phoneNumber = getIntent().getData().toString();
            
            if (phoneNumber.startsWith("tel:")) {
            	phoneNumber = phoneNumber.replace("tel:","");
            } else if (phoneNumber.startsWith("voicemail:")) {
            	phoneNumber = phoneNumber.replace("voicemail:","");
            }
            phoneNumber = MyTelephony.cleanPhoneNumber2(phoneNumber);
            
            if (MyTelephony.isPhoneNumber(phoneNumber)) {
                actionPhoneNumber = phoneNumber;
            } else {
                actionPhoneNumber = null;
            }
        	getIntent().setData(null);
            Log.d("AireCall handling1 ph# " + phoneNumber + " > " + actionPhoneNumber);
        } else {
        	actionPhoneNumber = null;
        }
		if (actionPhoneNumber != null) {
        	spinnerISO.post(new Runnable () {
				@Override
				public void run() {
			        spinnerISO.setSelection(0);
				}
        	});
        	tNumberField.post(new Runnable () {
				@Override
				public void run() {
		        	number = actionPhoneNumber;
		        	tNumberField.setText(number);
				}
        	});
		}
		//***tml
    	super.onResume();
    	setVolumeControlStream(AudioManager.STREAM_MUSIC);
    	//onContactQuery();
    }
    
    @Override
	protected void onPause() {
    	mHandler.removeCallbacks(showTooltip);
    	setVolumeControlStream(AudioManager.STREAM_RING);
    	super.onPause();
    }
    
    @Override
    protected void onRestart() {
    	super.onRestart();
    	float mCredit=mPref.readFloat("Credit",0);
    	if (main_page.findViewById(R.id.credit)!=null)
    		((TextView)main_page.findViewById(R.id.credit)).setText(String.format(getString(R.string.credit), mCredit));
    	
    	onContactQuery();
    }
    
    @Override
	protected void onDestroy() {
		if (mContactCursor!=null && !mContactCursor.isClosed()) {
			try {
				if(Build.VERSION.SDK_INT < 14)  
				{  
					mContactCursor.close();
					mContactCursor=null;
				}
			} catch (Exception e) {
				Log.e("airecall onDestroy !@#$ " + e.getMessage());
			}
		}
		
		if (mCLDB!=null && mCLDB.isOpen())
			mCLDB.close();
		quitPreServiceY();  //tml*** preAV reg
		System.gc();
		System.gc();
		setVolumeControlStream(AudioManager.STREAM_RING);
		instance=null;
		super.onDestroy();
	}

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
	
	private void quitPreServiceY() {
//		if (AireVenus.instance() != null) {
//			new Thread(new Runnable() {
//				@Override
//				public void run() {
//					AireVenus.instance().quitServiceY();
//				}
//			});
//		}
	}
	//***tml
    
	String mGlobalnumber;
    private void makeCall(String dialNumber, String displayname)
    {
    	String _number;
    	if (dialNumber==null)
    	{
    		String n=tNumberField.getText().toString();
    		number=n;
//    		Log.d(n);
    		if (n.length()==0 && StoredNumber.length()>0)
    		{
    			tNumberField.setText(StoredNumber);
    			number=StoredNumber;
    			return;
    		}
    		
    	}
		_number = number;
		Log.d("AireCall checkISO... " + _number);
    	
    	if (_number!=null && _number.length()<=6)
			return;
    	
    	float mCredit=mPref.readFloat("Credit",0);
		if (mCredit<0.010 && dialNumber==null)
		{
			Intent it = new Intent(SipCallActivity.this, CommonDialog.class);
			it.putExtra("msgContent", getString(R.string.credit_not_enough));
			it.putExtra("numItems", 1);
			it.putExtra("ItemCaption0", getString(R.string.done));
			it.putExtra("ItemResult0", RESULT_OK);
			startActivity(it);
			return;
		}
		
		previousCredit=mCredit;
		
		if (AireVenus.getLc()!=null && MyUtil.CheckServiceExists(SipCallActivity.this, "com.pingshow.voip.AireVenus"))
        {
			if (AireVenus.runAsSipAccount == false) {
				Log.e("!!! STOPPING AireVenus/ServiceY *** SipCallActivity @ voip.makeCall :: !runasSipAcc");
				Intent itx = new Intent(SipCallActivity.this, AireVenus.class);
	    		stopService(itx);
			}
        }
		
		String globalnumber = _number;

		if (!MyTelephony.isPhoneNumber(_number))
		{
			globalnumber="s*"+_number;
		}
		else if (_number.length() > 7)
		{
			_number = MyTelephony.cleanPhoneNumber2(_number);
			//tml*** notsmart query
			if (_number.startsWith("011")) _number = _number.substring(3);
			else if (_number.startsWith("00")) _number = _number.substring(2);
			else if (_number.startsWith("0")) _number = _number.substring(1);
			
			MyTelephony.init(SipCallActivity.this);
			
			if (MyTelephony.validWithCurrentISO(_number)) {
				globalnumber = MyTelephony.addPrefixWithCurrentISO(_number);
				isMobileNumber = true;
				Log.d("AireCall globalnumber1 " + globalnumber);
			} else {
				if (MyTelephony.validLandLineWithCurrentISO(_number)) {	
					isMobileNumber = false;
					globalnumber = MyTelephony.addPrefixLandLineWithCurrentISO(_number);
					Log.d("AireCall globalnumber2 " + globalnumber);
				}
			}
			
			if (!globalnumber.startsWith("+")) {
				globalnumber = MyTelephony.attachPrefix(SipCallActivity.this, _number);
				if (globalnumber.startsWith("+")) isMobileNumber = true;
				Log.d("AireCall globalnumber3 " + globalnumber);
			}
			if (!globalnumber.startsWith("+")) {
//				globalnumber = MyTelephony.attachFixedPrefix(SipCallActivity.this, _number);
				globalnumber = "+" + _number;  //tml*** notsmart query
				if (globalnumber.startsWith("+")) isMobileNumber = false;
				Log.d("AireCall globalnumber4 " + globalnumber);
			}
			Log.d("AireCall globalnumber! = " + globalnumber);
			
			cIndex = MyTelephony.getCountryIndexByNumber(globalnumber, 1);
			if (cIndex == -1) {  //tml*** country iso fix
				cIndex = MyTelephony.getCountryIndexByNumber(globalnumber, 3);
				if (cIndex == -1) cIndex = 0;
			}
			cIso = MyTelephony.getCountryIsoByIndex(cIndex);

		}
		Log.d("AireCall globalnumber!!= " + globalnumber + " " + cIndex + " " + cIso + " " + isMobileNumber);
		if (dialNumber!=null && (dialNumber.startsWith("*") && !dialNumber.startsWith("**") && dialNumber.length()==6))
		{
			AireVenus.setCallType(AireVenus.CALLTYPE_CHATROOM);
			MakeCall.SipCall(SipCallActivity.this, dialNumber, displayname, false);
			number="";
		}
		else
		{
			long contact_id=cq.getContactIdByNumber(_number);
			displayname=globalnumber;
			if (contact_id>0)
				displayname=cq.getNameByContactId(contact_id);
			long CallLogRowId=mCLDB.insert(displayname, globalnumber, contact_id);
			
			CallLogRowIdList.add(CallLogRowId);
			
			if (globalnumber!=null && globalnumber.length()>7)
			{
				AireVenus.setCallType(AireVenus.CALLTYPE_AIRECALL);
				MakeCall.SipCall(SipCallActivity.this, globalnumber, displayname, false);
				StoredNumber=_number;
				
				callingOut=true;
			}
		}
    }
    
    public void onContactQuery() 
	{
		if (mContactQueryHandler == null)
			mContactQueryHandler = new QueryContactHandler(getContentResolver());
		
		if (sortMethod==0)
		{
			mContactQueryHandler.startQuery(0, null,
					ContactsContract.Contacts.CONTENT_URI,
					new String[]{"_id","display_name"},
					ContactsContract.Contacts.HAS_PHONE_NUMBER + "=1", null,
					ContactsContract.Contacts.DISPLAY_NAME + 
		            " COLLATE LOCALIZED ASC");
		}else{
			mContactQueryHandler.startQuery(0, null,
					ContactsContract.Contacts.CONTENT_URI,
					new String[]{"_id","display_name"},
					ContactsContract.Contacts.HAS_PHONE_NUMBER + "=1", null,
					ContactsContract.Contacts.LAST_TIME_CONTACTED+" desc");
		}
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
						if(Build.VERSION.SDK_INT < 14)  
						{  
							mContactCursor.close();
						}
					} catch (Exception e) {
						Log.e("airecall onQueryComplete !@#$ " + e.getMessage());
					}
				}
					
		    	
		    	Cursor [] cursor=new Cursor[2];
				cursor[0] = mCLDB.fetch();
				cursor[1] = c;
				
				mContactCursor= new MergeCursor(cursor);
				
				if (mContactCursorAdapter == null) {
					mContactCursorAdapter = new ContactAdapter(SipCallActivity.this, mContactCursor, cq);
					mContactCursorAdapter.setFilterQueryProvider(SipCallActivity.this);
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
    
    private String numberSel;
    private OnItemClickListener OnContactClickListener = new OnItemClickListener()
	{
		@Override  
		public void onItemClick(AdapterView<?> parent, View view, int position, long id) 
		{
			preServiceY();  //tml*** preAV reg
			if (scrollView.menuOut)
			{
				scrollView.smoothScrollTo(menu.getMeasuredWidth(), 0);
				scrollView.menuOut = false;
				return;
			}
			mKeypad.setVisibility(View.VISIBLE);
			numberSel = (String) view.getTag();
			if (numberSel.length() < 4) {
//				number="alec.kuo.c";
				return;
			}
			
//			number=number.replace(" ", "");
			//tml*** siplist country
			numberSel = MyTelephony.cleanPhoneNumber2(numberSel);
			spinnerISO.setSelection(0);
			number = numberSel;

			mHandler.postDelayed(new Runnable() {
				@Override
				public void run() {
					tNumberField.setText(numberSel);
					try {
						tNumberField.setSelection(numberSel.length());
					} catch (Exception e) {}
				}
			}, 200);
			//***tml
		}
	};

	@Override
	public Cursor runQuery (CharSequence constraint) {
		try{
			Cursor [] cursor=new Cursor[2];
			
	    	if (constraint == null || constraint.length () == 0)
	        {
	    		if (sortMethod==0)
	    		{
		    		cursor[1] = getContentResolver().query(ContactsContract.Contacts.CONTENT_URI,
							new String[]{"_id","display_name"},
							ContactsContract.Contacts.HAS_PHONE_NUMBER + "=1", null,
							ContactsContract.Contacts.DISPLAY_NAME + 
				            " COLLATE LOCALIZED ASC");
	    		}else{
		    		cursor[1] = getContentResolver().query(ContactsContract.Contacts.CONTENT_URI,
							new String[]{"_id","display_name"},
							ContactsContract.Contacts.HAS_PHONE_NUMBER + "=1", null,
							ContactsContract.Contacts.LAST_TIME_CONTACTED+" desc");
	    		}
	    		
	    		cursor[0] = mCLDB.fetch();
	        }
	        else
	        {
	        	String key = constraint.toString();
	        	//tml*** search airecall
	        	if (((EditText) findViewById(R.id.searchkeyword)).getVisibility() == View.VISIBLE) {
		        	cursor[1] = getContentResolver().query(CommonDataKinds.Phone.CONTENT_URI,
		        			ContactAdapter.CONTACTS_PROJECTION,
							CommonDataKinds.Phone.DISPLAY_NAME+" LIKE '%"+ key + "%'",
							null, ContactsContract.Contacts.DISPLAY_NAME + 
				            " COLLATE LOCALIZED ASC");
	        	} else {
		        	if (sortMethod==0) {
			        	cursor[1] = getContentResolver().query(CommonDataKinds.Phone.CONTENT_URI,
			        			ContactAdapter.CONTACTS_PROJECTION,
								CommonDataKinds.Phone.NUMBER+" LIKE '%"+ key + "%'",
								null, ContactsContract.Contacts.DISPLAY_NAME + 
					            " COLLATE LOCALIZED ASC");
		    		} else {
			        	cursor[1] = getContentResolver().query(CommonDataKinds.Phone.CONTENT_URI,
			        			ContactAdapter.CONTACTS_PROJECTION,
								CommonDataKinds.Phone.NUMBER+" LIKE '%"+ key + "%'",
								null, CommonDataKinds.Phone.LAST_TIME_CONTACTED+" desc");
			    	}
	        	}

	        	if (((EditText) findViewById(R.id.searchkeyword)).getVisibility() == View.VISIBLE) {
        			cursor[0] = mCLDB.fetchLikeName(key);
	        	} else {
		        	if (key.length() > 1 && key.startsWith("0")) {
		        		cursor[0] = mCLDB.fetchLike(key.substring(1));
		        	} else {
		        		cursor[0] = mCLDB.fetchLike(key);
		        	}
	        	}
	        	//***tml
	        	
	        }
	    	
			mContactCursor= new MergeCursor(cursor);
			
	        return mContactCursor;
		}
		catch(Exception e)
		{
			try{
				Cursor [] cursor=new Cursor[2];
				if (sortMethod==0)
	    		{
					cursor[1] = getContentResolver().query(ContactsContract.Contacts.CONTENT_URI,
							new String[]{"_id","display_name"},
							ContactsContract.Contacts.HAS_PHONE_NUMBER + "=1", null,
							ContactsContract.Contacts.DISPLAY_NAME + 
				            " COLLATE LOCALIZED ASC");
	    		}else{
			    	cursor[1] = getContentResolver().query(ContactsContract.Contacts.CONTENT_URI,
								new String[]{"_id","display_name"},
								ContactsContract.Contacts.HAS_PHONE_NUMBER + "=1", null,
								ContactsContract.Contacts.LAST_TIME_CONTACTED+" desc");
	    		}
		    	cursor[0] = mCLDB.fetch();
		    	
		    	mContactCursor= new MergeCursor(cursor);
	        }catch(Exception e2){
	        	return null;
	        }
	        return mContactCursor;
		}
    }
	
	@Override
	public void onBackPressed() {
		if (mKeypad.getVisibility()==View.VISIBLE){
			mKeypad.setVisibility(View.INVISIBLE);
			stayHere();
		}
		else
			super.onBackPressed();
		return;
	}
	
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) { 
        if (keyCode>=KeyEvent.KEYCODE_0 && keyCode<=KeyEvent.KEYCODE_9)
        {
        	int num=(keyCode-KeyEvent.KEYCODE_0);
        	if (num==0 && event.isLongPress())
        	{
	        	number+="+";
				tNumberField.setText(number);
				if(keypad_voice)
					tg.startTone(ToneGenerator.TONE_DTMF_0, 400);
        	}else{
        		
        		if (number.equals("0") && num==0)
        			number="+";
        		else
        			number+=(""+num);
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
        	if (number.length()>0)
			{
				number=number.substring(0,number.length()-1);
				tNumberField.setText(number);
			}
            return true;
        }
        else if (keyCode==KeyEvent.KEYCODE_SPACE)
        {
        	makeCall(null, null);
        	return true;
        }
        else if (keyCode==KeyEvent.KEYCODE_BACK)
        {
        	if (mKeypad.getVisibility()==View.VISIBLE){
        		
        		mKeypad.setVisibility(View.INVISIBLE);
        		stayHere();
        	}
    		else
    			super.onBackPressed();
    		return true;
        }
        return false; 
	}
	
	void stayHere()
	{
		int menuWidth = menu.getMeasuredWidth();
		Log.i("stayHere menuWidth=" + menuWidth);
        scrollView.scrollTo(menuWidth, 0);
	}
	
	void stayHereR() {
		scrollView.fullScroll(View.FOCUS_RIGHT);
	}
	
	static class ClickListenerForScrolling implements OnClickListener {
		MyHorizontalScrollView scrollView;
        View menu;

        public ClickListenerForScrolling(MyHorizontalScrollView scrollView, View menu) {
            super();
            this.scrollView = scrollView;
            this.menu = menu;
        }

        @Override
        public void onClick(View v) {
            int menuWidth = menu.getMeasuredWidth();

            if (!scrollView.menuOut) {
                // Scroll to 0 to reveal menu
                int left = 0;
                scrollView.smoothScrollTo(left, 0);
            } else {
                // Scroll to menuWidth so menu isn't on screen.
                int left = menuWidth;
                scrollView.smoothScrollTo(left, 0);
            }
            scrollView.menuOut = !scrollView.menuOut;
        }
    }
	
    static class SizeCallbackForMenu implements SizeCallback {
        int btnWidth;
        View btnSlide;

        public SizeCallbackForMenu(View btnSlide) {
            super();
            this.btnSlide = btnSlide;
        }

        @Override
        public void onGlobalLayout() {
            btnWidth = btnSlide.getMeasuredWidth();
        }

        @Override
        public void getViewSize(int idx, int w, int h, int[] dims) {
            dims[0] = w;
            dims[1] = h;
            final int menuIdx = 0;
            if (idx == menuIdx) {
                dims[0] = w - btnWidth;
            }
        }
    }
    
    OnItemClickListener onClickMenuListener=new OnItemClickListener()
	{
		@Override
		public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
			
			if (position==0)
			{
				startActivity(new Intent(SipCallActivity.this, AireCallLogActivity.class));
			}
			else if (position==1)
			{
				startActivity(new Intent(SipCallActivity.this, QueryRateActivity.class));
			}
			else if (position==2)
			{
				startActivity(new Intent(SipCallActivity.this, ClassSelectActivity.class));
			}
			else if (position==3)
			{
				try {
					Class.forName("android.util.Base64");
				}catch (ClassNotFoundException e) {
					return;
				}
				startActivity(new Intent(SipCallActivity.this, PurchaseActivity.class));
			}
			else if (position==4)
			{
				startActivity(new Intent(SipCallActivity.this, TransactionActivity.class));
			}
			else if (position==5)
			{
				try{
					//tml*** china ip
					String domain = AireJupiter.myAcDomain_default;
					if (AireJupiter.getInstance() != null) {
						domain = AireJupiter.getInstance().getIsoDomain();
					}
					Intent i = new Intent(SipCallActivity.this, WebViewActivity.class);
					i.putExtra("URL", "http://" + domain + "/help.php?p=and&lang="+Locale.getDefault().getLanguage());
					i.putExtra("Title", getString(R.string.help));
					startActivity(i);
				}catch(Exception e){}
			}
		}
	};
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.menu_sort_airecall, menu);
		return true;
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.gosearch:  //tml*** search airecall
			if (((EditText) findViewById(R.id.searchkeyword)).getVisibility() == View.VISIBLE) {
                InputMethodManager imm = (InputMethodManager) main_page.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
                if (imm != null) {
                    imm.hideSoftInputFromWindow(main_page.getWindowToken(), 0);
                }
				((EditText) findViewById(R.id.searchkeyword)).setText("");
				((EditText) findViewById(R.id.searchkeyword)).setVisibility(View.GONE);
				((ImageView) findViewById(R.id.clearkeyword)).setVisibility(View.GONE);
                stayHereR();
			} else {
				((EditText) findViewById(R.id.searchkeyword)).setVisibility(View.VISIBLE);
				((ImageView) findViewById(R.id.clearkeyword)).setVisibility(View.VISIBLE);
				((EditText) findViewById(R.id.searchkeyword)).requestFocus();
			}
			break;
		case R.id.sortbyname:
			if (sortMethod==0) return false;
			mPref.write("ContactsSortMethod", sortMethod=0);
			break;
		case R.id.sortbytime:
			if (sortMethod==1) return false;
			mPref.write("ContactsSortMethod", sortMethod=1);
			break;
		}
		onContactQuery();
		return true;
	}

	//tml|bj*** neverdie
	public void neverSayNeverDie(Context context) {
		if (AireJupiter.getInstance()==null) {
			Log.e("AireJupiter is NULL");
			Intent vip1 = new Intent(context, AireVenus.class);
			context.stopService(vip1);
			Intent vip2 = new Intent(context, AireJupiter.class);
			context.stopService(vip2);
			
			Intent vip00 = new Intent(context, AireJupiter.class);
			context.startService(vip00);
		}
	}
	
}