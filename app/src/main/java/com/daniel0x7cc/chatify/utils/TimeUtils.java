package com.daniel0x7cc.chatify.utils;

import android.content.Context;
import android.text.format.DateFormat;

import com.daniel0x7cc.chatify.App;
import com.daniel0x7cc.chatify.R;

import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

public class TimeUtils {

    private static final int SECOND_MILLIS = 1000;
    private static final int MINUTE_MILLIS = 60 * SECOND_MILLIS;
    private static final int HOUR_MILLIS = 60 * MINUTE_MILLIS;
    private static final int DAY_MILLIS = 24 * HOUR_MILLIS;

    public static String getTimeAgo(long time, Context ctx) {
        if (time < 1000000000000L) {
            // if timestamp given in seconds, convert to millis
            time *= 1000;
        }

        long now = System.currentTimeMillis();
        if (time > now || time <= 0) {
            return null;
        }

        Date date = new Date(time);

        if(isYesterday(date)){
            return ctx.getString(R.string.yesterday);
        }

        // TODO: localize
        final long diff = now - time;
        if (diff < MINUTE_MILLIS) {
            return ctx.getString(R.string.online_now);
        } else if (diff < 2 * MINUTE_MILLIS) {
            return ctx.getString(R.string.one_minute_ago);
        } else if (diff < 50 * MINUTE_MILLIS) {
            return diff / MINUTE_MILLIS + " " + ctx.getString(R.string.minutes_ago);
        } else if (diff < 90 * MINUTE_MILLIS) {
            return ctx.getString(R.string.one_hour_ago);
        } else if (diff < 24 * HOUR_MILLIS) {
            return diff / HOUR_MILLIS + " " + ctx.getString(R.string.minutes_ago);
        } else if (diff < 48 * HOUR_MILLIS) {
            return ctx.getString(R.string.yesterday);
        } else {
            return diff / DAY_MILLIS + " " + ctx.getString(R.string.days_ago);
        }
    }

    private static boolean isYesterday(Date date){
        Calendar cal1 = Calendar.getInstance();
        cal1.add(Calendar.DAY_OF_YEAR, -1);

        Calendar c2 = Calendar.getInstance();
        c2.setTime(date);

        return (cal1.get(Calendar.YEAR) == c2.get(Calendar.YEAR)
                && cal1.get(Calendar.DAY_OF_YEAR) == c2.get(Calendar.DAY_OF_YEAR));
    }

    private static java.text.DateFormat dateUTCFormat;

    private TimeUtils() {
    }

