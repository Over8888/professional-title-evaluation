package com.ruoyi.evaluation.domain;

import java.util.Date;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.ruoyi.common.annotation.Excel;
import com.ruoyi.common.core.domain.BaseEntity;

public class Voter extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    private Long id;
    @Excel(name = "评委编号")
    private Integer importSeq;
    private Long activityId;
    private Long voterId;
    private String sourceType;
    private Long sourceActivityId;
    private Long sourceActivityVoterId;
    @Excel(name = "姓名")
    private String name;
    @Excel(name = "工号")
    private String employeeId;
    @Excel(name = "部门")
    private String department;
    private String status;
    private String voteToken;
    private Boolean pool;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date submittedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Integer getImportSeq() { return importSeq; }
    public void setImportSeq(Integer importSeq) { this.importSeq = importSeq; }
    public Long getActivityId() { return activityId; }
    public void setActivityId(Long activityId) { this.activityId = activityId; }
    public Long getVoterId() { return voterId; }
    public void setVoterId(Long voterId) { this.voterId = voterId; }
    public String getSourceType() { return sourceType; }
    public void setSourceType(String sourceType) { this.sourceType = sourceType; }
    public Long getSourceActivityId() { return sourceActivityId; }
    public void setSourceActivityId(Long sourceActivityId) { this.sourceActivityId = sourceActivityId; }
    public Long getSourceActivityVoterId() { return sourceActivityVoterId; }
    public void setSourceActivityVoterId(Long sourceActivityVoterId) { this.sourceActivityVoterId = sourceActivityVoterId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getEmployeeId() { return employeeId; }
    public void setEmployeeId(String employeeId) { this.employeeId = employeeId; }
    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getVoteToken() { return voteToken; }
    public void setVoteToken(String voteToken) { this.voteToken = voteToken; }
    public Boolean getPool() { return pool; }
    public void setPool(Boolean pool) { this.pool = pool; }
    public Date getSubmittedAt() { return submittedAt; }
    public void setSubmittedAt(Date submittedAt) { this.submittedAt = submittedAt; }
}
