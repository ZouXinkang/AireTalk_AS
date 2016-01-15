/*
    Open Manager, an open source file manager for the Android system
    Copyright (C) 2009, 2010, 2011  Joe Berria <nexesdevelopment@gmail.com>

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/

package com.pingshow.airecenter.browser;

import java.io.File;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import com.pingshow.airecenter.AireJupiter;
import com.pingshow.airecenter.DialerFrame;
import com.pingshow.airecenter.Global;
import com.pingshow.airecenter.HomeIOTActivity;
import com.pingshow.airecenter.Log;
import com.pingshow.airecenter.MainActivity;
import com.pingshow.airecenter.MyPreference;
import com.pingshow.airecenter.R;
import com.pingshow.airecenter.SecurityNewActivity;
import com.pingshow.airecenter.ShoppingActivity;
import com.pingshow.airecenter.SplashScreen;
import com.pingshow.beehive.BeeHiveService;
import com.pingshow.voip.AireVenus;
import com.pingshow.voip.DialerActivity;
import com.pingshow.voip.VideoCallActivity;

import android.app.Dialog;
import android.app.AlertDialog;
import android.app.ListActivity;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.hardware.usb.UsbAccessory;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.net.Uri;
import android.os.Handler;
import android.os.StatFs;
import android.os.Environment;
import android.os.Bundle;
import android.support.v4.widget.DrawerLayout;
import android.view.InputDevice;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.View.OnClickListener;
import android.view.View.OnGenericMotionListener;
import android.view.Menu;
import android.view.MenuItem;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.ImageView;
import android.widget.ImageButton;
import android.widget.Button;
import android.widget.Toast;

/**
 * This is the main activity. The activity that is presented to the user
 * as the application launches. This class is, and expected not to be, instantiated.
 * <br>
 * <p>
 * This class handles creating the buttons and
 * text views. This class relies on the class EventHandler to handle all button
 * press logic and to control the data displayed on its ListView. This class
 * also relies on the FileManager class to handle all file operations such as
 * copy/paste zip/unzip etc. However most interaction with the FileManager class
 * is done via the EventHandler class. Also the SettingsMangager class to load
 * and save user settings. 
 * <br>
 * <p>
 * The design objective with this class is to control only the look of the
 * GUI (option menu, context menu, ListView, buttons and so on) and rely on other
 * supporting classes to do the heavy lifting. 
 *
 * @author Joe Berria
 *
 */
public final class MainBrowser extends ListActivity {
	public static final String ACTION_WIDGET = "com.nexes.manager.Main.ACTION_WIDGET";
	
	private static final String PREFS_NAME = "ManagerPrefsFile";	//user preference file name
	private static final String PREFS_HIDDEN = "hidden";
	private static final String PREFS_COLOR = "color";
	private static final String PREFS_THUMBNAIL = "thumbnail";
	private static final String PREFS_SORT = "sort";
	private static final String PREFS_STORAGE = "sdcard space";
	
	private static final int MENU_MKDIR =   0x00;			//option menu id
	private static final int MENU_SETTING = 0x01;			//option menu id
	private static final int MENU_SEARCH =  0x02;			//option menu id
	private static final int MENU_SPACE =   0x03;			//option menu id
	private static final int MENU_QUIT = 	0x04;			//option menu id
	private static final int SEARCH_B = 	0x09;
	
	private static final int D_MENU_DELETE = 0x05;			//context menu id
	private static final int D_MENU_RENAME = 0x06;			//context menu id
	private static final int D_MENU_COPY =   0x07;			//context menu id
	private static final int D_MENU_PASTE =  0x08;			//context menu id
	private static final int D_MENU_ZIP = 	 0x0e;			//context menu id
	private static final int D_MENU_UNZIP =  0x0f;			//context menu id
	private static final int D_MENU_MOVE = 	 0x30;			//context menu id
	private static final int F_MENU_MOVE = 	 0x20;			//context menu id
	private static final int F_MENU_DELETE = 0x0a;			//context menu id
	private static final int F_MENU_RENAME = 0x0b;			//context menu id
	private static final int F_MENU_ATTACH = 0x0c;			//context menu id
	private static final int F_MENU_COPY =   0x0d;			//context menu id
	private static final int SETTING_REQ = 	 0x10;			//request code for intent

	private FileManager mFileMag;
	private EventHandler mHandler;
	private EventHandler.TableRow mTable;
	private Handler mmHandler = new Handler(); 
	
	private SharedPreferences mSettings;
	private boolean mReturnIntent = false;
	private boolean mHoldingFile = false;
	private boolean mHoldingZip = false;
	private boolean mUseBackKey = true;
	private String mCopiedTarget;
	private String mZippedTarget;
	private String mSelectedListItem;				//item from context menu
	private TextView  mPathLabel, mDetailLabel, mStorageLabel2;
	
	private ImageButton btnSearch, btnSettings, btnHomeFolders, btnUSBHome;
	private Button btnPaste2;
	private ImageButton btnVideo, btnImage, btnMusic, btnFiles, btnSecurity, btnExternal;
	private TextView mStorageLabel1;
	public static String destination = null;
	public static MainBrowser _this;
	private MyPreference mPref;
	
	private boolean mSortChanged = false;
	private int sort_state;

	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().requestFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.bwr_main);

		mPref = new MyPreference(this);
		
		_this = this;
        
		//tml*** redirect preregister
		boolean registered = mPref.readBoolean("AireRegistered", false);
		if (!registered) {
			Intent intent = new Intent();
			intent.setClass(MainBrowser.this, SplashScreen.class);
			intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			startActivity(intent);
			finish();
			return;
		}

		if (getIntent().getBooleanExtra("launchFromSelf", false))
			this.overridePendingTransition(R.anim.freeze, R.anim.freeze);
		
        neverSayNeverDie(_this);  //tml|bj*** neverdie/
        
        /*read settings*/
        mSettings = getSharedPreferences(PREFS_NAME, 0);
        boolean hide = mSettings.getBoolean(PREFS_HIDDEN, false);
        boolean thumb = mSettings.getBoolean(PREFS_THUMBNAIL, true);
        int space = mSettings.getInt(PREFS_STORAGE, View.VISIBLE);
        int color = mSettings.getInt(PREFS_COLOR, -1);
        int sort = mSettings.getInt(PREFS_SORT, 3);
        
        mFileMag = new FileManager();
        mFileMag.setShowHiddenFiles(hide);
        mFileMag.setSortType(sort);
        
        if (savedInstanceState != null)
        	mHandler = new EventHandler(MainBrowser.this, mFileMag, savedInstanceState.getString("location"));
        else
        	mHandler = new EventHandler(MainBrowser.this, mFileMag);
        
        mHandler.setTextColor(color);
        mHandler.setShowThumbnails(thumb);
        mTable = mHandler.new TableRow();
        
        /*sets the ListAdapter for our ListActivity and
         *gives our EventHandler class the same adapter
         */
        mHandler.setListAdapter(mTable);
        setListAdapter(mTable);
        
        /* register context menu for our list view */
        registerForContextMenu(getListView());
        mStorageLabel1 = (TextView)findViewById(R.id.storage_label1);
        mStorageLabel2 = (TextView) findViewById(R.id.storage_label2);
        mDetailLabel = (TextView)findViewById(R.id.detail_label);
        mPathLabel = (TextView)findViewById(R.id.path_label);
		btnVideo = (ImageButton) findViewById(R.id.dest1);
		btnImage = (ImageButton) findViewById(R.id.dest2);
		btnMusic = (ImageButton) findViewById(R.id.dest3);
		btnFiles = (ImageButton) findViewById(R.id.dest4);
		btnSecurity = (ImageButton) findViewById(R.id.dest5);
		btnExternal = (ImageButton) findViewById(R.id.dest6);

		mHandler.updateDirectory(mFileMag.setHomeDir(Environment
				.getExternalStorageDirectory().getPath()));

