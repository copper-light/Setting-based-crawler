package com.onycom.common;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.concurrent.SynchronousQueue;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.codec.Charsets;
import org.jsoup.helper.StringUtil;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import com.onycom.crawler.data.Work;

/**
 * 유틸성 메서드 모음
 * */
public class Util  {
	
	static String[] SPLIT_URL_TOKEN = {"/", "?" ,"#"};
	public static String[] SplitDomainAndSubURL(Work urlInfo, String target){
		String[] ret = new String[2];
		int idx;
		if(target.matches("^(http).*$")){
			for(String t : SPLIT_URL_TOKEN){
				idx = target.indexOf(t, 8);
				if(idx != -1){
					ret[0] = target.substring(0, idx);
					ret[1] = target.substring(idx);
					break;
				}
			}
			if(ret[0] == null){
				ret[0] = target;
				ret[1] = "/";
			}
		}else{
			//String parentUrl = urlInfo.getURL();
			String domainUrl = urlInfo.getDomainURL();
			String parentSubUrl = urlInfo.getSubURL();
			
			// ./* 일때 파싱 처리
			if(target.matches("^(\\./).*$")){
				idx = parentSubUrl.lastIndexOf("/");
				if(idx != -1){
					ret[0] = domainUrl;
					ret[1] = parentSubUrl.substring(0, idx) + target.substring(1);
				}else{
					ret[0] = domainUrl;
					ret[1] = target.substring(1);
				}
				
			// ../* 일때 파싱 처리
			}else if(target.matches("^(\\.\\./).*$")){
				idx = parentSubUrl.lastIndexOf("/");
				if(idx != -1){
					idx = parentSubUrl.substring(0, idx).lastIndexOf("/");
				}
				
				if(idx != -1){
					ret[0] = domainUrl;
					ret[1] = parentSubUrl.substring(0, idx) + target.substring(2);
				}else{
					ret[0] = domainUrl;
					ret[1] = target.substring(1);
				}
				ret[0] = domainUrl;
				ret[1] = target.substring(2);
			}else{
				ret[0] = domainUrl;
				ret[1] = target;
			}
		}
		return ret;
	}
	
	public static CharSequence GetConfigFile(String path){
		File file = new File(path);
		char[] ch = new char[(int) file.length()];
		StringBuilder sb = new StringBuilder();
		try {
			BufferedReader br = new BufferedReader(new FileReader(file));
			br.read(ch);
			sb.append(ch);
		} catch (IOException e) {
			return null;
		}
		//System.out.println(sb.toString());
		return sb.toString();
	}
	
	public static String ConvertForRegex(String str){
		str = str.replace("?", "\\?");
		str = str.replace(".", "\\.");
		return str;
	}
	
	private static final Charset UTF_8 = Charset.forName("UTF-8");
	public static String EncodingUTF8(String str){
		return new String(str.getBytes(), UTF_8);
	}
	
	public static String Remove4ByteEmoji(String str){
		Pattern ptEmoji = Pattern.compile("[\\uD83C-\\uDBFF\\uDC00-\\uDFFF]+");
		Matcher m = ptEmoji.matcher(str);
		return m.replaceAll(" ");
	}
	
	public static float CalcExpiredTime(Date startDate){
		return ((new Date().getTime() - startDate.getTime())/ 1000f); 
	}
	
	public static String GetCssSelector(Element e){
		String tagName = e.tagName();
		if(tagName.equalsIgnoreCase("html")) return tagName;
		String selector = ":nth-child("+ (e.elementSiblingIndex() + 1) +")";
		if(e.parent() == null || (e.parent() instanceof Document)){
			return null;
		}else{
			String parent = GetCssSelector(e.parent());
			if(parent != null){
				return parent + " > " + selector;
			}else{
				return selector;
			}
		}
		//e.get
		
		
//		if (id().length() > 0)
//            return "#" + id();
//
//        // Translate HTML namespace ns:tag to CSS namespace syntax ns|tag
//        String tagName = tagName().replace(':', '|');
//        StringBuilder selector = new StringBuilder(tagName);
//        String classes = StringUtil.join(classNames(), ".");
//        if (classes.length() > 0)
//            selector.append('.').append(classes);
//
//        if (parent() == null || parent() instanceof Document) // don't add Document to selector, as will always have a html node
//            return selector.toString();
//
//        selector.insert(0, " > ");
//        if (parent().select(selector.toString()).size() > 1)
//            selector.append(String.format(
//                ":nth-child(%d)", elementSiblingIndex() + 1));
//
//        return parent().cssSelector() + selector.toString();
	}
}
