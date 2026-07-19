package com.ruoyi.evaluation.service.impl;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.util.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.evaluation.domain.Activity;
import com.ruoyi.evaluation.domain.ActivityCandidate;
import com.ruoyi.evaluation.domain.Candidate;
import com.ruoyi.evaluation.domain.ImportPreviewResult;
import com.ruoyi.evaluation.mapper.ActivityCandidateMapper;
import com.ruoyi.evaluation.mapper.ActivityMapper;
import com.ruoyi.evaluation.mapper.CandidateMapper;
import com.ruoyi.evaluation.service.ICandidateService;

@Service
public class CandidateServiceImpl implements ICandidateService
{
    @Autowired
    private CandidateMapper candidateMapper;

    @Autowired
    private ActivityCandidateMapper activityCandidateMapper;

    @Autowired
    private ActivityMapper activityMapper;

    @Autowired
    private ExcelImportPreviewService excelImportPreviewService;

    @Override
    public List<Candidate> selectCandidateList(Candidate candidate)
    {
        if (candidate != null && candidate.getActivityId() != null)
        {
            return activityCandidateMapper.selectCandidateSnapshotList(candidate);
        }
        return candidateMapper.selectCandidateList(candidate);
    }

    @Override
    public Map<String, List<String>> selectCandidateOptions()
    {
        Map<String, List<String>> options = new LinkedHashMap<>();
        options.put("companies", candidateMapper.selectCandidateOptionValues("company"));
        options.put("departments", candidateMapper.selectCandidateOptionValues("department"));
        options.put("thirdLevelDepartments", candidateMapper.selectCandidateOptionValues("thirdLevelDepartment"));
        options.put("positions", candidateMapper.selectCandidateOptionValues("position"));
        options.put("currentLevels", candidateMapper.selectCandidateOptionValues("currentLevel"));
        options.put("appliedLevels", candidateMapper.selectCandidateOptionValues("appliedLevel"));
        return options;
    }

    @Override
    public Candidate selectCandidateById(Long id) { return candidateMapper.selectCandidateById(id); }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int insertCandidate(Candidate candidate)
    {
        normalizeIdCard(candidate);
        validatePoolIdCardUnique(candidate.getIdCard(), null);
        assignNextPoolSequence(candidate);
        return candidateMapper.insertCandidate(candidate);
    }

    @Override
    public int updateCandidate(Candidate candidate)
    {
        normalizeIdCard(candidate);
        Candidate existing = candidateMapper.selectCandidateById(candidate.getId());
        if (existing != null)
        {
            candidate.setImportSeq(existing.getImportSeq());
        }
        validatePoolIdCardUnique(candidate.getIdCard(), candidate.getId());
        return candidateMapper.updateCandidate(candidate);
    }
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int deleteCandidateByIds(Long[] ids)
    {
        int rows = candidateMapper.deleteCandidateByIds(ids);
        resequencePool();
        return rows;
    }
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int deleteCandidateById(Long id)
    {
        int rows = candidateMapper.deleteCandidateById(id);
        resequencePool();
        return rows;
    }
    @Override
    public int deleteCandidateByActivityId(Long activityId)
    {
        requireConfigurableActivity(activityId);
        return activityCandidateMapper.deleteActivityCandidateByActivityId(activityId);
    }

