package com.ruoyi.evaluation.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.io.File;
import java.io.FileInputStream;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.test.util.ReflectionTestUtils;
import org.junit.jupiter.api.io.TempDir;
import com.ruoyi.common.config.RuoYiConfig;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.evaluation.domain.Activity;
import com.ruoyi.evaluation.domain.ActivityCandidate;
import com.ruoyi.evaluation.domain.ExportJob;
import com.ruoyi.evaluation.domain.FinalEvaluation;
import com.ruoyi.evaluation.domain.ResultAgg;
import com.ruoyi.evaluation.domain.RuleConfig;
import com.ruoyi.evaluation.domain.Vote;
import com.ruoyi.evaluation.mapper.ActivityCandidateMapper;
import com.ruoyi.evaluation.mapper.ActivityMapper;
import com.ruoyi.evaluation.mapper.ActivityVoterMapper;
import com.ruoyi.evaluation.mapper.ExportJobMapper;
import com.ruoyi.evaluation.mapper.FinalEvaluationMapper;
import com.ruoyi.evaluation.mapper.ResultAggMapper;
import com.ruoyi.evaluation.mapper.RuleConfigMapper;
import com.ruoyi.evaluation.mapper.VoteMapper;

class ResultAggServiceImplTest
{
    private ResultAggServiceImpl service;

    @Mock
    private ActivityMapper activityMapper;

    @Mock
    private ActivityCandidateMapper activityCandidateMapper;

    @Mock
    private ActivityVoterMapper activityVoterMapper;

    @Mock
    private VoteMapper voteMapper;

    @Mock
    private RuleConfigMapper ruleConfigMapper;

    @Mock
    private ResultAggMapper resultAggMapper;

    @Mock
    private ExportJobMapper exportJobMapper;

    @Mock
    private FinalEvaluationMapper finalEvaluationMapper;

    @TempDir
    private File tempDir;

    @BeforeEach
    void setUp()
    {
        MockitoAnnotations.openMocks(this);
        service = new ResultAggServiceImpl();
        ReflectionTestUtils.setField(service, "activityMapper", activityMapper);
        ReflectionTestUtils.setField(service, "activityCandidateMapper", activityCandidateMapper);
        ReflectionTestUtils.setField(service, "activityVoterMapper", activityVoterMapper);
        ReflectionTestUtils.setField(service, "voteMapper", voteMapper);
        ReflectionTestUtils.setField(service, "ruleConfigMapper", ruleConfigMapper);
        ReflectionTestUtils.setField(service, "resultAggMapper", resultAggMapper);
        ReflectionTestUtils.setField(service, "exportJobMapper", exportJobMapper);
        ReflectionTestUtils.setField(service, "finalEvaluationMapper", finalEvaluationMapper);
        new RuoYiConfig().setProfile(tempDir.getAbsolutePath());
    }

