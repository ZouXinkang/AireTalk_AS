package com.pingshow.airecenter;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.ThumbnailUtils;
import android.os.Build;
import android.provider.MediaStore.Video;
import android.text.format.DateFormat;
import android.text.format.DateUtils;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.pingshow.airecenter.R;
import com.pingshow.network.MyNet;
import com.pingshow.util.ImageUtil;
import com.pingshow.util.MyUtil;
import com.pingshow.util.OpenDifferentFile;

public class SecurityHistoryAdapter extends BaseAdapter{

	private List<Map<String, String>> itemList;
	private Context mContext;
	private MyPreference mPref;
	
	public SecurityHistoryAdapter(Context context)
	{
		mContext=context;
		mPref = new MyPreference(mContext);
	}
	
	public void setItemList(List<Map<String, String>> list) {
		itemList=list;
	}
	
	@Override
	public int getCount() {
		return itemList.size();
	}

	@Override
	public Object getItem(int arg0) {
		return itemList.get(arg0);
	}

	@Override
	public long getItemId(int position) {
		return position;
	}

	class ViewHolder {
		ImageView thumb;
		TextView timeView;
		TextView lengthView;
		TextView uploadName;
		ProgressBar uploadStatus;
		ImageView uploadFail;
		ImageView uploadOk;
	}
	
	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		Map<String, String> map = itemList.get(position);
		
		ViewHolder holder;
		
		if (convertView == null) {
			
			holder = new ViewHolder();
			convertView = View.inflate(mContext, R.layout.video_item_cell, null);
			
			holder.thumb = (ImageView) convertView.findViewById(R.id.thumb);
			holder.timeView = (TextView) convertView.findViewById(R.id.time);
			holder.lengthView = (TextView) convertView.findViewById(R.id.length);
			holder.uploadName = (TextView) convertView.findViewById(R.id.upload_name);
			holder.uploadStatus = (ProgressBar) convertView.findViewById(R.id.upload_status);
			holder.uploadFail = (ImageView) convertView.findViewById(R.id.upload_fail);
			holder.uploadOk = (ImageView) convertView.findViewById(R.id.upload_ok);
			convertView.setTag(holder);
		} else {
			holder = (ViewHolder) convertView.getTag();
		}
		String filePath=(String)map.get("0");
		
		if (filePath!=null)
		{
			Bitmap bmp=getVideoThumbnail(mContext, filePath);
			holder.thumb.setImageBitmap(bmp);
		}

		long ts=Long.parseLong((String)map.get("1"));
		holder.timeView.setText(ShowBetterTime(ts,new Date().getTime()));
		holder.lengthView.setText((String)map.get("2"));
		String date = "nodate";
		if (map.get("3") != null) {  //tml*** upload suv
			date = (String) map.get("3");
		}
		
		holder.thumb.setTag(filePath);
		holder.thumb.setOnClickListener(thumbClickListener);
		
		((Button)convertView.findViewById(R.id.delete)).setTag(filePath);
		((Button)convertView.findViewById(R.id.delete)).setOnClickListener(deleteListener);
		
		((Button)convertView.findViewById(R.id.save)).setTag(filePath);
		((Button)convertView.findViewById(R.id.save)).setOnClickListener(copyClickListener);
		
		//tml*** upload suv
		((Button) convertView.findViewById(R.id.upload)).setTag(filePath + "-" + date);
		((Button) convertView.findViewById(R.id.upload)).setOnClickListener(upClickListener);

		int uploaded = mPref.readInt("suvUP_" + filePath, 0);
		String filename = "(" + date + ") " + filePath.substring(filePath.lastIndexOf("/") + 1).replace(" ", "");
		holder.uploadName.setText(filename);
		
