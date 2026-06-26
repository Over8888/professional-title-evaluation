package com.ruoyi.evaluation.service.impl;

import java.io.InputStream;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.apache.commons.io.FilenameUtils;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.DateUtils;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.evaluation.domain.ActivityCandidate;
import com.ruoyi.evaluation.domain.ActivityVoter;
import com.ruoyi.evaluation.domain.Candidate;
import com.ruoyi.evaluation.domain.ExportJob;
import com.ruoyi.evaluation.domain.ImportPreviewResult;
import com.ruoyi.evaluation.domain.ImportPreviewResult.ImportError;
import com.ruoyi.evaluation.domain.ImportPreviewResult.SheetPreview;
import com.ruoyi.evaluation.domain.Voter;
import com.ruoyi.evaluation.mapper.ActivityCandidateMapper;
import com.ruoyi.evaluation.mapper.ActivityVoterMapper;
import com.ruoyi.evaluation.mapper.CandidateMapper;
import com.ruoyi.evaluation.mapper.ExportJobMapper;
import com.ruoyi.evaluation.mapper.VoterMapper;

@Service
public class ExcelImportPreviewService
{
    private static final int MAX_PREVIEW_ROWS_PER_SHEET = 20;
    private static final int MAX_SCAN_ROWS_PER_SHEET = 2000;
    private static final int MAX_SCAN_COLUMNS = 80;
    private static final int MAX_JOB_ERROR_MESSAGE_LENGTH = 1000;
    private static final String IMPORT_TYPE_CANDIDATE = "CANDIDATE_IMPORT";
    private static final String IMPORT_TYPE_VOTER = "VOTER_IMPORT";

    @Autowired
    private ExportJobMapper exportJobMapper;

    public ImportPreviewResult preview(Long activityId, MultipartFile file, String importType, String username) throws Exception
    {
        if (activityId != null && IMPORT_TYPE_CANDIDATE.equals(importType))
        {
            return previewSortedCandidates(activityId, file, importType, username);
        }
        validateExcelFile(file);
        ImportPreviewResult result = new ImportPreviewResult();
        result.setActivityId(activityId);
        result.setImportType(importType);
        result.setFileName(file.getOriginalFilename());
        try (InputStream inputStream = file.getInputStream(); Workbook workbook = WorkbookFactory.create(inputStream))
        {
            result.setSheetCount(workbook.getNumberOfSheets());
            for (int i = 0; i < workbook.getNumberOfSheets(); i++)
            {
                parseSheet(workbook.getSheetAt(i), result);
            }
            saveJob(activityId, importType, file.getOriginalFilename(), username, result.getErrors().isEmpty() ? "SUCCESS" : "PARTIAL_SUCCESS", buildErrorMessage(result));
            return result;
        }
        catch (Exception e)
        {
            saveJob(activityId, importType, file.getOriginalFilename(), username, "FAILED", e.getMessage());
            throw e;
        }
    }

