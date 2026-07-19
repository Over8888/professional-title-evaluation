package com.ruoyi.evaluation.service.impl;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.ruoyi.common.config.RuoYiConfig;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.DateUtils;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.common.utils.file.FileUtils;
import com.ruoyi.evaluation.domain.Activity;
import com.ruoyi.evaluation.domain.ActivityCandidate;
import com.ruoyi.evaluation.domain.ActivityVoter;
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
import com.ruoyi.evaluation.service.IResultAggService;
import com.ruoyi.evaluation.support.EvaluationTitleUtils;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Service
public class ResultAggServiceImpl implements IResultAggService
{
    private static final ZoneId ZONE_SHANGHAI = ZoneId.of("Asia/Shanghai");
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final String SCOPE_CANDIDATE_GROUP = "CANDIDATE_GROUP";
    private static final String TYPE_VOTE_SUMMARY = "VOTE_SUMMARY";
    private static final String TYPE_STAT_RESULT = "STAT_RESULT";
    private static final String TYPE_PASS_DECISION = "PASS_DECISION";
    private static final String TYPE_FINAL_DECISION = "FINAL_DECISION";
    @Autowired
    private ActivityMapper activityMapper;

    @Autowired
    private ActivityCandidateMapper activityCandidateMapper;

    @Autowired
    private ActivityVoterMapper activityVoterMapper;

    @Autowired
    private VoteMapper voteMapper;

    @Autowired
    private RuleConfigMapper ruleConfigMapper;

    @Autowired
    private ResultAggMapper resultAggMapper;

    @Autowired
    private ExportJobMapper exportJobMapper;

    @Autowired
    private FinalEvaluationMapper finalEvaluationMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> calculate(Long activityId, String username, boolean force)
    {
        if (activityId == null)
        {
            throw new ServiceException("Activity ID is required.");
        }
        Activity activity = activityMapper.selectActivityByIdForUpdate(activityId);
        if (activity == null)
        {
            throw new ServiceException("Activity does not exist.");
        }
        String status = StringUtils.defaultIfEmpty(activity.getStatus(), "DRAFT");
        if (!"CLOSED".equals(status) && !"CALCULATED".equals(status))
        {
            throw new ServiceException("Only CLOSED activities can be calculated.");
        }
        requireAllVotersDone(activityId);
        if (!force && resultAggMapper.countResultAggByActivityId(activityId) > 0)
        {
            return summary(activityId);
        }

        List<ActivityCandidate> candidates = loadCandidates(activityId);
        if (candidates.isEmpty())
        {
            throw new ServiceException("Activity has no candidate snapshots.");
        }

        RuleConfig ruleConfig = ruleConfigMapper.selectRuleConfigByActivityId(activityId);
        if (ruleConfig == null || ruleConfig.getPassRatio() == null)
        {
            throw new ServiceException("Activity rule config is required before calculation.");
        }

        List<ResultAgg> resultRows = buildResultRows(activityId, candidates, loadVoteCounts(activityId), ruleConfig, username);
        resultAggMapper.deleteResultAggByActivityId(activityId);
        resultAggMapper.batchInsertResultAgg(resultRows);

        Activity update = new Activity();
        update.setId(activityId);
        update.setStatus("CALCULATED");
        update.setUpdatedBy(username);
        activityMapper.updateActivity(update);
        return summary(activityId);
    }

    @Override
    public Map<String, Object> summary(Long activityId)
    {
        if (activityId == null)
        {
            throw new ServiceException("Activity ID is required.");
        }
        Activity activity = activityMapper.selectActivityById(activityId);
        if (activity == null)
        {
            throw new ServiceException("Activity does not exist.");
        }

        List<ActivityCandidate> candidates = loadCandidates(activityId);
        int voteCandidateCount = activityCandidateMapper.countVoteCandidatesByActivityId(activityId);
        int totalVoters = activityVoterMapper.countActivityVoterByActivityId(activityId);
        int doneVoters = activityVoterMapper.countDoneByActivityId(activityId);
        boolean allCompleted = voteCandidateCount == 0 || (totalVoters > 0 && totalVoters == doneVoters);
        int resultCount = resultAggMapper.countResultAggByActivityId(activityId);
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("activityId", activity.getId());
        data.put("activityName", activity.getName());
        data.put("status", activity.getStatus());
        data.put("activityStatus", activity.getStatus());
        data.put("candidateCount", candidates.size());
        data.put("voteCount", voteCandidateCount);
        data.put("totalVoters", totalVoters);
        data.put("doneVoters", doneVoters);
        data.put("pendingVoters", Math.max(0, totalVoters - doneVoters));
        data.put("allCompleted", allCompleted);
        data.put("resultCount", resultCount);
        data.put("calculated", resultCount > 0);
        data.put("calculatedAt", resultAggMapper.selectLatestCalculatedAt(activityId));
        data.put("canClose", ("PUBLISHED".equals(activity.getStatus()) || "VOTING".equals(activity.getStatus())) && allCompleted);
        data.put("canCalculate", "CLOSED".equals(activity.getStatus()) && allCompleted);
        data.put("canExport", resultCount > 0);
        if (resultCount > 0)
        {
            ResultAgg query = new ResultAgg();
            query.setActivityId(activityId);
            query.setStatScope(SCOPE_CANDIDATE_GROUP);
            List<ResultAgg> rows = resultAggMapper.selectResultAggList(query);
            long passCount = rows.stream().filter(row -> "PASS".equals(row.getFinalResult())).count();
            long rejectCount = rows.stream().filter(row -> "REJECT".equals(row.getFinalResult())).count();
            data.put("passCount", passCount);
            data.put("rejectCount", rejectCount);
        }
        else
        {
            data.put("passCount", 0);
            data.put("rejectCount", 0);
        }
        data.put("calculatedAt", formatDateTime(resultAggMapper.selectLatestCalculatedAt(activityId)));
        return data;
    }

