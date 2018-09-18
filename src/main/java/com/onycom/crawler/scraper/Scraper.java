package com.onycom.crawler.scraper;

import java.io.IOException;
import java.net.URLEncoder;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.json.JSONException;
import org.json.JSONObject;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.Keys;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.phantomjs.PhantomJSDriver;
import org.openqa.selenium.phantomjs.PhantomJSDriverService;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.Select;
import org.openqa.selenium.support.ui.WebDriverWait;

import com.onycom.crawler.core.Crawler;
import com.onycom.crawler.data.Config;
import com.onycom.crawler.data.Cookie;
import com.onycom.crawler.data.Action;
import com.onycom.crawler.data.URLInfo;

/**
 * 웹페이지의 데이터를 가져오는 역할을 수행
 * (AJAX 와 같은 동적  스크립트는 불러오지 못함 : 해당 케이스의 경우 Selenium 같은 콘솔 브라우저를 이용해야함)
 * */
public class Scraper {
	public static String charset = "UTF-8";
	private static Cookie mCookie;
	private static String TYPE = Config.CRAWLING_TYPE_SCENARIO_STATIC;
		
	static WebDriver mSeleniumDriver;
	
	static Map<String, String> mJSData = new HashMap<String, String>();
	
	private Scraper(){ 
		mCookie = new Cookie();
	}
	
	public static void SetConfig(Config config){
		TYPE = config.CRAWLING_TYPE;
	}
	
	public static void open(){
		if(TYPE.contentEquals(Config.CRAWLING_TYPE_SCENARIO_DYNAMIC)){
			connectSelenium();
		}
	}
	
	public static void close(){
		if(TYPE.contentEquals(Config.CRAWLING_TYPE_SCENARIO_DYNAMIC)){
			quitSelenium();
		}
	}
	
	public static Document GetDocument(URLInfo urlInfo) throws JSONException, org.openqa.selenium.TimeoutException, WebDriverException, NoSuchAlgorithmException, KeyManagementException, IOException {
		if(TYPE.contentEquals(Config.CRAWLING_TYPE_SCENARIO_DYNAMIC)){
			return useSelenium(urlInfo);
		}else{
			return useJsoup(urlInfo);
		}
	}
	
	/**
	 * 주소로 접근해서 HTML 문서 반환<p>
	 *
	 * @param URLInfo 객체
	 * @return Document
	 */
	private static Document useJsoup(URLInfo urlInfo) throws NoSuchAlgorithmException, KeyManagementException, IOException  {
		TrustManager[] trustAllCerts = new TrustManager[]{new X509TrustManager(){
            public X509Certificate[] getAcceptedIssuers(){return new X509Certificate[0];}
            public void checkClientTrusted(X509Certificate[] certs, String authType){}
            public void checkServerTrusted(X509Certificate[] certs, String authType){}
        }};

        SSLContext sc;
		sc = SSLContext.getInstance("TLS");
		sc.init(null, trustAllCerts, new SecureRandom());
        HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());

        Document doc = null;
        Connection conn = Jsoup.connect(urlInfo.getURL());
        //Map<String,String> cookies = mCOOKIE.get(urlInfo.getRootURL());
        //if(cookies != null) conn.cookies(cookies);
        conn.userAgent(Crawler.USER_AGENT_NAME);
        
        if(urlInfo.getData() != null && urlInfo.getData().size() > 0){
        	for(String key: urlInfo.getData().keySet()){
        		conn.data(key, urlInfo.getData().get(key).toString());
        	}
        }
       
