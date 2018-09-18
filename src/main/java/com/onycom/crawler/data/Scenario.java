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
	
	public void add(int target_depth, String cssSelector){
		add(target_depth, cssSelector, null, null);
	}
	
	public void add(int target_depth, String cssSelector, String action, String value){
		mAryAction.add(new Action(target_depth, cssSelector, action, value));
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