    @Override
    public List<Map<String, Object>> selectResultAggList(ResultAgg resultAgg)
    {
        if (resultAgg == null || resultAgg.getActivityId() == null)
        {
            throw new ServiceException("Activity ID is required.");
        }
        if (StringUtils.isEmpty(resultAgg.getStatScope()))
        {
            resultAgg.setStatScope(SCOPE_CANDIDATE_GROUP);
        }
        List<ResultAgg> rows = resultAggMapper.selectResultAggList(resultAgg);
        return buildGroupSummaryRows(rows);
    }

    @Override
    public List<ResultAgg> selectCandidateResultList(ResultAgg resultAgg)
    {
        if (resultAgg == null || resultAgg.getActivityId() == null)
        {
            throw new ServiceException("Activity ID is required.");
        }
        resultAgg.setStatScope(SCOPE_CANDIDATE_GROUP);
        return resultAggMapper.selectResultAggList(resultAgg);
    }

    @Override
    public Map<String, Object> voteSummary(Long activityId)
    {
        if (activityId == null)
        {
            throw new ServiceException("Activity ID is required.");
        }
        Activity activity = activityMapper.selectActivityById(activityId);
        if (activity == null)
        {
            throw new ServiceException("Activity does not exist.");
        }
        ResultAgg query = new ResultAgg();
        query.setActivityId(activityId);
        query.setStatScope(SCOPE_CANDIDATE_GROUP);
        List<ResultAgg> rows = resultAggMapper.selectResultAggList(query);
        List<Vote> details = voteMapper.selectVoteDetailByActivityId(activityId);
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("activityId", activityId);
        data.put("summaryRows", voteSummaryRows(activityId, rows));
        data.put("detailRows", voteDetailRows(details));
        data.put("displayRows", voteDisplayRows(rows));
        return data;
    }

    @Override
    public Map<String, Object> publicResult(Long activityId)
    {
        Map<String, Object> data = summary(activityId);
        boolean allCompleted = Boolean.TRUE.equals(data.get("allCompleted"));
        boolean calculated = Boolean.TRUE.equals(data.get("calculated"));
        data.put("activityStatus", data.get("status"));
        if (!allCompleted || !calculated)
        {
            data.put("resultRows", new ArrayList<>());
            return data;
        }

        ResultAgg query = new ResultAgg();
        query.setActivityId(activityId);
        query.setStatScope(SCOPE_CANDIDATE_GROUP);
        List<ResultAgg> rows = resultAggMapper.selectResultAggList(query);
        data.put("resultRows", rows.stream()
                .map(this::toPublicResultRow)
                .collect(Collectors.toList()));
        return data;
    }

    @Override
    public void exportResult(HttpServletResponse response, Long activityId, String exportType, String username)
    {
        ExportJob job = export(activityId, exportType, username);
        try
        {
            File target = new File(RuoYiConfig.getDownloadPath(), job.getFileName().replace("/", File.separator));
            FileUtils.setAttachmentResponseHeader(response, target.getName());
            FileUtils.writeBytes(target.getPath(), response.getOutputStream());
        }
        catch (Exception e)
        {
            throw new ServiceException("Result export download failed: " + e.getMessage());
        }
    }

    public ExportJob export(Long activityId, String exportType, String username)
    {
        if (activityId == null)
        {
            throw new ServiceException("Activity ID is required.");
        }
        String type = normalizeExportType(exportType);
        ExportJob job = new ExportJob();
        job.setActivityId(activityId);
        job.setJobType("EXPORT");
        job.setExportType(type);
        job.setGeneratedBy(username);
        try
        {
            Activity activity = activityMapper.selectActivityById(activityId);
            if (activity == null)
            {
                throw new ServiceException("Activity does not exist.");
            }
            if (resultAggMapper.countResultAggByActivityId(activityId) == 0)
            {
                throw new ServiceException("Please calculate results before export.");
            }
            ResultAgg query = new ResultAgg();
            query.setActivityId(activityId);
            query.setStatScope(SCOPE_CANDIDATE_GROUP);
            List<ResultAgg> rows = resultAggMapper.selectResultAggList(query);
            if (TYPE_FINAL_DECISION.equals(type))
            {
                applyConfirmedFinalEvaluation(activityId, rows);
            }
            String relativeFileName = writeExportFile(activity, rows, ruleConfigMapper.selectRuleConfigByActivityId(activityId), type);
            job.setStatus("SUCCESS");
            job.setFileName(relativeFileName);
            job.setFileUrl(downloadUrl(relativeFileName));
            exportJobMapper.insertExportJob(job);
            markExported(activityId, type, username);
            return job;
        }
        catch (Exception e)
        {
            job.setStatus("FAILED");
            job.setErrorMessage(limit(e.getMessage()));
            exportJobMapper.insertExportJob(job);
            if (e instanceof ServiceException)
            {
                throw (ServiceException) e;
            }
            throw new ServiceException("Result export failed: " + e.getMessage());
        }
    }

    private List<ActivityCandidate> loadCandidates(Long activityId)
    {
        return activityCandidateMapper.selectAllCandidatesByActivityId(activityId);
    }

    private void requireAllVotersDone(Long activityId)
    {
        if (activityCandidateMapper.countVoteCandidatesByActivityId(activityId) == 0)
        {
            return;
        }
        int total = activityVoterMapper.countActivityVoterByActivityId(activityId);
        int done = activityVoterMapper.countDoneByActivityId(activityId);
        if (total <= 0)
        {
            throw new ServiceException("Activity has no voters.");
        }
        if (done != total)
        {
            throw new ServiceException("All voters must submit before close or calculation. Pending voters: " + (total - done));
        }
    }

    private Map<Long, VoteCounts> loadVoteCounts(Long activityId)
    {
        Map<Long, VoteCounts> counts = new HashMap<>();
        for (Vote summary : voteMapper.selectVoteSummaryByActivityId(activityId))
        {
            VoteCounts row = counts.computeIfAbsent(summary.getActivityCandidateId(), key -> new VoteCounts());
            int count = summary.getVoteCount() == null ? 0 : summary.getVoteCount();
            if ("PASS".equals(summary.getResult()))
            {
                row.pass = count;
            }
            else if ("REJECT".equals(summary.getResult()))
            {
                row.reject = count;
            }
        }
        return counts;
    }

