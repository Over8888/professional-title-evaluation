package com.ruoyi.evaluation.service;

import java.util.List;
import java.util.Map;
import org.springframework.web.multipart.MultipartFile;
import com.ruoyi.evaluation.domain.Candidate;
import com.ruoyi.evaluation.domain.ImportPreviewResult;

public interface ICandidateService
{
    List<Candidate> selectCandidateList(Candidate candidate);
    Map<String, List<String>> selectCandidateOptions();
    Candidate selectCandidateById(Long id);
    int insertCandidate(Candidate candidate);
    int updateCandidate(Candidate candidate);
    int deleteCandidateByIds(Long[] ids);
    int deleteCandidateById(Long id);
    int deleteCandidateByActivityId(Long activityId);
    ImportPreviewResult previewImport(Long activityId, MultipartFile file, String username) throws Exception;
    ImportPreviewResult importData(Long activityId, MultipartFile file, boolean updateSupport, String username) throws Exception;
    ImportPreviewResult importPoolData(MultipartFile file, boolean updateSupport, String username) throws Exception;
    int selectFromPool(Long activityId, List<Long> ids, boolean replaceExisting, String username);
    int copyFromActivity(Long activityId, Long sourceActivityId, boolean replaceExisting, String username);
}
