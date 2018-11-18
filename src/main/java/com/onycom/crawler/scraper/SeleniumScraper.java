package com.onycom.crawler.scraper;

import com.onycom.common.CrawlerLog;
import com.onycom.crawler.data.Action;
import com.onycom.crawler.data.Config;
import com.onycom.crawler.data.Cookie;
import com.onycom.crawler.data.Work;
import org.apache.log4j.Logger;
import org.json.JSONException;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.openqa.selenium.*;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.phantomjs.PhantomJSDriver;
import org.openqa.selenium.phantomjs.PhantomJSDriverService;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.Select;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.util.*;

/**
 * 싱글톤으로 운영
 * */
public class SeleniumScraper implements Scraper {
    //static SeleniumScraper mInstance;

    static Logger mLogger = CrawlerLog.GetInstance(SeleniumScraper.class);
    static WebDriver mSeleniumDriver;
    static Map<String, String> mJSData = new HashMap<String, String>();
    static Config mConfig;

    public SeleniumScraper(Config config){
        mConfig = config;
    }
//    private SeleniumScraper(){}
//
//    public SeleniumScraper getInstance(){
//        if(mInstance == null){
//            mInstance = new SeleniumScraper();
//        }
//        return mInstance;
//    }


    public boolean open() {
        return connectSelenium(mConfig);
    }

    public void close() {
        quitSelenium();
    }