    private List<ResultAgg> buildResultRows(Long activityId, List<ActivityCandidate> candidates,
            Map<Long, VoteCounts> counts, RuleConfig ruleConfig, String username)
    {
        List<ResultRow> workingRows = new ArrayList<>();
        for (ActivityCandidate candidate : candidates)
        {
            VoteCounts voteCounts = counts.getOrDefault(candidate.getId(), new VoteCounts());
            ResultRow row = new ResultRow();
            row.candidate = candidate;
            row.pass = voteCounts.pass;
            row.reject = voteCounts.reject;
            workingRows.add(row);
        }

        Map<String, List<ResultRow>> grouped = workingRows.stream()
                .collect(Collectors.groupingBy(this::groupKey, LinkedHashMap::new, Collectors.toList()));
        for (Map.Entry<String, List<ResultRow>> entry : grouped.entrySet())
        {
            assignGroupResult(entry.getValue(), ruleConfig);
        }

        Date now = new Date();
        List<ResultAgg> resultRows = new ArrayList<>();
        for (ResultRow row : workingRows)
        {
            resultRows.add(toResultAgg(activityId, row, username, now));
        }
        return resultRows;
    }

    private List<Map<String, Object>> buildGroupSummaryRows(List<ResultAgg> rows)
    {
        Map<String, GroupSummary> grouped = new LinkedHashMap<>();
        for (ResultAgg row : rows)
        {
            String department = text(row.getDepartment());
            String appliedLevel = text(row.getAppliedLevel());
            String key = department + "\u0000" + appliedLevel;
            GroupSummary summary = grouped.computeIfAbsent(key, unused -> new GroupSummary(department, appliedLevel));
            summary.candidateCount++;
            summary.passCount += value(row.getFinalResult(), "PASS");
            summary.rejectCount += value(row.getFinalResult(), "REJECT");
            summary.thirdLevelDepartments.add(text(row.getThirdLevelDepartment()));
            summary.calculatedAt = latestTime(summary.calculatedAt, row.getCalculatedAt());
        }

        List<Map<String, Object>> result = new ArrayList<>();
        for (GroupSummary summary : grouped.values())
        {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("department", summary.department);
            row.put("appliedLevel", summary.appliedLevel);
            row.put("candidateCount", summary.candidateCount);
            row.put("passCount", summary.passCount);
            row.put("rejectCount", summary.rejectCount);
            row.put("thirdLevelDepartment", String.join("、", summary.thirdLevelDepartments));
            row.put("passRate", rate(summary.passCount, summary.candidateCount));
            row.put("rejectRate", rate(summary.rejectCount, summary.candidateCount));
            row.put("calculatedAt", formatDateTime(summary.calculatedAt));
            result.add(row);
        }
        result.sort(Comparator
                .comparing((Map<String, Object> row) -> departmentRank(text(row.get("department"))))
                .thenComparing(row -> text(row.get("department")))
                .thenComparing((Map<String, Object> row) -> levelRank(text(row.get("appliedLevel"))))
                .thenComparing(row -> text(row.get("appliedLevel")))
                .thenComparing((Map<String, Object> row) -> -intValue(row.get("passCount")))
                .thenComparing((Map<String, Object> row) -> intValue(row.get("rejectCount")))
                .thenComparing((Map<String, Object> row) -> text(row.get("calculatedAt"))));
        return result;
    }

    private Map<String, Object> toPublicResultRow(ResultAgg row)
    {
        Map<String, Object> publicRow = new LinkedHashMap<>();
        publicRow.put("activityCandidateId", row.getActivityCandidateId());
        publicRow.put("importSeq", row.getImportSeq());
        publicRow.put("name", row.getCandidateName());
        publicRow.put("company", row.getCompany());
        publicRow.put("position", row.getPosition());
        publicRow.put("department", row.getDepartment());
        publicRow.put("thirdLevelDepartment", row.getThirdLevelDepartment());
        publicRow.put("appliedLevel", row.getAppliedLevel());
        publicRow.put("passCount", row.getVotePassCount());
        publicRow.put("rejectCount", row.getVoteRejectCount());
        publicRow.put("totalVotes", row.getTotalVotes());
        publicRow.put("passRate", row.getPassRate());
        publicRow.put("rejectRate", row.getRejectRate());
        publicRow.put("rankNo", row.getRankNo());
        publicRow.put("fixedType", row.getFixedType());
        publicRow.put("finalResult", row.getFinalResult());
        return publicRow;
    }

    private int value(String finalResult, String expected)
    {
        if (expected == null)
        {
            return 0;
        }
        return expected.equals(finalResult) ? 1 : 0;
    }

    private Date latestTime(Date current, Date next)
    {
        if (next == null)
        {
            return current;
        }
        if (current == null)
        {
            return next;
        }
        return next.after(current) ? next : current;
    }

    private int intValue(Object value)
    {
        if (value instanceof Number)
        {
            return ((Number) value).intValue();
        }
        if (value == null)
        {
            return 0;
        }
        try
        {
            return Integer.parseInt(String.valueOf(value));
        }
        catch (Exception e)
        {
            return 0;
        }
    }

    private int departmentRank(String department)
    {
        if ("船体组".equals(department))
        {
            return 1;
        }
        if ("船机组".equals(department))
        {
            return 2;
        }
        if ("船电组".equals(department))
        {
            return 3;
        }
        if ("综合组".equals(department))
        {
            return 4;
        }
        return 99;
    }

    private int levelRank(String level)
    {
        return EvaluationTitleUtils.levelRank(level);
    }

