package com.ruoyi.web.controller.evaluation;

import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.evaluation.domain.FinalEvaluation;
import com.ruoyi.evaluation.service.IFinalEvaluationService;

@RestController
@RequestMapping("/evaluation/final")
public class FinalEvaluationController extends BaseController
{
    @Autowired
    private IFinalEvaluationService finalEvaluationService;

    @PreAuthorize("@ss.hasPermi('evaluation:final:list') or @ss.hasPermi('evaluation:result:list')")
    @GetMapping("/list")
    public TableDataInfo list(FinalEvaluation finalEvaluation)
    {
        startPage();
        List<FinalEvaluation> list = finalEvaluationService.selectFinalEvaluationList(finalEvaluation);
        return getDataTable(list);
    }

    @PreAuthorize("@ss.hasPermi('evaluation:final:list') or @ss.hasPermi('evaluation:result:list')")
    @GetMapping("/summary/{activityId}")
    public AjaxResult summary(@PathVariable Long activityId)
    {
        return success(finalEvaluationService.summary(activityId));
    }

    @PreAuthorize("@ss.hasPermi('evaluation:final:generate') or @ss.hasPermi('evaluation:result:calculate')")
    @Log(title = "FinalEvaluation generate", businessType = BusinessType.INSERT)
    @PostMapping("/generate/{activityId}")
    public AjaxResult generate(@PathVariable Long activityId,
            @RequestParam(value = "replaceExisting", required = false, defaultValue = "false") boolean replaceExisting)
    {
        return toAjax(finalEvaluationService.generateFromResult(activityId, getUsername(), replaceExisting));
    }

    @PreAuthorize("@ss.hasPermi('evaluation:final:edit')")
    @Log(title = "FinalEvaluation", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody FinalEvaluation finalEvaluation)
    {
        return toAjax(finalEvaluationService.updateFinalEvaluation(finalEvaluation, getUsername()));
    }

    @PreAuthorize("@ss.hasPermi('evaluation:final:edit')")
    @Log(title = "FinalEvaluation batch save", businessType = BusinessType.UPDATE)
    @PutMapping("/activity/{activityId}")
    public AjaxResult saveActivity(@PathVariable Long activityId, @RequestBody Map<String, Object> body)
    {
        List<FinalEvaluation> evaluations = com.alibaba.fastjson2.JSON.parseArray(
                com.alibaba.fastjson2.JSON.toJSONString(body.get("evaluations")), FinalEvaluation.class);
        String signedBy = body.get("signedBy") == null ? null : String.valueOf(body.get("signedBy"));
        String confirmRemark = body.get("confirmRemark") == null ? null : String.valueOf(body.get("confirmRemark"));
        return toAjax(finalEvaluationService.saveActivityFinalEvaluations(activityId, evaluations, signedBy, confirmRemark, getUsername()));
    }

    @PreAuthorize("@ss.hasPermi('evaluation:final:confirm')")
    @Log(title = "FinalEvaluation confirm", businessType = BusinessType.UPDATE)
    @PutMapping("/confirm/{activityId}")
    public AjaxResult confirm(@PathVariable Long activityId)
    {
        return toAjax(finalEvaluationService.confirmActivity(activityId, getUsername()));
    }

    @PreAuthorize("@ss.hasPermi('evaluation:final:remove')")
    @Log(title = "FinalEvaluation", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids)
    {
        return toAjax(finalEvaluationService.deleteFinalEvaluationByIds(ids));
    }
}
