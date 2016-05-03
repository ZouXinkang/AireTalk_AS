package com.pingshow.util;

import android.content.Context;
import android.telephony.TelephonyManager;

import com.pingshow.amper.AireJupiter;
import com.pingshow.amper.Log;
import com.pingshow.amper.MyPreference;
import com.pingshow.amper.R;

public class MyTelephony {
	static public String iso;
	static public String isoNet;
	static public String myPhoneNumber;
	
	static public void init(Context context)
	{
		if (iso==null || iso.length()==0)
		{
			TelephonyManager tMgr = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
			iso = tMgr.getSimCountryIso().toLowerCase();
			isoNet = tMgr.getNetworkCountryIso().toLowerCase();
			if (iso == null || iso.length() == 0) {//no sim card
				MyPreference p = new MyPreference(context);
				iso = p.read("iso","us");
				isoNet = p.read("iso","us");
			}
			if (AireJupiter.getInstance() != null && AireJupiter.myPhoneNumber != null)
				myPhoneNumber = AireJupiter.myPhoneNumber;
		}
		Log.d("MyTelephony iso=" + iso + " " + isoNet + " " + myPhoneNumber);
	}
	
	static public boolean SameNumber(String A, String B)
	{
		if (A==null || B==null)
		{
			return false;
		}
		if (A.startsWith("+") && B.startsWith("+"))
		{
			return A.equals(B);
		}
		int a=A.length();
		int b=B.length();
		if (a>b)
		{
			if (a-b>6) return false;
			if (A.startsWith(B, a-b) || A.startsWith(B.substring(1), a-b+1))
				return true;
		}
		else{
			if (b-a>6) return false;
			if (B.startsWith(A, b-a) || B.startsWith(A.substring(1), b-a+1))
				return true;
		}
		return false;
	}
	
	static public boolean isPhoneNumber(String tmp)
	{
		if (tmp==null) return false;
		if (tmp.startsWith("+"))
			return tmp.substring(1).matches("^[0-9]+$");
		return tmp.matches("^[0-9]+$");
	}
	
