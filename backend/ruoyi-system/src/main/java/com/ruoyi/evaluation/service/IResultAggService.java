package com.ruoyi.evaluation.service;

import java.util.List;
import java.util.Map;
import jakarta.servlet.http.HttpServletResponse;
import com.ruoyi.evaluation.domain.ResultAgg;

public interface IResultAggService
{
    Map<String, Object> calculate(Long activityId, String username, boolean force);
    Map<String, Object> summary(Long activityId);
    Map<String, Object> voteSummary(Long activityId);
    List<Map<String, Object>> selectResultAggList(ResultAgg resultAgg);
    List<ResultAgg> selectCandidateResultList(ResultAgg resultAgg);
    Map<String, Object> publicResult(Long activityId);
    void exportResult(HttpServletResponse response, Long activityId, String exportType, String username);
}
