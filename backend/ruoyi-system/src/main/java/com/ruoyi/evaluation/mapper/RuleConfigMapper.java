package com.ruoyi.evaluation.mapper;

import java.util.List;
import com.ruoyi.evaluation.domain.RuleConfig;

public interface RuleConfigMapper
{
    List<RuleConfig> selectRuleConfigList(RuleConfig ruleConfig);
    RuleConfig selectRuleConfigById(Long id);
    RuleConfig selectRuleConfigByActivityId(Long activityId);
    int insertRuleConfig(RuleConfig ruleConfig);
    int updateRuleConfig(RuleConfig ruleConfig);
    int deleteRuleConfigById(Long id);
    int deleteRuleConfigByIds(Long[] ids);
    int deleteRuleConfigByActivityId(Long activityId);
}
