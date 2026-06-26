package com.ruoyi.evaluation.domain;

import java.math.BigDecimal;
import com.ruoyi.common.core.domain.BaseEntity;

public class RuleConfig extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    private Long id;
    private Long activityId;
    private BigDecimal passRatio;
    private BigDecimal rejectRatio;
    private String voteType;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getActivityId() { return activityId; }
    public void setActivityId(Long activityId) { this.activityId = activityId; }
    public BigDecimal getPassRatio() { return passRatio; }
    public void setPassRatio(BigDecimal passRatio) { this.passRatio = passRatio; }
    public BigDecimal getRejectRatio() { return rejectRatio; }
    public void setRejectRatio(BigDecimal rejectRatio) { this.rejectRatio = rejectRatio; }
    public String getVoteType() { return voteType; }
    public void setVoteType(String voteType) { this.voteType = voteType; }
}
