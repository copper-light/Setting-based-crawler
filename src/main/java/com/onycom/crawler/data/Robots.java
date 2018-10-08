package com.onycom.crawler.data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.onycom.crawler.data.Robots.User.Allow;

public class Robots {
	// "User-agent: "
	// "Allow: "
	// "Disallow: "
	String RootURL;
	
	Map<String, User> mMapUsers;
	
	public Robots(String page){ 
		if(page == null) return;
		mMapUsers = new HashMap<String, User>();
	}
	
	public void add(String userAgent, String path, boolean isAllow){
		if(path == null) return;
		User user = mMapUsers.get(userAgent);
		if(user == null){
			user = new User(userAgent);
			mMapUsers.put(userAgent, user);
		}
		user.add(path, isAllow);
	}
	
	public void add(String userAgent, int delay){
		if (userAgent == null) userAgent = "*"; 
		User user = mMapUsers.get(userAgent);
		if(user == null){
			user = new User(userAgent);
			mMapUsers.put(userAgent, user);
		}
		user.delay = delay;
	}
	
	public boolean isAllow(String userAgent, String path){
		if(userAgent == null) userAgent = "*";
		User user = mMapUsers.get(userAgent);
		if(user == null){
			user = mMapUsers.get("*");
		}
		if(user == null){
			return true;
		}
		
		List<User.Allow> list = user.getAllowList();
		String key;
		String temp;
		boolean ret = true;
		int pre_grade = 0;
		int key_grade = 0;
		int size = list.size();
		User.Allow allow;
		for(int i = 0 ; i < size ; i ++){
			allow = list.get(i);
			temp = allow.path.replace("/*", "/.*");
			if(path.matches("^(" + temp + ").*$")){
				// 문자열이 긴것을 우선순위로 하며,
				// 경로의 댑스가 깊을 것으로 추측한다.
				// 제일 정확한것은 "/" 의 개수로 체크하는게 나을듯
				key_grade = allow.path.length();
				if(pre_grade < key_grade){
					ret = allow.isAllow;
					pre_grade = key_grade;
				}
				System.err.println(path + " @ " + allow.path + " @ " + allow.isAllow);
			} 
			//path = String.valueOf(user.allows.get(key));
		}
		
		return ret;
	}
	
	class User{
		String userAgent;
		int delay;
		ArrayList<Allow> allows;
		
		public User(String name){
			this.userAgent = name;
			this.delay = 0;
			this.allows = new ArrayList<Allow>();
		}
		
		public void add(String path , boolean isAllow){
			Allow a = new Allow();
			a.isAllow = isAllow;
			a.path = path;
			this.allows.add(a);
		}
		
		public Allow getAllow(int idx){
			return this.allows.get(idx);
		}
		
		public ArrayList<Allow> getAllowList(){
			return allows;
		}
		
		class Allow{
			String path;
			boolean isAllow;
		}
	}
}
