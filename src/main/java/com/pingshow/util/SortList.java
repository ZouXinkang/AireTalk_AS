package com.pingshow.util;

import java.io.UnsupportedEncodingException;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

public class SortList {

	public static int getGBCode(char c) {
		byte[] bytes = null;
		try {
			bytes = new StringBuffer().append(c).toString().getBytes("gbk");
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		if (bytes.length == 1) {
			return bytes[0];
		}
		int a = bytes[0] - 0xA0 + 256;
		int b = bytes[1] - 0xA0 + 256;

		return a * 100 + b;
	}

	public static List<String> sortList(List<String> strList) {
		Collections.sort(strList, new Comparator<String>() {
			public int compare(String o1, String o2) {
				char[] a1 = o1.toCharArray();
				char[] a2 = o2.toCharArray();

				for (int i = 0; i < a1.length && i < a2.length; i++) {
					int c1 = getGBCode(a1[i]);
					int c2 = getGBCode(a2[i]);

					if (c1 == c2)
						continue;

					return c1 - c2;
				}

				if (a1.length == a2.length) {
					return 0;
				}

				return a1.length - a2.length;
			}
		});
		return strList;

	}
	public static List<Map<String, String>> sortMapList(List<Map<String, String>> strList) {
		Collections.sort(strList, new Comparator<Map<String, String>>() {
			@Override
			public int compare(Map<String, String> lhs, Map<String, String> rhs) {
				
				char[] a1 ; 
				char[] a2 ;
				
				if(lhs.get("containChinese").equals("yes"))
					
					a1 = lhs.get("change").toLowerCase().toCharArray();	
				
				else
					
					a1 = lhs.get("displayName").toLowerCase().toCharArray();
				
				if(rhs.get("containChinese").equals("yes"))
					
					a2 = rhs.get("change").toLowerCase().toCharArray();
					
				else
					
					a2 = rhs.get("displayName").toLowerCase().toCharArray();

				for (int i = 0; i < a1.length && i < a2.length; i++) {
					int c1 = getGBCode(a1[i]);
					int c2 = getGBCode(a2[i]);

					if (c1 == c2)
						continue;

					return c1 - c2;
				}

				if (a1.length == a2.length) {
					return 0;
				}

				return a1.length - a2.length;
			}
		});
		return strList;
	}

}
