package com.pingshow.airecenter;

import java.net.URLEncoder;
import java.util.Date;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Handler;
import android.provider.ContactsContract.CommonDataKinds;

import com.facebook.android.AsyncFacebookRunner;
import com.facebook.android.DialogError;
import com.facebook.android.Facebook;
import com.facebook.android.FacebookError;
import com.pingshow.airecenter.R;
import com.pingshow.airecenter.contacts.ContactsOnline;
import com.pingshow.airecenter.contacts.RelatedUserInfo;
import com.pingshow.airecenter.db.AmpUserDB;
import com.pingshow.airecenter.db.RelatedUserDB;
import com.pingshow.airecenter.register.BaseRequestListener;
import com.pingshow.network.MyNet;
import com.pingshow.network.NetInfo;
import com.pingshow.util.MyTelephony;
import com.pingshow.util.MyUtil;

public class FacebookSearch extends Activity{
	
	private Handler mHandler=new Handler();
	
	private AsyncFacebookRunner mAsyncRunner;
	private Facebook facebook;
	private MyPreference mPref;
	protected static JSONArray jsonArray;
	
	private AmpUserDB mADB;
	private RelatedUserDB mRDB;
	
	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.fbsearch_dialog);
        mPref=new MyPreference(this);
        
        mADB=new AmpUserDB(FacebookSearch.this);
		mADB.open();
		
		mRDB=new RelatedUserDB(FacebookSearch.this);
		mRDB.open();
        
        int mode=getIntent().getIntExtra("Mode", 0);
        
        if (mode==0)//Facebook
        {
	        facebook = new Facebook("133881496748582");
	        mAsyncRunner=new AsyncFacebookRunner(facebook);
	
	        facebook.authorize(this, new String[]{"read_friendlists"}, new com.facebook.android.Facebook.DialogListener() {
	            @Override
	            public void onComplete(Bundle values) {
	            	requestUserData();
	            }
	
	            @Override
	            public void onFacebookError(FacebookError error) {
	            	
	            	Intent it=new Intent(FacebookSearch.this, CommonDialog.class);
	    			it.putExtra("msgContent", error.getMessage());
	    			it.putExtra("numItems", 1);
	    			it.putExtra("ItemCaption0", getString(R.string.done));
	    			it.putExtra("ItemResult0", RESULT_OK);
	    			startActivity(it);
	            }
	
	            @Override
	            public void onError(DialogError e) {
	            	Intent it=new Intent(FacebookSearch.this, CommonDialog.class);
	    			it.putExtra("msgContent", e.getMessage());
	    			it.putExtra("numItems", 1);
	    			it.putExtra("ItemCaption0", getString(R.string.done));
	    			it.putExtra("ItemResult0", RESULT_OK);
	    			startActivity(it);
	            }
	
	            @Override
	            public void onCancel() {
	            	setResult(RESULT_CANCELED);
	            	finish();
	            }
	        });
        
        }else{
        	new Thread(searchFriendsByPhonebook).start();
        }
    }
	
	public void requestFriends() {
		Bundle params = new Bundle();
        params.putString("fields", "name, picture, location");
        mAsyncRunner.request("me/friends", new UserRequestListenerFriends());
    }
	
	public void requestUserData() {
		Bundle params = new Bundle();
        params.putString("fields", "name, email, gender");
        mAsyncRunner.request("me", params, new UserRequestListenerMe());
    }
	
	ProgressDialog progress;
	Runnable popupProgressDialog=new Runnable()
	{
		@Override
		public void run() {
			try{
				progress = ProgressDialog.show(FacebookSearch.this, "", getString(R.string.in_progress), true, true);
			}catch(Exception e){}
		}
	};
	
	Runnable dismissProgressDialog=new Runnable()
	{
		@Override
		public void run() {
			try{
				if (progress!=null)
				{
					if (progress.isShowing()){
						progress.dismiss();
					}
				}
			}catch(Exception e){}
		}
	};
	
	public static String FacebookIDtoAireID(String fbID)
	{
		double fb64=Double.valueOf(fbID);
        int high=(int)(fb64/4294967296.);
        long low=(long)(fb64-high*4294967296.);
        String hexId="";
        String hexId_low="";
        if (high>0)
        {
        	hexId=Integer.toHexString(high);
        	hexId_low=Long.toHexString(low);
        	while (hexId_low.length()<8)
        		hexId_low="0"+hexId_low;
        }
        else
        	hexId_low=Long.toHexString(low);
        return "fb"+hexId+hexId_low;
	}
	
	public class UserRequestListenerMe extends BaseRequestListener {
        @Override
        public void onComplete(final String response, final Object state) {
        	
        	mHandler.post(popupProgressDialog);
        	
            JSONObject jsonObject;
            try {
                jsonObject = new JSONObject(response);

                final String gender = jsonObject.getString("gender");
                final String nickname = jsonObject.getString("name");
                String email = jsonObject.getString("email");
                final String fbUID = jsonObject.getString("id");
                
                if (mPref.read("email", email)==null)
                	mPref.write("email", email);
                
                String mynickname=mPref.read("myNickname","");
                if (mynickname.length()<2 || mynickname.matches("[0-9]*"))
                	mPref.write("myNickname", nickname);
                
                mPref.write("myFacebookID",fbUID);
                mPref.write("myGender",gender);
                
                Intent intent = new Intent(Global.Action_InternalCMD);
				intent.putExtra("Command", Global.CMD_UPDATE_MY_NICKNAME);
				sendBroadcast(intent);
               
            } catch (JSONException e) {
                e.printStackTrace();
            } catch (Exception e) {
                e.printStackTrace();
            }
            
            requestFriends();
        }

		@Override
		public void onFacebookError(FacebookError e, Object state) {
		}
    }
	
	String search_result="";
	
	public class UserRequestListenerFriends extends BaseRequestListener {
        @Override
        public void onComplete(final String response, final Object state) {
        	try {
        		jsonArray = new JSONObject(response).getJSONArray("data");
        	} catch (JSONException e) {}
        	
    		new Thread(new Runnable(){
    			public void run()
    			{
    				search_result="";
    				List<RelatedUserInfo> PossibleList=null;
    				try {
    					String list="";
		        		for(int i=0;i<jsonArray.length();i++)
		        		{
		        			long friendId = jsonArray.getJSONObject(i).getLong("id");
		        			if (list.length()==0)
		        				list+=friendId;
		        			else
		        				list+=("+"+friendId);
		        		}
		        		if (list.length()>0)
		        		{
		        			try {
		        				int count = 0;
		        				do {
		        					MyNet net = new MyNet(FacebookSearch.this);
		        					PossibleList = net.doPostHttpsWithXML("searchfb.php", "fblist="+list
		        							+ "&id="+URLEncoder.encode(mPref.read("myPhoneNumber","----"), "UTF-8")
		        							+ "&password=" + URLEncoder.encode(mPref.read("password","----"), "UTF-8"),
		        							null);
		        					if (PossibleList!=null) break;
		        					MyUtil.Sleep(1500);
		        				} while (count++ < 3);
		        			} catch (Exception e) {}
		        		}
    				} catch (JSONException e) {
    				} catch (Exception e) {
    				}
    				
    				if (PossibleList!=null) 
    				{
    					boolean result=false;
    					try{
    						if (mRDB.isOpen() && mADB.isOpen() && PossibleList.size()>0)
    						{
    							int myID=Integer.parseInt(mPref.read("myID","0"),16);
    							String nickname;
    							int idx;
    							for (int i=0;i<PossibleList.size();i++)
    							{
    								RelatedUserInfo r=PossibleList.get(i);
    								idx=r.getIdx();
    								String address=r.getAddress();
    								if (mRDB.isUserBlocked(address)==1 || mADB.isUserBlocked(address)==1) continue;
    								if (idx==myID) continue;
    								search_result+=address+",";
    								if (mADB.isFafauser(address)) continue;
    								nickname=r.getNickName();
    								if (mADB.insertUser(address, idx, nickname)>0)
    									result=true;
    								if (mRDB.isFafauser(address))
    									mRDB.deleteContactByAddress(address);
    							}
    						}
    					}catch(Exception e){}
    					
    					if (result) {
    						Intent intent = new Intent(Global.Action_Refresh_Gallery);
    						sendBroadcast(intent);
    					}
    					
    					mPref.writeLong("last_time_checking_photos", 0);
    					Intent intent = new Intent(Global.Action_InternalCMD);
    					intent.putExtra("Command", Global.CMD_DOWNLOAD_PHOTO_FROMNET);
    					intent.putExtra("type", 0);
    					sendBroadcast(intent);
    				}
    				
    				mPref.writeLong("facebookFriendsSynchronized", new Date().getTime());
    				
    				mHandler.post(dismissProgressDialog);
    				
    				mHandler.post(new Runnable(){
    					public void run()
    					{
    						Intent it=new Intent();
    						it.putExtra("search_result", search_result);
    						setResult(RESULT_OK,it);
    						finish();
    					}
    				});
    			}
    		}).start();
        }

		@Override
		public void onFacebookError(FacebookError e, Object state) {
		}
    }
	
	public void onResume() {    
        super.onResume();
        if (facebook!=null) facebook.extendAccessTokenIfNeeded(this, null);
//        MobclickAgent.onResume(this);
    }
	
	@Override
	protected void onPause() {
//		MobclickAgent.onPause(this);
		super.onPause();
	}

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (facebook!=null) facebook.authorizeCallback(requestCode, resultCode, data);
    }
    
    private Runnable searchFriendsByPhonebook=new Runnable()
	{
		public void run()
		{
			if (!new NetInfo(FacebookSearch.this).isConnected()) {
				finish();
				return;
			}
			if (!mRDB.isOpen() || !mADB.isOpen()){
				finish();
				return;
			}
			
			Log.d("Query By Phonebook");
			
			String addr="";
			
			String myPhoneNumber=mPref.read("myPhoneNumber","----");
			
			try{
				
				MyUtil.Sleep(500);
				
				mHandler.post(popupProgressDialog);
				
				MyUtil.Sleep(4000);
				
				Cursor cursor = getContentResolver().query(
						CommonDataKinds.Phone.CONTENT_URI,
						new String[] { CommonDataKinds.Phone.CONTACT_ID,
								CommonDataKinds.Phone.NUMBER },null,
						null, CommonDataKinds.Phone.LAST_TIME_CONTACTED + " desc");
	
				if (cursor.moveToFirst()) {
					String phonenumber;
					String lastn="";
					int i=0;
					do {
						phonenumber = MyTelephony.cleanPhoneNumber(cursor.getString(1));
						if (phonenumber!=null && phonenumber.length() >= 7) {
							phonenumber = MyTelephony.attachPrefix(FacebookSearch.this, phonenumber);
							if (!phonenumber.equals(myPhoneNumber))
							{
								if (phonenumber.startsWith("+"))//alec
								{
									if (!phonenumber.equals(lastn))
									{
										if (i!=0) addr+=",";
										addr+=phonenumber;
										i++;
										lastn=phonenumber;
										if (i>360) break;
									}
								}
							}
						}
					} while (cursor.moveToNext());
				}
				if(cursor!=null && !cursor.isClosed())
					cursor.close();
			
			}catch(Exception e){}
			
			List<RelatedUserInfo> PossibleList=null;
			
			try {
				int count = 0;
				do {
					MyNet net = new MyNet(FacebookSearch.this);
					PossibleList = net.doPostHttpsWithXML("queryusers_aire.php", "addr=" + URLEncoder.encode(addr,"UTF-8"), null);
					if (PossibleList!=null) break;
					MyUtil.Sleep(1500);
				} while (count++ < 3);
			} catch (Exception e) {}
			
			if (PossibleList!=null) 
			{
				boolean result=false;
				try{
					String myIdHex=mPref.read("myID","0");
					int myIdx=Integer.parseInt(myIdHex,16);
					
					if (mRDB.isOpen() && mADB.isOpen() && PossibleList.size()>0)
					{
						String nickname;
						int idx;
						for (int i=0;i<PossibleList.size();i++)
						{
							RelatedUserInfo r=PossibleList.get(i);
							idx=r.getIdx();
							String address=r.getAddress();
							
							if (mRDB.isUserBlocked(address)==1 || mADB.isUserBlocked(address)==1) continue;
							if (idx==myIdx) continue;
							search_result+=address+",";
							
							//if (mADB.isUserDeleted(address)) continue;
							if (mADB.isFafauser(address)) continue;
							
							nickname=r.getNickName();
							if (mADB.insertUser(address, idx, nickname)>0)
							{
								mRDB.deleteContactByAddress(address);
								ContactsOnline.setContactOnlineStatus(address,1);
								result=true;
							}
						}
					}
					
					mHandler.post(dismissProgressDialog);
					
				}catch(Exception e){}
				
				if (result) {
					Intent intent = new Intent(Global.Action_Refresh_Gallery);
					sendBroadcast(intent);
				}
				
				Intent intent = new Intent();
				intent.setAction(Global.Action_InternalCMD);
				intent.putExtra("Command", Global.CMD_DOWNLOAD_PHOTO_FROMNET);
				intent.putExtra("type", 0);
				sendBroadcast(intent);
				
				mHandler.post(new Runnable(){
					public void run()
					{
						Intent it=new Intent();
						it.putExtra("search_result", search_result);
						setResult(RESULT_OK,it);
						finish();
					}
				});
			}
		}
	};
	
	@Override
	public void onDestroy() {
		if (mADB!=null && mADB.isOpen())
		mADB.close();
		if (mRDB!=null && mRDB.isOpen())
		mRDB.close();
		super.onDestroy();
	}
}
