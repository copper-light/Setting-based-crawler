package com.onycom.crawler.data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.google.gson.JsonObject;
import com.onycom.common.CrawlerLog;

/**
 * 설정 정보를 담는 객체. json 형태의 설정파일을 파싱하여 객체화함
 */
public class Config {
	static Logger mLogger = CrawlerLog.GetInstanceSysout(Config.class);

	public static final String SAVE_TYPE_DB = "DB";
	public static final String SAVE_TYPE_CSV = "CSV";
	public static final String SAVE_TYPE_JSON = "JSON";
	public static final String SAVE_TYPE_XML = "XML";

	public static final String CRAWLING_TYPE_STATIC = "STATIC";
	public static final String CRAWLING_TYPE_SCENARIO_STATIC = "SCENARIO_STATIC";
	public static final String CRAWLING_TYPE_SCENARIO_DYNAMIC = "SCENARIO_DYNAMIC";

	public static final String DEAULT_OUTPUT_FILE_PATH = "./output";
	public static final String DEAULT_HTML_FILE_PATH = "./html";

	public static final String COLLECT_COLUMN_TYPE_URL = "URL";
	public static final String COLLECT_COLUMN_TYPE_DATETIME = "DATETIME";
	public static final String COLLECT_COLUMN_TYPE_ELEMENT = "ELEMENT	";

	public Work mSeedInfo;

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
	public String CRAWLING_NAME = "";
	
	public boolean SAVE_HTML = false;

	List<Duplicate> mFilterDuplicate; // URL중복 체크 조건
	List<String> mFilterAllow; // 허용된 URL(또는 패턴)
	List<String> mFilterDisallow; // 접속하지 않는 URL(또는 패턴)
	List<String> mLeafURL; // 크롤링 마지막 URL(또는 패턴) : 해당 페이지의 링크는 수집하지 않는다

	List<CollectRecode> mCollects; // 수집할 콘텐츠
	Map<Integer, Scenario> mScenarios;

	Map<String, Robots> mRobots;

	public static final String KEY_CRAWLING_NAME = "name";
	public static final String KEY_IGNORE_ROBOTS = "ignore_robots";
	public static final String KEY_CRAWLING_MAX_DEPTH = "crawling_max_depth";
	public static final String KEY_CRAWLING_DELAY = "crawling_delay";
	public static final String KEY_CRAWLING_MAX_COUNT = "crawling_max_count";
	public static final String KEY_CRAWLING_TYPE = "crawling_type";
	public static final String KEY_CONTENTS_SAVE_TYPE = "contents_save_type";
	public static final String KEY_OUTPUT_FILE_PATH = "output_file_path";
	public static final String KEY_OUTPUT_DB_ID = "output_db_id";
	public static final String KEY_OUTPUT_DB_PW = "output_db_pw";
	public static final String KEY_OUTPUT_DB_PATH = "output_db_path";
	public static final String KEY_SAVE_HTML = "save_html";
	public static final boolean KEY_SELENIUM_HEADLESS = true;
	
