package com.ruoyi.evaluation.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Param;
import com.ruoyi.evaluation.domain.Candidate;

public interface CandidateMapper
{
    List<Candidate> selectCandidateList(Candidate candidate);
    List<String> selectCandidateOptionValues(@Param("column") String column);
    Candidate selectCandidateById(Long id);
    Candidate selectCandidateByIdCard(@Param("idCard") String idCard);
    Candidate selectCandidateByActivityIdAndIdCard(Candidate candidate);
    Integer selectMaxImportSeq();
    int insertCandidate(Candidate candidate);
    int updateCandidate(Candidate candidate);
    int prepareCandidateImportSeqResequence();
    int finishCandidateImportSeqResequence();
    int deleteCandidateById(Long id);
    int deleteCandidateByIds(Long[] ids);
}
