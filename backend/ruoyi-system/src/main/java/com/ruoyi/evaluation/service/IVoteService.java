package com.ruoyi.evaluation.service;

import java.util.List;
import java.util.Map;
import com.ruoyi.evaluation.domain.VoteConfirmRequest;
import com.ruoyi.evaluation.domain.VoteSubmitRequest;

public interface IVoteService
{
    Map<String, Object> getEntry(String token, boolean strict);
    Map<String, Object> confirm(String token, VoteConfirmRequest request);
    List<Map<String, Object>> listCandidates(String token);
    Map<String, Object> submit(String token, VoteSubmitRequest request);
    Map<String, Object> result(String token);
}
