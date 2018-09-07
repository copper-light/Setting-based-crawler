package com.onycom.crawler.data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * 설정 정보를 담는 객체. json 형태의 설정파일을 파싱하여 객체화함
 * */
public class Config {
	public static final String SAVE_TYPE_DB = "DB";
	public static final String SAVE_TYPE_CSV = "CSV";
	public static final String SAVE_TYPE_JSON = "JSON";
	public static final String SAVE_TYPE_XML = "XML";
	
	public static final String CRAWLING_TYPE_STATIC = "STATIC";
	public static final String CRAWLING_TYPE_SCENARIO_STATIC = "SCENARIO_STATIC"; 
	public static final String CRAWLING_TYPE_SCENARIO_DYNAMIC = "SCENARIO_DYNAMIC";
	
	public static final String DEAULT_OUTPUT_FILE_PATH = "/output";
	
	public static final String COLLECT_ITEMTYPE_URL = "URL";
	public static final String COLLECT_ITEMTYPE_DATETIME = "DATETIME";
	public static final String COLLECT_ITEMTYPE_TAG = "TAG";
	
	public URLInfo mSeedInfo;
	
	public boolean IGNORE_ROBOTS = false;
	
	public String CRAWLING_TYPE = CRAWLING_TYPE_STATIC;
	public int CRAWLING_DELAY = -1; // ms
	public int CRAWLING_MAX_COUNT = -1;
	public int CRAWLING_MAX_DEPTH = -1;
	public boolean CRAWLING_UPPER_SEARCH = false;
	
	public String OUTPUT_SAVE_TYPE = SAVE_TYPE_CSV; // json, xml, db;
	public String OUTPUT_FILE_PATH = DEAULT_OUTPUT_FILE_PATH;
	public String OUTPUT_DB_PATH = "";
	public String OUTPUT_DB_ID = "";
	public String OUTPUT_DB_PW = "";
	
	List<Duplicate> mFilterDuplicate; // URL중복 체크 조건 
	List<String> mFilterAllow;     // 허용된 URL(또는 패턴)
	List<String> mFilterDisallow;  // 접속하지 않는 URL(또는 패턴)
	List<String> mLeafURL;         // 크롤링 마지막 URL(또는 패턴) : 해당 페이지의 링크는 수집하지 않는다
	
	List<Collect> mCollects;       // 수집할 콘텐츠
	Map<Integer, Scenario> mScenarios;
	
	Map<String, Robots> mRobots;
	