    @Test
    void calculateLocksFixedResultsAndRanksVoteRows()
    {
        when(activityMapper.selectActivityByIdForUpdate(1L)).thenReturn(activity("CLOSED"));
        when(activityMapper.selectActivityById(1L)).thenReturn(activity("CALCULATED"));
        when(activityVoterMapper.countActivityVoterByActivityId(1L)).thenReturn(3);
        when(activityVoterMapper.countDoneByActivityId(1L)).thenReturn(3);
        when(activityCandidateMapper.countVoteCandidatesByActivityId(1L)).thenReturn(2);
        when(resultAggMapper.countResultAggByActivityId(1L)).thenReturn(0);
        when(activityCandidateMapper.selectAllCandidatesByActivityId(1L)).thenReturn(Arrays.asList(
                candidate(101L, 1, "PASS"),
                candidate(102L, 2, "VOTE"),
                candidate(103L, 3, "VOTE"),
                candidate(104L, 4, "REJECT")));
        RuleConfig rule = new RuleConfig();
        rule.setActivityId(1L);
        rule.setPassRatio(BigDecimal.valueOf(50));
        when(ruleConfigMapper.selectRuleConfigByActivityId(1L)).thenReturn(rule);
        when(voteMapper.selectVoteSummaryByActivityId(1L)).thenReturn(Arrays.asList(
                voteSummary(102L, "PASS", 2),
                voteSummary(102L, "REJECT", 1),
                voteSummary(103L, "PASS", 2),
                voteSummary(103L, "REJECT", 0)));

        service.calculate(1L, "hr", false);

        ArgumentCaptor<List<ResultAgg>> captor = ArgumentCaptor.forClass(List.class);
        verify(resultAggMapper).batchInsertResultAgg(captor.capture());
        List<ResultAgg> rows = captor.getValue();
        ResultAgg fixedPass = row(rows, 101L);
        ResultAgg firstVote = row(rows, 103L);
        ResultAgg secondVote = row(rows, 102L);
        ResultAgg fixedReject = row(rows, 104L);
        assertEquals("PASS", fixedPass.getFinalResult());
        assertEquals(1, fixedPass.getRankNo());
        assertEquals("PASS", firstVote.getFinalResult());
        assertEquals(1, firstVote.getRankNo());
        assertEquals("REJECT", secondVote.getFinalResult());
        assertEquals(2, secondVote.getRankNo());
        assertEquals("REJECT", fixedReject.getFinalResult());
        assertEquals(4, fixedReject.getRankNo());
    }

    @Test
    void calculateUsesImportSeqAsTieBreaker()
    {
        when(activityMapper.selectActivityByIdForUpdate(1L)).thenReturn(activity("CLOSED"));
        when(activityMapper.selectActivityById(1L)).thenReturn(activity("CALCULATED"));
        when(activityVoterMapper.countActivityVoterByActivityId(1L)).thenReturn(2);
        when(activityVoterMapper.countDoneByActivityId(1L)).thenReturn(2);
        when(activityCandidateMapper.countVoteCandidatesByActivityId(1L)).thenReturn(2);
        when(resultAggMapper.countResultAggByActivityId(1L)).thenReturn(0);
        when(activityCandidateMapper.selectAllCandidatesByActivityId(1L)).thenReturn(Arrays.asList(
                candidate(201L, 2, "VOTE"),
                candidate(202L, 1, "VOTE")));
        RuleConfig rule = new RuleConfig();
        rule.setPassRatio(BigDecimal.valueOf(50));
        when(ruleConfigMapper.selectRuleConfigByActivityId(1L)).thenReturn(rule);
        when(voteMapper.selectVoteSummaryByActivityId(1L)).thenReturn(Arrays.asList(
                voteSummary(201L, "PASS", 1),
                voteSummary(201L, "REJECT", 1),
                voteSummary(202L, "PASS", 1),
                voteSummary(202L, "REJECT", 1)));

        service.calculate(1L, "hr", false);

        ArgumentCaptor<List<ResultAgg>> captor = ArgumentCaptor.forClass(List.class);
        verify(resultAggMapper).batchInsertResultAgg(captor.capture());
        assertEquals("PASS", row(captor.getValue(), 202L).getFinalResult());
        assertEquals(1, row(captor.getValue(), 202L).getRankNo());
        assertEquals("REJECT", row(captor.getValue(), 201L).getFinalResult());
        assertEquals(2, row(captor.getValue(), 201L).getRankNo());
    }