    @Override
    public ImportPreviewResult previewImport(Long activityId, MultipartFile file, String username) throws Exception
    {
        return excelImportPreviewService.preview(activityId, file, "CANDIDATE_IMPORT", username);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ImportPreviewResult importData(Long activityId, MultipartFile file, boolean updateSupport, String username) throws Exception
    {
        if (activityId == null)
        {
            throw new ServiceException("活动候选人导入必须指定活动");
        }
        requireConfigurableActivity(activityId);
        if (updateSupport)
        {
            activityCandidateMapper.deleteActivityCandidateByActivityId(activityId);
        }
        return excelImportPreviewService.importCandidates(activityId, file, username, candidateMapper, activityCandidateMapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ImportPreviewResult importPoolData(MultipartFile file, boolean updateSupport, String username) throws Exception
    {
        if (updateSupport)
        {
            Candidate query = new Candidate();
            query.setPool(true);
            List<Candidate> pool = candidateMapper.selectCandidateList(query);
            for (Candidate candidate : pool)
            {
                candidateMapper.deleteCandidateById(candidate.getId());
            }
        }
        return excelImportPreviewService.importCandidates(null, file, username, candidateMapper, activityCandidateMapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int selectFromPool(Long activityId, List<Long> ids, boolean replaceExisting, String username)
    {
        if (activityId == null)
        {
            throw new ServiceException("请选择活动");
        }
        requireConfigurableActivity(activityId);
        if (CollectionUtils.isEmpty(ids))
        {
            throw new ServiceException("请选择候选人资料");
        }
        if (replaceExisting)
        {
            activityCandidateMapper.deleteActivityCandidateByActivityId(activityId);
        }
        int inserted = 0;
        for (Long id : ids)
        {
            Candidate source = candidateMapper.selectCandidateById(id);
            if (source == null)
            {
                continue;
            }
            inserted += insertSnapshot(activityId, source, "POOL", null, null, username);
        }
        return inserted;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int copyFromActivity(Long activityId, Long sourceActivityId, boolean replaceExisting, String username)
    {
        if (activityId == null || sourceActivityId == null)
        {
            throw new ServiceException("请选择源活动和目标活动");
        }
        requireConfigurableActivity(activityId);
        if (activityId.equals(sourceActivityId))
        {
            throw new ServiceException("不能复制当前活动自身");
        }
        if (replaceExisting)
        {
            activityCandidateMapper.deleteActivityCandidateByActivityId(activityId);
        }
        ActivityCandidate query = new ActivityCandidate();
        query.setActivityId(sourceActivityId);
        int inserted = 0;
        for (ActivityCandidate source : activityCandidateMapper.selectActivityCandidateList(query))
        {
            inserted += insertSnapshot(activityId, source, username);
        }
        return inserted;
    }

    private int insertSnapshot(Long activityId, Candidate source, String sourceType, Long sourceActivityId, Long sourceActivityCandidateId, String username)
    {
        ActivityCandidate snapshot = new ActivityCandidate();
        snapshot.setActivityId(activityId);
        snapshot.setCandidateId(source.getId());
        snapshot.setSourceType(sourceType);
        snapshot.setSourceActivityId(sourceActivityId);
        snapshot.setSourceActivityCandidateId(sourceActivityCandidateId);
        snapshot.setImportSeq(source.getImportSeq());
        snapshot.setName(source.getName());
        snapshot.setGender(source.getGender());
        snapshot.setBirthDate(source.getBirthDate());
        snapshot.setEducation(source.getEducation());
        snapshot.setCompany(source.getCompany());
        snapshot.setFirstLevelDepartment(source.getFirstLevelDepartment());
        snapshot.setDepartment(source.getDepartment());
        snapshot.setThirdLevelDepartment(source.getThirdLevelDepartment());
        snapshot.setPosition(source.getPosition());
        snapshot.setIdCard(source.getIdCard());
        snapshot.setCurrentLevel(source.getCurrentLevel());
        snapshot.setAppliedLevel(source.getAppliedLevel());
        snapshot.setFixedType(null);
        snapshot.setLastYearAssessment(source.getLastYearAssessment());
        snapshot.setEvaluationScore(source.getEvaluationScore());
        snapshot.setCreateBy(username);
        if (StringUtils.isNotEmpty(snapshot.getIdCard()))
        {
            ActivityCandidate duplicated = new ActivityCandidate();
            duplicated.setActivityId(activityId);
            duplicated.setIdCard(snapshot.getIdCard());
            if (activityCandidateMapper.selectActivityCandidateByActivityIdAndIdCard(duplicated) != null)
            {
                return 0;
            }
        }
        return activityCandidateMapper.insertActivityCandidate(snapshot);
    }

    private void assignNextPoolSequence(Candidate candidate)
    {
        int nextSeq = nextPoolSequence();
        candidate.setImportSeq(nextSeq);
    }

    private void normalizeIdCard(Candidate candidate)
    {
        if (candidate != null && candidate.getIdCard() != null)
        {
            candidate.setIdCard(normalizeIdCard(candidate.getIdCard()));
        }
    }

    private String normalizeIdCard(String idCard)
    {
        return idCard == null ? null : idCard.replace('\u3000', ' ').trim().toUpperCase(Locale.ROOT);
    }

    private void validatePoolIdCardUnique(String idCard, Long selfId)
    {
        if (StringUtils.isEmpty(idCard))
        {
            return;
        }
        Candidate duplicated = candidateMapper.selectCandidateByIdCard(idCard);
        if (duplicated != null && (selfId == null || !selfId.equals(duplicated.getId())))
        {
            throw new ServiceException("ID card already exists in candidate pool: " + idCard);
        }
    }

    private int nextPoolSequence()
    {
        Integer maxImportSeq = candidateMapper.selectMaxImportSeq();
        return (maxImportSeq == null ? 0 : maxImportSeq) + 1;
    }

    private void resequencePool()
    {
        candidateMapper.prepareCandidateImportSeqResequence();
        candidateMapper.finishCandidateImportSeqResequence();
    }

    private int insertSnapshot(Long activityId, ActivityCandidate source, String username)
    {
        ActivityCandidate snapshot = new ActivityCandidate();
        snapshot.setActivityId(activityId);
        snapshot.setCandidateId(source.getCandidateId());
        snapshot.setSourceType("COPY_ACTIVITY");
        snapshot.setSourceActivityId(source.getActivityId());
        snapshot.setSourceActivityCandidateId(source.getId());
        snapshot.setImportSeq(source.getImportSeq());
        snapshot.setName(source.getName());
        snapshot.setGender(source.getGender());
        snapshot.setBirthDate(source.getBirthDate());
        snapshot.setEducation(source.getEducation());
        snapshot.setCompany(source.getCompany());
        snapshot.setFirstLevelDepartment(source.getFirstLevelDepartment());
        snapshot.setDepartment(source.getDepartment());
        snapshot.setThirdLevelDepartment(source.getThirdLevelDepartment());
        snapshot.setPosition(source.getPosition());
        snapshot.setIdCard(source.getIdCard());
        snapshot.setCurrentLevel(source.getCurrentLevel());
        snapshot.setAppliedLevel(source.getAppliedLevel());
        snapshot.setFixedType(null);
        snapshot.setLastYearAssessment(source.getLastYearAssessment());
        snapshot.setEvaluationScore(source.getEvaluationScore());
        snapshot.setCreateBy(username);
        if (StringUtils.isNotEmpty(snapshot.getIdCard()))
        {
            ActivityCandidate duplicated = new ActivityCandidate();
            duplicated.setActivityId(activityId);
            duplicated.setIdCard(snapshot.getIdCard());
            if (activityCandidateMapper.selectActivityCandidateByActivityIdAndIdCard(duplicated) != null)
            {
                return 0;
            }
        }
        return activityCandidateMapper.insertActivityCandidate(snapshot);
    }

    private void requireConfigurableActivity(Long activityId)
    {
        Activity activity = activityMapper.selectActivityById(activityId);
        if (activity == null)
        {
            throw new ServiceException("Activity does not exist.");
        }
        String status = activity.getStatus();
        if (!StringUtils.isEmpty(status) && !"CONFIGURED".equals(status) && !"DRAFT".equals(status))
        {
            throw new ServiceException("Activity candidates can only be changed before publishing.");
        }
    }
}