    private void assignGroupResult(List<ResultRow> group, RuleConfig ruleConfig)
    {
        if (!group.isEmpty() && isAutoPassLevel(group.get(0).candidate.getAppliedLevel()))
        {
            for (ResultRow row : group)
            {
                row.rankNo = row.candidate.getImportSeq();
                row.finalResult = "PASS";
            }
            return;
        }
        int maxPass = maxPassCount(group, ruleConfig);
        int fixedPassCount = 0;
        for (ResultRow row : group)
        {
            if ("PASS".equals(row.candidate.getFixedType()))
            {
                row.rankNo = row.candidate.getImportSeq();
                row.finalResult = "PASS";
                fixedPassCount++;
            }
            else if ("REJECT".equals(row.candidate.getFixedType()))
            {
                row.rankNo = row.candidate.getImportSeq();
                row.finalResult = "REJECT";
            }
        }
        int remainingPass = Math.max(0, maxPass - fixedPassCount);
        List<ResultRow> voteRows = group.stream()
                .filter(row -> "VOTE".equals(row.candidate.getFixedType()))
                .sorted(voteRankComparator())
                .collect(Collectors.toList());
        int rank = 1;
        for (ResultRow row : voteRows)
        {
            row.rankNo = rank++;
            if (remainingPass > 0)
            {
                row.finalResult = "PASS";
                remainingPass--;
            }
            else
            {
                row.finalResult = "REJECT";
            }
        }
    }

    private int maxPassCount(List<ResultRow> group, RuleConfig ruleConfig)
    {
        if (group == null || group.isEmpty())
        {
            return 0;
        }
        if (isAutoPassLevel(group.get(0).candidate.getAppliedLevel()))
        {
            return group.size();
        }
        return ratioCount(group.size(), ruleConfig.getPassRatio());
    }

    private ResultAgg toResultAgg(Long activityId, ResultRow row, String username, Date now)
    {
        int total = row.pass + row.reject;
        ResultAgg result = new ResultAgg();
        result.setActivityId(activityId);
        result.setActivityCandidateId(row.candidate.getId());
        result.setStatScope(SCOPE_CANDIDATE_GROUP);
        result.setStatKey(groupKey(row));
        result.setVotePassCount(row.pass);
        result.setVoteRejectCount(row.reject);
        result.setTotalVotes(total);
        result.setPassRate(rate(row.pass, total));
        result.setRejectRate(rate(row.reject, total));
        result.setRankNo(row.rankNo);
        result.setFinalResult(row.finalResult);
        result.setUpdateAt(now);
        result.setCalculatedBy(username);
        result.setCalculatedAt(now);
        return result;
    }

    private String writeExportFile(Activity activity, List<ResultAgg> rows, RuleConfig ruleConfig, String type) throws Exception
    {
        List<ResultAgg> exportRows = exportRows(rows);
        String relativeFileName = DateUtils.datePath() + "/" + exportFileName(activity.getId(), type);
        File target = new File(RuoYiConfig.getDownloadPath(), relativeFileName.replace("/", File.separator));
        File parent = target.getParentFile();
        if (parent != null && !parent.exists() && !parent.mkdirs())
        {
            throw new ServiceException("Failed to create export directory.");
        }

        if (TYPE_STAT_RESULT.equals(type))
        {
            writeDepartmentZip(target, exportRows);
            return relativeFileName;
        }

        try (XSSFWorkbook workbook = new XSSFWorkbook(); FileOutputStream output = new FileOutputStream(target))
        {
            CellStyle headerStyle = headerStyle(workbook);
            if (TYPE_VOTE_SUMMARY.equals(type))
            {
                writeFigure2(workbook, headerStyle, activity.getId(), exportRows);
            }
            else if (TYPE_PASS_DECISION.equals(type))
            {
                writeFigure4(workbook, headerStyle, exportRows);
            }
            else if (TYPE_FINAL_DECISION.equals(type))
            {
                writeFinalDecision(workbook, headerStyle, exportRows);
            }
            else
            {
                writeFigure3(workbook, headerStyle, exportRows, ruleConfig);
            }
            workbook.write(output);
        }
        return relativeFileName;
    }

    private void writeFigure2(XSSFWorkbook workbook, CellStyle headerStyle, Long activityId, List<ResultAgg> rows)
    {
        Sheet summarySheet = workbook.createSheet("投票情况");
        writeHeader(summarySheet, headerStyle, 0, "主体名称", "人数", "未投票", "未提交", "已投票");
        int summaryRowNo = 1;
        for (Map<String, Object> summary : voteSummaryRows(activityId, rows))
        {
            Row row = summarySheet.createRow(summaryRowNo++);
            cell(row, 0, summary.get("subjectName"));
            cell(row, 1, summary.get("total"));
            cell(row, 2, summary.get("notVoted"));
            cell(row, 3, summary.get("notSubmitted"));
            cell(row, 4, summary.get("voted"));
        }
        autosize(summarySheet, 5);

        List<Vote> detailRows = voteMapper.selectVoteDetailByActivityId(activityId);
        Map<String, List<Vote>> detailByDepartment = detailRows.stream()
                .collect(Collectors.groupingBy(vote -> text(vote.getVoterDepartment()), LinkedHashMap::new, Collectors.toList()));
        List<String> departments = sortedVoteDepartments(voteSummaryRows(activityId, rows), detailByDepartment);
        int sheetNo = 1;
        for (String department : departments)
        {
            Sheet detailSheet = workbook.createSheet(safeSheetName(sheetNo + "、" + StringUtils.defaultIfEmpty(department, "未分组")));
            writeHeader(detailSheet, headerStyle, 0, "序列码", "推荐类别", "候选人", "编号", "单位", "部门", "岗位", "投票情况");
            int rowNo = 1;
            for (Vote detail : detailByDepartment.getOrDefault(department, new ArrayList<>()))
            {
                Row row = detailSheet.createRow(rowNo++);
                cell(row, 0, voteSequenceCode(detail));
                cell(row, 1, recommendationCategory(detail));
                cell(row, 2, detail.getCandidateName());
                cell(row, 3, detail.getImportSeq());
                cell(row, 4, detail.getCompany());
                cell(row, 5, candidateDepartmentLevel(detail));
                cell(row, 6, detail.getPosition());
                cell(row, 7, voteResultText(detail.getResult()));
            }
            autosize(detailSheet, 8);
            sheetNo++;
        }
    }

