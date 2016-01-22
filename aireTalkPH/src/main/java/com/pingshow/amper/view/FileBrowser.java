package com.pingshow.amper.view;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import android.content.Context;
import android.text.TextUtils.TruncateAt;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import com.pingshow.amper.InflateImageTask;
import com.pingshow.amper.OnFileBrowserListener;
import com.pingshow.amper.R;

public class FileBrowser extends ListView implements
		android.widget.AdapterView.OnItemClickListener
{
	private final String namespace = "http://www.xingfafa.com";
	private String sdcardDirectory;
	private List<File> fileList = new ArrayList<File>();
	private Stack<String> dirStack = new Stack<String>();
	private FileListAdapter fileListAdapter;
	private OnFileBrowserListener onFileBrowserListener;
	private int folderImageResId;
	private int otherFileImageResId;
	private int ImageFileResId;
	private Map<String, Integer> fileImageResIdMap = new HashMap<String, Integer>();
	private boolean onlyFolder = false;
	private int curSelectPos = 0;
	private float mDensity=1.0f;
	private int width;
	private boolean samsungSD=false;
	private Context mContext;
	public FileBrowser(Context context, AttributeSet attrs)
	{
		super(context, attrs);
		mContext=context;
		sdcardDirectory = android.os.Environment.getExternalStorageDirectory().toString();
		File storageDir = new File("/mnt/");
		if(storageDir.isDirectory()){
		    String[] dirList = storageDir.list();
		    for (String path:dirList)
		    {
		    	if (path.contains("extSdCard")||path.equals("external_sd"))
		    	{
		    		sdcardDirectory="/mnt";
		    		samsungSD=true;
		    		break;
		    	}
		    }
		}
		
		setOnItemClickListener(this);
		folderImageResId = R.drawable.folder;
		otherFileImageResId = R.drawable.file;
		onlyFolder = attrs.getAttributeBooleanValue(namespace, "onlyFolder",false);
		mDensity = context.getResources().getDisplayMetrics().density;
		width=context.getResources().getDisplayMetrics().widthPixels;
		
		String [] extName={"jpg","png","bmp","mp3","amr","docx","doc","pdf","mp4","mov","3gp"};
		Integer [] fileImageResId={R.drawable.jpg,R.drawable.jpg,R.drawable.jpg,R.drawable.music,R.drawable.music,
				R.drawable.file_word,R.drawable.file_word,R.drawable.file_pdf,
				R.drawable.file_mp4,R.drawable.file_mp4,R.drawable.file_mp4,R.drawable.file_mp4
		};
		
		for (int i=0;i<extName.length;i++)
		{
			fileImageResIdMap.put(extName[i].toUpperCase(), fileImageResId[i]);
			fileImageResIdMap.put(extName[i].toLowerCase(), fileImageResId[i]);
		}
		
		ImageFileResId=fileImageResId[0];

		dirStack.push(sdcardDirectory);
		addFiles();

		fileListAdapter = new FileListAdapter(getContext());
		setAdapter(fileListAdapter);

	}

	private void addFiles()
	{
		try{
			fileList.clear();
			String currentPath = getCurrentPath();
			File[] files = new File(currentPath).listFiles();
			if (dirStack.size() > 1)
				fileList.add(null);
			List<File> flList = new ArrayList<File>();
			for (File file : files)
			{
				if(file.getName().startsWith(".")) continue;
				if (file.isDirectory() && dirStack.size()==1 && samsungSD)
				{
					boolean omit=false;
					String fn=file.getName().toLowerCase();
					if(!fn.contains("sdcard"))
						omit=true;
					if (omit) continue;
				}
				if (onlyFolder)
				{
					if (file.isDirectory())
						fileList.add(file);
				}
				else
				{
					if (file.isDirectory())
						fileList.add(file);
					else
						flList.add(file);
				}
			}
			fileList = sortList(fileList);
			flList = sortList(flList);
			if (!onlyFolder)
			{
				fileList.addAll(flList);
			}
		}catch(Exception e){}
	}
	private List<File> sortList(List<File> list){
		File tmpFile = null;
		boolean flag = false;
		for(int i=0;i<list.size();i++){
			for(int j=0;j<list.size()-i-1;j++){
				if(list.get(j)==null) continue;
				String tmp1 = list.get(j).getName().toLowerCase();
				String tmp2 = list.get(j+1).getName().toLowerCase();
				if(tmp1.compareTo(tmp2)>0){
					tmpFile = list.get(j);
					list.set(j, list.get(j+1));
					list.set(j+1, tmpFile);
					flag = true;
				}
			}
			if(!flag) break;
		}
		return list;
	}
	private String getCurrentPath()
	{
		String path = "";
		for (String dir : dirStack)
		{
			path += dir + "/";
		}
		path = path.substring(0, path.length() - 1);
		return path;
	}

	private String getExtName(String filename)
	{

		int position = filename.lastIndexOf(".");
		if (position >= 0)
			return filename.substring(position + 1);
		else
			return "";
	}
	public int getSelectedPosition(){
		return curSelectPos;
	}
	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position,
			long id)
	{
		this.curSelectPos = position;
		if (fileList.get(position) == null)
		{
			dirStack.pop();
			addFiles();
			fileListAdapter.notifyDataSetChanged();
			if (onFileBrowserListener != null)
			{
				onFileBrowserListener.onDirItemClick(getCurrentPath());
			}
		}
		else if (fileList.get(position).isDirectory())
		{
			dirStack.push(fileList.get(position).getName());
			addFiles();
			fileListAdapter.notifyDataSetChanged();
			if (onFileBrowserListener != null)
			{
				onFileBrowserListener.onDirItemClick(getCurrentPath());
			}
		}
		else
		{
			if (onFileBrowserListener != null)
			{
				String filename = getCurrentPath() + "/"
						+ fileList.get(position).getName();
				onFileBrowserListener.onFileItemClick(filename);
			}
		}
	}
	
	private class FileListAdapter extends BaseAdapter
	{
		private Context context;

		public FileListAdapter(Context context)
		{
			this.context = context;
		}

		@Override
		public int getCount()
		{
			return fileList.size();
		}

		@Override
		public Object getItem(int position)
		{
			return fileList.get(position);
		}

		@Override
		public long getItemId(int position)
		{
			return position;
		}
		
		class ViewHolder {
			ImageView ivFile;
			TextView tvFile;
			TextView tvSize;
			InflateImageTask async;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent)
		{
			ViewHolder holder;
			if (convertView==null)
			{
				holder=new ViewHolder();
				convertView = new LinearLayout(context);
				((LinearLayout)convertView).setLayoutParams(new LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT));
				((LinearLayout)convertView).setOrientation(LinearLayout.HORIZONTAL);
				((LinearLayout)convertView).setPadding((int)(5.f*mDensity), (int)(5.f*mDensity), 0, (int)(5.f*mDensity));
				
				holder.ivFile = new ImageView(context);
				holder.ivFile.setLayoutParams(new LayoutParams((int)(60.f*mDensity), (int)(60.f*mDensity)));
				
				holder.tvFile = new TextView(context);
				holder.tvFile.setSingleLine();
				holder.tvFile.setTextColor(0xff404040);
				holder.tvFile.setEllipsize(TruncateAt.END);
				holder.tvFile.setTextAppearance(context,android.R.style.TextAppearance_Medium);
				holder.tvFile.setLayoutParams(new LayoutParams(LayoutParams.WRAP_CONTENT, (int)(60.f*mDensity)));
				holder.tvFile.setGravity(Gravity.CENTER_VERTICAL);
				holder.tvFile.setMaxWidth(width-(int)(110*mDensity));
				holder.tvFile.setPadding((int)(5.f*mDensity), 0, 0, 0);
				
				holder.tvSize = new TextView(context);
				holder.tvSize.setTextColor(0xff5dbdbd);
				holder.tvSize.setPadding(0, 0, (int)(5.f*mDensity), 0);
				holder.tvSize.setLayoutParams(new LayoutParams(LayoutParams.FILL_PARENT, (int)(60.f*mDensity)));
				holder.tvSize.setGravity(Gravity.RIGHT|Gravity.CENTER_VERTICAL);
				
				((LinearLayout)convertView).addView(holder.ivFile);
				((LinearLayout)convertView).addView(holder.tvFile);
				((LinearLayout)convertView).addView(holder.tvSize);
				convertView.setTag(holder);
			}else{
				holder=(ViewHolder)convertView.getTag();
				if (holder.async!=null)
					holder.async.stop();
			}
			
			holder.tvFile.setTextColor(0xff404040);
			
			if (fileList.get(position) == null)
			{
				if (folderImageResId > 0)
					holder.ivFile.setImageResource(folderImageResId);
				holder.tvFile.setText(". .");
			}else if (fileList.get(position).isDirectory()){
				if (folderImageResId > 0)
					holder.ivFile.setImageResource(folderImageResId);
				holder.tvFile.setText(fileList.get(position).getName());
			}else{
				holder.tvFile.setText(fileList.get(position).getName());
				Integer resId = fileImageResIdMap.get(getExtName(fileList.get(position).getName()));
				int fileImageResId = 0;
				if (resId != null){
					if (resId > 0){
						fileImageResId = resId;
					}
				}
				if (fileImageResId > 0){
					holder.ivFile.setImageResource(fileImageResId);
					try {
						holder.async=new InflateImageTask(mContext, fileList.get(position).getPath(),fileImageResId);
						holder.async.execute(holder.ivFile);
					}catch (Exception e){}
				}
				else if (otherFileImageResId > 0)
					holder.ivFile.setImageResource(otherFileImageResId);
			}
			
			if (fileList.get(position) != null && !fileList.get(position).isDirectory())
			{
				long size=fileList.get(position).length();
				String label;
				if (size>0)
					label=(size>=1024?(size/1024+" KB"):"1 KB");
				else
					label="0 KB";
				
				holder.tvSize.setText(label);
			}
			else
				holder.tvSize.setText(" ");
			
			return convertView;
		}
	}

	public void setOnFileBrowserListener(OnFileBrowserListener listener) {
		this.onFileBrowserListener = listener;
	}
}