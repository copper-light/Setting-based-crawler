package com.onycom.common;

import java.io.IOException;

import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.DailyRollingFileAppender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;

public class CrawlerLog {
	static Logger mLogger;
	
	public static String LOGGER_NAME = "DEFALUT";
	
	public static void SetName(String name){
		LOGGER_NAME = name.replace(" ","_");
	}
	
	public static Logger GetInstanceSysout(Class logClass) {
		String fileName = null;
		String logName = LOGGER_NAME; 
		mLogger = Logger.getLogger(logClass);
		// 로그 파일 대한 패턴을 정의
		String pattern = "%d{yyyy-MM-dd HH:mm:ss} %-5p %c{1}:%L - %m%n";
		PatternLayout layout = new PatternLayout(pattern);

		// 날짜 패턴에 따라 추가될 파일 이름
		String datePattern = ".yyyyMMdd";

		ConsoleAppender appender = new ConsoleAppender(layout);
		appender.setThreshold(Level.ALL);
		mLogger.addAppender(appender);
		return mLogger;
	}
	
	public static Logger GetInstance(Class logClass) {
		String fileName = null;
		String logName = LOGGER_NAME; 
		mLogger = Logger.getLogger(logClass);
		// 로그 파일 대한 패턴을 정의
		String pattern = "%d{yyyy-MM-dd HH:mm:ss} %-5p %c{1}:%L - %m%n";
		PatternLayout layout = new PatternLayout(pattern);

		// 날짜 패턴에 따라 추가될 파일 이름
		String datePattern = ".yyyyMMdd";

		DailyRollingFileAppender appender = null;
		try {
			fileName = "./log/" + logName + "/" + logName +"_error.log";
			appender = new DailyRollingFileAppender(layout, fileName, datePattern);
			appender.setThreshold(Level.ERROR);
			mLogger.addAppender(appender);

			fileName = "./log/" + logName + "/" + logName +"_all.log";
			appender = new DailyRollingFileAppender(layout, fileName, datePattern);
			appender.setThreshold(Level.ALL);
			mLogger.addAppender(appender);
		} catch (IOException ioe) {
			ioe.printStackTrace();
		}
		ConsoleAppender appender2 = new ConsoleAppender(layout);
		appender.setThreshold(Level.ALL);
		mLogger.addAppender(appender2);
		return mLogger;
	}
}