//        mPathLabel.setText("path: /sdcard");
		if(mPathLabel != null)
		{
			mPathLabel.setText(mFileMag.getCurrentDir());
		}
        
        updateStorageLabel();
        mStorageLabel2.setVisibility(space);
        
        mHandler.setUpdateLabels(mPathLabel, mDetailLabel);
        
        /* setup buttons */
        btnHomeFolders = (ImageButton) findViewById(R.id.home_button);
        btnHomeFolders.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				updateStorageLabel();
				((RelativeLayout) findViewById(R.id.navsplash)).setVisibility(View.VISIBLE);
				((RelativeLayout) findViewById(R.id.navfolder)).setVisibility(View.GONE);
			}
		});
        btnUSBHome = (ImageButton) findViewById(R.id.ext_button);
        btnUSBHome.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				updateStorageLabel();
			}
		});

		btnVideo.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				((RelativeLayout) findViewById(R.id.navsplash)).setVisibility(View.GONE);
				((RelativeLayout) findViewById(R.id.navfolder)).setVisibility(View.VISIBLE);
				updateStorageLabel();
				destination = Global.SdcardPath_video;
	    		mHandler.updateDirectory(mFileMag.setHomeDir(checkDirectory(destination)));
	    		if(mPathLabel != null) {
	    			mPathLabel.setText(mFileMag.getCurrentDir());
	    		}
	    		((ImageButton) findViewById(R.id.home_button)).requestFocus();  //tml*** prefocus
			}
		});
		((ImageButton) findViewById(R.id.dest1)).requestFocus();  //tml*** prefocus
		
		btnImage.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				((RelativeLayout) findViewById(R.id.navsplash)).setVisibility(View.GONE);
				((RelativeLayout) findViewById(R.id.navfolder)).setVisibility(View.VISIBLE);
				updateStorageLabel();
				destination = Global.SdcardPath_image;
	    		mHandler.updateDirectory(mFileMag.setHomeDir(checkDirectory(destination)));
	    		if(mPathLabel != null) {
	    			mPathLabel.setText(mFileMag.getCurrentDir());
	    		}
	    		((ImageButton) findViewById(R.id.home_button)).requestFocus();  //tml*** prefocus
			}
		});
		
		btnMusic.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				((RelativeLayout) findViewById(R.id.navsplash)).setVisibility(View.GONE);
				((RelativeLayout) findViewById(R.id.navfolder)).setVisibility(View.VISIBLE);
				updateStorageLabel();
				destination = Global.SdcardPath_music;
	    		mHandler.updateDirectory(mFileMag.setHomeDir(checkDirectory(destination)));
	    		if(mPathLabel != null) {
	    			mPathLabel.setText(mFileMag.getCurrentDir());
	    		}
	    		((ImageButton) findViewById(R.id.home_button)).requestFocus();  //tml*** prefocus
			}
		});
		
		btnFiles.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				((RelativeLayout) findViewById(R.id.navsplash)).setVisibility(View.GONE);
				((RelativeLayout) findViewById(R.id.navfolder)).setVisibility(View.VISIBLE);
				updateStorageLabel();
				destination = Global.SdcardPath_files;
	    		mHandler.updateDirectory(mFileMag.setHomeDir(checkDirectory(destination)));
	    		if(mPathLabel != null) {
	    			mPathLabel.setText(mFileMag.getCurrentDir());
	    		}
	    		((ImageButton) findViewById(R.id.home_button)).requestFocus();  //tml*** prefocus
			}
		});
		
		btnSecurity.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				((RelativeLayout) findViewById(R.id.navsplash)).setVisibility(View.GONE);
				((RelativeLayout) findViewById(R.id.navfolder)).setVisibility(View.VISIBLE);
				updateStorageLabel();
				destination = Global.SdcardPath_record;
	    		mHandler.updateDirectory(mFileMag.setHomeDir(checkDirectory(destination)));
	    		if(mPathLabel != null) {
	    			mPathLabel.setText(mFileMag.getCurrentDir());
	    		}
	    		((ImageButton) findViewById(R.id.home_button)).requestFocus();  //tml*** prefocus
			}
		});
		
		btnExternal.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				((RelativeLayout) findViewById(R.id.navsplash)).setVisibility(View.GONE);
				((RelativeLayout) findViewById(R.id.navfolder)).setVisibility(View.VISIBLE);
				updateStorageLabel();
				destination = Global.Storage_USBSD;
	    		mHandler.updateDirectory(mFileMag.setHomeDir(checkDirectory(destination)));
	    		if(mPathLabel != null) {
	    			mPathLabel.setText(mFileMag.getCurrentDir());
	    		}
	    		((ImageButton) findViewById(R.id.home_button)).requestFocus();  //tml*** prefocus
			}
		});
        
        btnSearch = (ImageButton) findViewById(R.id.search_button);
        btnSearch.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				showDialog(MENU_SEARCH);
			}
		});
        
        btnSettings = (ImageButton) findViewById(R.id.setting_button);
        btnSettings.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
