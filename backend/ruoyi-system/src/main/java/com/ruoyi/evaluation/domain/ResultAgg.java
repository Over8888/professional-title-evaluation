package com.ruoyi.evaluation.domain;

import java.math.BigDecimal;
import java.util.Date;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.ruoyi.common.core.domain.BaseEntity;

public class ResultAgg extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    private Long id;
    private Long activityId;
    private Long activityCandidateId;
    private String statScope;
    private String statKey;
    private Integer votePassCount;
    private Integer voteRejectCount;
    private Integer totalVotes;
    private BigDecimal passRate;
    private BigDecimal rejectRate;
    private Integer rankNo;
    private String finalResult;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date updateAt;
    private String calculatedBy;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date calculatedAt;

    private Integer importSeq;
    private String candidateName;
    private String idCard;
    private String company;
    private String firstLevelDepartment;
    private String department;
    private String thirdLevelDepartment;
    private String position;
    private String currentLevel;
    private String appliedLevel;
    private String fixedType;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getActivityId() { return activityId; }
    public void setActivityId(Long activityId) { this.activityId = activityId; }
    public Long getActivityCandidateId() { return activityCandidateId; }
    public void setActivityCandidateId(Long activityCandidateId) { this.activityCandidateId = activityCandidateId; }
    public String getStatScope() { return statScope; }
    public void setStatScope(String statScope) { this.statScope = statScope; }
    public String getStatKey() { return statKey; }
    public void setStatKey(String statKey) { this.statKey = statKey; }
    public Integer getVotePassCount() { return votePassCount; }
    public void setVotePassCount(Integer votePassCount) { this.votePassCount = votePassCount; }
    public Integer getVoteRejectCount() { return voteRejectCount; }
    public void setVoteRejectCount(Integer voteRejectCount) { this.voteRejectCount = voteRejectCount; }
    public Integer getTotalVotes() { return totalVotes; }
    public void setTotalVotes(Integer totalVotes) { this.totalVotes = totalVotes; }
    public BigDecimal getPassRate() { return passRate; }
    public void setPassRate(BigDecimal passRate) { this.passRate = passRate; }
    public BigDecimal getRejectRate() { return rejectRate; }
    public void setRejectRate(BigDecimal rejectRate) { this.rejectRate = rejectRate; }
    public Integer getRankNo() { return rankNo; }
    public void setRankNo(Integer rankNo) { this.rankNo = rankNo; }
    public String getFinalResult() { return finalResult; }
    public void setFinalResult(String finalResult) { this.finalResult = finalResult; }
    public Date getUpdateAt() { return updateAt; }
    public void setUpdateAt(Date updateAt) { this.updateAt = updateAt; }
    public String getCalculatedBy() { return calculatedBy; }
    public void setCalculatedBy(String calculatedBy) { this.calculatedBy = calculatedBy; }
    public Date getCalculatedAt() { return calculatedAt; }
    public void setCalculatedAt(Date calculatedAt) { this.calculatedAt = calculatedAt; }

    public Integer getImportSeq() { return importSeq; }
    public void setImportSeq(Integer importSeq) { this.importSeq = importSeq; }
    public String getCandidateName() { return candidateName; }
    public void setCandidateName(String candidateName) { this.candidateName = candidateName; }
    public String getName() { return candidateName; }
    public void setName(String name) { this.candidateName = name; }
    public String getIdCard() { return idCard; }
    public void setIdCard(String idCard) { this.idCard = idCard; }
    public String getCompany() { return company; }
    public void setCompany(String company) { this.company = company; }
    public String getFirstLevelDepartment() { return firstLevelDepartment; }
    public void setFirstLevelDepartment(String firstLevelDepartment) { this.firstLevelDepartment = firstLevelDepartment; }
    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }
    public String getThirdLevelDepartment() { return thirdLevelDepartment; }
    public void setThirdLevelDepartment(String thirdLevelDepartment) { this.thirdLevelDepartment = thirdLevelDepartment; }
    public String getPosition() { return position; }
    public void setPosition(String position) { this.position = position; }
    public String getCurrentLevel() { return currentLevel; }
    public void setCurrentLevel(String currentLevel) { this.currentLevel = currentLevel; }
    public String getAppliedLevel() { return appliedLevel; }
    public void setAppliedLevel(String appliedLevel) { this.appliedLevel = appliedLevel; }
    public String getFixedType() { return fixedType; }
    public void setFixedType(String fixedType) { this.fixedType = fixedType; }
}
