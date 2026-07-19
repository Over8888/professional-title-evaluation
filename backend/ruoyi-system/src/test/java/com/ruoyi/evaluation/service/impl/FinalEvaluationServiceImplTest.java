package com.ruoyi.evaluation.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.test.util.ReflectionTestUtils;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.evaluation.domain.Activity;
import com.ruoyi.evaluation.domain.FinalEvaluation;
import com.ruoyi.evaluation.domain.ResultAgg;
import com.ruoyi.evaluation.mapper.ActivityMapper;
import com.ruoyi.evaluation.mapper.FinalEvaluationMapper;
import com.ruoyi.evaluation.mapper.ResultAggMapper;

class FinalEvaluationServiceImplTest
{
    private FinalEvaluationServiceImpl service;

    @Mock
    private FinalEvaluationMapper finalEvaluationMapper;

    @Mock
    private ResultAggMapper resultAggMapper;

    @Mock
    private ActivityMapper activityMapper;

    @BeforeEach
    void setUp()
    {
        MockitoAnnotations.openMocks(this);
        service = new FinalEvaluationServiceImpl();
        ReflectionTestUtils.setField(service, "finalEvaluationMapper", finalEvaluationMapper);
        ReflectionTestUtils.setField(service, "resultAggMapper", resultAggMapper);
        ReflectionTestUtils.setField(service, "activityMapper", activityMapper);
    }

    @Test
    void generateFromResultRequiresCalculatedActivity()
    {
        Activity activity = new Activity();
        activity.setId(1L);
        activity.setStatus("CLOSED");
        when(activityMapper.selectActivityById(1L)).thenReturn(activity);

        ServiceException ex = assertThrows(ServiceException.class,
                () -> service.generateFromResult(1L, "hr", false));

        assertEquals("FinalEvaluation can only be generated after calculation and before confirmation.", ex.getMessage());
        verify(finalEvaluationMapper, never()).insertFinalEvaluation(any());
    }

    @Test
    void generateFromResultCreatesDraftRowsFromCalculatedResults()
    {
        Activity activity = new Activity();
        activity.setId(1L);
        activity.setStatus("CALCULATED");
        ResultAgg pass = result(11L, 101L, "PASS");
        ResultAgg reject = result(12L, 102L, "REJECT");
        when(activityMapper.selectActivityById(1L)).thenReturn(activity);
        when(resultAggMapper.countResultAggByActivityId(1L)).thenReturn(2);
        when(finalEvaluationMapper.countFinalEvaluationByActivityId(1L)).thenReturn(0);
        when(resultAggMapper.selectResultAggList(any())).thenReturn(Arrays.asList(pass, reject));
        when(finalEvaluationMapper.insertFinalEvaluation(any())).thenReturn(1);

        int inserted = service.generateFromResult(1L, "hr", false);

        assertEquals(2, inserted);
        ArgumentCaptor<FinalEvaluation> captor = ArgumentCaptor.forClass(FinalEvaluation.class);
        verify(finalEvaluationMapper, org.mockito.Mockito.times(2)).insertFinalEvaluation(captor.capture());
        FinalEvaluation first = captor.getAllValues().get(0);
        assertEquals(1L, first.getActivityId());
        assertEquals(101L, first.getActivityCandidateId());
        assertEquals(11L, first.getResultAggId());
        assertEquals("PASS", first.getFinalResult());
        assertEquals("DRAFT", first.getConfirmStatus());
        assertEquals("PENDING", first.getSignatureStatus());
    }