    public Document getDocument(Work work) throws  Exception {
        Document ret = null;
        //List<String> checkSelector;
        WebElement we = null;
        List<WebElement> wes = null;
        //String newWindow;
        Action action = work.getAction();

        // urlInfo.setParentWindow(mSeleniumDriver.getWindowHandle());
        if (action != null) {
            String selector = action.getSelector();
            String empty_selector = action.getEmptySelector();
            String value = action.getValue();
            if (selector != null) {
                wes = waitingForAllElements(mSeleniumDriver, 10, selector, empty_selector);
                if (wes == null) { // 못찾음
                    //mLogger.error("Not found element : " + selector);
                    work.result().addError(Work.Error.ERR_ACTION, selector, null);
                    return null;
                }
                if (wes.isEmpty()) { // 이제 없음
                    //mLogger.info("empty element : " + selector);
                    if (action.getType().contentEquals(Action.TYPE_PARSE_CONTENTS)) {
                        work.setDepth(action.getTargetDepth());
                        work.setURL(mSeleniumDriver.getCurrentUrl());
                        ret = Jsoup.parse(mSeleniumDriver.getPageSource());
                        return ret;
                    } else {
                        return null;
                    }
                }
                if (wes.size() == 1) {
                    we = wes.get(0);
                    // System.err.println("[action] "+action.getType() +" @ "
                    // +selector);
                    if (action.getType().equalsIgnoreCase(Action.TYPE_CLICK)) {
                        we.click();
                    } else if (action.getType().equalsIgnoreCase(Action.TYPE_INPUT) && value != null) {
                        we.sendKeys(value);
                    } else if (action.getType().equalsIgnoreCase(Action.TYPE_VERTICAL_SCROLL) && value != null) {
                        JavascriptExecutor jse = (JavascriptExecutor) mSeleniumDriver;
                        jse.executeScript("window.scrollBy(0," + value + ")", "");
                    } else if (action.getType().equalsIgnoreCase(Action.TYPE_SELECT) && value != null) {
                        Select dropdown = new Select(we);
                        try {
                            int intValue = Integer.parseInt(value);
                            dropdown.selectByIndex(intValue);
                        } catch (NumberFormatException e) { // exception 이라면
                            // String 이므로 String
                            // 처리
                            dropdown.selectByValue(value);
                        }
                    } else if (action.getType().equalsIgnoreCase(Action.TYPE_JAVASCRIPT) && value != null) {
                        JavascriptExecutor jse = (JavascriptExecutor) mSeleniumDriver;
                        Iterator<String> it = mJSData.keySet().iterator();
                        String k, v;
                        while (it.hasNext()) {
                            k = it.next();
                            v = mJSData.get(k);
                            value = "var " + k + "=" + v + "; " + value;
                        }
                        Object js_ret = jse.executeScript(value,
                                ""); /* 스크립트 오류 익셉션 체크해야함 */
                        if (js_ret != null) {
                            try {
                                System.out.println(String.valueOf(js_ret));
                                JSONObject json = new JSONObject(String.valueOf(js_ret));
                                for (Object key : json.keySet().toArray()) {
                                    k = String.valueOf(key);
                                    mJSData.put(k, String.valueOf(json.get(k)));
                                }
                            } catch (JSONException e) {
                                throw e;
                                // Crawler.Log('e', e.getMessage(),
                                // e.fillInStackTrace());
                                // e.printStackTrace();
                            }
                        }
                    }
                    if (action.getTargetDepth() != -1) {
                        work.setParseType(Work.PARSE_SCENARIO);
                        work.setDepth(action.getTargetDepth());
                    }
                    work.setURL(mSeleniumDriver.getCurrentUrl());
                    ret = Jsoup.parse(mSeleniumDriver.getPageSource());
                    // mSeleniumDriver.
                } else {
                    /*
                     * 액션 찾는 로직으로 전달
                     */
                    work.setParseType(Work.PARSE_FIND_ACTION);
                    work.setURL(mSeleniumDriver.getCurrentUrl());
                    ret = Jsoup.parse(mSeleniumDriver.getPageSource());
                }
            } else {
                if (action.getType().equalsIgnoreCase(Action.TYPE_SWITCH_WINDOW)) {
                    ArrayList<String> tab = new ArrayList<String>(mSeleniumDriver.getWindowHandles());
                    int cur_idx = 0;
                    int tab_size = tab.size();
                    for (int i = 0; i < tab_size; i++) {
                        if (tab.get(i).contentEquals(mSeleniumDriver.getWindowHandle())) {
                            cur_idx = i;
                        }
                    }
                    // 값이 없으면 무조건 마지막 윈도우로
                    if (value == null) {
                        mSeleniumDriver.switchTo().window(tab.get(tab_size - 1));
                    } else { // 현재를 기준으로 이동하고자 할때
                        int new_idx = Integer.parseInt(value);
                        if (value.indexOf('+') != -1 || value.indexOf('-') != -1) {
                            new_idx = (cur_idx + new_idx);
                            if (new_idx < 0)
                                new_idx = 0;
                        }
                        if (tab_size > new_idx) {
                            mSeleniumDriver.switchTo().window(tab.get(new_idx));
                        }
                    }
                } else if (action.getType().equalsIgnoreCase(Action.TYPE_CLOSE_WINDOW)) {
                    ArrayList<String> tab = new ArrayList<String>(mSeleniumDriver.getWindowHandles());
                    int tab_size = tab.size();
                    int cur_idx = 0;
                    for (int i = 0; i < tab_size; i++) {
                        if (tab.get(i).contentEquals(mSeleniumDriver.getWindowHandle())) {
                            cur_idx = i;
                        }
                    }
                    if (value == null) {
                        mSeleniumDriver.close();
                    } else { // 현재를 기준으로 이동하고자 할때
                        int new_idx = Integer.parseInt(value);
                        if (tab_size > new_idx) {
                            mSeleniumDriver.switchTo().window(tab.get(new_idx));
                            mSeleniumDriver.close();
                        }
                    }
                    tab_size = mSeleniumDriver.getWindowHandles().size();
                    if (tab_size > cur_idx) {
                        mSeleniumDriver.switchTo().window(tab.get(cur_idx));
                    } else {
                        mSeleniumDriver.switchTo().window(tab.get(tab_size - 1));
                    }
                } else if (action.getType().equalsIgnoreCase(Action.TYPE_BACKWORD_WINDOW)) {
                    mSeleniumDriver.navigate().back();
                } else if (action.getType().equalsIgnoreCase(Action.TYPE_FORWORD_WINDOW)) {
                    mSeleniumDriver.navigate().forward();
                } else if (action.getType().equalsIgnoreCase(Action.TYPE_REFRESH_WINDOW)) {
                    mSeleniumDriver.navigate().refresh();
                } else if (action.getType().equalsIgnoreCase(Action.TYPE_CLOSE_POPUP)) {
                    if (value != null) {
                        String cur_handle = mSeleniumDriver.getWindowHandle();
                        Integer target_idx = Integer.parseInt(value);
                        ArrayList<String> tab = new ArrayList<String>(mSeleniumDriver.getWindowHandles());
                        int tab_size = tab.size();
                        if (tab_size > target_idx) {
                            mSeleniumDriver.switchTo().window(tab.get(target_idx));
                            mSeleniumDriver.close();
                            mSeleniumDriver.switchTo().window(cur_handle);
                        }
                    }
                }
                return null;
            }
        } else { // 액션 없는 seed 일 경우
            // URL 호출
            mSeleniumDriver.get(work.toString());
            //mSeleniumDriver.

            // // 로딩 확인하고
            // if(urlInfo.getLoadCheckSelectors() != null){
            // for(String selector : urlInfo.getLoadCheckSelectors()){
            // waitingForElement(mSeleniumDriver, selector);
            // }
            // }
            // window 정보 저장하고
            work.setParentWindow(mSeleniumDriver.getWindowHandle());
            work.setParseType(Work.PARSE_SCENARIO);
            // 문서 리턴
            ret = Jsoup.parse(mSeleniumDriver.getPageSource());
        }
        System.out.println(work.toString());
        return ret;
    }

//    public static void closeDocument(Work urlInfo) {
//        String window = urlInfo.getParentWindow();
//        String curWindow = mSeleniumDriver.getWindowHandle();
//        if (window == null) {
//            mSeleniumDriver.close();
//        } else if (!window.contentEquals(curWindow)) {
//            mSeleniumDriver.close();
//            mSeleniumDriver.switchTo().window(window);
//        } else {
//            mSeleniumDriver.navigate().back();
//        }
//    }
//
//    public static List<WebElement> GetEelements(String selector) {
//        try {
//            return waitingForAllElements(mSeleniumDriver, selector);
//        } catch (TimeoutException e) {
//            return null;
//        }
//    }
//
//    private static WebElement waitingForElement(WebDriver wd, String selector)
//            throws org.openqa.selenium.TimeoutException {
//        return new WebDriverWait(wd, 10).until(ExpectedConditions.presenceOfElementLocated(By.cssSelector(selector)));
//    }

