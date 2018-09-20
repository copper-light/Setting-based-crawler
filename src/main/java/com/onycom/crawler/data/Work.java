package com.onycom.crawler.data;

import java.beans.Encoder;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.http.client.utils.URLEncodedUtils;

/**
 * 주소 저장 객체
 * 같은 주소라도 request value 에 따라 다른 값을 받기 때문에
 * 객체로 만들어서 data 까지 비교한다
 * */ 
public class Work {
	public static final byte POST = 0x0;
	public static final byte GET = 0x1;
	
	public static final int STATE_IDLE = 0x0;
	public static final int STATE_WORKING = 0x1;
	public static final int STATE_FAIL = 0x2;
	public static final int STATE_SUCCESS = 0x3;
	
	public static final byte PARSE_NORMAL = 0x0;
	public static final byte PARSE_SCENARIO = 0x1;
	public static final byte PARSE_FIND_ACTION =  0x2;
	
	int mState = STATE_IDLE;
	
	boolean mHighPriority = false; 
	
	byte mContentType;
	String mUrl = null;
	String mDomainURL = null;
	String mSubUrl = null;
	
	byte mParseType = PARSE_NORMAL;
	Action mAction;
	String mParentWindow;
	
	List<String> mLoadCheckSelector;
	Result mResult;
	
	// 현재는 쓰지 않지만 향후에 써야할 가능성도 있을것으로 보임
	// Map<String, String> mHeader;
	
	Map<String, String> mDataMap;
	
	char[] URL_SPLIT_CHAR = {'/','?','#'};
	
	/**
	 * 시드 URL으로부터 몆 번 링크 타고 들어갔는지 횟수
	 * 크롤러의 검색 범위를 제한하고자 할때 사용한다. 
	 * 0 은 시드 URL을 의미
	 * */ 
	int mDepth = -1; 
	
	/**
	 * url 값은 반드시 http와 루트 도메인 주소가 포함된 전체 주소여야 한다.
	 * 그 외의 형식이라면 NULL 반환
	 * */
	public Work(String url){
		setURL(url);
	}
	
	public void setURL(String url){
		url = url.trim();
		
		/* 마지막 문자가 "/" 이면 삭제할 것
		 * 모든 URL 연산시 마지막 / 은 없다고 가정하고 진행하기 위해서
		 * */
		String lastChar = url.substring(url.length()-1, url.length());
		while(true){
			if(lastChar.equalsIgnoreCase("/")){
				url = url.substring(0, url.length()-1);
				lastChar = url.substring(url.length()-1, url.length());
			}else{
				break;
			}
		}
		mUrl = url;
		
		/* 8 의미 - http:// 또는  htts:// 를 제외하기 위한 길이 */
		/* 추가 구현 고려 사항 : 주소 인코딩에 대한 고려도 있어야 할 것으로 보임 */
		if(url != null && url.length() > 8 && url.matches("http(.*)")){
			/* # 처리 : 페이지 내에 인덱싱 기능이므로 삭제해버린다 */
			int idx = url.indexOf('#', 8);
			if(idx != -1){
				url = url.substring(0, idx);
			}
			
			/* 도메인과 서브 주소를 분류 */
			idx = url.indexOf('/', 8);
			if(idx != -1){
				mDomainURL = url.substring(0, idx);
				mSubUrl = url.substring(idx);
			}else{
				mDomainURL = url;
				mSubUrl = "/";
			}
			
			/* 쿼리스트링 분류 */
			String query = null;
			if (mSubUrl.contentEquals("/")){
				idx = mDomainURL.indexOf('?', 8);
				if(idx != -1){
					if(mDomainURL.length() >= (idx+1)){
						query = mDomainURL.substring(idx+1);
					}
					mDomainURL = mDomainURL.substring(0, idx);
				}
			}else{
				idx = mSubUrl.indexOf('?', 8);
				if(idx != -1){
					query = mSubUrl.substring(idx+1);
					mSubUrl = mSubUrl.substring(0, idx);
				}
			}
			
			/* ? 쿼리스트링 데이터 파싱 */
			if(query != null){
				String[] row = query.split("&");
				String[] data;
				for(String set : row){
					data = set.split("=");
					if(data.length == 2){
						setData(data[0], data[1]);
					}
				}
			}
			
			/* 주소 인코딩에 대한 고려도 있어야 할 것으로 보임 */
//			try {
//				//mUrl = URLEncoder.encode(url, "UTF-8");
//				// 이미 인코딩된 주소라면  JSOUP이 
//				// 다시 인코딩에서 사용하기 떄문에 디코딩해서 저장함 
//				url = URLDecoder.decode(url, "UTF-8");
//			} catch (UnsupportedEncodingException e) {
//				e.printStackTrace();
//				mUrl = null;
//			}
			
		}else{
			mDomainURL = null;
		}
		
		mContentType = Work.GET;
	}
	
