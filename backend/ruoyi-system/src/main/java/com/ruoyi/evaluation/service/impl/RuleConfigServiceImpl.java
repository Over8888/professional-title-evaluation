package com.ruoyi.evaluation.service.impl;

import java.util.ArrayList;
import java.math.BigDecimal;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.evaluation.domain.ActivityRangeSetting;
import com.ruoyi.evaluation.domain.ActivityVoter;
import com.ruoyi.evaluation.domain.Candidate;
import com.ruoyi.evaluation.domain.RuleConfig;
import com.ruoyi.evaluation.mapper.ActivityCandidateMapper;
import com.ruoyi.evaluation.mapper.ActivityRangeSettingMapper;
import com.ruoyi.evaluation.mapper.ActivityVoterMapper;
import com.ruoyi.evaluation.mapper.RuleConfigMapper;
import com.ruoyi.evaluation.service.IRuleConfigService;

@Service
public class RuleConfigServiceImpl implements IRuleConfigService
{
    @Autowired
    private RuleConfigMapper ruleConfigMapper;

    @Autowired
    private ActivityCandidateMapper activityCandidateMapper;

    @Autowired
    private ActivityRangeSettingMapper activityRangeSettingMapper;

    @Autowired
    private ActivityVoterMapper activityVoterMapper;

    @Override
    public List<RuleConfig> selectRuleConfigList(RuleConfig ruleConfig) { return ruleConfigMapper.selectRuleConfigList(ruleConfig); }
    @Override
    public RuleConfig selectRuleConfigById(Long id) { return ruleConfigMapper.selectRuleConfigById(id); }
    @Override
    public RuleConfig selectRuleConfigByActivityId(Long activityId) { return ruleConfigMapper.selectRuleConfigByActivityId(activityId); }
    @Override
    public int insertRuleConfig(RuleConfig ruleConfig)
    {
        requireActivityId(ruleConfig == null ? null : ruleConfig.getActivityId());
        validateRatio(ruleConfig);
        ruleConfig.setVoteType("PASS_REJECT");
        RuleConfig existing = ruleConfigMapper.selectRuleConfigByActivityId(ruleConfig.getActivityId());
        if (existing != null)
        {
            ruleConfig.setId(existing.getId());
            ruleConfig.setUpdateBy(ruleConfig.getCreateBy());
            return ruleConfigMapper.updateRuleConfig(ruleConfig);
        }
        return ruleConfigMapper.insertRuleConfig(ruleConfig);
    }
    @Override
    public int updateRuleConfig(RuleConfig ruleConfig)
    {
        validateRatio(ruleConfig);
        ruleConfig.setVoteType("PASS_REJECT");
        return ruleConfigMapper.updateRuleConfig(ruleConfig);
    }
    @Override
    public int deleteRuleConfigByIds(Long[] ids) { return ruleConfigMapper.deleteRuleConfigByIds(ids); }
    @Override
    public int deleteRuleConfigById(Long id) { return ruleConfigMapper.deleteRuleConfigById(id); }

    @Override
    public List<Map<String, Object>> previewCandidateRange(Long activityId)
    {
        requireActivityId(activityId);
        RuleConfig ruleConfig = ruleConfigMapper.selectRuleConfigByActivityId(activityId);
        Candidate query = new Candidate();
        query.setActivityId(activityId);
        List<Candidate> candidates = activityCandidateMapper.selectCandidateSnapshotList(query);
        List<Map<String, Object>> rows = new ArrayList<>();
        if (candidates.isEmpty())
        {
            return rows;
        }
        sortCandidatesByRange(candidates);
        Map<String, Integer> voterCountByDepartment = voterCountByDepartment(activityId);
        Map<String, ActivityRangeSetting> settingByGroup = rangeSettingByGroup(activityId);
        Map<String, List<Candidate>> grouped = candidates.stream()
                .collect(Collectors.groupingBy(this::rangeGroupKey, LinkedHashMap::new, Collectors.toList()));
        for (Map.Entry<String, List<Candidate>> entry : grouped.entrySet())
        {
            sortCandidates(entry.getValue());
            int candidateCount = entry.getValue().size();
            int maxPassCount = ratioCount(candidateCount, ruleConfig == null ? null : ruleConfig.getPassRatio());
            Map<String, Object> row = buildRangeRow(entry.getKey(), entry.getValue(), maxPassCount,
                    ruleConfig == null ? null : ruleConfig.getPassRatio(),
                    ruleConfig == null ? null : ruleConfig.getRejectRatio(), settingByGroup.get(entry.getKey()));
            row.put("plannedVoterCount", voterCountByDepartment.getOrDefault(stringValue(row, "department"), 0));
            rows.add(row);
        }
        sortRangeRows(rows);
        return rows;
    }

