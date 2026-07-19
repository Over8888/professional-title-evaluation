package com.ruoyi.evaluation.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Date;
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
import com.ruoyi.evaluation.domain.ActivityCandidate;
import com.ruoyi.evaluation.domain.ActivityVoter;
import com.ruoyi.evaluation.domain.VoteConfirmRequest;
import com.ruoyi.evaluation.domain.Vote;
import com.ruoyi.evaluation.domain.VoteSubmitItem;
import com.ruoyi.evaluation.domain.VoteSubmitRequest;
import com.ruoyi.evaluation.mapper.ActivityCandidateMapper;
import com.ruoyi.evaluation.mapper.ActivityMapper;
import com.ruoyi.evaluation.mapper.ActivityVoterMapper;
import com.ruoyi.evaluation.mapper.VoteMapper;
import com.ruoyi.evaluation.service.IResultAggService;

class VoteServiceImplTest
{
    private VoteServiceImpl service;

    @Mock
    private ActivityMapper activityMapper;

    @Mock
    private ActivityVoterMapper activityVoterMapper;

    @Mock
    private ActivityCandidateMapper activityCandidateMapper;

    @Mock
    private VoteMapper voteMapper;

    @Mock
    private IResultAggService resultAggService;

    @BeforeEach
    void setUp()
    {
        MockitoAnnotations.openMocks(this);
        service = new VoteServiceImpl();
        ReflectionTestUtils.setField(service, "activityMapper", activityMapper);
        ReflectionTestUtils.setField(service, "activityVoterMapper", activityVoterMapper);
        ReflectionTestUtils.setField(service, "activityCandidateMapper", activityCandidateMapper);
        ReflectionTestUtils.setField(service, "voteMapper", voteMapper);
        ReflectionTestUtils.setField(service, "resultAggService", resultAggService);
    }

    @Test
    void submitRequiresAllVoteCandidates()
    {
        mockOpenVotingContext();

        VoteSubmitRequest request = request(item(101L, "PASS"));
        ServiceException ex = assertThrows(ServiceException.class, () -> service.submit("token", request));

        assertEquals("必须完成全部候选人投票后才能提交", ex.getMessage());
        verify(voteMapper, never()).batchInsertVote(any());
    }

    @Test
    void submitRejectsDuplicateCandidates()
    {
        mockOpenVotingContext();

        VoteSubmitRequest request = request(item(101L, "PASS"), item(101L, "REJECT"));
        ServiceException ex = assertThrows(ServiceException.class, () -> service.submit("token", request));

        assertEquals("投票候选人不能重复", ex.getMessage());
    }

    @Test
    void submitRejectsRepeatedVoter()
    {
        mockOpenVotingContext();
        when(voteMapper.countByActivityVoter(any())).thenReturn(2);

        VoteSubmitRequest request = request(item(101L, "PASS"), item(102L, "REJECT"));
        ServiceException ex = assertThrows(ServiceException.class, () -> service.submit("token", request));

        assertEquals("您已提交投票，不能重复提交", ex.getMessage());
        verify(voteMapper, never()).batchInsertVote(any());
    }

    @Test
    void submitPersistsVotesMarksDoneAndClosesWhenAllCompleted()
    {
        mockOpenVotingContext();
        when(voteMapper.countByActivityVoter(any())).thenReturn(0);
        when(voteMapper.batchInsertVote(any())).thenReturn(2);
        when(activityVoterMapper.markDone(any())).thenReturn(1);
        when(activityVoterMapper.countActivityVoterByActivityId(1L)).thenReturn(1);
        when(activityVoterMapper.countDoneByActivityId(1L)).thenReturn(1);

        Map<String, Object> response = service.submit("token", request(item(101L, "PASS"), item(102L, "REJECT")));

        assertEquals(true, response.get("submitted"));
        assertEquals(2, response.get("voteCount"));
        assertEquals(true, response.get("allCompleted"));
        assertEquals("CLOSED", response.get("activityStatus"));
        ArgumentCaptor<List<Vote>> votesCaptor = ArgumentCaptor.forClass(List.class);
        verify(voteMapper).batchInsertVote(votesCaptor.capture());
        assertEquals(2, votesCaptor.getValue().size());
        assertEquals(101L, votesCaptor.getValue().get(0).getActivityCandidateId());
        ArgumentCaptor<Activity> activityCaptor = ArgumentCaptor.forClass(Activity.class);
        verify(activityMapper).updateActivity(activityCaptor.capture());
        assertEquals("CLOSED", activityCaptor.getValue().getStatus());
    }