    private List<Map<String, Object>> voteSummaryRows(Long activityId, List<ResultAgg> rows)
    {
        Map<String, VoterGroupStats> grouped = loadVoterGroupStats(activityId, rows);
        List<Map<String, Object>> result = new ArrayList<>();
        for (VoterGroupStats stats : grouped.values())
        {
            Map<String, Object> row = new LinkedHashMap<>();
            int pending = Math.max(0, stats.total - stats.done);
            row.put("subjectName", stats.department);
            row.put("total", stats.total);
            row.put("notVoted", pending);
            row.put("notSubmitted", pending);
            row.put("voted", stats.done);
            result.add(row);
        }
        result.sort(Comparator
                .comparing((Map<String, Object> row) -> departmentRank(text(row.get("subjectName"))))
                .thenComparing(row -> text(row.get("subjectName"))));
        return result;
    }

    private List<Map<String, Object>> voteDetailRows(List<Vote> details)
    {
        List<Map<String, Object>> result = new ArrayList<>();
        for (Vote detail : details)
        {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("serialCode", voteSequenceCode(detail));
            row.put("recommendCategory", recommendationCategory(detail));
            row.put("candidateName", detail.getCandidateName());
            row.put("importSeq", detail.getImportSeq());
            row.put("company", detail.getCompany());
            row.put("department", candidateDepartmentLevel(detail));
            row.put("position", detail.getPosition());
            row.put("voteResult", voteResultText(detail.getResult()));
            row.put("voterDepartment", detail.getVoterDepartment());
            row.put("voterName", detail.getVoterName());
            result.add(row);
        }
        return result;
    }

    private List<Map<String, Object>> voteDisplayRows(List<ResultAgg> rows)
    {
        List<Map<String, Object>> result = new ArrayList<>();
        for (ResultAgg row : rows)
        {
            Map<String, Object> displayRow = new LinkedHashMap<>();
            displayRow.put("activityCandidateId", row.getActivityCandidateId());
            displayRow.put("candidateName", row.getCandidateName());
            displayRow.put("importSeq", row.getImportSeq());
            displayRow.put("department", row.getDepartment());
            displayRow.put("appliedLevel", row.getAppliedLevel());
            displayRow.put("position", row.getPosition());
            displayRow.put("fixedType", row.getFixedType());
            displayRow.put("passCount", row.getVotePassCount());
            displayRow.put("rejectCount", row.getVoteRejectCount());
            displayRow.put("rankNo", row.getRankNo());
            displayRow.put("displayBucket", voteDisplayBucket(row));
            displayRow.put("voteStatus", voteDisplayStatus(row));
            result.add(displayRow);
        }
        result.sort(Comparator
                .comparingInt((Map<String, Object> row) -> departmentRank(text(row.get("department"))))
                .thenComparing(row -> text(row.get("department")))
                .thenComparingInt(row -> levelRank(text(row.get("appliedLevel"))))
                .thenComparing(row -> text(row.get("appliedLevel")))
                .thenComparingInt(row -> intValue(row.get("displayBucket")))
                .thenComparingInt(row -> intValue(row.get("rankNo")))
                .thenComparingInt(row -> intValue(row.get("importSeq")))
                .thenComparing(row -> text(row.get("candidateName"))));
        return result;
    }

    private int voteDisplayBucket(ResultAgg row)
    {
        if ("PASS".equals(row.getFixedType()))
        {
            return 1;
        }
        if ("VOTE".equals(row.getFixedType()) && "PASS".equals(row.getFinalResult()))
        {
            return 2;
        }
        if ("VOTE".equals(row.getFixedType()) && "REJECT".equals(row.getFinalResult()))
        {
            return 3;
        }
        if ("REJECT".equals(row.getFixedType()))
        {
            return 4;
        }
        return 99;
    }

    private String voteDisplayStatus(ResultAgg row)
    {
        if ("PASS".equals(row.getFixedType()))
        {
            return "固定通过";
        }
        if ("REJECT".equals(row.getFixedType()))
        {
            return "固定淘汰";
        }
        return "推荐 " + intValue(row.getVotePassCount()) + " 票 / 淘汰 " + intValue(row.getVoteRejectCount()) + " 票";
    }

    private List<String> sortedVoteDepartments(List<Map<String, Object>> summaryRows, Map<String, List<Vote>> detailByDepartment)
    {
        LinkedHashSet<String> departments = new LinkedHashSet<>();
        for (Map<String, Object> summary : summaryRows)
        {
            departments.add(text(summary.get("subjectName")));
        }
        departments.addAll(detailByDepartment.keySet());
        List<String> result = new ArrayList<>(departments);
        result.sort(Comparator
                .comparingInt(this::departmentRank)
                .thenComparing(department -> department));
        return result;
    }

    private String voteSequenceCode(Vote detail)
    {
        return StringUtils.defaultIfEmpty(detail.getVoterEmployeeId(),
                detail.getVoterImportSeq() == null ? StringUtils.defaultString(detail.getVoterName())
                        : String.valueOf(detail.getVoterImportSeq()));
    }

    private String recommendationCategory(Vote detail)
    {
        return candidateDepartmentLevel(detail) + "(" + StringUtils.defaultString(detail.getVoterDepartment()) + ")";
    }

    private String candidateDepartmentLevel(Vote detail)
    {
        return StringUtils.defaultString(detail.getDepartment()) + "（" + StringUtils.defaultString(detail.getAppliedLevel()) + "）";
    }

    private String voteResultText(String result)
    {
        if ("PASS".equals(result))
        {
            return "推荐";
        }
        if ("REJECT".equals(result))
        {
            return "不推荐";
        }
        return StringUtils.defaultString(result);
    }

