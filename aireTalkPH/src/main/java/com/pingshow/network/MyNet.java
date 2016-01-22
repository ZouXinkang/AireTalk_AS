package com.pingshow.network;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.net.URLConnection;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.conn.params.ConnRouteParams;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.util.EntityUtils;

import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

import com.pingshow.amper.AireJupiter;
import com.pingshow.amper.ConversationActivity;
import com.pingshow.amper.Global;
import com.pingshow.amper.Log;
import com.pingshow.amper.contacts.RelatedUserInfo;
import com.pingshow.amper.contacts.XMLContentHandler;
import com.pingshow.amper.view.ProgressBar;
import com.pingshow.network.NetworkControl.NetType;
import com.pingshow.util.MyUtil;

public class MyNet {
	
	final static int HTTP_READ_TIME_OUT = 90000;
	final static int HTTP_CONNECTION_TIME_OUT = 10000;  //15000
	private String HostURL = "http://" + AireJupiter.myPhpServer_default + "/onair/";
	private String HostURLattach = "http://" + AireJupiter.myPhpServer_default + "/onair/";
	public boolean NetExists=false;
	private Context mContext = null;
	private URL proxyurl;
	
	final static HostnameVerifier DO_NOT_VERIFY = new HostnameVerifier() {
        public boolean verify(String hostname, SSLSession session) {
                return true;
        }
	};
	
	public MyNet(Context context)
	{
		this.mContext = context;
		if (AireJupiter.getInstance() != null) {
//			HostURL = "http://" + AireJupiter.myPhpServer + "/onair/";
			//tml*** china ip
			String domainPhp = AireJupiter.getInstance().getIsoPhp(1, true, null);
			HostURL = "http://" + domainPhp + "/onair/";
			String attachPhp = AireJupiter.getInstance().getIsoPhp(1, false, null);
			HostURLattach = "http://" + attachPhp + "/onair/";
		}
		NetInfo ni = new NetInfo(context);
		NetExists = ni.isConnected();
	}
	
	public MyNet(Context context, String phpServer)
	{	
		HostURL = "http://" + phpServer + "/onair/";
		NetInfo ni = new NetInfo(context);
		NetExists = ni.isConnected();
	}
	
	public boolean getNetStatus()
	{
		return NetExists;
	}
	public String doPost(String surl, String data,String phpIP) 
	{
		NetType netType = NetworkControl.getNetType(mContext);
		if (!getNetStatus()) return "";
		String Return="";
    	String sURL = "";
	    try {
	    	HttpURLConnection URLConn=null;
	    	if(phpIP==null)
	    		sURL = HostURL+surl;
	    	else
	    		sURL="http://"+phpIP+"/onair/"+surl;
	    	Log.i("net." + sURL + " << " + data);
	    	String proxyHost = android.net.Proxy.getDefaultHost();
	    	if(netType==null||!netType.isWap())
	    		URLConn = (HttpURLConnection) new URL(sURL).openConnection();
	    	else if (proxyHost != null&&netType.isWap()){
	    		java.net.Proxy p = new java.net.Proxy(java.net.Proxy.Type.HTTP, new InetSocketAddress(
    					android.net.Proxy.getDefaultHost(), android.net.Proxy.getDefaultPort()));
    			URLConn = (HttpURLConnection) new URL(sURL).openConnection(p);
	    	}
	
	    	URLConn.setReadTimeout(HTTP_READ_TIME_OUT);
	    	URLConn.setDoOutput(true); 
			URLConn.setDoInput(true); 
			URLConn.setRequestMethod("POST"); 
			URLConn.setUseCaches(false); 
			URLConn.setAllowUserInteraction(true); 
			//HttpURLConnection.setFollowRedirects(true); 
			URLConn.setInstanceFollowRedirects(true); 
	
			URLConn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded"); 
			URLConn.setRequestProperty("Content-Length", String.valueOf(data.getBytes().length));
			
			URLConn.setConnectTimeout(HTTP_CONNECTION_TIME_OUT);
			//URLConn.connect();
			
			OutputStream os=URLConn.getOutputStream();
			java.io.DataOutputStream dos = new java.io.DataOutputStream(os); 

			dos.writeBytes(data);
			dos.close();
	
			int code = URLConn.getResponseCode();
            if (code == 200)
            {
            	String line = "";
                java.io.InputStream is = URLConn.getInputStream();
                
                BufferedReader reader = new BufferedReader(new InputStreamReader(is));
                while ((line = reader.readLine()) != null)
                    if (line.length() > 0)
                    	Return += trimBOM(line.trim());
                reader.close();
            } else {
            	Return = "Error code:" + code;
            }
            
           	URLConn.disconnect();
            
	    } catch (java.io.IOException e) {
	    	Log.e("doPost !@#$ " + e.getMessage());
	    }
		Log.i("net.post! " + sURL + " :: " + Return);
		return Return;
	}
	
