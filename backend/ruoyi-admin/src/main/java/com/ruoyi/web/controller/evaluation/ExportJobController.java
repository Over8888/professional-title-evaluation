package com.ruoyi.web.controller.evaluation;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.evaluation.domain.ExportJob;
import com.ruoyi.evaluation.service.IExportJobService;

@RestController
@RequestMapping("/evaluation/export-job")
public class ExportJobController extends BaseController
{
    @Autowired
    private IExportJobService exportJobService;

    @PreAuthorize("@ss.hasPermi('evaluation:export:list')")
    @GetMapping("/list")
    public TableDataInfo list(ExportJob exportJob)
    {
        startPage();
        List<ExportJob> list = exportJobService.selectExportJobList(exportJob);
        return getDataTable(list);
    }

    @PreAuthorize("@ss.hasPermi('evaluation:export:query')")
    @GetMapping("/{id}")
    public AjaxResult getInfo(@PathVariable Long id)
    {
        return success(exportJobService.selectExportJobById(id));
    }

    @PreAuthorize("@ss.hasPermi('evaluation:export:remove')")
    @Log(title = "ExportJob", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids)
    {
        return toAjax(exportJobService.deleteExportJobByIds(ids));
    }
}