    @Test
    void confirmActivityMarksRowsAndActivityConfirmed()
    {
        Activity activity = new Activity();
        activity.setId(1L);
        activity.setStatus("CALCULATED");
        FinalEvaluation signed = finalRow(1L, "leader");
        FinalEvaluation unsigned = finalRow(2L, null);
        when(activityMapper.selectActivityById(1L)).thenReturn(activity);
        when(resultAggMapper.countResultAggByActivityId(1L)).thenReturn(2);
        when(finalEvaluationMapper.countFinalEvaluationByActivityId(1L)).thenReturn(2);
        when(finalEvaluationMapper.selectFinalEvaluationList(any())).thenReturn(Arrays.asList(signed, unsigned));
        when(finalEvaluationMapper.updateFinalEvaluation(any())).thenReturn(1);

        int updated = service.confirmActivity(1L, "hr");

        assertEquals(2, updated);
        ArgumentCaptor<FinalEvaluation> rowCaptor = ArgumentCaptor.forClass(FinalEvaluation.class);
        verify(finalEvaluationMapper, org.mockito.Mockito.times(2)).updateFinalEvaluation(rowCaptor.capture());
        assertEquals("SIGNED", rowCaptor.getAllValues().get(0).getSignatureStatus());
        assertEquals("PENDING", rowCaptor.getAllValues().get(1).getSignatureStatus());
        ArgumentCaptor<Activity> activityCaptor = ArgumentCaptor.forClass(Activity.class);
        verify(activityMapper).updateActivity(activityCaptor.capture());
        assertEquals("CONFIRMED", activityCaptor.getValue().getStatus());
    }

    @Test
    void summaryCountsFinalResultAndConfirmStatus()
    {
        when(finalEvaluationMapper.selectFinalEvaluationList(any())).thenReturn(Arrays.asList(
                finalRow("PASS", "CONFIRMED"),
                finalRow("REJECT", "DRAFT"),
                finalRow("PASS", "DRAFT")));

        java.util.Map<String, Object> summary = service.summary(1L);

        assertEquals(3, summary.get("total"));
        assertEquals(2L, summary.get("pass"));
        assertEquals(1L, summary.get("reject"));
        assertEquals(1L, summary.get("confirmed"));
        assertEquals(2L, summary.get("pending"));
    }

    @Test
    void generateRejectsDuplicateWithoutReplace()
    {
        Activity activity = new Activity();
        activity.setId(1L);
        activity.setStatus("CALCULATED");
        when(activityMapper.selectActivityById(1L)).thenReturn(activity);
        when(resultAggMapper.countResultAggByActivityId(1L)).thenReturn(1);
        when(finalEvaluationMapper.countFinalEvaluationByActivityId(1L)).thenReturn(1);

        ServiceException ex = assertThrows(ServiceException.class,
                () -> service.generateFromResult(1L, "hr", false));

        assertEquals("FinalEvaluation already exists. Use replaceExisting=true to rebuild it.", ex.getMessage());
    }

    @Test
    void confirmRequiresExistingRows()
    {
        Activity activity = new Activity();
        activity.setId(1L);
        activity.setStatus("CALCULATED");
        when(activityMapper.selectActivityById(1L)).thenReturn(activity);
        when(resultAggMapper.countResultAggByActivityId(1L)).thenReturn(1);
        when(finalEvaluationMapper.countFinalEvaluationByActivityId(1L)).thenReturn(0);
        when(finalEvaluationMapper.selectFinalEvaluationList(any())).thenReturn(Collections.emptyList());

        assertThrows(ServiceException.class, () -> service.confirmActivity(1L, "hr"));
    }

    @Test
    void updateFinalEvaluationRejectsConfirmedActivity()
    {
        FinalEvaluation existing = finalRow(1L, null);
        existing.setActivityId(1L);
        when(finalEvaluationMapper.selectFinalEvaluationById(1L)).thenReturn(existing);
        when(activityMapper.selectActivityById(1L)).thenReturn(activity("CONFIRMED"));

        FinalEvaluation update = new FinalEvaluation();
        update.setId(1L);
        update.setFinalResult("PASS");

        assertThrows(ServiceException.class, () -> service.updateFinalEvaluation(update, "hr"));
    }

    @Test
    void updateFinalEvaluationValidatesResultAndStatus()
    {
        FinalEvaluation existing = finalRow(1L, null);
        existing.setActivityId(1L);
        when(finalEvaluationMapper.selectFinalEvaluationById(1L)).thenReturn(existing);
        when(activityMapper.selectActivityById(1L)).thenReturn(activity("CALCULATED"));
        when(resultAggMapper.countResultAggByActivityId(1L)).thenReturn(1);

        FinalEvaluation update = new FinalEvaluation();
        update.setId(1L);
        update.setFinalResult("MAYBE");
        assertThrows(ServiceException.class, () -> service.updateFinalEvaluation(update, "hr"));

        update.setFinalResult("PASS");
        update.setConfirmStatus("DONE");
        assertThrows(ServiceException.class, () -> service.updateFinalEvaluation(update, "hr"));
    }

