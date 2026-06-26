package com.ruoyi.web.controller.evaluation;

import java.util.List;
import jakarta.servlet.http.HttpServletRequest;
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
import com.ruoyi.evaluation.domain.SelectionRequest;
import com.ruoyi.evaluation.domain.Voter;
import com.ruoyi.evaluation.service.IVoterService;

@RestController
@RequestMapping("/evaluation/voter")
public class VoterController extends BaseController
{
    @Autowired
    private IVoterService voterService;

    @PreAuthorize("@ss.hasPermi('evaluation:voter:list')")
    @GetMapping("/list")
    public TableDataInfo list(Voter voter)
    {
        startPage();
        List<Voter> list = voterService.selectVoterList(voter);
        return getDataTable(list);
    }

    @PreAuthorize("@ss.hasPermi('evaluation:voter:list')")
    @GetMapping("/options")
    public AjaxResult options()
    {
        return success(voterService.selectVoterOptions());
    }

    @Log(title = "Voter", businessType = BusinessType.EXPORT)
    @PreAuthorize("@ss.hasPermi('evaluation:voter:export')")
    @PostMapping("/export")
    public void export(HttpServletResponse response, Voter voter)
    {
        List<Voter> list = voterService.selectVoterList(voter);
        ExcelUtil<Voter> util = new ExcelUtil<Voter>(Voter.class);
        util.exportExcel(response, list, "Voter");
    }

    @PostMapping("/importTemplate")
    public void importTemplate(HttpServletResponse response)
    {
        ExcelUtil<Voter> util = new ExcelUtil<Voter>(Voter.class);
        util.importTemplateExcel(response, "Voter data");
    }

    @Log(title = "Voter import preview", businessType = BusinessType.IMPORT)
    @PreAuthorize("@ss.hasPermi('evaluation:voter:import')")
    @PostMapping("/importPreview")
    public AjaxResult importPreview(@RequestParam("activityId") Long activityId, @RequestParam("file") MultipartFile file) throws Exception
    {
        return success(voterService.previewImport(activityId, file, getUsername()));
    }

    @Log(title = "Voter import data", businessType = BusinessType.IMPORT)
    @PreAuthorize("@ss.hasPermi('evaluation:voter:import')")
    @PostMapping("/importData")
    public AjaxResult importData(@RequestParam("activityId") Long activityId, MultipartFile file, boolean updateSupport) throws Exception
    {
        AjaxResult ajax = success(voterService.importData(activityId, file, updateSupport, getUsername()));
        ajax.put("msg", "Voter import completed.");
        return ajax;
    }

    @Log(title = "Voter pool import data", businessType = BusinessType.IMPORT)
    @PreAuthorize("@ss.hasPermi('evaluation:voter:import')")
    @PostMapping("/pool/importData")
    public AjaxResult importPoolData(MultipartFile file, boolean updateSupport) throws Exception
    {
        AjaxResult ajax = success(voterService.importPoolData(file, updateSupport, getUsername()));
        ajax.put("msg", "评委资料库导入完成");
        return ajax;
    }

    @PreAuthorize("@ss.hasPermi('evaluation:voter:add')")
    @Log(title = "Voter select from pool", businessType = BusinessType.INSERT)
    @PostMapping("/activity/{activityId}/select")
    public AjaxResult selectFromPool(@PathVariable Long activityId, @RequestBody SelectionRequest request)
    {
        boolean replaceExisting = Boolean.TRUE.equals(request.getReplaceExisting());
        return toAjax(voterService.selectFromPool(activityId, request.getIds(), replaceExisting, getUsername()));
    }

    @PreAuthorize("@ss.hasPermi('evaluation:voter:add')")
    @Log(title = "Voter copy from activity", businessType = BusinessType.INSERT)
    @PostMapping("/activity/{activityId}/copy/{sourceActivityId}")
    public AjaxResult copyFromActivity(@PathVariable Long activityId, @PathVariable Long sourceActivityId, @RequestBody(required = false) SelectionRequest request)
    {
        boolean replaceExisting = request != null && Boolean.TRUE.equals(request.getReplaceExisting());
        return toAjax(voterService.copyFromActivity(activityId, sourceActivityId, replaceExisting, getUsername()));
    }

    @PreAuthorize("@ss.hasPermi('evaluation:voter:query')")
    @GetMapping("/{id}")
    public AjaxResult getInfo(@PathVariable Long id)
    {
        return success(voterService.selectVoterById(id));
    }

    @PreAuthorize("@ss.hasPermi('evaluation:voter:add')")
    @Log(title = "Voter", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@Validated @RequestBody Voter voter)
    {
        voter.setCreateBy(getUsername());
        return toAjax(voterService.insertVoter(voter));
    }

    @PreAuthorize("@ss.hasPermi('evaluation:voter:edit')")
    @Log(title = "Voter", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@Validated @RequestBody Voter voter)
    {
        voter.setUpdateBy(getUsername());
        return toAjax(voterService.updateVoter(voter));
    }

    @PreAuthorize("@ss.hasPermi('evaluation:voter:token')")
    @Log(title = "Voter token", businessType = BusinessType.UPDATE)
    @PutMapping("/{id}/token")
    public AjaxResult token(@PathVariable Long id)
    {
        return toAjax(voterService.regenerateVoteToken(id, getUsername()));
    }

    @PreAuthorize("@ss.hasPermi('evaluation:voter:list')")
    @GetMapping("/progress/{activityId}")
    public AjaxResult progress(@PathVariable Long activityId)
    {
        return success(voterService.selectVoterProgress(activityId));
    }

    @PreAuthorize("@ss.hasPermi('evaluation:voter:list')")
    @GetMapping("/activity/{activityId}/links")
    public AjaxResult links(@PathVariable Long activityId, HttpServletRequest request)
    {
        return success(voterService.selectVoteLinks(activityId, frontendBaseUrl(request)));
    }

    @PreAuthorize("@ss.hasPermi('evaluation:voter:remove')")
    @Log(title = "Voter", businessType = BusinessType.DELETE)
    @DeleteMapping("/clear/{activityId}")
    public AjaxResult clear(@PathVariable Long activityId)
    {
        return toAjax(voterService.deleteVoterByActivityId(activityId));
    }

    @PreAuthorize("@ss.hasPermi('evaluation:voter:remove')")
    @Log(title = "Voter", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids)
    {
        return toAjax(voterService.deleteVoterByIds(ids));
    }

    private String frontendBaseUrl(HttpServletRequest request)
    {
        String origin = request.getHeader("Origin");
        if (origin != null && origin.length() > 0)
        {
            return origin;
        }
        if ("localhost".equals(request.getServerName()) && request.getServerPort() == 8080)
        {
            return "http://localhost:8081";
        }
        return request.getScheme() + "://" + request.getServerName() + ":" + request.getServerPort();
    }
}