    @Override
    public List<Map<String, Object>> previewCandidateDetailRange(Long activityId)
    {
        requireActivityId(activityId);
        Candidate query = new Candidate();
        query.setActivityId(activityId);
        List<Candidate> candidates = activityCandidateMapper.selectCandidateSnapshotList(query);
        sortCandidatesByRange(candidates);
        List<Map<String, Object>> rows = new ArrayList<>();
        for (Candidate candidate : candidates)
        {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("department", candidate.getDepartment());
            row.put("currentLevel", candidate.getCurrentLevel());
            row.put("appliedLevel", appliedLevel(candidate));
            row.put("importSeq", candidate.getImportSeq());
            row.put("name", candidate.getName());
            row.put("company", candidate.getCompany());
            row.put("fixedType", candidate.getFixedType());
            row.put("source", "已应用范围");
            rows.add(row);
        }
        return rows;
    }

    @Override
    public int applyCandidateRange(Long activityId, String username)
    {
        return applyCandidateRange(activityId, null, username);
    }

    @Override
    @Transactional
    public int applyCandidateRange(Long activityId, List<Map<String, Object>> confirmedRanges, String username)
    {
        requireActivityId(activityId);
        RuleConfig ruleConfig = ruleConfigMapper.selectRuleConfigByActivityId(activityId);
        Candidate query = new Candidate();
        query.setActivityId(activityId);
        List<Candidate> candidates = activityCandidateMapper.selectCandidateSnapshotList(query);
        if (candidates.isEmpty())
        {
            throw new ServiceException("当前活动没有候选人，无法应用规则范围");
        }
        Map<String, List<Candidate>> grouped = candidates.stream()
                .collect(Collectors.groupingBy(this::rangeGroupKey, LinkedHashMap::new, Collectors.toList()));
        Map<String, Map<String, Object>> confirmedByGroup = confirmedRangeMap(confirmedRanges);
        Map<String, ActivityRangeSetting> settingByGroup = rangeSettingByGroup(activityId);
        validateAndSaveRangeSettings(activityId, username, ruleConfig, grouped, confirmedByGroup, settingByGroup);
        int updated = 0;
        for (Map.Entry<String, List<Candidate>> entry : grouped.entrySet())
        {
            List<Candidate> group = entry.getValue();
            sortCandidates(group);
            ActivityRangeSetting setting = settingByGroup.get(entry.getKey());
            int lockedPassCount = setting == null || setting.getLockedPassCount() == null ? 0 : setting.getLockedPassCount();
            int lockedRejectCount = setting == null || setting.getLockedRejectCount() == null ? 0 : setting.getLockedRejectCount();
            for (int i = 0; i < group.size(); i++)
            {
                Candidate candidate = group.get(i);
                if (i < lockedPassCount)
                {
                    candidate.setFixedType("PASS");
                }
                else if (i >= group.size() - lockedRejectCount)
                {
                    candidate.setFixedType("REJECT");
                }
                else
                {
                    candidate.setFixedType("VOTE");
                }
                candidate.setUpdateBy(username);
                updated += activityCandidateMapper.updateCandidateSnapshot(candidate);
            }
        }
        return updated;
    }