    @Test
    void updateFinalEvaluationSavesEditableFieldsBeforeConfirmation()
    {
        FinalEvaluation existing = finalRow(1L, null);
        existing.setActivityId(1L);
        when(finalEvaluationMapper.selectFinalEvaluationById(1L)).thenReturn(existing);
        when(activityMapper.selectActivityById(1L)).thenReturn(activity("CALCULATED"));
        when(resultAggMapper.countResultAggByActivityId(1L)).thenReturn(1);
        when(finalEvaluationMapper.updateFinalEvaluation(any())).thenReturn(1);

        FinalEvaluation update = new FinalEvaluation();
        update.setId(1L);
        update.setFinalResult("PASS");
        update.setConfirmStatus("DRAFT");

        assertEquals(1, service.updateFinalEvaluation(update, "hr"));
        assertEquals("hr", update.getUpdateBy());
    }

    @Test
    void saveActivityFinalEvaluationsUpdatesAllRowsAndSharesSignatureAndRemark()
    {
        FinalEvaluation first = finalRow(1L, null);
        FinalEvaluation second = finalRow(2L, null);
        first.setActivityCandidateId(1L);
        second.setActivityCandidateId(2L);
        when(activityMapper.selectActivityById(1L)).thenReturn(activity("CALCULATED"));
        when(resultAggMapper.countResultAggByActivityId(1L)).thenReturn(2);
        when(finalEvaluationMapper.selectFinalEvaluationList(any())).thenReturn(Arrays.asList(first, second));
        when(resultAggMapper.selectResultAggList(any())).thenReturn(Arrays.asList(
                result(11L, 1L, "PASS"), result(12L, 2L, "PASS")));
        when(finalEvaluationMapper.updateFinalEvaluation(any())).thenReturn(1);

        FinalEvaluation change = new FinalEvaluation();
        change.setId(1L);
        change.setFinalResult("REJECT");
        int updated = service.saveActivityFinalEvaluations(1L, Arrays.asList(change), "主任", null, "hr");

        assertEquals(2, updated);
        ArgumentCaptor<FinalEvaluation> captor = ArgumentCaptor.forClass(FinalEvaluation.class);
        verify(finalEvaluationMapper, org.mockito.Mockito.times(2)).updateFinalEvaluation(captor.capture());
        assertEquals("REJECT", captor.getAllValues().get(0).getFinalResult());
        assertEquals("PASS", captor.getAllValues().get(1).getFinalResult());
        assertEquals("主任", captor.getAllValues().get(0).getSignedBy());
        assertEquals("主任", captor.getAllValues().get(1).getSignedBy());
        assertEquals(null, captor.getAllValues().get(0).getConfirmRemark());
    }

    @Test
    void deleteFinalEvaluationChecksActivityStatus()
    {
        FinalEvaluation existing = finalRow(1L, null);
        existing.setActivityId(1L);
        when(finalEvaluationMapper.selectFinalEvaluationById(1L)).thenReturn(existing);
        when(activityMapper.selectActivityById(1L)).thenReturn(activity("CALCULATED"));
        when(resultAggMapper.countResultAggByActivityId(1L)).thenReturn(1);
        when(finalEvaluationMapper.deleteFinalEvaluationByIds(any())).thenReturn(1);

        assertEquals(1, service.deleteFinalEvaluationByIds(new Long[] { 1L }));
    }

    private Activity activity(String status)
    {
        Activity activity = new Activity();
        activity.setId(1L);
        activity.setStatus(status);
        return activity;
    }

    private ResultAgg result(Long id, Long activityCandidateId, String finalResult)
    {
        ResultAgg row = new ResultAgg();
        row.setId(id);
        row.setActivityId(1L);
        row.setActivityCandidateId(activityCandidateId);
        row.setFinalResult(finalResult);
        return row;
    }

    private FinalEvaluation finalRow(Long id, String signedBy)
    {
        FinalEvaluation row = new FinalEvaluation();
        row.setId(id);
        row.setActivityId(1L);
        row.setSignedBy(signedBy);
        return row;
    }

    private FinalEvaluation finalRow(String result, String status)
    {
        FinalEvaluation row = new FinalEvaluation();
        row.setFinalResult(result);
        row.setConfirmStatus(status);
        return row;
    }
}