    public ImportPreviewResult importCandidates(Long activityId, MultipartFile file, String username, CandidateMapper candidateMapper, ActivityCandidateMapper activityCandidateMapper) throws Exception
    {
        if (activityId != null)
        {
            return importSortedCandidates(activityId, file, username, candidateMapper, activityCandidateMapper);
        }
        Set<String> seenIdCards = new HashSet<>();
        int[] nextPoolImportSeq = new int[] { nextPoolImportSeq(candidateMapper) };
        return importRows(activityId, file, IMPORT_TYPE_CANDIDATE, username, (headers, values, rowNo, result) -> {
            Candidate candidate = new Candidate();
            candidate.setName(valueOf(values, "\u59d3\u540d", "\u540d\u79f0", "name"));
            candidate.setGender(valueOf(values, "\u6027\u522b", "gender"));
            candidate.setBirthDate(parseDate(valueOf(values, "\u51fa\u751f\u5e74\u6708", "\u51fa\u751f\u65e5\u671f", "birthDate")));
            candidate.setEducation(valueOf(values, "\u5b66\u5386", "\u6700\u9ad8\u5b66\u5386", "education"));
            candidate.setCompany(valueOf(values, "\u5355\u4f4d", "\u5de5\u4f5c\u5355\u4f4d", "\u6240\u5728\u5355\u4f4d", "company"));
            candidate.setFirstLevelDepartment(valueOf(values, "\u4e00\u7ea7\u90e8\u95e8", "\u4e00\u7ea7\u7ec4\u7ec7", "firstLevelDepartment"));
            candidate.setDepartment(valueOf(values, "\u8bc4\u5ba1\u4e13\u4e1a", "\u7ec4\u522b", "\u7533\u62a5\u7ec4\u522b", "\u4e8c\u7ea7\u90e8\u95e8", "\u4e8c\u7ea7\u7ec4\u7ec7", "department"));
            candidate.setPosition(valueOf(values, "\u5c97\u4f4d", "\u90e8\u95e8", "\u804c\u52a1", "\u73b0\u5c97\u4f4d", "position"));
            candidate.setIdCard(normalizeIdCard(valueOf(values, "\u8eab\u4efd\u8bc1\u53f7", "\u8eab\u4efd\u8bc1\u53f7\u7801", "\u8bc1\u4ef6\u53f7\u7801", "idCard")));
            candidate.setCurrentLevel(valueOf(values, "\u5f53\u524d\u7b49\u7ea7", "\u73b0\u7b49\u7ea7", "\u73b0\u804c\u79f0", "currentLevel"));
            candidate.setAppliedLevel(valueOf(values, "\u7533\u62a5\u7b49\u7ea7", "\u7533\u62a5\u804c\u79f0", "\u62a5\u540d\u7b49\u7ea7", "\u7b49\u7ea7", "\u804c\u79f0", "appliedLevel", "level"));
            candidate.setThirdLevelDepartment(buildThirdLevelDepartment(candidate.getDepartment(), candidate.getCurrentLevel()));
            candidate.setLastYearAssessment(valueOf(values, "\u4e0a\u5e74\u5ea6\u8003\u6838\u7ed3\u679c", "lastYearAssessment"));
            candidate.setEvaluationScore(valueOf(values, "\u8bc4\u4ef7\u7ed3\u679c", "\u8bc4\u4ef7\u5206", "\u8bc4\u5206", "\u7efc\u5408\u5f97\u5206", "evaluationScore"));
            candidate.setFixedType(null);
            candidate.setCreateBy(username);
            if (StringUtils.isEmpty(candidate.getName()))
            {
                result.getErrors().add(new ImportError(null, rowNo, "name", "Candidate name is empty; skipped"));
                return false;
            }
            if (StringUtils.isEmpty(candidate.getIdCard()))
            {
                result.getErrors().add(new ImportError(null, rowNo, "idCard", "ID card is empty; skipped"));
                return false;
            }
            if (StringUtils.isEmpty(candidate.getDepartment()))
            {
                result.getErrors().add(new ImportError(null, rowNo, "department", "Candidate department/group is empty; skipped"));
                return false;
            }
            if (StringUtils.isEmpty(candidate.getAppliedLevel()))
            {
                result.getErrors().add(new ImportError(null, rowNo, "appliedLevel", "Candidate applied level is empty; skipped"));
                return false;
            }
            if (!seenIdCards.add(candidate.getIdCard()))
            {
                result.getErrors().add(new ImportError(null, rowNo, "idCard", "Duplicate ID card in uploaded file; skipped"));
                return false;
            }
            Candidate duplicated = new Candidate();
            duplicated.setIdCard(candidate.getIdCard());
            if (activityId == null && candidateMapper.selectCandidateByIdCard(candidate.getIdCard()) != null)
            {
                result.getErrors().add(new ImportError(null, rowNo, "idCard", "ID card already exists in candidate pool; skipped"));
                return false;
            }
            if (activityId == null)
            {
                assignPoolSequence(candidate, nextPoolImportSeq);
                candidateMapper.insertCandidate(candidate);
            }
            else
            {
                Candidate poolCandidate = candidateMapper.selectCandidateByIdCard(candidate.getIdCard());
                if (poolCandidate == null)
                {
                    assignPoolSequence(candidate, nextPoolImportSeq);
                    candidateMapper.insertCandidate(candidate);
                    poolCandidate = candidate;
                }
                ActivityCandidate activityCandidate = toActivityCandidate(activityId, poolCandidate, candidate, "EXCEL", username);
                if (activityCandidateMapper.selectActivityCandidateByActivityIdAndIdCard(activityCandidate) != null)
                {
                    result.getErrors().add(new ImportError(null, rowNo, "idCard", "ID card already exists in this activity; skipped"));
                    return false;
                }
                activityCandidateMapper.insertActivityCandidate(activityCandidate);
            }
            return true;
        });
    }

