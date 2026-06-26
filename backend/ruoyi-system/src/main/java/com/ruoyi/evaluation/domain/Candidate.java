package com.ruoyi.evaluation.domain;

import java.util.Date;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.ruoyi.common.annotation.Excel;
import com.ruoyi.common.annotation.Excel.ColumnType;
import com.ruoyi.common.core.domain.BaseEntity;

public class Candidate extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    private Long id;
    private Long candidateId;
    private Long activityId;
    private Long sourceActivityId;
    private Long sourceActivityCandidateId;
    private String sourceType;

    @Excel(name = "序号")
    private Integer importSeq;

    @Excel(name = "姓名")
    private String name;

    private String gender;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private Date birthDate;

    private String education;

    @Excel(name = "单位")
    private String company;

    private String firstLevelDepartment;

    @Excel(name = "二级部门")
    private String department;

    @Excel(name = "三级部门")
    private String thirdLevelDepartment;

    @Excel(name = "岗位")
    private String position;

    @Excel(name = "身份证号", cellType = ColumnType.TEXT)
    private String idCard;

    @Excel(name = "当前等级")
    private String currentLevel;

    @Excel(name = "申报等级")
    private String appliedLevel;

    private String fixedType;
    private Boolean pool;
    private String lastYearAssessment;
    private String evaluationScore;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getCandidateId() { return candidateId; }
    public void setCandidateId(Long candidateId) { this.candidateId = candidateId; }
    public Long getActivityId() { return activityId; }
    public void setActivityId(Long activityId) { this.activityId = activityId; }
    public Long getSourceActivityId() { return sourceActivityId; }
    public void setSourceActivityId(Long sourceActivityId) { this.sourceActivityId = sourceActivityId; }
    public Long getSourceActivityCandidateId() { return sourceActivityCandidateId; }
    public void setSourceActivityCandidateId(Long sourceActivityCandidateId) { this.sourceActivityCandidateId = sourceActivityCandidateId; }
    public String getSourceType() { return sourceType; }
    public void setSourceType(String sourceType) { this.sourceType = sourceType; }
    public Integer getImportSeq() { return importSeq; }
    public void setImportSeq(Integer importSeq) { this.importSeq = importSeq; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getGender() { return gender; }
    public void setGender(String gender) { this.gender = gender; }
    public Date getBirthDate() { return birthDate; }
    public void setBirthDate(Date birthDate) { this.birthDate = birthDate; }
    public String getEducation() { return education; }
    public void setEducation(String education) { this.education = education; }
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
    public String getIdCard() { return idCard; }
    public void setIdCard(String idCard) { this.idCard = idCard; }
    public String getCurrentLevel() { return currentLevel; }
    public void setCurrentLevel(String currentLevel) { this.currentLevel = currentLevel; }
    public String getAppliedLevel() { return appliedLevel; }
    public void setAppliedLevel(String appliedLevel) { this.appliedLevel = appliedLevel; }
    public String getFixedType() { return fixedType; }
    public void setFixedType(String fixedType) { this.fixedType = fixedType; }
    public Boolean getPool() { return pool; }
    public void setPool(Boolean pool) { this.pool = pool; }
    public String getLastYearAssessment() { return lastYearAssessment; }
    public void setLastYearAssessment(String lastYearAssessment) { this.lastYearAssessment = lastYearAssessment; }
    public String getEvaluationScore() { return evaluationScore; }
    public void setEvaluationScore(String evaluationScore) { this.evaluationScore = evaluationScore; }
}