	public String doPostHttps(String surl, String data, String phpIP) {
		NetType netType = NetworkControl.getNetType(mContext);
		if (!getNetStatus())
			return "";
		String Return = "";
		String sURL = "";
		try {
			HttpURLConnection URLConn = null;
			if (phpIP == null)
				if (AireJupiter.getInstance() != null) {  //tml*** china ip
					phpIP = AireJupiter.getInstance().getIsoPhp(1, true, null);
					sURL = "https://" + phpIP + "/onair/" + surl;
				} else {
					sURL = "https://" + AireJupiter.myPhpServer + "/onair/"
							+ surl;
				}
			else
				sURL = "https://" + phpIP + "/onair/" + surl;
	    	Log.i("net." + sURL + " << " + data);

			trustAllHosts();
			URL Url = new URL(sURL);
			HttpsURLConnection https = null;
			String proxyHost = android.net.Proxy.getDefaultHost();
			if (netType == null || !netType.isWap())
				https = (HttpsURLConnection) Url.openConnection();
			else if (proxyHost != null && netType.isWap()) {
				java.net.Proxy p = new java.net.Proxy(java.net.Proxy.Type.HTTP,
						new InetSocketAddress(
								android.net.Proxy.getDefaultHost(),
								android.net.Proxy.getDefaultPort()));
				https = (HttpsURLConnection) Url.openConnection(p);
			}
			https.setHostnameVerifier(DO_NOT_VERIFY);
			URLConn = https;

			URLConn.setReadTimeout(HTTP_READ_TIME_OUT);
			URLConn.setDoOutput(true);
			URLConn.setDoInput(true);
			URLConn.setRequestMethod("POST");
			URLConn.setUseCaches(false);
			URLConn.setAllowUserInteraction(true);
			// HttpURLConnection.setFollowRedirects(true);
			URLConn.setInstanceFollowRedirects(true);

			URLConn.setRequestProperty("Content-Type",
					"application/x-www-form-urlencoded");
			URLConn.setRequestProperty("Content-Length",
					String.valueOf(data.getBytes().length));

			URLConn.setConnectTimeout(HTTP_CONNECTION_TIME_OUT);
			URLConn.connect();

			OutputStream os = URLConn.getOutputStream();
			java.io.DataOutputStream dos = new java.io.DataOutputStream(os);

			dos.writeBytes(data);
			dos.close();

			int code = URLConn.getResponseCode();
			if (code == 200) {
				String line = "";
				java.io.InputStream is = URLConn.getInputStream();

				BufferedReader reader = new BufferedReader(
						new InputStreamReader(is));
				while ((line = reader.readLine()) != null)
					if (line.length() > 0)
						Return += trimBOM(line.trim());
				reader.close();
			} else {
				Return = "Error code:" + code;
			}

			URLConn.disconnect();

		} catch (java.io.IOException e) {
			Log.e("doPostHttps !@#$ " + e.getMessage());
		}
		Log.i("net.postHttps! " + sURL + " :: " + Return);
		return Return;
	}
	