//    			Intent settings_int = new Intent(MainBrowser.this, Settings.class);
//    			settings_int.putExtra("HIDDEN", mSettings.getBoolean(PREFS_HIDDEN, false));
//    			settings_int.putExtra("THUMBNAIL", mSettings.getBoolean(PREFS_THUMBNAIL, true));
//    			settings_int.putExtra("COLOR", mSettings.getInt(PREFS_COLOR, -1));
//    			settings_int.putExtra("SORT", mSettings.getInt(PREFS_SORT, 0));
//    			settings_int.putExtra("SPACE", mSettings.getInt(PREFS_STORAGE, View.VISIBLE));
//    			settings_int.putExtra("launchFromSelf", true);
//    			
//    			startActivityForResult(settings_int, SETTING_REQ);
    			
    			sort_state = mSettings.getInt(PREFS_SORT, 0);
    			
				AlertDialog.Builder builder = new AlertDialog.Builder(MainBrowser.this);
    			CharSequence[] options = {getResources().getString(R.string.bwr_settsortby1),
    					getResources().getString(R.string.bwr_settsortby2),
    					getResources().getString(R.string.bwr_settsortby3),
    					getResources().getString(R.string.bwr_settsortby4)};
    			
    			builder.setTitle(getResources().getString(R.string.bwr_settsort1));
    			builder.setIcon(R.drawable.bwr_filter);
    			builder.setSingleChoiceItems(options, sort_state, new DialogInterface.OnClickListener() {					
					@Override
					public void onClick(DialogInterface dialog, int index) {
						switch(index) {
						case 0:
							sort_state = 0;
							mSortChanged = true;
							mPref.write(PREFS_SORT, sort_state);
							break;
						case 1:
							sort_state = 1;
							mSortChanged = true;
							mPref.write(PREFS_SORT, sort_state);
							break;
						case 2:
							sort_state = 2;
							mSortChanged = true;
							mPref.write(PREFS_SORT, sort_state);
							break;
						case 3:
							sort_state = 3;
							mSortChanged = true;
							mPref.write(PREFS_SORT, sort_state);
							break;
						}

			    		mFileMag.setSortType(sort_state);
			    		mHandler.updateDirectory(mFileMag.getNextDir(mFileMag.getCurrentDir(), true));
					}
				});
    			
    			builder.create().show();
			}
		});

        btnPaste2 = (Button) findViewById(R.id.paste_2);
        btnPaste2.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
    			boolean multi_select = mHandler.hasMultiSelectData();
    			
    			if(multi_select) {
    				mHandler.copyFileMultiSelect(mFileMag.getCurrentDir());
    			} else if(mHoldingFile && mCopiedTarget.length() > 1) {
    				mHandler.copyFile(mCopiedTarget, mFileMag.getCurrentDir());
    				mDetailLabel.setText("");
    			}
    			    			   			
    			mHoldingFile = false;
    	    	btnPaste2.setVisibility(View.GONE);
    	    	mHandler.updateDirectory(mFileMag.getNextDir(mFileMag.getCurrentDir(), false));
    	    	mHandler.updateView();
    	    	updateStorageLabel();
			}
		});
        
//        int[] img_button_id = {R.id.home_button, R.id.back_button,
//        					   R.id.multiselect_button, R.id.ext_button};
        int[] img_button_id = {R.id.back_button,
				   R.id.multiselect_button, R.id.ext_button};
        
        int[] button_id = {R.id.hidden_copy, R.id.hidden_attach,
        				   R.id.hidden_delete, R.id.hidden_move, R.id.hidden_cancel};
        
        ImageButton[] bimg = new ImageButton[img_button_id.length];
        Button[] bt = new Button[button_id.length];
        
        for(int i = 0; i < img_button_id.length; i++) {
        	bimg[i] = (ImageButton)findViewById(img_button_id[i]);
        	bimg[i].setOnClickListener(mHandler);
        }
        
        for(int i = 0; i < button_id.length; i++) {
    		bt[i] = (Button)findViewById(button_id[i]);
    		bt[i].setOnClickListener(mHandler);
        }
        
    
//        Intent intent = getIntent();
//
//        if(intent.getAction().equals(Intent.ACTION_GET_CONTENT)) {
//            Log.e("AMP", "see4b");
//        	bimg[5].setVisibility(View.GONE);
//        	mReturnIntent = true;
//        
//        } else if (intent.getAction().equals(ACTION_WIDGET)) {
//            Log.e("AMP", "see4c");
//        	Log.e("MAIN", "Widget action, string = " + intent.getExtras().getString("folder"));
//        	mHandler.updateDirectory(mFileMag.getNextDir(intent.getExtras().getString("folder"), true));
//        	
//        }
        

		// //// SideBar

		((ImageView) findViewById(R.id.bar1)).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				Intent it = new Intent(MainBrowser.this, MainActivity.class);
				it.putExtra("launchFromSelf", true);
				startActivity(it);
				finish();
			}
		});
		//tml*** beta ui2 X
//		((ImageView) findViewById(R.id.bar6)).setOnClickListener(new OnClickListener() {
//			@Override
//			public void onClick(View v) {
//				Intent it = new Intent(MainBrowser.this, ShoppingActivity.class);
//				it.putExtra("launchFromSelf", true);
//				startActivity(it);
//				finish();
//			}
//		});
		((ImageView) findViewById(R.id.bar7)).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				Intent it = new Intent(MainBrowser.this, SecurityNewActivity.class);
				it.putExtra("launchFromSelf", true);
				startActivity(it);
				finish();
			}
		});
        
		//tml*** beta ui2 X
        ((ImageView)findViewById(R.id.bar9)).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				//tml*** alpha iot ui
//				Intent it=new Intent(MainBrowser.this, HomeIOTActivity.class);
//				it.putExtra("launchFromSelf", true);
//				startActivity(it);
//				finish();
				Intent it=new Intent(MainBrowser.this, MainActivity.class);
				it.putExtra("launchFromSelf", true);
				it.putExtra("switchToInflate", 5);
				startActivity(it);
				finish();
			}
		});
        ((ImageView)findViewById(R.id.bar9)).setImageAlpha(150);  //temp until icon-faded available
        //tml*** beta ui2 X
//		((ImageView) findViewById(R.id.bar8)).setOnClickListener(new OnClickListener() {
//			@Override
//			public void onClick(View v) {
//				mHandler.stopThumbnailThread();
//				((RelativeLayout) findViewById(R.id.navsplash)).setVisibility(View.VISIBLE);
//				((RelativeLayout) findViewById(R.id.navfolder)).setVisibility(View.GONE);
//			}
//		});
		
