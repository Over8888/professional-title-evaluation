package com.ruoyi.evaluation.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Param;
import com.ruoyi.evaluation.domain.Voter;

public interface VoterMapper
{
    List<Voter> selectVoterList(Voter voter);
    List<String> selectVoterOptionValues(@Param("column") String column);
    Voter selectVoterById(Long id);
    Voter selectVoterByEmployeeId(String employeeId);
    Voter selectVoterByUniqueKey(Voter voter);
    Integer selectMaxImportSeq();
    int insertVoter(Voter voter);
    int updateVoter(Voter voter);
    int deleteVoterById(Long id);
    int deleteVoterByIds(Long[] ids);
}
