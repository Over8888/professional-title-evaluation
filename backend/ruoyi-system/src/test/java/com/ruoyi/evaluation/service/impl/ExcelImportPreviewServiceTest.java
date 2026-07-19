package com.ruoyi.evaluation.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.io.ByteArrayOutputStream;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.evaluation.domain.ActivityCandidate;
import com.ruoyi.evaluation.domain.ActivityVoter;
import com.ruoyi.evaluation.domain.ImportPreviewResult;
import com.ruoyi.evaluation.domain.Voter;
import com.ruoyi.evaluation.mapper.ActivityCandidateMapper;
import com.ruoyi.evaluation.mapper.ActivityVoterMapper;
import com.ruoyi.evaluation.mapper.CandidateMapper;
import com.ruoyi.evaluation.mapper.ExportJobMapper;
import com.ruoyi.evaluation.mapper.VoterMapper;

class ExcelImportPreviewServiceTest
{
    private ExcelImportPreviewService service;

    @Mock
    private ExportJobMapper exportJobMapper;

    @Mock
    private CandidateMapper candidateMapper;

    @Mock
    private ActivityCandidateMapper activityCandidateMapper;

    @Mock
    private VoterMapper voterMapper;

    @Mock
    private ActivityVoterMapper activityVoterMapper;

    @BeforeEach
    void setUp()
    {
        MockitoAnnotations.openMocks(this);
        service = new ExcelImportPreviewService();
        ReflectionTestUtils.setField(service, "exportJobMapper", exportJobMapper);
        when(exportJobMapper.insertExportJob(any())).thenReturn(1);
    }

    @Test
    void importCandidatesRecognizesChineseHeadersAndNormalizesIdCard() throws Exception
    {
        when(candidateMapper.selectMaxImportSeq()).thenReturn(9);
        when(candidateMapper.insertCandidate(any())).thenAnswer(invocation -> {
            com.ruoyi.evaluation.domain.Candidate candidate = invocation.getArgument(0);
            candidate.setId(500L);
            return 1;
        });
        when(activityCandidateMapper.selectActivityCandidateByActivityIdAndScope(any())).thenReturn(null);
        when(activityCandidateMapper.insertActivityCandidate(any())).thenReturn(1);

        ImportPreviewResult result = service.importCandidates(1L, workbookFile("candidate.xlsx",
                new String[] { "序号", "姓名", "身份证号", "申报职称", "评审专业", "单位", "岗位" },
                new String[] { "7", "张三", " 11010119900101123x ", "工程师", "船体组", "一公司", "设计师" }),
                "hr", candidateMapper, activityCandidateMapper);

        assertEquals(1, result.getImportedRows());
        ArgumentCaptor<ActivityCandidate> captor = ArgumentCaptor.forClass(ActivityCandidate.class);
        org.mockito.Mockito.verify(activityCandidateMapper).insertActivityCandidate(captor.capture());
        ActivityCandidate row = captor.getValue();
        assertEquals(1L, row.getActivityId());
        assertEquals(7, row.getImportSeq());
        assertEquals("张三", row.getName());
        assertEquals("11010119900101123X", row.getIdCard());
        assertEquals("中级工程师", row.getAppliedLevel());
        assertEquals("船体组", row.getDepartment());
    }

    @Test
    void importCandidatesNormalizesSupportedTitleAliases() throws Exception
    {
        when(candidateMapper.selectMaxImportSeq()).thenReturn(0);
        when(candidateMapper.insertCandidate(any())).thenAnswer(invocation -> {
            com.ruoyi.evaluation.domain.Candidate candidate = invocation.getArgument(0);
            candidate.setId((long) candidate.getImportSeq());
            return 1;
        });
        when(activityCandidateMapper.selectActivityCandidateByActivityIdAndScope(any())).thenReturn(null);
        when(activityCandidateMapper.insertActivityCandidate(any())).thenReturn(1);

        ImportPreviewResult result = service.importCandidates(1L, workbookFile("candidate.xlsx",
                new String[] { "序号", "姓名", "身份证号", "申报职称", "评审专业" },
                new String[] { "1", "张三", "110101199001011231", "高级", "船体组" },
                new String[] { "2", "李四", "110101199001011232", "中级", "船体组" },
                new String[] { "3", "王五", "110101199001011233", "工程师", "船体组" },
                new String[] { "4", "赵六", "110101199001011234", "初级", "船体组" },
                new String[] { "5", "钱七", "110101199001011235", "员级", "船体组" }),
                "hr", candidateMapper, activityCandidateMapper);

        assertEquals(5, result.getImportedRows());
        ArgumentCaptor<ActivityCandidate> captor = ArgumentCaptor.forClass(ActivityCandidate.class);
        org.mockito.Mockito.verify(activityCandidateMapper, org.mockito.Mockito.times(5)).insertActivityCandidate(captor.capture());
        assertEquals("副高级工程师", captor.getAllValues().get(0).getAppliedLevel());
        assertEquals("中级工程师", captor.getAllValues().get(1).getAppliedLevel());
        assertEquals("中级工程师", captor.getAllValues().get(2).getAppliedLevel());
        assertEquals("助理工程师", captor.getAllValues().get(3).getAppliedLevel());
        assertEquals("技术员", captor.getAllValues().get(4).getAppliedLevel());
    }