	public boolean setConfig(String config){
		JSONObject root;
		try{
			root = new JSONObject(config);
		}catch(JSONException e){
			System.out.println("Config ERR : wrong config file");
			return false;
		}
		
		if(!root.isNull(KEY_CRAWLING_NAME)){
			CRAWLING_NAME = root.getString(KEY_CRAWLING_NAME);
			CRAWLING_NAME = CRAWLING_NAME.trim().replace(" ", "_");
		}else{
			//CRAWLING_NAME = "DEFAULT_CRAWLER";
			System.out.println("Config ERR : Require crawling name");
			return false;
		}
		
		if(!root.isNull(KEY_IGNORE_ROBOTS)){
			IGNORE_ROBOTS = root.getBoolean(KEY_IGNORE_ROBOTS);
		}
		
		if(!root.isNull(KEY_CRAWLING_MAX_DEPTH)){
			CRAWLING_MAX_DEPTH = root.getInt(KEY_CRAWLING_MAX_DEPTH);
		}
		
		if(!root.isNull(KEY_CRAWLING_DELAY)){
			CRAWLING_DELAY = root.getInt(KEY_CRAWLING_DELAY);
		}
		
		if(!root.isNull(KEY_CRAWLING_MAX_COUNT)){
			CRAWLING_MAX_COUNT = root.getInt(KEY_CRAWLING_MAX_COUNT);
		}
		
		if(!root.isNull(KEY_CRAWLING_TYPE)){
			CRAWLING_TYPE = root.getString(KEY_CRAWLING_TYPE);
		}
		
		if(!root.isNull(KEY_CONTENTS_SAVE_TYPE)){
			OUTPUT_SAVE_TYPE = root.getString(KEY_CONTENTS_SAVE_TYPE);
		}
		
		if(!root.isNull(KEY_OUTPUT_FILE_PATH)){
			OUTPUT_FILE_PATH = root.getString(KEY_OUTPUT_FILE_PATH);
		}
		
		if(!root.isNull(KEY_OUTPUT_DB_ID)){
			OUTPUT_DB_ID = root.getString(KEY_OUTPUT_DB_ID);
		}
		
		if(!root.isNull(KEY_OUTPUT_DB_PW)){
			OUTPUT_DB_PW = root.getString(KEY_OUTPUT_DB_PW);
		}
		
		if(!root.isNull(KEY_OUTPUT_DB_PATH)){
			OUTPUT_DB_PATH = root.getString(KEY_OUTPUT_DB_PATH);
		}
		
		if(!root.isNull(KEY_SAVE_HTML)){
			SAVE_HTML = root.getBoolean(KEY_SAVE_HTML);
		}
//		
//		if(!root.isNull(KEY_)){
//			IGNORE_ROBOTS = root.getBoolean(KEY_IGNORE_ROBOTS);
//			mLogger.info("Set Config ignore_robots : " + IGNORE_ROBOTS);
//		}
		
		
		//CRAWLING_UPPER_SEARCH = root.getBoolean("crawling_upper_search");
		
		mFilterDuplicate = new ArrayList<Duplicate>();
		mFilterAllow = new ArrayList<String>();
		mFilterDisallow = new ArrayList<String>();
		
		mLeafURL = new ArrayList<String>();
		mCollects = new ArrayList<CollectRecode>();
		mScenarios = new HashMap<Integer, Scenario>();
		
		mRobots = new HashMap<String, Robots>();
		
		JSONObject object;
		JSONArray array;
		
		/**
		 * SEED 파싱
		 * */
		try{
			object = root.getJSONObject("seed");
			mSeedInfo = new Work(object.getString("url"));
			mSeedInfo.setDepth(0); // 루트로 인식
			if(object.isNull("type")) {
				mSeedInfo.setContentType(Work.GET);
			}else{
				String type = object.getString("type");
				if(type.equalsIgnoreCase("POST")){
					mSeedInfo.setContentType(Work.POST);
				}else{
					mSeedInfo.setContentType(Work.GET);
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
			System.out.println("Config ERR : seed");
			return false;
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
				System.out.println("CONFIG duplicate : " + array.length());
			} catch (JSONException e) { mFilterDuplicate.clear(); }
			
			try {
				array = object.getJSONArray("allow");
				for (Object url : array) {
					if (url != null) {
						mFilterAllow.add((String) url);
					}
				}
				System.out.println("CONFIG allow : " + array.length());
			} catch (JSONException e) {  mFilterAllow.clear(); }

			try {
				array = object.getJSONArray("disallow");
				for (Object url : array) {
					if (url != null) {
						mFilterDisallow.add((String) url);
					}
				}
				System.out.println("CONFIG disallow : " + array.length());
			} catch (JSONException e) { mFilterDisallow.clear();}

			try {
				array = object.getJSONArray("leaf_url");
				for (Object url : array) {
					if (url != null) {
						mLeafURL.add((String) url);
					}
				}
				System.out.println("CONFIG leaf_url : " + array.length());
			} catch (JSONException e) { mLeafURL.clear(); }
		} catch (JSONException e) { }

		JSONArray aryCols,aryJson;
		JSONObject col, jsonElement;
		CollectRecode recode;
		int len_i, len_j, len_k;
		String[] regexs;
		String recode_selector;
		CollectRecode.Column.Element[] elements;
		CollectRecode.Column.Element el;
		if(root.isNull("collect_recode")){
			mLogger.info("Config err : collect_recode");
			return false;
		}else{
			array = root.getJSONArray("collect_recode");
			len_i = array.length();
			for(int i = 0 ; i < len_i ; i ++){
				try{
					object = array.getJSONObject(i);
					recode = new CollectRecode(object.isNull("regex_url")? "": object.getString("regex_url"), 
											   object.getString("name"));
					if(!object.isNull("depth")) recode.setDepth(object.getInt("depth"));
					if(object.isNull("recode_selector")){
						recode_selector = "html";
					}else{
						recode_selector = object.getString("recode_selector");
						if(recode_selector.trim().isEmpty()){
							recode_selector = "html";
						}
					}
					recode.setRecodeSelector(recode_selector);
					aryCols = object.getJSONArray("column");
					len_j = aryCols.length();
					for(int j = 0 ; j < len_j ; j++ ){
						col = aryCols.getJSONObject(j);
						regexs = null;
						if(!col.isNull("regex_filter")){
							aryJson = col.getJSONArray("regex_filter");
							regexs = new String[aryJson.length()];
							aryJson.toList().toArray(regexs);
						}
						
						elements = null;
						if(!col.isNull("element")){
							aryJson = col.getJSONArray("element");
							elements = new CollectRecode.Column.Element[aryJson.length()];
							len_k = aryJson.length();
							for(int k = 0 ; k < len_k ; k++){
								jsonElement = aryJson.getJSONObject(k);
								elements[k] = 
										new CollectRecode.Column.Element(
												jsonElement.isNull("selector")? "": jsonElement.getString("selector"), 
												jsonElement.isNull("type")? "": jsonElement.getString("type"),
												jsonElement.isNull("from_root")? false: jsonElement.getBoolean("from_root"));
							}
						}
						
						recode.add(col.isNull("key")? false : true,
								   col.isNull("type")? "" : col.getString("type"),
								   elements,
								   col.isNull("data_type")? "" : col.getString("data_type"), 
								   col.isNull("data_name")? "" : col.getString("data_name"),
								   col.isNull("allow_null")? false : col.getBoolean("allow_null"),
								   regexs);
					}
					mCollects.add(recode);
				} catch (JSONException e) { 
					//mCollects.clear(); 
					System.out.println("Config err : collect_recode " + i + "row");
					//e.printStackTrace(); 
				}
			}
		}
			
		try{
			array = root.getJSONArray("scenario");
			len_i = array.length();
			int depth;
			JSONArray aryScenario, aryLoadCheckSelector;
			JSONObject scenObject;
			Scenario scenario;
			for(int i = 0 ; i < len_i ; i++){
				object = array.getJSONObject(i);
				aryScenario = object.getJSONArray("action");
				depth = object.getInt("depth");
				len_j = aryScenario.length();
				scenario = new Scenario(depth, len_j);
				mScenarios.put(depth, scenario);
				if(!object.isNull("check_load_selector")){
					aryLoadCheckSelector = object.getJSONArray("check_load_selector");
					for(Object selector: aryLoadCheckSelector){
						scenario.addLoadCheckSelector(String.valueOf(selector));
					}
				}
				for(int j = 0 ; j < len_j ; j++){
					scenObject = aryScenario.getJSONObject(j);
					scenario.add(scenObject.isNull("target_depth")? -1: scenObject.getInt("target_depth"), 
								 scenObject.isNull("selector")? null : scenObject.getString("selector"),
								 scenObject.isNull("empty_selector")? null : scenObject.getString("empty_selector"),
								 scenObject.isNull("type")? "click": scenObject.getString("type"),
								 scenObject.isNull("value")? null: scenObject.getString("value"));
				}
				for(int j = 0 ; j < scenario.getSize() ; j++){
					scenario.getAction(j);
				}
			}
		} catch (JSONException e){ 
			mScenarios.clear(); 
		}
		
		return true;
	}

	public Work getSeedInfo() {
		return mSeedInfo;
	}

	public Map<String, Robots> getRobots() {
		return mRobots;
	}

	public Map<Integer, Scenario> getScenarios() {
		return mScenarios;
	}

	public List<Duplicate> getFilterDuplicate() {
		return mFilterDuplicate;
	}

	public List<String> getFilterAllow() {
		return mFilterAllow; // 허용된 URL(또는 패턴)
	}

	public List<String> getFilterDisallow() {
		return mFilterDisallow;
	}

	public List<String> getLeafURL() {
		return mLeafURL;// 크롤링 마지막 URL(또는 패턴) : 해당 페이지의 링크는 수집하지 않는다
	}

	public List<CollectRecode> getCollects() {
		return mCollects;// 수집할 콘텐츠
	}
}