    private ImportPreviewResult importSortedCandidates(Long activityId, MultipartFile file, String username,
            CandidateMapper candidateMapper, ActivityCandidateMapper activityCandidateMapper) throws Exception
    {
        validateExcelFile(file);
        ImportPreviewResult result = new ImportPreviewResult();
        result.setActivityId(activityId);
        result.setImportType(IMPORT_TYPE_CANDIDATE);
        result.setFileName(file.getOriginalFilename());
        Set<String> seenCandidateKeys = new HashSet<>();
        int[] nextPoolImportSeq = new int[] { nextPoolImportSeq(candidateMapper) };
        int imported = 0;
        try (InputStream inputStream = file.getInputStream(); Workbook workbook = WorkbookFactory.create(inputStream))
        {
            result.setSheetCount(workbook.getNumberOfSheets());
            for (int i = 0; i < workbook.getNumberOfSheets(); i++)
            {
                Sheet sheet = workbook.getSheetAt(i);
                SheetPreview preview = new SheetPreview();
                preview.setSheetName(sheet.getSheetName());
                result.getSheets().add(preview);

                int headerRowIndex = findSortedCandidateHeaderRow(sheet);
                if (headerRowIndex < 0)
                {
                    continue;
                }
                String title = sheetTitle(sheet, headerRowIndex);
                if (!isSortedCandidateSheet(sheet, title))
                {
                    continue;
                }
                String groupName = resolveGroupName(file.getOriginalFilename(), title, sheet.getSheetName());
                if (StringUtils.isEmpty(groupName))
                {
                    result.getErrors().add(new ImportError(sheet.getSheetName(), headerRowIndex + 1, "department", "Cannot identify candidate group from file name or sheet title; skipped"));
                    continue;
                }

                List<String> headers = readHeaders(sheet.getRow(headerRowIndex));
                preview.setHeaderRowNo(headerRowIndex + 1);
                preview.setHeaders(headers);
                if (headers.isEmpty())
                {
                    result.getErrors().add(new ImportError(sheet.getSheetName(), headerRowIndex + 1, null, "Header row is empty"));
                    continue;
                }

                int lastRow = Math.min(sheet.getLastRowNum(), headerRowIndex + MAX_SCAN_ROWS_PER_SHEET);
                for (int rowIndex = headerRowIndex + 1; rowIndex <= lastRow; rowIndex++)
                {
                    Row row = sheet.getRow(rowIndex);
                    if (isRowEmpty(row, headers.size()))
                    {
                        continue;
                    }
                    result.setTotalRows(result.getTotalRows() + 1);
                    Map<String, String> values = rowValues(row, headers);
                    if (preview.getRows().size() < MAX_PREVIEW_ROWS_PER_SHEET)
                    {
                        Map<String, String> previewValues = new LinkedHashMap<>(values);
                        previewValues.put("_rowNo", String.valueOf(rowIndex + 1));
                        previewValues.put("_group", groupName);
                        preview.getRows().add(previewValues);
                        result.setPreviewRows(result.getPreviewRows() + 1);
                    }
                    imported += importSortedCandidateRow(activityId, groupName, values, rowIndex + 1, result,
                            seenCandidateKeys, nextPoolImportSeq, candidateMapper, activityCandidateMapper, username) ? 1 : 0;
                }
                if (sheet.getLastRowNum() > lastRow)
                {
                    result.getErrors().add(new ImportError(sheet.getSheetName(), lastRow + 1, null, "Rows after scan limit are not imported"));
                }
            }
            saveJob(activityId, IMPORT_TYPE_CANDIDATE, file.getOriginalFilename(), username,
                    result.getErrors().isEmpty() ? "SUCCESS" : "PARTIAL_SUCCESS", "imported=" + imported + errorSuffix(result));
            return result;
        }
        catch (Exception e)
        {
            saveJob(activityId, IMPORT_TYPE_CANDIDATE, file.getOriginalFilename(), username, "FAILED", e.getMessage());
            throw e;
        }
    }

    private ImportPreviewResult previewSortedCandidates(Long activityId, MultipartFile file, String importType,
            String username) throws Exception
    {
        validateExcelFile(file);
        ImportPreviewResult result = new ImportPreviewResult();
        result.setActivityId(activityId);
        result.setImportType(importType);
        result.setFileName(file.getOriginalFilename());
        try (InputStream inputStream = file.getInputStream(); Workbook workbook = WorkbookFactory.create(inputStream))
        {
            result.setSheetCount(workbook.getNumberOfSheets());
            for (int i = 0; i < workbook.getNumberOfSheets(); i++)
            {
                parseSortedCandidateSheet(workbook.getSheetAt(i), file.getOriginalFilename(), result);
            }
            saveJob(activityId, importType, file.getOriginalFilename(), username,
                    result.getErrors().isEmpty() ? "SUCCESS" : "PARTIAL_SUCCESS", buildErrorMessage(result));
            return result;
        }
        catch (Exception e)
        {
            saveJob(activityId, importType, file.getOriginalFilename(), username, "FAILED", e.getMessage());
            throw e;
        }
    }

