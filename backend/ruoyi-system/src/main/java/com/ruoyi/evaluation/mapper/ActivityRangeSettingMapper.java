package com.ruoyi.evaluation.mapper;

import java.util.List;
import com.ruoyi.evaluation.domain.ActivityRangeSetting;

public interface ActivityRangeSettingMapper
{
    List<ActivityRangeSetting> selectActivityRangeSettingByActivityId(Long activityId);
    int upsertActivityRangeSetting(ActivityRangeSetting activityRangeSetting);
    int deleteActivityRangeSettingByActivityId(Long activityId);
}
