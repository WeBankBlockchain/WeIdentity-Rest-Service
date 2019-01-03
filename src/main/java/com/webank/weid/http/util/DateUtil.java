package com.webank.weid.http.util;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.apache.commons.lang3.StringUtils;

import com.webank.weid.http.exception.BizException;

public class DateUtil {

	
	/** yyyy-MM-dd HH:mm:ss **/
	public final static String YMDHMS = "yyyy-MM-dd HH:mm:ss";
	
	/** yyyyMMddHHmmss **/
	public final static String YMDHMS_SIMPLE = "yyyyMMddHHmmss";
	
	/** yyyyMMdd **/
	public final static String YMD_SIMPLE = "yyyyMMdd";
	
	/** yyMMdd **/
	public final static String YMD_SIMPLE_0 = "yyMMdd";
	
	/**
	 * get current date
	 * @author darwin du
	 * @return Date
	 */
	public static Date getCurrentDate() {
		return new Date();
	}

	/**
	 * get current date time
	 * @author darwin du
	 * @return Long
	 */
	public static Long getCurrentDateTime() {
		return new Date().getTime();
	}
	
	/**
	 * get designation date
	 * @author darwin du
	 * @param time this is Long
	 * @return Date
	 */
	public static Date getAppointDate(long time) {
		return new Date(time);
	}
	
	/**
	 * date to string
	 * @author darwin du
	 * @param date this is Date
	 * @param format this is format
	 * @return
	 */
	public static String dateToStr(Date date, String format) {
		if(date == null || StringUtils.isBlank(format)) {
			return null;
		}
		return new SimpleDateFormat(format).format(date);
	}

	/**
	 * string to date
	 * @param date
	 * @param format
	 * @return
	 */
	public static Date strToDate(String date, String format) {
		if(StringUtils.isBlank(date) || StringUtils.isBlank(format)) {
			return null;
		}
		try {
			return new SimpleDateFormat(format).parse(date);
		} catch (ParseException e) {
			throw new BizException("[strToDate]: string to date is exception.", e);
		}
	}

}
