package com.onycom.crawler.scraper;

import java.io.IOException;
import java.net.URLEncoder;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
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

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.phantomjs.PhantomJSDriver;
import org.openqa.selenium.phantomjs.PhantomJSDriverService;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.openqa.selenium.support.ui.ExpectedConditions;
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
	
	public static Document GetDocument(URLInfo urlInfo) throws Exception{
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
	
	private static Document useSelenium(URLInfo urlInfo){
		Document ret = null;
		List<String> checkSelector;
		WebElement we = null;
		String newWindow;
		Action action = urlInfo.getAction();
		urlInfo.setParentWindow(mSeleniumDriver.getWindowHandle());
		if(action != null){
			String selector = action.getSelector();
			we = waitingForElement(mSeleniumDriver, selector);
			if(we != null){
				System.out.println("action type " + action.getType() );
				if(action.getType().contentEquals(Action.TYPE_CLICK)){
					we.click();
				}else{ // if(action.getType() == Action.TYPE_INPUT)
					//we.sendKeys(keysToSend);
				}
				if(action.getTarget().contentEquals(Action.TARGET_BLANK)){
					ArrayList<String> tab = new ArrayList<String> (mSeleniumDriver.getWindowHandles());
					mSeleniumDriver.switchTo().window(tab.get(tab.size()-1));
				}else{ //if(action.getTarget() == Action.TARGET_SELF)
					
				}
				// 로딩 확인하고
				if(urlInfo.getLoadCheckSelectors() != null){
					for(String s : urlInfo.getLoadCheckSelectors()){
						if(waitingForElement(mSeleniumDriver, s) == null){
							return null;
						}
					}
				}
				ret = Jsoup.parse(mSeleniumDriver.getPageSource());
			}
		}else{ // 액션 없는 seed 일 경우
			
			// URL 호출
			mSeleniumDriver.get(urlInfo.getURL());
			
			// 로딩 확인하고
			if(urlInfo.getLoadCheckSelectors() != null){
				for(String selector : urlInfo.getLoadCheckSelectors()){
					if(waitingForElement(mSeleniumDriver, selector) == null){
						return null;
					}
				}
			}
			//window 정보 저장하고
			urlInfo.setParentWindow(mSeleniumDriver.getWindowHandle());
			
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
	
	private static WebElement waitingForElement(WebDriver wd, String selector){
		try{
			return new WebDriverWait(wd, 10)
						.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector(selector)));
		}catch(org.openqa.selenium.TimeoutException e){
			return null;
		}
	}
	
	private static void connectSelenium(){
		if (mSeleniumDriver == null){
			System.setProperty("webdriver.chrome.driver", "chromedriver.exe");
			ChromeOptions options= new ChromeOptions();
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
