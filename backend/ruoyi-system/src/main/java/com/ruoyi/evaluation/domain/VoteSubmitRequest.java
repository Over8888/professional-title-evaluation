package com.ruoyi.evaluation.domain;

import java.util.List;

public class VoteSubmitRequest
{
    private Integer roundNo;
    private List<VoteSubmitItem> votes;

    public Integer getRoundNo() { return roundNo; }
    public void setRoundNo(Integer roundNo) { this.roundNo = roundNo; }
    public List<VoteSubmitItem> getVotes() { return votes; }
    public void setVotes(List<VoteSubmitItem> votes) { this.votes = votes; }
}
