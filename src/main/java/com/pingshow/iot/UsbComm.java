package com.pingshow.iot;

import java.util.ArrayList;

import android.content.Context;
import android.hardware.usb.UsbConstants;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbEndpoint;
import android.hardware.usb.UsbInterface;
import android.hardware.usb.UsbManager;

import com.pingshow.airecenter.AireJupiter;
import com.pingshow.airecenter.Log;
import com.pingshow.airecenter.MyPreference;
import com.pingshow.airecenter.SecurityNewActivity;
import com.pingshow.util.MyUtil;
//tml*** usb dongle
public class UsbComm implements Runnable {
	private final boolean usbTestOnly = true;
	/* dongle (HC-12-USB)
	 * pid: 22336
	 * vid: 1154
	 * to add more -> //insert usbd
	 * dongleN_...
	 */
	public static final int dongle1_code = 100;
	public static final int dongle1_pid = 22336;
	public static final int dongle1_vid = 1154;
	/* test usbs (dell mouse, aire mouse, logitech mouse, myusb)
	 * pid: 9488, 16641, 50479, 21877
	 * vid: 2362,  1578,  1133,  1921
	 */
	public static final int dongleTest_code = 999;
	public static final int dongleTest_pid = 1;
	public static final int dongleTest_vid = 1;
	
	private Context mContext;
	private MyPreference mPref;
	private UsbDevice mUsbDevice = null;
	private int usbCode = -1;
	private int prodId = -1;
	private int vendId = -1;
	private String dname = "";
	private int dprtcl = 0;
	private int dcls = 0, dsubcls = 0;
	private int dIfcount = 0;
	private boolean usbCommRdyRunning = false;
	private ArrayList<UsbInterface> listUsbIf;
	private ArrayList<UsbEndpoint> listUsbEndp;
	private ArrayList<Integer> listDIfEndpcount;

	public UsbComm(Context context, UsbDevice usbDevice, int myUsbCode, int pid, int vid) {
		mContext = context;
		mPref = new MyPreference(context);
		mUsbDevice = usbDevice;
		usbCode = myUsbCode;
		prodId = pid;
		vendId = vid;
		if (mUsbDevice != null) {
			dname = mUsbDevice.getDeviceName();
			dprtcl = mUsbDevice.getDeviceProtocol();
			dcls = mUsbDevice.getDeviceClass();
			dsubcls = mUsbDevice.getDeviceSubclass();
			dIfcount = mUsbDevice.getInterfaceCount();
			listUsbIf = new ArrayList<UsbInterface>();
			Log.i("myusb  dongle comm is " + dname + " prtcl=" + dprtcl + " class=" + dcls + "/" + dsubcls + " if=" + dIfcount);
		} else {
			Log.e("myusb  dongle comm nulled " + pid);
			abortUsbComm();
		}
	}
	
	public boolean isUsbCommRdyRunning() {  //merge with mpref?
		return usbCommRdyRunning;
	}
	
	public void abortUsbComm() {
		mUsbDevice = null;
		usbCommRdyRunning = false;
//		mPref.write("threadUsbComm" + usbCode, false);
	}
	
	private String getEndpTypeS(int type) {
		String stype = "";
		if (type == UsbConstants.USB_ENDPOINT_XFER_CONTROL) {
			stype = "ctrl zero";
		} else if (type == UsbConstants.USB_ENDPOINT_XFER_ISOC) {
			stype = "isochronous";
		} else if (type == UsbConstants.USB_ENDPOINT_XFER_BULK) {
			stype = "bulk";
		} else if (type == UsbConstants.USB_ENDPOINT_XFER_INT) {
			stype = "interrupt";
		} else {
			stype = "invalid/unknown";
		}
		return stype;
	}
	
	private String getEndpDirS(int dir) {
		String stype = "";
		if (dir == UsbConstants.USB_DIR_IN) {
			stype = "USB_DIR_IN";
		} else if (dir == UsbConstants.USB_DIR_OUT) {
			stype = "USB_DIR_OUT";
		} else {
			stype = "invalid/unknown";
		}
		return stype;
	}
		