    @Test
    void voteSummaryBuildsDisplayRowsForAllCandidatesWithoutChangingExportDetails()
    {
        when(activityMapper.selectActivityById(1L)).thenReturn(activity("CALCULATED"));
        ResultAgg fixedReject = exportRow(101L, "船体组", "中级工程师", "REJECT", "REJECT", 0, 0);
        fixedReject.setRankNo(2);
        ResultAgg voted = exportRow(102L, "船体组", "副高级工程师", "PASS", "VOTE", 6, 2);
        voted.setRankNo(1);
        ResultAgg fixedPass = exportRow(103L, "船体组", "副高级工程师", "PASS", "PASS", 0, 0);
        fixedPass.setRankNo(1);
        when(resultAggMapper.selectResultAggList(any())).thenReturn(Arrays.asList(fixedReject, voted, fixedPass));
        Vote exportOnlyDetail = voteDetail("船体组", "E001", "评委一", 102L, "投票候选人", "副高级工程师", "PASS");
        when(voteMapper.selectVoteDetailByActivityId(1L)).thenReturn(Arrays.asList(exportOnlyDetail));
        when(activityVoterMapper.selectActivityVoterList(any())).thenReturn(Arrays.asList(voter("船体组", "DONE")));

        Map<String, Object> summary = service.voteSummary(1L);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> displayRows = (List<Map<String, Object>>) summary.get("displayRows");
        assertEquals(Arrays.asList("候选人103", "候选人102", "候选人101"), displayRows.stream()
                .map(row -> String.valueOf(row.get("candidateName"))).collect(java.util.stream.Collectors.toList()));
        assertEquals("固定通过", displayRows.get(0).get("voteStatus"));
        assertEquals("推荐 6 票 / 淘汰 2 票", displayRows.get(1).get("voteStatus"));
        assertEquals("固定淘汰", displayRows.get(2).get("voteStatus"));

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> detailRows = (List<Map<String, Object>>) summary.get("detailRows");
        assertEquals(1, detailRows.size());
        assertEquals("推荐类别", detailRows.get(0).containsKey("recommendCategory") ? "推荐类别" : "");
    }

    @Test
    void voteSummaryOrdersAllCandidatesByFixedAndVotingResultBuckets()
    {
        when(activityMapper.selectActivityById(1L)).thenReturn(activity("CALCULATED"));
        ResultAgg fixedReject = exportRow(101L, "船体组", "中级工程师", "REJECT", "REJECT", 0, 0);
        fixedReject.setRankNo(1);
        ResultAgg votedReject = exportRow(102L, "船体组", "中级工程师", "REJECT", "VOTE", 3, 5);
        votedReject.setRankNo(2);
        ResultAgg votedPass = exportRow(103L, "船体组", "中级工程师", "PASS", "VOTE", 6, 2);
        votedPass.setRankNo(3);
        ResultAgg fixedPass = exportRow(104L, "船体组", "中级工程师", "PASS", "PASS", 0, 0);
        fixedPass.setRankNo(4);
        when(resultAggMapper.selectResultAggList(any())).thenReturn(Arrays.asList(
                fixedReject, votedReject, votedPass, fixedPass));
        when(voteMapper.selectVoteDetailByActivityId(1L)).thenReturn(Arrays.asList());
        when(activityVoterMapper.selectActivityVoterList(any())).thenReturn(Arrays.asList());

        Map<String, Object> summary = service.voteSummary(1L);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> displayRows = (List<Map<String, Object>>) summary.get("displayRows");
        assertEquals(Arrays.asList("候选人104", "候选人103", "候选人102", "候选人101"), displayRows.stream()
                .map(row -> String.valueOf(row.get("candidateName"))).collect(java.util.stream.Collectors.toList()));
        assertEquals(Arrays.asList(1, 2, 3, 4), displayRows.stream()
                .map(row -> ((Number) row.get("displayBucket")).intValue()).collect(java.util.stream.Collectors.toList()));
    }

    @Test
    void calculateRejectsPendingVoters()
    {
        when(activityMapper.selectActivityByIdForUpdate(1L)).thenReturn(activity("CLOSED"));
        when(activityCandidateMapper.countVoteCandidatesByActivityId(1L)).thenReturn(1);
        when(activityVoterMapper.countActivityVoterByActivityId(1L)).thenReturn(3);
        when(activityVoterMapper.countDoneByActivityId(1L)).thenReturn(2);

        ServiceException ex = assertThrows(ServiceException.class, () -> service.calculate(1L, "hr", false));

        assertEquals("All voters must submit before close or calculation. Pending voters: 1", ex.getMessage());
        verify(resultAggMapper, never()).batchInsertResultAgg(any());
    }

