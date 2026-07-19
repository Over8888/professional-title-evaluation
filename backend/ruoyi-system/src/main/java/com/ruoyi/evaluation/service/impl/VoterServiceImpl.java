package com.ruoyi.evaluation.service.impl;

import java.util.List;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.web.multipart.MultipartFile;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.evaluation.domain.Activity;
import com.ruoyi.evaluation.domain.ActivityVoter;
import com.ruoyi.evaluation.domain.ImportPreviewResult;
import com.ruoyi.evaluation.domain.Voter;
import com.ruoyi.evaluation.mapper.ActivityMapper;
import com.ruoyi.evaluation.mapper.ActivityVoterMapper;
import com.ruoyi.evaluation.mapper.VoterMapper;
import com.ruoyi.evaluation.service.IVoterService;

@Service
public class VoterServiceImpl implements IVoterService
{
    @Autowired
    private VoterMapper voterMapper;

    @Autowired
    private ActivityVoterMapper activityVoterMapper;

    @Autowired
    private ActivityMapper activityMapper;

    @Autowired
    private ExcelImportPreviewService excelImportPreviewService;

    @Override
    public List<Voter> selectVoterList(Voter voter)
    {
        if (voter != null && voter.getActivityId() != null)
        {
            return activityVoterMapper.selectVoterSnapshotList(voter);
        }
        return voterMapper.selectVoterList(voter);
    }

    @Override
    public Map<String, List<String>> selectVoterOptions()
    {
        Map<String, List<String>> options = new LinkedHashMap<>();
        options.put("employeeIds", voterMapper.selectVoterOptionValues("employeeId"));
        options.put("departments", voterMapper.selectVoterOptionValues("department"));
        return options;
    }

    @Override
    public Voter selectVoterById(Long id) { return voterMapper.selectVoterById(id); }

    @Override
    public int insertVoter(Voter voter)
    {
        if (voter.getActivityId() != null)
        {
            requireConfigurableActivity(voter.getActivityId());
            Voter poolVoter = ensurePoolVoter(voter, voter.getCreateBy());
            return insertSnapshot(voter.getActivityId(), poolVoter, "MANUAL", null, null, voter.getCreateBy());
        }
        assignNextPoolSequence(voter);
        return voterMapper.insertVoter(voter);
    }

    @Override
    public int updateVoter(Voter voter)
    {
        if (voter.getActivityId() != null)
        {
            requireConfigurableActivity(voter.getActivityId());
            return activityVoterMapper.updateVoterSnapshot(voter);
        }
        return voterMapper.updateVoter(voter);
    }

    @Override
    public int deleteVoterByIds(Long[] ids) { return voterMapper.deleteVoterByIds(ids); }

    @Override
    public int deleteVoterById(Long id) { return voterMapper.deleteVoterById(id); }

    @Override
    public int deleteVoterByActivityId(Long activityId)
    {
        requireConfigurableActivity(activityId);
        return activityVoterMapper.deleteActivityVoterByActivityId(activityId);
    }

    @Override
    public int regenerateVoteToken(Long id, String username)
    {
        ActivityVoter existing = activityVoterMapper.selectActivityVoterById(id);
        if (existing == null)
        {
            throw new ServiceException("Activity voter does not exist.");
        }
        requireConfigurableActivity(existing.getActivityId());
        ActivityVoter voter = new ActivityVoter();
        voter.setId(id);
        voter.setVoteToken(UUID.randomUUID().toString().replace("-", ""));
        voter.setUpdateBy(username);
        return activityVoterMapper.updateActivityVoter(voter);
    }

