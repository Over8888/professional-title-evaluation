package com.ruoyi.evaluation.service.impl;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.evaluation.domain.Activity;
import com.ruoyi.evaluation.domain.ActivityCandidate;
import com.ruoyi.evaluation.domain.ActivityVoter;
import com.ruoyi.evaluation.domain.Vote;
import com.ruoyi.evaluation.domain.VoteConfirmRequest;
import com.ruoyi.evaluation.domain.VoteSubmitItem;
import com.ruoyi.evaluation.domain.VoteSubmitRequest;
import com.ruoyi.evaluation.mapper.ActivityCandidateMapper;
import com.ruoyi.evaluation.mapper.ActivityMapper;
import com.ruoyi.evaluation.mapper.ActivityVoterMapper;
import com.ruoyi.evaluation.mapper.VoteMapper;
import com.ruoyi.evaluation.service.IResultAggService;
import com.ruoyi.evaluation.service.IVoteService;

@Service
public class VoteServiceImpl implements IVoteService
{
    @Autowired
    private ActivityMapper activityMapper;

    @Autowired
    private ActivityVoterMapper activityVoterMapper;

    @Autowired
    private ActivityCandidateMapper activityCandidateMapper;

    @Autowired
    private VoteMapper voteMapper;

    @Autowired
    private IResultAggService resultAggService;

    @Override
    public Map<String, Object> getEntry(String token, boolean strict)
    {
        VoteContext context = loadContext(token);
        String reason = unavailableReason(context.activity);
        if (strict && reason != null)
        {
            throw new ServiceException(reason);
        }
        Map<String, Object> data = buildEntry(context);
        data.put("canVote", reason == null);
        data.put("unavailableReason", reason);
        return data;
    }

    @Override
    public Map<String, Object> confirm(String token, VoteConfirmRequest request)
    {
        VoteContext context = loadContext(token);
        requireVoteOpen(context.activity);
        if (request == null || StringUtils.isEmpty(request.getName()) || StringUtils.isEmpty(request.getEmployeeId()))
        {
            throw new ServiceException("请填写身份信息");
        }
        if (context.voter == null)
        {
            ActivityVoter query = new ActivityVoter();
            query.setActivityId(context.activity.getId());
            query.setName(StringUtils.defaultString(request.getName()).trim());
            query.setEmployeeId(StringUtils.defaultString(request.getEmployeeId()).trim());
            context.voter = activityVoterMapper.selectActivityVoterByIdentity(query);
        }
        if (context.voter == null ||
            !sameText(context.voter.getName(), request.getName()) ||
            !sameText(context.voter.getEmployeeId(), request.getEmployeeId()))
        {
            throw new ServiceException("评委姓名或工号不匹配");
        }
        Map<String, Object> data = buildEntry(context);
        data.put("confirmed", true);
        data.put("canVote", true);
        return data;
    }

