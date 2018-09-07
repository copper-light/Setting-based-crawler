package com.onycom.crawler.data;

import java.util.ArrayList;
import java.util.List;

public class Scenario {
	
	int mDepth;
	List<String> mAryLoadCheckSelector;
	List<Action> mAryAction;
	
	public Scenario(int depth, int size) {
		mDepth = depth;
		mAryAction = new ArrayList<Action>(size);
		mAryLoadCheckSelector = new ArrayList<String>();
	}
	
	public void add(int depth, String cssSelector){
		add(depth,cssSelector, null, null, null);
	}
	
	public void add(int depth, String cssSelector, String target, String action, String value){
		mAryAction.add(new Action(depth, cssSelector, target, action, value));
	}
	
	public Action getAction(int idx){
		return mAryAction.get(idx);
	}
	
	public int getSize(){
		return mAryAction.size();
	}
	
	public void addLoadCheckSelector(String selector){
		mAryLoadCheckSelector.add(selector);
	}
	
	public List<String> getLoadCheckSelector(){
		return mAryLoadCheckSelector;
	}
}