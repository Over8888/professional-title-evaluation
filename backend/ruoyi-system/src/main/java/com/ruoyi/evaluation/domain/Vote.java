package com.ruoyi.evaluation.domain;

import java.util.Date;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.ruoyi.common.core.domain.BaseEntity;

public class Vote extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    private Long id;
    private Long activityId;
    private Long activityVoterId;
    private Long activityCandidateId;
    private String result;
    private Integer roundNo;
    private Integer voteCount;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createdAt;

    private String voterName;
    private String voterEmployeeId;
    private Integer voterImportSeq;
    private String voterDepartment;
    private Integer importSeq;
    private String candidateName;
    private String company;
    private String department;
    private String thirdLevelDepartment;
    private String position;
    private String appliedLevel;
    private String fixedType;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getActivityId() { return activityId; }
    public void setActivityId(Long activityId) { this.activityId = activityId; }
    public Long getActivityVoterId() { return activityVoterId; }
    public void setActivityVoterId(Long activityVoterId) { this.activityVoterId = activityVoterId; }
    public Long getActivityCandidateId() { return activityCandidateId; }
    public void setActivityCandidateId(Long activityCandidateId) { this.activityCandidateId = activityCandidateId; }
    public String getResult() { return result; }
    public void setResult(String result) { this.result = result; }
    public Integer getRoundNo() { return roundNo; }
    public void setRoundNo(Integer roundNo) { this.roundNo = roundNo; }
    public Integer getVoteCount() { return voteCount; }
    public void setVoteCount(Integer voteCount) { this.voteCount = voteCount; }
    public Date getCreatedAt() { return createdAt; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }
    public String getVoterName() { return voterName; }
    public void setVoterName(String voterName) { this.voterName = voterName; }
    public String getVoterEmployeeId() { return voterEmployeeId; }
    public void setVoterEmployeeId(String voterEmployeeId) { this.voterEmployeeId = voterEmployeeId; }
    public Integer getVoterImportSeq() { return voterImportSeq; }
    public void setVoterImportSeq(Integer voterImportSeq) { this.voterImportSeq = voterImportSeq; }
    public String getVoterDepartment() { return voterDepartment; }
    public void setVoterDepartment(String voterDepartment) { this.voterDepartment = voterDepartment; }
    public Integer getImportSeq() { return importSeq; }
    public void setImportSeq(Integer importSeq) { this.importSeq = importSeq; }
    public String getCandidateName() { return candidateName; }
    public void setCandidateName(String candidateName) { this.candidateName = candidateName; }
    public String getCompany() { return company; }
    public void setCompany(String company) { this.company = company; }
    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }
    public String getThirdLevelDepartment() { return thirdLevelDepartment; }
    public void setThirdLevelDepartment(String thirdLevelDepartment) { this.thirdLevelDepartment = thirdLevelDepartment; }
    public String getPosition() { return position; }
    public void setPosition(String position) { this.position = position; }
    public String getAppliedLevel() { return appliedLevel; }
    public void setAppliedLevel(String appliedLevel) { this.appliedLevel = appliedLevel; }
    public String getFixedType() { return fixedType; }
    public void setFixedType(String fixedType) { this.fixedType = fixedType; }
}
