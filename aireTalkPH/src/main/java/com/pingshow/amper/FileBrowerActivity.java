package com.pingshow.amper;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toast;

import com.pingshow.amper.view.FileBrowser;

public class FileBrowerActivity extends Activity implements OnFileBrowserListener
{
	private ImageButton cancel = null;
	private FileBrowser fileBrowser;
	
	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.filebrower);
		fileBrowser = (FileBrowser)findViewById(R.id.filebrowser);
		fileBrowser.setOnFileBrowserListener(this);
		
		((ImageView)findViewById(R.id.cancel)).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				setResult(RESULT_CANCELED);
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
