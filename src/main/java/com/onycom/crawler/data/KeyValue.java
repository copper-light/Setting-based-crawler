package com.onycom.crawler.data;

public class KeyValue {
	String key;
	String value;
	static KeyValue Create(String key, String value){
		return new KeyValue(key, value);
	}
	
	public KeyValue(String key, String value){
		this.key = key;
		this.value = value;
	}
	
	public String key(){
		return this.key;
	}
	
	public String value(){
		return this.value;
	}
	
	public void key(String key){
		this.key = key;
	}
	
	public void value(String value){
		this.value = value;
	}
}
