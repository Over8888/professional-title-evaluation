package com.ruoyi.evaluation.mapper;

import java.util.List;
import com.ruoyi.evaluation.domain.Vote;

public interface VoteMapper
{
    int insertVote(Vote vote);
    int batchInsertVote(List<Vote> votes);
    int countByActivityVoter(Vote vote);
    List<Vote> selectVoteSummaryByActivityId(Long activityId);
    int deleteVoteByActivityId(Long activityId);
}