    @Test
    void calculateForceRebuildsCalculatedRowsWithoutFinalEvaluationGate()
    {
        when(activityMapper.selectActivityByIdForUpdate(1L)).thenReturn(activity("CALCULATED"));
        when(activityMapper.selectActivityById(1L)).thenReturn(activity("CALCULATED"));
        when(activityCandidateMapper.countVoteCandidatesByActivityId(1L)).thenReturn(1);
        when(activityVoterMapper.countActivityVoterByActivityId(1L)).thenReturn(1);
        when(activityVoterMapper.countDoneByActivityId(1L)).thenReturn(1);
        when(activityCandidateMapper.selectAllCandidatesByActivityId(1L)).thenReturn(Arrays.asList(candidate(201L, 1, "VOTE")));
        RuleConfig rule = new RuleConfig();
        rule.setActivityId(1L);
        rule.setPassRatio(BigDecimal.valueOf(100));
        when(ruleConfigMapper.selectRuleConfigByActivityId(1L)).thenReturn(rule);
        when(voteMapper.selectVoteSummaryByActivityId(1L)).thenReturn(Arrays.asList(voteSummary(201L, "PASS", 1)));

        service.calculate(1L, "hr", true);

        verify(resultAggMapper).deleteResultAggByActivityId(1L);
        verify(resultAggMapper).batchInsertResultAgg(any());
    }

    @Test
    void calculateAutoPassesAssistantAndTechnicianWithoutVoteCandidates()
    {
        when(activityMapper.selectActivityByIdForUpdate(1L)).thenReturn(activity("CLOSED"));
        when(activityMapper.selectActivityById(1L)).thenReturn(activity("CALCULATED"));
        when(activityCandidateMapper.countVoteCandidatesByActivityId(1L)).thenReturn(0);
        when(resultAggMapper.countResultAggByActivityId(1L)).thenReturn(0);
        ActivityCandidate first = candidate(301L, 1, null);
        first.setAppliedLevel("助理工程师");
        ActivityCandidate second = candidate(302L, 2, null);
        second.setAppliedLevel("技术员");
        when(activityCandidateMapper.selectAllCandidatesByActivityId(1L)).thenReturn(Arrays.asList(first, second));
        RuleConfig rule = new RuleConfig();
        rule.setActivityId(1L);
        rule.setPassRatio(BigDecimal.valueOf(50));
        when(ruleConfigMapper.selectRuleConfigByActivityId(1L)).thenReturn(rule);

        service.calculate(1L, "hr", false);

        ArgumentCaptor<List<ResultAgg>> captor = ArgumentCaptor.forClass(List.class);
        verify(resultAggMapper).batchInsertResultAgg(captor.capture());
        assertEquals("PASS", row(captor.getValue(), 301L).getFinalResult());
        assertEquals("PASS", row(captor.getValue(), 302L).getFinalResult());
    }

    @Test
    void calculateAllowsFixedOnlyResultsWithoutVoters()
    {
        when(activityMapper.selectActivityByIdForUpdate(1L)).thenReturn(activity("CLOSED"));
        when(activityMapper.selectActivityById(1L)).thenReturn(activity("CALCULATED"));
        when(activityCandidateMapper.countVoteCandidatesByActivityId(1L)).thenReturn(0);
        when(resultAggMapper.countResultAggByActivityId(1L)).thenReturn(0);
        when(activityCandidateMapper.selectAllCandidatesByActivityId(1L)).thenReturn(Arrays.asList(
                candidate(401L, 1, "PASS"),
                candidate(402L, 2, "REJECT")));
        RuleConfig rule = new RuleConfig();
        rule.setActivityId(1L);
        rule.setPassRatio(BigDecimal.valueOf(50));
        when(ruleConfigMapper.selectRuleConfigByActivityId(1L)).thenReturn(rule);

        service.calculate(1L, "hr", false);

        ArgumentCaptor<List<ResultAgg>> captor = ArgumentCaptor.forClass(List.class);
        verify(resultAggMapper).batchInsertResultAgg(captor.capture());
        assertEquals("PASS", row(captor.getValue(), 401L).getFinalResult());
        assertEquals("REJECT", row(captor.getValue(), 402L).getFinalResult());
    }

