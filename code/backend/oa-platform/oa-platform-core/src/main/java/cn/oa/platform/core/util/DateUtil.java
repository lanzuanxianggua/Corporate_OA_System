package cn.oa.platform.core.util;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Date;

/**
 * 日期工具类
 *
 * @author oa-platform
 */
public final class DateUtil {

    private DateUtil() {
        // 私有构造函数，防止实例化
    }

    // ==================== 日期格式化器 ====================

    /**
     * 日期格式化器：yyyy-MM-dd
     */
    public static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    /**
     * 时间格式化器：HH:mm:ss
     */
    public static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss");

    /**
     * 日期时间格式化器：yyyy-MM-dd HH:mm:ss
     */
    public static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * 紧凑日期时间格式化器：yyyyMMddHHmmss
     */
    public static final DateTimeFormatter COMPACT_DATETIME_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    /**
     * 时间戳格式化器：yyyyMMddHHmmssSSS
     */
    public static final DateTimeFormatter TIMESTAMP_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS");

    // ==================== 当前时间获取 ====================

    /**
     * 获取当前日期
     */
    public static LocalDate currentDate() {
        return LocalDate.now();
    }

    /**
     * 获取当前时间
     */
    public static LocalTime currentTime() {
        return LocalTime.now();
    }

    /**
     * 获取当前日期时间
     */
    public static LocalDateTime currentDateTime() {
        return LocalDateTime.now();
    }

    /**
     * 获取当前时间戳（毫秒）
     */
    public static long currentTimestamp() {
        return System.currentTimeMillis();
    }

    /**
     * 获取当前时间戳字符串
     */
    public static String currentTimestampStr() {
        return format(currentDateTime(), TIMESTAMP_FORMATTER);
    }

    // ==================== 格式化 ====================

    /**
     * 格式化日期
     *
     * @param date    日期
     * @param pattern 格式
     * @return 格式化字符串
     */
    public static String format(LocalDate date, String pattern) {
        if (date == null) {
            return null;
        }
        return date.format(DateTimeFormatter.ofPattern(pattern));
    }

    /**
     * 格式化日期时间
     *
     * @param dateTime 日期时间
     * @param pattern  格式
     * @return 格式化字符串
     */
    public static String format(LocalDateTime dateTime, String pattern) {
        if (dateTime == null) {
            return null;
        }
        return dateTime.format(DateTimeFormatter.ofPattern(pattern));
    }

    /**
     * 格式化日期（使用默认格式 yyyy-MM-dd）
     */
    public static String formatDate(LocalDate date) {
        return date == null ? null : date.format(DATE_FORMATTER);
    }

    /**
     * 格式化时间（使用默认格式 HH:mm:ss）
     */
    public static String formatTime(LocalTime time) {
        return time == null ? null : time.format(TIME_FORMATTER);
    }

    /**
     * 格式化日期时间（使用默认格式 yyyy-MM-dd HH:mm:ss）
     */
    public static String formatDateTime(LocalDateTime dateTime) {
        return dateTime == null ? null : dateTime.format(DATETIME_FORMATTER);
    }

    /**
     * 格式化日期时间（使用指定格式化器）
     */
    public static String format(LocalDateTime dateTime, DateTimeFormatter formatter) {
        return dateTime == null ? null : dateTime.format(formatter);
    }

    // ==================== 解析 ====================

    /**
     * 解析日期
     *
     * @param dateStr 日期字符串
     * @param pattern 格式
     * @return 日期
     */
    public static LocalDate parseDate(String dateStr, String pattern) {
        if (dateStr == null || dateStr.isEmpty()) {
            return null;
        }
        return LocalDate.parse(dateStr, DateTimeFormatter.ofPattern(pattern));
    }

    /**
     * 解析日期时间
     *
     * @param dateTimeStr 日期时间字符串
     * @param pattern     格式
     * @return 日期时间
     */
    public static LocalDateTime parseDateTime(String dateTimeStr, String pattern) {
        if (dateTimeStr == null || dateTimeStr.isEmpty()) {
            return null;
        }
        return LocalDateTime.parse(dateTimeStr, DateTimeFormatter.ofPattern(pattern));
    }

    /**
     * 解析日期（使用默认格式 yyyy-MM-dd）
     */
    public static LocalDate parseDate(String dateStr) {
        if (dateStr == null || dateStr.isEmpty()) {
            return null;
        }
        return LocalDate.parse(dateStr, DATE_FORMATTER);
    }

    /**
     * 解析日期时间（使用默认格式 yyyy-MM-dd HH:mm:ss）
     */
    public static LocalDateTime parseDateTime(String dateTimeStr) {
        if (dateTimeStr == null || dateTimeStr.isEmpty()) {
            return null;
        }
        return LocalDateTime.parse(dateTimeStr, DATETIME_FORMATTER);
    }

    // ==================== 类型转换 ====================

    /**
     * Date 转 LocalDateTime
     */
    public static LocalDateTime toLocalDateTime(Date date) {
        if (date == null) {
            return null;
        }
        return date.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
    }

