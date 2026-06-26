package com.ruoyi.evaluation.service;

import java.util.List;
import com.ruoyi.evaluation.domain.ExportJob;

public interface IExportJobService
{
    List<ExportJob> selectExportJobList(ExportJob exportJob);
    ExportJob selectExportJobById(Long id);
    int insertExportJob(ExportJob exportJob);
    int updateExportJob(ExportJob exportJob);
    int deleteExportJobByIds(Long[] ids);
    int deleteExportJobById(Long id);
}
