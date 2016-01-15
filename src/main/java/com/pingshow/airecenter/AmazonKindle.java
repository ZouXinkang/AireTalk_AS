package com.pingshow.airecenter;

import java.util.List;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Build;
import com.pingshow.airecenter.R;

public class AmazonKindle {

	static public boolean IsKindle()
	{
		if (Build.MANUFACTURER.equalsIgnoreCase("Amazon"))
			return true;
		return false;
	}
	
	static public boolean canHandleCameraIntent(Context context)
	{
		if (Build.MANUFACTURER.equalsIgnoreCase("Amazon"))
		{
			if (Build.MODEL.equalsIgnoreCase("KFOT") || Build.MODEL.equalsIgnoreCase("Kindle Fire") || Build.MODEL.equalsIgnoreCase("KFSOWI"))
				return false;
		}
		
		final Intent intent = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
		final List<ResolveInfo> results = context.getPackageManager().queryIntentActivities(intent, 0);
		return (results.size() > 0);
	}
	
	static public boolean hasMicrophone(Context context)
	{
		boolean hasFeature=true;
		
		if (Build.MANUFACTURER.equalsIgnoreCase("Amazon"))
		{
			if (Build.MODEL.equalsIgnoreCase("KFOT") || Build.MODEL.equalsIgnoreCase("Kindle Fire") || Build.MODEL.equalsIgnoreCase("KFSOWI"))
				hasFeature=false;
		}
		
		if (hasFeature)
		{
			PackageManager pm = context.getPackageManager();
			hasFeature=pm.hasSystemFeature(PackageManager.FEATURE_MICROPHONE);
		}
		
		if (!hasFeature)
		{
			Intent it = new Intent(context, CommonDialog.class);
			it.putExtra("msgContent", "Sorry, cannot find audio-in device.");
			it.putExtra("numItems", 1);
			it.putExtra("ItemCaption0", context.getResources().getString(R.string.cancel));
			it.putExtra("ItemResult0", 0);
			context.startActivity(it);
		}
		
		return hasFeature;
	}
	
	static public boolean hasMicrophone_NoWarnning(Context context)
	{
		boolean hasFeature=true;
		
		if (Build.MANUFACTURER.equalsIgnoreCase("Amazon"))
		{
			if (Build.MODEL.equalsIgnoreCase("KFOT") || Build.MODEL.equalsIgnoreCase("Kindle Fire") || Build.MODEL.equalsIgnoreCase("KFSOWI"))
				hasFeature=false;
		}
		
		if (hasFeature)
		{
			PackageManager pm = context.getPackageManager();
			hasFeature=pm.hasSystemFeature(PackageManager.FEATURE_MICROPHONE);
		}
		
		return hasFeature;
	}
}
