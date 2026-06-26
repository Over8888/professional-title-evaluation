package com.ruoyi.web.controller.evaluation;

import java.util.List;
import java.util.Map;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.common.utils.poi.ExcelUtil;
import com.ruoyi.evaluation.domain.RangePreviewRow;
import com.ruoyi.evaluation.domain.RuleConfig;
import com.ruoyi.evaluation.service.IRuleConfigService;

@RestController
@RequestMapping("/evaluation/rule")
public class RuleConfigController extends BaseController
{
    @Autowired
    private IRuleConfigService ruleConfigService;

    @PreAuthorize("@ss.hasPermi('evaluation:rule:list')")
    @GetMapping("/list")
    public TableDataInfo list(RuleConfig ruleConfig)
    {
        startPage();
        List<RuleConfig> list = ruleConfigService.selectRuleConfigList(ruleConfig);
        return getDataTable(list);
    }

    @PreAuthorize("@ss.hasPermi('evaluation:rule:query')")
    @GetMapping("/{id}")
    public AjaxResult getInfo(@PathVariable Long id)
    {
        return success(ruleConfigService.selectRuleConfigById(id));
    }

    @PreAuthorize("@ss.hasPermi('evaluation:rule:query')")
    @GetMapping("/activity/{activityId}")
    public AjaxResult getByActivity(@PathVariable Long activityId)
    {
        return success(ruleConfigService.selectRuleConfigByActivityId(activityId));
    }

    @PreAuthorize("@ss.hasPermi('evaluation:rule:preview')")
    @GetMapping("/preview/{activityId}")
    public AjaxResult preview(@PathVariable Long activityId)
    {
        return success(ruleConfigService.previewCandidateRange(activityId));
    }

    @PreAuthorize("@ss.hasPermi('evaluation:rule:preview')")
    @GetMapping("/registrationStats")
    public TableDataInfo registrationStats(RuleConfig ruleConfig)
    {
        List<java.util.Map<String, Object>> list = ruleConfigService.previewCandidateRange(ruleConfig.getActivityId());
        return getDataTable(list);
    }

    @PreAuthorize("@ss.hasPermi('evaluation:rule:preview')")
    @GetMapping("/rangePreview")
    public TableDataInfo rangePreview(RuleConfig ruleConfig)
    {
        List<java.util.Map<String, Object>> list = ruleConfigService.previewCandidateRange(ruleConfig.getActivityId());
        return getDataTable(list);
    }

    @PreAuthorize("@ss.hasPermi('evaluation:candidate:export')")
    @Log(title = "Rule range export", businessType = BusinessType.EXPORT)
    @PostMapping("/rangeExport")
    public void rangeExport(HttpServletResponse response, RuleConfig ruleConfig)
    {
        List<java.util.Map<String, Object>> list = ruleConfigService.previewCandidateRange(ruleConfig.getActivityId());
        List<RangePreviewRow> rows = new java.util.ArrayList<>();
        for (java.util.Map<String, Object> item : list)
        {
            RangePreviewRow row = new RangePreviewRow();
            row.setDepartment((String) item.get("department"));
            row.setAppliedLevel((String) item.get("appliedLevel"));
            row.setCandidateCount((Integer) item.get("candidateCount"));
            Object passRatio = item.get("passRatio");
            row.setPassRatio(passRatio == null ? null : (java.math.BigDecimal) passRatio);
            Object maxPassCount = item.get("maxPassCount");
            row.setMaxPassCount(maxPassCount == null ? null : ((Number) maxPassCount).longValue());
            Object fixedPassCount = item.get("fixedPassCount");
            row.setFixedPassCount(fixedPassCount == null ? null : ((Number) fixedPassCount).longValue());
            Object minVoteRejectCount = item.get("minVoteRejectCount");
            row.setMinVoteRejectCount(minVoteRejectCount == null ? null : ((Number) minVoteRejectCount).longValue());
            Object fixedRejectCount = item.get("fixedRejectCount");
            row.setFixedRejectCount(fixedRejectCount == null ? null : ((Number) fixedRejectCount).longValue());
            row.setFixedPassRange((String) item.get("confirmedPassRange"));
            row.setVoteRange((String) item.get("voteRange"));
            row.setFixedRejectRange((String) item.get("confirmedRejectRange"));
            row.setPlannedVoterCount((Integer) item.get("plannedVoterCount"));
            row.setStatus((String) item.get("status"));
            rows.add(row);
        }
        ExcelUtil<RangePreviewRow> util = new ExcelUtil<RangePreviewRow>(RangePreviewRow.class);
        util.exportExcel(response, rows, "范围预览");
    }

    @PreAuthorize("@ss.hasPermi('evaluation:rule:edit')")
    @Log(title = "RuleConfig apply", businessType = BusinessType.UPDATE)
    @PutMapping("/apply/{activityId}")
    public AjaxResult apply(@PathVariable Long activityId)
    {
        return toAjax(ruleConfigService.applyCandidateRange(activityId, getUsername()));
    }

    @PreAuthorize("@ss.hasPermi('evaluation:rule:edit')")
    @Log(title = "RuleConfig confirmed apply", businessType = BusinessType.UPDATE)
    @PutMapping("/apply/{activityId}/confirmed")
    public AjaxResult applyConfirmed(@PathVariable Long activityId, @RequestBody List<Map<String, Object>> confirmedRanges)
    {
        return toAjax(ruleConfigService.applyCandidateRange(activityId, confirmedRanges, getUsername()));
    }

    @PreAuthorize("@ss.hasPermi('evaluation:rule:edit')")
    @Log(title = "RuleConfig", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@Validated @RequestBody RuleConfig ruleConfig)
    {
        ruleConfig.setCreateBy(getUsername());
        return toAjax(ruleConfigService.insertRuleConfig(ruleConfig));
    }

    @PreAuthorize("@ss.hasPermi('evaluation:rule:edit')")
    @Log(title = "RuleConfig", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@Validated @RequestBody RuleConfig ruleConfig)
    {
        ruleConfig.setUpdateBy(getUsername());
        return toAjax(ruleConfigService.updateRuleConfig(ruleConfig));
    }

    @PreAuthorize("@ss.hasPermi('evaluation:rule:edit')")
    @Log(title = "RuleConfig", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids)
    {
        return toAjax(ruleConfigService.deleteRuleConfigByIds(ids));
    }
}
