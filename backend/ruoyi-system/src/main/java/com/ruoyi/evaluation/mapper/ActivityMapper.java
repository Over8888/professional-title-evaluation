package com.ruoyi.evaluation.mapper;

import java.util.List;
import com.ruoyi.evaluation.domain.Activity;

public interface ActivityMapper
{
    List<Activity> selectActivityList(Activity activity);
    Activity selectActivityById(Long id);
    Activity selectActivityByIdForUpdate(Long id);
    Activity selectActivityByVoteEntryKey(String voteEntryKey);
    int updateExpiredActivities();
    int updateStartedActivities();
    int insertActivity(Activity activity);
    int updateActivity(Activity activity);
    int deleteActivityById(Long id);
    int deleteActivityByIds(Long[] ids);
}