    @Test
    void normalizeExportTypeAcceptsFigureAliases()
    {
        assertEquals("VOTE_SUMMARY", ReflectionTestUtils.invokeMethod(service, "normalizeExportType", "figure2"));
        assertEquals("FINAL_DECISION", ReflectionTestUtils.invokeMethod(service, "normalizeExportType", "figure5"));
        assertEquals("FINAL_DECISION", ReflectionTestUtils.invokeMethod(service, "normalizeExportType", "final"));
    }

    @Test
    void maskIdCardKeepsEndsOnly()
    {
        assertEquals("1101**********1234", ReflectionTestUtils.invokeMethod(service, "maskIdCard", "110101199001011234"));
        assertEquals("12345678", ReflectionTestUtils.invokeMethod(service, "maskIdCard", "12345678"));
    }

    @Test
    void exportRowsPlacesPassBeforeRejectAndUsesTitleLevelOrder()
    {
        ResultAgg rejectSenior = exportRow(101L, "船体组", "副高级工程师", "REJECT", "VOTE", 1, 2);
        ResultAgg passTechnician = exportRow(102L, "船体组", "技术员", "PASS", "PASS", 0, 0);
        ResultAgg passIntermediate = exportRow(103L, "船体组", "中级工程师", "PASS", "VOTE", 3, 0);
        ResultAgg rejectAssistant = exportRow(104L, "船体组", "助理工程师", "REJECT", "REJECT", 0, 0);
        ResultAgg passSenior = exportRow(105L, "船体组", "副高级工程师", "PASS", "VOTE", 3, 0);

        @SuppressWarnings("unchecked")
        List<ResultAgg> sorted = ReflectionTestUtils.invokeMethod(service, "exportRows",
                Arrays.asList(rejectSenior, passTechnician, passIntermediate, rejectAssistant, passSenior));

        assertEquals(Arrays.asList(105L, 103L, 102L, 101L, 104L), sorted.stream()
                .map(ResultAgg::getActivityCandidateId).collect(java.util.stream.Collectors.toList()));
    }

    @Test
    void exportFigure5AliasRequiresConfirmedHrFinalEvaluation() throws Exception
    {
        mockCalculatedExportContext("CALCULATED");

        ServiceException ex = assertThrows(ServiceException.class,
                () -> service.export(1L, "FIGURE5", "hr"));

        assertEquals("Please confirm FinalEvaluation before exporting the final decision.", ex.getMessage());
    }

    @Test
    void exportFinalDecisionUsesConfirmedHrDecision() throws Exception
    {
        mockCalculatedExportContext("CALCULATED");
        when(exportJobMapper.insertExportJob(any())).thenReturn(1);
        when(finalEvaluationMapper.selectFinalEvaluationList(any())).thenReturn(Arrays.asList(
                finalEvaluation(101L, "REJECT", "CONFIRMED")));

        ExportJob job = service.export(1L, "FINAL_DECISION", "hr");

        File exported = new File(RuoYiConfig.getDownloadPath(), job.getFileName().replace("/", File.separator));
        try (FileInputStream input = new FileInputStream(exported);
             org.apache.poi.ss.usermodel.Workbook workbook = WorkbookFactory.create(input))
        {
            assertEquals("REJECT", workbook.getSheetAt(0).getRow(1).getCell(16).getStringCellValue());
        }
    }

    @Test
    void selectResultAggListBuildsGroupSummaryRows()
    {
        ResultAgg query = new ResultAgg();
        query.setActivityId(1L);
        ResultAgg pass = exportRow(101L, "船体组", "工程师", "PASS", "VOTE", 3, 0);
        pass.setCalculatedAt(new java.util.Date(1_000));
        ResultAgg reject = exportRow(102L, "船体组", "工程师", "REJECT", "VOTE", 1, 2);
        reject.setCalculatedAt(new java.util.Date(2_000));
        when(resultAggMapper.selectResultAggList(any())).thenReturn(Arrays.asList(pass, reject));

        List<java.util.Map<String, Object>> rows = service.selectResultAggList(query);

        assertEquals(1, rows.size());
        assertEquals("船体组", rows.get(0).get("department"));
        assertEquals(2, rows.get(0).get("candidateCount"));
        assertEquals(1, rows.get(0).get("passCount"));
        assertEquals(1, rows.get(0).get("rejectCount"));
        assertEquals("1970-01-01 08:00:02", rows.get(0).get("calculatedAt"));
    }