    private Map<String, Object> buildRangeRow(String groupKey, List<Candidate> candidates, int maxPassCount,
            BigDecimal passRatio, BigDecimal rejectRatio, ActivityRangeSetting setting)
    {
        String[] keys = groupKey.split("\\|", -1);
        int candidateCount = candidates.size();
        int maxPass = Math.max(0, Math.min(maxPassCount, candidateCount));
        RangeCounts counts = resolveRangeCounts(null, setting, maxPass, candidateCount, rejectRatio);
        int voteCount = Math.max(0, candidateCount - counts.lockedPassCount - counts.lockedRejectCount);
        int voteRejectCount = Math.max(0, maxPass - counts.lockedPassCount - counts.lockedRejectCount);
        int voteEnd = candidateCount - counts.lockedRejectCount;
        String passRange = formatOrdinalRange(1, counts.lockedPassCount);
        String voteRange = formatOrdinalRange(counts.lockedPassCount + 1, voteEnd);
        String rejectRange = formatOrdinalRange(voteEnd + 1, candidateCount);
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("groupKey", groupKey);
        row.put("department", keys.length > 0 ? keys[0] : "");
        row.put("appliedLevel", keys.length > 1 ? keys[1] : "");
        row.put("candidateCount", candidateCount);
        row.put("total", candidateCount);
        row.put("passRatio", passRatio);
        row.put("maxPassCount", maxPass);
        row.put("minRejectCount", 0);
        row.put("fixedPassCount", counts.lockedPassCount);
        row.put("lockedPassCount", counts.lockedPassCount);
        row.put("voteCount", voteCount);
        row.put("minVoteRejectCount", voteRejectCount);
        row.put("fixedRejectCount", counts.lockedRejectCount);
        row.put("lockedRejectCount", counts.lockedRejectCount);
        row.put("confirmedPassRange", passRange);
        row.put("fixedPassRange", passRange);
        row.put("voteRange", voteRange);
        row.put("confirmedRejectRange", rejectRange);
        row.put("fixedRejectRange", rejectRange);
        row.put("status", setting == null ? "PENDING_CONFIRM" : "READY");
        return row;
    }

    private Map<String, Map<String, Object>> confirmedRangeMap(List<Map<String, Object>> confirmedRanges)
    {
        Map<String, Map<String, Object>> confirmedByGroup = new LinkedHashMap<>();
        if (confirmedRanges == null || confirmedRanges.isEmpty())
        {
            return confirmedByGroup;
        }
        for (Map<String, Object> row : confirmedRanges)
        {
            if (row == null)
            {
                continue;
            }
            String groupKey = StringUtils.defaultString((String) row.get("groupKey"));
            if (StringUtils.isEmpty(groupKey))
            {
                groupKey = StringUtils.defaultString((String) row.get("department")) + "|"
                        + StringUtils.defaultString((String) row.get("appliedLevel"));
            }
            confirmedByGroup.put(groupKey, row);
        }
        return confirmedByGroup;
    }

    private void validateAndSaveRangeSettings(Long activityId, String username, RuleConfig ruleConfig,
            Map<String, List<Candidate>> grouped, Map<String, Map<String, Object>> confirmedByGroup,
            Map<String, ActivityRangeSetting> settingByGroup)
    {
        for (Map.Entry<String, List<Candidate>> entry : grouped.entrySet())
        {
            Map<String, Object> row = confirmedByGroup.get(entry.getKey());
            int candidateCount = entry.getValue().size();
            int maxPass = ratioCount(candidateCount, ruleConfig == null ? null : ruleConfig.getPassRatio());
            RangeCounts counts = resolveRangeCounts(row, settingByGroup.get(entry.getKey()), maxPass, candidateCount,
                    ruleConfig == null ? null : ruleConfig.getRejectRatio());
            validateRangeCounts(counts, maxPass, candidateCount);
            saveRangeSetting(activityId, entry.getKey(), counts, username);
            settingByGroup.put(entry.getKey(), toSetting(activityId, entry.getKey(), counts));
        }
    }

    private void validateRangeCounts(RangeCounts counts, int maxPass, int candidateCount)
    {
        if (counts.lockedPassCount < 0 || counts.lockedRejectCount < 0)
        {
            throw new ServiceException("锁定通过人数和锁定不通过人数不能小于 0");
        }
        if (counts.lockedPassCount > maxPass)
        {
            throw new ServiceException("锁定通过人数不能大于最多通过人数");
        }
        if (counts.lockedPassCount + counts.lockedRejectCount > candidateCount)
        {
            throw new ServiceException("锁定通过人数和锁定不通过人数不能大于候选人数");
        }
        if (candidateCount > 0 && counts.lockedPassCount + counts.lockedRejectCount >= candidateCount)
        {
            throw new ServiceException("至少保留 1 名候选人进入投票范围");
        }
    }

    private RangeCounts resolveRangeCounts(Map<String, Object> row, ActivityRangeSetting setting, int maxPass,
            int candidateCount, BigDecimal rejectRatio)
    {
        int passCount = Math.min(maxPass, Math.round(maxPass * 0.6f));
        int rejectCount = ratioCount(candidateCount, rejectRatio);
        if (setting != null)
        {
            passCount = setting.getLockedPassCount() == null ? passCount : setting.getLockedPassCount();
            rejectCount = setting.getLockedRejectCount() == null ? rejectCount : setting.getLockedRejectCount();
        }
        if (row != null)
        {
            passCount = parseRangeInteger(firstPresent(row.get("lockedPassCount"), row.get("fixedPassCount")), "锁定通过人数");
            rejectCount = parseRangeInteger(firstPresent(row.get("lockedRejectCount"), row.get("fixedRejectCount")), "锁定不通过人数");
            return new RangeCounts(passCount, rejectCount);
        }
        passCount = clamp(passCount, 0, maxPass);
        rejectCount = clamp(rejectCount, 0, candidateCount);
        if (passCount + rejectCount >= candidateCount)
        {
            rejectCount = Math.max(0, candidateCount - passCount - 1);
        }
        return new RangeCounts(passCount, rejectCount);
    }