	public List<RelatedUserInfo> doPostHttpsWithXML(String surl, String data, String phpIP) {
		NetType netType = NetworkControl.getNetType(mContext);
		if (!getNetStatus())
			return null;
		List<RelatedUserInfo> fi=null;
		String sURL = "";
		try {
			HttpURLConnection URLConn = null;
			if (phpIP == null)
				if (AireJupiter.getInstance() != null) {  //tml*** china ip
					phpIP = AireJupiter.getInstance().getIsoPhp(1, true, null);
					sURL = "https://" + phpIP + "/onair/" + surl;
				} else {
					sURL = "https://" + AireJupiter.myPhpServer + "/onair/"
							+ surl;
				}
			else
				sURL = "https://" + phpIP + "/onair/" + surl;
	    	Log.i("net." + sURL + " << " + data);

			trustAllHosts();
			URL Url = new URL(sURL);
			HttpsURLConnection https = null;
			String proxyHost = android.net.Proxy.getDefaultHost();
			if (netType == null || !netType.isWap())
				https = (HttpsURLConnection) Url.openConnection();
			else if (proxyHost != null && netType.isWap()) {
				java.net.Proxy p = new java.net.Proxy(java.net.Proxy.Type.HTTP,
						new InetSocketAddress(
								android.net.Proxy.getDefaultHost(),
								android.net.Proxy.getDefaultPort()));
				https = (HttpsURLConnection) Url.openConnection(p);
			}
			https.setHostnameVerifier(DO_NOT_VERIFY);
			URLConn = https;

			URLConn.setReadTimeout(HTTP_READ_TIME_OUT);
			URLConn.setDoOutput(true);
			URLConn.setDoInput(true);
			URLConn.setRequestMethod("POST");
			URLConn.setUseCaches(false);
			URLConn.setAllowUserInteraction(true);
			// HttpURLConnection.setFollowRedirects(true);
			URLConn.setInstanceFollowRedirects(true);

			URLConn.setRequestProperty("Content-Type",
					"application/x-www-form-urlencoded");
			URLConn.setRequestProperty("Content-Length",
					String.valueOf(data.getBytes().length));

			URLConn.setConnectTimeout(HTTP_CONNECTION_TIME_OUT);
			// URLConn.connect();

			OutputStream os = URLConn.getOutputStream();
			java.io.DataOutputStream dos = new java.io.DataOutputStream(os);

			dos.writeBytes(data);
			dos.close();

			int code = URLConn.getResponseCode();
			if (code == 200) {
				java.io.InputStream is = URLConn.getInputStream();
				fi=readXML(is);
				is.close();
			}

			URLConn.disconnect();

		} catch (java.io.IOException e) {
			Log.e("doPostHttpsXML !@#$ " + e.getMessage());
		}
		Log.i("net.postHttpsXML! " + sURL);
		return fi;
	}
	
	
	public List<RelatedUserInfo> doPostHttpWithXML(String surl, String data, String phpIP) {
		NetType netType = NetworkControl.getNetType(mContext);
		if (!getNetStatus())
			return null;
		List<RelatedUserInfo> fi=null;
		String sURL = "";
		try {
			HttpURLConnection URLConn = null;
			if (phpIP == null)
				sURL = "http://" + AireJupiter.myPhpServer + "/onair/"
						+ surl;
			else
				sURL = "http://" + phpIP + "/onair/" + surl;
	    	Log.i("net." + sURL + " << " + data);

			//trustAllHosts();
			URL Url = new URL(sURL);
			HttpURLConnection http = null;
			String proxyHost = android.net.Proxy.getDefaultHost();
			if (netType == null || !netType.isWap())
				http = (HttpURLConnection) Url.openConnection();
			else if (proxyHost != null && netType.isWap()) {
				java.net.Proxy p = new java.net.Proxy(java.net.Proxy.Type.HTTP,
						new InetSocketAddress(
								android.net.Proxy.getDefaultHost(),
								android.net.Proxy.getDefaultPort()));
				http = (HttpURLConnection) Url.openConnection(p);
			}
			URLConn = http;

			URLConn.setReadTimeout(HTTP_READ_TIME_OUT);
			URLConn.setDoOutput(true);
			URLConn.setDoInput(true);
			URLConn.setRequestMethod("POST");
			URLConn.setUseCaches(false);
			URLConn.setAllowUserInteraction(true);
			// HttpURLConnection.setFollowRedirects(true);
			URLConn.setInstanceFollowRedirects(true);

			URLConn.setRequestProperty("Content-Type",
					"application/x-www-form-urlencoded");
			URLConn.setRequestProperty("Content-Length",
					String.valueOf(data.getBytes().length));

			URLConn.setConnectTimeout(HTTP_CONNECTION_TIME_OUT);
			// URLConn.connect();

			OutputStream os = URLConn.getOutputStream();
			java.io.DataOutputStream dos = new java.io.DataOutputStream(os);

			dos.writeBytes(data);
			dos.close();

			int code = URLConn.getResponseCode();
			if (code == 200) {
				java.io.InputStream is = URLConn.getInputStream();
				fi=readXML(is);
				is.close();
			}

			URLConn.disconnect();

		} catch (java.io.IOException e) {
			Log.e("doPostHttpXML !@#$ " + e.getMessage());
		}
		Log.i("net.postHttpXML! " + sURL);
		return fi;
	}
	
	public static List<RelatedUserInfo> readXML(InputStream inStream) {
		try {
			SAXParserFactory spf = SAXParserFactory.newInstance();
			SAXParser saxParser = spf.newSAXParser();
			//saxParser.setProperty("http://xml.org/sax/features/namespaces",true);
			XMLContentHandler handler = new XMLContentHandler();
			saxParser.parse(inStream, handler);
			inStream.close();
			return handler.getPersons();
		} catch (Exception e) {
			Log.e("net.List ERR " + e.getMessage());
		}
		return null;
	}
	