    @Test
    void selectCandidateResultListReturnsCalculatedCandidateRows()
    {
        ResultAgg query = new ResultAgg();
        query.setActivityId(1L);
        ResultAgg pass = exportRow(101L, "船体组", "工程师", "PASS", "VOTE", 3, 0);
        ResultAgg reject = exportRow(102L, "船体组", "工程师", "REJECT", "VOTE", 1, 2);
        when(resultAggMapper.selectResultAggList(any())).thenReturn(Arrays.asList(pass, reject));

        List<ResultAgg> rows = service.selectCandidateResultList(query);

        assertEquals(Arrays.asList(pass, reject), rows);
        ArgumentCaptor<ResultAgg> captor = ArgumentCaptor.forClass(ResultAgg.class);
        verify(resultAggMapper).selectResultAggList(captor.capture());
        assertEquals("CANDIDATE_GROUP", captor.getValue().getStatScope());
    }

    @Test
    void publicResultHidesRowsUntilCalculatedAndCompleted()
    {
        when(activityMapper.selectActivityById(1L)).thenReturn(activity("CLOSED"));
        when(activityCandidateMapper.selectAllCandidatesByActivityId(1L)).thenReturn(Arrays.asList(candidate(101L, 1, "VOTE")));
        when(activityCandidateMapper.countVoteCandidatesByActivityId(1L)).thenReturn(1);
        when(activityVoterMapper.countActivityVoterByActivityId(1L)).thenReturn(2);
        when(activityVoterMapper.countDoneByActivityId(1L)).thenReturn(1);
        when(resultAggMapper.countResultAggByActivityId(1L)).thenReturn(0);

        java.util.Map<String, Object> result = service.publicResult(1L);

        assertEquals(false, result.get("allCompleted"));
        assertEquals(0, ((List<?>) result.get("resultRows")).size());
    }

    @Test
    void publicResultReturnsRowsWhenCalculatedAndCompleted()
    {
        when(activityMapper.selectActivityById(1L)).thenReturn(activity("CALCULATED"));
        when(activityCandidateMapper.selectAllCandidatesByActivityId(1L)).thenReturn(Arrays.asList(candidate(101L, 1, "VOTE")));
        when(activityCandidateMapper.countVoteCandidatesByActivityId(1L)).thenReturn(1);
        when(activityVoterMapper.countActivityVoterByActivityId(1L)).thenReturn(1);
        when(activityVoterMapper.countDoneByActivityId(1L)).thenReturn(1);
        when(resultAggMapper.countResultAggByActivityId(1L)).thenReturn(1);
        ResultAgg row = exportRow(101L, "船体组", "工程师", "PASS", "VOTE", 3, 0);
        when(resultAggMapper.selectResultAggList(any())).thenReturn(Arrays.asList(row));

        java.util.Map<String, Object> result = service.publicResult(1L);

        List<?> rows = (List<?>) result.get("resultRows");
        assertEquals(1, rows.size());
        assertEquals(true, result.get("allCompleted"));
    }

