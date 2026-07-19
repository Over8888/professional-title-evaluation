package com.ruoyi.evaluation.service.impl;

import java.util.List;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.math.BigDecimal;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.evaluation.domain.Activity;
import com.ruoyi.evaluation.domain.ActivityCreateRequest;
import com.ruoyi.evaluation.domain.RuleConfig;
import com.ruoyi.evaluation.mapper.ActivityCandidateMapper;
import com.ruoyi.evaluation.mapper.ActivityVoterMapper;
import com.ruoyi.evaluation.mapper.ActivityMapper;
import com.ruoyi.evaluation.mapper.FinalEvaluationMapper;
import com.ruoyi.evaluation.mapper.ResultAggMapper;
import com.ruoyi.evaluation.mapper.RuleConfigMapper;
import com.ruoyi.evaluation.mapper.VoteMapper;
import com.ruoyi.evaluation.service.IActivityService;

@Service
public class ActivityServiceImpl implements IActivityService
{
    @Autowired
    private ActivityMapper activityMapper;

    @Autowired
    private RuleConfigMapper ruleConfigMapper;

    @Autowired
    private ActivityCandidateMapper activityCandidateMapper;

    @Autowired
    private ActivityVoterMapper activityVoterMapper;

    @Autowired
    private VoteMapper voteMapper;

    @Autowired
    private ResultAggMapper resultAggMapper;