    private void saveRangeSetting(Long activityId, String groupKey, RangeCounts counts, String username)
    {
        String[] keys = groupKey.split("\\|", -1);
        ActivityRangeSetting setting = new ActivityRangeSetting();
        setting.setActivityId(activityId);
        setting.setDepartment(keys.length > 0 ? keys[0] : "");
        setting.setAppliedLevel(keys.length > 1 ? keys[1] : "");
        setting.setLockedPassCount(counts.lockedPassCount);
        setting.setLockedRejectCount(counts.lockedRejectCount);
        setting.setCreateBy(username);
        setting.setUpdateBy(username);
        activityRangeSettingMapper.upsertActivityRangeSetting(setting);
    }

    private ActivityRangeSetting toSetting(Long activityId, String groupKey, RangeCounts counts)
    {
        String[] keys = groupKey.split("\\|", -1);
        ActivityRangeSetting setting = new ActivityRangeSetting();
        setting.setActivityId(activityId);
        setting.setDepartment(keys.length > 0 ? keys[0] : "");
        setting.setAppliedLevel(keys.length > 1 ? keys[1] : "");
        setting.setLockedPassCount(counts.lockedPassCount);
        setting.setLockedRejectCount(counts.lockedRejectCount);
        return setting;
    }

    private Map<String, ActivityRangeSetting> rangeSettingByGroup(Long activityId)
    {
        Map<String, ActivityRangeSetting> settingByGroup = new LinkedHashMap<>();
        for (ActivityRangeSetting setting : activityRangeSettingMapper.selectActivityRangeSettingByActivityId(activityId))
        {
            settingByGroup.put(StringUtils.defaultString(setting.getDepartment()) + "|"
                    + StringUtils.defaultString(setting.getAppliedLevel()), setting);
        }
        return settingByGroup;
    }

    private int clamp(int value, int min, int max)
    {
        return Math.max(min, Math.min(value, max));
    }

    private static class RangeCounts
    {
        private final int lockedPassCount;
        private final int lockedRejectCount;

        private RangeCounts(int lockedPassCount, int lockedRejectCount)
        {
            this.lockedPassCount = lockedPassCount;
            this.lockedRejectCount = lockedRejectCount;
        }
    }

    private void validateConfirmedRanges(List<Map<String, Object>> confirmedRanges, Map<String, List<Candidate>> grouped)
    {
        if (confirmedRanges == null || confirmedRanges.isEmpty())
        {
            return;
        }
        Map<String, Map<String, Object>> confirmedByGroup = confirmedRangeMap(confirmedRanges);
        for (Map.Entry<String, List<Candidate>> entry : grouped.entrySet())
        {
            Map<String, Object> row = confirmedByGroup.get(entry.getKey());
            if (row == null)
            {
                continue;
            }
            int candidateCount = entry.getValue().size();
            int maxPass = Math.max(0, Math.min(parseRangeInteger(row.get("maxPassCount"), "最多通过人数"), candidateCount));
            RangeCounts counts = new RangeCounts(
                    parseRangeInteger(firstPresent(row.get("lockedPassCount"), row.get("fixedPassCount")), "锁定通过人数"),
                    parseRangeInteger(firstPresent(row.get("lockedRejectCount"), row.get("fixedRejectCount")), "锁定不通过人数"));
            validateRangeCounts(counts, maxPass, candidateCount);
        }
    }

    private Object firstPresent(Object first, Object second)
    {
        return first == null ? second : first;
    }

    private String rangeGroupKey(Candidate candidate)
    {
        return StringUtils.defaultString(candidate.getDepartment()) + "|" + appliedLevel(candidate);
    }

    private String appliedLevel(Candidate candidate)
    {
        return StringUtils.defaultString(candidate.getAppliedLevel());
    }

