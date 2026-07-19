package com.ruoyi.evaluation.service;

import java.util.List;
import java.util.Map;
import com.ruoyi.evaluation.domain.FinalEvaluation;

public interface IFinalEvaluationService
{
    List<FinalEvaluation> selectFinalEvaluationList(FinalEvaluation finalEvaluation);
    Map<String, Object> summary(Long activityId);
    int generateFromResult(Long activityId, String username, boolean replaceExisting);
    int updateFinalEvaluation(FinalEvaluation finalEvaluation, String username);
    int saveActivityFinalEvaluations(Long activityId, List<FinalEvaluation> evaluations, String signedBy, String confirmRemark, String username);
    int confirmActivity(Long activityId, String username);
    int deleteFinalEvaluationByIds(Long[] ids);
}
