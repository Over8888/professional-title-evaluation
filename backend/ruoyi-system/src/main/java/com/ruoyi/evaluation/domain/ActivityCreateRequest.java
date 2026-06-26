package com.ruoyi.evaluation.domain;

import java.io.Serializable;

public class ActivityCreateRequest implements Serializable
{
    private static final long serialVersionUID = 1L;

    private Activity activity;
    private RuleConfig ruleConfig;

    public Activity getActivity() { return activity; }
    public void setActivity(Activity activity) { this.activity = activity; }
    public RuleConfig getRuleConfig() { return ruleConfig; }
    public void setRuleConfig(RuleConfig ruleConfig) { this.ruleConfig = ruleConfig; }
}