    private void writeFigure3(XSSFWorkbook workbook, CellStyle headerStyle, List<ResultAgg> rows, RuleConfig ruleConfig)
    {
        Sheet sheet = workbook.createSheet("图3_统计结果");
        writeHeader(sheet, headerStyle, 0, "序号", "二级部门", "申报职称", "候选人数",
                "最多通过人数", "最终通过人数", "最终不通过人数");
        Map<String, GroupStats> grouped = new LinkedHashMap<>();
        for (ResultAgg row : rows)
        {
            String key = text(row.getDepartment()) + "|" + text(row.getAppliedLevel());
            GroupStats stats = grouped.computeIfAbsent(key, unused -> new GroupStats(row.getDepartment(), row.getAppliedLevel()));
            stats.candidateCount++;
            if ("PASS".equals(row.getFinalResult()))
            {
                stats.finalPass++;
            }
            else if ("REJECT".equals(row.getFinalResult()))
            {
                stats.finalReject++;
            }
        }

        int rowNo = 1;
        BigDecimal passRatio = ruleConfig == null ? null : ruleConfig.getPassRatio();
        for (GroupStats stats : grouped.values())
        {
            Row row = sheet.createRow(rowNo);
            cell(row, 0, rowNo);
            cell(row, 1, stats.department);
            cell(row, 2, stats.appliedLevel);
            cell(row, 3, stats.candidateCount);
            cell(row, 4, isAutoPassLevel(stats.appliedLevel) ? stats.candidateCount : ratioCount(stats.candidateCount, passRatio));
            cell(row, 5, stats.finalPass);
            cell(row, 6, stats.finalReject);
            rowNo++;
        }

        int objectStart = rowNo + 2;
        writeHeader(sheet, headerStyle, objectStart, "序号", "二级部门", "三级部门", "申报职称", "排序", "姓名",
                "推荐票", "不推荐票", "推荐率", "不推荐率", "投票排名", "最终结果");
        int detailRowNo = objectStart + 1;
        int no = 1;
        for (ResultAgg result : rows)
        {
            Row row = sheet.createRow(detailRowNo);
            cell(row, 0, no);
            cell(row, 1, result.getDepartment());
            cell(row, 2, result.getThirdLevelDepartment());
            cell(row, 3, result.getAppliedLevel());
            cell(row, 4, result.getImportSeq());
            cell(row, 5, result.getCandidateName());
            cell(row, 6, result.getVotePassCount());
            cell(row, 7, result.getVoteRejectCount());
            cell(row, 8, result.getPassRate());
            cell(row, 9, result.getRejectRate());
            cell(row, 10, result.getRankNo());
            cell(row, 11, result.getFinalResult());
            detailRowNo++;
            no++;
        }
        autosize(sheet, 12);
    }

    private void writeFigure4(XSSFWorkbook workbook, CellStyle headerStyle, List<ResultAgg> rows)
    {
        Sheet sheet = workbook.createSheet("图4_通过表决");
        writeHeader(sheet, headerStyle, 0, "序号", "二级部门", "三级部门", "申报职称", "排序", "姓名",
                "单位", "岗位", "推荐票", "不推荐票", "投票排名");
        int rowNo = 1;
        for (ResultAgg result : rows)
        {
            if (!"PASS".equals(result.getFinalResult()))
            {
                continue;
            }
            Row row = sheet.createRow(rowNo);
            cell(row, 0, rowNo);
            cell(row, 1, result.getDepartment());
            cell(row, 2, result.getThirdLevelDepartment());
            cell(row, 3, result.getAppliedLevel());
            cell(row, 4, result.getImportSeq());
            cell(row, 5, result.getCandidateName());
            cell(row, 6, result.getCompany());
            cell(row, 7, result.getPosition());
            cell(row, 8, result.getVotePassCount());
            cell(row, 9, result.getVoteRejectCount());
            cell(row, 10, result.getRankNo());
            rowNo++;
        }
        autosize(sheet, 11);
    }

    private void writeFinalDecision(XSSFWorkbook workbook, CellStyle headerStyle, List<ResultAgg> rows)
    {
        Sheet sheet = workbook.createSheet("最终总榜单");
        writeHeader(sheet, headerStyle, 0, "序号", "二级部门", "评审专业", "申报职称", "排序", "姓名",
                "身份证号", "单位", "岗位", "范围类型", "推荐票", "不推荐票", "总票数",
                "推荐率", "不推荐率", "投票排名", "最终结果");
        int rowNo = 1;
        int no = 1;
        for (ResultAgg result : finalDecisionRows(rows))
        {
            Row row = sheet.createRow(rowNo);
            cell(row, 0, no);
            cell(row, 1, result.getDepartment());
            cell(row, 2, result.getThirdLevelDepartment());
            cell(row, 3, result.getAppliedLevel());
            cell(row, 4, result.getImportSeq());
            cell(row, 5, result.getCandidateName());
            cell(row, 6, maskIdCard(result.getIdCard()));
            cell(row, 7, result.getCompany());
            cell(row, 8, result.getPosition());
            cell(row, 9, result.getFixedType());
            cell(row, 10, result.getVotePassCount());
            cell(row, 11, result.getVoteRejectCount());
            cell(row, 12, result.getTotalVotes());
            cell(row, 13, result.getPassRate());
            cell(row, 14, result.getRejectRate());
            cell(row, 15, result.getRankNo());
            cell(row, 16, result.getFinalResult());
            rowNo++;
            no++;
        }
        autosize(sheet, 17);
    }

    private List<ResultAgg> finalDecisionRows(List<ResultAgg> rows)
    {
        return exportRows(rows);
    }

    private List<ResultAgg> exportRows(List<ResultAgg> rows)
    {
        return rows.stream()
                .sorted(Comparator
                        .comparingInt((ResultAgg row) -> resultBucket(row))
                        .thenComparingInt(row -> departmentRank(text(row.getDepartment())))
                        .thenComparing(row -> text(row.getDepartment()))
                        .thenComparingInt(row -> levelRank(text(row.getAppliedLevel())))
                        .thenComparing(row -> text(row.getAppliedLevel()))
                        .thenComparing(Comparator.comparingInt((ResultAgg row) -> intValue(row.getVotePassCount())).reversed())
                        .thenComparingInt(row -> intValue(row.getVoteRejectCount()))
                        .thenComparing(row -> row.getImportSeq(), Comparator.nullsLast(Integer::compareTo))
                        .thenComparing(row -> row.getActivityCandidateId(), Comparator.nullsLast(Long::compareTo)))
                .collect(Collectors.toList());
    }

