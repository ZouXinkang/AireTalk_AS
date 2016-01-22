package com.pingshow.amper;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Date;

import android.content.Context;

public class DQCurrency {
	private MyPreference mPref;
	String mCode;
	private String iso;
	private Context mContext;
	
	public DQCurrency(Context context)
	{
		mContext=context;
		mPref=new MyPreference(context);
		iso=mPref.read("iso","us");
	}

	public static final Object[][] Notes=new Object[][]
	{
	    {"tw", "NTD",30.0327357,      "$"},//Taiwan
	    {"th", "THB",31.075202,       "฿"},//Thailand
	    {"mm", "MMK",1000,            "K"},//Myanmar
	    {"sg", "SGD",1.27169997,      "$"},//Singapore
	    {"hk", "HKD",7.75398167,      "$"},//HongKong
	    {"kr", "KRW",1140.25086,      "₩"},//Korea
	    {"jp", "JPY",99.8402556,      "¥"},//Japan
	    
	    {"do", "DOP",41.949828,       "$"},//DOMINICAN REPUBLIC
	    {"jm", "JMD",100.877635,      "$"},//JAMAICA
	    {"us", "USD",1,               "$"},//US
	    {"ca", "CAD",1.05079987,      "$"},//Canada
	    
	    {"cn", "CNY",6.12718817,      "¥"},//China
	    
	    {"br", "BRL",2.26649955,      "R$"},//Brazil
	    {"ar", "ARS",5.39580961,      "$"},//Argentina
	    {"mx", "MXN",12.9242381,      "$"},//Mexico
	    {"cr", "CRC",500.500501,      "₡"},//COSTA RICA
	    
	    {"au", "AUD",1.10023105,      "$"},//Australia
	    {"at", "EUR",0.769349131,     "€"},//Austria
	    {"bd", "USD",1          ,     "$"},//Bangladesh
	    {"by", "EUR",0.769349131,     "€"},//Belarus
	    {"be", "EUR",0.769349131,     "€"},//Belgium
	    {"bg", "BGN",1.50199991,      "лв"},//Bulgaria
	    
	    {"bg", "USD",1,               "$"},//Central African
	    
	    {"co", "COP",1915.70881,      "$"},//Colombia
	    {"ck", "USD",1,               "$"},//Cook Islands
	    {"cu", "USD",1,               "$"},//Cuba
	    {"dk", "DKK",5.73950675,      "kr"},//Denmark
	    {"eg", "EGP",7.02878992,      "£"},//Egypt
	    {"ee", "EUR",0.769349131,     "€"},//Estonia
	    {"fi", "EUR",0.769349131,     "€"},//Finland
	    {"fr", "EUR",0.769349131,     "€"},//France
	    {"de", "EUR",0.769349131,     "€"},//Germany
	    
	    {"gr", "EUR",0.769349131,     "€"},//Greece
	    {"hu", "HUF",226.500566,      "Ft"},//Hungary
	    {"in", "INR",60.2119461,      "$"},//India
	    {"id", "IDR",9900.9901,       "Rp"},//Indonesia
	    {"ir", "USD",1,               "$"},//Iran
	    {"iq", "USD",1,               "$"},//Iraq
	    {"ie", "EUR",0.769349131,     "€"},//Ireland
	    {"il", "ILS",3.63410388,      "₪"},//Israel
	    {"it", "EUR",0.769349131,     "€"},//Italy
	    {"kz", "KZT",151.998784,      "$"},//Kazakhstan
	    {"la", "USD",1,               "$"},//Lao
	    {"li", "CHF",0.947400333,     "Fr"},//Liechtenstein
	    {"mo", "HKD",7.75398167,      "P"},//Macao
	    {"my", "MYR",3.18250387,      "RM"},//Malaysia
	    {"mt", "USD",1,               "$"},//Malta
	    
	    {"mn", "USD",1,               "$"},//Mongolia
	    {"mc", "EUR",0.769349131,     "€"},//Monaco
	    {"nl", "EUR",0.769349131,     "€"},//Netherlands
	    {"nz", "NZD",1.28584287,      "$"},//NewZealand
	    {"no", "NOK",6.11071391,      "kr"},//Norway
	    {"om", "OMR",0.38499001,      "$"},//Oman
	    {"pk", "PKR",99.950025,       "₨"},//Pakistan
	    {"pe", "PEN",2.78500331,      "S/."},//Peru
	    {"ph", "PHP",43.3801839,      "₱"},//Philippines
	    {"pl", "PLN",3.30980065,      "zł"},//Poland
	    {"pt", "EUR",0.769349131,     "€"},//Portugal
	    {"ro", "RON",3.41770508,      "L"},//Romania
	    {"ru", "RUB",33.1564987,      "руб."},//Russian
	    
	    {"za", "ZAR",10.087153,       "R"},//SouthAfrica
	    {"es", "EUR",0.769349131,     "€"},//Spain
	    {"sd", "USD",1,               "$"},//Sudan
	    {"se", "SEK",6.69160404,      "kr"},//Sweden
	    {"ch", "CHF",0.947400333,     "Fr"},//Switzerland
	    
	    {"ua", "UAH",8.1530158,       "₴"},//Ukraine
	    {"gb", "GBP",0.655093351,     "£"},//UK
	    
	    {"uz", "UZS",2092.05021,      "лв"},//Uzbekistan
	    {"vn", "VND",21276.5957,      "₫"},//Vietnam
	    
	    {"gt", "USD",1,               "$"},//GUATEMALA
	    {"sv", "USD",1,               "$"},//El Salvador
	    {"hn", "HNL",20.3000345,      "L"},//HONDURAS
	    {"ni", "NIO",24.6099326,      "C$"},//NICARAGUA
	    {"pa", "USD",1,               "$"},//PANAMA
	    {"ve", "VEF",2145.92275,      "Bs F"},//VENEZUELA
	    {"ec", "USD",1,               "$"},//ECUADOR
	    {"bo", "BOB",6.90999046,      "Bs."},//BOLIVIA
	    
	    {"py", "PYG",4524.88688,      "₲"},//PARAGUAY
	    {"uy", "UYU",20.7550694,      "$"},//URUGUAY
	    {"cl", "CLP",503.524673,      "$"},//CHILE
	    {"gy", "USD",1,               "$"},//GUYANA
	    
	    {"tr", "TRY",1.94649854,      "₤"},//TURKEY
	    {"sy", "USD",1,               "$"},//SYRIAN ARAB REPUBLIC
	    {"jo", "JOD",0.708200251,     "$"},//JORDAN
	    {"kw", "KWD",0.28576997,      "د.ك"},//KUWAIT
	    {"sa", "SAR",3.75030471,      "ر.س"},//SAUDI ARABIA 

	    {"sy", "YER",215.053763,      "$"},//YEMEN
	    {"ae", "AED",3.67300014,      "د.إ"},//UNITED ARAB EMIRATES
	    
	    {"ma", "MAD",8.56069102,      "$"},//MOROCCO
	    {"dz", "DZD",80.3987779,      "$"},//ALGERIA
	    
	    {"si", "EUR",0.769349131,     "€"},//SLOVENIA
	    {"hr", "HRK",5.74758889,      "kn"},//CROATIA
	    {"sb", "USD",1,               "$"},//SOLOMON ISLANDS
	    {"mk", "MKD",47.0898474,      "ден"},//MACEDONIA
	    {"cs", "EUR",0.769349131,     "€"},//SERBIA AND MONTENEGRO
	    {"cz", "CZK",20.0589734,      "Kč"},//CZECH REPUBLIC
	    {"lt", "LTL",2.65569687,      "$"},//LITHUANIA
	    {"ee", "EUR",0.769349131,     "€"},//ESTONIA
	    {"md", "MDL",12.5448478,      "L"},//MOLDOVA
	    
	    {"et", "USD",1,               "$"},//ETHIOPIA
	    {"sz", "USD",1,               "$"},//SWAZILAND
	    {"ls", "USD",1,               "$"},//LESOTHO
	    {"ng", "NGN",160.102466,      "₦"},//NIGERIA
	    {"et", "USD",1,               "$"},//GAMBIA
	    {"tn", "TND",1.66090057,      "د.ت"},//TUNISIA
	    
	    {"kh", "USD",1,               "$"},//CAMBODIA
	    {"pw", "USD",1,               "$"},//PALAU
	    {"mv", "USD",1,               "$"},//Maldives
	    {"np", "NPR",96.3483958,      "₨"},//Nepal
	    {"lu", "EUR",0.769349131,     "€"},//Luxembourg
	    {"ci", "USD",1,               "$"},//Cote d'Ivoire
	    {"fj", "FJD",1.87336081,      "$"},//Fiji
	    {"lb", "LBP",1510.57402,      "$"},//LEBANON
	    {"mg", "USD",1,               "$"},//Madagascar
	    
	    {"af", "USD",1,               "$"},//Afghanista
	    {"al", "USD",1,               "$"},//Albania
	    {"ao", "USD",1,               "$"},//Angola
	    {"am", "USD",1,               "$"},//Armenia
	};
	
