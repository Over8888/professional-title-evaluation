package com.ruoyi.evaluation.domain;

import java.math.BigDecimal;
import com.ruoyi.common.annotation.Excel;

public class RangePreviewRow
{
    @Excel(name = "申报等级")
    private String appliedLevel;
    @Excel(name = "组别/专业")
    private String department;
    @Excel(name = "申报人数")
    private Integer candidateCount;
    @Excel(name = "最高通过比例")
    private BigDecimal passRatio;
    @Excel(name = "最多通过人数")
    private Long maxPassCount;
    @Excel(name = "锁定通过人数")
    private Long fixedPassCount;
    @Excel(name = "确定通过序号范围")
    private String fixedPassRange;
    @Excel(name = "需要投票范围")
    private String voteRange;
    @Excel(name = "投票不推荐人数")
    private Long minVoteRejectCount;
    @Excel(name = "锁定不通过人数")
    private Long fixedRejectCount;
    @Excel(name = "确定不通过序号范围")
    private String fixedRejectRange;
    @Excel(name = "计划评委数")
    private Integer plannedVoterCount;
    @Excel(name = "状态")
    private String status;

    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }
    public String getAppliedLevel() { return appliedLevel; }
    public void setAppliedLevel(String appliedLevel) { this.appliedLevel = appliedLevel; }
    public Integer getCandidateCount() { return candidateCount; }
    public void setCandidateCount(Integer candidateCount) { this.candidateCount = candidateCount; }
    public BigDecimal getPassRatio() { return passRatio; }
    public void setPassRatio(BigDecimal passRatio) { this.passRatio = passRatio; }
    public Long getMaxPassCount() { return maxPassCount; }
    public void setMaxPassCount(Long maxPassCount) { this.maxPassCount = maxPassCount; }
    public Long getFixedPassCount() { return fixedPassCount; }
    public void setFixedPassCount(Long fixedPassCount) { this.fixedPassCount = fixedPassCount; }
    public Long getMinVoteRejectCount() { return minVoteRejectCount; }
    public void setMinVoteRejectCount(Long minVoteRejectCount) { this.minVoteRejectCount = minVoteRejectCount; }
    public Long getFixedRejectCount() { return fixedRejectCount; }
    public void setFixedRejectCount(Long fixedRejectCount) { this.fixedRejectCount = fixedRejectCount; }
    public String getFixedPassRange() { return fixedPassRange; }
    public void setFixedPassRange(String fixedPassRange) { this.fixedPassRange = fixedPassRange; }
    public String getVoteRange() { return voteRange; }
    public void setVoteRange(String voteRange) { this.voteRange = voteRange; }
    public String getFixedRejectRange() { return fixedRejectRange; }
    public void setFixedRejectRange(String fixedRejectRange) { this.fixedRejectRange = fixedRejectRange; }
    public Integer getPlannedVoterCount() { return plannedVoterCount; }
    public void setPlannedVoterCount(Integer plannedVoterCount) { this.plannedVoterCount = plannedVoterCount; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
