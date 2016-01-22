package com.pingshow.codec;


public class XTEA {

	static public void encipher(int num_rounds, int v[], int k[]) {
	    int v0=v[0], v1=v[1], sum=0, delta=0x9E3779B9;
	    for (int i=0; i < num_rounds; i++) {
	        v0 += (((v1 << 4) ^ (v1 >> 5)) + v1) ^ (sum + k[sum & 3]);
	        sum += delta;
	        v1 += (((v0 << 4) ^ (v0 >> 5)) + v0) ^ (sum + k[(sum>>11) & 3]);
	    }
	    v[0]=v0; v[1]=v1;
	}
	
	static public void decipher(int num_rounds, int v[], int k[]) {
	    int v0=v[0], v1=v[1], delta=0x9E3779B9, sum=delta*num_rounds;
	    for (int i=0; i < num_rounds; i++) {
	        v1 -= (((v0 << 4) ^ (v0 >> 5)) + v0) ^ (sum + k[(sum>>11) & 3]);
	        sum -= delta;
	        v0 -= (((v1 << 4) ^ (v1 >> 5)) + v1) ^ (sum + k[sum & 3]);
	    }
	    v[0]=v0; v[1]=v1;
	}
	
	public static int readUnsignedInt(byte [] bytes, int offset) {  
	    int b0 = ((int) (bytes[offset] & 0xff));  
	    int b1 = ((int) (bytes[offset+1] & 0xff)) << 8;  
	    int b2 = ((int) (bytes[offset+2] & 0xff)) << 16;  
	    int b3 = ((int) (bytes[offset+3] & 0xff)) << 24;  
	    return (int) (b0 | b1 | b2 | b3);  
	}
	
	public static void writeUnsignedInt(int src, byte [] bytes, int offset) {  
		bytes[offset]=(byte) (src & 0xff);  
		bytes[offset+1]=(byte) ((src>>8) & 0xff); 
		bytes[offset+2]=(byte) ((src>>16) & 0xff); 
		bytes[offset+3]=(byte) ((src>>24) & 0xff);
	}
	
	public static short readShortInt(byte [] bytes, int offset) {  
	    int b0 = ((int) (bytes[offset] & 0xff));  
	    int b1 = ((int) (bytes[offset+1] & 0xff)) << 8;  
	    return (short) (b0 | b1);  
	}
	
	public static void writeShortInt(short src, byte [] bytes, int offset) {  
		bytes[offset]=(byte) (src & 0xff);  
		bytes[offset+1]=(byte) ((src>>8) & 0xff); 
	}
	
	public static void encode(byte[] data, int length, int key[])
	{
		int v[]=new int[2];
		for(int i=0;i<length/8*8;i+=8)
		{
			v[0]=readUnsignedInt(data,i);
			v[1]=readUnsignedInt(data,i+4);
			encipher(2,v,key);
			writeUnsignedInt(v[0],data,i);
			writeUnsignedInt(v[1],data,i+4);
		}
	}
	
	public static void decode(byte[] data, int offset, int length, int key[])
	{
		int v[]=new int[2];
		for(int i=offset;i<(length-offset)/8*8;i+=8)
		{
			v[0]=readUnsignedInt(data,i);
			v[1]=readUnsignedInt(data,i+4);
			decipher(2,v,key);
			writeUnsignedInt(v[0],data,i);
			writeUnsignedInt(v[1],data,i+4);
		}
	}
	
	public static int readUnsignedInt_13(byte [] bytes, int offset, int tail) {  
	    int b0 = ((int) (bytes[offset] & 0xff));  
	    int b1 = ((int) (bytes[tail] & 0xff)) << 8;  
	    int b2 = ((int) (bytes[tail+1] & 0xff)) << 16;  
	    int b3 = ((int) (bytes[tail+2] & 0xff)) << 24;
	    return (int) (b0 | b1 | b2 | b3);  
	}
}
