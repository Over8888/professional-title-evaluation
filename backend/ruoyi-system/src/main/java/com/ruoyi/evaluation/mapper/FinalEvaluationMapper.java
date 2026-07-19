package com.ruoyi.evaluation.mapper;

import java.util.List;
import com.ruoyi.evaluation.domain.FinalEvaluation;

public interface FinalEvaluationMapper
{
    List<FinalEvaluation> selectFinalEvaluationList(FinalEvaluation finalEvaluation);
    int countFinalEvaluationByActivityId(Long activityId);
    FinalEvaluation selectFinalEvaluationById(Long id);
    FinalEvaluation selectFinalEvaluationByActivityCandidateId(Long activityCandidateId);
    int insertFinalEvaluation(FinalEvaluation finalEvaluation);
    int updateFinalEvaluation(FinalEvaluation finalEvaluation);
    int deleteFinalEvaluationByActivityId(Long activityId);
    int deleteFinalEvaluationByIds(Long[] ids);
}