    @Autowired
    private FinalEvaluationMapper finalEvaluationMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<Activity> selectActivityList(Activity activity)
    {
        syncActivityStatusByTime();
        return activityMapper.selectActivityList(activity);
    }
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Activity selectActivityById(Long id)
    {
        syncActivityStatusByTime();
        return activityMapper.selectActivityById(id);
    }
    @Override
    public int insertActivity(Activity activity)
    {
        validateActivityTime(activity);
        validateSupportedType(activity.getType());
        if (StringUtils.isEmpty(activity.getStatus())) { activity.setStatus("CONFIGURED"); }
        if (StringUtils.isEmpty(activity.getType())) { activity.setType("TITLE_REVIEW"); }
        if (StringUtils.isEmpty(activity.getArchived())) { activity.setArchived("0"); }
        return activityMapper.insertActivity(activity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Activity createWithRule(ActivityCreateRequest request, String username)
    {
        if (request == null || request.getActivity() == null || request.getRuleConfig() == null)
        {
            throw new ServiceException("创建活动必须同时提交活动信息和规则配置");
        }
        Activity activity = request.getActivity();
        activity.setCreatedBy(username);
        activity.setStatus("CONFIGURED");
        validateSupportedType(activity.getType());
        validateRuleConfig(request.getRuleConfig());
        insertActivity(activity);
        RuleConfig ruleConfig = request.getRuleConfig();
        ruleConfig.setActivityId(activity.getId());
        ruleConfig.setVoteType("PASS_REJECT");
        ruleConfig.setCreateBy(username);
        if (ruleConfigMapper.selectRuleConfigByActivityId(activity.getId()) == null)
        {
            ruleConfigMapper.insertRuleConfig(ruleConfig);
        }
        else
        {
            ruleConfigMapper.updateRuleConfig(ruleConfig);
        }
        return activity;
    }

    @Override
    public int updateActivity(Activity activity)
    {
        validateActivityTime(activity);
        validateSupportedType(activity.getType());
        if (StringUtils.isNotEmpty(activity.getStatus()))
        {
            Activity current = activityMapper.selectActivityById(activity.getId());
            if (current != null && !current.getStatus().equals(activity.getStatus()))
            {
                if ("CALCULATED".equals(activity.getStatus()))
                {
                    throw new ServiceException("Use /evaluation/result/calculate/{activityId} to calculate results.");
                }
                validateStatusTransition(current, activity.getStatus());
            }
        }
        return activityMapper.updateActivity(activity);
    }
    @Override
    public int updateActivityStatus(Long id, String status, String username)
    {
        Activity current = activityMapper.selectActivityById(id);
        if ("CALCULATED".equals(status))
        {
            throw new ServiceException("Use /evaluation/result/calculate/{activityId} to calculate results.");
        }
        validateStatusTransition(current, status);
        if ("CLOSED".equals(status))
        {
            validateAllVotersDone(id);
        }
        Activity activity = new Activity();
        activity.setId(id);
        activity.setStatus(status);
        if ("PUBLISHED".equals(status))
        {
            activity.setPublishTime(new java.util.Date());
        }
        if ("CLOSED".equals(status))
        {
            activity.setEndTime(new java.util.Date());
        }
        activity.setUpdatedBy(username);
        return activityMapper.updateActivity(activity);
    }

    @Override
    public Map<String, Object> publishActivity(Long id, String username, String requestBaseUrl)
    {
        Activity current = activityMapper.selectActivityById(id);
        validateStatusTransition(current, "PUBLISHED");
        boolean noVoteRequired = activityCandidateMapper.countVoteCandidatesByActivityId(id) == 0;
        Activity activity = new Activity();
        activity.setId(id);
        activity.setStatus(noVoteRequired ? "CLOSED" : resolvePublishedStatus(current));
        activity.setPublishTime(new java.util.Date());
        if (noVoteRequired)
        {
            activity.setEndTime(new java.util.Date());
        }
        if (StringUtils.isEmpty(current.getVoteEntryKey()))
        {
            activity.setVoteEntryKey(UUID.randomUUID().toString().replace("-", ""));
        }
        activity.setUpdatedBy(username);
        activityMapper.updateActivity(activity);
        Activity published = activityMapper.selectActivityById(id);
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("activityId", published.getId());
        data.put("status", published.getStatus());
        data.put("noVoteRequired", noVoteRequired);
        data.put("publishedAt", published.getPublishTime());
        data.put("voteEntryKey", published.getVoteEntryKey());
        data.put("voteUrl", requestBaseUrl + "/vote/activity/" + published.getVoteEntryKey());
        data.put("voteLinksUrl", requestBaseUrl + "/evaluation/voter/activity/" + id + "/links");
        data.put("message", "请复制活动投票入口，评委进入后通过姓名和工号确认身份");
        return data;
    }
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int deleteActivityByIds(Long[] ids)
    {
        if (ids == null || ids.length == 0)
        {
            return 0;
        }
        int rows = 0;
        for (Long id : ids)
        {
            rows += deleteActivityById(id);
        }
        return rows;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int deleteActivityById(Long id)
    {
        Activity activity = activityMapper.selectActivityById(id);
        validateDeleteReady(activity);
        voteMapper.deleteVoteByActivityId(id);
        finalEvaluationMapper.deleteFinalEvaluationByActivityId(id);
        resultAggMapper.deleteResultAggByActivityId(id);
        activityVoterMapper.deleteActivityVoterByActivityId(id);
        activityCandidateMapper.deleteActivityCandidateByActivityId(id);
        ruleConfigMapper.deleteRuleConfigByActivityId(id);
        return activityMapper.deleteActivityById(id);
    }

    private void validateStatusTransition(Activity current, String targetStatus)
    {
        if (current == null)
        {
            throw new ServiceException("活动不存在");
        }
        String currentStatus = StringUtils.defaultIfEmpty(current.getStatus(), "DRAFT");
        if (currentStatus.equals(targetStatus))
        {
            return;
        }
        Set<String> allowed = allowedNextStatuses(currentStatus);
        if (!allowed.contains(targetStatus))
        {
            throw new ServiceException("活动状态不允许从 " + currentStatus + " 变更为 " + targetStatus);
        }
        if ("PUBLISHED".equals(targetStatus))
        {
            validatePublishReady(current.getId());
        }
        if ("CLOSED".equals(targetStatus) || "CALCULATED".equals(targetStatus))
        {
            validateAllVotersDone(current.getId());
        }
    }

    private void syncActivityStatusByTime()
    {
        activityMapper.updateExpiredActivities();
        activityMapper.updateStartedActivities();
    }

    private String resolvePublishedStatus(Activity activity)
    {
        java.util.Date now = new java.util.Date();
        if (activity.getEndTime() != null && !activity.getEndTime().after(now))
        {
            return "CLOSED";
        }
        if (activity.getStartTime() != null && !activity.getStartTime().after(now))
        {
            return "VOTING";
        }
        return "PUBLISHED";
    }

    private Set<String> allowedNextStatuses(String currentStatus)
    {
        if ("DRAFT".equals(currentStatus))
        {
            return new HashSet<>(Arrays.asList("CONFIGURED", "PUBLISHED"));
        }
        if ("CONFIGURED".equals(currentStatus))
        {
            return new HashSet<>(Arrays.asList("PUBLISHED"));
        }
        if ("PUBLISHED".equals(currentStatus))
        {
            return new HashSet<>(Arrays.asList("VOTING", "CLOSED"));
        }
        if ("VOTING".equals(currentStatus))
        {
            return new HashSet<>(Arrays.asList("CLOSED"));
        }
        if ("CLOSED".equals(currentStatus))
        {
            return new HashSet<>(Arrays.asList("CALCULATED"));
        }
        if ("CALCULATED".equals(currentStatus))
        {
            return new HashSet<>(Arrays.asList("CONFIRMED"));
        }
        if ("CONFIRMED".equals(currentStatus) || "EXPORTED".equals(currentStatus))
        {
            return new HashSet<>(Arrays.asList("ARCHIVED"));
        }
        return new HashSet<>();
    }

    private void validatePublishReady(Long activityId)
    {
        Activity activity = activityMapper.selectActivityById(activityId);
        if (activity == null)
        {
            throw new ServiceException("活动不存在");
        }
        if (activity.getStartTime() == null || activity.getEndTime() == null)
        {
            throw new ServiceException("发布前必须配置活动开始时间和结束时间");
        }
        validateActivityTime(activity);
        if (ruleConfigMapper.selectRuleConfigByActivityId(activityId) == null)
        {
            throw new ServiceException("发布前必须先保存规则配置");
        }
        if (activityCandidateMapper.countActivityCandidateByActivityId(activityId) == 0)
        {
            throw new ServiceException("发布前必须先选择或导入候选人");
        }
        if (activityCandidateMapper.countUnconfirmedByActivityId(activityId) > 0)
        {
            throw new ServiceException("Apply candidate range before publishing.");
        }
        if (activityCandidateMapper.countVoteCandidatesByActivityId(activityId) > 0
                && activityVoterMapper.countActivityVoterByActivityId(activityId) == 0)
        {
            throw new ServiceException("发布前必须先选择或导入评委");
        }
    }

    private void validateDeleteReady(Activity activity)
    {
        if (activity == null)
        {
            throw new ServiceException("活动不存在");
        }
        if ("VOTING".equals(activity.getStatus()))
        {
            throw new ServiceException("投票中的活动需要先结束才能删除");
        }
    }

    private void validateAllVotersDone(Long activityId)
    {
        if (activityCandidateMapper.countVoteCandidatesByActivityId(activityId) == 0)
        {
            return;
        }
        int total = activityVoterMapper.countActivityVoterByActivityId(activityId);
        int done = activityVoterMapper.countDoneByActivityId(activityId);
        if (total <= 0)
        {
            throw new ServiceException("当前活动没有评委，不能结束投票");
        }
        if (done != total)
        {
            throw new ServiceException("仍有 " + (total - done) + " 名评委未提交，不能结束投票");
        }
    }

    private void validateActivityTime(Activity activity)
    {
        if (activity == null || activity.getStartTime() == null || activity.getEndTime() == null)
        {
            return;
        }
        if (activity.getEndTime().before(activity.getStartTime()))
        {
            throw new ServiceException("活动结束时间不能早于开始时间");
        }
        if (!activity.getEndTime().after(new java.util.Date()))
        {
            throw new ServiceException("活动结束时间必须晚于当前时间");
        }
    }

    private void validateSupportedType(String type)
    {
        if ("TALENT_SELECTION".equals(type))
        {
            throw new ServiceException("人才评选为二期能力，当前版本暂未开放");
        }
    }

    private void validateRuleConfig(RuleConfig ruleConfig)
    {
        if (ruleConfig == null)
        {
            throw new ServiceException("规则配置不能为空");
        }
        BigDecimal passRatio = ruleConfig.getPassRatio();
        BigDecimal rejectRatio = ruleConfig.getRejectRatio();
        if (passRatio == null || rejectRatio == null)
        {
            throw new ServiceException("通过比例和淘汰比例不能为空");
        }
        if (passRatio.compareTo(BigDecimal.ZERO) < 0 || rejectRatio.compareTo(BigDecimal.ZERO) < 0)
        {
            throw new ServiceException("通过比例和淘汰比例不能小于 0");
        }
        if (passRatio.compareTo(BigDecimal.valueOf(100)) > 0 || rejectRatio.compareTo(BigDecimal.valueOf(100)) > 0)
        {
            throw new ServiceException("通过比例和淘汰比例不能大于 100");
        }
        if (passRatio.add(rejectRatio).compareTo(BigDecimal.valueOf(100)) > 0)
        {
            throw new ServiceException("通过比例和淘汰比例之和不能大于 100");
        }
    }
}