    private int resultBucket(ResultAgg row)
    {
        String finalResult = row == null ? null : row.getFinalResult();
        if ("PASS".equals(finalResult)) return 1;
        if ("REJECT".equals(finalResult)) return 2;
        return 3;
    }

    private void writeDepartmentZip(File target, List<ResultAgg> rows) throws Exception
    {
        Map<String, List<ResultAgg>> byDepartment = rows.stream()
                .collect(Collectors.groupingBy(row -> text(row.getDepartment()), LinkedHashMap::new, Collectors.toList()));
        try (ZipOutputStream zip = new ZipOutputStream(new FileOutputStream(target), StandardCharsets.UTF_8))
        {
            for (Map.Entry<String, List<ResultAgg>> entry : byDepartment.entrySet())
            {
                String department = StringUtils.defaultIfEmpty(entry.getKey(), "未分组");
                byte[] workbookBytes = departmentWorkbook(department, entry.getValue());
                ZipEntry zipEntry = new ZipEntry(safeFileName(department) + ".xlsx");
                zip.putNextEntry(zipEntry);
                zip.write(workbookBytes);
                zip.closeEntry();
            }
        }
    }

    private byte[] departmentWorkbook(String department, List<ResultAgg> rows) throws Exception
    {
        try (XSSFWorkbook workbook = new XSSFWorkbook(); ByteArrayOutputStream output = new ByteArrayOutputStream())
        {
            CellStyle headerStyle = headerStyle(workbook);
            CellStyle titleStyle = titleStyle(workbook);
            Map<String, List<ResultAgg>> byLevel = rows.stream()
                    .collect(Collectors.groupingBy(row -> text(row.getAppliedLevel()), LinkedHashMap::new, Collectors.toList()));
            for (Map.Entry<String, List<ResultAgg>> entry : byLevel.entrySet())
            {
                String level = StringUtils.defaultIfEmpty(entry.getKey(), "未分级");
                writeDepartmentLevelSheet(workbook, titleStyle, headerStyle, safeSheetName(level), department, level, entry.getValue());
            }
            workbook.write(output);
            return output.toByteArray();
        }
    }

