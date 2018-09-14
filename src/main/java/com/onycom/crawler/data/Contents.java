package com.onycom.crawler.data;

import java.util.ArrayList;
import java.util.List;

/**
 * 콘텐츠의 레코드 하나를 담는 객체. 현재는 컬럼의 순서가 없다. 이를 고려하는 로직이 필요할 듯.
 * */
public class Contents {
	String mName;
	List<KeyValue> mData;
	
	public Contents(String name, int len){
		mName = name;
		mData = new ArrayList<KeyValue>(len);
	}
	
	public String getName(){
		return mName;
	}
	
	public void add(String key, String value){
		mData.add(KeyValue.Create(key, value));
	}
	
	public void add(int idx, String key, String value){
		mData.add(KeyValue.Create(key, value));
	}
	
	public List<KeyValue> getData(){
		return mData;
	}
	
	public int size(){
		return mData.size();
	}
	
	public KeyValue get(int idx){
		return mData.get(idx);
	}
}
