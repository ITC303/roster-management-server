package com.project.cafeemployeemanagement.util;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class utils {
    private static SimpleDateFormat simpleDateFormat = new SimpleDateFormat();

    public static String formatDate(Date date) {
        simpleDateFormat.applyPattern(Constants.DATE_FORMAT);
        return simpleDateFormat.format(date);
    }

    public static String formatTime(Date date) {
        simpleDateFormat.applyPattern(Constants.TIME_FORMAT);
        return simpleDateFormat.format(date);
    }

    public static String formatDateTime(Date date) {
        simpleDateFormat.applyPattern(Constants.DATE_TIME_FORMAT);
        return simpleDateFormat.format(date);
    }

    public static Date getDateTimeFromDateAndTime(Date date, Date time) {
        try
        {
            SimpleDateFormat formatter = new SimpleDateFormat(Constants.DATE_TIME_FORMAT);
            String strDateTime = String.format("%s %s", formatDate(date), formatTime(time));
            return formatter.parse(strDateTime);
        } catch (ParseException pe) {
            pe.printStackTrace();
        }
        return null;
    }
}