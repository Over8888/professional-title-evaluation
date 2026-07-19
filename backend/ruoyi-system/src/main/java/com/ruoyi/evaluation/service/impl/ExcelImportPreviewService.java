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
import com.ruoyi.evaluation.support.EvaluationTitleUtils;

@Service
public class ExcelImportPreviewService
{
    private static final int MAX_PREVIEW_ROWS_PER_SHEET = 20;
    private static final int MAX_SCAN_ROWS_PER_SHEET = 2000;
    private static final int MAX_SCAN_COLUMNS = 80;
    private static final int MAX_JOB_ERROR_MESSAGE_LENGTH = 1000;
    private static final String IMPORT_TYPE_CANDIDATE = "CANDIDATE_IMPORT";
    private static final String IMPORT_TYPE_VOTER = "VOTER_IMPORT";
    private static final String NO_CANDIDATE_IMPORTED_MESSAGE = "未识别到可导入候选人，请检查 sheet 名是否为申报等级、是否为说明 sheet、表头是否包含姓名/身份证号。";

    private enum CandidateImportMappingMode
    {
        DEFAULT, SORTED, SYSTEM
    }

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
        return importSortedCandidates(activityId, file, username, candidateMapper, activityCandidateMapper);
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

                if (isInstructionSheet(sheet))
                {
                    continue;
                }
                int headerRowIndex = findSortedCandidateHeaderRow(sheet);
                boolean sortedSheet = false;
                if (headerRowIndex < 0)
                {
                    headerRowIndex = findCandidateHeaderRow(sheet);
                }
                else
                {
                    String sortedTitle = sheetTitle(sheet, headerRowIndex);
                    sortedSheet = isSortedCandidateSheet(sheet, sortedTitle)
                            || isSortedCandidateFile(file.getOriginalFilename());
                    if (!sortedSheet && parseAppliedLevel(sheet.getSheetName()) == null)
                    {
                        headerRowIndex = findCandidateHeaderRow(sheet);
                    }
                }
                if (headerRowIndex < 0)
                {
                    result.getErrors().add(new ImportError(sheet.getSheetName(), null, null, "No candidate header row found; skipped"));
                    continue;
                }
                String title = sheetTitle(sheet, headerRowIndex);
                sortedSheet = sortedSheet || isSortedCandidateSheet(sheet, title)
                        || isSortedCandidateFile(file.getOriginalFilename());
                String sheetAppliedLevel = parseAppliedLevel(sheet.getSheetName());
                String groupName = sortedSheet
                        ? resolveGroupName(file.getOriginalFilename(), null, null)
                        : resolveGroupName(file.getOriginalFilename(), title, sheet.getSheetName());

                List<String> headers = readHeaders(sheet.getRow(headerRowIndex));
                preview.setHeaderRowNo(headerRowIndex + 1);
                preview.setHeaders(headers);
                if (headers.isEmpty())
                {
                    result.getErrors().add(new ImportError(sheet.getSheetName(), headerRowIndex + 1, null, "Header row is empty"));
                    continue;
                }
                if (!sortedSheet && StringUtils.isEmpty(sheetAppliedLevel) && !hasAppliedLevelHeader(headers))
                {
                    result.getErrors().add(new ImportError(sheet.getSheetName(), headerRowIndex + 1, "appliedLevel", "Cannot identify applied level from sheet name or row header; skipped"));
                    continue;
                }
                if (StringUtils.isEmpty(groupName) && !hasDepartmentHeader(headers))
                {
                    result.getErrors().add(new ImportError(sheet.getSheetName(), headerRowIndex + 1, "department", "Cannot identify candidate group from file name, sheet title or row header; skipped"));
                    continue;
                }
                CandidateImportMappingMode mappingMode = resolveCandidateMappingMode(file.getOriginalFilename(), sortedSheet);

