package com.onycom.crawler.data;

import java.util.ArrayList;
import java.util.List;

/**
 * 콘텐츠의 레코드 하나를 담는 객체. 현재는 컬럼의 순서가 없다. 이를 고려하는 로직이 필요할 듯.
 * */
public class Contents {
	String mName;
	KeyValue[] mData;
	int mIdx;
	
	public Contents(String name, int len){
		mName = name;
		mData = new KeyValue[len];
		mIdx = 0;
	}
	
	public String getName(){
		return mName;
	}
	
	public void add(String key, String value){
		mData[mIdx++] = KeyValue.Create(key, value);
	}
	
	public void add(int idx, String key, String value){
		mData[idx] = KeyValue.Create(key, value);
	}
	
	public KeyValue[] getData(){
		return mData;
	}
	
	public int size(){
		return mData.length;
	}
	
	public KeyValue get(int idx){
		return mData[idx];
	}
}