    @Override
    public Map<String, Object> selectVoterProgress(Long activityId)
    {
        Activity activity = activityMapper.selectActivityById(activityId);
        Voter query = new Voter();
        query.setActivityId(activityId);
        List<Voter> voters = activityVoterMapper.selectVoterSnapshotList(query);
        long done = voters.stream().filter(v -> "DONE".equalsIgnoreCase(v.getStatus())).count();
        List<Map<String, Object>> pendingVoters = new ArrayList<>();
        List<Map<String, Object>> voterRows = new ArrayList<>();
        for (Voter voter : voters)
        {
            boolean submitted = "DONE".equalsIgnoreCase(voter.getStatus());
            Map<String, Object> row = buildVoterStatusRow(voter, submitted);
            voterRows.add(row);
            if (!submitted)
            {
                pendingVoters.add(row);
            }
        }
        Map<String, Object> progress = new LinkedHashMap<>();
        if (activity != null)
        {
            progress.put("activityId", activity.getId());
            progress.put("activityStatus", activity.getStatus());
            progress.put("status", activity.getStatus());
            progress.put("endTime", activity.getEndTime());
        }
        progress.put("total", voters.size());
        progress.put("done", done);
        progress.put("submitted", done);
        progress.put("pending", voters.size() - done);
        progress.put("unsubmitted", voters.size() - done);
        progress.put("rate", voters.isEmpty() ? BigDecimal.ZERO : BigDecimal.valueOf(done)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(voters.size()), 2, RoundingMode.HALF_UP));
        progress.put("allCompleted", !voters.isEmpty() && done == voters.size());
        progress.put("resultAvailable", !voters.isEmpty() && done == voters.size());
        progress.put("nextAction", !voters.isEmpty() && done == voters.size() ? "CALCULATE_RESULT" : "WAIT_FOR_VOTERS");
        progress.put("pendingVoters", pendingVoters);
        progress.put("voters", voterRows);
        return progress;
    }

    @Override
    public List<Map<String, Object>> selectVoteLinks(Long activityId, String requestBaseUrl)
    {
        Voter query = new Voter();
        query.setActivityId(activityId);
        List<Voter> voters = activityVoterMapper.selectVoterSnapshotList(query);
        List<Map<String, Object>> rows = new ArrayList<>();
        for (Voter voter : voters)
        {
            Map<String, Object> row = buildVoterStatusRow(voter, "DONE".equalsIgnoreCase(voter.getStatus()));
            row.put("voteToken", voter.getVoteToken());
            row.put("voteUrl", requestBaseUrl + "/vote/" + voter.getVoteToken());
            rows.add(row);
        }
        return rows;
    }

    @Override
    public ImportPreviewResult previewImport(Long activityId, MultipartFile file, String username) throws Exception
    {
        return excelImportPreviewService.preview(activityId, file, "VOTER_IMPORT", username);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ImportPreviewResult importData(Long activityId, MultipartFile file, boolean updateSupport, String username) throws Exception
    {
        if (activityId == null)
        {
            throw new ServiceException("活动评委导入必须指定活动");
        }
        requireConfigurableActivity(activityId);
        if (updateSupport)
        {
            activityVoterMapper.deleteActivityVoterByActivityId(activityId);
        }
        return excelImportPreviewService.importVoters(activityId, file, username, voterMapper, activityVoterMapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ImportPreviewResult importPoolData(MultipartFile file, boolean updateSupport, String username) throws Exception
    {
        if (updateSupport)
        {
            Voter query = new Voter();
            query.setPool(true);
            List<Voter> pool = voterMapper.selectVoterList(query);
            for (Voter voter : pool)
            {
                voterMapper.deleteVoterById(voter.getId());
            }
        }
        return excelImportPreviewService.importVoters(null, file, username, voterMapper, activityVoterMapper);
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
            throw new ServiceException("请选择评委资料");
        }
        if (replaceExisting)
        {
            activityVoterMapper.deleteActivityVoterByActivityId(activityId);
        }
        int inserted = 0;
        for (Long id : ids)
        {
            Voter source = voterMapper.selectVoterById(id);
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
            activityVoterMapper.deleteActivityVoterByActivityId(activityId);
        }
        ActivityVoter query = new ActivityVoter();
        query.setActivityId(sourceActivityId);
        int inserted = 0;
        for (ActivityVoter source : activityVoterMapper.selectActivityVoterList(query))
        {
            inserted += insertSnapshot(activityId, source, username);
        }
        return inserted;
    }

    private Voter ensurePoolVoter(Voter source, String username)
    {
        Voter duplicated = voterMapper.selectVoterByUniqueKey(source);
        if (duplicated != null)
        {
            return duplicated;
        }
        Voter voter = new Voter();
        voter.setName(source.getName());
        voter.setEmployeeId(source.getEmployeeId());
        voter.setDepartment(source.getDepartment());
        voter.setRemark(source.getRemark());
        voter.setCreateBy(username);
        assignNextPoolSequence(voter);
        voterMapper.insertVoter(voter);
        return voter;
    }

    private int insertSnapshot(Long activityId, Voter source, String sourceType, Long sourceActivityId, Long sourceActivityVoterId, String username)
    {
        ActivityVoter snapshot = new ActivityVoter();
        snapshot.setActivityId(activityId);
        snapshot.setVoterId(source.getId());
        snapshot.setImportSeq(source.getImportSeq());
        snapshot.setSourceType(sourceType);
        snapshot.setSourceActivityId(sourceActivityId);
        snapshot.setSourceActivityVoterId(sourceActivityVoterId);
        snapshot.setName(source.getName());
        snapshot.setEmployeeId(source.getEmployeeId());
        snapshot.setDepartment(source.getDepartment());
        snapshot.setStatus("PENDING");
        snapshot.setVoteToken(UUID.randomUUID().toString().replace("-", ""));
        snapshot.setRemark(source.getRemark());
        snapshot.setCreateBy(username);
        return insertSnapshot(snapshot);
    }

    private int insertSnapshot(Long activityId, ActivityVoter source, String username)
    {
        ActivityVoter snapshot = new ActivityVoter();
        snapshot.setActivityId(activityId);
        snapshot.setVoterId(source.getVoterId());
        snapshot.setImportSeq(source.getImportSeq());
        snapshot.setSourceType("COPY_ACTIVITY");
        snapshot.setSourceActivityId(source.getActivityId());
        snapshot.setSourceActivityVoterId(source.getId());
        snapshot.setName(source.getName());
        snapshot.setEmployeeId(source.getEmployeeId());
        snapshot.setDepartment(source.getDepartment());
        snapshot.setStatus("PENDING");
        snapshot.setVoteToken(UUID.randomUUID().toString().replace("-", ""));
        snapshot.setRemark(source.getRemark());
        snapshot.setCreateBy(username);
        return insertSnapshot(snapshot);
    }

    private int insertSnapshot(ActivityVoter snapshot)
    {
        if (StringUtils.isEmpty(snapshot.getName()))
        {
            throw new ServiceException("评委姓名不能为空");
        }
        if (activityVoterMapper.selectActivityVoterByUniqueKey(snapshot) != null)
        {
            return 0;
        }
        return activityVoterMapper.insertActivityVoter(snapshot);
    }

    private Map<String, Object> buildVoterStatusRow(Voter voter, boolean submitted)
    {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("id", voter.getId());
        row.put("name", voter.getName());
        row.put("employeeId", voter.getEmployeeId());
        row.put("department", voter.getDepartment());
        row.put("status", voter.getStatus());
        row.put("submitted", submitted);
        row.put("submittedAt", voter.getSubmittedAt());
        return row;
    }

    private void assignNextPoolSequence(Voter voter)
    {
        if (voter.getImportSeq() != null)
        {
            return;
        }
        Integer maxImportSeq = voterMapper.selectMaxImportSeq();
        voter.setImportSeq((maxImportSeq == null ? 0 : maxImportSeq) + 1);
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
            throw new ServiceException("Activity voters can only be changed before publishing.");
        }
    }
}
