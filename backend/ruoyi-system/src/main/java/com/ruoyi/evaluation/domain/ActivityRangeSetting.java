package com.ruoyi.evaluation.domain;

import com.ruoyi.common.core.domain.BaseEntity;

public class ActivityRangeSetting extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    private Long id;
    private Long activityId;
    private String department;
    private String appliedLevel;
    private Integer lockedPassCount;
    private Integer lockedRejectCount;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getActivityId() { return activityId; }
    public void setActivityId(Long activityId) { this.activityId = activityId; }
    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }
    public String getAppliedLevel() { return appliedLevel; }
    public void setAppliedLevel(String appliedLevel) { this.appliedLevel = appliedLevel; }
    public Integer getLockedPassCount() { return lockedPassCount; }
    public void setLockedPassCount(Integer lockedPassCount) { this.lockedPassCount = lockedPassCount; }
    public Integer getLockedRejectCount() { return lockedRejectCount; }
    public void setLockedRejectCount(Integer lockedRejectCount) { this.lockedRejectCount = lockedRejectCount; }
}