    private boolean importSortedCandidateRow(Long activityId, String groupName, Map<String, String> values, int rowNo,
            ImportPreviewResult result, Set<String> seenCandidateKeys, int[] nextPoolImportSeq,
            CandidateMapper candidateMapper, ActivityCandidateMapper activityCandidateMapper, String username)
    {
        Candidate candidate = new Candidate();
        candidate.setImportSeq(parseInteger(valueOf(values, "排序", "序号", "排名", "importSeq")));
        candidate.setCompany(valueOf(values, "单位", "工作单位", "所在单位", "company"));
        candidate.setName(valueOf(values, "姓名", "姓 名", "名称", "name"));
        candidate.setIdCard(normalizeIdCard(valueOf(values, "身份证号", "身份证号码", "证件号码", "idCard")));
        candidate.setAppliedLevel(valueOf(values, "申报职称", "申报等级", "报名等级", "职称", "等级", "appliedLevel", "level"));
        candidate.setThirdLevelDepartment(valueOf(values, "评审专业", "专业", "专业组别", "thirdLevelDepartment"));
        candidate.setDepartment(groupName);
        candidate.setEvaluationScore(valueOf(values, "评价结果", "评价分", "评分", "综合得分", "evaluationScore"));
        candidate.setPosition(valueOf(values, "现岗位", "岗位", "职务", "position"));
        candidate.setCreateBy(username);
        candidate.setFixedType(null);

        if (candidate.getImportSeq() == null)
        {
            result.getErrors().add(new ImportError(null, rowNo, "importSeq", "Candidate rank is empty or invalid; skipped"));
            return false;
        }
        if (StringUtils.isEmpty(candidate.getName()))
        {
            result.getErrors().add(new ImportError(null, rowNo, "name", "Candidate name is empty; skipped"));
            return false;
        }
        if (StringUtils.isEmpty(candidate.getIdCard()))
        {
            result.getErrors().add(new ImportError(null, rowNo, "idCard", "ID card is empty; skipped"));
            return false;
        }
        if (StringUtils.isEmpty(candidate.getAppliedLevel()))
        {
            result.getErrors().add(new ImportError(null, rowNo, "appliedLevel", "Candidate applied title/level is empty; skipped"));
            return false;
        }
        if (StringUtils.isEmpty(candidate.getDepartment()))
        {
            result.getErrors().add(new ImportError(null, rowNo, "department", "Candidate group is empty; skipped"));
            return false;
        }
        String candidateKey = activityCandidateScopeKey(candidate);
        if (!seenCandidateKeys.add(candidateKey))
        {
            result.getErrors().add(new ImportError(null, rowNo, "idCard", "Duplicate candidate in the same group and applied level in uploaded file; skipped"));
            return false;
        }

        Integer activityImportSeq = candidate.getImportSeq();
        Candidate poolCandidate = candidateMapper.selectCandidateByIdCard(candidate.getIdCard());
        if (poolCandidate == null)
        {
            assignPoolSequence(candidate, nextPoolImportSeq);
            candidateMapper.insertCandidate(candidate);
            poolCandidate = candidate;
        }
        candidate.setImportSeq(activityImportSeq);
        ActivityCandidate activityCandidate = toActivityCandidate(activityId, poolCandidate, candidate, "SORTED_EXCEL", username);
        if (activityCandidateMapper.selectActivityCandidateByActivityIdAndScope(activityCandidate) != null)
        {
            result.getErrors().add(new ImportError(null, rowNo, "idCard", "Candidate already exists in this activity group and applied level; skipped"));
            return false;
        }
        activityCandidateMapper.insertActivityCandidate(activityCandidate);
        return true;
    }

    private String activityCandidateScopeKey(Candidate candidate)
    {
        return StringUtils.defaultString(candidate.getDepartment()) + "|"
                + StringUtils.defaultString(candidate.getAppliedLevel()) + "|"
                + StringUtils.defaultString(candidate.getIdCard());
    }

    private ActivityCandidate toActivityCandidate(Long activityId, Candidate poolCandidate, Candidate importedCandidate, String sourceType, String username)
    {
        ActivityCandidate activityCandidate = new ActivityCandidate();
        activityCandidate.setActivityId(activityId);
        activityCandidate.setCandidateId(poolCandidate == null ? null : poolCandidate.getId());
        activityCandidate.setSourceType(sourceType);
        activityCandidate.setImportSeq(importedCandidate.getImportSeq());
        activityCandidate.setName(importedCandidate.getName());
        activityCandidate.setGender(importedCandidate.getGender());
        activityCandidate.setBirthDate(importedCandidate.getBirthDate());
        activityCandidate.setEducation(importedCandidate.getEducation());
        activityCandidate.setCompany(importedCandidate.getCompany());
        activityCandidate.setFirstLevelDepartment(importedCandidate.getFirstLevelDepartment());
        activityCandidate.setDepartment(importedCandidate.getDepartment());
        activityCandidate.setThirdLevelDepartment(importedCandidate.getThirdLevelDepartment());
        activityCandidate.setPosition(importedCandidate.getPosition());
        activityCandidate.setIdCard(importedCandidate.getIdCard());
        activityCandidate.setCurrentLevel(importedCandidate.getCurrentLevel());
        activityCandidate.setAppliedLevel(importedCandidate.getAppliedLevel());
        activityCandidate.setFixedType(null);
        activityCandidate.setLastYearAssessment(importedCandidate.getLastYearAssessment());
        activityCandidate.setEvaluationScore(importedCandidate.getEvaluationScore());
        activityCandidate.setCreateBy(username);
        return activityCandidate;
    }

    private int nextPoolImportSeq(CandidateMapper candidateMapper)
    {
        Integer maxImportSeq = candidateMapper.selectMaxImportSeq();
        return (maxImportSeq == null ? 0 : maxImportSeq) + 1;
    }

    private void assignPoolSequence(Candidate candidate, int[] nextPoolImportSeq)
    {
        int importSeq = nextPoolImportSeq[0]++;
        candidate.setImportSeq(importSeq);
    }

    private String buildThirdLevelDepartment(String department, String currentLevel)
    {
        String dept = StringUtils.defaultString(department);
        String level = StringUtils.defaultString(currentLevel);
        if (StringUtils.isEmpty(dept) && StringUtils.isEmpty(level))
        {
            return null;
        }
        return dept + "(" + level + ")";
    }

