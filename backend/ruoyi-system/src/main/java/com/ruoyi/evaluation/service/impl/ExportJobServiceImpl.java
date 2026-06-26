package com.ruoyi.evaluation.service.impl;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.ruoyi.evaluation.domain.ExportJob;
import com.ruoyi.evaluation.mapper.ExportJobMapper;
import com.ruoyi.evaluation.service.IExportJobService;

@Service
public class ExportJobServiceImpl implements IExportJobService
{
    @Autowired
    private ExportJobMapper exportJobMapper;

    @Override
    public List<ExportJob> selectExportJobList(ExportJob exportJob) { return exportJobMapper.selectExportJobList(exportJob); }
    @Override
    public ExportJob selectExportJobById(Long id) { return exportJobMapper.selectExportJobById(id); }
    @Override
    public int insertExportJob(ExportJob exportJob) { return exportJobMapper.insertExportJob(exportJob); }
    @Override
    public int updateExportJob(ExportJob exportJob) { return exportJobMapper.updateExportJob(exportJob); }
    @Override
    public int deleteExportJobByIds(Long[] ids) { return exportJobMapper.deleteExportJobByIds(ids); }
    @Override
    public int deleteExportJobById(Long id) { return exportJobMapper.deleteExportJobById(id); }
}
