package com.ruoyi.evaluation.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.test.util.ReflectionTestUtils;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.evaluation.domain.Activity;
import com.ruoyi.evaluation.domain.Candidate;
import com.ruoyi.evaluation.mapper.ActivityCandidateMapper;
import com.ruoyi.evaluation.mapper.ActivityMapper;
import com.ruoyi.evaluation.mapper.CandidateMapper;

class CandidateServiceImplTest
{
    private CandidateServiceImpl service;

    @Mock
    private CandidateMapper candidateMapper;

    @Mock
    private ActivityCandidateMapper activityCandidateMapper;

    @Mock
    private ActivityMapper activityMapper;

    @Mock
    private ExcelImportPreviewService excelImportPreviewService;

    @BeforeEach
    void setUp()
    {
        MockitoAnnotations.openMocks(this);
        service = new CandidateServiceImpl();
        ReflectionTestUtils.setField(service, "candidateMapper", candidateMapper);
        ReflectionTestUtils.setField(service, "activityCandidateMapper", activityCandidateMapper);
        ReflectionTestUtils.setField(service, "activityMapper", activityMapper);
        ReflectionTestUtils.setField(service, "excelImportPreviewService", excelImportPreviewService);
    }

    @Test
    void selectFromPoolRejectsPublishedActivity()
    {
        when(activityMapper.selectActivityById(1L)).thenReturn(activity("PUBLISHED"));

        ServiceException ex = assertThrows(ServiceException.class,
                () -> service.selectFromPool(1L, Arrays.asList(100L), false, "hr"));

        assertEquals("Activity candidates can only be changed before publishing.", ex.getMessage());
        verify(activityCandidateMapper, never()).insertActivityCandidate(any());
    }

    @Test
    void clearActivityCandidatesRequiresConfigurableActivity()
    {
        when(activityMapper.selectActivityById(1L)).thenReturn(activity("VOTING"));

        assertThrows(ServiceException.class, () -> service.deleteCandidateByActivityId(1L));

        verify(activityCandidateMapper, never()).deleteActivityCandidateByActivityId(1L);
    }

    @Test
    void insertCandidateNormalizesIdCardAndAssignsPoolSequence()
    {
        Candidate candidate = new Candidate();
        candidate.setName("张三");
        candidate.setIdCard(" 11010119900101123x ");
        when(candidateMapper.selectMaxImportSeq()).thenReturn(4);
        when(candidateMapper.insertCandidate(candidate)).thenReturn(1);

        int inserted = service.insertCandidate(candidate);

        assertEquals(1, inserted);
        assertEquals("11010119900101123X", candidate.getIdCard());
        assertEquals(5, candidate.getImportSeq());
    }

    @Test
    void updateCandidateRejectsDuplicatePoolIdCard()
    {
        Candidate existing = new Candidate();
        existing.setId(1L);
        existing.setImportSeq(9);
        Candidate duplicated = new Candidate();
        duplicated.setId(2L);
        duplicated.setIdCard("110101199001011234");
        Candidate update = new Candidate();
        update.setId(1L);
        update.setIdCard("110101199001011234");
        when(candidateMapper.selectCandidateById(1L)).thenReturn(existing);
        when(candidateMapper.selectCandidateByIdCard("110101199001011234")).thenReturn(duplicated);

        assertThrows(ServiceException.class, () -> service.updateCandidate(update));

        verify(candidateMapper, never()).updateCandidate(any());
    }

    private Activity activity(String status)
    {
        Activity activity = new Activity();
        activity.setId(1L);
        activity.setStatus(status);
        return activity;
    }
}