		if (uploaded == 0) {
			holder.uploadName.setVisibility(View.GONE);
			holder.uploadStatus.setVisibility(View.GONE);
			holder.uploadFail.setVisibility(View.GONE);
			holder.uploadOk.setVisibility(View.GONE);
		} else if (uploaded == 1) {
			holder.uploadName.setVisibility(View.GONE);
			holder.uploadStatus.setVisibility(View.VISIBLE);
			holder.uploadFail.setVisibility(View.GONE);
			holder.uploadOk.setVisibility(View.GONE);
		} else if (uploaded == 2) {
			holder.uploadName.setVisibility(View.GONE);
			holder.uploadStatus.setVisibility(View.GONE);
			holder.uploadFail.setVisibility(View.VISIBLE);
			holder.uploadOk.setVisibility(View.GONE);
		} else if (uploaded == 3) {
			holder.uploadName.setVisibility(View.VISIBLE);
			holder.uploadStatus.setVisibility(View.GONE);
			holder.uploadFail.setVisibility(View.GONE);
			holder.uploadOk.setVisibility(View.VISIBLE);
			((Button) convertView.findViewById(R.id.upload)).setVisibility(View.INVISIBLE);
		} else {
			holder.uploadName.setVisibility(View.GONE);
			holder.uploadStatus.setVisibility(View.GONE);
			holder.uploadFail.setVisibility(View.GONE);
			holder.uploadOk.setVisibility(View.GONE);
		}
		//***tml
		return convertView;
	}
	
	OnClickListener copyClickListener=new OnClickListener() 
	{
		public void onClick(View v) {
			String f=(String)v.getTag();
			if(f!=null){
				Intent it=new Intent(mContext, FileBrowerActivity.class);
				it.putExtra("srcFilePath",f);
				it.putExtra("folderOnly", "true");
				((Activity)mContext).startActivityForResult(it, 20);
			}
		}
	};
	//tml*** upload suv
	private String fileTag, filePath, fileDate, fileName, fileDir;
	OnClickListener upClickListener = new OnClickListener() {
		public void onClick(View v) {
			//tml|sw*** new subs model
			String securitySubscription = mPref.read("SecurityDueDate", "---");
	        if (!securitySubscription.startsWith("success")) {
	        	Toast.makeText(mContext, mContext.getString(R.string.expired), Toast.LENGTH_LONG);
	        	Intent it = new Intent(mContext, ShoppingActivity.class);
				mContext.startActivity(it);
	        } else {
				fileTag = (String) v.getTag();
				if (fileTag != null) {
					filePath = backCompatTagDate(fileTag, 0);
					fileDate = backCompatTagDate(fileTag, 1);
					fileTag = filePath;
					new Thread(new Runnable() {
						public void run() {
							Log.e("tmlupClickListener");
							new Thread(readingFileContent).start();
						}
					}).start();
				}
	        }
		}
	};
	//***tml
	OnClickListener thumbClickListener=new OnClickListener() 
	{
		public void onClick(View v) {
			String f=(String)v.getTag();
			if(f!=null){
				File file = new File(f);
				if (file.exists()){
					OpenDifferentFile openDifferentFile = new OpenDifferentFile(mContext);
					openDifferentFile.openFile(f);
				}
			}
		}
	};
	
	OnClickListener deleteListener=new OnClickListener() 
	{
		public void onClick(View v) {
			String f=(String)v.getTag();
			if(f!=null){
				File file = new File(f);
				file.delete();
			}
			
			if (SecurityNewActivity.getInstance()!=null)
			{
				SecurityNewActivity.getInstance().onDeleteFile(f);
			}
			mPref.delect("suvUP_" + f);
		}
	};

	static public String ShowBetterTime(long when, long now)
	{
		try{
			String s=(String) DateUtils.getRelativeTimeSpanString(when,now,DateUtils.MINUTE_IN_MILLIS,DateUtils.FORMAT_ABBREV_TIME);
			if (s.length()>10) s=s.trim();
			return s;
		}catch(Exception e){}
		return "";
	}
	
	private Bitmap getVideoThumbnail(Context context, String filePath) 
	{	
		String thumbnailPath=Global.SdcardPath_record+"thumb_"+filePath.substring(filePath.lastIndexOf("/")+1)+".jpg";
		//tml*** suv save dest
		if (mPref.readBoolean("suvsavedest", false)) {
			//TODO
			String suvsavedest_ext = mPref.read("suvsavedest_ext", mContext.getResources().getString(R.string.recording_external));
			File extpath = new File(suvsavedest_ext);
			String extpathName = extpath.getPath();
		} else {
			//TODO
		}
		
		Bitmap thumb;
		if (new File(thumbnailPath).exists())
		{
			thumb = BitmapFactory.decodeFile(thumbnailPath);
		}
		else{
			Bitmap videobitmap=null;
			
			if (Integer.parseInt(Build.VERSION.SDK) >= 8)
				videobitmap = ThumbnailUtils.createVideoThumbnail(new File(filePath).getAbsolutePath(), Video.Thumbnails.FULL_SCREEN_KIND);
			
			if (videobitmap == null)
				videobitmap = BitmapFactory.decodeResource(context.getResources(), R.drawable.novideo);
			
			Bitmap play=BitmapFactory.decodeResource(context.getResources(), R.drawable.start_play);
			thumb=ImageUtil.combineImages(context, videobitmap, play, true);
			try {
				File file = new File(thumbnailPath);
				FileOutputStream outStream = new FileOutputStream(file);
				thumb.compress(Bitmap.CompressFormat.JPEG, 100, outStream);
			    outStream.flush();
			    outStream.close();
			} catch (Exception e) {}
		}
		
		return thumb;
	}
	//tml*** upload suv
	private volatile boolean Running = false;
	private volatile int partUse;
	private ArrayList<byte[]> mPartitionQueue = new ArrayList<byte[]>();
	private ArrayList<String> mFilePaths = new ArrayList<String>();
	Runnable readingFileContent = new Runnable() {
		public void run() {
			Running = false;
			boolean partingRun = false;
			boolean partitionOK = false;
			
			if (mPartitionQueue != null && mPartitionQueue.size() > 0) mPartitionQueue.clear();
			if (mFilePaths != null && mFilePaths.size() > 0) mFilePaths.clear();
			
			File file = new File(filePath);
			long fileSize = file.length();
			if (fileSize <= 0) {
				mPref.write("suvUP_" + fileTag, 2);
				SecurityNewActivity.getInstance().notifyHistory(2);
				Running = false;
				return;
			}
			fileDir = filePath.substring(0, filePath.lastIndexOf("/") + 1);
			fileName = filePath.substring(fileDir.length());
			Log.e("tmlup prep fileDir=" + fileDir + " filePath=" + filePath);
			Log.e("tmlup prep fileName=" + fileName + " fileSize=" + fileSize);
			
			double partSize2 = 10000000;
			double partSize1 =  1000000;
			double partUnit  =  1000000;
			int numPartsRU, numPartsRD;
			numPartsRU = (int) Math.ceil((fileSize / partSize1));
			numPartsRD = (int) Math.ceil((fileSize / (int) partSize1));
			partUse = (int) (partSize1 / partUnit);
			if (numPartsRU > 100) {
				numPartsRU = (int) Math.ceil((fileSize / partSize2));
				numPartsRD = (int) Math.ceil((fileSize / (int) partSize2));
				partUse = (int) (partSize2 / partUnit);
			}
			Log.e("tmlup prep numParts(" + partUse + ")=" + numPartsRU + "," + numPartsRD);

			FileInputStream frIStream = null;
			int remainSize;
			int chunkSize = (int) (partUse * partUnit);
			int chunkRead = 0;
			int readLength;
			partingRun = true;
			
			do {
				try {
					frIStream = new FileInputStream(new File(filePath));
				} catch (Exception e) {
					Log.e("FileInputStream ERR! " + e.getMessage());
					partingRun = false;
					return;
				}

				for (int i = 0; i < numPartsRU; i++) {
					try {
						remainSize = (int) fileSize - i * chunkSize;
						if (remainSize < chunkSize) {
							chunkSize = remainSize;
						}
						byte[] fileContent = new byte[chunkSize];
						readLength = frIStream.read(fileContent);
						if (readLength >= 0) {
							chunkRead++;
							mPartitionQueue.add(fileContent);
							Log.e("tmlup read +" + chunkRead + " fileContent=" + readLength);
//							Thread.sleep(10 + mOutputQueue.size() * 20);
						}
						
//						if (frIStream != null) {
//							frIStream.close();
//							frIStream = null;
//						}
					} catch (Exception e) {
						Log.e("readFileContent ERR " + e.getMessage());
						partingRun = false;
						break;
					}
				}

				try {
					if (frIStream != null) {
						frIStream.close();
						frIStream = null;
					}
				} catch (Exception e) {
					frIStream = null;
				} finally {
					if (!partingRun) break;
				}

				FileOutputStream fwOStream = null;
				int partNum, resumSize = 0;
				
				for (int i = 0; i < numPartsRU; i++) {
					partNum = i + 1;
					String partNumS;
					if (partNum < 10) {
						partNumS = "_00" + Integer.toString(partNum);
					} else if (partNum < 100) {
						partNumS = "_0" + Integer.toString(partNum);
					} else {
						partNumS = "_" + Integer.toString(partNum);
					}
					String newPartFileDir = Global.SdcardPath_temp;
					String newPartFileNameN = fileName.substring(0, fileName.lastIndexOf(".")) + partNumS;
					String newPartFilePath = newPartFileDir + newPartFileNameN;
					mFilePaths.add(newPartFilePath);
//					String getNewPartFilePath = mFilePaths.get(i); //test
//					File newPartFile = new File(getNewPartFilePath);
					File newPartFile = new File(newPartFilePath);
					if (newPartFile.exists()) {
						newPartFile.delete();
					}
					
					try {
						byte[] newFileContent = mPartitionQueue.remove(0);
						fwOStream = new FileOutputStream(new File(newPartFilePath));
						fwOStream.write(newFileContent);
						
						if (fwOStream != null) {
							fwOStream.close();
							fwOStream = null;
						}
						
						int newPartFileLength = (int) newPartFile.length();
						resumSize = resumSize + newPartFileLength;
						Log.e("tmlup write newFilePath=" + newPartFilePath + " " + newPartFileLength);
//						Log.e("tmlup write newFilePath=" + getNewPartFilePath + " " + newFileLength); //test
					} catch (Exception e) {
						Log.e("writeFileContent ERR " + e.getMessage());
						break;
					}
				}

				try {
					if (fwOStream != null) {
						fwOStream.close();
						fwOStream = null;
					}
				} catch (Exception e) {
					fwOStream = null;
				}

				int totalPartFiles = 0;
				if (mFilePaths != null && mFilePaths.size() > 0) {
					totalPartFiles = mFilePaths.size();
				}
				
				if (partingRun) partitionOK = true;
				Log.e("tmlup partition END" + partingRun + " " + resumSize + " #" + totalPartFiles);
				
				partingRun = false;
			} while (partingRun);
			
			partingRun = false;
			if (partitionOK) {
				readyUploadFileContent(0);
			} else {
				mPref.write("suvUP_" + fileTag, 2);
			}
		}
	};

	private void readyUploadFileContent(int offset) {
		uploadedCount = 0;
		abortupload = false;
		if (mFilePaths != null && mFilePaths.size() > 0) {
			String path;
			int countFilePaths = mFilePaths.size();

			mPref.write("suvUP_" + fileTag, 1);
			SecurityNewActivity.getInstance().notifyHistory(1);
			
			for (int i = offset; i < countFilePaths; i++) {
				UploadFileContent uploadFileContent = new UploadFileContent();
				path = mFilePaths.get(i);
				uploadFileContent.setData(countFilePaths, path);
				new Thread(uploadFileContent).start();
//				if (i == 3) abortupload = true; //test
				MyUtil.Sleep(300);
				if (abortupload) break;
			}
			if (abortupload) {
				mPref.write("suvUP_" + fileTag, 2);
				SecurityNewActivity.getInstance().notifyHistory(2);
				Running = false;
				for (int i = offset; i < countFilePaths; i++) {
					path = mFilePaths.get(i);
					MyNet net = new MyNet(mContext);
					net.stopUploading(path);
				}
			}
			mFilePaths.clear();
		} else {
			mPref.write("suvUP_" + fileTag, 2);
			SecurityNewActivity.getInstance().notifyHistory(2);
			Running = false;
		}
	}
	
	private volatile int uploadedCount = 0;
	private volatile boolean abortupload = false;
	public class UploadFileContent implements Runnable {
		private String uFilePath;
		private int fileCount;
		
		public void setData(int count, String path) {
			this.uFilePath = path;
			this.fileCount = count;
		}
		
		@Override
		public void run() {
			int myidx = Integer.valueOf(mPref.read("myID", "0"), 16);
			String myid = mPref.read("myPhoneNumber");
			String Return = "";
//			String Return = "Done"; //test
			try {
				File file = new File(uFilePath);
				if (file.exists()) {
					String filename = uFilePath.substring(uFilePath.lastIndexOf("/") + 1).replace(" ", "");
					filename = URLEncoder.encode(filename, "UTF-8");
					int count = 0;
					do {
						if (abortupload) return;
						Log.e("tmlup UPLOAD! myidx=" + myidx + " file=" + uFilePath + " name=" + filename);
						MyNet net = new MyNet(mContext);
						Return = net.doPostAttachSUV("http://" + AireJupiter.myPhpServer_main + "/onair/airesecurityupload.php", myidx,
								filename, uFilePath);
						if (Return.startsWith("Done"))
							break;
						count++;
						MyUtil.Sleep(1000);
					} while (count < 2);
					file.delete();
				}
    		} catch (Exception e) {
    			Log.e("tmlup UploadFileContent ERR " + e.getMessage());
    		}
			
			if (Return.contains("Done")) {
    			uploadedCount++;
    			Log.e("tmlupload uploadedCount=" + uploadedCount + "/" + fileCount);
	        } else {
				mPref.write("suvUP_" + fileTag, 2);
				abortupload = true;
				Running = false;
				SecurityNewActivity.getInstance().notifyHistory(2);
				Running = false;
	        	Log.e("tmlupload FAIL");
	        }
			
			if (uploadedCount == fileCount) {
				new Thread(notifyUploadSUV).start();
			}
		}
	}
	
	Runnable notifyUploadSUV = new Runnable() {
		@Override
		public void run() {
			int myidx = Integer.valueOf(mPref.read("myID", "0"), 16);
			String myid = mPref.read("myPhoneNumber");
			String Return = "";
//			String Return = "Done"; //test
			try {
				String filename = filePath.substring(filePath.lastIndexOf("/") + 1).replace(" ", "");
				String fileext = filename.substring(filename.lastIndexOf("."));
				String filesrc = filename.substring(0, filename.lastIndexOf("."));
				filesrc = URLEncoder.encode(filesrc, "UTF-8");
				filename = filesrc + "-" + fileDate + fileext;
				filename = URLEncoder.encode(filename, "UTF-8");
				myid = URLEncoder.encode(myid, "UTF-8");
				int count = 0;
				do {
					Log.e("tmlup NOTIFY! myidx=" + myidx + " myname=" + myid + " name=" + filename + " src=" + filesrc);
					MyNet net = new MyNet(mContext);
					Return = net.doAnyPostHttp("http://" + AireJupiter.myPhpServer_main + "/onair/merge.php",
							"id=" + myid
							+ "&source=" + filesrc
							+ "&output=" + filename);
					if (Return.startsWith("Done"))
						break;
					count++;
					MyUtil.Sleep(1000);
				} while (count < 3);
    		} catch (Exception e) {
    			Log.e("tmlup notifyUploadSUV ERR " + e.getMessage());
    		}
			
			if (Return.contains("Done")) {
				mPref.write("suvUP_" + fileTag, 3);
				SecurityNewActivity.getInstance().notifyHistory(3);
	        } else {
				mPref.write("suvUP_" + fileTag, 2);
				SecurityNewActivity.getInstance().notifyHistory(2);
	        	Log.e("tmlupload notify FAIL");
	        }
			Running = false;
		}
	};
	
	public String backCompatTagDate(String tag, int part) {
		String goodTag = "";
		String dateext = tag.substring(tag.length() - 13, tag.length());
		if (part == 0) {  //path
			if (dateext.contains("-")) {
				goodTag = tag.substring(0, tag.lastIndexOf("-"));
			} else {
				goodTag = tag;
			}
		} else if (part == 1) {  //date
			if (dateext.contains("-")) {
				goodTag = dateext.substring(dateext.indexOf("-") + 1);
			} else {
				goodTag = "nodate";
			}
		}
//		Log.e("tmlup backCompatTagDate" + part + "=" + tag + " > " + goodTag);
		return goodTag;
	}
	//***tml
}
