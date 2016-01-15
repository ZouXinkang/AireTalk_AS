package com.pingshow.video.capture;

 
public class AvcEncoder {
/*
	private static MediaCodec mediaCodec;
	
	public static int _width=1280;
	public static int _height=720;
	public static int _fps=10;
	public static int _bitrate=5600000;
 
	public void init() 
	{
		mediaCodec = MediaCodec.createEncoderByType("video/avc");
		MediaFormat mediaFormat = MediaFormat.createVideoFormat("video/avc", _width, _height);
		mediaFormat.setInteger(MediaFormat.KEY_BIT_RATE, _bitrate);
		mediaFormat.setInteger(MediaFormat.KEY_FRAME_RATE, _fps);
		mediaFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Planar);
		mediaFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1);
		mediaFormat.setInteger(MediaFormat.KEY_HEIGHT, _height);
		mediaFormat.setInteger(MediaFormat.KEY_WIDTH, _width);
		mediaCodec.configure(mediaFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
		mediaCodec.start();
	}		
 
	public void close() {
		mediaCodec.stop();
		mediaCodec.release();
	}
 
	public int offerVideo(byte[] input, byte[] encoded) {
		int ret=0;
		try {
			ByteBuffer[] inputBuffers = mediaCodec.getInputBuffers();
			int inputBufferIndex = mediaCodec.dequeueInputBuffer(-1);
			if (inputBufferIndex >= 0) {
				ByteBuffer inputBuffer = inputBuffers[inputBufferIndex];
				inputBuffer.clear();
				inputBuffer.put(input);
				mediaCodec.queueInputBuffer(inputBufferIndex, 0, input.length, 0, 0);
			}
			
			ByteBuffer[] outputBuffers = mediaCodec.getOutputBuffers();
			MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
			int outputBufferIndex = mediaCodec.dequeueOutputBuffer(bufferInfo, 0);
			if (outputBufferIndex >= 0) {
				ByteBuffer outputBuffer = outputBuffers[outputBufferIndex];
				encoded = new byte[bufferInfo.size];
				ret=bufferInfo.size;
				outputBuffer.get(encoded);
				outputBuffer.clear();
				
				mediaCodec.releaseOutputBuffer(outputBufferIndex, false);
			}
		} catch (Throwable t) {
			t.printStackTrace();
		}
		return ret;
	}
	*/
}