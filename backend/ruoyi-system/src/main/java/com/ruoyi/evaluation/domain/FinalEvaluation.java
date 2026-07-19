package com.ruoyi.evaluation.domain;

import java.util.Date;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.ruoyi.common.core.domain.BaseEntity;

public class FinalEvaluation extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    private Long id;
    private Long activityId;
    private Long activityCandidateId;
    private Long resultAggId;
    private String finalResult;
    private String confirmStatus;
    private String confirmedBy;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date confirmedAt;
    private String signedBy;
    private String signatureStatus;
    private String confirmRemark;

    private Integer importSeq;
    private String candidateName;
    private String idCard;
    private String company;
    private String department;
    private String thirdLevelDepartment;
    private String position;
    private String currentLevel;
    private String appliedLevel;
    private String fixedType;
    private Integer votePassCount;
    private Integer voteRejectCount;
    private Integer totalVotes;
    private Integer rankNo;
    private String calculatedResult;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getActivityId() { return activityId; }
    public void setActivityId(Long activityId) { this.activityId = activityId; }
    public Long getActivityCandidateId() { return activityCandidateId; }
    public void setActivityCandidateId(Long activityCandidateId) { this.activityCandidateId = activityCandidateId; }
    public Long getResultAggId() { return resultAggId; }
    public void setResultAggId(Long resultAggId) { this.resultAggId = resultAggId; }
    public String getFinalResult() { return finalResult; }
    public void setFinalResult(String finalResult) { this.finalResult = finalResult; }
    public String getConfirmStatus() { return confirmStatus; }
    public void setConfirmStatus(String confirmStatus) { this.confirmStatus = confirmStatus; }
    public String getConfirmedBy() { return confirmedBy; }
    public void setConfirmedBy(String confirmedBy) { this.confirmedBy = confirmedBy; }
    public Date getConfirmedAt() { return confirmedAt; }
    public void setConfirmedAt(Date confirmedAt) { this.confirmedAt = confirmedAt; }
    public String getSignedBy() { return signedBy; }
    public void setSignedBy(String signedBy) { this.signedBy = signedBy; }
    public String getSignatureStatus() { return signatureStatus; }
    public void setSignatureStatus(String signatureStatus) { this.signatureStatus = signatureStatus; }
    public String getConfirmRemark() { return confirmRemark; }
    public void setConfirmRemark(String confirmRemark) { this.confirmRemark = confirmRemark; }

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
    public Integer getVotePassCount() { return votePassCount; }
    public void setVotePassCount(Integer votePassCount) { this.votePassCount = votePassCount; }
    public Integer getVoteRejectCount() { return voteRejectCount; }
    public void setVoteRejectCount(Integer voteRejectCount) { this.voteRejectCount = voteRejectCount; }
    public Integer getTotalVotes() { return totalVotes; }
    public void setTotalVotes(Integer totalVotes) { this.totalVotes = totalVotes; }
    public Integer getRankNo() { return rankNo; }
    public void setRankNo(Integer rankNo) { this.rankNo = rankNo; }
    public String getCalculatedResult() { return calculatedResult; }
    public void setCalculatedResult(String calculatedResult) { this.calculatedResult = calculatedResult; }
}
