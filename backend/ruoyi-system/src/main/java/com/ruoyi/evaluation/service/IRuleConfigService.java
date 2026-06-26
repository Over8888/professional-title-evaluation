package com.ruoyi.evaluation.service;

import java.util.List;
import java.util.Map;
import com.ruoyi.evaluation.domain.RuleConfig;

public interface IRuleConfigService
{
    List<RuleConfig> selectRuleConfigList(RuleConfig ruleConfig);
    RuleConfig selectRuleConfigById(Long id);
    RuleConfig selectRuleConfigByActivityId(Long activityId);
    int insertRuleConfig(RuleConfig ruleConfig);
    int updateRuleConfig(RuleConfig ruleConfig);
    int deleteRuleConfigByIds(Long[] ids);
    int deleteRuleConfigById(Long id);
    List<Map<String, Object>> previewCandidateRange(Long activityId);
    List<Map<String, Object>> previewCandidateDetailRange(Long activityId);
    int applyCandidateRange(Long activityId, String username);
    int applyCandidateRange(Long activityId, List<Map<String, Object>> confirmedRanges, String username);
}