    private boolean isSortedCandidateSheet(Sheet sheet, String title)
    {
        String sheetName = StringUtils.defaultString(sheet.getSheetName());
        String titleText = StringUtils.defaultString(title);
        return sheetName.contains("排序") || titleText.contains("统分表");
    }

    private String sheetTitle(Sheet sheet, int headerRowIndex)
    {
        for (int rowIndex = Math.max(sheet.getFirstRowNum(), headerRowIndex - 3); rowIndex < headerRowIndex; rowIndex++)
        {
            Row row = sheet.getRow(rowIndex);
            if (row == null)
            {
                continue;
            }
            int lastCell = Math.min(Math.max(row.getLastCellNum(), 0), MAX_SCAN_COLUMNS);
            for (int cellIndex = 0; cellIndex < lastCell; cellIndex++)
            {
                String text = getCellText(row.getCell(cellIndex));
                if (StringUtils.isNotEmpty(text))
                {
                    return text;
                }
            }
        }
        return sheet.getSheetName();
    }

    private String resolveGroupName(String fileName, String title, String sheetName)
    {
        String text = StringUtils.defaultString(fileName) + " " + StringUtils.defaultString(title) + " " + StringUtils.defaultString(sheetName);
        if (text.contains("综合组") || text.contains("综合"))
        {
            return "综合组";
        }
        if (text.contains("船体组") || text.contains("船体"))
        {
            return "船体组";
        }
        if (text.contains("船机组") || text.contains("船机"))
        {
            return "船机组";
        }
        if (text.contains("船电组") || text.contains("船电"))
        {
            return "船电组";
        }
        return null;
    }

    private void parseSortedCandidateSheet(Sheet sheet, String fileName, ImportPreviewResult result)
    {
        SheetPreview preview = new SheetPreview();
        preview.setSheetName(sheet.getSheetName());
        result.getSheets().add(preview);

        int headerRowIndex = findSortedCandidateHeaderRow(sheet);
        if (headerRowIndex < 0)
        {
            return;
        }
        String title = sheetTitle(sheet, headerRowIndex);
        if (!isSortedCandidateSheet(sheet, title))
        {
            return;
        }
        String groupName = resolveGroupName(fileName, title, sheet.getSheetName());
        if (StringUtils.isEmpty(groupName))
        {
            result.getErrors().add(new ImportError(sheet.getSheetName(), headerRowIndex + 1, "department",
                    "Cannot identify candidate group from file name or sheet title; skipped"));
            return;
        }

        List<String> headers = readHeaders(sheet.getRow(headerRowIndex));
        preview.setHeaderRowNo(headerRowIndex + 1);
        preview.setHeaders(headers);
        int lastRow = Math.min(sheet.getLastRowNum(), headerRowIndex + MAX_SCAN_ROWS_PER_SHEET);
        for (int rowIndex = headerRowIndex + 1; rowIndex <= lastRow; rowIndex++)
        {
            Row row = sheet.getRow(rowIndex);
            if (isRowEmpty(row, headers.size()))
            {
                continue;
            }
            result.setTotalRows(result.getTotalRows() + 1);
            if (preview.getRows().size() >= MAX_PREVIEW_ROWS_PER_SHEET)
            {
                continue;
            }
            Map<String, String> values = rowValues(row, headers);
            values.put("_rowNo", String.valueOf(rowIndex + 1));
            values.put("_group", groupName);
            preview.getRows().add(values);
            result.setPreviewRows(result.getPreviewRows() + 1);
        }
    }