    @Override
    public List<Map<String, Object>> listCandidates(String token)
    {
        VoteContext context = loadContext(token);
        requireVoteOpen(context.activity);
        requireVoterToken(context);
        List<ActivityCandidate> candidates = activityCandidateMapper.selectVoteCandidatesByActivityId(context.activity.getId());
        List<Map<String, Object>> rows = new ArrayList<>();
        for (ActivityCandidate candidate : candidates)
        {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("activityCandidateId", candidate.getId());
            row.put("importSeq", candidate.getImportSeq());
            row.put("name", candidate.getName());
            row.put("company", candidate.getCompany());
            row.put("department", candidate.getDepartment());
            row.put("thirdLevelDepartment", candidate.getThirdLevelDepartment());
            row.put("position", candidate.getPosition());
            row.put("currentLevel", candidate.getCurrentLevel());
            row.put("appliedLevel", candidate.getAppliedLevel());
            row.put("fixedType", candidate.getFixedType());
            rows.add(row);
        }
        return rows;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> submit(String token, VoteSubmitRequest request)
    {
        ActivityVoter voter = activityVoterMapper.selectActivityVoterByTokenForUpdate(token);
        if (voter == null)
        {
            throw new ServiceException("投票链接无效");
        }
        Activity activity = activityMapper.selectActivityByIdForUpdate(voter.getActivityId());
        if (activity == null)
        {
            throw new ServiceException("活动不存在");
        }
        requireVoteOpen(activity);
        if ("DONE".equals(voter.getStatus()))
        {
            throw new ServiceException("您已提交投票，不能重复提交");
        }
        int roundNo = request != null && request.getRoundNo() != null ? request.getRoundNo() : 1;
        if (roundNo != 1)
        {
            throw new ServiceException("当前阶段仅支持第一轮投票");
        }
        List<ActivityCandidate> candidates = activityCandidateMapper.selectVoteCandidatesByActivityId(activity.getId());
        if (candidates.isEmpty())
        {
            throw new ServiceException("当前活动没有需要投票的候选人，无需提交投票");
        }
        List<VoteSubmitItem> items = request == null ? null : request.getVotes();
        if (items == null || items.size() != candidates.size())
        {
            throw new ServiceException("必须完成全部候选人投票后才能提交");
        }
        Map<Long, ActivityCandidate> candidateMap = candidates.stream()
                .collect(Collectors.toMap(ActivityCandidate::getId, c -> c));
        Set<Long> seen = new HashSet<>();
        List<Vote> votes = new ArrayList<>();
        for (VoteSubmitItem item : items)
        {
            if (item == null || item.getActivityCandidateId() == null)
            {
                throw new ServiceException("投票候选人不能为空");
            }
            if (!seen.add(item.getActivityCandidateId()))
            {
                throw new ServiceException("投票候选人不能重复");
            }
            if (!candidateMap.containsKey(item.getActivityCandidateId()))
            {
                throw new ServiceException("投票候选人不属于当前活动投票范围");
            }
            if (!"PASS".equals(item.getResult()) && !"REJECT".equals(item.getResult()))
            {
                throw new ServiceException("投票结果只能为通过或淘汰");
            }
            Vote vote = new Vote();
            vote.setActivityId(activity.getId());
            vote.setActivityVoterId(voter.getId());
            vote.setActivityCandidateId(item.getActivityCandidateId());
            vote.setResult(item.getResult());
            vote.setRoundNo(roundNo);
            votes.add(vote);
        }
        if (!seen.equals(candidateMap.keySet()))
        {
            throw new ServiceException("必须完成全部候选人投票后才能提交");
        }
        Vote query = new Vote();
        query.setActivityId(activity.getId());
        query.setActivityVoterId(voter.getId());
        query.setRoundNo(roundNo);
        if (voteMapper.countByActivityVoter(query) > 0)
        {
            throw new ServiceException("您已提交投票，不能重复提交");
        }
        try
        {
            voteMapper.batchInsertVote(votes);
        }
        catch (DuplicateKeyException e)
        {
            throw new ServiceException("您已提交投票，不能重复提交");
        }
        ActivityVoter done = new ActivityVoter();
        done.setId(voter.getId());
        done.setUpdateBy("vote-token");
        if (activityVoterMapper.markDone(done) == 0)
        {
            throw new ServiceException("您已提交投票，不能重复提交");
        }
        Date submittedAt = new Date();
        boolean allCompleted = isAllCompleted(activity.getId());
        if (allCompleted && isVotingStatus(activity.getStatus()))
        {
            Activity closed = new Activity();
            closed.setId(activity.getId());
            closed.setStatus("CLOSED");
            closed.setEndTime(submittedAt);
            closed.setUpdatedBy("vote-token");
            activityMapper.updateActivity(closed);
            activity.setStatus("CLOSED");
            activity.setEndTime(submittedAt);
        }
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("submitted", true);
        data.put("voteCount", votes.size());
        data.put("allCompleted", allCompleted);
        data.put("activityStatus", activity.getStatus());
        data.put("endTime", activity.getEndTime());
        data.put("submittedAt", submittedAt);
        return data;
    }

    @Override
    public Map<String, Object> result(String token)
    {
        VoteContext context = loadContext(token);
        requireVoterToken(context);
        return resultAggService.publicResult(context.activity.getId());
    }

    private VoteContext loadContext(String token)
    {
        if (StringUtils.isEmpty(token))
        {
            throw new ServiceException("投票链接无效");
        }
        ActivityVoter voter = activityVoterMapper.selectActivityVoterByToken(token);
        Activity activity;
        if (voter == null)
        {
            activity = activityMapper.selectActivityByVoteEntryKey(token);
            if (activity == null)
            {
                throw new ServiceException("投票链接无效");
            }
        }
        else
        {
            activity = activityMapper.selectActivityById(voter.getActivityId());
        }
        if (activity == null)
        {
            throw new ServiceException("活动不存在");
        }
        VoteContext context = new VoteContext();
        context.activity = activity;
        context.voter = voter;
        return context;
    }

    private Map<String, Object> buildEntry(VoteContext context)
    {
        Map<String, Object> activity = new LinkedHashMap<>();
        activity.put("id", context.activity.getId());
        activity.put("name", context.activity.getName());
        activity.put("status", context.activity.getStatus());
        activity.put("startTime", context.activity.getStartTime());
        activity.put("endTime", context.activity.getEndTime());

        Map<String, Object> voter = new LinkedHashMap<>();
        if (context.voter != null)
        {
            voter.put("id", context.voter.getId());
            voter.put("name", context.voter.getName());
            voter.put("employeeId", context.voter.getEmployeeId());
            voter.put("department", context.voter.getDepartment());
            voter.put("status", context.voter.getStatus());
            voter.put("voteToken", context.voter.getVoteToken());
            voter.put("submittedAt", context.voter.getSubmittedAt());
        }

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("activity", activity);
        data.put("voter", voter);
        data.put("sharedEntry", context.voter == null);
        data.put("submitted", context.voter != null && "DONE".equals(context.voter.getStatus()));
        data.put("submittedAt", context.voter == null ? null : context.voter.getSubmittedAt());
        int voteCandidateCount = activityCandidateMapper.countVoteCandidatesByActivityId(context.activity.getId());
        data.put("candidateCount", voteCandidateCount);
        data.put("noVoteRequired", voteCandidateCount == 0);
        data.put("allCompleted", isAllCompleted(context.activity.getId()));
        return data;
    }

    private void requireVoteOpen(Activity activity)
    {
        String reason = unavailableReason(activity);
        if (reason != null)
        {
            throw new ServiceException(reason);
        }
    }

    private void requireVoterToken(VoteContext context)
    {
        if (context.voter == null)
        {
            throw new ServiceException("请先完成身份确认");
        }
    }

    private String unavailableReason(Activity activity)
    {
        if (activity == null)
        {
            return "活动不存在";
        }
        if ("CLOSED".equals(activity.getStatus()))
        {
            return "活动已结束";
        }
        if (!"PUBLISHED".equals(activity.getStatus()) && !"VOTING".equals(activity.getStatus()))
        {
            return "活动当前状态不可投票";
        }
        Date now = new Date();
        if (activity.getStartTime() != null && now.before(activity.getStartTime()))
        {
            return "活动尚未开始";
        }
        if (activity.getEndTime() != null && now.after(activity.getEndTime()))
        {
            return "活动已结束";
        }
        return null;
    }

    private boolean isAllCompleted(Long activityId)
    {
        if (activityCandidateMapper.countVoteCandidatesByActivityId(activityId) == 0)
        {
            return true;
        }
        int total = activityVoterMapper.countActivityVoterByActivityId(activityId);
        int done = activityVoterMapper.countDoneByActivityId(activityId);
        return total > 0 && total == done;
    }

    private boolean isVotingStatus(String status)
    {
        return "PUBLISHED".equals(status) || "VOTING".equals(status);
    }

    private boolean sameText(String a, String b)
    {
        return StringUtils.defaultString(a).trim().equals(StringUtils.defaultString(b).trim());
    }

    private static class VoteContext
    {
        private Activity activity;
        private ActivityVoter voter;
    }
}
