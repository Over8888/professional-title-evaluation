package com.ruoyi.evaluation.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.test.util.ReflectionTestUtils;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.evaluation.domain.Activity;
import com.ruoyi.evaluation.domain.ActivityRangeSetting;
import com.ruoyi.evaluation.domain.ActivityVoter;
import com.ruoyi.evaluation.domain.Candidate;
import com.ruoyi.evaluation.domain.RuleConfig;
import com.ruoyi.evaluation.mapper.ActivityCandidateMapper;
import com.ruoyi.evaluation.mapper.ActivityMapper;
import com.ruoyi.evaluation.mapper.ActivityRangeSettingMapper;
import com.ruoyi.evaluation.mapper.ActivityVoterMapper;
import com.ruoyi.evaluation.mapper.RuleConfigMapper;

class RuleConfigServiceImplTest
{
    private RuleConfigServiceImpl service;

    @Mock
    private RuleConfigMapper ruleConfigMapper;

    @Mock
    private ActivityMapper activityMapper;

    @Mock
    private ActivityCandidateMapper activityCandidateMapper;

    @Mock
    private ActivityRangeSettingMapper activityRangeSettingMapper;

    @Mock
    private ActivityVoterMapper activityVoterMapper;

    @BeforeEach
    void setUp()
    {
        MockitoAnnotations.openMocks(this);
        service = new RuleConfigServiceImpl();
        ReflectionTestUtils.setField(service, "ruleConfigMapper", ruleConfigMapper);
        ReflectionTestUtils.setField(service, "activityMapper", activityMapper);
        ReflectionTestUtils.setField(service, "activityCandidateMapper", activityCandidateMapper);
        ReflectionTestUtils.setField(service, "activityRangeSettingMapper", activityRangeSettingMapper);
        ReflectionTestUtils.setField(service, "activityVoterMapper", activityVoterMapper);
    }

    @Test
    void insertRuleConfigRejectsPublishedActivity()
    {
        when(activityMapper.selectActivityById(1L)).thenReturn(activity("PUBLISHED"));

        ServiceException ex = assertThrows(ServiceException.class, () -> service.insertRuleConfig(rule(1L, 50, 20)));

        assertEquals("Activity rules can only be changed before publishing.", ex.getMessage());
        verify(ruleConfigMapper, never()).insertRuleConfig(any());
    }

    @Test
    void insertRuleConfigValidatesRatioSum()
    {
        when(activityMapper.selectActivityById(1L)).thenReturn(activity("CONFIGURED"));

        ServiceException ex = assertThrows(ServiceException.class, () -> service.insertRuleConfig(rule(1L, 70, 40)));

        assertEquals("通过比例和淘汰比例之和不能大于 100", ex.getMessage());
    }

    @Test
    void applyCandidateRangeUsesConfirmedCounts()
    {
        when(activityMapper.selectActivityById(1L)).thenReturn(activity("CONFIGURED"));
        when(ruleConfigMapper.selectRuleConfigByActivityId(1L)).thenReturn(rule(1L, 50, 25));
        when(activityCandidateMapper.selectCandidateSnapshotList(any())).thenReturn(Arrays.asList(
                candidate(101L, 1),
                candidate(102L, 2),
                candidate(103L, 3),
                candidate(104L, 4)));
        when(activityRangeSettingMapper.selectActivityRangeSettingByActivityId(1L)).thenReturn(Collections.emptyList());
        when(activityCandidateMapper.updateCandidateSnapshot(any())).thenReturn(1);

        int updated = service.applyCandidateRange(1L, Arrays.asList(confirmedRange(1, 1)), "hr");

        assertEquals(4, updated);
        ArgumentCaptor<ActivityRangeSetting> settingCaptor = ArgumentCaptor.forClass(ActivityRangeSetting.class);
        verify(activityRangeSettingMapper).upsertActivityRangeSetting(settingCaptor.capture());
        assertEquals(1, settingCaptor.getValue().getLockedPassCount());
        assertEquals(1, settingCaptor.getValue().getLockedRejectCount());
        ArgumentCaptor<Candidate> candidateCaptor = ArgumentCaptor.forClass(Candidate.class);
        verify(activityCandidateMapper, org.mockito.Mockito.times(4)).updateCandidateSnapshot(candidateCaptor.capture());
        List<Candidate> rows = candidateCaptor.getAllValues();
        assertEquals("PASS", rows.get(0).getFixedType());
        assertEquals("VOTE", rows.get(1).getFixedType());
        assertEquals("VOTE", rows.get(2).getFixedType());
        assertEquals("REJECT", rows.get(3).getFixedType());
    }