//		usbDetect(1);

        ((Button)findViewById(R.id.back_main)).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				Intent it=new Intent(MainBrowser.this, MainActivity.class);
				it.putExtra("launchFromSelf", true);
				it.putExtra("switchToInflate", 5);
				startActivity(it);
				finish();
			}
		});
        //tml*** beta ui2
        ((RelativeLayout) findViewById(R.id.sidebar_ghost)).setOnGenericMotionListener(sideBarMotionListener);
        ((ImageView) findViewById(R.id.menu_main)).setOnClickListener(new OnClickListener () {
			@Override
			public void onClick(View v) {
				((DrawerLayout) findViewById(R.id.main_content)).openDrawer((RelativeLayout) findViewById(R.id.sidebar_frame_drawer));
			}
        });
        
		DialerFrame.setFrame(this, findViewById(android.R.id.content));
		
    }
	
	public void showPasteV (boolean show) {
		if (show) {
			btnPaste2.setVisibility(View.VISIBLE);
		} else {
			btnPaste2.setVisibility(View.GONE);
		}
	}
	
	@Override
	protected void onDestroy() {
		try {  //tml*** unregistered rcvr destroy
			unregisterReceiver(mAttachReceiver);
		} catch (IllegalArgumentException e) {
		}
		super.onDestroy();
	}

	@Override
	protected void onResume() {
		DialerFrame.checkEmbeddedDialer(findViewById(android.R.id.content));
		IntentFilter attachFilter = new IntentFilter();
		attachFilter.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED);
		attachFilter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);
		this.registerReceiver(mAttachReceiver, attachFilter);
		super.onResume();
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		
		outState.putString("location", mFileMag.getCurrentDir());
	}
	
	/*(non Java-Doc)
	 * Returns the file that was selected to the intent that
	 * called this activity. usually from the caller is another application.
	 */
	private void returnIntentResults(File data) {
		mReturnIntent = false;
		
		Intent ret = new Intent();
		ret.setData(Uri.fromFile(data));
		setResult(RESULT_OK, ret);
		
		finish();
	}
	
	public void updateStorageLabel() {
		long total, aval;
		int kb = 1024;
		
		StatFs fs = new StatFs(Environment.
								getExternalStorageDirectory().getPath());
		
		total = fs.getBlockCount() * (fs.getBlockSize() / kb);
		aval = fs.getAvailableBlocks() * (fs.getBlockSize() / kb);
		
		mStorageLabel2.setText(String.format(getResources().getString(R.string.bwr_storage) + ":" + "\n"
				+ getResources().getString(R.string.bwr_available) + " %.2f GB" + "\n"
				+ getResources().getString(R.string.bwr_total) + " %.2f GB ", 
				(double)aval / (kb * kb), (double)total / (kb * kb)));
		
		mStorageLabel1.setText(String.format(getResources().getString(R.string.bwr_storage) + ": " +
				getResources().getString(R.string.bwr_available) + " %.2f GB " + "\t\t"
				+ getResources().getString(R.string.bwr_total) + " %.2f GB", 
				(double)aval / (kb * kb), (double)total / (kb * kb)));
	}
	
	private String checkDirectory(String path) {
//		File folder = new File(Environment.getExternalStorageDirectory() + path);
		File folder = new File(path);
		boolean success = false;
		if (!folder.exists()) {
		    success = folder.mkdirs();
		}
		Log.d("CHECK DIR>> " + folder.getPath());
//		return ("/sdcard/" + path);
		return folder.getPath();
	}
	
	/**
	 *  To add more functionality and let the user interact with more
	 *  file types, this is the function to add the ability. 
	 *  
	 *  (note): this method can be done more efficiently 
	 */
    @Override
    public void onListItemClick(ListView parent, View view, int position, long id) {
    	final String item = mHandler.getData(position);
    	boolean multiSelect = mHandler.isMultiSelected();
    	File file = new File(mFileMag.getCurrentDir() + "/" + item);
    	String item_ext = null;
    	
    	try {
    		item_ext = item.substring(item.lastIndexOf("."), item.length());
    		
    	} catch(IndexOutOfBoundsException e) {	
    		item_ext = ""; 
    	}
    	
    	/*
    	 * If the user has multi-select on, we just need to record the file
    	 * not make an intent for it.
    	 */
    	if(multiSelect) {
    		mTable.addMultiPosition(position, file.getPath());
    		
    	} else {
	    	if (file.isDirectory()) {
				if(file.canRead()) {
					mHandler.stopThumbnailThread();
		    		mHandler.updateDirectory(mFileMag.getNextDir(item, false));
		    		mPathLabel.setText(mFileMag.getCurrentDir());
		    		
		    		/*set back button switch to true 
		    		 * (this will be better implemented later)
		    		 */
		    		if(!mUseBackKey)
		    			mUseBackKey = true;
		    		
	    		} else {
	    			Toast.makeText(this, getResources().getString(R.string.bwr_permissions), 
	    							Toast.LENGTH_SHORT).show();
	    		}
	    	}
	    	
	    	/*music file selected--add more audio formats*/
	    	else if (item_ext.equalsIgnoreCase(".mp3") || 
	    			 item_ext.equalsIgnoreCase(".wma") || 
	    			 item_ext.equalsIgnoreCase(".m4a") || 
	    			 item_ext.equalsIgnoreCase(".mid") || 
	    			 item_ext.equalsIgnoreCase(".wav") || 
	    			 item_ext.equalsIgnoreCase(".amr")) {
	    		
	    		if(mReturnIntent) {
	    			returnIntentResults(file);
	    		} else {
	    			Intent i = new Intent();
    				i.setAction(android.content.Intent.ACTION_VIEW);
    				i.setDataAndType(Uri.fromFile(file), "audio/*");
    				startActivity(i);
	    		}
	    	}
	    	
	    	/*photo file selected*/
	    	else if(item_ext.equalsIgnoreCase(".jpeg") || 
	    			item_ext.equalsIgnoreCase(".jpg")  ||
	    			item_ext.equalsIgnoreCase(".png")  ||
	    			item_ext.equalsIgnoreCase(".gif")  || 
	    			item_ext.equalsIgnoreCase(".bmp")  || 
	    			item_ext.equalsIgnoreCase(".tiff")) {
	 			    		
	    		if (file.exists()) {
	    			if(mReturnIntent) {
	    				returnIntentResults(file);
	    				
	    			} else {
			    		Intent picIntent = new Intent();
			    		picIntent.setAction(android.content.Intent.ACTION_VIEW);
			    		picIntent.setDataAndType(Uri.fromFile(file), "image/*");
			    		startActivity(picIntent);
	    			}
	    		}
	    	}
	    	
	    	/*video file selected--add more video formats*/
	    	else if(item_ext.equalsIgnoreCase(".m4v") || 
	    			item_ext.equalsIgnoreCase(".3gp") ||
	    			item_ext.equalsIgnoreCase(".wmv") || 
	    			item_ext.equalsIgnoreCase(".mp4") || 
	    			item_ext.equalsIgnoreCase(".rmvb") || 
	    			item_ext.equalsIgnoreCase(".avi") || 
	    			item_ext.equalsIgnoreCase(".ogg")) {
	    		
	    		if (file.exists()) {
	    			if(mReturnIntent) {
	    				returnIntentResults(file);
	    				
	    			} else {
	    				Intent movieIntent = new Intent();
			    		movieIntent.setAction(android.content.Intent.ACTION_VIEW);
			    		movieIntent.setDataAndType(Uri.fromFile(file), "video/*");
			    		startActivity(movieIntent);
	    			}
	    		}
	    	}
	    	
	    	/*zip file */
	    	else if(item_ext.equalsIgnoreCase(".zip")) {
	    		
	    		if(mReturnIntent) {
	    			returnIntentResults(file);
	    			
	    		} else {
		    		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		    		AlertDialog alert;
		    		mZippedTarget = mFileMag.getCurrentDir() + "/" + item;
		    		CharSequence[] option = {getResources().getString(R.string.bwr_extract),
		    				getResources().getString(R.string.bwr_extracto)};
		    		
		    		builder.setTitle(getResources().getString(R.string.bwr_extrac));
		    		builder.setItems(option, new DialogInterface.OnClickListener() {
		
						public void onClick(DialogInterface dialog, int which) {
							switch(which) {
								case 0:
									String dir = mFileMag.getCurrentDir();
									mHandler.unZipFile(item, dir + "/");
									break;
									
								case 1:
									mDetailLabel.setText(getResources().getString(R.string.bwr_holding)
											+ " " + item + " " + getResources().getString(R.string.bwr_oextract));
									mHoldingZip = true;
									break;
							}
						}
		    		});
		    		
		    		alert = builder.create();
		    		alert.show();
	    		}
	    	}
	    	
	    	/* gzip files, this will be implemented later */
	    	else if(item_ext.equalsIgnoreCase(".gzip") ||
	    			item_ext.equalsIgnoreCase(".gz")) {
	    		
	    		if(mReturnIntent) {
	    			returnIntentResults(file);
	    			
	    		} else {
	    			//TODO:
	    		}
	    	}
	    	
	    	/*pdf file selected*/
	    	else if(item_ext.equalsIgnoreCase(".pdf")) {
	    		
	    		if(file.exists()) {
	    			if(mReturnIntent) {
	    				returnIntentResults(file);
	    				
	    			} else {
			    		Intent pdfIntent = new Intent();
			    		pdfIntent.setAction(android.content.Intent.ACTION_VIEW);
			    		pdfIntent.setDataAndType(Uri.fromFile(file), 
			    								 "application/pdf");
			    		
			    		try {
			    			startActivity(pdfIntent);
			    		} catch (ActivityNotFoundException e) {
			    			Toast.makeText(this, getResources().getString(R.string.bwr_couldnotfound) + getResources().getString(R.string.bwr_pdfview), 
									Toast.LENGTH_SHORT).show();
			    		}
		    		}
	    		}
	    	}
	    	
	    	/*Android application file*/
	    	else if(item_ext.equalsIgnoreCase(".apk")){
	    		
	    		if(file.exists()) {
	    			if(mReturnIntent) {
	    				returnIntentResults(file);
	    				
	    			} else {
		    			Intent apkIntent = new Intent();
		    			apkIntent.setAction(android.content.Intent.ACTION_VIEW);
		    			apkIntent.setDataAndType(Uri.fromFile(file), "application/vnd.android.package-archive");
		    			startActivity(apkIntent);
	    			}
	    		}
	    	}
	    	
	    	/* HTML file */
	    	else if(item_ext.equalsIgnoreCase(".html")) {
	    		
	    		if(file.exists()) {
	    			if(mReturnIntent) {
	    				returnIntentResults(file);
	    				
	    			} else {
		    			Intent htmlIntent = new Intent();
		    			htmlIntent.setAction(android.content.Intent.ACTION_VIEW);
		    			htmlIntent.setDataAndType(Uri.fromFile(file), "text/html");
		    			
		    			try {
		    				startActivity(htmlIntent);
		    			} catch(ActivityNotFoundException e) {
		    				Toast.makeText(this, getResources().getString(R.string.bwr_couldnotfound) + getResources().getString(R.string.bwr_htmlview), 
		    									Toast.LENGTH_SHORT).show();
		    			}
	    			}
	    		}
	    	}
	    	
	    	/* text file*/
	    	else if(item_ext.equalsIgnoreCase(".txt")) {
	    		
	    		if(file.exists()) {
	    			if(mReturnIntent) {
	    				returnIntentResults(file);
	    				
	    			} else {
		    			Intent txtIntent = new Intent();
		    			txtIntent.setAction(android.content.Intent.ACTION_VIEW);
		    			txtIntent.setDataAndType(Uri.fromFile(file), "text/plain");
		    			
		    			try {
		    				startActivity(txtIntent);
		    			} catch(ActivityNotFoundException e) {
		    				txtIntent.setType("text/*");
		    				startActivity(txtIntent);
		    			}
	    			}
	    		}
	    	}

	    	/* doc file*/
	    	else if(item_ext.equalsIgnoreCase(".doc")
	    			|| item_ext.equalsIgnoreCase(".docx")) {
	    		
	    		if(file.exists()) {
	    			if(mReturnIntent) {
	    				returnIntentResults(file);
	    				
	    			} else {
		    			Intent txtIntent = new Intent();
		    			txtIntent.setAction(android.content.Intent.ACTION_VIEW);
		    			txtIntent.setDataAndType(Uri.fromFile(file), "application/msword");
		    			
		    			try {
		    				startActivity(txtIntent);
		    			} catch(ActivityNotFoundException e) {
		    				txtIntent.setType("text/*");
		    				startActivity(txtIntent);
		    			}
	    			}
	    		}
	    	}

	    	/* excel file*/
	    	else if(item_ext.equalsIgnoreCase(".xlsx")
	    			|| item_ext.equalsIgnoreCase(".xls")
	    			|| item_ext.equalsIgnoreCase(".csv")) {
	    		
	    		if(file.exists()) {
	    			if(mReturnIntent) {
	    				returnIntentResults(file);
	    				
	    			} else {
		    			Intent txtIntent = new Intent();
		    			txtIntent.setAction(android.content.Intent.ACTION_VIEW);
		    			txtIntent.setDataAndType(Uri.fromFile(file), "application/vnd.ms-excel");
		    			
		    			try {
		    				startActivity(txtIntent);
		    			} catch(ActivityNotFoundException e) {
		    				txtIntent.setType("text/*");
		    				startActivity(txtIntent);
		    			}
	    			}
	    		}
	    	}

	    	/* powerpoint file*/
	    	else if(item_ext.equalsIgnoreCase(".ppt")
	    			|| item_ext.equalsIgnoreCase(".pptx")) {
	    		
	    		if(file.exists()) {
	    			if(mReturnIntent) {
	    				returnIntentResults(file);
	    				
	    			} else {
		    			Intent txtIntent = new Intent();
		    			txtIntent.setAction(android.content.Intent.ACTION_VIEW);
		    			txtIntent.setDataAndType(Uri.fromFile(file), "application/vnd.ms-powerpoint");
		    			
		    			try {
		    				startActivity(txtIntent);
		    			} catch(ActivityNotFoundException e) {
		    				txtIntent.setType("text/*");
		    				startActivity(txtIntent);
		    			}
	    			}
	    		}
	    	}
	    	
	    	/* generic intent */
	    	else {
	    		if(file.exists()) {
	    			if(mReturnIntent) {
	    				returnIntentResults(file);
	    				
	    			} else {
			    		Intent generic = new Intent();
			    		generic.setAction(android.content.Intent.ACTION_VIEW);
			    		generic.setDataAndType(Uri.fromFile(file), "text/plain");
			    		
			    		try {
			    			startActivity(generic);
			    		} catch(ActivityNotFoundException e) {
			    			Toast.makeText(this, getResources().getString(R.string.bwr_couldnotfound) + getResources().getString(R.string.bwr_anyopen) + " " + file.getName(), 
			    						   Toast.LENGTH_SHORT).show();
			    		}
	    			}
	    		}
	    	}
    	}
	}
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    	super.onActivityResult(requestCode, resultCode, data);
    	
    	SharedPreferences.Editor editor = mSettings.edit();
    	boolean check;
    	boolean thumbnail;
    	int color, sort, space;
    	
    	/* resultCode must equal RESULT_CANCELED because the only way
    	 * out of that activity is pressing the back button on the phone
    	 * this publishes a canceled result code not an ok result code
    	 */
    	if(requestCode == SETTING_REQ && resultCode == RESULT_CANCELED) {
    		//save the information we get from settings activity
    		check = data.getBooleanExtra("HIDDEN", false);
    		thumbnail = data.getBooleanExtra("THUMBNAIL", true);
    		color = data.getIntExtra("COLOR", -1);
    		sort = data.getIntExtra("SORT", 0);
    		space = data.getIntExtra("SPACE", View.VISIBLE);
    		
    		editor.putBoolean(PREFS_HIDDEN, check);
    		editor.putBoolean(PREFS_THUMBNAIL, thumbnail);
    		editor.putInt(PREFS_COLOR, color);
    		editor.putInt(PREFS_SORT, sort);
    		editor.putInt(PREFS_STORAGE, space);
    		editor.commit();
    		  		
    		mFileMag.setShowHiddenFiles(check);
    		mFileMag.setSortType(sort);
    		mHandler.setTextColor(color);
    		mHandler.setShowThumbnails(thumbnail);
    		mStorageLabel1.setVisibility(space);
    		mHandler.updateDirectory(mFileMag.getNextDir(mFileMag.getCurrentDir(), true));
    	}
    }
    
    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo info) {
    	super.onCreateContextMenu(menu, v, info);
    	
    	boolean multi_data = mHandler.hasMultiSelectData();
    	AdapterContextMenuInfo _info = (AdapterContextMenuInfo)info;
    	mSelectedListItem = mHandler.getData(_info.position);

    	/* is it a directory and is multi-select turned off */
    	if(mFileMag.isDirectory(mSelectedListItem) && !mHandler.isMultiSelected()) {
    		menu.setHeaderTitle("Folder operations");
        	menu.add(0, D_MENU_DELETE, 0, getResources().getString(R.string.bwr_delete) + " " +
        			getResources().getString(R.string.bwr_folder));
        	menu.add(0, D_MENU_RENAME, 0, getResources().getString(R.string.bwr_rename) + " " +
        			getResources().getString(R.string.bwr_folder));
        	menu.add(0, D_MENU_COPY, 0, getResources().getString(R.string.bwr_copy) + " " +
        			getResources().getString(R.string.bwr_folder));
        	menu.add(0, D_MENU_MOVE, 0, getResources().getString(R.string.bwr_move) + " " +
        			getResources().getString(R.string.bwr_folder));
        	menu.add(0, D_MENU_ZIP, 0, getResources().getString(R.string.bwr_zip) + " " +
        			getResources().getString(R.string.bwr_folder));
        	menu.add(0, D_MENU_PASTE, 0, getResources().getString(R.string.bwr_paste))
        			.setEnabled(mHoldingFile || multi_data);
        	menu.add(0, D_MENU_UNZIP, 0, getResources().getString(R.string.bwr_extract))
        			.setEnabled(mHoldingZip);
    		
        /* is it a file and is multi-select turned off */
    	} else if(!mFileMag.isDirectory(mSelectedListItem) && !mHandler.isMultiSelected()) {
        	menu.setHeaderTitle("File Operations");
    		menu.add(0, F_MENU_DELETE, 0, getResources().getString(R.string.bwr_delete) + " " +
        			getResources().getString(R.string.bwr_file));
    		menu.add(0, F_MENU_RENAME, 0, getResources().getString(R.string.bwr_rename) + " " +
        			getResources().getString(R.string.bwr_file));
    		menu.add(0, F_MENU_COPY, 0, getResources().getString(R.string.bwr_copy) + " " +
        			getResources().getString(R.string.bwr_file));
    		menu.add(0, F_MENU_MOVE, 0, getResources().getString(R.string.bwr_move) + " " +
        			getResources().getString(R.string.bwr_file));
    		menu.add(0, F_MENU_ATTACH, 0, getResources().getString(R.string.bwr_email) + " " +
        			getResources().getString(R.string.bwr_file));
    	}	
    }
    
    @Override
    public boolean onContextItemSelected(MenuItem item) {

    	switch(item.getItemId()) {
    		case D_MENU_DELETE:
    		case F_MENU_DELETE:
    			AlertDialog.Builder builder = new AlertDialog.Builder(this);
    			builder.setTitle("Warning ");
    			builder.setIcon(R.drawable.bwr_warning);
    			builder.setMessage(getResources().getString(R.string.bwr_deleting1) + " " + mSelectedListItem +
    					"\n" + getResources().getString(R.string.del_confirm1) + " " + getResources().getString(R.string.del_confirm2));
    			builder.setCancelable(false);
    			
    			builder.setNegativeButton(getResources().getString(R.string.bwr_cancel), new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int which) {
						dialog.dismiss();
						Toast.makeText(MainBrowser.this, "取消", Toast.LENGTH_SHORT).show();
					}
    			});
    			builder.setPositiveButton(getResources().getString(R.string.bwr_delete), new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int which) {
						mHandler.deleteFile(mFileMag.getCurrentDir() + "/" + mSelectedListItem);
					}
    			});
    			AlertDialog alert_d = builder.create();
    			alert_d.show();
    	    	updateStorageLabel();
    			return true;
    			
    		case D_MENU_RENAME:
    			showDialog(D_MENU_RENAME);
    			return true;
    			
    		case F_MENU_RENAME:
    			showDialog(F_MENU_RENAME);
    			return true;
    			
    		case F_MENU_ATTACH:
    			File file = new File(mFileMag.getCurrentDir() +"/"+ mSelectedListItem);
    			Intent mail_int = new Intent();
    			
    			mail_int.setAction(android.content.Intent.ACTION_SEND);
    			mail_int.setType("application/mail");
    			mail_int.putExtra(Intent.EXTRA_BCC, "");
    			mail_int.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(file));
    			startActivity(mail_int);
    			return true;
    		
    		case F_MENU_MOVE:
    		case D_MENU_MOVE:
    		case F_MENU_COPY:
    		case D_MENU_COPY:
    			if(item.getItemId() == F_MENU_MOVE || item.getItemId() == D_MENU_MOVE)
    				mHandler.setDeleteAfterCopy(true);
    			
    			mHoldingFile = true;
    			btnPaste2.setVisibility(View.VISIBLE);
    			
    			mCopiedTarget = mFileMag.getCurrentDir() +"/"+ mSelectedListItem;
    			mDetailLabel.setText(getResources().getString(R.string.bwr_holding) + " " + mSelectedListItem);
    	    	updateStorageLabel();
    			return true;
    			
    		
    		case D_MENU_PASTE:
    			boolean multi_select = mHandler.hasMultiSelectData();
    			
    			if(multi_select) {
    				mHandler.copyFileMultiSelect(mFileMag.getCurrentDir() +"/"+ mSelectedListItem);
    				
    			} else if(mHoldingFile && mCopiedTarget.length() > 1) {
    				
    				mHandler.copyFile(mCopiedTarget, mFileMag.getCurrentDir() +"/"+ mSelectedListItem);
    				mDetailLabel.setText("");
    			}
    			    			   			
    			mHoldingFile = false;
    	    	btnPaste2.setVisibility(View.GONE);
    	    	updateStorageLabel();
    			return true;
    			
    		case D_MENU_ZIP:
    			String dir = mFileMag.getCurrentDir();
    			
    			mHandler.zipFile(dir + "/" + mSelectedListItem);
    	    	updateStorageLabel();
    			return true;
    			
    		case D_MENU_UNZIP:
    			if(mHoldingZip && mZippedTarget.length() > 1) {
    				String current_dir = mFileMag.getCurrentDir() + "/" + mSelectedListItem + "/";
    				String old_dir = mZippedTarget.substring(0, mZippedTarget.lastIndexOf("/"));
    				String name = mZippedTarget.substring(mZippedTarget.lastIndexOf("/") + 1, mZippedTarget.length());
    				
    				if(new File(mZippedTarget).canRead() && new File(current_dir).canWrite()) {
	    				mHandler.unZipFileToDir(name, current_dir, old_dir);				
	    				mPathLabel.setText(current_dir);
	    				
    				} else {
    					Toast.makeText(this, getResources().getString(R.string.bwr_permissions2) + " " + name, 
    							Toast.LENGTH_SHORT).show();
    				}
    			}
    			
    			mHoldingZip = false;
    			mDetailLabel.setText("");
    			mZippedTarget = "";
    	    	updateStorageLabel();
    			return true;
    	}
    	return false;
    }
    
    /* ================Menus, options menu and context menu end here=================*/

    @Override
    protected Dialog onCreateDialog(int id) {
    	final Dialog dialog = new Dialog(MainBrowser.this);
    	
    	switch(id) {
    		case D_MENU_RENAME:
    		case F_MENU_RENAME:
    			dialog.setContentView(R.layout.bwr_input_layout);
    			dialog.setTitle(getResources().getString(R.string.bwr_rename) + " " + mSelectedListItem);
    			dialog.setCancelable(false);
    			
    			ImageView rename_icon = (ImageView)dialog.findViewById(R.id.input_icon);
    			rename_icon.setImageResource(R.drawable.bwr_rename);
    			
    			TextView rename_label = (TextView)dialog.findViewById(R.id.input_label);
    			rename_label.setText(mFileMag.getCurrentDir());
    			final EditText rename_input = (EditText)dialog.findViewById(R.id.input_inputText);
    			
    			Button rename_cancel = (Button)dialog.findViewById(R.id.input_cancel_b);
    			Button rename_create = (Button)dialog.findViewById(R.id.input_create_b);
    			rename_create.setText(getResources().getString(R.string.bwr_rename));
    			
    			rename_create.setOnClickListener(new OnClickListener() {
    				public void onClick (View v) {
    					if(rename_input.getText().length() < 1)
    						dialog.dismiss();
    					
    					if(mFileMag.renameTarget(mFileMag.getCurrentDir() +"/"+ mSelectedListItem, rename_input.getText().toString()) == 0) {
    						Toast.makeText(MainBrowser.this, mSelectedListItem + " " + getResources().getString(R.string.bwr_rename) + ":" +rename_input.getText().toString(),
    								Toast.LENGTH_LONG).show();
    					}
    						
    					dialog.dismiss();
    					String temp = mFileMag.getCurrentDir();
    					mHandler.updateDirectory(mFileMag.getNextDir(temp, true));
    				}
    			});
    			rename_cancel.setOnClickListener(new OnClickListener() {
    				public void onClick (View v) {	dialog.dismiss(); }
    			});
    		break;
    		
    		case SEARCH_B:
    		case MENU_SEARCH:
    			dialog.setContentView(R.layout.bwr_input_layout);
    			dialog.setTitle(getResources().getString(R.string.bwr_search));
    			dialog.setCancelable(false);
    			
    			ImageView searchIcon = (ImageView)dialog.findViewById(R.id.input_icon);
    			searchIcon.setImageResource(R.drawable.bwr_search);
    			
    			TextView search_label = (TextView)dialog.findViewById(R.id.input_label);
    			search_label.setText(getResources().getString(R.string.bwr_search));
    			final EditText search_input = (EditText)dialog.findViewById(R.id.input_inputText);
    			
    			Button search_button = (Button)dialog.findViewById(R.id.input_create_b);
    			Button cancel_button = (Button)dialog.findViewById(R.id.input_cancel_b);
    			search_button.setText(getResources().getString(R.string.bwr_search));
    			
    			search_button.setOnClickListener(new OnClickListener() {
    				public void onClick(View v) {
    					String temp = search_input.getText().toString();
    					
    					if (temp.length() > 0)
    						mHandler.searchForFile(temp);
    					dialog.dismiss();
    				}
    			});
    			
    			cancel_button.setOnClickListener(new OnClickListener() {
    				public void onClick(View v) { dialog.dismiss(); }
    			});

    		break;
    	}
    	return dialog;
    }
    
    /*
     * (non-Javadoc)
     * This will check if the user is at root directory. If so, if they press back
     * again, it will close the application. 
     * @see android.app.Activity#onKeyDown(int, android.view.KeyEvent)
     */