    @Test
    void exportVoteSummaryUsesExampleWorkbookStructureAndChineseVoteText() throws Exception
    {
        mockCalculatedExportContext("CALCULATED");
        when(exportJobMapper.insertExportJob(any())).thenReturn(1);
        when(voteMapper.selectVoteDetailByActivityId(1L)).thenReturn(Arrays.asList(
                voteDetail("船体组", "E001", "评委一", 101L, "张三", "副高级工程师", "PASS"),
                voteDetail("船体组", "E001", "评委一", 102L, "李四", "中级工程师", "REJECT")));

        ExportJob job = service.export(1L, "VOTE_SUMMARY", "hr");

        assertEquals("SUCCESS", job.getStatus());
        File exported = new File(RuoYiConfig.getDownloadPath(), job.getFileName().replace("/", File.separator));
        try (FileInputStream input = new FileInputStream(exported);
             org.apache.poi.ss.usermodel.Workbook workbook = WorkbookFactory.create(input))
        {
            assertEquals("投票情况", workbook.getSheetAt(0).getSheetName());
            assertHeader(workbook.getSheetAt(0), "主体名称", "人数", "未投票", "未提交", "已投票");
            assertEquals("1、船体组", workbook.getSheetAt(1).getSheetName());
            assertHeader(workbook.getSheetAt(1), "序列码", "推荐类别", "候选人", "编号", "单位", "部门", "岗位", "投票情况");
            assertEquals("推荐", workbook.getSheetAt(1).getRow(1).getCell(7).getStringCellValue());
            assertEquals("不推荐", workbook.getSheetAt(1).getRow(2).getCell(7).getStringCellValue());
        }
    }

    @Test
    void exportStandardTypesCreateExpectedWorkbookOrZip() throws Exception
    {
        java.util.Map<String, String> sheetNames = new java.util.HashMap<>();
        sheetNames.put("VOTE_SUMMARY", "投票情况");
        sheetNames.put("PASS_DECISION", "图4_通过表决");
        sheetNames.put("FINAL_DECISION", "最终总榜单");
        for (String type : Arrays.asList("VOTE_SUMMARY", "PASS_DECISION", "FINAL_DECISION", "STAT_RESULT"))
        {
            mockCalculatedExportContext("CALCULATED");
            when(exportJobMapper.insertExportJob(any())).thenReturn(1);
            if ("FINAL_DECISION".equals(type))
            {
                when(finalEvaluationMapper.selectFinalEvaluationList(any())).thenReturn(Arrays.asList(
                        finalEvaluation(101L, "PASS", "CONFIRMED")));
            }
            ExportJob job = service.export(1L, type, "hr");
            assertEquals("SUCCESS", job.getStatus());
            File exported = new File(RuoYiConfig.getDownloadPath(), job.getFileName().replace("/", File.separator));
            assertEquals(true, exported.exists());
            if (!"STAT_RESULT".equals(type))
            {
                try (FileInputStream input = new FileInputStream(exported);
                     org.apache.poi.ss.usermodel.Workbook workbook = WorkbookFactory.create(input))
                {
                    assertEquals("VOTE_SUMMARY".equals(type) ? 2 : 1, workbook.getNumberOfSheets());
                    assertEquals(sheetNames.get(type), workbook.getSheetAt(0).getSheetName());
                }
            }
            else
            {
                assertEquals(true, exported.getName().endsWith(".zip"));
            }
        }
    }

    private Activity activity(String status)
    {
        Activity activity = new Activity();
        activity.setId(1L);
        activity.setName("测试活动");
        activity.setStatus(status);
        return activity;
    }

    private ActivityCandidate candidate(Long id, int importSeq, String fixedType)
    {
        ActivityCandidate candidate = new ActivityCandidate();
        candidate.setId(id);
        candidate.setActivityId(1L);
        candidate.setDepartment("船体组");
        candidate.setAppliedLevel("工程师");
        candidate.setImportSeq(importSeq);
        candidate.setFixedType(fixedType);
        return candidate;
    }

    private Vote voteSummary(Long activityCandidateId, String result, int count)
    {
        Vote vote = new Vote();
        vote.setActivityCandidateId(activityCandidateId);
        vote.setResult(result);
        vote.setVoteCount(count);
        return vote;
    }