	static public String attachPrefix(Context context, String userNumber)
	{
		if (!isPhoneNumber(userNumber)) return userNumber;
		if (context==null || userNumber==null) return userNumber;

		if (userNumber.startsWith("+"))
			return userNumber;

		if (iso==null || iso.length()==0)
		{
			TelephonyManager tMgr=(TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
			iso=tMgr.getSimCountryIso().toLowerCase();
			isoNet=tMgr.getNetworkCountryIso().toLowerCase();
			if (iso==null || iso.length()==0) {//no sim card
				MyPreference p=new MyPreference(context);
				iso=p.read("iso","us");
				isoNet=p.read("iso","us");
			}
			if (AireJupiter.getInstance()!=null && AireJupiter.myPhoneNumber!=null)
				myPhoneNumber=AireJupiter.myPhoneNumber;
		}

		return addPrefix(isoNet, iso, userNumber, false);
	}
	
	static public String attachFixedPrefix(Context context, String userNumber)
	{
		if (!isPhoneNumber(userNumber)) return userNumber;
		if (context==null || userNumber==null) return userNumber;

		if (userNumber.startsWith("+"))
			return userNumber;

		if (iso==null || iso.length()==0)
		{
			TelephonyManager tMgr=(TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
			iso=tMgr.getSimCountryIso().toLowerCase();
			isoNet=tMgr.getNetworkCountryIso().toLowerCase();
			if (iso==null || iso.length()==0) {//no sim card
				MyPreference p=new MyPreference(context);
				iso=p.read("iso","us");
				isoNet=p.read("iso","us");
			}
			if (AireJupiter.getInstance()!=null && AireJupiter.myPhoneNumber!=null)
				myPhoneNumber=AireJupiter.myPhoneNumber;
		}
		
		return addFixedPrefix(isoNet, iso, userNumber, false);
	}
	
	static public String getCountryCode(String iso)
	{
		for(int i = 0;i<COUNTRIES.length;i++)
			if(iso.equals(COUNTRIES[i][0]))
				return COUNTRIES[i][3]+" ";
		return "+86 ";
	}
	
	
	static public String cleanPhoneNumber(String number)
	{
		if (number==null) return null;
		String s=number.replace(" ", "").replace("-", "").replace("(", "").replace(")","").replace("*","");//.replace(".","").replace(",", "").replace("p", "");
		if (s.contains("#"))
			s.substring(0, s.indexOf("#"));
		return s;
	}
	
	static public String cleanPhoneNumber2(String number)
	{
		if (number==null) return null;
		String s=number.replace(" ", "").replace("-", "").replace("(", "").replace(")","");//.replace(".","").replace(",", "").replace("p", "");
		if (s.contains("#"))
			s.substring(0, s.indexOf("#"));
		return s;
	}

	static public String cleanPhoneNumber3(String number)
	{
		if (number==null) return null;
		String s=number.replace(" ", "").replace("-", "").replace("(", "").replace(")","").replace(".","").replace(",", "").replace("p", "");
		if (s.contains("#"))
			s.substring(0, s.indexOf("#"));
		return s;
	}
	
	/**
	 * country iso {{iso,prefix,numberLen},...}
	 */
	public static final String[][] COUNTRIES=new String[][]{
		{"cn","+8613|+8614|+8615|+8618|+8617","14","+86"},  // china jack 16/5/3 增加17字段
		{"tw","+8869","13","+886"}, // taiwan
		{"do","+1809|+1829|+1849","12","+1"},//DOMINICAN REPUBLIC
		{"jm","+1876","12","+1"},//JAMAICA
		{"dm","+1767","12","+1"},//DOMINICA
		{"vi","+1340","12","+1"},//VIRGIN ISLANDS
		{"vg","+1340","12","+1"},//VIRGIN ISLANDS
		{"ca","+1778|+1587|+1236|+1450|+1431|+1438|+1437|+1819|+1418|+1416|+1780|+1807|+1902|+1403|+1250|+1289|+1226|+1905|+1604|+1514|+1204|+1506|+1365|+1873|+1709|+1867|+1306|+1647|+1905|+1343|+1306|+1705|+1519|+1613|+1639|+1249|+1581","12","+1"},  // Canada
		{"us","+1","12","+1"},  // US
		{"hk","+8525|+8526|+8529","12","+852"},  // Hong Kong
		{"mo","+8536","12","+853"},  // Macau
		{"jp","+8180|+8190|+8170","13","+81"},  // JAPAN
		{"kr","+821","13","+82"}, // South Korea
		{"sg","+658|+659","11","+65"},  // Singapore
		{"fr","+336","12","+33"},  // France
		{"es","+346","12","+34"},  // SPAIN
		{"se","+467","12","+46"},  // SWEDEN
		{"ch","+417","12","+41"},  // SWITZERLAND
		{"gb","+447","13","+44"},  // UNITED KINGDOM
		{"nz","+642","11|13","+64"},  // NEW ZEALAND       
		{"fi","+3584|+3585","13","+358"},  // FINLAND
		{"de","+491","13|14","+49"},  // Germany
		{"it","+393","13","+39"},  // Italy
		{"nl","+316","12","+31"},  // NETHERLANDS
		{"dk","+4540","11","+45"},  // DENMARK 
		{"ru","+79|+75","12","+7"},  // RUSSIAN FEDERATION 
		{"in","+919","13","+91"},  // India 
		{"th","+668","12","+66"},  // Thailand
		{"my","+601","12|13","+60"},  // MALAYSIA
		{"no","+474|+479","11","+47"},  // Norway
		{"mx","+521","14|13","+52"},  // Mexico
		{"br","+55","13","+55"},  // Brazil 
		{"za","+277|+278","12","+27"},  // South Africa
		{"ie","+3538","13","+353"},  // IRELAND 
		{"ar","+549","14","+54"},  // ARGENTINA    IMPORTANT! If the cell number begins with 15 you must drop these two digits
		{"au","+61","12","+61"},  // AUSTRALIA 
		{"at","+436","13","+43"},  // AUSTRIA 
		{"mc","+3774|+3776","12","+377"},  // MONACO 
		{"be","+324","12","+32"},  // BELGIUM  
		{"co","+573","13","+57"},  // COLOMBIA 
		{"il","+9725","13","+972"},   // ISRAEL
		{"id","+628","12|13","+62"},  // INDONESIA 
		{"gr","+306","13","+30"},  // GREECE
		{"hu","+3620|+3630|+3670","12","+36"},  // HUNGARY  
		{"ph","+639","13|12","+63"},  // PHILIPPINES        some cellular numbers may contain less digits; however the first digit is always 9   
		{"pl","+48","12","+48"}, // POLAND
		{"pt","+3519","13","+351"},   // PORTUGAL
		{"ro","+407","12","+40"},  // ROMANIA
		{"am","+3749","12","+374"},  // ARMENIA
		{"ua","+380","13","+380"},  // UKRAINE
		{"bd","+8801","14","+880"},  // BANGLADESH
		{"cu","+535","11","+53"},  // CUBA
		{"eg","+201","12|13","+20"},  // EGYPT     
		{"by","+37525|+37529|+37533|+37544","13","+375"},  // BELARUS
		{"bg","+35987|+35988|+35989|+35998","13","+359"},  // BULGARIA 
		{"cf","+2367","12","+236"},  // CENTRAL AFRICAN REPUBLIC
		{"ee","+3725|+37281|+37282","12","+372"},  // ESTONIA     
		{"la","+85620","14","+856"},  // LAO PEOPLE'S DEMOCRATIC REPUBLIC  
		{"li","+4237","11","+423"},  // LIECHTENSTEIN
		{"mt","+35679|+35699","12","+356"},  // MALTA
		{"ck","+6825|+6827","9","+682"},  // COOK ISLANDS 
		{"mn","+9765|+9768|+9769","12","+976"},  // MONGOLIA 
		{"kz","+76","12","+7"},  // KAZAKHSTAN  
		{"om","+9689","12","+968"},  // OMAN  
		{"pk","+923","13","+92"}, // PAKISTAN   
		{"pe","+519","12","+51"},  // PERU 
		{"ir","+989","13","+98"}, // IRAN
		{"iq","+9647","14","+964"}, // IRAQ
		{"af","+937","12","+93"},  // AFGHANISTAN 
		{"al","+3556","13","+355"},  // ALBANIA
		{"ao","+2449","13","+244"},  // ANGOLA 
		{"uz","+9989","13","+998"},  // UZBEKISTAN   
		{"vn","+849|+841","12","+84"},  // VIETNAM
		{"sd","+2499","13","+249"},   // SUDAN
		
		//alec: added on Sep 21, 2012
		{"cr","+5068","12","+506"},//COSTA RICA
		{"gt","+5024|+5025","12","+502"},//GUATEMALA
		{"sv","+5037","12","+503"},//El Salvador
		{"hn","+5043|+5047|+5048|+5049","12","+504"},//HONDURAS
		{"ni","+5058","12","+505"},//NICARAGUA
		{"pa","+507","12","+507"},//PANAMA
		{"ve","+584","13","+58"},//VENEZUELA
		{"ec","+5939|+5938","12","+593"},//ECUADOR
		{"bo","+5917","12","+591"},//BOLIVIA
		
		{"py","+5959","13","+595"},//PARAGUAY
		{"uy","+5989","12","+598"},//URUGUAY
		{"cl","+569","12","+56"},//CHILE
		{"gy","+5926","11","+592"},//GUYANA
		
		{"tr","+905","13","+90"},//TURKEY
		{"sy","+9639","13","+963"},//SYRIAN ARAB REPUBLIC
		{"jo","+9627","13","+962"},//JORDAN
		{"kw","+9655|+9656|+9659","12","+965"},//KUWAIT
		{"sa","+9665","13","+966"},//SAUDI ARABIA
		{"ye","+9677","13","+967"},//YEMEN
		{"ae","+9715","13","+971"},//UNITED ARAB EMIRATES
		
		{"ma","+2126","13","+212"},//MOROCCO
		{"dz","+21355|+21366|+21369|+21377|+21379","13","+213"},//ALGERIA
		
		{"si","+386","12","+386"},//SLOVENIA
		{"hr","+3859","13","+385"},//CROATIA
		{"sb","+6777|+67778","11","+677"},//SOLOMON ISLANDS
		{"mk","+38970","12","+389"},//MACEDONIA
		{"cs","+3816","12|13","+381"},//SERBIA AND MONTENEGRO
		{"cz","+4206|+4207","13","+420"},//CZECH REPUBLIC
		{"lt","+3706","12","+370"},//LITHUANIA
		{"ee","+3725|+37281|+37282","12","+372"},//ESTONIA
		{"md","+3736|+3737","12","+373"},//MOLDOVA
		
		{"et","+25191","13","+251"},//ETHIOPIA
		{"sz","+2687","12","+268"},//SWAZILAND
		{"ls","+2665|+2666","12","+266"},//LESOTHO
		{"ng","+23470|+23480|+23481","14","+234"},//NIGERIA
		{"gm","+2207|+2209","11","+220"},//GAMBIA
		{"tn","+2169|+2162","12","+216"},//TUNISIA
		
		{"mm","+959","11|12|13","+95"},//MYANMAR
		{"kh","+855","12|13","+855"},//CAMBODIA
		{"pw","+680","11","+680"},//PALAU
		{"mv","+9607|+9609","11","+960"},//Maldives
		{"np","+97798","14","+977"},//Nepal
		{"lu","+3526","13","+352"},//Luxembourg
		{"ci","+2250|+2254|+2256","12","+225"},//Cote d'Ivoire
		{"fj","+6799|+6797","11","+679"},//Fiji
		{"lb","+9613|+9617","11|12","+961"},//LEBANON
		{"mg","+2613","13","+261"},//Madagascar
		
		{"bh","+9733","12","+973"},//Bahrain
		{"sr","+5976|+5977|+5978","11","+597"},//SURINAME
		{"az","+994","13","+994"},//Azerbaijan
		{"qa","+97433|+97455|+97466|+97477","12","+974"},//Qatar
		{"gh","+2332|+2335","13","+233"},//Ghana
		{"mu","+230","11","+230"},//Mauritius
		{"lv","+3712","12","+371"},//LATVIA
		{"ge","+9955|+9957","13","+995"},//GEORGIA
		
		{"pg","+6757","12","+675"},//Papua New Guinea
		{"lk","+947","12","+94"},//Sri Lanka
		
		{"ba","+3876","12","+387"},//Bosnia and Herzegovina
	};
	
	public static final String[][] FIXED_COUNTRIES=new String[][]{
		{"cn","+8610|+862|+863|+864|+865|+866|+867|+868|+869","14|13|11|12","+86"},  // china
		{"tw","+8862|+8863|+8864|+8865|+8866|+8867|+8868","13|12","+886"}, // taiwan
		
		{"do","+1809|+1829|+1849","12","+1"},//DOMINICAN REPUBLIC
		{"jm","+1876","12","+1"},//JAMAICA
		{"dm","+1767","12","+1"},//DOMINICA
		{"vi","+1340","12","+1"},//VIRGIN ISLANDS
		{"vg","+1340","12","+1"},//VIRGIN ISLANDS
		
		{"ca","+1778|+1587|+1236|+1450|+1431|+1438|+1437|+1819|+1418|+1416|+1780|+1807|+1902|+1403|+1250|+1289|+1226|+1905|+1604|+1514|+1204|+1506|+1365|+1873|+1709|+1867|+1306|+1647|+1905|+1343|+1306|+1705|+1519|+1613|+1639|+1249|+1581","12","+1"},  // Canada
		{"us","+1","12","+1"},  // US
		
		{"hk","+852","12","+852"},  // Hong Kong
		{"mo","+853","12","+853"},  // Macau
		{"jp","+81","12","+81"},  // JAPAN
		{"kr","+82","11|12","+82"}, // South Korea
		{"sg","+65","11","+65"},  // Singapore
		{"fr","+33","12","+33"},  // France
		{"es","+34","12","+34"},  // SPAIN
		{"se","+46","12|11|10","+46"},  // SWEDEN
		{"ch","+41","12","+41"},  // SWITZERLAND
		{"gb","+44","13","+44"},  // UNITED KINGDOM
		{"nz","+64","11","+64"},  // NEW ZEALAND       
		{"fi","+358","13|14","+358"},  // FINLAND
		{"de","+49","13|14|15|16","+49"},  // Germany
		{"it","+39","13|9|10|11|12","+39"},  // Italy
		{"nl","+31","12","+31"},  // NETHERLANDS
		{"dk","+45","11","+45"},  // DENMARK 
		{"ru","+7","12","+7"},  // RUSSIAN FEDERATION 
		{"in","+91","13","+91"},  // India 
		{"th","+66","12","+66"},  // Thailand
		{"my","+60","12|13","+60"},  // MALAYSIA
		{"no","+47","11","+47"},  // Norway
		{"mx","+52","13","+52"},  // Mexico
		{"br","+55","13|14","+55"},  // Brazil 
		{"za","+27","12","+27"},  // South Africa
		{"ie","+353","13","+353"},  // IRELAND 
		{"ar","+54","13","+54"},  // ARGENTINA
		{"au","+61","12","+61"},  // AUSTRALIA 
		{"at","+43","11","+43"},  // AUSTRIA 
		{"mc","+377","12","+377"},  // MONACO 
		{"be","+32","11","+32"},  // BELGIUM  
		{"co","+57","11","+57"},  // COLOMBIA 
		{"il","+972","12","+972"},   // ISRAEL
		{"id","+62","12|13|10|11|14","+62"},  // INDONESIA 
		{"gr","+30","13","+30"},  // GREECE
		{"hu","+36","11","+36"},  // HUNGARY  
		{"ph","+63","11|12","+63"},  // PHILIPPINES  
		{"pl","+48","12","+48"}, // POLAND
		{"pt","+351","13","+351"},   // PORTUGAL
		{"ro","+40","12","+40"},  // ROMANIA
		{"am","+374","12","+374"},  // ARMENIA
		{"ua","+380","13","+380"},  // UKRAINE
		{"bd","+880","11|12|13|14","+880"},  // BANGLADESH
		{"cu","+53","11|10|9","+53"},  // CUBA
		{"eg","+20","12|11","+20"},  // EGYPT     
		{"by","+375","13","+375"},  // BELARUS
		{"bg","+359","11|12","+359"},  // BULGARIA 
		{"cf","+236","12","+236"},  // CENTRAL AFRICAN REPUBLIC
		{"ee","+372","11","+372"},  // ESTONIA     
		{"la","+856","14","+856"},  // LAO PEOPLE'S DEMOCRATIC REPUBLIC  
		{"li","+423","11","+423"},  // LIECHTENSTEIN
		{"mt","+356","12","+356"},  // MALTA
		{"ck","+682","9","+682"},  // COOK ISLANDS 
		{"mn","+976","11|12","+976"},  // MONGOLIA 
		{"kz","+7","12","+7"},  // KAZAKHSTAN  
		{"om","+968","12","+968"},  // OMAN  
		{"pk","+92","13|12","+92"}, // PAKISTAN   
		{"pe","+51","11","+51"},  // PERU 
		{"ir","+98","13","+98"}, // IRAN
		{"iq","+964","12|13","+964"}, // IRAQ
		{"af","+93","12","+93"},  // AFGHANISTAN 
		{"al","+355","12","+355"},  // ALBANIA
		{"ao","+2442","13","+244"},  // ANGOLA 
		{"uz","+998","13","+998"},  // UZBEKISTAN   
		{"vn","+84","9|10|11|12|13","+84"},  // VIETNAM
		{"sd","+249","13","+249"},   // SUDAN
		
		//alec: added on Sep 21, 2012
		{"cr","+506","12","+506"},//COSTA RICA
		{"gt","+502","12","+502"},//GUATEMALA
		{"sv","+5032","11","+503"},//El Salvador
		{"hn","+5042","12","+504"},//HONDURAS
		{"ni","+5052","12","+505"},//NICARAGUA
		{"pa","+507","11","+507"},//PANAMA
		{"ve","+58","13","+58"},//VENEZUELA
		{"ec","+593","12","+593"},//ECUADOR
		{"bo","+591","12","+591"},//BOLIVIA
		
		{"py","+595","13|12","+595"},//PARAGUAY
		{"uy","+598","12","+598"},//URUGUAY
		{"cl","+56","12|11","+56"},//CHILE
		{"gy","+592","11","+592"},//GUYANA
		
		{"tr","+90","13","+90"},//TURKEY
		{"sy","+963","13|12","+963"},//SYRIAN ARAB REPUBLIC
		{"jo","+962","12","+962"},//JORDAN
		{"kw","+965","12","+965"},//KUWAIT
		{"sa","+966","13|12","+966"},//SAUDI ARABIA
		{"ye","+967","12|11","+967"},//YEMEN
		{"ae","+971","12","+971"},//UNITED ARAB EMIRATES
		
		{"ma","+212","13","+212"},//MOROCCO
		{"dz","+213","12","+213"},//ALGERIA
		
		{"si","+386","12","+386"},//SLOVENIA
		{"hr","+385","12","+385"},//CROATIA
		{"sb","+677","9","+677"},//SOLOMON ISLANDS
		{"mk","+389","12","+389"},//MACEDONIA
		{"cs","+381","12|13","+381"},//SERBIA AND MONTENEGRO
		{"cz","+420","13","+420"},//CZECH REPUBLIC
		{"lt","+370","12","+370"},//LITHUANIA
		{"ee","+372","11","+372"},//ESTONIA
		{"md","+373","12","+373"},//MOLDOVA
		
		{"et","+251","13","+251"},//ETHIOPIA
		{"sz","+268","11","+268"},//SWAZILAND
		{"ls","+266","12","+266"},//LESOTHO
		{"ng","+234","11|12","+234"},//NIGERIA
		{"gm","+220","11","+220"},//GAMBIA
		{"tn","+2167","12","+216"},//TUNISIA
		
		{"mm","+95","11|12|10|9","+95"},//MYANMAR
		{"kh","+855","12|13","+855"},//CAMBODIA
		{"pw","+680","11","+680"},//PALAU
		{"mv","+960","11","+960"},//Maldives
		{"np","+977","12","+977"},//Nepal
		{"lu","+352","13|14|12|15|11","+352"},//Luxembourg
		{"ci","+225","12","+225"},//Cote d'Ivoire
		{"fj","+679","11","+679"},//Fiji
		{"lb","+961","11","+961"},//LEBANON
		{"mg","+261","13","+261"},//Madagascar
		
		{"bh","+973","12","+973"},//Bahrain
		{"sr","+597","10","+597"},//SURINAME
		{"az","+994","13","+994"},//Azerbaijan
		{"qa","+974","12","+974"},//Qatar
		{"gh","+2333","13","+233"},//Ghana
		{"mu","+230","11","+230"},//Mauritius
		{"lv","+3716","13","+371"},//LATVIA
		{"ge","+9953|+9954","13","+995"},//GEORGIA
		
		{"pg","+6753|+6755|+6759","11","+675"},//Papua New Guinea
		{"lk","+94","12","+94"},//Sri Lanka
		
		{"ba","+3875|+3873|+3874|+387","12","+387"},//Bosnia and Herzegovina
	};
	
	public static boolean validWithCurrentISO(String number)
	{
	    String tmp=number;
	    if (tmp==null || tmp.length()<7) return false;
	    int index=getCountryIndexByIso(iso);
	    
	    if(tmp.startsWith("0") && !tmp.startsWith("00") && !tmp.startsWith("011"))    // phone number has prefix 0
	    	tmp=tmp.substring(1);
	    if (!tmp.startsWith("+"))
	    	tmp=COUNTRIES[index][3]+tmp;
	    
	    if(checkValidePhone(iso,tmp))
	        return true;
	    
	    return false;
	}
	
	public static String addPrefixWithCurrentISO(String number)
	{
	    String tmp=number;
	    if (tmp==null || tmp.length()<7) return tmp;
	    int index=getCountryIndexByIso(iso);
	    
	    if(tmp.startsWith("0") && !tmp.startsWith("00") && !tmp.startsWith("011"))    // phone number has prefix 0
	    	tmp=tmp.substring(1);
	    if (!tmp.startsWith("+"))
	    	tmp=COUNTRIES[index][3]+tmp;
	    
	    return tmp;
	}
	
	public static boolean validLandLineWithCurrentISO(String number)
	{
	    String tmp=number;
	    if (tmp==null || tmp.length()<7) return false;
	    int index=getCountryIndexByIso(iso);
	    
	    if(tmp.startsWith("0") && !tmp.startsWith("00") && !tmp.startsWith("011"))    // phone number has prefix 0
	    	tmp=tmp.substring(1);
	    if (!tmp.startsWith("+"))
	    	tmp=COUNTRIES[index][3]+tmp;
	    
	    if(checkValidFixed(iso,tmp))
	        return true;
	    
	    return false;
	}
	
	public static String addPrefixLandLineWithCurrentISO(String number)
	{
	    String tmp=number;
	    if (tmp==null || tmp.length()<7) return tmp;
	    int index=getCountryIndexByIso(iso);
	    
	    if(tmp.startsWith("0") && !tmp.startsWith("00") && !tmp.startsWith("011"))    // phone number has prefix 0
	    	tmp=tmp.substring(1);
	    if (!tmp.startsWith("+"))
	    	tmp=COUNTRIES[index][3]+tmp;
	    
	    return tmp;
	}
		
	public static boolean checkValidePhone(String iso, String number)
	{
		if (iso==null || number==null || number.length()<11) 
			return false;
		
		for(int i = 0;i<COUNTRIES.length;i++)
		{
			if(iso.equals(COUNTRIES[i][0]))
			{
				String[] startStr = COUNTRIES[i][1].split("\\|");
				for(int j = 0;j<startStr.length;j++)
				{
					if(number.startsWith(startStr[j]))
					{
						String[] numberLength = COUNTRIES[i][2].split("\\|");
						for(int k = 0;k<numberLength.length;k++)
						{
							if(number.length() == Integer.parseInt(numberLength[k]))
								return true;
						}
					}
				}
				break;
			}
		}
		return false;
	}
	
	public static boolean checkValidFixed(String iso, String number)
	{
		if (iso==null || number==null || number.length()<11) 
			return false;
		
		for(int i = 0;i<FIXED_COUNTRIES.length;i++)
		{
			if(iso.equals(FIXED_COUNTRIES[i][0]))
			{
				String[] startStr = FIXED_COUNTRIES[i][1].split("\\|");
				for(int j = 0;j<startStr.length;j++)
				{
					if(number.startsWith(startStr[j]))
					{
						String[] numberLength = FIXED_COUNTRIES[i][2].split("\\|");
						for(int k = 0;k<numberLength.length;k++)
						{
							if(number.length() == Integer.parseInt(numberLength[k]))
								return true;
						}
					}
				}
				break;
			}
		}
		return false;
	}
	
	
	static public String addPrefix(String networkCountryIso, String simCountryIso, String phoneNumber, boolean forceSIMcardIso)
    {
    	if(phoneNumber == null || networkCountryIso == null || simCountryIso == null || phoneNumber.length()==0)
    	{
    		return phoneNumber;
    	}
    	if(phoneNumber.startsWith("+")) // phone number has prefix +
    	{
    		return phoneNumber;
    	}
    	//alec
        if(phoneNumber.startsWith("0") && !phoneNumber.startsWith("00") && !phoneNumber.startsWith("011"))    // phone number has prefix 0
        {
        	phoneNumber=phoneNumber.substring(1);        
        }

    	if(phoneNumber.startsWith("00") && phoneNumber.length()>11) // phone number has prefix 00
    	{
    		for(int i = 0;i<COUNTRIES.length;i++)
    		{
    			String temp="+"+phoneNumber.substring(2);
    			if (checkValidePhone(COUNTRIES[i][0], temp))
    				return temp;
    		}
    	}
    	if(phoneNumber.startsWith("011") && phoneNumber.length()>11) // phone number has prefix 011
		{
    		for(int i = 0;i<COUNTRIES.length;i++)
    		{
    			String temp="+"+phoneNumber.substring(3);
    			if (checkValidePhone(COUNTRIES[i][0], temp))
    				return temp;
    		}
		}
    	if(phoneNumber.startsWith("00") && (networkCountryIso.equals("tw")||networkCountryIso.equals("id")) && phoneNumber.length()>13) // phone number has prefix 00X
    	{
    		for(int i = 0;i<COUNTRIES.length;i++)
    		{
    			String temp="+"+phoneNumber.substring(3);
    			if (checkValidePhone(COUNTRIES[i][0], temp))
    				return temp;
    		}
    	}
		if(forceSIMcardIso)
    	{
    		for(int i = 0;i<COUNTRIES.length;i++)
    		{
    			if(simCountryIso.equals(COUNTRIES[i][0]))
    			{
    				return dropPrefixZero(i, COUNTRIES[i][3], filterNumber(simCountryIso,phoneNumber), simCountryIso);
    			}
    		}
    	}
		if((simCountryIso.equals("us") || simCountryIso.equals("ca") || simCountryIso.equals("pr") || simCountryIso.equals("dm")) && phoneNumber.length()==7)//US/CN area code
		{
			if (myPhoneNumber.startsWith("+1"))
			{
				String areaCode=myPhoneNumber.substring(2, 5);
				return "+1" + areaCode + phoneNumber;
			}
		}
		
		for(int i = 0;i<COUNTRIES.length;i++)
		{
			String temp="+"+phoneNumber;
			if (checkValidePhone(COUNTRIES[i][0], temp))
				return temp;
		}

		if(simCountryIso.equals(networkCountryIso)) // Judge simCountryIso and networkCountryIso is the same
    	{
			String temp;
    		for(int i = 0;i<COUNTRIES.length;i++)
    		{
    			if(simCountryIso.equals(COUNTRIES[i][0]))
    			{
    				temp=dropPrefixZero(i, COUNTRIES[i][3], filterNumber(simCountryIso,phoneNumber), simCountryIso);
    				if (checkValidePhone(networkCountryIso, temp))
    					return temp;
    			}
    		}
    		for(int i = 0;i<COUNTRIES.length;i++)
    		{
				temp=dropPrefixZero(i, COUNTRIES[i][3], phoneNumber, COUNTRIES[i][0]);
				if (checkValidePhone(COUNTRIES[i][0], temp))
					return temp;
    		}
    	}
    	else // which doesn't equal
    	{
    		String temp;
    		for(int i = 0;i<COUNTRIES.length;i++)
    		{
    			if(networkCountryIso.equals(COUNTRIES[i][0]))
    			{
    				temp = dropPrefixZero(i, COUNTRIES[i][3], filterNumber(networkCountryIso, phoneNumber), networkCountryIso);
    				if (checkValidePhone(networkCountryIso, temp))
    					return temp;
    			}
    			if(simCountryIso.equals(COUNTRIES[i][0]))
    			{
    				temp = dropPrefixZero(i, COUNTRIES[i][3], filterNumber(simCountryIso, phoneNumber), simCountryIso);
    				if (checkValidePhone(simCountryIso, temp))
    					return temp;
    			}
    		}
		}

		for(int i = 0;i<COUNTRIES.length;i++)
		{
			String temp=COUNTRIES[i][3]+phoneNumber;
			if (checkValidePhone(COUNTRIES[i][0], temp))
				return temp;
		}
		
    	return phoneNumber;
    }
	
	
	static public String addFixedPrefix(String networkCountryIso, String simCountryIso, String phoneNumber, boolean forceSIMcardIso)
    {
    	if(phoneNumber == null || networkCountryIso == null || simCountryIso == null || phoneNumber.length()==0)
    	{
    		return phoneNumber;
    	}
    	if(phoneNumber.startsWith("+")) // phone number has prefix +
    	{
    		return phoneNumber;
    	}
    	//alec
        if(phoneNumber.startsWith("0") && !phoneNumber.startsWith("00") && !phoneNumber.startsWith("011") && !networkCountryIso.equals("it"))    // phone number has prefix 0
        {
        	phoneNumber=phoneNumber.substring(1);        
        }
    	if(phoneNumber.startsWith("00") && phoneNumber.length()>11) // phone number has prefix 00
    	{
    		for(int i = 0;i<COUNTRIES.length;i++)
    		{
    			String temp="+"+phoneNumber.substring(2);
    			if (checkValidFixed(FIXED_COUNTRIES[i][0], temp))
    				return temp;
    		}
    	}
    	if(phoneNumber.startsWith("011") && phoneNumber.length()>11) // phone number has prefix 011
		{
    		for(int i = 0;i<FIXED_COUNTRIES.length;i++)
    		{
    			String temp="+"+phoneNumber.substring(3);
    			if (checkValidFixed(FIXED_COUNTRIES[i][0], temp))
    				return temp;
    		}
		}
    	if(phoneNumber.startsWith("00") && (networkCountryIso.equals("tw") || networkCountryIso.equals("id")) && phoneNumber.length()>13) // phone number has prefix 00X
    	{
    		for(int i = 0;i<FIXED_COUNTRIES.length;i++)
    		{
    			String temp="+"+phoneNumber.substring(3);
    			if (checkValidFixed(FIXED_COUNTRIES[i][0], temp))
    				return temp;
    		}
    	}
    	
		if(forceSIMcardIso)
    	{
    		for(int i = 0;i<FIXED_COUNTRIES.length;i++)
    		{
    			if(simCountryIso.equals(FIXED_COUNTRIES[i][0]))
    			{
    				return dropPrefixZero_Fixed(i, FIXED_COUNTRIES[i][3], filterNumber(simCountryIso,phoneNumber), simCountryIso);
    			}
    		}
    	}
		if((simCountryIso.equals("us") || simCountryIso.equals("ca") || simCountryIso.equals("pr") || simCountryIso.equals("dm")) && phoneNumber.length()==7)//US/CN area code
		{
			if (myPhoneNumber.startsWith("+1"))
			{
				String areaCode=myPhoneNumber.substring(2, 5);
				return "+1" + areaCode + phoneNumber;
			}
		}
		
		for(int i = 0;i<COUNTRIES.length;i++)
		{
			String temp="+"+phoneNumber;
			if (checkValidePhone(COUNTRIES[i][0], temp))
				return temp;
		}
		
		if(simCountryIso.equals(networkCountryIso)) // Judge simCountryIso and networkCountryIso is the same
    	{
			String temp;
    		for(int i = 0;i<FIXED_COUNTRIES.length;i++)
    		{
    			if(simCountryIso.equals(FIXED_COUNTRIES[i][0]))
    			{
    				temp=dropPrefixZero_Fixed(i, FIXED_COUNTRIES[i][3], filterNumber(simCountryIso,phoneNumber), simCountryIso);
    				if (checkValidFixed(networkCountryIso, temp))
    					return temp;
    			}
    		}
    		for(int i = 0;i<FIXED_COUNTRIES.length;i++)
    		{
				temp=dropPrefixZero_Fixed(i, FIXED_COUNTRIES[i][3], phoneNumber, FIXED_COUNTRIES[i][0]);
				if (checkValidFixed(FIXED_COUNTRIES[i][0], temp))
					return temp;
    		}
    	}
    	else // which doesn't equal
    	{
    		String temp;
    		for(int i = 0;i<FIXED_COUNTRIES.length;i++)
    		{
    			if(networkCountryIso.equals(FIXED_COUNTRIES[i][0]))
    			{
    				temp = dropPrefixZero_Fixed(i, FIXED_COUNTRIES[i][3], filterNumber(networkCountryIso, phoneNumber), networkCountryIso);
    				if (checkValidFixed(networkCountryIso, temp))
    					return temp;
    			}
    			if(simCountryIso.equals(FIXED_COUNTRIES[i][0]))
    			{
    				temp = dropPrefixZero_Fixed(i, FIXED_COUNTRIES[i][3], filterNumber(simCountryIso, phoneNumber), simCountryIso);
    				if (checkValidFixed(simCountryIso, temp))
    					return temp;
    			}
    		}
		}
		
		for(int i = 0;i<FIXED_COUNTRIES.length;i++)
		{
			String temp=FIXED_COUNTRIES[i][3]+phoneNumber;
			if (checkValidFixed(FIXED_COUNTRIES[i][0], temp))
				return temp;
		}
		
    	return phoneNumber;
    }
	
    static private String dropPrefixZero(int index, String prefix, String phoneNumber, String iso)
    {
    	//alec handle the case: with country code but no plus
    	try{
	    	if (phoneNumber.length()==Integer.parseInt(COUNTRIES[index][2])-1)
	    	{
	    		if (phoneNumber.startsWith(COUNTRIES[index][3].substring(1)))
	    			return "+"+phoneNumber;
	    	}
    	}catch(Exception e){}
    	
    	//alec remove prefix single '0'
    	if (phoneNumber.startsWith("0") && !phoneNumber.startsWith("00"))
    	{
    		return prefix+phoneNumber.substring(1);
    	}
    	
		return prefix+phoneNumber;
    }
    
    static private String dropPrefixZero_Fixed(int index, String prefix, String phoneNumber, String iso)
    {
    	//alec handle the case: with country code but no plus
    	try{
	    	if (phoneNumber.length()==Integer.parseInt(FIXED_COUNTRIES[index][2])-1)
	    	{
	    		if (phoneNumber.startsWith(FIXED_COUNTRIES[index][3].substring(1)))
	    			return "+"+phoneNumber;
	    	}
    	}catch(Exception e){}
    	
    	//alec remove prefix single '0'
    	if (phoneNumber.startsWith("0") && !phoneNumber.startsWith("00"))
    	{
    		return prefix+phoneNumber.substring(1);
    	}
    	
		return prefix+phoneNumber;
    }
    /**
     * filter mobile:12593 17951, unicom: 17911 10193, telecom: 17901
     * @param phoneNumber
     * @return
     */
    static private String filterNumber(String iso, String phoneNumber)
    {
    	if(iso.equals("cn") && phoneNumber.length()==16)
    	{
    		String[] filter = {"125931" , "179511" ,"179111", "101931", "179011"};
    		for (int i = 0; i<filter.length; i++)
    		{
    			if(phoneNumber.startsWith(filter[i]) && phoneNumber.length()==16)
    			{
    				try{
    					phoneNumber=phoneNumber.substring(5);
    				}catch(Exception e){}
    				
    				return phoneNumber;
    			}
    		}
    	}
    	return phoneNumber;
    }
    public boolean isInternationalCall(String iso,String phoneNumber)
    {
    	if(iso==null || phoneNumber==null || (phoneNumber.startsWith("+") && phoneNumber.startsWith("00")) || phoneNumber.length()<10)
    	{
			return false;
    	}
    	if(phoneNumber.startsWith("00")){
    		phoneNumber = "+"+phoneNumber.substring(2);
    	}
    	
    	String[] prefx = {phoneNumber.substring(0, 2),phoneNumber.substring(0, 3),phoneNumber.substring(0, 4)};
    	String myPrefx = null;
    	String frdPrefx = null;
    	for(int i = 0;i<COUNTRIES.length;i++){
    		if(COUNTRIES[i][3].equals(prefx[0]) || COUNTRIES[i][3].equals(prefx[1]) || COUNTRIES[i][3].equals(prefx[2])){
    			frdPrefx = COUNTRIES[i][3];
    		}
    		if(iso.equals(COUNTRIES[i][0])){
    			myPrefx = COUNTRIES[i][3];
    		}
    	}
    	if(frdPrefx==null || myPrefx==null){
    		return false;
    	}else if(frdPrefx.equals(myPrefx)){
    		return false;
    	}else if(!frdPrefx.equals(myPrefx)){
    		return true;
    	}
    	return false;
    }
    
    //alec
    public static String getCountryNameByNumber(Context context, String globalNumber)
    {
    	try{
	    	String [] array = context.getResources().getStringArray(R.array.MyTelephony_CountryName);
	    	for (int i = 0; i<array.length; i++)
			{
	    		String[] startStr = COUNTRIES[i][1].split("\\|");
				for(int j = 0;j<startStr.length;j++)
				{
					if(globalNumber.startsWith(startStr[j]))
						return array[i];
				}
			}
	    	
	    	for (int i = 0; i<array.length; i++)
			{
	    		String[] startStr = FIXED_COUNTRIES[i][1].split("\\|");
				for(int j = 0;j<startStr.length;j++)
				{
					if(globalNumber.startsWith(startStr[j]))
						return array[i];
				}
			}
    	}catch(Exception e){}
    	return " ";
    }
    
    //alec
//    public static int getCountryIndexByNumber(String number)
    public static int getCountryIndexByNumber(String number, int index)  //tml*** country iso fix
    {
    	if (index == 1 || index == 3) {
        	try {
        		for (int i = 0; i<COUNTRIES.length; i++)
    			{
//    	    		String[] startStr = COUNTRIES[i][1].split("\\|");
    	    		String[] startStr = COUNTRIES[i][index].split("\\|");  //tml*** country iso fix
    				for(int j = 0;j<startStr.length;j++)
    				{
    					if (number.startsWith(startStr[j])) {
//    						Log.d("countryIndexByNum1 " + number + " " + startStr[j] + " " + i);
    						return i;
    					}
//    					else if (i == 1) {
//    						Log.d("1iso" + i + " " + number + " " + startStr[j]);
//    					}
    				}
    			}
        		
    	    	for (int i = 0; i<FIXED_COUNTRIES.length; i++)
    			{
//    	    		String[] startStr = FIXED_COUNTRIES[i][1].split("\\|");
    	    		String[] startStr = FIXED_COUNTRIES[i][index].split("\\|");  //tml*** country iso fix
    				for(int j = 0;j<startStr.length;j++)
    				{
    					if (number.startsWith(startStr[j])) {
//    						Log.d("countryIndexByNum2 " + number + " " + startStr[j] + " " + i);
    						return i;
    					}
//    					else if (i == 1) {
//    						Log.d("2iso" + i + " " + number + " " + startStr[j]);
//    					}
    				}
    			}
        	} catch (Exception e) {}
    	} else {
    		return 0;
    	}
    	return -1;
    }
    
    //alec
    public static String getCountryIsoByIndex(int index)
    {
    	return COUNTRIES[index][0];
    }
    
    //alec
    public static int getCountryIndexByIso(String iso)
    {
    	for(int j = 0;j<COUNTRIES.length;j++)
		{
			if(iso.equals(COUNTRIES[j][0]))
				return j;
		}
    	return 0;
    }
    
    //alec
    public static String getCountryNameByIso(String iso, Context context)
    {
    	try{
    		String [] array = context.getResources().getStringArray(R.array.MyTelephony_CountryName);
			for(int j = 0;j<COUNTRIES.length;j++)
			{
				if(iso.equals(COUNTRIES[j][0]))
					return array[j];
			}
    	}catch(Exception e){}
    	return " ";
    }
    
    //alec
    public static String getCountryNameByIndex(int index, Context context)
    {
    	try{
    		String [] array = context.getResources().getStringArray(R.array.MyTelephony_CountryName);
    		return array[index];
    	}catch(Exception e){}
    	return " ";
    }
    
    //alec
    public static String getCountryPrefixByIso(String iso)
    {
    	for(int j = 0;j<COUNTRIES.length;j++)
		{
    		if(iso.equals(COUNTRIES[j][0]))
			return COUNTRIES[j][3];
		}
    	return " ";
    }
    
    //alec
    public static String doHyphenation(String iso, String number)
    {
    	StringBuffer global=new StringBuffer(number);
    	for (int i = 0; i<COUNTRIES.length; i++)
		{
    		if(iso.equals(COUNTRIES[i][0]))
			{
    			if (number.startsWith(COUNTRIES[i][3]))
    			{
    				int p=COUNTRIES[i][3].length();
    				global.insert(p, '-');
    				global.insert(p+4, '-');
    				global.insert(p+8, '-');
    				break;
    			}
			}
		}
    	return global.toString();
    }
}
