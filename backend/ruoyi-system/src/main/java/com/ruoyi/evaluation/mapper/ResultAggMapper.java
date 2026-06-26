package com.ruoyi.evaluation.mapper;

import java.util.List;
import java.util.Date;
import com.ruoyi.evaluation.domain.ResultAgg;

public interface ResultAggMapper
{
    List<ResultAgg> selectResultAggList(ResultAgg resultAgg);
    int countResultAggByActivityId(Long activityId);
    Date selectLatestCalculatedAt(Long activityId);
    int batchInsertResultAgg(List<ResultAgg> resultAggList);
    int deleteResultAggByActivityId(Long activityId);
}