    public ImportPreviewResult importVoters(Long activityId, MultipartFile file, String username, VoterMapper voterMapper, ActivityVoterMapper activityVoterMapper) throws Exception
    {
        Set<String> seenKeys = new HashSet<>();
        int[] nextPoolImportSeq = new int[] { nextVoterPoolImportSeq(voterMapper) };
        return importRows(activityId, file, IMPORT_TYPE_VOTER, username, (headers, values, rowNo, result) -> {
            Voter voter = new Voter();
            voter.setName(valueOf(values, "\u59d3\u540d", "\u8bc4\u59d4\u59d3\u540d", "\u540d\u79f0", "name"));
            voter.setEmployeeId(valueOf(values, "\u5de5\u53f7", "\u5458\u5de5\u7f16\u53f7", "\u804c\u5de5\u53f7", "employeeId"));
            voter.setDepartment(valueOf(values, "\u90e8\u95e8", "\u5355\u4f4d", "\u6240\u5728\u90e8\u95e8", "department"));
            voter.setRemark(valueOf(values, "\u5907\u6ce8", "remark"));
            voter.setCreateBy(username);
            if (StringUtils.isEmpty(voter.getName()))
            {
                result.getErrors().add(new ImportError(null, rowNo, "name", "Voter name is empty; skipped"));
                return false;
            }
            String dedupeKey = StringUtils.isNotEmpty(voter.getEmployeeId()) ? voter.getEmployeeId() : voter.getName() + "|" + StringUtils.defaultString(voter.getDepartment());
            if (!seenKeys.add(dedupeKey))
            {
                result.getErrors().add(new ImportError(null, rowNo, "employeeId", "Duplicate voter in uploaded file; skipped"));
                return false;
            }
            if (activityId == null)
            {
                if (voterMapper.selectVoterByUniqueKey(voter) != null)
                {
                    result.getErrors().add(new ImportError(null, rowNo, "employeeId", "Voter already exists in voter pool; skipped"));
                    return false;
                }
                assignVoterPoolSequence(voter, nextPoolImportSeq);
                voterMapper.insertVoter(voter);
                return true;
            }
            Voter poolVoter = voterMapper.selectVoterByUniqueKey(voter);
            if (poolVoter == null)
            {
                assignVoterPoolSequence(voter, nextPoolImportSeq);
                voterMapper.insertVoter(voter);
                poolVoter = voter;
            }
            ActivityVoter activityVoter = toActivityVoter(activityId, poolVoter, voter, "EXCEL", username);
            if (activityVoterMapper.selectActivityVoterByUniqueKey(activityVoter) != null)
            {
                result.getErrors().add(new ImportError(null, rowNo, "employeeId", "Voter already exists in this activity; skipped"));
                return false;
            }
            activityVoterMapper.insertActivityVoter(activityVoter);
            return true;
        });
    }
    private ActivityVoter toActivityVoter(Long activityId, Voter poolVoter, Voter importedVoter, String sourceType, String username)
    {
        ActivityVoter activityVoter = new ActivityVoter();
        activityVoter.setActivityId(activityId);
        activityVoter.setVoterId(poolVoter == null ? null : poolVoter.getId());
        activityVoter.setImportSeq(poolVoter == null ? importedVoter.getImportSeq() : poolVoter.getImportSeq());
        activityVoter.setSourceType(sourceType);
        activityVoter.setName(importedVoter.getName());
        activityVoter.setEmployeeId(importedVoter.getEmployeeId());
        activityVoter.setDepartment(importedVoter.getDepartment());
        activityVoter.setStatus("PENDING");
        activityVoter.setVoteToken(UUID.randomUUID().toString().replace("-", ""));
        activityVoter.setRemark(importedVoter.getRemark());
        activityVoter.setCreateBy(username);
        return activityVoter;
    }

    private int nextVoterPoolImportSeq(VoterMapper voterMapper)
    {
        Integer maxImportSeq = voterMapper.selectMaxImportSeq();
        return (maxImportSeq == null ? 0 : maxImportSeq) + 1;
    }

    private void assignVoterPoolSequence(Voter voter, int[] nextPoolImportSeq)
    {
        if (voter.getImportSeq() == null)
        {
            voter.setImportSeq(nextPoolImportSeq[0]++);
        }
        else if (voter.getImportSeq() >= nextPoolImportSeq[0])
        {
            nextPoolImportSeq[0] = voter.getImportSeq() + 1;
        }
    }
    private ImportPreviewResult importRows(Long activityId, MultipartFile file, String importType, String username, RowImporter importer) throws Exception
    {
        validateExcelFile(file);
        ImportPreviewResult result = new ImportPreviewResult();
        result.setActivityId(activityId);
        result.setImportType(importType);
        result.setFileName(file.getOriginalFilename());
        int imported = 0;
        try (InputStream inputStream = file.getInputStream(); Workbook workbook = WorkbookFactory.create(inputStream))
        {
            result.setSheetCount(workbook.getNumberOfSheets());
            for (int i = 0; i < workbook.getNumberOfSheets(); i++)
            {
                Sheet sheet = workbook.getSheetAt(i);
                SheetPreview preview = new SheetPreview();
                preview.setSheetName(sheet.getSheetName());
                result.getSheets().add(preview);
                int headerRowIndex = findHeaderRow(sheet);
                if (headerRowIndex < 0)
                {
                    result.getErrors().add(new ImportError(sheet.getSheetName(), null, null, "No non-empty header row found"));
                    continue;
                }
                preview.setHeaderRowNo(headerRowIndex + 1);
                List<String> headers = readHeaders(sheet.getRow(headerRowIndex));
                preview.setHeaders(headers);
                int lastRow = Math.min(sheet.getLastRowNum(), headerRowIndex + MAX_SCAN_ROWS_PER_SHEET);
                for (int rowIndex = headerRowIndex + 1; rowIndex <= lastRow; rowIndex++)
                {
                    Row row = sheet.getRow(rowIndex);
                    if (isRowEmpty(row, headers.size()))
                    {
                        continue;
                    }
                    result.setTotalRows(result.getTotalRows() + 1);
                    Map<String, String> values = rowValues(row, headers);
                    if (preview.getRows().size() < MAX_PREVIEW_ROWS_PER_SHEET)
                    {
                        Map<String, String> previewValues = new LinkedHashMap<>(values);
                        previewValues.put("_rowNo", String.valueOf(rowIndex + 1));
                        preview.getRows().add(previewValues);
                        result.setPreviewRows(result.getPreviewRows() + 1);
                    }
                    imported += importer.importRow(headers, values, rowIndex + 1, result) ? 1 : 0;
                }
                if (sheet.getLastRowNum() > lastRow)
                {
                    result.getErrors().add(new ImportError(sheet.getSheetName(), lastRow + 1, null, "Rows after scan limit are not imported"));
                }
            }
            saveJob(activityId, importType, file.getOriginalFilename(), username, result.getErrors().isEmpty() ? "SUCCESS" : "PARTIAL_SUCCESS", "imported=" + imported + errorSuffix(result));
            return result;
        }
        catch (Exception e)
        {
            saveJob(activityId, importType, file.getOriginalFilename(), username, "FAILED", e.getMessage());
            throw e;
        }
    }