//    @Override
//   public boolean onKeyDown(int keycode, KeyEvent event) {
//    	String current = mFileMag.getCurrentDir();
//    	
//    	if(keycode == KeyEvent.KEYCODE_SEARCH) {
//    		showDialog(SEARCH_B);
//    		
//    		return true;
//    		
//    	} else if(keycode == KeyEvent.KEYCODE_BACK && mUseBackKey && !current.equals("/")) {
//    		if(mHandler.isMultiSelected()) {
//    			mTable.killMultiSelect(true);
//    			Toast.makeText(MainBrowser.this, getResources().getString(R.string.multisel_off), Toast.LENGTH_SHORT).show();
//    		
//    		} else {
//    			//stop updating thumbnail icons if its running
//    			mHandler.stopThumbnailThread();
//				if ((mFileMag.getCurrentDir() + "/").equals(Global.SdcardPath_record)
//						|| (mFileMag.getCurrentDir() + "/").equals(Global.SdcardPath_video)
//						|| (mFileMag.getCurrentDir() + "/").equals(Global.SdcardPath_image)
//						|| (mFileMag.getCurrentDir() + "/").equals(Global.SdcardPath_files)
//						|| (mFileMag.getCurrentDir() + "/").equals(Global.SdcardPath_music)) {
//					mHandler.updateDirectory(mFileMag.setHomeDir(trimDir(Global.SdcardPath_airetalk)));
//				} else if ((mFileMag.getCurrentDir() + "/").equals(Global.Storage_USBSD)) {
//					mHandler.updateDirectory(mFileMag.setHomeDir("/storage"));
//				} else if ((mFileMag.getCurrentDir() + "/").equals(Global.SdcardPath_airetalk)) {
//					mHandler.updateDirectory(mFileMag.setHomeDir("/sdcard"));
//				} else {
//					mHandler.updateDirectory(mFileMag.getPreviousDir());
//				}
//	    		mPathLabel.setText(mFileMag.getCurrentDir());
//    		}
//    		return true;
//    		
//    	} else if(keycode == KeyEvent.KEYCODE_BACK && mUseBackKey && current.equals("/")) {
////    		Toast.makeText(MainBrowser.this, getResources().getString(R.string.back_again), Toast.LENGTH_SHORT).show();
//    		
//    		if(mHandler.isMultiSelected()) {
//    			mTable.killMultiSelect(true);
//    			Toast.makeText(MainBrowser.this, getResources().getString(R.string.multisel_off), Toast.LENGTH_SHORT).show();
//    		}
//    		
//    		mUseBackKey = false;
//    		mPathLabel.setText(mFileMag.getCurrentDir());
//    		
//    		return false;
//    		
//    	} else if(keycode == KeyEvent.KEYCODE_BACK && !mUseBackKey && current.equals("/")) {
////    		finish();
//    		
//    		return false;
//    	}
//    	return false;
//    }

	public void close()
	{
		finish();
	}

    private String trimDir (String path) {
    	String trimmed;
    	trimmed = path.substring(0, path.lastIndexOf("/"));
    	return trimmed;
    }

	public FrameLayout getNotificationLayout()
	{
		return (FrameLayout)findViewById(R.id.notification);
	}
	
	//tml|bj*** neverdie
	public void neverSayNeverDie(Context context) {
		if (AireJupiter.getInstance()==null) {
			Log.e("AireJupiter.getInstance() is null, RESETTING");
			Intent vip0 = new Intent(context, BeeHiveService.class);
			context.stopService(vip0);
			Intent vip1 = new Intent(context, AireVenus.class);
			context.stopService(vip1);
			Intent vip2 = new Intent(context, AireJupiter.class);
			context.stopService(vip2);
			
			Intent vip00 = new Intent(context, AireJupiter.class);
			context.startService(vip00);
		}
	}
	
	//will confuse device and acces, ac100 also has hidden 4 usb detected
	BroadcastReceiver mAttachReceiver = new BroadcastReceiver() {
		@Override
	    public void onReceive(Context context, Intent intent) {
			String intact = intent.getAction();
			if (intact.equals(UsbManager.ACTION_USB_DEVICE_ATTACHED)) {
//				btnUSBHome.getBackground().setAlpha(255);
    			Toast.makeText(getApplicationContext(), getResources().getString(R.string.usb_detect1), 
						Toast.LENGTH_SHORT).show();
		        Log.d("HI USB");
//			} else if (!usbDetect(0) && intact.equals(UsbManager.ACTION_USB_DEVICE_DETACHED)) {
			} else if (intact.equals(UsbManager.ACTION_USB_DEVICE_DETACHED)) {
//				btnUSBHome.getBackground().setAlpha(70);
		        Log.d("BYE USB");
			} else {
				Log.e("usb but no usb??");
			}
		}
	};
	
	private boolean usbDetect (int mode) {
		UsbManager usbManager = (UsbManager) getSystemService(Context.USB_SERVICE);
		HashMap<String, UsbDevice> usbMap = usbManager.getDeviceList();
		final int hiddenusbs = 1;
//		Log.e("aloha usb# " + (usbMap.size() - hiddenusbs));
		if (mode == 0) {
			if(usbMap.size() > hiddenusbs) {
				return true;
			} else {
				return false;
			}
		} else if (mode == 1) {
			if(usbMap.size() > hiddenusbs) {
				btnUSBHome.getBackground().setAlpha(255);
				return true;
			} else {
				btnUSBHome.getBackground().setAlpha(70);
				return false;
			}
		} else {
			return false;
		}
	}

	//tml*** beta ui2
	private OnGenericMotionListener sideBarMotionListener = new OnGenericMotionListener() {
		@Override
		public boolean onGenericMotion(View v, MotionEvent event) {
			int action = event.getAction();
			int source = event.getSource();
			
			if (source == InputDevice.SOURCE_MOUSE) {
				switch (action) {
					case MotionEvent.ACTION_HOVER_ENTER:
						if (v.getId() == R.id.sidebar_ghost) {
							Log.d("aloha mouse onto sidebar zone");
							if (!((DrawerLayout) findViewById(R.id.main_content)).isDrawerOpen((RelativeLayout) findViewById(R.id.sidebar_frame_drawer))) {
								((DrawerLayout) findViewById(R.id.main_content)).openDrawer((RelativeLayout) findViewById(R.id.sidebar_frame_drawer));
							}
						}
						break;
					case MotionEvent.ACTION_HOVER_EXIT:
						if (v.getId() == R.id.sidebar_ghost) {
							Log.d("aloha mouse left sidebar zone");
							if (((DrawerLayout) findViewById(R.id.main_content)).isDrawerOpen((RelativeLayout) findViewById(R.id.sidebar_frame_drawer))) {
								mmHandler.postDelayed(new Runnable() {
									@Override
									public void run() {
										((DrawerLayout) findViewById(R.id.main_content)).closeDrawer((RelativeLayout) findViewById(R.id.sidebar_frame_drawer));
									}
								}, 500);
							}
						}
						break;
				}
			}
			return false;
		}
	};
}
