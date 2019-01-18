package com.onycom.crawler.data;

public class Action{
//	public static String TARGET_BLANK = "blank";
//	public static String TARGET_SELF = "self";
	public static String TYPE_URL = "url";
	public static String TYPE_CLICK = "click";
	public static String TYPE_INPUT = "input";
	public static String TYPE_VERTICAL_SCROLL = "vertical_scroll";
	public static String TYPE_SELECT = "select";
	public static String TYPE_JAVASCRIPT = "javascript"; 
	public static String TYPE_SWITCH_WINDOW = "switch_window";
	public static String TYPE_CLOSE_WINDOW = "close_window";
	public static String TYPE_PARSE_CONTENTS = "parse_contents";
	public static String TYPE_BACKWORD_WINDOW = "backward_window";
	public static String TYPE_FORWORD_WINDOW = "forword_window";
	public static String TYPE_REFRESH_WINDOW = "refresh_window";
	public static String TYPE_CLOSE_POPUP = "close_popup";
	public static String TYPE_REMOVE_ELEMENTS = "remove_elements";
	public static String TYPE_SLEEP = "sleep";

	/**
	 * depth 에 따라 시나리오가 흘러감
	 * 무한루프 주의 필요
	 * 스크롤 같은 기능에서 0depth 페이지에서 스크롤 action에 depth 0 줄경우
	 * 무한 루프에 빠짐
	 * 따라서 scroll 처럼 페이지에 변화가 없는 경우 설정에서 반드시 -1 값을 넣거나 생략해야함 
	 * */
	int target_depth;
	int action_idx;
	String cssSelector;
	String emptySelector;
	String type;
	String value;
	
	public Action(int target_depth, String cssSelector, String emptySelector, String type, String value){
		this.target_depth = target_depth;
		this.cssSelector = cssSelector;
		if(type == null){
			this.type = TYPE_URL;
		}else{
			this.type = type;
		}
		this.value = value;
		this.emptySelector = emptySelector;
	}
	
	public void setEmptySelector(String selector){
		emptySelector = selector;
	}
	
	public String getEmptySelector(){
		return emptySelector;
	}
	
	public int getTargetDepth(){
		return target_depth;
	}
	
	public void setValue(String value){
		this.value = value;
	}
	
	public String getType(){
		return type;
	}
	
	public String getSelector() {
		return cssSelector;
	}
	
	public String getValue(){
		return value;
	}
}
