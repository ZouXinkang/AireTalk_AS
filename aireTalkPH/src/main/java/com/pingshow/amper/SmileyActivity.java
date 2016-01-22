package com.pingshow.amper;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Handler;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.view.View.OnClickListener;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TableLayout;
import android.widget.TableLayout.LayoutParams;
import android.widget.TableRow;
import android.widget.ToggleButton;

import com.pingshow.util.AsyncImageLoader;

public class SmileyActivity extends Activity 
{
	private static final int COLUMN = 6; 
	private LinearLayout mainLayout = null;
	private TableLayout emoticonTable = null;
	private TableLayout AnimListView = null;
	public static int index = 0;
	float mDensity=1.0f;
	private boolean largeScreen=false;
	private MyPreference mPref;
	private Handler mHandler=new Handler();
	
	public static Object[][] smiles = {
		{":-)",R.drawable.sm01},
		{":-D",R.drawable.sm02},
		{":-[",R.drawable.sm03},
		{";-)",R.drawable.sm04},
		{":-p",R.drawable.sm05},
		{":-*",R.drawable.sm06},
		
		{"o_O",R.drawable.sm07},
		{":-$",R.drawable.sm08},
		{":-(",R.drawable.sm09},
		{"*^*",R.drawable.sm10},
		{"`:|",R.drawable.sm11},
		{":'(",R.drawable.sm12},
		
		{"(d)",R.drawable.sm13},
		{":-O",R.drawable.sm14},
		{":-o",R.drawable.sm15},
		{"@_@",R.drawable.sm16},
		{"B-|",R.drawable.sm17},
		{"B-}",R.drawable.sm18},
		
		{"(H)",R.drawable.sm19},
		{"(F)",R.drawable.sm20},
		{"(Sk)",R.drawable.sm21},
		{"$_$",R.drawable.sm22},
		{"(W)",R.drawable.sm23},
		{"(u)",R.drawable.sm24},
		
		{"A--",R.drawable.sm25},
		{"(Sl.",R.drawable.sm26},
		{"(B)",R.drawable.sm27},
		{"(Ju.",R.drawable.sm28},
		{"@-}--",R.drawable.sm29},
		{"(fa.",R.drawable.sm30},
		
		{"(Sn.",R.drawable.sm31},
		{"(Ca.",R.drawable.sm32},
		{"(Sc.",R.drawable.sm33},
		{"(Sp.",R.drawable.sm34},
		{"(Ri.",R.drawable.sm35},
		{"(Y)",R.drawable.sm36},
		
		{"(Pr.",R.drawable.sm37},
		{"(L)",R.drawable.sm38},
		{"(Al.",R.drawable.sm39},
		{"(De.",R.drawable.sm40},
		{"(Co.",R.drawable.sm41},
		{"(Mi.",R.drawable.sm42},
		
		{"(Fl.",R.drawable.sm43},
		{"Dog",R.drawable.sm44},
		{"Owl",R.drawable.sm45},
		{"(Xm.",R.drawable.sm46},
		{"(Pi.",R.drawable.sm47},
		{"(ok.",R.drawable.sm48},
		
		{"(Bo.",R.drawable.sm49},
		{"(No.",R.drawable.sm50},
		{"(Ho.",R.drawable.sm51},
		{"(Re.",R.drawable.sm52},
		{"(Sn2.",R.drawable.sm53},
		{"(Sa.",R.drawable.sm54},
		
		{"@O@",R.drawable.sm55},
		{"Pig",R.drawable.sm56},
		{"(Te.",R.drawable.sm57},
		{"(Pe.",R.drawable.sm58},
		{"(Bc.",R.drawable.sm59},
		{"(Bu.",R.drawable.sm60},
		
		{"(Tb.",R.drawable.sm61},
		{"(k)",R.drawable.sm62},
		{"(?)",R.drawable.sm63},
		{"(Dr.",R.drawable.sm64},
		{"(Ya.",R.drawable.sm65},
		{":[]",R.drawable.sm66},
		
		{"(Vm)",R.drawable.sm67}, // voice
		{"(iPh)",R.drawable.sm68},
		{"(vExp)",R.drawable.sm69},
		{"(vdo)",R.drawable.sm70},// video
		{"(fl)",R.drawable.sm71}, // file
		{"(mAp)",R.drawable.mapview}, //map
		{"(mCl)",android.R.drawable.sym_call_missed},
		{"(COt)",android.R.drawable.sym_call_outgoing}, //map
		{"(iCc)",android.R.drawable.sym_call_incoming}, //map
		
		{"(g.f001)",R.drawable.em001}, // gif 75
		{"(g.f002)",R.drawable.em002},
		{"(g.f003)",R.drawable.em003},
		{"(g.f004)",R.drawable.em004},
		{"(g.f005)",R.drawable.em005},
		{"(g.f006)",R.drawable.em006},
		{"(g.f007)",R.drawable.em007},
		{"(g.f008)",R.drawable.em008},
		{"(g.f009)",R.drawable.em009},
		{"(g.f010)",R.drawable.em010},
		{"(g.f011)",R.drawable.em011},
		{"(g.f012)",R.drawable.em012},
		{"(g.f013)",R.drawable.em013},
		{"(g.f014)",R.drawable.em014},
		{"(g.f015)",R.drawable.em015},
		{"(g.f016)",R.drawable.em016},
		{"(g.f017)",R.drawable.em017},
		{"(g.f018)",R.drawable.em018},
		{"(g.f019)",R.drawable.em019},
		{"(g.f020)",R.drawable.em020},
		{"(g.f021)",R.drawable.em021},
		{"(g.f022)",R.drawable.em022},	// gif 96
		{"(g.f023)",R.drawable.em023},
		{"(g.f024)",R.drawable.em024},
		
		{"(g.f025)",R.drawable.em025},
		{"(g.f026)",R.drawable.em026},
		{"(g.f027)",R.drawable.em027},
		{"(g.f028)",R.drawable.em028},
		{"(g.f029)",R.drawable.em029},
		{"(g.f030)",R.drawable.em030},
		{"(g.f031)",R.drawable.em031},
		{"(g.f032)",R.drawable.em032},
		{"(g.f033)",R.drawable.em033},
		{"(g.f034)",R.drawable.em034},
		{"(g.f035)",R.drawable.em035},
		{"(g.f036)",R.drawable.em036},
		{"(g.f037)",R.drawable.em037},
		{"(g.f038)",R.drawable.em038},
		{"(g.f039)",R.drawable.em039},
		{"(g.f040)",R.drawable.em040},
		
		{"(g.f041)",R.drawable.em041},
		{"(g.f042)",R.drawable.em042},
		{"(g.f043)",R.drawable.em043},
		{"(g.f044)",R.drawable.em044},
		{"(g.f045)",R.drawable.em045},
		{"(g.f046)",R.drawable.em046},
		{"(g.f047)",R.drawable.em047},
		{"(g.f048)",R.drawable.em048},
		{"(g.f049)",R.drawable.em049},
		{"(g.f050)",R.drawable.em050},
		{"(g.f051)",R.drawable.em051},
		{"(g.f052)",R.drawable.em052},
		{"(g.f053)",R.drawable.em053},
		{"(g.f054)",R.drawable.em054},
		{"(g.f055)",R.drawable.em055},
		{"(g.f056)",R.drawable.em056},
		{"(g.f057)",R.drawable.em057},
		{"(g.f058)",R.drawable.em058},
		{"(g.f059)",R.drawable.em059},
		{"(g.f060)",R.drawable.em060},
		
		{"(g.f061)",R.drawable.em061},
		{"(g.f062)",R.drawable.em062},
		{"(g.f063)",R.drawable.em063},
		{"(g.f064)",R.drawable.em064},
		{"(g.f065)",R.drawable.em065},
		{"(g.f066)",R.drawable.em066},
		{"(g.f067)",R.drawable.em067},
		{"(g.f068)",R.drawable.em068},
		{"(g.f069)",R.drawable.em069},
		{"(g.f070)",R.drawable.em070},
		{"(g.f071)",R.drawable.em071},
		{"(g.f072)",R.drawable.em072},
		{"(g.f073)",R.drawable.em073},
		{"(g.f074)",R.drawable.em074},
		{"(g.f075)",R.drawable.em075},
		{"(g.f076)",R.drawable.em076},
		{"(g.f077)",R.drawable.em077},
		{"(g.f078)",R.drawable.em078},
		{"(g.f079)",R.drawable.em079},
		{"(g.f080)",R.drawable.em080},
		
		{"(g.f081)",R.drawable.em081},
		{"(g.f082)",R.drawable.em082},
		{"(g.f083)",R.drawable.em083},
		{"(g.f084)",R.drawable.em084},
		{"(g.f085)",R.drawable.em085},
		{"(g.f086)",R.drawable.em086},
		{"(g.f087)",R.drawable.em087},
		{"(g.f088)",R.drawable.em088},
		{"(g.f089)",R.drawable.em089},
		{"(g.f090)",R.drawable.em090},
		{"(g.f091)",R.drawable.em091},
		{"(g.f092)",R.drawable.em092},
		{"(g.f093)",R.drawable.em093},
		{"(g.f094)",R.drawable.em094},
		{"(g.f095)",R.drawable.em095},
		{"(g.f096)",R.drawable.em096},
		{"(g.f097)",R.drawable.em097},
		{"(g.f098)",R.drawable.em098},
		{"(g.f099)",R.drawable.em099},
		{"(g.f100)",R.drawable.em100},
		{"(iMG)",null}
		// zhao,please insert into the second last positon when add smile
	};
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.smile_dialog);
		
		this.overridePendingTransition(R.anim.slide_slow_down, R.anim.freeze);
		
		largeScreen=(findViewById(R.id.large)!=null);
		mDensity=getResources().getDisplayMetrics().density;
		Log.d("smiley::mDensity==="+mDensity);
		android.view.WindowManager.LayoutParams lp=getWindow().getAttributes();
		//tml*** beta ui
		lp.gravity = Gravity.TOP;
		lp.y = (int) ((largeScreen? 80 : 50) * mDensity);