    @Test
    void submitRejectsWhenNoVoteCandidates()
    {
        when(activityVoterMapper.selectActivityVoterByTokenForUpdate("token")).thenReturn(voter("PENDING"));
        when(activityMapper.selectActivityByIdForUpdate(1L)).thenReturn(activity("VOTING"));
        when(activityCandidateMapper.selectVoteCandidatesByActivityId(1L)).thenReturn(Arrays.asList());

        ServiceException ex = assertThrows(ServiceException.class, () -> service.submit("token", request()));

        assertTrue(String.valueOf(ex.getMessage()).length() > 0);
        verify(voteMapper, never()).batchInsertVote(any());
    }

    @Test
    void getEntryReportsUnavailableReasonBeforeStart()
    {
        Activity activity = activity("PUBLISHED");
        activity.setStartTime(new Date(System.currentTimeMillis() + 60_000));
        ActivityVoter voter = voter("PENDING");
        when(activityVoterMapper.selectActivityVoterByToken("token")).thenReturn(voter);
        when(activityMapper.selectActivityById(1L)).thenReturn(activity);
        when(activityCandidateMapper.countVoteCandidatesByActivityId(1L)).thenReturn(2);
        when(activityVoterMapper.countActivityVoterByActivityId(1L)).thenReturn(1);
        when(activityVoterMapper.countDoneByActivityId(1L)).thenReturn(0);

        Map<String, Object> entry = service.getEntry("token", false);

        assertEquals(false, entry.get("canVote"));
        assertTrue(String.valueOf(entry.get("unavailableReason")).contains("尚未开始"));
    }

    @Test
    void getEntryReportsNoVoteRequiredAndAllCompleted()
    {
        Activity activity = activity("PUBLISHED");
        activity.setVoteEntryKey("entry");
        when(activityVoterMapper.selectActivityVoterByToken("entry")).thenReturn(null);
        when(activityMapper.selectActivityByVoteEntryKey("entry")).thenReturn(activity);
        when(activityCandidateMapper.countVoteCandidatesByActivityId(1L)).thenReturn(0);

        Map<String, Object> entry = service.getEntry("entry", false);

        assertEquals(true, entry.get("canVote"));
        assertEquals(true, entry.get("noVoteRequired"));
        assertEquals(true, entry.get("allCompleted"));
        assertEquals(0, entry.get("candidateCount"));
    }

    @Test
    void confirmSharedEntryFindsVoterByIdentity()
    {
        Activity activity = activity("VOTING");
        activity.setVoteEntryKey("entry");
        when(activityVoterMapper.selectActivityVoterByToken("entry")).thenReturn(null);
        when(activityMapper.selectActivityByVoteEntryKey("entry")).thenReturn(activity);
        when(activityVoterMapper.selectActivityVoterByIdentity(any())).thenReturn(voter("PENDING"));
        when(activityCandidateMapper.countVoteCandidatesByActivityId(1L)).thenReturn(2);
        when(activityVoterMapper.countActivityVoterByActivityId(1L)).thenReturn(1);
        when(activityVoterMapper.countDoneByActivityId(1L)).thenReturn(0);

        VoteConfirmRequest request = new VoteConfirmRequest();
        request.setName(" 张三 ");
        request.setEmployeeId(" E001 ");
        Map<String, Object> result = service.confirm("entry", request);

        assertEquals(true, result.get("confirmed"));
        assertEquals(true, result.get("canVote"));
    }