    @Test
    void previewCandidateRangeUsesDocumentCounts()
    {
        when(ruleConfigMapper.selectRuleConfigByActivityId(1L)).thenReturn(rule(1L, 65, 10));
        when(activityCandidateMapper.selectCandidateSnapshotList(any())).thenReturn(candidates(100, "中级工程师"));
        when(activityVoterMapper.selectActivityVoterList(any())).thenReturn(Collections.emptyList());
        when(activityRangeSettingMapper.selectActivityRangeSettingByActivityId(1L)).thenReturn(Collections.emptyList());

        List<Map<String, Object>> rows = service.previewCandidateRange(1L);

        assertEquals(1, rows.size());
        Map<String, Object> row = rows.get(0);
        assertEquals(100, row.get("candidateCount"));
        assertEquals(65, row.get("maxPassCount"));
        assertEquals(60, row.get("lockedPassCount"));
        assertEquals(10, row.get("lockedRejectCount"));
        assertEquals(30, row.get("voteCount"));
        assertEquals(-5, row.get("minVoteRejectCount"));
        assertEquals("1-60", row.get("confirmedPassRange"));
        assertEquals("61-90", row.get("voteRange"));
        assertEquals("91-100", row.get("confirmedRejectRange"));
    }

    @Test
    void applyCandidateRangeAllowsNoVoteCandidate()
    {
        when(activityMapper.selectActivityById(1L)).thenReturn(activity("CONFIGURED"));
        when(ruleConfigMapper.selectRuleConfigByActivityId(1L)).thenReturn(rule(1L, 100, 0));
        when(activityCandidateMapper.selectCandidateSnapshotList(any())).thenReturn(Arrays.asList(candidate(101L, 1), candidate(102L, 2)));
        when(activityRangeSettingMapper.selectActivityRangeSettingByActivityId(1L)).thenReturn(Collections.emptyList());
        when(activityCandidateMapper.updateCandidateSnapshot(any())).thenReturn(1);

        int updated = service.applyCandidateRange(1L, Arrays.asList(confirmedRange(2, 0)), "hr");

        assertEquals(2, updated);
        ArgumentCaptor<Candidate> candidateCaptor = ArgumentCaptor.forClass(Candidate.class);
        verify(activityCandidateMapper, org.mockito.Mockito.times(2)).updateCandidateSnapshot(candidateCaptor.capture());
        assertEquals("PASS", candidateCaptor.getAllValues().get(0).getFixedType());
        assertEquals("PASS", candidateCaptor.getAllValues().get(1).getFixedType());
    }

    @Test
    void previewCandidateRangeAutoPassesAssistantAndTechnicianLevels()
    {
        when(ruleConfigMapper.selectRuleConfigByActivityId(1L)).thenReturn(rule(1L, 65, 10));
        List<Candidate> candidates = new ArrayList<>();
        candidates.addAll(candidates(3, "助理工程师"));
        candidates.addAll(candidates(2, "技术员"));
        candidates.get(3).setId(101L);
        candidates.get(3).setImportSeq(1);
        candidates.get(4).setId(102L);
        candidates.get(4).setImportSeq(2);
        when(activityCandidateMapper.selectCandidateSnapshotList(any())).thenReturn(candidates);
        when(activityVoterMapper.selectActivityVoterList(any())).thenReturn(Collections.emptyList());
        when(activityRangeSettingMapper.selectActivityRangeSettingByActivityId(1L)).thenReturn(Collections.emptyList());

        List<Map<String, Object>> rows = service.previewCandidateRange(1L);

        assertEquals(2, rows.size());
        assertEquals(3, rows.get(0).get("maxPassCount"));
        assertEquals(3, rows.get(0).get("lockedPassCount"));
        assertEquals(0, rows.get(0).get("lockedRejectCount"));
        assertEquals(0, rows.get(0).get("voteCount"));
        assertEquals(0, rows.get(0).get("minVoteRejectCount"));
        assertEquals("1-3", rows.get(0).get("confirmedPassRange"));
        assertEquals("-", rows.get(0).get("voteRange"));
        assertEquals("-", rows.get(0).get("confirmedRejectRange"));
        assertEquals("技术员", rows.get(1).get("appliedLevel"));
        assertEquals(2, rows.get(1).get("lockedPassCount"));
        assertEquals(0, rows.get(1).get("voteCount"));
    }

