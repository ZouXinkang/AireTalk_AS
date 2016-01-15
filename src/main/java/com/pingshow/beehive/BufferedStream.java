package com.pingshow.beehive;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

public class BufferedStream extends FileInputStream {

	public BufferedStream(String path) throws FileNotFoundException {
		super(path);
		// TODO Auto-generated constructor stub
	}

	@Override
	public int read() throws IOException {
		// TODO Auto-generated method stub
		return super.read();
	}

	@Override
	public int read(byte[] buffer, int offset, int byteCount)
			throws IOException {
		// TODO Auto-generated method stub
		return super.read(buffer, offset, byteCount);
	}

	@Override
	public int read(byte[] buffer) throws IOException {
		// TODO Auto-generated method stub
		return super.read(buffer);
	}
	
	
}
