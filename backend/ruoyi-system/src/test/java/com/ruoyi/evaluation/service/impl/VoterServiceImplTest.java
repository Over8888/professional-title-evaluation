package com.ruoyi.evaluation.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.test.util.ReflectionTestUtils;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.evaluation.domain.Activity;
import com.ruoyi.evaluation.domain.ActivityVoter;
import com.ruoyi.evaluation.domain.Voter;
import com.ruoyi.evaluation.mapper.ActivityMapper;
import com.ruoyi.evaluation.mapper.ActivityVoterMapper;
import com.ruoyi.evaluation.mapper.VoterMapper;

class VoterServiceImplTest
{
    private VoterServiceImpl service;

    @Mock
    private VoterMapper voterMapper;

    @Mock
    private ActivityVoterMapper activityVoterMapper;

    @Mock
    private ActivityMapper activityMapper;

    @Mock
    private ExcelImportPreviewService excelImportPreviewService;

    @BeforeEach
    void setUp()
    {
        MockitoAnnotations.openMocks(this);
        service = new VoterServiceImpl();
        ReflectionTestUtils.setField(service, "voterMapper", voterMapper);
        ReflectionTestUtils.setField(service, "activityVoterMapper", activityVoterMapper);
        ReflectionTestUtils.setField(service, "activityMapper", activityMapper);
        ReflectionTestUtils.setField(service, "excelImportPreviewService", excelImportPreviewService);
    }

    @Test
    void selectFromPoolRejectsPublishedActivity()
    {
        when(activityMapper.selectActivityById(1L)).thenReturn(activity("PUBLISHED"));

        ServiceException ex = assertThrows(ServiceException.class,
                () -> service.selectFromPool(1L, Arrays.asList(100L), false, "hr"));

        assertEquals("Activity voters can only be changed before publishing.", ex.getMessage());
        verify(activityVoterMapper, never()).insertActivityVoter(any());
    }

    @Test
    void regenerateVoteTokenRejectsPublishedActivity()
    {
        ActivityVoter row = new ActivityVoter();
        row.setId(10L);
        row.setActivityId(1L);
        when(activityVoterMapper.selectActivityVoterById(10L)).thenReturn(row);
        when(activityMapper.selectActivityById(1L)).thenReturn(activity("VOTING"));

        assertThrows(ServiceException.class, () -> service.regenerateVoteToken(10L, "hr"));

        verify(activityVoterMapper, never()).updateActivityVoter(any());
    }

    @Test
    void regenerateVoteTokenWorksBeforePublish()
    {
        ActivityVoter row = new ActivityVoter();
        row.setId(10L);
        row.setActivityId(1L);
        when(activityVoterMapper.selectActivityVoterById(10L)).thenReturn(row);
        when(activityMapper.selectActivityById(1L)).thenReturn(activity("CONFIGURED"));
        when(activityVoterMapper.updateActivityVoter(any())).thenReturn(1);

        int updated = service.regenerateVoteToken(10L, "hr");

        assertEquals(1, updated);
        ArgumentCaptor<ActivityVoter> captor = ArgumentCaptor.forClass(ActivityVoter.class);
        verify(activityVoterMapper).updateActivityVoter(captor.capture());
        assertEquals(10L, captor.getValue().getId());
        assertNotNull(captor.getValue().getVoteToken());
        assertEquals("hr", captor.getValue().getUpdateBy());
    }

    @Test
    void insertVoterAddsActivitySnapshotWhenActivityIdProvided()
    {
        Voter input = new Voter();
        input.setActivityId(1L);
        input.setName("李四");
        input.setEmployeeId("E002");
        input.setCreateBy("hr");
        Voter pool = new Voter();
        pool.setId(20L);
        pool.setName("李四");
        pool.setEmployeeId("E002");
        pool.setImportSeq(3);
        when(activityMapper.selectActivityById(1L)).thenReturn(activity("CONFIGURED"));
        when(voterMapper.selectVoterByUniqueKey(input)).thenReturn(pool);
        when(activityVoterMapper.insertActivityVoter(any())).thenReturn(1);

        int inserted = service.insertVoter(input);

        assertEquals(1, inserted);
        ArgumentCaptor<ActivityVoter> captor = ArgumentCaptor.forClass(ActivityVoter.class);
        verify(activityVoterMapper).insertActivityVoter(captor.capture());
        assertEquals(1L, captor.getValue().getActivityId());
        assertEquals(20L, captor.getValue().getVoterId());
        assertEquals("PENDING", captor.getValue().getStatus());
    }

    private Activity activity(String status)
    {
        Activity activity = new Activity();
        activity.setId(1L);
        activity.setStatus(status);
        return activity;
    }
}