    private void writeDepartmentLevelSheet(XSSFWorkbook workbook, CellStyle titleStyle, CellStyle headerStyle,
            String sheetName, String department, String level, List<ResultAgg> rows)
    {
        Sheet sheet = workbook.createSheet(sheetName);
        Row title = sheet.createRow(0);
        Cell titleCell = title.createCell(0);
        titleCell.setCellValue(department + "统分表--" + level);
        titleCell.setCellStyle(titleStyle);
        sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 7));
        writeHeader(sheet, headerStyle, 1, "排序", "单位", "身份证号", "姓名", "申报职称", "评审专业", "评价结果", "评审结果");

        int rowNo = 2;
        int no = 1;
        for (ResultAgg result : rows)
        {
            Row row = sheet.createRow(rowNo);
            cell(row, 0, no);
            cell(row, 1, result.getCompany());
            cell(row, 2, "");
            cell(row, 3, result.getCandidateName());
            cell(row, 4, result.getAppliedLevel());
            cell(row, 5, result.getThirdLevelDepartment());
            cell(row, 6, result.getPassRate() == null ? null : result.getPassRate().multiply(BigDecimal.valueOf(100)).setScale(2, RoundingMode.HALF_UP));
            cell(row, 7, result.getFinalResult());
            rowNo++;
            no++;
        }
        autosize(sheet, 8);
    }

    private CellStyle headerStyle(XSSFWorkbook workbook)
    {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        style.setFont(font);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        return style;
    }

    private CellStyle titleStyle(XSSFWorkbook workbook)
    {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        font.setFontHeightInPoints((short) 14);
        style.setFont(font);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        return style;
    }

    private void writeHeader(Sheet sheet, CellStyle style, int rowNo, String... headers)
    {
        Row row = sheet.createRow(rowNo);
        for (int i = 0; i < headers.length; i++)
        {
            Cell cell = row.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(style);
        }
    }

    private void cell(Row row, int column, Object value)
    {
        Cell cell = row.createCell(column);
        if (value == null)
        {
            cell.setBlank();
        }
        else if (value instanceof Number)
        {
            cell.setCellValue(((Number) value).doubleValue());
        }
        else
        {
            cell.setCellValue(String.valueOf(value));
        }
    }

    private void autosize(Sheet sheet, int columns)
    {
        for (int i = 0; i < columns; i++)
        {
            sheet.autoSizeColumn(i);
            if (sheet.getColumnWidth(i) > 12000)
            {
                sheet.setColumnWidth(i, 12000);
            }
        }
    }

    private String normalizeExportType(String exportType)
    {
        String type = StringUtils.defaultIfEmpty(exportType, TYPE_STAT_RESULT).trim().toUpperCase();
        if ("FIGURE3".equals(type) || "GROUP_SUMMARY".equals(type))
        {
            return TYPE_STAT_RESULT;
        }
        if ("FIGURE2".equals(type))
        {
            return TYPE_VOTE_SUMMARY;
        }
        if ("FIGURE4".equals(type) || "FINAL_PASS".equals(type))
        {
            return TYPE_PASS_DECISION;
        }
        if ("FINAL".equals(type) || "FINAL_DECISION".equals(type) || "FIGURE5".equals(type) || "FINAL_EVALUATION".equals(type))
        {
            return TYPE_FINAL_DECISION;
        }
        if (!TYPE_STAT_RESULT.equals(type) && !TYPE_VOTE_SUMMARY.equals(type) && !TYPE_PASS_DECISION.equals(type)
                && !TYPE_FINAL_DECISION.equals(type))
        {
            throw new ServiceException("Unsupported export type: " + exportType);
        }
        return type;
    }

    private String exportFileName(Long activityId, String type)
    {
        String extension = TYPE_STAT_RESULT.equals(type) ? ".zip" : ".xlsx";
        return "evaluation_result_" + activityId + "_" + type + "_" + DateUtils.dateTimeNow() + extension;
    }

    private String downloadUrl(String relativeFileName)
    {
        return "/common/download?fileName=" + URLEncoder.encode(relativeFileName, StandardCharsets.UTF_8) + "&delete=false";
    }

    private void markExported(Long activityId, String type, String username)
    {
        if (!TYPE_FINAL_DECISION.equals(type))
        {
            return;
        }
        Activity activity = new Activity();
        activity.setId(activityId);
        activity.setStatus("EXPORTED");
        activity.setUpdatedBy(username);
        activityMapper.updateActivity(activity);
    }

    private void applyConfirmedFinalEvaluation(Long activityId, List<ResultAgg> rows)
    {
        FinalEvaluation query = new FinalEvaluation();
        query.setActivityId(activityId);
        List<FinalEvaluation> finalRows = finalEvaluationMapper.selectFinalEvaluationList(query);
        if (finalRows.isEmpty() || finalRows.stream().anyMatch(row -> !"CONFIRMED".equals(row.getConfirmStatus())))
        {
            throw new ServiceException("Please confirm FinalEvaluation before exporting the final decision.");
        }
        Map<Long, String> finalResultByCandidateId = finalRows.stream()
                .collect(Collectors.toMap(FinalEvaluation::getActivityCandidateId, FinalEvaluation::getFinalResult));
        for (ResultAgg row : rows)
        {
            String finalResult = finalResultByCandidateId.get(row.getActivityCandidateId());
            if (finalResult == null)
            {
                throw new ServiceException("FinalEvaluation is incomplete for the current activity.");
            }
            row.setFinalResult(finalResult);
        }
    }

    private Comparator<ResultRow> voteRankComparator()
    {
        return Comparator.comparingInt((ResultRow row) -> row.pass).reversed()
                .thenComparingInt(row -> row.reject)
                .thenComparing(row -> row.candidate.getImportSeq(), Comparator.nullsLast(Integer::compareTo))
                .thenComparing(row -> row.candidate.getId(), Comparator.nullsLast(Long::compareTo));
    }

    private String groupKey(ResultRow row)
    {
        return text(row.candidate.getDepartment()) + "|" + text(row.candidate.getAppliedLevel());
    }

    private boolean isAutoPassLevel(String level)
    {
        return EvaluationTitleUtils.isAutoPassLevel(level);
    }

    private int ratioCount(int total, BigDecimal ratio)
    {
        if (total <= 0 || ratio == null)
        {
            return 0;
        }
        return ratio.multiply(BigDecimal.valueOf(total)).divide(BigDecimal.valueOf(100), 0, RoundingMode.HALF_UP).intValue();
    }

    private BigDecimal rate(int count, int total)
    {
        if (total <= 0)
        {
            return BigDecimal.ZERO;
        }
        return BigDecimal.valueOf(count).divide(BigDecimal.valueOf(total), 4, RoundingMode.HALF_UP);
    }

    private String text(String value)
    {
        return StringUtils.defaultString(value);
    }

    private String text(Object value)
    {
        return value == null ? "" : String.valueOf(value);
    }

    private String limit(String message)
    {
        String value = StringUtils.defaultString(message);
        return value.length() > 1000 ? value.substring(0, 1000) : value;
    }

    private String formatDateTime(Date date)
    {
        if (date == null)
        {
            return null;
        }
        return date.toInstant().atZone(ZONE_SHANGHAI).format(DATE_TIME_FORMATTER);
    }

    private String maskIdCard(String idCard)
    {
        String value = StringUtils.defaultString(idCard).trim();
        if (value.length() <= 8)
        {
            return value;
        }
        return value.substring(0, 4) + "**********" + value.substring(value.length() - 4);
    }

    private String safeFileName(String value)
    {
        String name = StringUtils.defaultIfEmpty(value, "未命名");
        return name.replaceAll("[\\\\/:*?\"<>|]", "_");
    }

    private String safeSheetName(String value)
    {
        String name = StringUtils.defaultIfEmpty(value, "Sheet");
        name = name.replaceAll("[\\\\/?*\\[\\]:]", "_");
        return name.length() > 31 ? name.substring(0, 31) : name;
    }

    private Map<String, VoterGroupStats> loadVoterGroupStats(Long activityId, List<ResultAgg> rows)
    {
        Map<String, VoterGroupStats> stats = new LinkedHashMap<>();
        for (ResultAgg row : rows)
        {
            String key = text(row.getDepartment());
            stats.computeIfAbsent(key, unused -> new VoterGroupStats(key));
        }
        ActivityVoter query = new ActivityVoter();
        query.setActivityId(activityId);
        List<ActivityVoter> voters = activityVoterMapper.selectActivityVoterList(query);
        for (ActivityVoter voter : voters)
        {
            String key = text(voter.getDepartment());
            VoterGroupStats row = stats.computeIfAbsent(key, unused -> new VoterGroupStats(key));
            row.total++;
            if ("DONE".equals(voter.getStatus()))
            {
                row.done++;
            }
        }
        return stats;
    }

    private static class GroupSummary
    {
        private final String department;
        private final String appliedLevel;
        private int candidateCount;
        private int passCount;
        private int rejectCount;
        private final Set<String> thirdLevelDepartments = new LinkedHashSet<>();
        private Date calculatedAt;

        private GroupSummary(String department, String appliedLevel)
        {
            this.department = department;
            this.appliedLevel = appliedLevel;
        }
    }

    private static class VoteCounts
    {
        private int pass;
        private int reject;
    }

    private static class ResultRow
    {
        private ActivityCandidate candidate;
        private int pass;
        private int reject;
        private Integer rankNo;
        private String finalResult;
    }

    private static class GroupStats
    {
        private final String department;
        private final String appliedLevel;
        private int candidateCount;
        private int finalPass;
        private int finalReject;

        private GroupStats(String department, String appliedLevel)
        {
            this.department = department;
            this.appliedLevel = appliedLevel;
        }
    }

    private static class VoterGroupStats
    {
        private final String department;
        private int total;
        private int done;

        private VoterGroupStats(String department)
        {
            this.department = department;
        }
    }
}