    private static List<WebElement> waitingForAllElements(WebDriver wd, int wait_sec, String selector,
                                                          String empty_selector) {
        List<WebElement> ret = null;
        WebElement check_sub_load;
        long ms = wait_sec * 1000;
        long startTime = System.currentTimeMillis();
        while (true) {
        	try{
        		//wd.
        		ret = wd.findElements(By.cssSelector(selector));
                if (ret != null && !ret.isEmpty()) {
                    break;
                }
                if (empty_selector != null) {
                    try {
                        check_sub_load = wd.findElement(By.cssSelector(empty_selector));
                    } catch (NoSuchElementException e) {
                        check_sub_load = null;
                    }
                    if (check_sub_load != null) {
                        ret = new ArrayList<WebElement>();
                        break;
                    }
                }
        	}catch(WebDriverException e){
        		mLogger.error("WebDriverException "+ e.getMessage());
        		return null;
        	}
            if ((System.currentTimeMillis() - startTime) > ms) {
                return null;
            }
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        return ret;
    }

//    private static List<WebElement> waitingForAllElements(WebDriver wd, String selector)
//            throws org.openqa.selenium.TimeoutException {
//        return new WebDriverWait(wd, 10)
//                .until(ExpectedConditions.presenceOfAllElementsLocatedBy(By.cssSelector(selector)));
//    }

    private static boolean connectSelenium(Config config) {
        String os = System.getProperty("os.name").toLowerCase();
        if (mSeleniumDriver == null) {
	        try {
	            if(config.SELENIUM_DRIVER_NAME.equalsIgnoreCase("chrome")){
                    if(config.SELENIUM_DRIVER_PATH.isEmpty()){
                        if(os.indexOf("win") >= 0){
                            System.setProperty("webdriver.chrome.driver", "./web_driver/chromedriver.exe");
                        }else{

                            System.setProperty("webdriver.chrome.driver", "./web_driver/chromedriver");
                        }
                    }else {
                        System.setProperty("webdriver.chrome.driver", config.SELENIUM_DRIVER_PATH);
                    }
                    ChromeOptions options = new ChromeOptions();
                    options.addArguments("--disable-notifications");
                    //options.addArguments("window-size=1920x1080");
                    if(config.SELENIUM_HEADLESS){
                        options.addArguments("headless");
                    }
                    mSeleniumDriver = new ChromeDriver(options);
	            }else{
	                DesiredCapabilities DesireCaps = new DesiredCapabilities();
	                if(config.SELENIUM_DRIVER_PATH.isEmpty()){
	                    if(os.indexOf("win") >= 0){
	                        DesireCaps.setCapability(PhantomJSDriverService.PHANTOMJS_EXECUTABLE_PATH_PROPERTY,
	                                "./web_driver/phantomjs.exe");
	                    }else{
	                        DesireCaps.setCapability(PhantomJSDriverService.PHANTOMJS_EXECUTABLE_PATH_PROPERTY,
	                                "./web_driver/phantomjs");
	                    }
	
	                }else {
	                    DesireCaps.setCapability(PhantomJSDriverService.PHANTOMJS_EXECUTABLE_PATH_PROPERTY,
	                            config.SELENIUM_DRIVER_PATH);
	                }
	
	                DesireCaps.setJavascriptEnabled(true);
	                //DesireCaps.setCapability("takesScreenshot", true);
	                mSeleniumDriver = new PhantomJSDriver(DesireCaps);
	                mSeleniumDriver.manage().window().setSize(new Dimension(1920, 1080));
	            }
	            
	        } catch (IllegalStateException e) {
	            mLogger.error("Not found SeleniumDriver");
	            return false;
	        }
	    }
        return true;
    }

    private static void quitSelenium() {
        if (mSeleniumDriver != null) {
            mSeleniumDriver.quit();
            mSeleniumDriver = null;
        }
    }


	public void clear() {
		if(mSeleniumDriver != null) {
            ArrayList<String> tab = new ArrayList<String>(mSeleniumDriver.getWindowHandles());
            
//            if(mSeleniumDriver.getClass() == PhantomJSDriver.class) {
//            	((PhantomJSDriver) mSeleniumDriver).executeScript("window.open()", "");
//            }else if(mSeleniumDriver.getClass() == ChromeDriver.class) {
//            	JavascriptExecutor jse = (JavascriptExecutor) mSeleniumDriver;
//                jse.executeScript("window.open()", "");
//            }
            JavascriptExecutor jse = (JavascriptExecutor) mSeleniumDriver;
            jse.executeScript("window.open()", "");
            
            for(int i = 0 ; i < tab.size() ; i ++) {
            	mSeleniumDriver.switchTo().window(tab.get(i));
            	if((tab.size() -1) > i) mSeleniumDriver.close();
            }
		}
	}
}