	private static void trustAllHosts() {
		// Create a trust manager that does not validate certificate chains
		TrustManager[] trustAllCerts = new TrustManager[] { new X509TrustManager() {
			public java.security.cert.X509Certificate[] getAcceptedIssuers() {
				return new java.security.cert.X509Certificate[] {};
			}

			public void checkClientTrusted(X509Certificate[] chain,
					String authType) throws CertificateException {
			}

			public void checkServerTrusted(X509Certificate[] chain,
					String authType) throws CertificateException {
			}
		} };

		// Install the all-trusting trust manager
		try {
			SSLContext sc = SSLContext.getInstance("TLS");
			sc.init(null, trustAllCerts, new java.security.SecureRandom());
			HttpsURLConnection
					.setDefaultSSLSocketFactory(sc.getSocketFactory());
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public String doPostAttach(String surl,int myidx,int toidx,String filename,String phpIP){
		String Return="";
    	String sURL = "";
    	if(phpIP==null)
    		sURL = HostURLattach+surl;
    	else
    		sURL="http://"+phpIP+"/onair/"+surl;
    	Log.i("net." + sURL + " << " + toidx + " " + filename);
		
		URLConnection urlConnection = null;
		java.net.Proxy proxy;
		String proxyHost = android.net.Proxy.getDefaultHost();
		HttpClient client = new DefaultHttpClient();
		HttpPost httpPost = null;
		
		try {
			proxyurl = new URL(sURL);
		} catch (MalformedURLException e1) {
			e1.printStackTrace();
		}
		NetType netType = NetworkControl.getNetType(mContext);
		try {
			if(netType==null||!netType.isWap())
				httpPost = new HttpPost(sURL);
			else if (proxyHost != null&&netType.isWap()) {
				netType.setProxy(proxyHost);
				netType.setPort(android.net.Proxy.getDefaultPort());
				netType.setWap(true);
				proxy = new java.net.Proxy(java.net.Proxy.Type.HTTP, new InetSocketAddress(
    					android.net.Proxy.getDefaultHost(), android.net.Proxy.getDefaultPort()));
				urlConnection = proxyurl.openConnection(proxy);
				urlConnection.connect();
				HttpHost proxy1 = new HttpHost(netType.getProxy(), netType.getPort());
				client.getParams().setParameter(ConnRouteParams.DEFAULT_PROXY, proxy1);
				httpPost = new HttpPost(proxyurl.toURI());
			}
			File file = new File(filename);
			FileBody fileBody = new FileBody(file);
			StringBody id = new StringBody(myidx+"");
			StringBody to = new StringBody(toidx+"");
			
			MultipartEntity reqEntity = new MultipartEntity();
			reqEntity.addPart("id", id);
			reqEntity.addPart("to", to);
			reqEntity.addPart("ufile", fileBody);
			httpPost.setEntity(reqEntity);
			
			//alec:
			HttpParams httpParameters = new BasicHttpParams();
			int timeoutConnection = 60000;
			HttpConnectionParams.setConnectionTimeout(httpParameters, timeoutConnection);
			int timeoutSocket = 300000;
			HttpConnectionParams.setSoTimeout(httpParameters, timeoutSocket);
			httpPost.setParams(httpParameters);
			
			HttpResponse response = new DefaultHttpClient().execute(httpPost);
			if (HttpStatus.SC_OK == response.getStatusLine().getStatusCode()) {
				HttpEntity entity = response.getEntity();
				if (entity != null) {
					Return = trimBOM(EntityUtils.toString(entity));
				}
				if (entity != null) {
					entity.consumeContent();
				}
			}
		}catch (Exception e) {
			Log.e("doPostAttach !@#$"+e.getMessage());
		}
		Log.i("net.postAttach! " + sURL + " :: " + Return);
		return Return;
	}
	
	
	public String doPostAttachFixed(String surl,String filename,String dstName,String phpIP){
		String Return="";
    	String sURL = "";
    	if(phpIP==null)
    		sURL = HostURLattach+surl;
    	else
    		sURL="http://"+phpIP+"/onair/"+surl;
    	Log.i("net." + sURL + " << " + dstName + " " + filename);
		
		URLConnection urlConnection = null;
		java.net.Proxy proxy;
		String proxyHost = android.net.Proxy.getDefaultHost();
		HttpClient client = new DefaultHttpClient();
		HttpPost httpPost = null;
		
		try {
			proxyurl = new URL(sURL);
		} catch (MalformedURLException e1) {
			e1.printStackTrace();
		}
		NetType netType = NetworkControl.getNetType(mContext);
		try {
			if(netType==null||!netType.isWap())
				httpPost = new HttpPost(sURL);
			else if (proxyHost != null&&netType.isWap()) {
				netType.setProxy(proxyHost);
				netType.setPort(android.net.Proxy.getDefaultPort());
				netType.setWap(true);
				proxy = new java.net.Proxy(java.net.Proxy.Type.HTTP, new InetSocketAddress(
    					android.net.Proxy.getDefaultHost(), android.net.Proxy.getDefaultPort()));
				urlConnection = proxyurl.openConnection(proxy);
				urlConnection.connect();
				HttpHost proxy1 = new HttpHost(netType.getProxy(), netType.getPort());
				client.getParams().setParameter(ConnRouteParams.DEFAULT_PROXY, proxy1);
				httpPost = new HttpPost(proxyurl.toURI());
			}
			File file = new File(filename);
			FileBody fileBody = new FileBody(file);
			StringBody dstname = new StringBody(dstName);
			
			MultipartEntity reqEntity = new MultipartEntity();
			reqEntity.addPart("dstname", dstname);
			reqEntity.addPart("ufile", fileBody);
			httpPost.setEntity(reqEntity);
			
			//alec:
			HttpParams httpParameters = new BasicHttpParams();
			int timeoutConnection = 60000;
			HttpConnectionParams.setConnectionTimeout(httpParameters, timeoutConnection);
			int timeoutSocket = 300000;
			HttpConnectionParams.setSoTimeout(httpParameters, timeoutSocket);
			httpPost.setParams(httpParameters);
			
			HttpResponse response = new DefaultHttpClient().execute(httpPost);
			if (HttpStatus.SC_OK == response.getStatusLine().getStatusCode()) {
				HttpEntity entity = response.getEntity();
				if (entity != null) {
					Return = trimBOM(EntityUtils.toString(entity));
				}
				if (entity != null) {
					entity.consumeContent();
				}
			}
		}catch (Exception e) {
			Log.e("doPostAttach !@#$"+e.getMessage());
		}
		Log.i("net.postAttachFix! " + sURL + " :: " + Return);
		return Return;
	}
	
	static class MyMultipartEntity extends MultipartEntity {
		private long total=1;
		private String filename;
		public MyMultipartEntity(long size, String fn)
		{
			total=size;
			filename=fn;
		}
		@Override
	    public void writeTo(final OutputStream outstream) throws IOException {
	        super.writeTo(new CountingOutputStream(outstream, total, filename));
	    }
	}
	
	static class CountingOutputStream extends FilterOutputStream {

		long amount=0;
		long total=1;
		private String filename;
		
	    CountingOutputStream(final OutputStream out, long size, String fn) {
	        super(out);
	        total=size;
	        filename=fn;
	    }

	    @Override
	    public void write(int b) throws IOException {
	        out.write(b);
	        amount++;
	        updateProgress();
	    }

	    @Override
	    public void write(byte[] b) throws IOException {
	        out.write(b);
	        amount+=b.length;
	        updateProgress();
	    }

	    @Override
	    public void write(byte[] b, int off, int len) throws IOException {
	        out.write(b, off, len);
	        amount+=len;
	        updateProgress();
	    }
	    
	    public void setFileTotalSize(long size)
	    {
	    	total=size;
	    }
	    
	    private void updateProgress()
	    {
	    	ConversationActivity msgPage=ConversationActivity.getInstance();
			if (msgPage!=null)
			{
				ProgressBar p=msgPage.getProgressBar(filename);
				if (p!=null) p.setProgress((float)amount/total);
			}
	    }
	}
	
	public void stopUploading(String filename)
	{
		Log.i("net.stopUploading < " + filename);
		try{
			HttpUriRequest post=(HttpUriRequest)mHttpPosts.get(filename);
			if (post!=null) post.abort();
		}catch(Exception e){}
	}
	
	static private Map<String, HttpPost> mHttpPosts = new HashMap<String, HttpPost>();
	
	public String doPostAttach8(String surl,int myidx,String fn, String filename,String phpIP){
		String Return="";
    	String sURL = "";
    	if(phpIP==null)
    		sURL = HostURLattach+surl;
    	else
    		sURL="http://"+phpIP+"/onair/"+surl;
    	Log.i("net." + sURL + " << " + myidx + " " + fn + " " + filename);
    	
		try {
			HttpPost httpPost = new HttpPost(sURL);
			mHttpPosts.put(filename, httpPost);
			
			File file = new File(filename);
			Log.d("uploading php=" + sURL + "  file?" + file.exists());
			FileBody fileBody = new FileBody(file);
			StringBody id = new StringBody(myidx+""); 
			StringBody sFn = new StringBody(fn); 
			MyMultipartEntity reqEntity = new MyMultipartEntity(file.length(), filename);
			reqEntity.addPart("id", id);
			reqEntity.addPart("fn", sFn);
			reqEntity.addPart("ufile", fileBody);
			
			httpPost.setEntity(reqEntity);
			
			//alec:
			HttpParams httpParameters = new BasicHttpParams();
			int timeoutConnection = 60000;
			HttpConnectionParams.setConnectionTimeout(httpParameters, timeoutConnection);
			int timeoutSocket = 300000;
			HttpConnectionParams.setSoTimeout(httpParameters, timeoutSocket);
			httpPost.setParams(httpParameters);
			
			DefaultHttpClient httpclient = new DefaultHttpClient();
			
			HttpResponse response = httpclient.execute(httpPost);
			if (HttpStatus.SC_OK == response.getStatusLine().getStatusCode()) {
				HttpEntity entity = response.getEntity();
				if (entity != null) {
					Return = trimBOM(EntityUtils.toString(entity));
				}
				if (entity != null) {
					entity.consumeContent();
				}
			}
			
		}catch (Exception e) {
			Log.e("doPostAttach8 !@#$ "+e.getMessage());
		}
		Log.i("net.postAttach8! " + sURL + " :: " + Return);
		return Return;
	}
	public boolean Download(String surl, String filename, int attached, String phpIP) 
	{
		if (!getNetStatus()) return false;
		boolean ret=false;
    	Intent intent = new Intent();
    	intent.setAction(Global.Action_FileDownload);
    	String sURL = "";
	    try {   
	    	if(phpIP==null)
	    		sURL = HostURLattach+surl;
	    	else
	    		sURL="http://"+phpIP+"/onair/"+surl;
	    	Log.i("net." + sURL + " << " + filename);
			
	    	HttpURLConnection URLConn = null;
	    	String proxyHost = android.net.Proxy.getDefaultHost();
    		if (proxyHost != null) {
    			java.net.Proxy p = new java.net.Proxy(java.net.Proxy.Type.HTTP, new InetSocketAddress(
    					android.net.Proxy.getDefaultHost(), android.net.Proxy.getDefaultPort()));
    			URLConn = (HttpURLConnection) new URL(sURL).openConnection(p);
    		} else {
    			URLConn = (HttpURLConnection) new URL(sURL).openConnection();
    		}
    		//
    		 
//	    	String sURL=HostURL+surl;
//	    	Log.d(sURL);
//	    	URL url = new URL(sURL);
//	    	HttpURLConnection URLConn = (HttpURLConnection) url.openConnection();
	    	URLConn.setReadTimeout(HTTP_READ_TIME_OUT);
	    	URLConn.setConnectTimeout(HTTP_CONNECTION_TIME_OUT);
	    	URLConn.setRequestMethod("GET");
	    	URLConn.setDoInput(true);
	    	URLConn.connect();
	    	
	    	InputStream stream = URLConn.getInputStream();
	    	File file1 = new File(filename.substring(0, filename.lastIndexOf("/")));
	    	if(!file1.exists()){
	    		file1.mkdirs();
	    	}
	    	String temp=filename+".tmp";
	    	FileOutputStream file= new FileOutputStream(temp);

	    	byte [] data=new byte[1024];
	    	int len;
	    	while((len=stream.read(data)) > 0)
	    	{
	    		file.write(data, 0, len);
	    	}
	    	file.flush();
	    	file.close();   
	    	stream.close();
	    	ret=true;
	    	
	    	MyUtil.renameFile(temp, filename);
	    	URLConn.disconnect();

	    	intent.putExtra("err", false);
	    } catch (Exception e) {
	    	Log.e("Download Failed1 !@#$ "+e.getMessage());
	    	intent.putExtra("err", true);
	    }
	    intent.putExtra("attached", attached);
	    intent.putExtra("filename", filename);
	    mContext.sendBroadcast(intent);
		Log.i("net.Download1! " + sURL);
	    return ret;
	}
	
	public int Download(String surl, String filename, String serverIP) 
	{
		if (!getNetStatus()) return 0;
		int ret=0;
    	String sURL = "";
	    try {   
	    	if (serverIP!=null)
	    		sURL = "http://"+serverIP+"/onair/"+surl;
	    	else
	    		sURL = HostURLattach+surl;
	    	Log.i("net." + sURL + " << " + filename);
			
	    	HttpURLConnection URLConn = null;
	    	String proxyHost = android.net.Proxy.getDefaultHost();
    		if (proxyHost != null) {
    			java.net.Proxy p = new java.net.Proxy(java.net.Proxy.Type.HTTP, new InetSocketAddress(
    					android.net.Proxy.getDefaultHost(), android.net.Proxy.getDefaultPort()));
    			URLConn = (HttpURLConnection) new URL(sURL).openConnection(p);
    		} else {
    			URLConn = (HttpURLConnection) new URL(sURL).openConnection();
    		}
    		 
	    	URLConn.setReadTimeout(HTTP_READ_TIME_OUT);
	    	URLConn.setConnectTimeout(HTTP_CONNECTION_TIME_OUT);
	    	URLConn.setRequestMethod("GET");
	    	URLConn.setDoInput(true);
	    	URLConn.connect();
	    	
	    	InputStream stream=null;
	    	try{
	    		stream = URLConn.getInputStream();
	    	}catch(Exception e1)
	    	{
	    		URLConn.disconnect();
	    		return 0;
	    	}
	    	
	    	File file1 = new File(filename.substring(0, filename.lastIndexOf("/")));
	    	if(!file1.exists()){
	    		file1.mkdirs();
	    	}
	    	
	    	String temp=filename+".tmp";
	    	FileOutputStream file= new FileOutputStream(temp);

	    	byte [] data=new byte[1024];
	    	int len;
	    	while((len=stream.read(data)) > 0)
	    	{
	    		file.write(data, 0, len);
	    	}
	    	file.flush();
	    	file.close();
	    	stream.close();
	    	
	    	MyUtil.renameFile(temp, filename);
	    	ret=1;
	    	URLConn.disconnect();
	    } catch (Exception e) {
	    	Log.e("Download Failed2: "+e.getMessage());
	    	ret=-1;
	    }
		Log.i("net.Download2! " + sURL);
	    return ret;
	}
	
	public boolean anyDownload(String full_url, String filename) 
	{
		if(!MyUtil.checkSDCard(mContext))
		{
			return false;
		}
		if (!getNetStatus()) return false;
    	Log.i("net." + full_url + " << " + filename);
		boolean ret=false;
	    try {   
	    	//
	    	HttpURLConnection URLConn = null;
	    	String proxyHost = android.net.Proxy.getDefaultHost();
    		if (proxyHost != null) {
    			java.net.Proxy p = new java.net.Proxy(java.net.Proxy.Type.HTTP, new InetSocketAddress(
    					android.net.Proxy.getDefaultHost(), android.net.Proxy.getDefaultPort()));
    			URLConn = (HttpURLConnection) new URL(full_url).openConnection(p);
    		} else {
    			URLConn = (HttpURLConnection) new URL(full_url).openConnection();
    		}
    		//
	    	
//	    	URL url = new URL(full_url);
//	    	HttpURLConnection URLConn = (HttpURLConnection) url.openConnection();
    		
	    	InputStream stream = URLConn.getInputStream();
	    	FileOutputStream file = new FileOutputStream(filename);
	    	URLConn.setReadTimeout(HTTP_READ_TIME_OUT);
	    	byte [] data=new byte[1024];
	    	int len;
	    	while((len=stream.read(data)) > 0)
	    		file.write(data, 0, len);
	    	file.flush();
	    	file.close();
	    	stream.close();
	    	ret=true;
	    	
	    	URLConn.disconnect();
	    	
	    } catch (Exception e) {
	    	Log.e("anyDownload Filed: "+e.getMessage());
	    }
		Log.i("net.anyDownload! " + full_url);
	    return ret; 
	}
	
	private String trimBOM(String s)
	{
		while(s.length()>=1 && s.charAt(0)==0xFEFF)
			s=s.substring(1);

		return s;
	}
	public String Upload(String surl, String filename,String phpIP) 
	{ 
		if (!getNetStatus()) return "";
		String Return="";
    	String sURL = "";
	    try {
	    	if(phpIP==null)
	    		sURL = HostURLattach+surl;
	    	else
	    		sURL="http://"+phpIP+"/onair/"+surl;
	    	Log.i("net." + sURL + " << " + filename);
	    	HttpURLConnection URLConn = null;
	    	String proxyHost = android.net.Proxy.getDefaultHost();
    		if (proxyHost != null) {
    			java.net.Proxy p = new java.net.Proxy(java.net.Proxy.Type.HTTP, new InetSocketAddress(
    					android.net.Proxy.getDefaultHost(), android.net.Proxy.getDefaultPort()));
    			URLConn = (HttpURLConnection) new URL(sURL).openConnection(p);
    		} else {
    			URLConn = (HttpURLConnection) new URL(sURL).openConnection();
    		}
    		//
//	    	String sURL=HostURL+surl;
//	    	URL url = new URL(sURL); 
//	    	HttpURLConnection URLConn = (HttpURLConnection) url.openConnection(); 
	    	URLConn.setUseCaches(false);
	    	URLConn.setDoInput(true);
			URLConn.setDoOutput(true);
			URLConn.setReadTimeout(HTTP_READ_TIME_OUT);
			URLConn.setRequestMethod("GET"); 
			URLConn.setConnectTimeout(HTTP_CONNECTION_TIME_OUT);// zhao
			File f=new File(filename);
			FileInputStream fin = new FileInputStream(f);
			OutputStream out = URLConn.getOutputStream();
			long total=f.length();
			
			byte[] data = new byte[1024];
			int len;
			long amount=0;
			while((len=fin.read(data))>0)
			{
				out.write(data,0,len);
				Log.d("upload >>  "+len);
			}
			out.flush();
            out.close();
            fin.close();

            int code = URLConn.getResponseCode();            
            if (code == 200)
            {
            	String line = "";
                java.io.InputStream is = URLConn.getInputStream();
                BufferedReader reader = new BufferedReader(new InputStreamReader(is));
                while ((line = reader.readLine()) != null)
                    if (line.length() > 0)
                    	Return = Return + trimBOM(line.trim());
                is.close();
            }else{
            	Return = "Error:" + code;
            }
            
            URLConn.disconnect();
            
	    } catch (Exception e) {
	    	Toast.makeText(null,"Network Error, Cannot Upload",Toast.LENGTH_LONG).show();
	    	Log.e("Upload !@#$"+e.getMessage());
	    }
		Log.i("net.Upload! " + sURL + " :: " + Return);
	    return Return; 
	}
	
	
	public String doAnyPostHttps(String fullUrl, String data) {
		
		if (!getNetStatus()) return "";
    	Log.i("net." + fullUrl + " << " + data);
		String Return = "";
		try {
			HttpURLConnection URLConn = null;
			String sURL = fullUrl;

			trustAllHosts();
			URL Url = new URL(sURL);
			HttpsURLConnection https = null;
			NetType netType = NetworkControl.getNetType(mContext);
			
			String proxyHost = android.net.Proxy.getDefaultHost();
			if (netType == null || !netType.isWap())
				https = (HttpsURLConnection) Url.openConnection();
			else if (proxyHost != null && netType.isWap()) {
				java.net.Proxy p = new java.net.Proxy(java.net.Proxy.Type.HTTP,
						new InetSocketAddress(
								android.net.Proxy.getDefaultHost(),
								android.net.Proxy.getDefaultPort()));
				https = (HttpsURLConnection) Url.openConnection(p);
			}
			https.setHostnameVerifier(DO_NOT_VERIFY);
			URLConn = https;

			URLConn.setReadTimeout(HTTP_READ_TIME_OUT);
			URLConn.setDoOutput(true);
			URLConn.setDoInput(true);
			URLConn.setRequestMethod("POST");
			URLConn.setUseCaches(false);
			URLConn.setAllowUserInteraction(true);
			// HttpURLConnection.setFollowRedirects(true);
			URLConn.setInstanceFollowRedirects(true);

			URLConn.setRequestProperty("Content-Type",
					"application/x-www-form-urlencoded");
			URLConn.setRequestProperty("Content-Length",
					String.valueOf(data.getBytes().length));

			URLConn.setConnectTimeout(HTTP_CONNECTION_TIME_OUT);
			// URLConn.connect();

			OutputStream os = URLConn.getOutputStream();
			java.io.DataOutputStream dos = new java.io.DataOutputStream(os);

			dos.writeBytes(data);
			dos.close();

			int code = URLConn.getResponseCode();
			if (code == 200) {
				String line = "";
				java.io.InputStream is = URLConn.getInputStream();

				BufferedReader reader = new BufferedReader(
						new InputStreamReader(is));
				while ((line = reader.readLine()) != null)
					if (line.length() > 0)
						Return += trimBOM(line.trim());
				reader.close();
			} else
				Return = "Error:" + code;

			URLConn.disconnect();

		} catch (java.io.IOException e) {
			Log.e("doAnyPostHttps !@#$ " + e.getMessage());
		}
		Log.i("net.anyPostHttps! " + fullUrl + " :: " + Return);
		return Return;
	}
	
	public String doAnyPostHttp(String ip, String data) throws IOException{
		String Return="";
		String url=ip;
    	Log.i("net." + ip + " << " + data);
		HttpURLConnection URLConn=null;
		try {
			URLConn = (HttpURLConnection) new URL(url).openConnection();
		} catch (MalformedURLException e1) {
			e1.printStackTrace();
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		URLConn.setReadTimeout(HTTP_READ_TIME_OUT);
		URLConn.setDoOutput(true); 
		URLConn.setDoInput(true); 
		try {
			URLConn.setRequestMethod("POST");
		} catch (ProtocolException e1) {
			e1.printStackTrace();
		}
		URLConn.setUseCaches(false); 
		URLConn.setAllowUserInteraction(true); 
		URLConn.setInstanceFollowRedirects(true); 
		URLConn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded"); 
		URLConn.setConnectTimeout(HTTP_CONNECTION_TIME_OUT);
		
		if (data!=null)
		{
			URLConn.setRequestProperty("Content-Length",
					String.valueOf(data.getBytes().length));

			URLConn.setConnectTimeout(HTTP_CONNECTION_TIME_OUT);
			// URLConn.connect();

			OutputStream os = URLConn.getOutputStream();
			java.io.DataOutputStream dos = new java.io.DataOutputStream(os);

			dos.writeBytes(data);
			dos.close();
		}
		
		int code=0;
		try {
			code = URLConn.getResponseCode();
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		if (code == 200)
		{
			String line = "";
			java.io.InputStream is = URLConn.getInputStream();
		           
			BufferedReader reader = new BufferedReader(new InputStreamReader(is));
			while ((line = reader.readLine()) != null)
				if (line.length() > 0)
					Return = Return + trimBOM(line.trim());
		       	is.close();
		}else
			Return = "Error:" + code;
		
		URLConn.disconnect();
		Log.i("net.anyPostHttp! " + ip + " :: " + Return);
		return Return;
	}
}
