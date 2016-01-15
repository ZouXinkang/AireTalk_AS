package com.pingshow.airecenter;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.pingshow.airecenter.R;
import com.pingshow.airecenter.view.FileBrowser;

public class FileBrowerActivity extends Activity implements OnFileBrowserListener
{
	private FileBrowser fileBrowser;
	private String srcFilePath=null;
	
	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		
		String folderOnly=getIntent().getStringExtra("folderOnly");
		if (folderOnly==null)
			setContentView(R.layout.filebrower);
		else{
			setContentView(R.layout.folderbrower);
			srcFilePath=getIntent().getStringExtra("srcFilePath");
		}
		//tml*** suv save dest
		String newTitle = getIntent().getStringExtra("folderTitle");
		if (newTitle != null) {
			if (newTitle.equals(getString(R.string.friend_address))) {  //"Location"
				((TextView) findViewById(R.id.title)).setText(getString(R.string.friend_address));
			}
		}
		//***tml
		
		fileBrowser = (FileBrowser)findViewById(R.id.filebrowser);
		fileBrowser.setOnFileBrowserListener(this);
		
		((ImageView)findViewById(R.id.cancel)).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				setResult(RESULT_CANCELED);
				finish();
			}
		});
		
		Button done=((Button)findViewById(R.id.done));
		if (done!=null)
			done.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					Intent intent = new Intent();
					intent.putExtra("filePath", fileBrowser.getCurrentPath());
					intent.putExtra("srcFilePath", srcFilePath);
					setResult(RESULT_OK, intent);
					finish();
				}
			});
	}
	
	@Override
	public void onFileItemClick(String filePath)
	{
//		title.setText(filePath);
		String filename="";
		if(filePath.contains("."))
			filename = filePath.substring(filePath.lastIndexOf("/")+1, filePath.lastIndexOf("."));
		else 
			filename=filePath.substring(filePath.lastIndexOf("/")+1, filePath.length());
		if(filename.length()>64){
			Toast.makeText(this, getString(R.string.filename_longerr), Toast.LENGTH_LONG).show();
			return;
		}
		Intent intent = new Intent();
		intent.putExtra("filePath", filePath);
		setResult(RESULT_OK, intent);
		finish();
	}
	@Override
	protected void onDestroy() {
		System.gc();
		System.gc();
		super.onDestroy();
	}
	@Override
	public void onDirItemClick(String path)
	{
//		title.setText(path);
	}
	
	@Override
	protected void onResume() {
		super.onResume();
//		MobclickAgent.onResume(this);
	}

	@Override
	protected void onPause() {
//		MobclickAgent.onPause(this);
		super.onPause();
	}
}
