package com.onycom.crawler.data;

import java.util.Map;

import org.json.JSONArray;

import com.onycom.common.Util;

/**
 * 설정파일에서 파싱된 중복 필터링 정보를 담는 객체
 * */
public class Duplicate {
	String mRegex;
	String[] mKeys;
	
	public Duplicate(JSONArray ja){
		int len = ja.length();
		if(len - 1 > 0){
			mKeys = new String[len-1];
		}
		String value;
		for(int i = 0 ; i < len ; i ++){
			value = ja.getString(i);
			if(i > 0){
				mKeys[i-1] = value;
			}else{ // i == 0
				mRegex = value;
			}
		}
	}
	
	public String regex(Work info){
		Map<String,String> data = info.getData();
		String ret = null;
		String value;
		ret = mRegex.replace("<URL>", Util.ConvertForRegex(info.getURL()));
		if(mKeys == null) return ret;
		for(int i = 0 ; i < mKeys.length ; i++){
			value = data.get(mKeys[i]);
			if(value != null){
				ret = ret.replace("<"+i+">", value);
			}else{
				ret = null;
				break;
			}
		}
		return ret;
	}
}