                int lastRow = Math.min(sheet.getLastRowNum(), headerRowIndex + MAX_SCAN_ROWS_PER_SHEET);
                for (int rowIndex = headerRowIndex + 1; rowIndex <= lastRow; rowIndex++)
                {
                    Row row = sheet.getRow(rowIndex);
                    if (isRowEmpty(row, headers.size()))
                    {
                        continue;
                    }
                    Map<String, String> values = rowValues(row, headers);
                    if (!sortedSheet && isCandidateIdentityBlank(values))
                    {
                        continue;
                    }
                    result.setTotalRows(result.getTotalRows() + 1);
                    if (preview.getRows().size() < MAX_PREVIEW_ROWS_PER_SHEET)
                    {
                        Map<String, String> previewValues = new LinkedHashMap<>(values);
                        previewValues.put("_rowNo", String.valueOf(rowIndex + 1));
                        previewValues.put("_group", groupName);
                        preview.getRows().add(previewValues);
                        result.setPreviewRows(result.getPreviewRows() + 1);
                    }
                    if (sortedSheet)
                    {
                        imported += importSortedCandidateRow(activityId, groupName, values, rowIndex + 1, result,
                                seenCandidateKeys, nextPoolImportSeq, candidateMapper, activityCandidateMapper, username,
                                mappingMode) ? 1 : 0;
                    }
                    else
                    {
                        imported += importCandidateRow(activityId, groupName, sheetAppliedLevel, values, rowIndex + 1, result,
                                seenCandidateKeys, nextPoolImportSeq, candidateMapper, activityCandidateMapper, username, "EXCEL",
                                mappingMode) ? 1 : 0;
                    }
                }
                if (sheet.getLastRowNum() > lastRow)
                {
                    result.getErrors().add(new ImportError(sheet.getSheetName(), lastRow + 1, null, "Rows after scan limit are not imported"));
                }
            }
            result.setImportedRows(imported);
            assertImportedCandidateRows(imported, result);
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
            CandidateMapper candidateMapper, ActivityCandidateMapper activityCandidateMapper, String username,
            CandidateImportMappingMode mappingMode)
    {
        Candidate candidate = buildCandidateFromValues(groupName, null, values, username, mappingMode);
        candidate.setImportSeq(parseInteger(valueOf(values, "排序", "序号", "排名", "importSeq")));
        if (candidate.getImportSeq() == null)
        {
            result.getErrors().add(new ImportError(null, rowNo, "importSeq", "Candidate rank is empty or invalid; skipped"));
            return false;
        }
        return importCandidate(activityId, candidate, rowNo, result, seenCandidateKeys, nextPoolImportSeq,
                candidateMapper, activityCandidateMapper, username, "SORTED_EXCEL");
    }

    private boolean importCandidateRow(Long activityId, String fallbackGroupName, String sheetAppliedLevel,
            Map<String, String> values, int rowNo, ImportPreviewResult result, Set<String> seenCandidateKeys,
            int[] nextPoolImportSeq, CandidateMapper candidateMapper, ActivityCandidateMapper activityCandidateMapper,
            String username, String sourceType, CandidateImportMappingMode mappingMode)
    {
        Candidate candidate = buildCandidateFromValues(fallbackGroupName, sheetAppliedLevel, values, username, mappingMode);
        Integer importSeq = parseInteger(valueOf(values, "排序", "序号", "排名", "importSeq"));
        candidate.setImportSeq(importSeq);
        return importCandidate(activityId, candidate, rowNo, result, seenCandidateKeys, nextPoolImportSeq,
                candidateMapper, activityCandidateMapper, username, sourceType);
    }

    private Candidate buildCandidateFromValues(String fallbackGroupName, String sheetAppliedLevel,
            Map<String, String> values, String username, CandidateImportMappingMode mappingMode)
    {
        Candidate candidate = new Candidate();
        candidate.setCompany(resolveCompany(values, mappingMode));
        candidate.setName(valueOf(values, "姓名", "姓 名", "名称", "编号", "人员编号", "name"));
        candidate.setGender(valueOf(values, "性别", "gender"));
        candidate.setBirthDate(parseDate(valueOf(values, "出生年月", "出生日期", "birthDate")));
        candidate.setEducation(valueOf(values, "学历", "最高学历", "education"));
        candidate.setFirstLevelDepartment(valueOf(values, "一级部门", "一级组织", "firstLevelDepartment"));
        candidate.setDepartment(resolveDepartment(values, fallbackGroupName, mappingMode));
        candidate.setPosition(resolvePosition(values, mappingMode));
        candidate.setIdCard(normalizeIdCard(valueOf(values, "身份证号", "身份证号码", "身份证件号", "证件号码", "证件号", "idCard")));
        candidate.setCurrentLevel(valueOf(values, "当前等级", "现等级", "现职称", "currentLevel"));
        candidate.setAppliedLevel(normalizeAppliedLevel(firstPresent(sheetAppliedLevel,
                valueOf(values, "申报职称", "申报等级", "报名等级", "职称", "等级", "appliedLevel", "level"))));
        candidate.setThirdLevelDepartment(resolveThirdLevelDepartment(values, candidate, mappingMode));
        candidate.setLastYearAssessment(valueOf(values, "上年度考核结果", "lastYearAssessment"));
        candidate.setEvaluationScore(valueOf(values, "评价结果", "评价分", "评分", "综合得分", "evaluationScore"));
        candidate.setCreateBy(username);
        candidate.setFixedType(null);
        return candidate;
    }

    private CandidateImportMappingMode resolveCandidateMappingMode(String fileName, boolean sortedSheet)
    {
        String normalizedFileName = StringUtils.defaultString(fileName);
        if (normalizedFileName.contains("系统导入"))
        {
            return CandidateImportMappingMode.SYSTEM;
        }
        if (sortedSheet || normalizedFileName.contains("排序"))
        {
            return CandidateImportMappingMode.SORTED;
        }
        return CandidateImportMappingMode.DEFAULT;
    }

    private String resolveCompany(Map<String, String> values, CandidateImportMappingMode mappingMode)
    {
        if (mappingMode == CandidateImportMappingMode.SORTED)
        {
            return valueOf(values, "工作单位", "所在单位", "company");
        }
        return valueOf(values, "单位", "工作单位", "所在单位", "company");
    }

    private String resolveDepartment(Map<String, String> values, String fallbackGroupName,
            CandidateImportMappingMode mappingMode)
    {
        if (mappingMode == CandidateImportMappingMode.SORTED)
        {
            return fallbackGroupName;
        }
        if (mappingMode == CandidateImportMappingMode.SYSTEM)
        {
            return firstPresent(valueOf(values, "二级部门", "二级组织", "department"),
                    valueOf(values, "评审专业", "组别", "申报组别"), fallbackGroupName);
        }
        return firstPresent(valueOf(values, "评审专业", "组别", "申报组别", "二级部门", "二级组织", "department"),
                fallbackGroupName);
    }

    private String resolvePosition(Map<String, String> values, CandidateImportMappingMode mappingMode)
    {
        if (mappingMode == CandidateImportMappingMode.SORTED)
        {
            return valueOf(values, "单位", "现岗位", "岗位", "部门/岗位", "部门", "职务", "position");
        }
        if (mappingMode == CandidateImportMappingMode.SYSTEM)
        {
            return valueOf(values, "岗位", "部门/岗位", "现岗位", "职务", "position");
        }
        return valueOf(values, "现岗位", "岗位", "部门/岗位", "部门", "职务", "position");
    }

    private String resolveThirdLevelDepartment(Map<String, String> values, Candidate candidate,
            CandidateImportMappingMode mappingMode)
    {
        if (mappingMode == CandidateImportMappingMode.SYSTEM)
        {
            return firstPresent(valueOf(values, "三级部门", "thirdLevelDepartment"),
                    valueOf(values, "专业", "专业组别"));
        }
        return firstPresent(valueOf(values, "三级部门", "评审专业", "专业", "专业组别", "thirdLevelDepartment"),
                buildThirdLevelDepartment(candidate.getDepartment(), candidate.getCurrentLevel()));
    }

    private boolean importCandidate(Long activityId, Candidate candidate, int rowNo, ImportPreviewResult result,
            Set<String> seenCandidateKeys, int[] nextPoolImportSeq, CandidateMapper candidateMapper,
            ActivityCandidateMapper activityCandidateMapper, String username, String sourceType)
    {
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
        if (activityId == null)
        {
            if (!seenCandidateKeys.add(candidate.getIdCard()))
            {
                result.getErrors().add(new ImportError(null, rowNo, "idCard", "Duplicate ID card in uploaded file; skipped"));
                return false;
            }
            if (candidateMapper.selectCandidateByIdCard(candidate.getIdCard()) != null)
            {
                result.getErrors().add(new ImportError(null, rowNo, "idCard", "ID card already exists in candidate pool; skipped"));
                return false;
            }
            assignPoolSequence(candidate, nextPoolImportSeq);
            candidateMapper.insertCandidate(candidate);
            return true;
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
        ActivityCandidate activityCandidate = toActivityCandidate(activityId, poolCandidate, candidate, sourceType, username);
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

    private boolean isCandidateIdentityBlank(Map<String, String> values)
    {
        return StringUtils.isEmpty(valueOf(values, "姓名", "姓 名", "名称", "name"))
                && StringUtils.isEmpty(valueOf(values, "身份证号", "身份证号码", "身份证件号", "证件号码", "证件号", "idCard"));
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

    private boolean isSortedCandidateFile(String fileName)
    {
        return StringUtils.defaultString(fileName).contains("排序");
    }

    private boolean isInstructionSheet(Sheet sheet)
    {
        String sheetName = normalize(sheet == null ? null : sheet.getSheetName()).toLowerCase(Locale.ROOT);
        return sheetName.contains("说明")
                || sheetName.contains("填报说明")
                || sheetName.contains("导入说明")
                || sheetName.contains("模板说明")
                || sheetName.contains("readme");
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
        String text = normalizeGroupSource(fileName) + " " + normalizeGroupSource(title) + " " + normalizeGroupSource(sheetName);
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

    private String normalizeGroupSource(String value)
    {
        String text = StringUtils.defaultString(value);
        while (text.contains("组组"))
        {
            text = text.replace("组组", "组");
        }
        return text;
    }

    private void parseSortedCandidateSheet(Sheet sheet, String fileName, ImportPreviewResult result)
    {
        if (isInstructionSheet(sheet))
        {
            return;
        }
        SheetPreview preview = new SheetPreview();
        preview.setSheetName(sheet.getSheetName());
        result.getSheets().add(preview);

        int headerRowIndex = findSortedCandidateHeaderRow(sheet);
        boolean sortedSheet = false;
        if (headerRowIndex < 0)
        {
            headerRowIndex = findCandidateHeaderRow(sheet);
        }
        else
        {
            String sortedTitle = sheetTitle(sheet, headerRowIndex);
            sortedSheet = isSortedCandidateSheet(sheet, sortedTitle) || isSortedCandidateFile(fileName);
            if (!sortedSheet && parseAppliedLevel(sheet.getSheetName()) == null)
            {
                headerRowIndex = findCandidateHeaderRow(sheet);
            }
        }
        if (headerRowIndex < 0)
        {
            return;
        }
        String title = sheetTitle(sheet, headerRowIndex);
        sortedSheet = sortedSheet || isSortedCandidateSheet(sheet, title) || isSortedCandidateFile(fileName);
        String sheetAppliedLevel = parseAppliedLevel(sheet.getSheetName());
        if (!sortedSheet && StringUtils.isEmpty(sheetAppliedLevel))
        {
            return;
        }
        String groupName = sortedSheet
                ? resolveGroupName(fileName, null, null)
                : resolveGroupName(fileName, title, sheet.getSheetName());
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
            Map<String, String> values = rowValues(row, headers);
            if (!sortedSheet && isCandidateIdentityBlank(values))
            {
                continue;
            }
            result.setTotalRows(result.getTotalRows() + 1);
            if (preview.getRows().size() >= MAX_PREVIEW_ROWS_PER_SHEET)
            {
                continue;
            }
            values.put("_rowNo", String.valueOf(rowIndex + 1));
            values.put("_group", groupName);
            if (StringUtils.isNotEmpty(sheetAppliedLevel))
            {
                values.put("_sheetAppliedLevel", sheetAppliedLevel);
            }
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
            result.setImportedRows(imported);
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

    private int findCandidateHeaderRow(Sheet sheet)
    {
        int lastRow = Math.min(sheet.getLastRowNum(), MAX_SCAN_ROWS_PER_SHEET);
        for (int i = sheet.getFirstRowNum(); i <= lastRow; i++)
        {
            Row row = sheet.getRow(i);
            if (isCandidateHeader(readHeaders(row)))
            {
                return i;
            }
        }
        return -1;
    }

    private boolean isCandidateHeader(List<String> headers)
    {
        if (headers == null || headers.isEmpty())
        {
            return false;
        }
        return hasAnyHeader(headers, "姓名", "姓 名", "名称", "编号", "人员编号", "name")
                && hasAnyHeader(headers, "身份证号", "身份证号码", "身份证件号", "证件号码", "证件号", "idCard");
    }

    private boolean hasAppliedLevelHeader(List<String> headers)
    {
        return hasAnyHeader(headers, "申报职称", "申报等级", "报名等级", "职称", "等级", "appliedLevel", "level");
    }

    private boolean hasDepartmentHeader(List<String> headers)
    {
        return hasAnyHeader(headers, "评审专业", "组别", "申报组别", "二级部门", "二级组织", "department");
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

    private String firstPresent(String... values)
    {
        if (values == null)
        {
            return null;
        }
        for (String value : values)
        {
            if (StringUtils.isNotEmpty(value))
            {
                return value;
            }
        }
        return null;
    }

    private String parseAppliedLevel(String text)
    {
        return normalizeAppliedLevel(text);
    }

    private String normalizeAppliedLevel(String value)
    {
        return EvaluationTitleUtils.normalizeAppliedLevel(value);
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

    private void assertImportedCandidateRows(int imported, ImportPreviewResult result)
    {
        if (imported > 0)
        {
            return;
        }
        if (result.getErrors().isEmpty())
        {
            result.getErrors().add(new ImportError(null, null, null, NO_CANDIDATE_IMPORTED_MESSAGE));
        }
        throw new ServiceException(NO_CANDIDATE_IMPORTED_MESSAGE + errorSuffix(result));
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

