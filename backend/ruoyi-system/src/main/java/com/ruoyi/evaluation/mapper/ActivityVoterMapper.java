package com.ruoyi.evaluation.mapper;

import java.util.List;
import com.ruoyi.evaluation.domain.ActivityVoter;
import com.ruoyi.evaluation.domain.Voter;

public interface ActivityVoterMapper
{
    List<ActivityVoter> selectActivityVoterList(ActivityVoter activityVoter);
    List<Voter> selectVoterSnapshotList(Voter voter);
    ActivityVoter selectActivityVoterById(Long id);
    ActivityVoter selectActivityVoterByToken(String voteToken);
    ActivityVoter selectActivityVoterByTokenForUpdate(String voteToken);
    ActivityVoter selectActivityVoterByIdentity(ActivityVoter activityVoter);
    ActivityVoter selectActivityVoterByUniqueKey(ActivityVoter activityVoter);
    int countActivityVoterByActivityId(Long activityId);
    int countDoneByActivityId(Long activityId);
    int insertActivityVoter(ActivityVoter activityVoter);
    int updateActivityVoter(ActivityVoter activityVoter);
    int markDone(ActivityVoter activityVoter);
    int updateVoterSnapshot(Voter voter);
    int deleteActivityVoterById(Long id);
    int deleteActivityVoterByIds(Long[] ids);
    int deleteActivityVoterByActivityId(Long activityId);
}
