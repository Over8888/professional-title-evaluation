package com.ruoyi.web.controller.evaluation;

import java.util.List;
import java.util.Map;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.evaluation.domain.ResultAgg;
import com.ruoyi.evaluation.service.IResultAggService;

@RestController
@RequestMapping("/evaluation/result")
public class ResultController extends BaseController
{
    @Autowired
    private IResultAggService resultAggService;

    @PreAuthorize("@ss.hasPermi('evaluation:result:calculate') or @ss.hasPermi('evaluation:activity:calculate')")
    @Log(title = "Result calculate", businessType = BusinessType.UPDATE)
    @PostMapping("/calculate/{activityId}")
    public AjaxResult calculate(@PathVariable Long activityId,
            @RequestParam(value = "force", required = false, defaultValue = "false") boolean force)
    {
        return success(resultAggService.calculate(activityId, getUsername(), force));
    }

    @PreAuthorize("@ss.hasPermi('evaluation:result:list') or @ss.hasPermi('evaluation:activity:list')")
    @GetMapping("/summary/{activityId}")
    public AjaxResult summary(@PathVariable Long activityId)
    {
        return success(resultAggService.summary(activityId));
    }

    @PreAuthorize("@ss.hasPermi('evaluation:result:list') or @ss.hasPermi('evaluation:activity:list')")
    @GetMapping("/voteSummary/{activityId}")
    public AjaxResult voteSummary(@PathVariable Long activityId)
    {
        return success(resultAggService.voteSummary(activityId));
    }

    @PreAuthorize("@ss.hasPermi('evaluation:result:list') or @ss.hasPermi('evaluation:activity:list')")
    @GetMapping("/list")
    public TableDataInfo list(ResultAgg resultAgg)
    {
        List<Map<String, Object>> list = resultAggService.selectResultAggList(resultAgg);
        return getDataTable(list);
    }

    @PreAuthorize("@ss.hasPermi('evaluation:result:list') or @ss.hasPermi('evaluation:activity:list')")
    @GetMapping("/candidateList")
    public TableDataInfo candidateList(ResultAgg resultAgg)
    {
        List<ResultAgg> list = resultAggService.selectCandidateResultList(resultAgg);
        return getDataTable(list);
    }

    @PreAuthorize("@ss.hasPermi('evaluation:result:export') or @ss.hasPermi('evaluation:activity:export')")
    @Log(title = "Result export", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, Long activityId, String exportType)
    {
        resultAggService.exportResult(response, activityId, exportType, getUsername());
    }
}
