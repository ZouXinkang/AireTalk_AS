
package com.pingshow.codec;

public class Ampitude {
	static float avgAmp=0;
	public static void feedAmp(short []pcm, int offset, int size) {
		int max=0;
		for (int i=offset; i<offset+size; i+=16)
		{
			if (Math.abs(pcm[i])>max)
				max=pcm[i];
		}
		avgAmp=(0.6f*avgAmp+0.4f*max);
	}
	public static float getAmp() {
		return avgAmp/28000;
	}
	public static void resetAmp() {
		avgAmp=0;
	}
}