	static private String trimBOM(String s)
	{
		while(s.length()>=1 && s.charAt(0)==0xFEFF)
			s=s.substring(1);
		return s;
	}
	
	static private String doHttpGET(String szURL)
	{
		String Return="";
		HttpURLConnection urlConnection=null;
		try{
			URL url = new URL(szURL);
			urlConnection = (HttpURLConnection) url.openConnection();
			java.io.InputStream is = urlConnection.getInputStream();
			String line = "";
			BufferedReader reader = new BufferedReader(new InputStreamReader(is));
			while ((line = reader.readLine()) != null)
				if (line.length() > 0)
					Return = Return + trimBOM(line.trim());
		       	is.close();
		}
		catch(Exception e)
		{
			
		}
		finally {
		     if (urlConnection!=null) urlConnection.disconnect();
		}
		return Return;
	}
	
	void updateCurrency(String code)
	{	    
		String Return=doHttpGET("http://rate-exchange.appspot.com/currency?from=USD&to="+code);
		if (Return.length()>0)
		{
	        String [] items=Return.split(",");
	        if (items.length>2 && items[1].length()>7)
	        {
	        	//{"to": "EUR", "rate": 0.73569499999999999, "from": "USD"}
	        	String rhs=items[1];
	        	rhs=rhs.replace(" ", "");
	        	if (rhs.startsWith("\"rate\":"))
	        		rhs=rhs.substring(7);
	        	try{
	        		float rate=Float.parseFloat(rhs);
		        	mPref.writeFloat(code,rate);
	        	}catch(Exception e)
	        	{
	        		
	        	}
	        }
		}
	}
	
