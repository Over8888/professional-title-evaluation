package com.ruoyi.evaluation.service.impl;

import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.evaluation.domain.Activity;
import com.ruoyi.evaluation.domain.FinalEvaluation;
import com.ruoyi.evaluation.domain.ResultAgg;
import com.ruoyi.evaluation.mapper.ActivityMapper;
import com.ruoyi.evaluation.mapper.FinalEvaluationMapper;
import com.ruoyi.evaluation.mapper.ResultAggMapper;
import com.ruoyi.evaluation.service.IFinalEvaluationService;

@Service
public class FinalEvaluationServiceImpl implements IFinalEvaluationService
{
    private static final String STATUS_DRAFT = "DRAFT";
    private static final String STATUS_CONFIRMED = "CONFIRMED";

    @Autowired
    private FinalEvaluationMapper finalEvaluationMapper;

    @Autowired
    private ResultAggMapper resultAggMapper;

    @Autowired
    private ActivityMapper activityMapper;

    @Override
    public List<FinalEvaluation> selectFinalEvaluationList(FinalEvaluation finalEvaluation)
    {
        return finalEvaluationMapper.selectFinalEvaluationList(finalEvaluation);
    }

    @Override
    public Map<String, Object> summary(Long activityId)
    {
        requireActivityId(activityId);
        FinalEvaluation query = new FinalEvaluation();
        query.setActivityId(activityId);
        List<FinalEvaluation> rows = finalEvaluationMapper.selectFinalEvaluationList(query);
        long pass = rows.stream().filter(row -> "PASS".equals(row.getFinalResult())).count();
        long reject = rows.stream().filter(row -> "REJECT".equals(row.getFinalResult())).count();
        long confirmed = rows.stream().filter(row -> STATUS_CONFIRMED.equals(row.getConfirmStatus())).count();
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("activityId", activityId);
        data.put("total", rows.size());
        data.put("pass", pass);
        data.put("reject", reject);
        data.put("confirmed", confirmed);
        data.put("pending", rows.size() - confirmed);
        data.put("ready", !rows.isEmpty());
        return data;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int generateFromResult(Long activityId, String username, boolean replaceExisting)
    {
        Activity activity = requireActivityWithStatus(activityId, "CALCULATED", "FinalEvaluation can only be generated after calculation and before confirmation.");
        int existing = finalEvaluationMapper.countFinalEvaluationByActivityId(activityId);
        if (existing > 0 && !replaceExisting)
        {
            throw new ServiceException("FinalEvaluation already exists. Use replaceExisting=true to rebuild it.");
        }
        if (replaceExisting)
        {
            finalEvaluationMapper.deleteFinalEvaluationByActivityId(activityId);
        }
        ResultAgg query = new ResultAgg();
        query.setActivityId(activityId);
        List<ResultAgg> results = resultAggMapper.selectResultAggList(query);
        if (results.isEmpty())
        {
            throw new ServiceException("No calculated result rows found for FinalEvaluation.");
        }
        int inserted = 0;
        for (ResultAgg result : results)
        {
            FinalEvaluation row = new FinalEvaluation();
            row.setActivityId(activityId);
            row.setActivityCandidateId(result.getActivityCandidateId());
            row.setResultAggId(result.getId());
            row.setFinalResult(result.getFinalResult());
            row.setConfirmStatus(STATUS_DRAFT);
            row.setSignatureStatus("PENDING");
            row.setCreateBy(username);
            inserted += finalEvaluationMapper.insertFinalEvaluation(row);
        }
        return inserted;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int updateFinalEvaluation(FinalEvaluation finalEvaluation, String username)
    {
        if (finalEvaluation == null || finalEvaluation.getId() == null)
        {
            throw new ServiceException("FinalEvaluation ID is required.");
        }
        FinalEvaluation existing = finalEvaluationMapper.selectFinalEvaluationById(finalEvaluation.getId());
        if (existing == null)
        {
            throw new ServiceException("FinalEvaluation does not exist.");
        }
        requireActivityWithStatus(existing.getActivityId(), "CALCULATED", "FinalEvaluation can only be edited before confirmation.");
        validateFinalResult(finalEvaluation.getFinalResult());
        validateConfirmStatus(finalEvaluation.getConfirmStatus());
        finalEvaluation.setUpdateBy(username);
        return finalEvaluationMapper.updateFinalEvaluation(finalEvaluation);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int saveActivityFinalEvaluations(Long activityId, List<FinalEvaluation> evaluations, String signedBy,
            String confirmRemark, String username)
    {
        requireActivityWithStatus(activityId, "CALCULATED", "FinalEvaluation can only be edited before confirmation.");
        FinalEvaluation query = new FinalEvaluation();
        query.setActivityId(activityId);
        List<FinalEvaluation> existingRows = finalEvaluationMapper.selectFinalEvaluationList(query);
        if (existingRows.isEmpty())
        {
            throw new ServiceException("Generate FinalEvaluation before saving.");
        }

        Map<Long, String> calculatedResults = new HashMap<>();
        ResultAgg resultQuery = new ResultAgg();
        resultQuery.setActivityId(activityId);
        for (ResultAgg result : resultAggMapper.selectResultAggList(resultQuery))
        {
            calculatedResults.put(result.getActivityCandidateId(), result.getFinalResult());
        }

        HashMap<Long, FinalEvaluation> changesById = new HashMap<>();
        if (evaluations != null)
        {
            for (FinalEvaluation evaluation : evaluations)
            {
                if (evaluation != null && evaluation.getId() != null)
                {
                    validateFinalResult(evaluation.getFinalResult());
                    changesById.put(evaluation.getId(), evaluation);
                }
            }
        }

        int updated = 0;
        for (FinalEvaluation existing : existingRows)
        {
            FinalEvaluation change = changesById.get(existing.getId());
            FinalEvaluation update = new FinalEvaluation();
            update.setId(existing.getId());
            update.setFinalResult(change == null ? StringUtils.defaultIfEmpty(existing.getFinalResult(),
                    calculatedResults.get(existing.getActivityCandidateId())) : change.getFinalResult());
            update.setConfirmStatus(STATUS_DRAFT);
            update.setSignedBy(signedBy);
            update.setConfirmRemark(confirmRemark);
            update.setSignatureStatus(StringUtils.isEmpty(signedBy) ? "PENDING" : "SIGNED");
            update.setUpdateBy(username);
            updated += finalEvaluationMapper.updateFinalEvaluation(update);
        }
        return updated;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int confirmActivity(Long activityId, String username)
    {
        requireActivityWithStatus(activityId, "CALCULATED", "Only calculated activities can be confirmed.");
        if (finalEvaluationMapper.countFinalEvaluationByActivityId(activityId) == 0)
        {
            throw new ServiceException("Generate FinalEvaluation before confirmation.");
        }
        FinalEvaluation query = new FinalEvaluation();
        query.setActivityId(activityId);
        List<FinalEvaluation> rows = finalEvaluationMapper.selectFinalEvaluationList(query);
        Date now = new Date();
        int updated = 0;
        for (FinalEvaluation row : rows)
        {
            FinalEvaluation update = new FinalEvaluation();
            update.setId(row.getId());
            update.setConfirmStatus(STATUS_CONFIRMED);
            update.setConfirmedBy(username);
            update.setConfirmedAt(now);
            update.setSignatureStatus(StringUtils.isEmpty(row.getSignedBy()) ? "PENDING" : "SIGNED");
            update.setUpdateBy(username);
            updated += finalEvaluationMapper.updateFinalEvaluation(update);
        }
        Activity activity = new Activity();
        activity.setId(activityId);
        activity.setStatus(STATUS_CONFIRMED);
        activity.setUpdatedBy(username);
        activityMapper.updateActivity(activity);
        return updated;
    }

    @Override
    public int deleteFinalEvaluationByIds(Long[] ids)
    {
        if (ids != null)
        {
            for (Long id : ids)
            {
                FinalEvaluation row = finalEvaluationMapper.selectFinalEvaluationById(id);
                if (row != null)
                {
                    requireActivityWithStatus(row.getActivityId(), "CALCULATED", "FinalEvaluation can only be deleted before confirmation.");
                }
            }
        }
        return finalEvaluationMapper.deleteFinalEvaluationByIds(ids);
    }

    private void requireActivityId(Long activityId)
    {
        if (activityId == null)
        {
            throw new ServiceException("Activity ID is required.");
        }
    }

    private Activity requireActivityWithStatus(Long activityId, String expectedStatus, String message)
    {
        requireActivityId(activityId);
        Activity activity = activityMapper.selectActivityById(activityId);
        if (activity == null)
        {
            throw new ServiceException("Activity does not exist.");
        }
        String status = StringUtils.defaultIfEmpty(activity.getStatus(), "");
        if (!expectedStatus.equals(status))
        {
            throw new ServiceException(message);
        }
        if (resultAggMapper.countResultAggByActivityId(activityId) == 0)
        {
            throw new ServiceException("No calculated results found.");
        }
        return activity;
    }

    private void validateFinalResult(String finalResult)
    {
        if (StringUtils.isEmpty(finalResult))
        {
            return;
        }
        if (!"PASS".equals(finalResult) && !"REJECT".equals(finalResult))
        {
            throw new ServiceException("Final result must be PASS or REJECT.");
        }
    }

    private void validateConfirmStatus(String confirmStatus)
    {
        if (StringUtils.isEmpty(confirmStatus))
        {
            return;
        }
        if (!STATUS_DRAFT.equals(confirmStatus) && !STATUS_CONFIRMED.equals(confirmStatus))
        {
            throw new ServiceException("Confirm status must be DRAFT or CONFIRMED.");
        }
    }
}