    @Test
    void previewCandidateRangeKeepsSeniorAndIntermediateInVoteRange()
    {
        when(ruleConfigMapper.selectRuleConfigByActivityId(1L)).thenReturn(rule(1L, 50, 25));
        List<Candidate> candidates = new ArrayList<>();
        candidates.addAll(candidates(4, "副高级工程师"));
        candidates.get(0).setDepartment("船体组");
        candidates.get(1).setDepartment("船体组");
        candidates.get(2).setDepartment("船体组");
        candidates.get(3).setDepartment("船体组");
        for (int i = 1; i <= 4; i++)
        {
            Candidate row = candidate(100L + i, i, "中级工程师");
            row.setDepartment("船机组");
            candidates.add(row);
        }
        when(activityCandidateMapper.selectCandidateSnapshotList(any())).thenReturn(candidates);
        when(activityVoterMapper.selectActivityVoterList(any())).thenReturn(Collections.emptyList());
        when(activityRangeSettingMapper.selectActivityRangeSettingByActivityId(1L)).thenReturn(Collections.emptyList());

        List<Map<String, Object>> rows = service.previewCandidateRange(1L);

        assertEquals(2, rows.size());
        assertEquals("副高级工程师", rows.get(0).get("appliedLevel"));
        assertEquals(2, rows.get(0).get("maxPassCount"));
        assertEquals(2, rows.get(0).get("lockedPassCount"));
        assertEquals(1, rows.get(0).get("lockedRejectCount"));
        assertEquals(1, rows.get(0).get("voteCount"));
        assertEquals("中级工程师", rows.get(1).get("appliedLevel"));
        assertEquals(2, rows.get(1).get("maxPassCount"));
        assertEquals(1, rows.get(1).get("voteCount"));
    }

    @Test
    void previewCandidateRangeCountsVotersByDepartment()
    {
        when(ruleConfigMapper.selectRuleConfigByActivityId(1L)).thenReturn(rule(1L, 50, 25));
        when(activityCandidateMapper.selectCandidateSnapshotList(any())).thenReturn(Arrays.asList(candidate(101L, 1), candidate(102L, 2)));
        when(activityVoterMapper.selectActivityVoterList(any())).thenReturn(Arrays.asList(voter("船体组"), voter("船体组")));
        when(activityRangeSettingMapper.selectActivityRangeSettingByActivityId(1L)).thenReturn(Collections.emptyList());

        List<Map<String, Object>> rows = service.previewCandidateRange(1L);

        assertEquals(1, rows.size());
        assertEquals(2, rows.get(0).get("plannedVoterCount"));
        assertEquals(2, rows.get(0).get("candidateCount"));
        assertEquals(1, rows.get(0).get("maxPassCount"));
    }

    @Test
    void updateRuleConfigLoadsActivityIdFromExistingRule()
    {
        RuleConfig existing = rule(1L, 50, 20);
        existing.setId(9L);
        RuleConfig update = new RuleConfig();
        update.setId(9L);
        update.setPassRatio(BigDecimal.valueOf(60));
        update.setRejectRatio(BigDecimal.valueOf(10));
        when(ruleConfigMapper.selectRuleConfigById(9L)).thenReturn(existing);
        when(activityMapper.selectActivityById(1L)).thenReturn(activity("CONFIGURED"));
        when(ruleConfigMapper.updateRuleConfig(update)).thenReturn(1);

        int updated = service.updateRuleConfig(update);

        assertEquals(1, updated);
        assertEquals(1L, update.getActivityId());
        assertEquals("PASS_REJECT", update.getVoteType());
    }

    @Test
    void insertRuleConfigUpdatesExistingActivityRule()
    {
        RuleConfig existing = rule(1L, 50, 20);
        existing.setId(9L);
        RuleConfig incoming = rule(1L, 60, 10);
        incoming.setCreateBy("hr");
        when(activityMapper.selectActivityById(1L)).thenReturn(activity("CONFIGURED"));
        when(ruleConfigMapper.selectRuleConfigByActivityId(1L)).thenReturn(existing);
        when(ruleConfigMapper.updateRuleConfig(incoming)).thenReturn(1);

        int updated = service.insertRuleConfig(incoming);

        assertEquals(1, updated);
        assertEquals(9L, incoming.getId());
        assertEquals("hr", incoming.getUpdateBy());
    }

    @Test
    void deleteRuleConfigChecksActivityStatus()
    {
        RuleConfig existing = rule(1L, 50, 20);
        existing.setId(9L);
        when(ruleConfigMapper.selectRuleConfigById(9L)).thenReturn(existing);
        when(activityMapper.selectActivityById(1L)).thenReturn(activity("CONFIGURED"));
        when(ruleConfigMapper.deleteRuleConfigById(9L)).thenReturn(1);

        assertEquals(1, service.deleteRuleConfigById(9L));
    }

    @Test
    void previewCandidateDetailRangeReturnsAppliedRows()
    {
        when(activityCandidateMapper.selectCandidateSnapshotList(any())).thenReturn(Arrays.asList(candidate(101L, 1), candidate(102L, 2)));

        List<Map<String, Object>> rows = service.previewCandidateDetailRange(1L);

        assertEquals(2, rows.size());
        assertEquals("已应用范围", rows.get(0).get("source"));
        assertEquals("船体组", rows.get(0).get("department"));
    }