    /**
     * Date 转 LocalDate
     */
    public static LocalDate toLocalDate(Date date) {
        if (date == null) {
            return null;
        }
        return date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
    }

    /**
     * LocalDateTime 转 Date
     */
    public static Date toDate(LocalDateTime localDateTime) {
        if (localDateTime == null) {
            return null;
        }
        return Date.from(localDateTime.atZone(ZoneId.systemDefault()).toInstant());
    }

    /**
     * LocalDate 转 Date
     */
    public static Date toDate(LocalDate localDate) {
        if (localDate == null) {
            return null;
        }
        return Date.from(localDate.atStartOfDay(ZoneId.systemDefault()).toInstant());
    }

    /**
     * 时间戳转 LocalDateTime
     */
    public static LocalDateTime toLocalDateTime(long timestamp) {
        return LocalDateTime.ofInstant(Instant.ofEpochMilli(timestamp), ZoneId.systemDefault());
    }

    /**
     * LocalDateTime 转时间戳
     */
    public static long toTimestamp(LocalDateTime localDateTime) {
        return localDateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
    }

    // ==================== 日期计算 ====================

    /**
     * 计算两个日期之间的天数
     */
    public static long daysBetween(LocalDate start, LocalDate end) {
        if (start == null || end == null) {
            return 0;
        }
        return ChronoUnit.DAYS.between(start, end);
    }

    /**
     * 计算两个日期时间之间的小时数
     */
    public static long hoursBetween(LocalDateTime start, LocalDateTime end) {
        if (start == null || end == null) {
            return 0;
        }
        return ChronoUnit.HOURS.between(start, end);
    }

    /**
     * 计算两个日期时间之间的分钟数
     */
    public static long minutesBetween(LocalDateTime start, LocalDateTime end) {
        if (start == null || end == null) {
            return 0;
        }
        return ChronoUnit.MINUTES.between(start, end);
    }

    /**
     * 添加天数
     */
    public static LocalDateTime plusDays(LocalDateTime dateTime, long days) {
        return dateTime == null ? null : dateTime.plusDays(days);
    }

    /**
     * 添加小时
     */
    public static LocalDateTime plusHours(LocalDateTime dateTime, long hours) {
        return dateTime == null ? null : dateTime.plusHours(hours);
    }

    /**
     * 添加分钟
     */
    public static LocalDateTime plusMinutes(LocalDateTime dateTime, long minutes) {
        return dateTime == null ? null : dateTime.plusMinutes(minutes);
    }

    /**
     * 减去天数
     */
    public static LocalDateTime minusDays(LocalDateTime dateTime, long days) {
        return dateTime == null ? null : dateTime.minusDays(days);
    }

    /**
     * 获取当天的开始时间 00:00:00
     */
    public static LocalDateTime startOfDay(LocalDate date) {
        return date == null ? null : date.atStartOfDay();
    }

    /**
     * 获取当天的结束时间 23:59:59
     */
    public static LocalDateTime endOfDay(LocalDate date) {
        return date == null ? null : date.atTime(23, 59, 59, 999999999);
    }

    /**
     * 获取当月第一天
     */
    public static LocalDate firstDayOfMonth(LocalDate date) {
        return date == null ? null : date.withDayOfMonth(1);
    }

    /**
     * 获取当月最后一天
     */
    public static LocalDate lastDayOfMonth(LocalDate date) {
        return date == null ? null : date.withDayOfMonth(date.lengthOfMonth());
    }

    // ==================== 判断方法 ====================

    /**
     * 判断是否为今天
     */
    public static boolean isToday(LocalDate date) {
        return date != null && date.equals(currentDate());
    }

    /**
     * 判断是否为今天
     */
    public static boolean isToday(LocalDateTime dateTime) {
        return dateTime != null && dateTime.toLocalDate().equals(currentDate());
    }

    /**
     * 判断日期是否在指定范围内
     */
    public static boolean isBetween(LocalDate date, LocalDate start, LocalDate end) {
        if (date == null || start == null || end == null) {
            return false;
        }
        return !date.isBefore(start) && !date.isAfter(end);
    }

    /**
     * 判断是否为工作日（周一到周五）
     */
    public static boolean isWorkday(LocalDate date) {
        if (date == null) {
            return false;
        }
        DayOfWeek dayOfWeek = date.getDayOfWeek();
        return dayOfWeek != DayOfWeek.SATURDAY && dayOfWeek != DayOfWeek.SUNDAY;
    }

    /**
     * 判断是否为周末
     */
    public static boolean isWeekend(LocalDate date) {
        return !isWorkday(date);
    }

    // ==================== 年龄计算 ====================

    /**
     * 计算年龄
     */
    public static int calculateAge(LocalDate birthDate) {
        if (birthDate == null) {
            return 0;
        }
        return Period.between(birthDate, currentDate()).getYears();
    }

    /**
     * 计算年龄（指定日期）
     */
    public static int calculateAge(LocalDate birthDate, LocalDate referenceDate) {
        if (birthDate == null || referenceDate == null) {
            return 0;
        }
        return Period.between(birthDate, referenceDate).getYears();
    }
}
