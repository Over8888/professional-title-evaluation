package com.ruoyi.web.controller.evaluation;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.ruoyi.common.annotation.Anonymous;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.evaluation.domain.VoteConfirmRequest;
import com.ruoyi.evaluation.domain.VoteSubmitRequest;
import com.ruoyi.evaluation.service.IVoteService;

@Anonymous
@RestController
@RequestMapping("/evaluation/vote")
public class VoteController extends BaseController
{
    @Autowired
    private IVoteService voteService;

    @GetMapping("/entry/{token}")
    public AjaxResult entry(@PathVariable String token)
    {
        return success(voteService.getEntry(token, false));
    }

    @PostMapping("/entry/{token}/confirm")
    public AjaxResult confirm(@PathVariable String token, @RequestBody VoteConfirmRequest request)
    {
        return success(voteService.confirm(token, request));
    }

    @GetMapping("/entry/{token}/candidates")
    public AjaxResult candidates(@PathVariable String token)
    {
        return success(voteService.listCandidates(token));
    }

    @PostMapping("/entry/{token}/submit")
    public AjaxResult submit(@PathVariable String token, @RequestBody VoteSubmitRequest request)
    {
        return success(voteService.submit(token, request));
    }

    @GetMapping("/entry/{token}/result")
    public AjaxResult result(@PathVariable String token)
    {
        return success(voteService.result(token));
    }
}
