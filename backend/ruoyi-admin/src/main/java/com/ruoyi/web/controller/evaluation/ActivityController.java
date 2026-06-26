package com.ruoyi.web.controller.evaluation;

import java.util.List;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpServletRequest;
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
import com.ruoyi.evaluation.domain.Activity;
import com.ruoyi.evaluation.domain.ActivityCreateRequest;
import com.ruoyi.evaluation.service.IActivityService;
import com.ruoyi.evaluation.service.IResultAggService;

@RestController
@RequestMapping("/evaluation/activity")
public class ActivityController extends BaseController
{
    @Autowired
    private IActivityService activityService;

    @Autowired
    private IResultAggService resultAggService;

    @PreAuthorize("@ss.hasPermi('evaluation:activity:list')")
    @GetMapping("/list")
    public TableDataInfo list(Activity activity)
    {
        startPage();
        List<Activity> list = activityService.selectActivityList(activity);
        return getDataTable(list);
    }

    @Log(title = "Activity", businessType = BusinessType.EXPORT)
    @PreAuthorize("@ss.hasPermi('evaluation:activity:export')")
    @PostMapping("/export")
    public void export(HttpServletResponse response, Activity activity)
    {
        List<Activity> list = activityService.selectActivityList(activity);
        ExcelUtil<Activity> util = new ExcelUtil<Activity>(Activity.class);
        util.exportExcel(response, list, "Activity");
    }

    @PreAuthorize("@ss.hasPermi('evaluation:activity:query')")
    @GetMapping("/{id}")
    public AjaxResult getInfo(@PathVariable Long id)
    {
        return success(activityService.selectActivityById(id));
    }

    @PreAuthorize("@ss.hasPermi('evaluation:activity:add')")
    @Log(title = "Activity", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@Validated @RequestBody Activity activity)
    {
        return AjaxResult.error("创建活动必须同时提交规则配置，请使用 createWithRule 接口。");
    }

    @PreAuthorize("@ss.hasPermi('evaluation:activity:add')")
    @Log(title = "Activity create with rule", businessType = BusinessType.INSERT)
    @PostMapping("/createWithRule")
    public AjaxResult createWithRule(@Validated @RequestBody ActivityCreateRequest request)
    {
        return success(activityService.createWithRule(request, getUsername()));
    }

    @PreAuthorize("@ss.hasPermi('evaluation:activity:edit')")
    @Log(title = "Activity", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@Validated @RequestBody Activity activity)
    {
        activity.setUpdatedBy(getUsername());
        return toAjax(activityService.updateActivity(activity));
    }

    @PreAuthorize("@ss.hasPermi('evaluation:activity:publish')")
    @Log(title = "Activity publish", businessType = BusinessType.UPDATE)
    @PutMapping("/publish/{id}")
    public AjaxResult publish(@PathVariable Long id, HttpServletRequest request)
    {
        return success(activityService.publishActivity(id, getUsername(), frontendBaseUrl(request)));
    }

    @PreAuthorize("@ss.hasPermi('evaluation:activity:close')")
    @Log(title = "Activity close", businessType = BusinessType.UPDATE)
    @PutMapping("/close/{id}")
    public AjaxResult close(@PathVariable Long id)
    {
        return toAjax(activityService.updateActivityStatus(id, "CLOSED", getUsername()));
    }

    @PreAuthorize("@ss.hasPermi('evaluation:activity:calculate')")
    @Log(title = "Activity calculated", businessType = BusinessType.UPDATE)
    @PutMapping("/calculated/{id}")
    public AjaxResult calculated(@PathVariable Long id)
    {
        return success(resultAggService.calculate(id, getUsername(), false));
    }

    @PreAuthorize("@ss.hasPermi('evaluation:activity:archive')")
    @Log(title = "Activity archive", businessType = BusinessType.UPDATE)
    @PutMapping("/archive/{id}")
    public AjaxResult archive(@PathVariable Long id)
    {
        Activity activity = new Activity();
        activity.setId(id);
        activity.setStatus("ARCHIVED");
        activity.setArchived("1");
        activity.setUpdatedBy(getUsername());
        return toAjax(activityService.updateActivity(activity));
    }

    @PreAuthorize("@ss.hasPermi('evaluation:activity:remove')")
    @Log(title = "Activity", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids)
    {
        return toAjax(activityService.deleteActivityByIds(ids));
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
