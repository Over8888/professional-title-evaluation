package com.ruoyi.evaluation.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Date;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.test.util.ReflectionTestUtils;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.evaluation.domain.Activity;
import com.ruoyi.evaluation.domain.RuleConfig;
import com.ruoyi.evaluation.mapper.ActivityCandidateMapper;
import com.ruoyi.evaluation.mapper.ActivityMapper;
import com.ruoyi.evaluation.mapper.ActivityVoterMapper;
import com.ruoyi.evaluation.mapper.FinalEvaluationMapper;
import com.ruoyi.evaluation.mapper.ResultAggMapper;
import com.ruoyi.evaluation.mapper.RuleConfigMapper;
import com.ruoyi.evaluation.mapper.VoteMapper;

class ActivityServiceImplTest
{
    private ActivityServiceImpl service;

    @Mock
    private ActivityMapper activityMapper;

    @Mock
    private RuleConfigMapper ruleConfigMapper;

    @Mock
    private ActivityCandidateMapper activityCandidateMapper;

    @Mock
    private ActivityVoterMapper activityVoterMapper;

    @Mock
    private VoteMapper voteMapper;

    @Mock
    private ResultAggMapper resultAggMapper;

    @Mock
    private FinalEvaluationMapper finalEvaluationMapper;

    @BeforeEach
    void setUp()
    {
        MockitoAnnotations.openMocks(this);
        service = new ActivityServiceImpl();
        ReflectionTestUtils.setField(service, "activityMapper", activityMapper);
        ReflectionTestUtils.setField(service, "ruleConfigMapper", ruleConfigMapper);
        ReflectionTestUtils.setField(service, "activityCandidateMapper", activityCandidateMapper);
        ReflectionTestUtils.setField(service, "activityVoterMapper", activityVoterMapper);
        ReflectionTestUtils.setField(service, "voteMapper", voteMapper);
        ReflectionTestUtils.setField(service, "resultAggMapper", resultAggMapper);
        ReflectionTestUtils.setField(service, "finalEvaluationMapper", finalEvaluationMapper);
    }

    @Test
    void publishRequiresVotersWhenVoteCandidatesExist()
    {
        Activity current = activity("CONFIGURED");
        when(activityMapper.selectActivityById(1L)).thenReturn(current);
        when(ruleConfigMapper.selectRuleConfigByActivityId(1L)).thenReturn(new RuleConfig());
        when(activityCandidateMapper.countActivityCandidateByActivityId(1L)).thenReturn(2);
        when(activityCandidateMapper.countUnconfirmedByActivityId(1L)).thenReturn(0);
        when(activityCandidateMapper.countVoteCandidatesByActivityId(1L)).thenReturn(1);
        when(activityVoterMapper.countActivityVoterByActivityId(1L)).thenReturn(0);

        ServiceException ex = assertThrows(ServiceException.class,
                () -> service.publishActivity(1L, "hr", "http://localhost:8081"));

        assertEquals("发布前必须先选择或导入评委", ex.getMessage());
        verify(activityMapper, never()).updateActivity(any());
    }

    @Test
    void publishClosesNoVoteCandidateActivityWithoutVoters()
    {
        Activity current = activity("CONFIGURED");
        Activity published = activity("CLOSED");
        when(activityMapper.selectActivityById(1L)).thenReturn(current, current, published);
        when(ruleConfigMapper.selectRuleConfigByActivityId(1L)).thenReturn(new RuleConfig());
        when(activityCandidateMapper.countActivityCandidateByActivityId(1L)).thenReturn(2);
        when(activityCandidateMapper.countUnconfirmedByActivityId(1L)).thenReturn(0);
        when(activityCandidateMapper.countVoteCandidatesByActivityId(1L)).thenReturn(0);

        java.util.Map<String, Object> result = service.publishActivity(1L, "hr", "http://localhost:8081");

        assertEquals("CLOSED", result.get("status"));
        assertEquals(true, result.get("noVoteRequired"));
        ArgumentCaptor<Activity> captor = ArgumentCaptor.forClass(Activity.class);
        verify(activityMapper).updateActivity(captor.capture());
        assertEquals("CLOSED", captor.getValue().getStatus());
    }

    @Test
    void closeAllowsNoVoteCandidateWithoutVoters()
    {
        when(activityMapper.selectActivityById(1L)).thenReturn(activity("PUBLISHED"));
        when(activityCandidateMapper.countVoteCandidatesByActivityId(1L)).thenReturn(0);
        when(activityMapper.updateActivity(any())).thenReturn(1);

        int updated = service.updateActivityStatus(1L, "CLOSED", "hr");

        assertEquals(1, updated);
        ArgumentCaptor<Activity> captor = ArgumentCaptor.forClass(Activity.class);
        verify(activityMapper).updateActivity(captor.capture());
        assertEquals("CLOSED", captor.getValue().getStatus());
    }

    @Test
    void closeStillRejectsPendingVotersWhenVoteCandidatesExist()
    {
        when(activityMapper.selectActivityById(1L)).thenReturn(activity("PUBLISHED"));
        when(activityCandidateMapper.countVoteCandidatesByActivityId(1L)).thenReturn(1);
        when(activityVoterMapper.countActivityVoterByActivityId(1L)).thenReturn(3);
        when(activityVoterMapper.countDoneByActivityId(1L)).thenReturn(2);

        ServiceException ex = assertThrows(ServiceException.class,
                () -> service.updateActivityStatus(1L, "CLOSED", "hr"));

        assertEquals("仍有 1 名评委未提交，不能结束投票", ex.getMessage());
        verify(activityMapper, never()).updateActivity(any());
    }

    private Activity activity(String status)
    {
        Activity activity = new Activity();
        activity.setId(1L);
        activity.setName("测试活动");
        activity.setStatus(status);
        activity.setStartTime(new Date(System.currentTimeMillis() + 60_000));
        activity.setEndTime(new Date(System.currentTimeMillis() + 3_600_000));
        return activity;
    }
}
