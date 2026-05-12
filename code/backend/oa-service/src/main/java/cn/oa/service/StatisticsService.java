package cn.oa.service;

import java.util.Map;
import java.util.List;

public interface StatisticsService {
    Map<String, Object> getDashboardStats(String period);
}
