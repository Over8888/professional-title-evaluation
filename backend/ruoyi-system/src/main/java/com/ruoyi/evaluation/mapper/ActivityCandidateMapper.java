package com.ruoyi.evaluation.mapper;

import java.util.List;
import com.ruoyi.evaluation.domain.ActivityCandidate;
import com.ruoyi.evaluation.domain.Candidate;

public interface ActivityCandidateMapper
{
    List<ActivityCandidate> selectActivityCandidateList(ActivityCandidate activityCandidate);
    List<Candidate> selectCandidateSnapshotList(Candidate candidate);
    ActivityCandidate selectActivityCandidateById(Long id);
    ActivityCandidate selectActivityCandidateByActivityIdAndIdCard(ActivityCandidate activityCandidate);
    ActivityCandidate selectActivityCandidateByActivityIdAndScope(ActivityCandidate activityCandidate);
    List<ActivityCandidate> selectAllCandidatesByActivityId(Long activityId);
    List<ActivityCandidate> selectVoteCandidatesByActivityId(Long activityId);
    int countVoteCandidatesByActivityId(Long activityId);
    int countActivityCandidateByActivityId(Long activityId);
    int countUnconfirmedByActivityId(Long activityId);
    int insertActivityCandidate(ActivityCandidate activityCandidate);
    int updateActivityCandidate(ActivityCandidate activityCandidate);
    int updateCandidateSnapshot(Candidate candidate);
    int deleteActivityCandidateByActivityId(Long activityId);
}