    private Map<String, Integer> voterCountByDepartment(Long activityId)
    {
        ActivityVoter query = new ActivityVoter();
        query.setActivityId(activityId);
        Map<String, Integer> counts = new LinkedHashMap<>();
        for (ActivityVoter voter : activityVoterMapper.selectActivityVoterList(query))
        {
            String department = StringUtils.defaultString(voter.getDepartment());
            counts.put(department, counts.getOrDefault(department, 0) + 1);
        }
        return counts;
    }

    private void sortRangeRows(List<Map<String, Object>> rows)
    {
        rows.sort(Comparator
                .comparing((Map<String, Object> row) -> levelRank(stringValue(row, "appliedLevel")))
                .thenComparing(row -> groupRank(stringValue(row, "department")))
                .thenComparing(row -> stringValue(row, "appliedLevel"))
                .thenComparing(row -> stringValue(row, "department")));
    }

    private String stringValue(Map<String, Object> row, String key)
    {
        Object value = row.get(key);
        return value == null ? "" : String.valueOf(value);
    }

    private int levelRank(String level)
    {
        if ("正高级".equals(level) || "正高级工程师".equals(level))
        {
            return 1;
        }
        if ("副高级".equals(level) || "高级工程师".equals(level))
        {
            return 2;
        }
        if ("中级".equals(level) || "工程师".equals(level))
        {
            return 3;
        }
        if ("初级".equals(level) || "助理工程师".equals(level))
        {
            return 4;
        }
        if ("员级".equals(level) || "技术员".equals(level))
        {
            return 5;
        }
        return 99;
    }

    private int groupRank(String department)
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

    private int parseRangeInteger(Object value, String fieldName)
    {
        if (value == null || StringUtils.isEmpty(String.valueOf(value)))
        {
            return 0;
        }
        if (value instanceof Number)
        {
            return ((Number) value).intValue();
        }
        try
        {
            return Integer.parseInt(String.valueOf(value));
        }
        catch (Exception e)
        {
            throw new ServiceException(fieldName + "格式错误");
        }
    }

    private int ratioCount(int total, java.math.BigDecimal ratio)
    {
        if (total <= 0 || ratio == null)
        {
            return 0;
        }
        return ratio.multiply(java.math.BigDecimal.valueOf(total)).divide(java.math.BigDecimal.valueOf(100), 0, java.math.RoundingMode.HALF_UP).intValue();
    }

    private void validateRatio(RuleConfig ruleConfig)
    {
        if (ruleConfig == null)
        {
            throw new ServiceException("规则配置不能为空");
        }
        BigDecimal passRatio = ruleConfig.getPassRatio();
        BigDecimal rejectRatio = ruleConfig.getRejectRatio();
        if (passRatio == null || rejectRatio == null)
        {
            throw new ServiceException("通过比例和淘汰比例不能为空");
        }
        if (passRatio.compareTo(BigDecimal.ZERO) < 0 || rejectRatio.compareTo(BigDecimal.ZERO) < 0)
        {
            throw new ServiceException("通过比例和淘汰比例不能小于 0");
        }
        if (passRatio.compareTo(BigDecimal.valueOf(100)) > 0 || rejectRatio.compareTo(BigDecimal.valueOf(100)) > 0)
        {
            throw new ServiceException("通过比例和淘汰比例不能大于 100");
        }
        if (passRatio.add(rejectRatio).compareTo(BigDecimal.valueOf(100)) > 0)
        {
            throw new ServiceException("通过比例和淘汰比例之和不能大于 100");
        }
    }

    private void requireActivityId(Long activityId)
    {
        if (activityId == null)
        {
            throw new ServiceException("规则操作必须指定活动");
        }
    }

    private String formatOrdinalRange(int start, int end)
    {
        if (end < start)
        {
            return "-";
        }
        return start + "-" + end;
    }

    private void sortCandidates(List<Candidate> candidates)
    {
        candidates.sort(candidateOrderComparator());
    }

    private void sortCandidatesByRange(List<Candidate> candidates)
    {
        candidates.sort(Comparator
                .comparing((Candidate candidate) -> levelRank(appliedLevel(candidate)))
                .thenComparing(candidate -> groupRank(candidate.getDepartment()))
                .thenComparing(candidateOrderComparator()));
    }

    private Comparator<Candidate> candidateOrderComparator()
    {
        return Comparator
                .comparing(Candidate::getImportSeq, Comparator.nullsLast(Integer::compareTo))
                .thenComparing(Candidate::getId, Comparator.nullsLast(Long::compareTo));
    }

}
