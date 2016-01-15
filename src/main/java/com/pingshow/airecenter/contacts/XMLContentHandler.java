package com.pingshow.airecenter.contacts;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.List;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import com.pingshow.airecenter.Log;

public class XMLContentHandler extends DefaultHandler {
	private List<RelatedUserInfo> FriendsInfo = null;
	private RelatedUserInfo currentFriendsInfo;
	private String tagName = null;
	private StringBuilder data=new StringBuilder();
	public List<RelatedUserInfo> getPersons() {
		return FriendsInfo;
	}

	@Override
	public void startDocument() throws SAXException {
		FriendsInfo = new ArrayList<RelatedUserInfo>();
	}

	@Override
	public void characters(char[] ch, int start, int length)
			throws SAXException {
			//Log.e("ch="+new String(ch)+";start="+start+";length="+length);
			data.append(ch, start, length);	
	}

	@Override
	public void startElement(String namespaceURI, String localName,
			String qName, Attributes atts) throws SAXException {
		data.setLength(0);
		if (localName.equals("f")) {
			currentFriendsInfo = new RelatedUserInfo();
			currentFriendsInfo.setIdx(Integer.parseInt(atts.getValue("idx")));
		}
		this.tagName = localName;
	}

	@Override
	public void endElement(String uri, String localName, String name)
			throws SAXException {
		if (tagName != null) {
		if (tagName.equals("id")) {
			this.currentFriendsInfo.setAddress(data.toString());
		} else if (tagName.equals("nn")) {
			try {
				this.currentFriendsInfo.setNickName(URLDecoder.decode(data.toString(), "UTF-8"));
			} catch (UnsupportedEncodingException e) {
			}			
		} else if (tagName.equals("jf")) {
			this.currentFriendsInfo.setjointfriends(Short.parseShort(data.toString()));
		}
		}
		if (localName.equals("f")) {
			FriendsInfo.add(currentFriendsInfo);
			currentFriendsInfo = null;
		}
		this.tagName = null;
	}
}