    @Test
    void confirmRejectsIdentityMismatch()
    {
        Activity activity = activity("VOTING");
        when(activityVoterMapper.selectActivityVoterByToken("token")).thenReturn(voter("PENDING"));
        when(activityMapper.selectActivityById(1L)).thenReturn(activity);
        VoteConfirmRequest request = new VoteConfirmRequest();
        request.setName("李四");
        request.setEmployeeId("E001");

        assertThrows(ServiceException.class, () -> service.confirm("token", request));
    }

    @Test
    void listCandidatesReturnsVoteScopeRows()
    {
        ActivityVoter voter = voter("PENDING");
        when(activityVoterMapper.selectActivityVoterByToken("token")).thenReturn(voter);
        when(activityMapper.selectActivityById(1L)).thenReturn(activity("VOTING"));
        ActivityCandidate candidate = candidate(101L);
        candidate.setName("张三");
        candidate.setCompany("一公司");
        candidate.setDepartment("船体组");
        candidate.setAppliedLevel("工程师");
        when(activityCandidateMapper.selectVoteCandidatesByActivityId(1L)).thenReturn(Arrays.asList(candidate));

        List<Map<String, Object>> rows = service.listCandidates("token");

        assertEquals(1, rows.size());
        assertEquals(101L, rows.get(0).get("activityCandidateId"));
        assertEquals("张三", rows.get(0).get("name"));
    }

    @Test
    void resultRequiresVoterToken()
    {
        when(activityVoterMapper.selectActivityVoterByToken("entry")).thenReturn(null);
        when(activityMapper.selectActivityByVoteEntryKey("entry")).thenReturn(activity("CALCULATED"));

        assertThrows(ServiceException.class, () -> service.result("entry"));
    }

    private void mockOpenVotingContext()
    {
        when(activityVoterMapper.selectActivityVoterByTokenForUpdate("token")).thenReturn(voter("PENDING"));
        when(activityMapper.selectActivityByIdForUpdate(1L)).thenReturn(activity("VOTING"));
        when(activityCandidateMapper.selectVoteCandidatesByActivityId(1L)).thenReturn(Arrays.asList(candidate(101L), candidate(102L)));
        when(activityVoterMapper.countActivityVoterByActivityId(1L)).thenReturn(1);
        when(activityVoterMapper.countDoneByActivityId(1L)).thenReturn(0);
    }

    private Activity activity(String status)
    {
        Activity activity = new Activity();
        activity.setId(1L);
        activity.setStatus(status);
        activity.setStartTime(new Date(System.currentTimeMillis() - 60_000));
        activity.setEndTime(new Date(System.currentTimeMillis() + 60_000));
        return activity;
    }

    private ActivityVoter voter(String status)
    {
        ActivityVoter voter = new ActivityVoter();
        voter.setId(10L);
        voter.setActivityId(1L);
        voter.setName("张三");
        voter.setEmployeeId("E001");
        voter.setStatus(status);
        return voter;
    }

    private ActivityCandidate candidate(Long id)
    {
        ActivityCandidate candidate = new ActivityCandidate();
        candidate.setId(id);
        candidate.setActivityId(1L);
        candidate.setFixedType("VOTE");
        return candidate;
    }

    private VoteSubmitRequest request(VoteSubmitItem... items)
    {
        VoteSubmitRequest request = new VoteSubmitRequest();
        request.setRoundNo(1);
        request.setVotes(Arrays.asList(items));
        return request;
    }

    private VoteSubmitItem item(Long candidateId, String result)
    {
        VoteSubmitItem item = new VoteSubmitItem();
        item.setActivityCandidateId(candidateId);
        item.setResult(result);
        return item;
    }
}