    private Map<String, String> rowValues(Row row, List<String> headers)
    {
        Map<String, String> values = new LinkedHashMap<>();
        for (int col = 0; col < headers.size(); col++)
        {
            String header = normalize(headers.get(col));
            if (StringUtils.isEmpty(header))
            {
                header = "COL_" + (col + 1);
            }
            values.put(header, getCellText(row.getCell(col)));
        }
        return values;
    }

    private void parseSheet(Sheet sheet, ImportPreviewResult result)
    {
        SheetPreview preview = new SheetPreview();
        preview.setSheetName(sheet.getSheetName());
        int headerRowIndex = findHeaderRow(sheet);
        if (headerRowIndex < 0)
        {
            result.getErrors().add(new ImportError(sheet.getSheetName(), null, null, "No non-empty header row found"));
            result.getSheets().add(preview);
            return;
        }
        preview.setHeaderRowNo(headerRowIndex + 1);
        List<String> headers = readHeaders(sheet.getRow(headerRowIndex));
        preview.setHeaders(headers);
        if (headers.isEmpty())
        {
            result.getErrors().add(new ImportError(sheet.getSheetName(), headerRowIndex + 1, null, "Header row is empty"));
            result.getSheets().add(preview);
            return;
        }
        int lastRow = Math.min(sheet.getLastRowNum(), headerRowIndex + MAX_SCAN_ROWS_PER_SHEET);
        for (int rowIndex = headerRowIndex + 1; rowIndex <= lastRow; rowIndex++)
        {
            Row row = sheet.getRow(rowIndex);
            if (isRowEmpty(row, headers.size()))
            {
                continue;
            }
            result.setTotalRows(result.getTotalRows() + 1);
            if (preview.getRows().size() >= MAX_PREVIEW_ROWS_PER_SHEET)
            {
                continue;
            }
            Map<String, String> values = rowValues(row, headers);
            values.put("_rowNo", String.valueOf(rowIndex + 1));
            preview.getRows().add(values);
            result.setPreviewRows(result.getPreviewRows() + 1);
        }
        result.getSheets().add(preview);
    }

    private void validateExcelFile(MultipartFile file)
    {
        if (file == null || file.isEmpty())
        {
            throw new ServiceException("Uploaded file is empty");
        }
        String extension = FilenameUtils.getExtension(file.getOriginalFilename());
        if (!"xls".equalsIgnoreCase(extension) && !"xlsx".equalsIgnoreCase(extension))
        {
            throw new ServiceException("Only .xls and .xlsx files are supported");
        }
    }

    private int findHeaderRow(Sheet sheet)
    {
        int lastRow = Math.min(sheet.getLastRowNum(), MAX_SCAN_ROWS_PER_SHEET);
        for (int i = sheet.getFirstRowNum(); i <= lastRow; i++)
        {
            Row row = sheet.getRow(i);
            if (!isRowEmpty(row, MAX_SCAN_COLUMNS))
            {
                return i;
            }
        }
        return -1;
    }

    private int findSortedCandidateHeaderRow(Sheet sheet)
    {
        int lastRow = Math.min(sheet.getLastRowNum(), MAX_SCAN_ROWS_PER_SHEET);
        for (int i = sheet.getFirstRowNum(); i <= lastRow; i++)
        {
            Row row = sheet.getRow(i);
            if (isSortedCandidateHeader(readHeaders(row)))
            {
                return i;
            }
        }
        return -1;
    }

    private boolean isSortedCandidateHeader(List<String> headers)
    {
        if (headers == null || headers.isEmpty())
        {
            return false;
        }
        return hasAnyHeader(headers, "排序", "序号", "排名", "importSeq")
                && hasAnyHeader(headers, "姓名", "姓 名", "名称", "name")
                && hasAnyHeader(headers, "身份证号", "身份证号码", "证件号码", "idCard")
                && hasAnyHeader(headers, "申报职称", "申报等级", "报名等级", "职称", "等级", "appliedLevel", "level");
    }

    private boolean hasAnyHeader(List<String> headers, String... names)
    {
        for (String header : headers)
        {
            String normalizedHeader = normalize(header);
            for (String name : names)
            {
                if (normalizedHeader.equals(normalize(name)))
                {
                    return true;
                }
            }
        }
        return false;
    }

