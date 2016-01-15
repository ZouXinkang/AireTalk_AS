package com.pingshow.airecenter;
//airecenter
public class Log {
	public static boolean enDEBUG = false;

	static public void d(String Msg)
	{
//		android.util.Log.d("AMP",Msg);
		if (enDEBUG) {
			android.util.Log.d("AMP", Msg);
		}
	}
	static public void i(String Msg)
	{
//		android.util.Log.i("AMP",Msg);
		if (enDEBUG) {
			android.util.Log.i("AMP", Msg);
		}
	}
	static public void e(String Msg)
	{
//		android.util.Log.e("AMP",Msg);
		if (enDEBUG) {
			android.util.Log.e("AMP", Msg);
		}
	}
	static public void w(String Msg)
	{
//		android.util.Log.w("AMP",Msg);
		if (enDEBUG) android.util.Log.w("AMP", Msg);
	}
}
