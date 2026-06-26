package com.ruoyi.evaluation.mapper;

import java.util.List;
import com.ruoyi.evaluation.domain.ExportJob;

public interface ExportJobMapper
{
    List<ExportJob> selectExportJobList(ExportJob exportJob);
    ExportJob selectExportJobById(Long id);
    int insertExportJob(ExportJob exportJob);
    int updateExportJob(ExportJob exportJob);
    int deleteExportJobById(Long id);
    int deleteExportJobByIds(Long[] ids);
}