        if(urlInfo.getContentType() == URLInfo.POST){
        	doc = conn.ignoreContentType(true).post();
        }else{ // urlInfo.getType() == URLInfo.GET
        	doc = conn.ignoreContentType(true).get();
        }
        //System.err.println(conn.response().);
        //mCOOKIE.put(urlInfo.getRootURL(), conn.response().cookies());
        return doc;
	}
	
	private static Document useSelenium(URLInfo urlInfo) throws JSONException, TimeoutException, WebDriverException {
		Document ret = null;
		List<String> checkSelector;
		WebElement we = null;
		List<WebElement> wes = null;
		String newWindow;
		Action action = urlInfo.getAction();
		
		//urlInfo.setParentWindow(mSeleniumDriver.getWindowHandle());
		if(action != null){
			String selector = action.getSelector();
			String value = action.getValue();
			if(selector != null){
				wes = waitingForAllElement(mSeleniumDriver, selector);
				if(wes.size() == 1){
					we = wes.get(0);
					System.err.println("[action] "+action.getType() +" @ " +selector);
					if(action.getType().contentEquals(Action.TYPE_CLICK)){
						we.click();
					}else if(action.getType().contentEquals(Action.TYPE_INPUT) && value != null) {
						we.sendKeys(value);
					}else if(action.getType().contentEquals(Action.TYPE_VERTICAL_SCROLL) && value != null) {
						JavascriptExecutor jse = (JavascriptExecutor)mSeleniumDriver;
						jse.executeScript("window.scrollBy(0,"+ value +")", "");
					}
					else if(action.getType().contentEquals(Action.TYPE_SELECT) && value != null) {
						Select dropdown = new Select(we);
						try{
							int intValue = Integer.parseInt(value);
							dropdown.selectByIndex(intValue);
						}catch(NumberFormatException  e){ // exception 이라면  String 이므로 String 처리
							dropdown.selectByValue(value);
						}
					}else if(action.getType().contentEquals(Action.TYPE_JAVASCRIPT) &&  value != null){
						JavascriptExecutor jse = (JavascriptExecutor)mSeleniumDriver;
						Iterator<String> it = mJSData.keySet().iterator();
						String k,v;
						while(it.hasNext()){
							k = it.next();
							v = mJSData.get(k);
							value = "var "+ k + "=" + v +"; "+ value;
						}
						Object js_ret = jse.executeScript(value, ""); /* 스크립트 오류 익셉션 체크해야함 */
						if(js_ret != null){
							try{
								System.out.println(String.valueOf(js_ret));
								JSONObject json = new JSONObject(String.valueOf(js_ret));
								for(Object key : json.keySet().toArray()){
									k = String.valueOf(key);
									mJSData.put(k, String.valueOf(json.get(k)));
								}
							}catch(JSONException e) {
								throw e;
								//Crawler.Log('e', e.getMessage(), e.fillInStackTrace());
								//e.printStackTrace();
							}
						}
					}
					// 로딩 확인하고
//					if(urlInfo.getLoadCheckSelectors() != null && urlInfo.getLoadCheckSelectors().size() > 0){
//						for(String s : urlInfo.getLoadCheckSelectors()){
//							waitingForElement(mSeleniumDriver, s);
//						}
//					}
					if(action.getTargetDepth() != -1){
						urlInfo.setParseType(URLInfo.PARSE_SCENARIO);
						urlInfo.setDepth(action.getTargetDepth());
					}
					urlInfo.setURL(mSeleniumDriver.getCurrentUrl());
					ret = Jsoup.parse(mSeleniumDriver.getPageSource());
				}else{
					/*
					 * 액션 찾는 로직으로 전달
					 * */
					urlInfo.setParseType(URLInfo.PARSE_FIND_ACTION);
					urlInfo.setURL(mSeleniumDriver.getCurrentUrl());
					ret = Jsoup.parse(mSeleniumDriver.getPageSource());
				}
			}else{
				if(action.getType().contentEquals(Action.TYPE_SWITCH_WINDOW)){
					ArrayList<String> tab = new ArrayList<String> (mSeleniumDriver.getWindowHandles());
					int cur_idx = 0;
					int tab_size = tab.size();
					for(int i = 0 ; i < tab_size; i ++){
						if(tab.get(i).contentEquals(mSeleniumDriver.getWindowHandle())){
							cur_idx = i;
						}
					}
					// 값이 없으면 무조건 마지막 윈도우로
					if(value == null){
						mSeleniumDriver.switchTo().window(tab.get(tab_size-1));
					}else{ // 현재를 기준으로 이동하고자 할때
						int new_idx = Integer.parseInt(value);
						if(value.indexOf('+') != -1 || value.indexOf('-') != -1){
							new_idx = (cur_idx + new_idx);
							if(new_idx < 0) new_idx = 0;
						}
						if(tab_size > new_idx){
							mSeleniumDriver.switchTo().window(tab.get(new_idx));
						}
					}
				}else if(action.getType().contentEquals(Action.TYPE_CLOSE_WINDOW)){
					ArrayList<String> tab = new ArrayList<String> (mSeleniumDriver.getWindowHandles());
					int tab_size = tab.size();
					int cur_idx = 0;
					for(int i = 0 ; i < tab_size; i ++){
						if(tab.get(i).contentEquals(mSeleniumDriver.getWindowHandle())){
							cur_idx = i;
						}
					}
					if(value == null){
						mSeleniumDriver.close();
					}else{ // 현재를 기준으로 이동하고자 할때
						int new_idx = Integer.parseInt(value);
						if(tab_size > new_idx){
							mSeleniumDriver.switchTo().window(tab.get(new_idx));
							mSeleniumDriver.close();
						}
					}
					tab_size = mSeleniumDriver.getWindowHandles().size();
					if(tab_size > cur_idx){
						mSeleniumDriver.switchTo().window(tab.get(cur_idx));
					}else{
						mSeleniumDriver.switchTo().window(tab.get(tab_size-1));
					}
				}else if(action.getType().contentEquals(Action.TYPE_BACKWORD_WINDOW)){
					mSeleniumDriver.navigate().back();
				}else if(action.getType().contentEquals(Action.TYPE_FORWORD_WINDOW)){
					mSeleniumDriver.navigate().forward();
				}else if(action.getType().contentEquals(Action.TYPE_REFRESH_WINDOW)){
					mSeleniumDriver.navigate().refresh();
				}else if(action.getType().equalsIgnoreCase(Action.TYPE_CLOSE_POPUP)){
					if(value != null){
						String cur_handle = mSeleniumDriver.getWindowHandle();
						Integer target_idx = Integer.parseInt(value);
						ArrayList<String> tab = new ArrayList<String> (mSeleniumDriver.getWindowHandles());
						int tab_size = tab.size();	
						if(tab_size > target_idx){
							mSeleniumDriver.switchTo().window(tab.get(target_idx));
							mSeleniumDriver.close();
							mSeleniumDriver.switchTo().window(cur_handle);
						}
					}
				}
				return null;
			}
		}else{ // 액션 없는 seed 일 경우
			// URL 호출
			mSeleniumDriver.get(urlInfo.toString());
			
//			// 로딩 확인하고
//			if(urlInfo.getLoadCheckSelectors() != null){
//				for(String selector : urlInfo.getLoadCheckSelectors()){
//					waitingForElement(mSeleniumDriver, selector);
//				}
//			}
			//window 정보 저장하고
			urlInfo.setParentWindow(mSeleniumDriver.getWindowHandle());
			urlInfo.setParseType(URLInfo.PARSE_SCENARIO);
			// 문서 리턴
			ret = Jsoup.parse(mSeleniumDriver.getPageSource());
		}
		return ret;
	}
	
	public static void closeDocument(URLInfo urlInfo){
		String window = urlInfo.getParentWindow();
		String curWindow = mSeleniumDriver.getWindowHandle();
		if(window == null){
			mSeleniumDriver.close();
		}else if(!window.contentEquals(curWindow)){
			mSeleniumDriver.close();
			mSeleniumDriver.switchTo().window(window);
		}else{
			mSeleniumDriver.navigate().back();
		}
	}
	
	public static List<WebElement> GetEelements(String selector){
		try{
			return waitingForAllElement(mSeleniumDriver, selector);
		} catch(TimeoutException e){
			return null;
		}
	}
	
	private static WebElement waitingForElement(WebDriver wd, String selector) throws org.openqa.selenium.TimeoutException{
		return new WebDriverWait(wd, 10).until(ExpectedConditions.presenceOfElementLocated(By.cssSelector(selector)));
	}
	
	private static List<WebElement> waitingForAllElement(WebDriver wd, String selector) throws org.openqa.selenium.TimeoutException{
		return new WebDriverWait(wd, 10).until(ExpectedConditions.presenceOfAllElementsLocatedBy(By.cssSelector(selector)));
	}
	
	private static void connectSelenium(){
		if (mSeleniumDriver == null){
			System.setProperty("webdriver.chrome.driver", "./web_driver/chromedriver.exe");
			ChromeOptions options= new ChromeOptions();
			options.addArguments("--disable-notifications");
			//options.addArguments("headless");
			mSeleniumDriver = new ChromeDriver(options);
//			DesiredCapabilities DesireCaps = new DesiredCapabilities();
//			DesireCaps.setCapability(PhantomJSDriverService.PHANTOMJS_EXECUTABLE_PATH_PROPERTY, "phantomjs.exe");
//			mSeleniumDriver = new PhantomJSDriver(DesireCaps);
		}
	}

	private static void quitSelenium(){
		if(mSeleniumDriver != null){
			mSeleniumDriver.quit();
		}
	}
}
