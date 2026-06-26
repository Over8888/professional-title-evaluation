package com.ruoyi.evaluation.service;

import java.util.List;
import java.util.Map;
import org.springframework.web.multipart.MultipartFile;
import com.ruoyi.evaluation.domain.ImportPreviewResult;
import com.ruoyi.evaluation.domain.Voter;

public interface IVoterService
{
    List<Voter> selectVoterList(Voter voter);
    Map<String, List<String>> selectVoterOptions();
    Voter selectVoterById(Long id);
    int insertVoter(Voter voter);
    int updateVoter(Voter voter);
    int deleteVoterByIds(Long[] ids);
    int deleteVoterById(Long id);
    int deleteVoterByActivityId(Long activityId);
    int regenerateVoteToken(Long id, String username);
    java.util.Map<String, Object> selectVoterProgress(Long activityId);
    java.util.List<java.util.Map<String, Object>> selectVoteLinks(Long activityId, String requestBaseUrl);
    ImportPreviewResult previewImport(Long activityId, MultipartFile file, String username) throws Exception;
    ImportPreviewResult importData(Long activityId, MultipartFile file, boolean updateSupport, String username) throws Exception;
    ImportPreviewResult importPoolData(MultipartFile file, boolean updateSupport, String username) throws Exception;
    int selectFromPool(Long activityId, List<Long> ids, boolean replaceExisting, String username);
    int copyFromActivity(Long activityId, Long sourceActivityId, boolean replaceExisting, String username);
}