    @Test
    void applyCandidateRangeConvenienceMethodUsesExistingSettings()
    {
        ActivityRangeSetting setting = new ActivityRangeSetting();
        setting.setActivityId(1L);
        setting.setDepartment("船体组");
        setting.setAppliedLevel("中级工程师");
        setting.setLockedPassCount(1);
        setting.setLockedRejectCount(1);
        when(activityMapper.selectActivityById(1L)).thenReturn(activity("CONFIGURED"));
        when(ruleConfigMapper.selectRuleConfigByActivityId(1L)).thenReturn(rule(1L, 50, 25));
        when(activityCandidateMapper.selectCandidateSnapshotList(any())).thenReturn(Arrays.asList(
                candidate(101L, 1), candidate(102L, 2), candidate(103L, 3)));
        when(activityRangeSettingMapper.selectActivityRangeSettingByActivityId(1L)).thenReturn(Arrays.asList(setting));
        when(activityCandidateMapper.updateCandidateSnapshot(any())).thenReturn(1);

        int updated = service.applyCandidateRange(1L, "hr");

        assertEquals(3, updated);
    }

    @Test
    void applyCandidateRangeRejectsInvalidConfirmedCounts()
    {
        when(activityMapper.selectActivityById(1L)).thenReturn(activity("CONFIGURED"));
        when(ruleConfigMapper.selectRuleConfigByActivityId(1L)).thenReturn(rule(1L, 50, 25));
        when(activityCandidateMapper.selectCandidateSnapshotList(any())).thenReturn(Arrays.asList(candidate(101L, 1), candidate(102L, 2)));
        when(activityRangeSettingMapper.selectActivityRangeSettingByActivityId(1L)).thenReturn(Collections.emptyList());

        ServiceException negative = assertThrows(ServiceException.class,
                () -> service.applyCandidateRange(1L, Arrays.asList(confirmedRange(-1, 0)), "hr"));
        assertEquals("锁定通过人数和锁定不通过人数不能小于 0", negative.getMessage());

        ServiceException tooManyPass = assertThrows(ServiceException.class,
                () -> service.applyCandidateRange(1L, Arrays.asList(confirmedRange(2, 0)), "hr"));
        assertEquals("锁定通过人数不能大于最多通过人数", tooManyPass.getMessage());
    }

    @Test
    void simpleRuleQueriesDelegateToMapper()
    {
        RuleConfig query = new RuleConfig();
        when(ruleConfigMapper.selectRuleConfigList(query)).thenReturn(Arrays.asList(rule(1L, 50, 20)));
        when(ruleConfigMapper.selectRuleConfigById(9L)).thenReturn(rule(1L, 50, 20));
        when(ruleConfigMapper.selectRuleConfigByActivityId(1L)).thenReturn(rule(1L, 50, 20));

        assertEquals(1, service.selectRuleConfigList(query).size());
        assertEquals(1L, service.selectRuleConfigById(9L).getActivityId());
        assertEquals(1L, service.selectRuleConfigByActivityId(1L).getActivityId());
    }

    private Activity activity(String status)
    {
        Activity activity = new Activity();
        activity.setId(1L);
        activity.setStatus(status);
        return activity;
    }

    private RuleConfig rule(Long activityId, int passRatio, int rejectRatio)
    {
        RuleConfig rule = new RuleConfig();
        rule.setActivityId(activityId);
        rule.setPassRatio(BigDecimal.valueOf(passRatio));
        rule.setRejectRatio(BigDecimal.valueOf(rejectRatio));
        return rule;
    }

    private Candidate candidate(Long id, int importSeq)
    {
        return candidate(id, importSeq, "中级工程师");
    }

    private Candidate candidate(Long id, int importSeq, String appliedLevel)
    {
        Candidate candidate = new Candidate();
        candidate.setId(id);
        candidate.setActivityId(1L);
        candidate.setDepartment("船体组");
        candidate.setAppliedLevel(appliedLevel);
        candidate.setImportSeq(importSeq);
        return candidate;
    }

    private List<Candidate> candidates(int count, String appliedLevel)
    {
        List<Candidate> rows = new ArrayList<>();
        for (int i = 1; i <= count; i++)
        {
            rows.add(candidate((long) i, i, appliedLevel));
        }
        return rows;
    }

    private Map<String, Object> confirmedRange(int lockedPass, int lockedReject)
    {
        Map<String, Object> row = new HashMap<>();
        row.put("groupKey", "船体组|中级工程师");
        row.put("lockedPassCount", lockedPass);
        row.put("lockedRejectCount", lockedReject);
        return row;
    }

    private ActivityVoter voter(String department)
    {
        ActivityVoter voter = new ActivityVoter();
        voter.setActivityId(1L);
        voter.setDepartment(department);
        return voter;
    }
}
