package com.example;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class DateParser {

    public static final SimpleDateFormat PR_DATE_FORMAT = createDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
    public static final SimpleDateFormat BASIC_DATE_FORMAT = createDateFormat("dd-MM-yyyy");

    public Date parseDate(SimpleDateFormat format, String dateString) {
        try {
            return format.parse(dateString);
        } catch (ParseException e) {
            throw new RuntimeException("Error parsing date", e);
        }
    }

    private static SimpleDateFormat createDateFormat(String pattern) {
        SimpleDateFormat dateFormat = new SimpleDateFormat(pattern);
        dateFormat.setLenient(false);
        return dateFormat;
    }
}
