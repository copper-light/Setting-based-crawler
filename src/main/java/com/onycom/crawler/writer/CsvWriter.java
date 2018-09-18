package com.onycom.crawler.writer;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import com.onycom.crawler.data.CollectRecode;
import com.onycom.crawler.data.Config;
import com.onycom.crawler.data.Contents;
import com.onycom.crawler.data.KeyValue;

import au.com.bytecode.opencsv.CSVWriter;

/**
 * CSV 파일 저장 구현체. CSVWriter 라이브러리 사용
 * */
public class CsvWriter implements Writer{
	static Logger mLogger = Logger.getLogger(DBWriter.class);
	
	Config mConfig;
	CSVWriter mCSVWriter;
	SimpleDateFormat DATE_FORMAT; 

	/** 
	 * 콘텐츠를 저장할 파일 목록 <콘텐츠 명칭, 파일> 
	 * */
	Map<String, CSVFile> mAryCSV;
	
	public CsvWriter(){
		mAryCSV = new HashMap<String, CSVFile>();
		DATE_FORMAT = new SimpleDateFormat("yyMMddHHmmss");
	}

	public void closeAll(){
		Iterator<CSVFile> list = mAryCSV.values().iterator();
		CSVFile cw;
		while(list.hasNext()){
			cw = list.next();
			cw.close();
		}
		mAryCSV.clear();
	}

	public void openAll() throws Exception {
		File outputDir;
		if(mConfig.OUTPUT_FILE_PATH != null && mConfig.OUTPUT_FILE_PATH.length() > 0){
			outputDir = new File(mConfig.OUTPUT_FILE_PATH);
		}else{
			outputDir = new File("./output");
		}
		
		if(!outputDir.exists()){
			outputDir.mkdirs();
		}
		
		List<CollectRecode> recodes = mConfig.getCollects();
		String filePath, name;
		CSVFile cw;
		String strDate = DATE_FORMAT.format(new Date());
		for(CollectRecode r : recodes){
			name = r.getName();
			cw = mAryCSV.get(name);
			if(cw == null){
				filePath = outputDir.getPath() + "/" + name + "_" + strDate +".csv";
				cw = new CSVFile(filePath);
				mAryCSV.put(name, cw);
			}
			cw.open();
		}
	}
	
	public void open() throws Exception {
		openAll();
	}

	public void close() throws Exception {
		closeAll();
	}

	public void setConfig(Config config) {
		mConfig = config;
		List<CollectRecode> collects = config.getCollects();
		String colName;
		Contents contents;
		try {
			openAll();
			for(CollectRecode c : collects){
				// TABLE NAME
				contents = new Contents(c.getName(), c.getColumns().size()); 
				for(CollectRecode.Column col : c.getColumns()){
					colName = col.getDataName(); // 컬럼 명
					contents.add("", colName);
				}
				write(contents);
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			closeAll();
		}
	}

	public int write(String... values) throws Exception {
//		if(values == null || values.length <= 1) return 1;
//		
//		CSVWriter cw = mAryCSV.get(values[0]);
//		if(cw == null) return 0;
//		values[1,10];
//		for(int i = 1 ; i < values.length ; i++){
//			
//		}
//		
		return 0;
	}

	public int write(Contents contents) throws Exception {
		CSVFile cw = mAryCSV.get(contents.getName());
		if(cw == null) return 0;
		int len = contents.size();
		String[] row = new String[len];
		KeyValue kv;
		for(int i = 0 ; i < len ; i++){
			kv = contents.get(i);
			if(kv != null){
				row[i] = kv.value();
			}else{
				row[i] = "";
			}
		}
		cw.write(row);
		return 1;
	}

	public int write(List<Contents> aryContents) throws Exception {
		// TODO Auto-generated method stub
		return 0;
	}
	
	class CSVFile {
		int writeCount;
		String fileName;
		CSVWriter cw;
		
		public CSVFile(String filename){
			this.fileName= filename;
			this.writeCount = 0;
			cw = null;
		}
		
		public void open(){
			OutputStreamWriter out;
			try {
				out = new OutputStreamWriter(new FileOutputStream(this.fileName, true), "utf-8");
				this.cw = new CSVWriter(out, ',', '"');
			} catch (UnsupportedEncodingException e) {
				e.printStackTrace();
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			}
		}
		
		public void close(){
			try {
				if(cw != null){
					cw.flush();
					cw.close();
					cw = null;
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
		public void write(String... cols){
			if(cw != null){
				cw.writeNext(cols);
				writeCount++;
			}
		}
		
		public void clear(){
			this.writeCount = 0;
			this.fileName = null;
			this.close();
		}
	}
}