package com.pingshow.beehive;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

import org.json.JSONObject;

public class FileBrowser {
	private String sdcardDirectory;
	private List<File> fileList = new ArrayList<File>();
	private Stack<String> fileStack = new Stack<String>();
	private Stack<String> dirStack = new Stack<String>();
	private JSONObject fileJsonObject = new JSONObject();
	
	public FileBrowser(){
		sdcardDirectory = android.os.Environment.getExternalStorageDirectory().toString();
		dirStack.push(sdcardDirectory);
	}
	
	public JSONObject addFiles()
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
				if (file.isDirectory())
					fileList.add(file);
				else
					flList.add(file);
			}
			fileList = sortList(fileList);
			flList = sortList(flList);
			fileJsonObject.put("folder", fileList);
			fileJsonObject.put("file", flList);
		}catch(Exception e){}
		
		return fileJsonObject;
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

}
