package com.ruoyi.evaluation.service;

import java.util.List;
import com.ruoyi.evaluation.domain.Activity;
import com.ruoyi.evaluation.domain.ActivityCreateRequest;

public interface IActivityService
{
    List<Activity> selectActivityList(Activity activity);
    Activity selectActivityById(Long id);
    int insertActivity(Activity activity);
    Activity createWithRule(ActivityCreateRequest request, String username);
    int updateActivity(Activity activity);
    int updateActivityStatus(Long id, String status, String username);
    java.util.Map<String, Object> publishActivity(Long id, String username, String requestBaseUrl);
    int deleteActivityByIds(Long[] ids);
    int deleteActivityById(Long id);
}
