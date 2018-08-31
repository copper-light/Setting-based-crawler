package com.onycom.crawler.data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 수집할 콘텐츠 정보를 담고 있는 객체. Config 객체에서 사용함
 * */
public class Collect {
	//Map<String, >
	String mUrl;        /* 데이터 파싱할 URL */
	String mName;       /* 데이터 셋의 명칭 (데이터셋이 저장될 이름) */
	int mDepth;
	List<Item> mItems;  /* cols 목록 */
	
	public Collect(String url, String name){
		mUrl = url;
		mName = name;
		mItems = new ArrayList<Item>();
		mDepth = -1;
	}
	
	public void setDepth(int depth){
		mDepth = depth;
	}
	
	public int getDepth(){
		return mDepth;
	}
	
	public String getUrl(){
		return mUrl;
	}
	
	public void setUrl(String url){
		mUrl = url;
	}
	
	public String getName(){
		return mName;
	}
	
	public void setName(String name){
		mName = name;
	}
	
	public void add(String selector, String tag_type, String attr_name, String data_type, String data_name, String[] regex){
		Item item = new Item("", selector, tag_type, attr_name, data_type, data_name);
		item.setAllowRegex(regex);
		mItems.add(item);
	}
	
	public void add(String type, String selector, String tag_type, String attr_name, String data_type, String data_name, String[] regex){
		Item item = new Item(type, selector, tag_type, attr_name, data_type, data_name);
		item.setAllowRegex(regex);
		mItems.add(item);
	}
	
	public String getSelector(int i){
		if (i < mItems.size()){
			return mItems.get(i).getSelector();
		}else{
			return null;
		}
	}
	
	public Item getItem(int i){
		if (i < mItems.size()){
			return mItems.get(i);
		}else{
			return null;
		}
	}
	
	public List<Item> getItems(){
		return mItems;
	}
	
	public class Item {
		String type;           /* 크롤링 정보 저장 타입 */
		String selector;       /* 태그 selector 패턴 */
		String tag_type;       /* 데이터 추출할 태그 타입 (text, attr) */
		String attr_name;      /* type 이 attr 일때, 속성명 */
		String data_type;      /* 저장될 데이터 타입 */
		String data_name;      /* 저장될 데이터 이름 */
		String[] allow_regex;
		
		public Item(String type, String selector, String tag_type, String attr_name, String data_type, String data_name){
			this.selector = selector;
			this.tag_type = tag_type;
			this.attr_name = attr_name;
			this.type = type;
			this.data_type = data_type;
			this.data_name = data_name;
		}
		
		public String getType(){
			return this.type;
		}
		
		public String getSelector(){
			return this.selector;
		}
		
		public String getDataType(){
			return this.data_type;
		}
		
		public String getDataName(){
			return this.data_name;
		}
		
		public String getTagType(){
			return this.tag_type;
		}
		
		public String getAttrName(){
			return this.attr_name;
		}
		
		public void setAllowRegex(String[] regex){
			allow_regex = regex;
		}
		
		public String[] getAllowRegex(){
			return allow_regex;
		}
	}
}
