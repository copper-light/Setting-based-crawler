package com.onycom.crawler.data;

public class Action{
	public static String TARGET_BLANK = "blank";
	public static String TARGET_SELF = "self";
	public static String TYPE_CLICK = "click";
	public static String TYPE_INPUT = "input";
	public static String TYPE_VERTICAL_SCROLL = "vertical_scroll";
	public static String TYPE_SELECT = "select";
	public static String TYPE_JAVASCRIPT = "javascript"; 
	
	/**
	 * depth 에 따라 시나리오가 흘러감
	 * 무한루프 주의 필요
	 * 스크롤 같은 기능에서 0depth 페이지에서 스크롤 action에 depth 0 줄경우
	 * 무한 루프에 빠짐
	 * 따라서 scroll 처럼 페이지에 변화가 없는 경우 설정에서 반드시 -1 값을 넣거나 생략해야함 
	 * */
	int depth;
	String cssSelector;
	String target;
	String type;
	String value;
	
	public Action(int depth, String cssSelector, String target, String type, String value){
		this.depth = depth;
		this.cssSelector = cssSelector;
		if(target == null){
			this.target = TARGET_SELF;
		}else{
			this.target = target;
		}
		if(type == null){
			this.type = TYPE_CLICK;
		}else{
			this.type = type;
		}
		this.value = value;
	}
	
	public void setValue(String value){
		this.value = value;
	}
	
	public String getTarget(){
		return target;
	}
	
	public String getType(){
		return type;
	}
	
	public String getSelector() {
		return cssSelector;
	}
	
	public int getDepth(){
		return depth;
	}
	
	public String getValue(){
		return value;
	}
}