	float getCurrencyFromUSD()
	{
	    if (mCode==null) mCode=getCurrencyCode();
	    if (mCode.equals("USD"))
	        return 1.0f;
	    
	    //UPDATE Currency : code
	    long last=mPref.readLong("LastUpdateCurrency",0);
	    long now=new Date().getTime();
	    if (now-last<43200000)
	    {
	        return getCurrency();
	    }
	    
	    mPref.writeLong("LastUpdateCurrency",now);
	    new Thread(){
	    	public void run() {
	    		updateCurrency(mCode);
	    	};
	    }.start();
	    
	    return getCurrency();
	}
	
	public String translate(String code)
	{
		final String _code[]={"USD","NTD","CNY","KRW","JPY","EUR","HKD","GBP"};
		for(int j = 0;j<_code.length;j++)
		{
			if (code.equals(_code[j]))
			{
				String [] array = mContext.getResources().getStringArray(R.array.currency_code);
				return array[j];
			}
		}
		return code;
	}
	
	public String getCurrencyCode()
	{
	    if (mCode!=null) return mCode;
	    
	    for (int i=0;i<Notes.length;i++)
	    {
	        if (iso.equals(Notes[i][0]))
	        {
	            return translate((String)Notes[i][1]);
	        }
	    }
	    return "USD";
	}
	
	public boolean isUsingUSD()
	{
	    for (int i=0;i<Notes.length;i++)
	    {
	        if (iso.equals(Notes[i][0]))
	        {
	            return ((String)Notes[i][1]).equals("USD");
	        }
	    }
	    return false;
	}
	
	public String getCurrencySymbol()
	{   
	    for (int i=0;i<Notes.length;i++)
	    {
	        if (iso.equals(Notes[i][0]))
	        {
	            return (String)Notes[i][3];
	        }
	    }
	    return "$";
	}
	
	public float getCurrency()
	{   
		float curr=mPref.readFloat(mCode,0);
		if (curr!=0) return curr;
		curr=1.f;
	    for (int i=0;i<Notes.length;i++)
	    {
	        if (iso.equals(Notes[i][0]))
	        {
	        	curr=Float.valueOf(String.valueOf(Notes[i][2]));
	            return curr;
	        }
	    }
	    return curr;
	}
	
	float getEuroCurrency()
	{
		long last=mPref.readLong("LastUpdateEuroCurrency", 0);
		long now=new Date().getTime();
	    if (now-last<43200000)
	    {
	        float curr=mPref.readFloat("EUR",0);
	        if (curr!=0) return curr;
	    }
	    mPref.writeLong("LastUpdateEuroCurrency",now);
	    updateCurrency("EUR");
	    
	    float curr=mPref.readFloat("EUR",0);
	    if (curr!=0) return curr;
	    
	    return 0.769349131f;
	}

}