//		lp.x=0;
//		lp.y=(int)((largeScreen?-160.:-83.)*mDensity);
		lp.width=LayoutParams.FILL_PARENT;
		lp.height=(int)(mDensity*(largeScreen?420:300));
		lp.flags = WindowManager.LayoutParams.FLAG_DIM_BEHIND;
		lp.dimAmount = 0.5f;
		getWindow().setAttributes(lp);
		
		mainLayout = (LinearLayout)findViewById(R.id.smiles);
		
		mPref=new MyPreference(this);
		if (mPref.readInt("SmileyType",1)==1)
		{
			((ToggleButton)findViewById(R.id.animated)).setChecked(true);
			initGifTable();
		}else{
			((ToggleButton)findViewById(R.id.smileys)).setChecked(true);
			initSmile();
		}
		
		((Button)findViewById(R.id.close)).setOnClickListener(new OnClickListener()
		{
			@Override
			public void onClick(View v) {
				finish();
			}
		});
		
		((ToggleButton)findViewById(R.id.smileys)).setOnClickListener(new OnClickListener()
		{
			@Override
			public void onClick(View v) {
				((ToggleButton)findViewById(R.id.animated)).setChecked(false);
				((ToggleButton)findViewById(R.id.smileys)).setEnabled(false);
				((ToggleButton)findViewById(R.id.animated)).setEnabled(true);
				initSmile();
				mPref.write("SmileyType", 0);
			}
		});
		
		((ToggleButton)findViewById(R.id.animated)).setOnClickListener(new OnClickListener()
		{
			@Override
			public void onClick(View v) {
				((ToggleButton)findViewById(R.id.smileys)).setChecked(false);
				((ToggleButton)findViewById(R.id.animated)).setEnabled(false);
				((ToggleButton)findViewById(R.id.smileys)).setEnabled(true);
				initGifTable();
				mPref.write("SmileyType", 1);
			}
		});
	}
	
	private void initSmile()
	{
		if (emoticonTable!=null) {
			if (AnimListView!=null) AnimListView.setVisibility(View.GONE);
			emoticonTable.setVisibility(View.VISIBLE);
			return;
		}
		
		emoticonTable=new TableLayout(this);
		
		int row = 11;
		int sum = 0;
		for(int r = 0;r<row;r++)
		{
			TableRow tableRow = new TableRow(this);
			for(int c = 0;c<COLUMN;c++)
			{
				if(sum>=66 && sum <=70)
				{
					c--;
					sum++;
					continue;
				}
				if(sum==smiles.length-1) break;
				ImageButton btn = new ImageButton(this);
				int id = Integer.valueOf(String.valueOf(smiles[sum][1]));
				btn.setImageResource(id);
				btn.setBackgroundResource(R.drawable.optionbtn);
				btn.setScaleType(ScaleType.FIT_CENTER);
				btn.setMinimumHeight(36);
				btn.setPadding((int)(4.*mDensity), (int)(7.5*mDensity), (int)(4.*mDensity), (int)(7.5*mDensity));
				btn.setTag(sum);
				btn.setOnClickListener(new OnClickListener()
				{
					@Override
					public void onClick(View v) 
					{
						index = Integer.valueOf(String.valueOf(v.getTag()));
						Intent intent = new Intent();
						intent.putExtra("index",index);
						setResult(RESULT_OK,intent);
						finish();
					}
				});
				TableRow.LayoutParams lp = null;
				if(sum>=71)
					lp = new TableRow.LayoutParams((int)(80.*mDensity), (int)(80.*mDensity));
				else{
					if (largeScreen)
						lp = new TableRow.LayoutParams(LayoutParams.FILL_PARENT, (int)(48.*mDensity));
					else
						lp = new TableRow.LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT);
				}
				lp.weight=1;
				lp.topMargin=5;
				lp.leftMargin=5;
				lp.rightMargin=5;
				lp.bottomMargin=5;
				btn.setLayoutParams(lp);
				tableRow.addView(btn);
				sum++;
			}
			emoticonTable.addView(tableRow, new TableLayout.LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT));
		}
		
		if (AnimListView!=null) AnimListView.setVisibility(View.GONE);
		mainLayout.addView(emoticonTable,new LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT));
	}
	
	private int sum=0;
	private void initGifTable()
	{
		if (AnimListView!=null) {
			if (emoticonTable!=null) emoticonTable.setVisibility(View.GONE);
			AnimListView.setVisibility(View.VISIBLE);
			return;
		}
		AnimListView=new TableLayout(this);
		AnimListView.setPadding(10, 5, 10, 0);
		
		System.gc();
		System.gc();
		
		int orientation=getResources().getConfiguration().orientation;
		int col = 5;
		
		if (orientation!=1)
			col=7;
		
		int w=(int)((float)getWindowManager().getDefaultDisplay().getWidth()/mDensity/col)-10;
		
		int h=w;
		if (h>100) h=100;
		
		for(int r=0;r<3;r++)
		{
			TableRow tableRow = new TableRow(this);
			for(int c=0;c<col;c++)
			{
				ImageView btn=new ImageView(SmileyActivity.this);
				try{
					BitmapFactory.Options bitmapOptions = new BitmapFactory.Options(); 
					bitmapOptions.inSampleSize=2;
					bitmapOptions.inPurgeable=true;
					Bitmap bitmapOrg = BitmapFactory.decodeResource(getResources(), R.drawable.em001+sum, bitmapOptions);
					btn.setImageBitmap(bitmapOrg);
					btn.setBackgroundResource(R.drawable.optionbtn);
				}catch(Error e){
					System.gc();
					System.gc();
				}

				btn.setTag(sum+75);
				btn.setClickable(true);
				if (orientation==1)
					btn.setPadding((int)(10.*mDensity), 0, (int)(10.*mDensity), 0);
				else
					btn.setPadding((int)(12.*mDensity), 0, (int)(12.*mDensity), 0);
				btn.setOnClickListener(new OnClickListener()
				{
					@Override
					public void onClick(View v) 
					{
						index = Integer.valueOf(String.valueOf(v.getTag()));
						Intent intent = new Intent();
						intent.putExtra("index",index);
						setResult(RESULT_OK,intent);
						finish();
						System.gc();
						System.gc();
					}
				});
//				Log.d("smiley:::w==="+w+";smiley:::h==="+h);
				TableRow.LayoutParams lp = new TableRow.LayoutParams((int)(w*mDensity), (int)(w*mDensity));
				lp.topMargin=5;
				lp.leftMargin=5;
				lp.rightMargin=5;
				btn.setLayoutParams(lp);
				tableRow.addView(btn);
				sum++;
			}
			AnimListView.addView(tableRow, new TableLayout.LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT));
		}
		
		if (emoticonTable!=null) emoticonTable.setVisibility(View.GONE);
		mainLayout.addView(AnimListView,new LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT));
		
		mHandler.postDelayed(gifContinue,500);
	}
	
	Runnable gifContinue=new Runnable(){
		public void run()
		{
			gifTableContinue();
		}
	};
	
	private void gifTableContinue()
	{
		int orientation=getResources().getConfiguration().orientation;
		
		int row = 20;
		int col = 5;
		
		if (orientation!=1)
		{
			col=7;
			row=15;
		}
		
		int w=(int)((float)getWindowManager().getDefaultDisplay().getWidth()/mDensity/col)-10;
		int h=w;
		if (h>100) h=100;
		for(int r=3;r<row;r++)
		{
			TableRow tableRow = new TableRow(this);
			for(int c=0;c<col;c++)
			{
				if (sum>=100) break;
				ImageView btn=new ImageView(SmileyActivity.this);
				try{
					BitmapFactory.Options bitmapOptions = new BitmapFactory.Options(); 
					bitmapOptions.inSampleSize=2;
					bitmapOptions.inPurgeable=true; 
					Bitmap bitmapOrg = BitmapFactory.decodeResource(getResources(), R.drawable.em001+sum, bitmapOptions);
					btn.setImageBitmap(bitmapOrg);
					btn.setBackgroundResource(R.drawable.optionbtn);
				}catch(Error e){
					System.gc();
					System.gc();
				}
				
				btn.setTag(sum+75);
				btn.setClickable(true);
				if (orientation==1)
					btn.setPadding((int)(10.*mDensity), 0, (int)(10.*mDensity), 0);
				else
					btn.setPadding((int)(12.*mDensity), 0, (int)(12.*mDensity), 0);
				btn.setOnClickListener(new OnClickListener()
				{
					@Override
					public void onClick(View v) 
					{
						index = Integer.valueOf(String.valueOf(v.getTag()));
						Intent intent = new Intent();
						intent.putExtra("index",index);
						setResult(RESULT_OK,intent);
						finish();
						System.gc();
						System.gc();
					}
				});
				Log.d("smiley:::w==="+w+";smiley:::h==="+h);
				TableRow.LayoutParams lp = new TableRow.LayoutParams((int)(w*mDensity), (int)(w*mDensity));
				lp.topMargin=5;
				lp.leftMargin=5;
				lp.rightMargin=5;
				btn.setLayoutParams(lp);
				tableRow.addView(btn);
				sum++;
			}
			AnimListView.addView(tableRow, new TableLayout.LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT));
		}
	}

	@Override
	protected void onDestroy() {
		System.gc();
		System.gc();
		super.onDestroy();
	}
	
	@Override
	public void onResume() {
		super.onResume();
//		MobclickAgent.onResume(this);
	}
	
	@Override
	public void onPause() {
		mHandler.removeCallbacks(gifContinue);
//		MobclickAgent.onPause(this);
		super.onPause();
	}
}
