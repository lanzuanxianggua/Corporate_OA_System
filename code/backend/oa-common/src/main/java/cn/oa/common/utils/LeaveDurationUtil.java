package cn.oa.common.utils;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;

public final class LeaveDurationUtil {

    private LeaveDurationUtil() {
    }

    public static BigDecimal calculateLeaveDays(LocalDateTime startTime,
                                                LocalDateTime endTime,
                                                String leavePeriod) {
        if (startTime == null || endTime == null) {
            return BigDecimal.ZERO;
        }
        LocalDate startDate = startTime.toLocalDate();
        LocalDate endDate = endTime.toLocalDate();
        if (endDate.isBefore(startDate)) {
            return BigDecimal.ZERO;
        }

        String period = leavePeriod != null ? leavePeriod : "full";
        long fullWeekdays = 0;
        for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
            DayOfWeek dayOfWeek = date.getDayOfWeek();
            if (dayOfWeek != DayOfWeek.SATURDAY && dayOfWeek != DayOfWeek.SUNDAY) {
                fullWeekdays++;
            }
        }

        if ("full".equals(period) || fullWeekdays == 0) {
            return BigDecimal.valueOf(fullWeekdays);
        }

        boolean sameDay = startDate.equals(endDate);
        if (sameDay) {
            return BigDecimal.valueOf(0.5);
        }
        return BigDecimal.valueOf(fullWeekdays - 1).add(BigDecimal.valueOf(0.5));
    }

    public static long calculateBusinessTripDays(LocalDateTime startTime, LocalDateTime endTime) {
        if (startTime == null || endTime == null) {
            return 0L;
        }
        LocalDate startDate = startTime.toLocalDate();
        LocalDate endDate = endTime.toLocalDate();
        if (endDate.isBefore(startDate)) {
            return 0L;
        }

        long weekdays = 0L;
        for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
            DayOfWeek dayOfWeek = date.getDayOfWeek();
            if (dayOfWeek != DayOfWeek.SATURDAY && dayOfWeek != DayOfWeek.SUNDAY) {
                weekdays++;
            }
        }
        return weekdays;
    }
}