	public void setConfig(String config){
		JSONObject root = new JSONObject(config);
		
		IGNORE_ROBOTS = root.getBoolean("ignore_robots");
		
		CRAWLING_MAX_DEPTH = root.getInt("crawling_max_depth");
		CRAWLING_DELAY = root.getInt("crawling_delay");
		CRAWLING_MAX_COUNT = root.getInt("crawling_max_count");
		CRAWLING_TYPE = root.getString("crawling_type");
		CRAWLING_UPPER_SEARCH = root.getBoolean("crawling_upper_search");
		
		OUTPUT_SAVE_TYPE = root.getString("contents_save_type");
		OUTPUT_FILE_PATH = root.getString("output_file_path");
		OUTPUT_DB_ID = root.getString("output_db_id");
		OUTPUT_DB_PW = root.getString("output_db_pw");
		OUTPUT_DB_PATH = root.getString("output_db_path");
		
		mFilterDuplicate = new ArrayList<Duplicate>();
		mFilterAllow = new ArrayList<String>();
		mFilterDisallow = new ArrayList<String>();
		
		mLeafURL = new ArrayList<String>();
		mCollects = new ArrayList<Collect>();
		mScenarios = new HashMap<Integer, Scenario>();
		
		mRobots = new HashMap<String, Robots>();
		
		JSONObject object;
		JSONArray array;
		
		/**
		 * SEED 파싱
		 * */
		try{
			object = root.getJSONObject("seed");
			mSeedInfo = new URLInfo(object.getString("url"));
			mSeedInfo.setDepth(0); // 루트로 인식
			if(object.isNull("type")) {
				mSeedInfo.setContentType(URLInfo.GET);
			}else{
				String type = object.getString("type");
				if(type.contentEquals("POST")){
					mSeedInfo.setContentType(URLInfo.POST);
				}else{
					mSeedInfo.setContentType(URLInfo.GET);
				}
			}
			if(!object.isNull("data")){
				array = object.getJSONArray("data");
				String key, value;
				for(int i = 0 ; i < array.length() ; i++){
					object = array.getJSONObject(i);
					key = object.getString("key");
					value = object.getString("value");
					mSeedInfo.setData(key, value);
				}
			}
		}catch(JSONException e){ // seed 파싱을 못할 경우 크롤링을 아예 진행하지 않도록
			e.printStackTrace();
			return ;
		}
		
		/**
		 * filter 파싱
		 * */
		
		Duplicate duplicate;
		
		try{
			object = root.getJSONObject("filter");
			try {
				array = object.getJSONArray("duplicate");
				for (Object url : array) {
					if (url != null) {
						duplicate = new Duplicate((JSONArray) url);
						mFilterDuplicate.add(duplicate);
					}
				}
			} catch (JSONException e) { e.printStackTrace(); mFilterDuplicate.clear(); }

			try {
				array = object.getJSONArray("allow");
				for (Object url : array) {
					if (url != null) {
						mFilterAllow.add((String) url);
					}
				}
			} catch (JSONException e) { e.printStackTrace(); mFilterAllow.clear(); }

			try {
				array = object.getJSONArray("disallow");
				for (Object url : array) {
					if (url != null) {
						mFilterDisallow.add((String) url);
					}
				}
			} catch (JSONException e) { e.printStackTrace(); mFilterDisallow.clear();}

			try {
				array = object.getJSONArray("leaf_url");
				for (Object url : array) {
					if (url != null) {
						mLeafURL.add((String) url);
					}
				}
			} catch (JSONException e) { e.printStackTrace(); mLeafURL.clear();}
		} catch (JSONException e) { e.printStackTrace(); }

		JSONArray aryItems,aryRegex;
		JSONObject item;
		Collect collect;
		int len_i, len_j;
		String[] regexs;
		try{
			array = root.getJSONArray("collect");
			len_i = array.length();
			for(int i = 0 ; i < len_i ; i ++){
				object = array.getJSONObject(i);
				collect = new Collect(object.isNull("url")? "": object.getString("url"), 
										  object.getString("name"));
				if(!object.isNull("depth")) collect.setDepth(object.getInt("depth"));
				aryItems = object.getJSONArray("items");
				len_j = aryItems.length();
				for(int j = 0 ; j < len_j ; j++ ){
					item = aryItems.getJSONObject(j);
					regexs = null;
					if(!item.isNull("allow_data_regex")){
						aryRegex = item.getJSONArray("allow_data_regex");
						regexs = new String[aryRegex.length()];
						aryRegex.toList().toArray(regexs);
					}
					collect.add(item.isNull("type")? "": item.getString("type"),
								item.isNull("selector")? "": item.getString("selector"),
								item.isNull("tag_type")? "": item.getString("tag_type"), 
								item.isNull("attr_name")? "": item.getString("attr_name"), 
								item.isNull("data_type")? "": item.getString("data_type"), 
								item.isNull("data_name")? "": item.getString("data_name"),
								regexs);
				}
				mCollects.add(collect);
			}
		} catch (JSONException e) { mCollects.clear(); e.printStackTrace(); }
		
		try{
			array = root.getJSONArray("scenario");
			len_i = array.length();
			int depth;
			JSONArray aryScenario, aryLoadCheckSelector;
			JSONObject scenObject;
			Scenario scenario;
			for(int i = 0 ; i < len_i ; i++){
				object = array.getJSONObject(i);
				aryScenario = object.getJSONArray("actions");
				depth = object.getInt("depth");
				len_j = aryScenario.length();
				scenario = new Scenario(depth, len_j);
				mScenarios.put(depth, scenario);
				if(!object.isNull("check_load_selector")){
					aryLoadCheckSelector = object.getJSONArray("check_load_selector");
					for(Object selector: aryLoadCheckSelector){
						System.out.println(String.valueOf(selector));
						scenario.addLoadCheckSelector(String.valueOf(selector));
					}
				}
				for(int j = 0 ; j < len_j ; j++){
					scenObject = aryScenario.getJSONObject(j);
					System.out.println(scenObject.getString("selector"));
					scenario.add(scenObject.isNull("depth")? -1: scenObject.getInt("depth"), 
								 scenObject.getString("selector"),
								 scenObject.isNull("target")? null: scenObject.getString("target"),
								 scenObject.isNull("type")? null: scenObject.getString("type"),
								 scenObject.isNull("value")? null: scenObject.getString("value"));
				}
				for(int j = 0 ; j < scenario.getSize() ; j++){
					scenario.getAction(j);
				}
			}
		} catch (JSONException e){ e.printStackTrace(); mScenarios.clear(); }
	}
	
	public URLInfo getSeedInfo(){
		return mSeedInfo;
	}
	
	public Map<String, Robots> getRobots(){
		return mRobots;
	}
	
	public Map<Integer, Scenario> getScenarios(){
		return mScenarios;
	}
	
	public List<Duplicate> getFilterDuplicate(){
		return mFilterDuplicate;
	}
	
	public List<String> getFilterAllow(){
		return mFilterAllow;     // 허용된 URL(또는 패턴)
	}
	
	public List<String> getFilterDisallow(){
		return mFilterDisallow;
	}
	
	public List<String> getLeafURL(){
		return mLeafURL;// 크롤링 마지막 URL(또는 패턴) : 해당 페이지의 링크는 수집하지 않는다
	}
	
	public List<Collect> getCollects(){
		return mCollects;// 수집할 콘텐츠
	}
}
