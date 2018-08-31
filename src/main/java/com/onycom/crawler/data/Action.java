package com.onycom.crawler.data;

public class Action{
	public static String TARGET_BLANK = "blank";
	public static String TARGET_SELF = "self";
	public static String TYPE_CLICK = "click";
	public static String TYPE_INPUT = "input";
	public static String TYPE_SCROLL_DOWN = "scroll_down";
	public static String TYPE_SCROLL_UP = "scroll_up"; 
	
	int depth;
	String cssSelector;
	String target;
	String type;
	
	public Action(int depth, String cssSelector, String target, String type){
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
}