	public void setAction(Action action){	
		mAction = action;
	}
	public Action getAction(){
		return mAction;
	}
	
	public void setParentWindow(String id){
		mParentWindow = id;
	}
	
	public String getParentWindow(){
		return mParentWindow;
	}
	
	public void setLoadCheckSelectors(List<String> selectors){
		mLoadCheckSelector = new ArrayList<String>();
		for(String str : selectors){
			mLoadCheckSelector.add(str);
		}
	}
	
	public List<String> getLoadCheckSelectors(){
		return mLoadCheckSelector;
	}
	
	public String getSubURL(){
		return mSubUrl;
	}
	
	public byte getContentType(){
		return mContentType;
	}
	
	public void setContentType(byte type){
		mContentType = type;
	}
	
	public String getDomainURL(){
		return mDomainURL;
	}
	
	public String getURL(){
		return mUrl;
	}
	
	public int getState(){
		return mState;
	}
	
	public void updateState(int state){
		mState = state;
	}
	
	public Map<String, String> getData(){
		return mDataMap;
	}
	
	public String getData(String key){
		return mDataMap.get(key);
	}
	
	public void setHighPriority(boolean isHighPriority){
		mHighPriority = isHighPriority;
	}
	
	public boolean isHighPriority(){
		return mHighPriority;
	}
	
	public void setParseType(byte type){
		mParseType = type;
	}
	
	public byte getParseType(){
		return mParseType;
	}
	
	public void setData(String key, String value){
		if(mDataMap == null) mDataMap = new HashMap<String, String>();
		if(value == null || value == ""){
			if(mDataMap.get(key) == null){
				return;
			}else{
				mDataMap.remove(key);
			}
		}
		mDataMap.put(key, value);
	}
	
	public Work setDepth(int depth){
		mDepth = depth;
		return this;
	}
	
	public int getDepth(){
		return mDepth;
	}
	
	/**
	 * request value 를 String 으로 변환
	 * */
	public String getDataToString(){
		String ret = null;
		if(mDataMap!= null && mDataMap.size() > 0){
			for(String key: mDataMap.keySet()){
				if(ret != null){
					ret += "&";
				}else{
					ret = "";
				}
				ret += (key + "=" + mDataMap.get(key));
			}	
		}
		return ret;
	}
	
	/**
	 * 클래스의 값을 URL GET 형식으로 반환 
	 * */
	@Override
	public String toString(){
		String ret = "";
		
		if(mDomainURL != null){
			ret = mDomainURL;
			if(mSubUrl != null){
				ret += mSubUrl;
			}
		}
		
		String data = this.getDataToString();
		if(data != null){ 
			ret += "?" + data;
		}
		return ret;
	}

	/*
	 * 컨텐츠의 내용을 비교하기 위해서 오버라이드 함
	 * */
	@Override
	public boolean equals(Object obj) {
		if(this.getClass() == obj.getClass()){
			Work target = (Work) obj;
			if(this.toString().contentEquals(target.toString())){
				return true;
			}
		}
		return false;
	}

	/*
	 * 컨텐츠의 내용을 비교하기 위해서 오버라이드 함
	 * */
	@Override
	public int hashCode() {
		return this.toString().hashCode();
	}
	
	public Result result(){
		if(mResult == null){
			mResult = new Result();
		}
		return mResult;
	}
	
	public class Result{
		boolean isSuccess;
		int saveCnt;

		ArrayList<Error> errorList;
		
		public Result(){
			this.saveCnt = 0;
			this.isSuccess = false;
			this.errorList = new ArrayList<Error>();
		}
		
		public void setSaveCount(int saveCnt){
			this.saveCnt = saveCnt;
		}
		
		public int getSaveCount(){
			return this.saveCnt;
		}
		
		public void success(){
			this.isSuccess = true;
		}
		
		public boolean isSuccess(){
			return this.isSuccess;
		}
		
		public void addError(byte type, String msg){
			Error e = new Error();
			e.type = type;
			e.msg = msg;
			e.timeMs = System.currentTimeMillis();
			
			this.errorList.add(e);
		}
		
		public ArrayList<Error> getErrorList(){
			return this.errorList;
		}
	}

	public static class Error{
		public static final byte ERR_URL = 0x0;
		public static final byte ERR_CONN = 0x1;
		public static final byte ERR_ACTION = 0x2;
		public static final byte ERR_CONTENTS_COL = 0x3;
		public static final byte ERR_CONTENTS_RECODE = 0x4;
		public final String[] TYPE_STR = {"ERR_URL","ERR_CONN","ERR_ACTION","ERR_CONTENTS_COL","ERR_CONTENTS_RECODE"};
		static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yy-MM-dd-HH:mm:ss");;
		long timeMs;
		byte type;
		String msg;
		
		public Error(){
		}
		
		public String toString(){
			String date = DATE_FORMAT.format(new Date(this.timeMs));
			return date + " [" + TYPE_STR[type] + "] " + this.msg;
		}
	}
}
