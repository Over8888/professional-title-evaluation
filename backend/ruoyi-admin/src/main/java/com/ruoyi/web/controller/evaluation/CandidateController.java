package com.ruoyi.web.controller.evaluation;

import java.util.List;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.common.utils.poi.ExcelUtil;
import com.ruoyi.evaluation.domain.Candidate;
import com.ruoyi.evaluation.domain.ImportPreviewResult;
import com.ruoyi.evaluation.domain.SelectionRequest;
import com.ruoyi.evaluation.service.ICandidateService;

@RestController
@RequestMapping("/evaluation/candidate")
public class CandidateController extends BaseController
{
    @Autowired
    private ICandidateService candidateService;

    @PreAuthorize("@ss.hasPermi('evaluation:candidate:list')")
    @GetMapping("/list")
    public TableDataInfo list(Candidate candidate)
    {
        startPage();
        List<Candidate> list = candidateService.selectCandidateList(candidate);
        return getDataTable(list);
    }

    @PreAuthorize("@ss.hasPermi('evaluation:candidate:list')")
    @GetMapping("/options")
    public AjaxResult options()
    {
        return success(candidateService.selectCandidateOptions());
    }

    @Log(title = "Candidate", businessType = BusinessType.EXPORT)
    @PreAuthorize("@ss.hasPermi('evaluation:candidate:export')")
    @PostMapping("/export")
    public void export(HttpServletResponse response, Candidate candidate)
    {
        List<Candidate> list = candidateService.selectCandidateList(candidate);
        ExcelUtil<Candidate> util = new ExcelUtil<Candidate>(Candidate.class);
        util.exportExcel(response, list, "Candidate");
    }

    @PostMapping("/importTemplate")
    public void importTemplate(HttpServletResponse response)
    {
        ExcelUtil<Candidate> util = new ExcelUtil<Candidate>(Candidate.class);
        util.importTemplateExcel(response, "Candidate data");
    }

    @Log(title = "Candidate import preview", businessType = BusinessType.IMPORT)
    @PreAuthorize("@ss.hasPermi('evaluation:candidate:import')")
    @PostMapping("/importPreview")
    public AjaxResult importPreview(@RequestParam("activityId") Long activityId, @RequestParam("file") MultipartFile file) throws Exception
    {
        return success(candidateService.previewImport(activityId, file, getUsername()));
    }

    @Log(title = "Candidate import data", businessType = BusinessType.IMPORT)
    @PreAuthorize("@ss.hasPermi('evaluation:candidate:import')")
    @PostMapping("/importData")
    public AjaxResult importData(@RequestParam("activityId") Long activityId, MultipartFile file, boolean updateSupport) throws Exception
    {
        ImportPreviewResult result = candidateService.importData(activityId, file, updateSupport, getUsername());
        AjaxResult ajax = success(result);
        ajax.put("msg", buildImportMessage("候选人导入完成", result));
        return ajax;
    }

    @Log(title = "Candidate pool import data", businessType = BusinessType.IMPORT)
    @PreAuthorize("@ss.hasPermi('evaluation:candidate:import')")
    @PostMapping("/pool/importData")
    public AjaxResult importPoolData(MultipartFile file, boolean updateSupport) throws Exception
    {
        ImportPreviewResult result = candidateService.importPoolData(file, updateSupport, getUsername());
        AjaxResult ajax = success(result);
        ajax.put("msg", buildImportMessage("候选人资料库导入完成", result));
        return ajax;
    }

    @PreAuthorize("@ss.hasPermi('evaluation:candidate:add')")
    @Log(title = "Candidate select from pool", businessType = BusinessType.INSERT)
    @PostMapping("/activity/{activityId}/select")
    public AjaxResult selectFromPool(@PathVariable Long activityId, @RequestBody SelectionRequest request)
    {
        boolean replaceExisting = Boolean.TRUE.equals(request.getReplaceExisting());
        return toAjax(candidateService.selectFromPool(activityId, request.getIds(), replaceExisting, getUsername()));
    }

    @PreAuthorize("@ss.hasPermi('evaluation:candidate:add')")
    @Log(title = "Candidate copy from activity", businessType = BusinessType.INSERT)
    @PostMapping("/activity/{activityId}/copy/{sourceActivityId}")
    public AjaxResult copyFromActivity(@PathVariable Long activityId, @PathVariable Long sourceActivityId, @RequestBody(required = false) SelectionRequest request)
    {
        boolean replaceExisting = request != null && Boolean.TRUE.equals(request.getReplaceExisting());
        return toAjax(candidateService.copyFromActivity(activityId, sourceActivityId, replaceExisting, getUsername()));
    }

    @PreAuthorize("@ss.hasPermi('evaluation:candidate:query')")
    @GetMapping("/{id}")
    public AjaxResult getInfo(@PathVariable Long id)
    {
        return success(candidateService.selectCandidateById(id));
    }

    @PreAuthorize("@ss.hasPermi('evaluation:candidate:add')")
    @Log(title = "Candidate", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@Validated @RequestBody Candidate candidate)
    {
        candidate.setCreateBy(getUsername());
        return toAjax(candidateService.insertCandidate(candidate));
    }

    @PreAuthorize("@ss.hasPermi('evaluation:candidate:edit')")
    @Log(title = "Candidate", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@Validated @RequestBody Candidate candidate)
    {
        candidate.setUpdateBy(getUsername());
        return toAjax(candidateService.updateCandidate(candidate));
    }

    @PreAuthorize("@ss.hasPermi('evaluation:candidate:remove')")
    @Log(title = "Candidate", businessType = BusinessType.DELETE)
    @DeleteMapping("/clear/{activityId}")
    public AjaxResult clear(@PathVariable Long activityId)
    {
        return toAjax(candidateService.deleteCandidateByActivityId(activityId));
    }

    @PreAuthorize("@ss.hasPermi('evaluation:candidate:remove')")
    @Log(title = "Candidate", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids)
    {
        return toAjax(candidateService.deleteCandidateByIds(ids));
    }

    private String buildImportMessage(String title, ImportPreviewResult result)
    {
        int imported = result == null ? 0 : result.getImportedRows();
        StringBuilder message = new StringBuilder(title).append("，成功处理 ").append(Math.max(imported, 0)).append(" 条。");
        if (result != null && result.getErrors() != null && !result.getErrors().isEmpty())
        {
            message.append("\n存在 ").append(result.getErrors().size()).append(" 条提示：");
            int limit = Math.min(10, result.getErrors().size());
            for (int i = 0; i < limit; i++)
            {
                ImportPreviewResult.ImportError error = result.getErrors().get(i);
                message.append("\n")
                       .append(error.getSheetName() == null ? "" : error.getSheetName())
                       .append(error.getRowNo() == null ? "" : " 第" + error.getRowNo() + "行")
                       .append(error.getField() == null ? "" : " [" + error.getField() + "]")
                       .append("：")
                       .append(error.getReason());
            }
            if (result.getErrors().size() > limit)
            {
                message.append("\n其余提示已省略。");
            }
        }
        return message.toString();
    }
}
