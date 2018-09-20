package com.onycom.crawler.data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 수집할 콘텐츠 정보를 담고 있는 객체. Config 객체에서 사용함
 * */
public class CollectRecode {
	//Map<String, >
	String mUrl;        /* 데이터 파싱할 URL 표준 정규식 */
	String mName;       /* 데이터 셋의 명칭 (데이터셋이 저장될 이름) */
	String mRecodeSelector; /* 하나의 레코드를 식별할 수 있는 최고 작은 단위의 Element */
	
	int mDepth;
	
	
	List<Column> mCols;  /* cols 목록 */
	
	public CollectRecode(String url, String name){
		mUrl = url;
		mName = name;
		mCols = new ArrayList<Column>();
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
	
	public void setRecodeSelector(String recodeSelector){
		mRecodeSelector = recodeSelector;
	}
	
	public String getRecodeSelector(){
		return mRecodeSelector;
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
	
	public void add(Column.Element[] elements, String data_type, String data_name, String[] regex){
		Column Column = new Column("", elements, data_type, data_name);
		Column.setRegexFilter(regex);
		mCols.add(Column);
	}
	
	public void add(String type, Column.Element[] elements, String data_type, String data_name, String[] regex){
		Column Column = new Column(type, elements, data_type, data_name);
		Column.setRegexFilter(regex);
		mCols.add(Column);
	}
	
	public void add(boolean key, String type, Column.Element[] elements, String data_type, String data_name, String[] regex){
		Column Column = new Column(type, elements, data_type, data_name);
		Column.setRegexFilter(regex);
		Column.setKey(key);
		mCols.add(Column);
	}
	
	public Column.Element[] getElements(int i){
		if (i < mCols.size()){
			return mCols.get(i).getElements();
		}else{
			return null;
		}
	}
	
	public Column getColumn(int i){
		if (i < mCols.size()){
			return mCols.get(i);
		}else{
			return null;
		}
	}
	
	public List<Column> getColumns(){
		return mCols;
	}
	
	public static class Column {
		String type;           /* 크롤링 정보 저장 타입 */
		Element[] elements;       /* 태그 selector 패턴 */
		String data_type;      /* 저장될 데이터 타입 */
		String data_name;      /* 저장될 데이터 이름 */
		boolean key = false;
		String[] regex_filter;
		
		public Column(String type, Element[] elments, String data_type, String data_name){
			this.elements = elments;
			this.type = type;
			this.data_type = data_type;
			this.data_name = data_name;
		}
		
		public void setKey(boolean key){
			this.key = key;
		}
		
		public boolean isKey(){
			return key;
		}
		
		public String getType(){
			return this.type;
		}
		
		public Element[] getElements(){
			return this.elements;
		}
		
		public String getDataType(){
			return this.data_type;
		}
		
		public String getDataName(){
			return this.data_name;
		}
		
		public void setRegexFilter(String[] regex){
			regex_filter = regex;
		}
		
		public String[] getRegexFilter(){
			return regex_filter;
		}
		
		public static class Element {
			String selector;
			String type;
			String attr_name;
			boolean from_root = false;
			
			public Element(String selector, String type, boolean fromRoot){
				this.selector = selector;
				if(type.indexOf('=') == -1){
					this.type = type;
					this.attr_name = null;
				}else{
					String[] temp = type.split("=");
					this.type = temp[0].trim();
					this.attr_name = temp[1].trim();
				}
				this.from_root = fromRoot;
			}
			
			public String getSelector(){
				return this.selector;
			}
			
			public String getType(){
				return this.type;
			}
			
			public String getAttrName(){
				return this.attr_name;
			}
			
			public boolean isFromRoot(){
				return from_root;
			}
		}
	}
}