    public static String getDateUTC() {
        if (dateUTCFormat == null) {
            dateUTCFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault());
            dateUTCFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
        }
        return dateUTCFormat.format(new Date());
    }

    public static final Map<String, Long> times = new LinkedHashMap<>();

    static {
        times.put(App.getContext().getResources().getString(R.string.time_year), TimeUnit.DAYS.toMillis(365));
        times.put(App.getContext().getResources().getString(R.string.time_month), TimeUnit.DAYS.toMillis(30));
        times.put(App.getContext().getResources().getString(R.string.time_week), TimeUnit.DAYS.toMillis(7));
        times.put(App.getContext().getResources().getString(R.string.time_day), TimeUnit.DAYS.toMillis(1));
        times.put(App.getContext().getResources().getString(R.string.time_hour), TimeUnit.HOURS.toMillis(1));
        times.put(App.getContext().getResources().getString(R.string.time_minute), TimeUnit.MINUTES.toMillis(1));
        times.put(App.getContext().getResources().getString(R.string.time_second), TimeUnit.SECONDS.toMillis(1));
    }

    public static final Map<String, Long> dates = new LinkedHashMap<>();

    static {
        dates.put(App.getContext().getResources().getString(R.string.time_day), TimeUnit.DAYS.toMillis(1));
        dates.put(App.getContext().getResources().getString(R.string.time_hour), TimeUnit.HOURS.toMillis(1));
        dates.put(App.getContext().getResources().getString(R.string.time_minute), TimeUnit.MINUTES.toMillis(1));
        dates.put(App.getContext().getResources().getString(R.string.time_second), TimeUnit.SECONDS.toMillis(1));
    }

    public static String toRelative(long duration, int maxLevel, int where) {
        StringBuilder res = new StringBuilder();
        int level = 0;

        if(where == 0) {
            for (Map.Entry<String, Long> time : times.entrySet()) {
                long timeDelta = duration / time.getValue();
                if (timeDelta > 0) {
                    res.append(timeDelta)
                            .append(" ");
                    if (time.getKey().contains("mês") && timeDelta > 1) {
                        res.append("mes");
                    } else {
                        res.append(time.getKey());
                    }
                    if (time.getKey().endsWith("s")) {
                        res.append(timeDelta > 1 ? "es" : "")
                                .append(", ");
                    } else {
                        res.append(timeDelta > 1 ? "s" : "")
                                .append(", ");
                    }

                    duration -= time.getValue() * timeDelta;
                    level++;
                }
                if (level == maxLevel) {
                    break;
                }
            }
        } else {
            for (Map.Entry<String, Long> time : dates.entrySet()) {
                long timeDelta = duration / time.getValue();
                if (timeDelta > 0) {
                    res.append(timeDelta)
                            .append(" ");
                    if (time.getKey().contains("mês") && timeDelta > 1) {
                        res.append("mes");
                    } else {
                        res.append(time.getKey());
                    }
                    if (time.getKey().endsWith("s")) {
                        res.append(timeDelta > 1 ? "es" : "")
                                .append(", ");
                    } else {
                        if(time.getKey().contains(App.getContext().getResources().getString(R.string.time_minute))) {
                            res.append(timeDelta > 1 ? "" : "")
                                    .append(", ");
                        } else {
                            res.append(timeDelta > 1 ? "s" : "")
                                    .append(", ");
                        }
                    }

                    duration -= time.getValue() * timeDelta;
                    level++;
                }
                if (level == maxLevel) {
                    break;
                }
            }

        }

        if ("".equals(res.toString())) {
            return App.getContext().getResources().getString(R.string.zero_seconds_ago);
        } else {
            res.setLength(res.length() - 2);
            return res.toString();
        }
    }

    public static String toRelative(long duration) {
        return toRelative(duration, times.size(), 0);
    }

    public static String toRelative(Date start, Date end) {
        return toRelative(end.getTime() - start.getTime());
    }

    public static String convertFromDatabaseString(String sqlDateTime) {
        String result = "";

        try {
            SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault());

            SimpleDateFormat formatGmt = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault());
            formatGmt.setTimeZone(TimeZone.getTimeZone("GMT"));

            Date timeNow = format.parse(formatGmt.format(new Date()));

            sqlDateTime = sqlDateTime.replace("T", " ");

            Date finalDate = Timestamp.valueOf(sqlDateTime);

            // Calculate how much time ago the post was
            result = TimeUtils.toRelative(finalDate, timeNow);
        } catch (ParseException e) {
            LogUtils.e("Erro ao formatar data.", e);
        }

        // Check if string is "Zero seconds ago" -- if not, parse it
        if (!result.equals(App.getInstance().getString(R.string.zero_seconds_ago))) {
            // Filter only first string before , to show
            String firstString = result.split(",", 2)[0];
            return firstString + " " + App.getContext().getResources().getString(R.string.time_ago);
        } else {
            return result;
        }
    }

    public static String ConvertFromDatabaseStringToJavaShortDate(String sqlDateTime) {
        String result = "";
        SimpleDateFormat input = new SimpleDateFormat("yyyy-MM-dd'T'hh:mm:ss");
        SimpleDateFormat output = new SimpleDateFormat("dd/MM/yy");

        input.setTimeZone(TimeZone.getTimeZone("UTC"));
        Date date = null;
        try {
            date = input.parse(sqlDateTime);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        input.setTimeZone(TimeZone.getDefault());

        try {
            result = output.format(date);
        } catch(Exception e) {
            e.printStackTrace();
        }

        return result;
    }

    public static String getDisplayTimeOrDate(Context context, long milli) {
        Date date = new Date(milli);

        if(isYesterday(date)) { // between 24 hours and 48 hours (yesterday)
            return context.getString(R.string.chat_header_yesterday);
        } else if(isToday(date)) {    // today
            return context.getString(R.string.chat_header_today);
        } else {
            return DateFormat.getDateFormat(context).format(date);
        }
    }

    public static String getDisplayDateTime(Context context, long milli) {
        Date date = new Date(milli);
        if (System.currentTimeMillis() - milli < 60 * 60 * 24 * 1000L) {
            return DateFormat.getTimeFormat(context).format(date);
        }
        return DateFormat.getTimeFormat(context).format(date);
    }

    private static boolean isSameDay(Date date1, Date date2) {
        if (date1 == null || date2 == null) {
            throw new IllegalArgumentException("The dates must not be null");
        }
        Calendar cal1 = Calendar.getInstance();
        cal1.setTime(date1);
        Calendar cal2 = Calendar.getInstance();
        cal2.setTime(date2);
        return isSameDay(cal1, cal2);
    }

    private static boolean isSameDay(Calendar cal1, Calendar cal2) {
        if (cal1 == null || cal2 == null) {
            throw new IllegalArgumentException("The dates must not be null");
        }
        return (cal1.get(Calendar.ERA) == cal2.get(Calendar.ERA) &&
                cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR));
    }

    private static boolean isToday(Date date) {
        return isSameDay(date, Calendar.getInstance().getTime());
    }

}
