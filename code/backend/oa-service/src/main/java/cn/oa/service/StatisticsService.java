package cn.oa.service;

import java.util.Map;
import java.time.LocalDate;

public interface StatisticsService {
    Map<String, Object> getDashboardStats(String period, Integer year, LocalDate date);
}