    private void mockCalculatedExportContext(String status)
    {
        Activity activity = activity(status);
        when(activityMapper.selectActivityById(1L)).thenReturn(activity);
        when(resultAggMapper.countResultAggByActivityId(1L)).thenReturn(1);
        ResultAgg result = exportRow(101L, "船体组", "工程师", "PASS", "VOTE", 3, 0);
        result.setId(501L);
        when(resultAggMapper.selectResultAggList(any())).thenReturn(Arrays.asList(result));
        RuleConfig rule = new RuleConfig();
        rule.setPassRatio(BigDecimal.valueOf(50));
        when(ruleConfigMapper.selectRuleConfigByActivityId(1L)).thenReturn(rule);
        when(activityVoterMapper.selectActivityVoterList(any())).thenReturn(Arrays.asList(
                voter("船体组", "DONE"),
                voter("船体组", "PENDING")));
        when(voteMapper.selectVoteDetailByActivityId(1L)).thenReturn(Arrays.asList());
    }

    private ResultAgg exportRow(Long candidateId, String department, String appliedLevel, String finalResult,
            String fixedType, int pass, int reject)
    {
        ResultAgg result = new ResultAgg();
        result.setActivityId(1L);
        result.setActivityCandidateId(candidateId);
        result.setStatScope("CANDIDATE_GROUP");
        result.setDepartment(department);
        result.setThirdLevelDepartment(department + "专业");
        result.setAppliedLevel(appliedLevel);
        result.setImportSeq(candidateId.intValue());
        result.setCandidateName("候选人" + candidateId);
        result.setCompany("一公司");
        result.setPosition("设计师");
        result.setIdCard("110101199001011234");
        result.setFixedType(fixedType);
        result.setVotePassCount(pass);
        result.setVoteRejectCount(reject);
        result.setTotalVotes(pass + reject);
        result.setPassRate(BigDecimal.valueOf(pass).divide(BigDecimal.valueOf(Math.max(pass + reject, 1)), 4, java.math.RoundingMode.HALF_UP));
        result.setRejectRate(BigDecimal.valueOf(reject).divide(BigDecimal.valueOf(Math.max(pass + reject, 1)), 4, java.math.RoundingMode.HALF_UP));
        result.setRankNo(1);
        result.setFinalResult(finalResult);
        result.setCalculatedAt(new java.util.Date());
        return result;
    }

    private FinalEvaluation finalEvaluation(Long activityCandidateId, String finalResult, String confirmStatus)
    {
        FinalEvaluation row = new FinalEvaluation();
        row.setActivityId(1L);
        row.setActivityCandidateId(activityCandidateId);
        row.setFinalResult(finalResult);
        row.setConfirmStatus(confirmStatus);
        return row;
    }

    private com.ruoyi.evaluation.domain.ActivityVoter voter(String department, String status)
    {
        com.ruoyi.evaluation.domain.ActivityVoter voter = new com.ruoyi.evaluation.domain.ActivityVoter();
        voter.setActivityId(1L);
        voter.setDepartment(department);
        voter.setStatus(status);
        return voter;
    }

    private Vote voteDetail(String voterDepartment, String voterEmployeeId, String voterName,
            Long candidateId, String candidateName, String appliedLevel, String result)
    {
        Vote vote = new Vote();
        vote.setActivityId(1L);
        vote.setActivityVoterId(11L);
        vote.setActivityCandidateId(candidateId);
        vote.setResult(result);
        vote.setVoterDepartment(voterDepartment);
        vote.setVoterEmployeeId(voterEmployeeId);
        vote.setVoterName(voterName);
        vote.setCandidateName(candidateName);
        vote.setImportSeq(candidateId.intValue());
        vote.setCompany("一公司");
        vote.setDepartment("船体组");
        vote.setAppliedLevel(appliedLevel);
        vote.setPosition("设计师");
        return vote;
    }

    private void assertHeader(org.apache.poi.ss.usermodel.Sheet sheet, String... headers)
    {
        for (int i = 0; i < headers.length; i++)
        {
            assertEquals(headers[i], sheet.getRow(0).getCell(i).getStringCellValue());
        }
    }

    private ResultAgg row(List<ResultAgg> rows, Long activityCandidateId)
    {
        return rows.stream()
                .filter(row -> activityCandidateId.equals(row.getActivityCandidateId()))
                .findFirst()
                .orElseThrow();
    }
}