    @Test
    void importCandidatesSkipsUnsupportedTitle() throws Exception
    {
        when(candidateMapper.selectMaxImportSeq()).thenReturn(0);

        ServiceException ex = assertThrows(ServiceException.class,
                () -> service.importCandidates(1L, workbookFile("candidate.xlsx",
                        new String[] { "序号", "姓名", "身份证号", "申报职称", "评审专业" },
                        new String[] { "1", "张三", "110101199001011231", "研究员", "船体组" }),
                        "hr", candidateMapper, activityCandidateMapper));

        org.junit.jupiter.api.Assertions.assertTrue(ex.getMessage().contains("Candidate applied title/level is empty; skipped"));
    }

    @Test
    void importCandidatesRejectsNonExcelFile()
    {
        MockMultipartFile file = new MockMultipartFile("file", "candidate.txt", "text/plain", "bad".getBytes());

        assertThrows(ServiceException.class,
                () -> service.importCandidates(1L, file, "hr", candidateMapper, activityCandidateMapper));
    }

    @Test
    void importCandidatesReportsDuplicateInSameUploadAsPartialSuccess() throws Exception
    {
        when(candidateMapper.selectMaxImportSeq()).thenReturn(0);
        when(candidateMapper.insertCandidate(any())).thenReturn(1);
        when(activityCandidateMapper.insertActivityCandidate(any())).thenReturn(1);

        ImportPreviewResult result = service.importCandidates(1L, workbookFile("candidate.xlsx",
                new String[] { "序号", "姓名", "身份证号", "申报职称", "评审专业" },
                new String[] { "1", "张三", "110101199001011234", "工程师", "船体组" },
                new String[] { "2", "李四", "110101199001011234", "工程师", "船体组" }),
                "hr", candidateMapper, activityCandidateMapper);

        assertEquals(1, result.getImportedRows());
        assertEquals(1, result.getErrors().size());
        assertEquals("Duplicate candidate in the same group and applied level in uploaded file; skipped",
                result.getErrors().get(0).getReason());
    }

    @Test
    void previewParsesGenericWorkbook() throws Exception
    {
        ImportPreviewResult result = service.preview(null, workbookFile("generic.xlsx",
                new String[] { "姓名", "工号", "部门" },
                new String[] { "评委一", "E001", "船体组" }),
                "VOTER_IMPORT", "hr");

        assertEquals(1, result.getSheetCount());
        assertEquals(1, result.getTotalRows());
        assertEquals(1, result.getPreviewRows());
        assertEquals("姓名", result.getSheets().get(0).getHeaders().get(0));
    }

    @Test
    void importVotersCreatesPoolAndActivitySnapshot() throws Exception
    {
        when(voterMapper.selectMaxImportSeq()).thenReturn(0);
        when(voterMapper.insertVoter(any())).thenAnswer(invocation -> {
            Voter voter = invocation.getArgument(0);
            voter.setId(300L);
            return 1;
        });
        when(activityVoterMapper.insertActivityVoter(any())).thenReturn(1);

        ImportPreviewResult result = service.importVoters(1L, workbookFile("voter.xlsx",
                new String[] { "姓名", "工号", "部门" },
                new String[] { "王五", "E005", "船体组" }),
                "hr", voterMapper, activityVoterMapper);

        assertEquals(1, result.getImportedRows());
        ArgumentCaptor<ActivityVoter> captor = ArgumentCaptor.forClass(ActivityVoter.class);
        org.mockito.Mockito.verify(activityVoterMapper).insertActivityVoter(captor.capture());
        assertEquals(1L, captor.getValue().getActivityId());
        assertEquals("王五", captor.getValue().getName());
        assertEquals("E005", captor.getValue().getEmployeeId());
        assertEquals("PENDING", captor.getValue().getStatus());
    }

