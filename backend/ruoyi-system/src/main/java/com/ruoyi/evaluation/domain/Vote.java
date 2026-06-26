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
}