    private List<String> readHeaders(Row row)
    {
        List<String> headers = new ArrayList<>();
        if (row == null)
        {
            return headers;
        }
        int lastCell = Math.min(Math.max(row.getLastCellNum(), 0), MAX_SCAN_COLUMNS);
        for (int i = 0; i < lastCell; i++)
        {
            headers.add(getCellText(row.getCell(i)));
        }
        for (int i = headers.size() - 1; i >= 0; i--)
        {
            if (StringUtils.isNotEmpty(headers.get(i)))
            {
                break;
            }
            headers.remove(i);
        }
        return headers;
    }

    private boolean isRowEmpty(Row row, int columnLimit)
    {
        if (row == null)
        {
            return true;
        }
        int lastCell = Math.min(Math.max(row.getLastCellNum(), 0), Math.max(columnLimit, 1));
        for (int i = 0; i < lastCell; i++)
        {
            if (StringUtils.isNotEmpty(getCellText(row.getCell(i))))
            {
                return false;
            }
        }
        return true;
    }

    private String getCellText(Cell cell)
    {
        if (cell == null)
        {
            return "";
        }
        if (cell.getCellType() == CellType.STRING)
        {
            return normalize(cell.getStringCellValue());
        }
        if (cell.getCellType() == CellType.NUMERIC)
        {
            if (DateUtil.isCellDateFormatted(cell))
            {
                return new SimpleDateFormat("yyyy-MM-dd").format(cell.getDateCellValue());
            }
            return new DecimalFormat("0.################").format(cell.getNumericCellValue());
        }
        if (cell.getCellType() == CellType.BOOLEAN)
        {
            return String.valueOf(cell.getBooleanCellValue());
        }
        if (cell.getCellType() == CellType.FORMULA)
        {
            try
            {
                return normalize(cell.getStringCellValue());
            }
            catch (IllegalStateException e)
            {
                return new DecimalFormat("0.################").format(cell.getNumericCellValue());
            }
        }
        return "";
    }

    private String normalize(String value)
    {
        return value == null ? "" : value.replace('\u3000', ' ').trim();
    }

    private String normalizeIdCard(String value)
    {
        String normalized = normalize(value);
        return StringUtils.isEmpty(normalized) ? null : normalized.toUpperCase(Locale.ROOT);
    }

    private String valueOf(Map<String, String> values, String... names)
    {
        for (String name : names)
        {
            String value = values.get(name);
            if (StringUtils.isNotEmpty(value))
            {
                return value;
            }
        }
        return null;
    }

    private Integer parseInteger(String value)
    {
        if (StringUtils.isEmpty(value))
        {
            return null;
        }
        try
        {
            return Integer.valueOf(value.split("\\.")[0]);
        }
        catch (Exception e)
        {
            return null;
        }
    }

    private Date parseDate(String value)
    {
        if (StringUtils.isEmpty(value))
        {
            return null;
        }
        return DateUtils.parseDate(value);
    }

    private String errorSuffix(ImportPreviewResult result)
    {
        String message = buildErrorMessage(result);
        return StringUtils.isEmpty(message) ? "" : "; " + message;
    }

    private interface RowImporter
    {
        boolean importRow(List<String> headers, Map<String, String> values, int rowNo, ImportPreviewResult result);
    }

    private void saveJob(Long activityId, String importType, String fileName, String username, String status, String errorMessage)
    {
        ExportJob job = new ExportJob();
        job.setActivityId(activityId);
        job.setJobType("IMPORT");
        job.setExportType(importType);
        job.setStatus(status);
        job.setFileName(fileName);
        job.setGeneratedBy(username);
        job.setErrorMessage(limitJobErrorMessage(errorMessage));
        exportJobMapper.insertExportJob(job);
    }

    private String buildErrorMessage(ImportPreviewResult result)
    {
        if (result.getErrors().isEmpty())
        {
            return null;
        }
        StringBuilder builder = new StringBuilder();
        for (ImportError error : result.getErrors())
        {
            if (builder.length() > 0)
            {
                builder.append("; ");
            }
            StringBuilder item = new StringBuilder();
            if (StringUtils.isNotEmpty(error.getSheetName()))
            {
                item.append(error.getSheetName());
            }
            if (error.getRowNo() != null)
            {
                if (item.length() > 0)
                {
                    item.append(":");
                }
                item.append("row ").append(error.getRowNo());
            }
            if (StringUtils.isNotEmpty(error.getField()))
            {
                item.append("[").append(error.getField()).append("]");
            }
            if (item.length() > 0)
            {
                item.append(": ");
            }
            item.append(error.getReason());
            builder.append(item);
            if (builder.length() > MAX_JOB_ERROR_MESSAGE_LENGTH)
            {
                break;
            }
        }
        return limitJobErrorMessage(builder.toString());
    }

    private String limitJobErrorMessage(String message)
    {
        if (StringUtils.isEmpty(message))
        {
            return message;
        }
        if (message.length() <= MAX_JOB_ERROR_MESSAGE_LENGTH)
        {
            return message;
        }
        return message.substring(0, MAX_JOB_ERROR_MESSAGE_LENGTH - 20) + "...[truncated]";
    }
}