    @Test
    void importVotersReportsDuplicateRowsAsPartialSuccess() throws Exception
    {
        when(voterMapper.selectMaxImportSeq()).thenReturn(0);
        when(voterMapper.insertVoter(any())).thenReturn(1);
        when(activityVoterMapper.insertActivityVoter(any())).thenReturn(1);

        ImportPreviewResult result = service.importVoters(null, workbookFile("voter.xlsx",
                new String[] { "姓名", "工号", "部门" },
                new String[] { "王五", "E005", "船体组" },
                new String[] { "赵六", "E005", "船体组" }),
                "hr", voterMapper, activityVoterMapper);

        assertEquals(1, result.getImportedRows());
        assertEquals(1, result.getErrors().size());
        assertEquals("Duplicate voter in uploaded file; skipped", result.getErrors().get(0).getReason());
    }

    @Test
    void previewSortedCandidateActivityWorkbook() throws Exception
    {
        ImportPreviewResult result = service.preview(1L, workbookFile("船体组排序表.xlsx",
                new String[] { "排序", "姓名", "身份证号", "申报职称", "工作单位", "单位" },
                new String[] { "1", "张三", "110101199001011234", "工程师", "一公司", "设计所" }),
                "CANDIDATE_IMPORT", "hr");

        assertEquals(1, result.getSheetCount());
        assertEquals(1, result.getPreviewRows());
        assertEquals("船体组", result.getSheets().get(0).getRows().get(0).get("_group"));
    }

    @Test
    void importSortedCandidateWorkbookRequiresRank() throws Exception
    {
        when(candidateMapper.selectMaxImportSeq()).thenReturn(0);

        ServiceException ex = assertThrows(ServiceException.class,
                () -> service.importCandidates(1L, workbookFile("船体组排序表.xlsx",
                        new String[] { "排序", "姓名", "身份证号", "申报职称", "工作单位", "单位" },
                        new String[] { "", "张三", "110101199001011234", "工程师", "一公司", "设计所" }),
                        "hr", candidateMapper, activityCandidateMapper));

        org.junit.jupiter.api.Assertions.assertTrue(ex.getMessage().contains("Candidate rank is empty or invalid; skipped"));
    }

    @Test
    void importCandidatePoolAssignsNextSequenceAndSkipsExisting() throws Exception
    {
        when(candidateMapper.selectMaxImportSeq()).thenReturn(4);
        when(candidateMapper.selectCandidateByIdCard("110101199001011234")).thenReturn(null);
        when(candidateMapper.selectCandidateByIdCard("110101199001011235")).thenReturn(new com.ruoyi.evaluation.domain.Candidate());
        when(candidateMapper.insertCandidate(any())).thenReturn(1);

        ImportPreviewResult result = service.importCandidates(null, workbookFile("pool.xlsx",
                new String[] { "姓名", "身份证号", "申报职称", "评审专业" },
                new String[] { "张三", "110101199001011234", "工程师", "船体组" },
                new String[] { "李四", "110101199001011235", "工程师", "船体组" }),
                "hr", candidateMapper, activityCandidateMapper);

        assertEquals(1, result.getImportedRows());
        assertEquals(1, result.getErrors().size());
        ArgumentCaptor<com.ruoyi.evaluation.domain.Candidate> captor =
                ArgumentCaptor.forClass(com.ruoyi.evaluation.domain.Candidate.class);
        org.mockito.Mockito.verify(candidateMapper).insertCandidate(captor.capture());
        assertEquals(5, captor.getValue().getImportSeq());
    }

    @Test
    void previewRejectsEmptyFile()
    {
        MockMultipartFile empty = new MockMultipartFile("file", "empty.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", new byte[0]);

        ServiceException ex = assertThrows(ServiceException.class,
                () -> service.preview(null, empty, "VOTER_IMPORT", "hr"));

        assertEquals("Uploaded file is empty", ex.getMessage());
    }

    @Test
    void previewReportsNoHeaderRow() throws Exception
    {
        ImportPreviewResult result = service.preview(null, workbookFile("blank.xlsx", new String[] { "" }),
                "VOTER_IMPORT", "hr");

        assertEquals(1, result.getErrors().size());
        assertEquals("No non-empty header row found", result.getErrors().get(0).getReason());
    }

    private MockMultipartFile workbookFile(String filename, String[] headers, String[]... rows) throws Exception
    {
        try (XSSFWorkbook workbook = new XSSFWorkbook(); ByteArrayOutputStream output = new ByteArrayOutputStream())
        {
            org.apache.poi.ss.usermodel.Sheet sheet = workbook.createSheet("候选人");
            org.apache.poi.ss.usermodel.Row header = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++)
            {
                header.createCell(i).setCellValue(headers[i]);
            }
            for (int r = 0; r < rows.length; r++)
            {
                org.apache.poi.ss.usermodel.Row row = sheet.createRow(r + 1);
                for (int c = 0; c < rows[r].length; c++)
                {
                    row.createCell(c).setCellValue(rows[r][c]);
                }
            }
            workbook.write(output);
            return new MockMultipartFile("file", filename,
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", output.toByteArray());
        }
    }
}
