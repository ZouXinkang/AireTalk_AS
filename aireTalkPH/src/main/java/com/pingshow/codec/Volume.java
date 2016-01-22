
package com.pingshow.codec;

public class Volume {
	static float ratio=1.f;
	public static void amplify(short []pcm, int offset, int size) {
		int val=0;
		int iRatio=(int)(ratio*128f);
		for (int i=offset; i<offset+size; i++)
		{
			val=iRatio*pcm[i];
			if (val>7829120)
				val=7829120;
			else if (val<-7829120)
				val=-7829120;
			pcm[i]=(short) (val>>7);
		}
	}
	public static void setVolume(float r)
	{
		ratio=r;
	}
	
	public static void amplify75(short []pcm, int size) {
		int val=0;
		int iRatio=96;
		for (int i=0; i<size; i++)
		{
			val=iRatio*pcm[i];
			pcm[i]=(short) (val>>7);
		}
	}
}
