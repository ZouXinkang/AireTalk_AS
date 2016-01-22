
package com.pingshow.voip.core;


public final class VideoSize {
	public static final int QCIF = 0;
	public static final int CIF = 1;
	public static final int HVGA = 2;
	public static final int QVGA = 3;
	public static final int VGA = 4;
	public static final int SVGA = 5;
	public static final int HD720pS = 6;
	public static final int HD720pM = 7;
	public static final int HD720p = 8;
	public static final int HD1080p = 9;

	public int width;
	public int height;

	public VideoSize() {}
	public VideoSize(int width, int height) {
		this.width = width;
		this.height = height;
	}

	public static final VideoSize createStandard(int code, boolean inverted) {
		switch (code) {
		case QCIF:
			return inverted? new VideoSize(144, 176) : new VideoSize(176, 144);
		case CIF:
			return inverted? new VideoSize(288, 352) : new VideoSize(352, 288);
		case HVGA:
			return inverted? new VideoSize(320, 480) : new VideoSize(480, 320);
		case QVGA:
			return inverted? new VideoSize(240, 320) : new VideoSize(320, 240);
		case VGA:
			return inverted? new VideoSize(480, 640) : new VideoSize(640, 480);
		case SVGA:
			return inverted? new VideoSize(600, 800) : new VideoSize(800, 600);
		case HD720pM:
			return inverted? new VideoSize(720, 960) : new VideoSize(960, 720);
		case HD720p:
			return inverted? new VideoSize(720, 1280) : new VideoSize(1280, 720);
		case HD1080p:
			return inverted? new VideoSize(1080, 1920) : new VideoSize(1920, 1080);
		default:
			return new VideoSize(352, 288); // Invalid one
		}
	}
	
	public boolean isValid() {
		return width > 0 && height > 0;
	}
	
	// Generated
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + height;
		result = prime * result + width;
		return result;
	}
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		VideoSize other = (VideoSize) obj;
		if (height != other.height)
			return false;
		if (width != other.width)
			return false;
		return true;
	}
	
	@Override
	public String toString() {
		return "width = "+width + " height = " + height;
	}
	public boolean isPortrait() {
		return height >= width;
	}
	public VideoSize createInverted() {
		return new VideoSize(height, width);
	}
	
}