	@Override
	public void run() {
		Log.d("myusb  dongle +++COMM+++ begin " + "p" + prodId + "/v" + vendId);
//		mPref.write("threadUsbComm" + usbCode, true);
		boolean testUsbOUT = mPref.readBoolean("USB_OUT", false);  //test
		boolean testUsbIN2 = mPref.readBoolean("USB_IN_2", false);  //test
		
		UsbManager usbManager = (UsbManager) mContext.getSystemService(Context.USB_SERVICE);
		UsbDeviceConnection usbDeviceConn = null;
		
		if (mUsbDevice != null)
		{
			//permission
			boolean permission = usbManager.hasPermission(mUsbDevice);
			if (!permission) {
				Log.e("myusb  dongle comm !@#$ no permission!");
				abortUsbComm();
				return;
			}

			UsbInterface dIf = null;
			UsbEndpoint dEndp_found = null;
			UsbEndpoint usbEndpoint_IN = null;
			UsbEndpoint usbEndpoint_OUT = null;
			
			//interfaces: If1, If2, ..
			for (int i = 0; i < dIfcount; i++) {
				dIf = mUsbDevice.getInterface(i);
				listUsbIf.add(dIf);
			}
			
			//endpoints per interface: If1.Endp-N, If2.Endp-N, ..
			int listUsbIfz = listUsbIf.size();
			if (listUsbIfz > 0)
			{
				listDIfEndpcount = new ArrayList<Integer>();
				int totEndpcount = 0;
				for (int j = 0; j < listUsbIfz; j++) {
					dIf = listUsbIf.get(j);
					int epcount = dIf.getEndpointCount();
					listDIfEndpcount.add(epcount);
					totEndpcount = totEndpcount + epcount;
					Log.i("myusb  dongle If[" + j + "] has " + epcount + " Endp (" + totEndpcount + ")");
				}
				
				int listDIfEpcountz = listDIfEndpcount.size();
				if (totEndpcount < 1 || listDIfEpcountz < 1) {
					Log.e("myusb  dongle comm !@#$ no endpoints found!");
					abortUsbComm();
					return;
				}
				
				//list all endpoints
				listUsbEndp = new ArrayList<UsbEndpoint>();
				for (int j2 = 0; j2 < listUsbIfz; j2++) {
					dIf = listUsbIf.get(j2);
					
					int if_endp = listDIfEndpcount.get(j2);
					for (int k = 0; k < if_endp; k++) {
						dEndp_found = dIf.getEndpoint(k);
						listUsbEndp.add(dEndp_found);
					}
				}
				
				//acquire endpoints data directions
				int listUsbEndpcountz = listUsbEndp.size();
				int this_if_endp = 1;
				int this_posIf_IN = 0;
				int this_posIf_OUT = 0;
				int offset_endp = 0;
				for (int m = 0; m < listUsbEndpcountz; m++) {
					dEndp_found = listUsbEndp.get(m);
					
					//match interface to its endpoint
					int cnt_if_endp = listDIfEndpcount.get(this_if_endp - 1);
					if ((m + offset_endp) == cnt_if_endp) {
						this_if_endp++;
						offset_endp = offset_endp + cnt_if_endp;
					}
					
					//log endpoint info
					int posIf = this_if_endp - 1;
					int dEndp_type = dEndp_found.getType();
					int dEndp_dir = dEndp_found.getDirection();
					if (dEndp_dir == UsbConstants.USB_DIR_IN) {
						Log.i("myusb  dongle Ep[" + m + "/" + listUsbEndpcountz + "|" + posIf + "] is USB_DIR_IN  > stb (" + getEndpTypeS(dEndp_type) + ")");
					} else {
						Log.i("myusb  dongle Ep[" + m + "/" + listUsbEndpcountz + "|" + posIf + "] is USB_DIR_OUT > usb (" + getEndpTypeS(dEndp_type) + ")");
					}
					
					//specify endpoint per target usbdongle
					//device 1
					if (prodId == dongle1_pid && vendId == dongle1_vid) {
						//m & posIf values first retrieved from above log results [m/_|posIf]
						if (!testUsbIN2)
						if (dEndp_dir == UsbConstants.USB_DIR_IN) {
							if (m == 2 && posIf == 1) {
								this_posIf_IN = posIf;
								usbEndpoint_IN = dEndp_found;
							}
						}
						
						if (testUsbIN2)
						if (dEndp_dir == UsbConstants.USB_DIR_IN) {
							if (m == 0 && posIf == 0) {
								this_posIf_IN = posIf;
								usbEndpoint_IN = dEndp_found;
							}
						}
						
						if (dEndp_dir == UsbConstants.USB_DIR_OUT) {
							if (m == 1 && posIf == 1) {
								this_posIf_OUT = posIf;
								usbEndpoint_OUT = dEndp_found;
							}
						}
					}
					//device 2
					if (prodId == dongleTest_pid && vendId == dongleTest_vid) {
						//dell mouse=0,0  aire mouse=1,1
						if (dEndp_dir == UsbConstants.USB_DIR_IN) {
							if (m == 1 && posIf == 1) {
								this_posIf_IN = posIf;
								usbEndpoint_IN = dEndp_found;
							}
						}
					}
				}
				
				if (usbTestOnly) {
					int cnt = 0;
					while (mUsbDevice != null) {
						MyUtil.Sleep(5000);
						cnt++;
						Log.e("usb TEST " + cnt);
					}
				}
				
				//*** endpoint acquired *** ready connection ***
				UsbEndpoint useEndpoint = usbEndpoint_IN;
				if (testUsbOUT)  //test
					useEndpoint = usbEndpoint_OUT;
				if (useEndpoint != null)
				{
					Log.d("myusb  dongle +++COMM+++ ready!");
					usbCommRdyRunning = true;
					//log endpoint info
					int endpnum = useEndpoint.getEndpointNumber();
					int endpaddr = useEndpoint.getAddress();
					int endpattr = useEndpoint.getAttributes();
					int endpintrvl = useEndpoint.getInterval();
					int endpdir = useEndpoint.getDirection();
					int endptype = useEndpoint.getType();
					int pktz = useEndpoint.getMaxPacketSize();
					Log.i("myusb  dongle comm Endpoint  #=" + endpnum + " addr=" + endpaddr + " attr=" + endpattr + " intrvl=" + endpintrvl);
					Log.i("myusb  dongle comm Endpoint  dir=" + getEndpDirS(endpdir) + " type=" + getEndpTypeS(endptype) + " pkt=" + pktz);
					
					//setup connection
					usbDeviceConn = usbManager.openDevice(mUsbDevice);
					if (usbDeviceConn != null)
					{
						//comm OUT > usb
						if (testUsbOUT) {  //testing if dongle can take input, led indicator
							dIf = listUsbIf.get(this_posIf_OUT);
							boolean claimed = usbDeviceConn.claimInterface(dIf, true);
							
							if (claimed) {
								Log.d("myusb  dongle +++COMM+++ OUT active");
								String stringOUT = "TESTT";
								byte[] dataOUT = stringOUT.getBytes();
								String hexOUT = MyUtil.bytesToHex(dataOUT);
								
								int count = 0;
								while (mUsbDevice != null && usbDeviceConn != null && useEndpoint != null) {
									count++;
									int result = usbDeviceConn.bulkTransfer(useEndpoint, dataOUT, dataOUT.length, 5000);
									Log.d("myusb  dongle comm OUT-RESULT=" + result + "  (" + count + ")  " + stringOUT + "  h." + hexOUT);
									MyUtil.Sleep(2000);
								}
								
							} else {
								usbDeviceConn.close();
								usbDeviceConn = null;
								Log.e("myusb  dongle comm !@#$ connection claim failed!");
								abortUsbComm();
								return;
							}
						}
						
						//comm IN > stb
						if (!testUsbOUT) {
							dIf = listUsbIf.get(this_posIf_IN);
							boolean claimed = usbDeviceConn.claimInterface(dIf, true);
							
							if (claimed) {
								Log.d("myusb  dongle +++COMM+++ IN active");
								byte[] dataIN = new byte[pktz];
								
								int count = 0;
								while (mUsbDevice != null && usbDeviceConn != null && useEndpoint != null) {
									int result = usbDeviceConn.bulkTransfer(useEndpoint, dataIN, dataIN.length, 1000);
									if (result >= 0) {
										count++;
										String dataResult = MyUtil.bytesToHex(dataIN);  //MyUtil.bytesToHex(dataIN) or new String(dataIN)
										Log.d("myusb  dongle comm IN-RESULT=" + result + "  (" + count + ")   h." + dataResult);
									}
								}
								
							} else {
								usbDeviceConn.close();
								usbDeviceConn = null;
								Log.e("myusb  dongle comm !@#$ connection claim failed!");
								abortUsbComm();
								return;
							}
						}
					}
					else
					{
						Log.e("myusb  dongle comm !@#$ connection failed!");
						abortUsbComm();
						return;
					}
				}
				else
				{
					Log.e("myusb  dongle comm !@#$ usbdongle unspecified / no endpoint matched!");
					abortUsbComm();
					return;
				}
			}
			else
			{
				Log.e("myusb  dongle comm !@#$ no interface found!");
				abortUsbComm();
				return;
			}
			
		}
		else
		{
			Log.e("myusb  dongle ---COMM--- end-null");
			abortUsbComm();
			return;
		}

		Log.d("myusb  dongle ---COMM--- end");
		if (usbDeviceConn != null) {
			usbDeviceConn.close();
			usbDeviceConn = null;
		}
		abortUsbComm();
	}
	
}
