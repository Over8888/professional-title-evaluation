package com.ruoyi.evaluation.domain;

import java.io.Serializable;
import java.util.List;

public class SelectionRequest implements Serializable
{
    private static final long serialVersionUID = 1L;

    private List<Long> ids;
    private Boolean replaceExisting;

    public List<Long> getIds() { return ids; }
    public void setIds(List<Long> ids) { this.ids = ids; }
    public Boolean getReplaceExisting() { return replaceExisting; }
    public void setReplaceExisting(Boolean replaceExisting) { this.replaceExisting = replaceExisting; }
}
