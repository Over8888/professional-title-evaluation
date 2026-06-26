package com.ruoyi.evaluation.domain;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ImportPreviewResult
{
    private String fileName;
    private Long activityId;
    private String importType;
    private int sheetCount;
    private int totalRows;
    private int previewRows;
    private List<SheetPreview> sheets = new ArrayList<>();
    private List<ImportError> errors = new ArrayList<>();

    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }
    public Long getActivityId() { return activityId; }
    public void setActivityId(Long activityId) { this.activityId = activityId; }
    public String getImportType() { return importType; }
    public void setImportType(String importType) { this.importType = importType; }
    public int getSheetCount() { return sheetCount; }
    public void setSheetCount(int sheetCount) { this.sheetCount = sheetCount; }
    public int getTotalRows() { return totalRows; }
    public void setTotalRows(int totalRows) { this.totalRows = totalRows; }
    public int getPreviewRows() { return previewRows; }
    public void setPreviewRows(int previewRows) { this.previewRows = previewRows; }
    public List<SheetPreview> getSheets() { return sheets; }
    public void setSheets(List<SheetPreview> sheets) { this.sheets = sheets; }
    public List<ImportError> getErrors() { return errors; }
    public void setErrors(List<ImportError> errors) { this.errors = errors; }

    public static class SheetPreview
    {
        private String sheetName;
        private int headerRowNo;
        private List<String> headers = new ArrayList<>();
        private List<Map<String, String>> rows = new ArrayList<>();

        public String getSheetName() { return sheetName; }
        public void setSheetName(String sheetName) { this.sheetName = sheetName; }
        public int getHeaderRowNo() { return headerRowNo; }
        public void setHeaderRowNo(int headerRowNo) { this.headerRowNo = headerRowNo; }
        public List<String> getHeaders() { return headers; }
        public void setHeaders(List<String> headers) { this.headers = headers; }
        public List<Map<String, String>> getRows() { return rows; }
        public void setRows(List<Map<String, String>> rows) { this.rows = rows; }
    }

    public static class ImportError
    {
        private String sheetName;
        private Integer rowNo;
        private String field;
        private String reason;

        public ImportError() {}

        public ImportError(String sheetName, Integer rowNo, String field, String reason)
        {
            this.sheetName = sheetName;
            this.rowNo = rowNo;
            this.field = field;
            this.reason = reason;
        }

        public String getSheetName() { return sheetName; }
        public void setSheetName(String sheetName) { this.sheetName = sheetName; }
        public Integer getRowNo() { return rowNo; }
        public void setRowNo(Integer rowNo) { this.rowNo = rowNo; }
        public String getField() { return field; }
        public void setField(String field) { this.field = field; }
        public String getReason() { return reason; }
        public void setReason(String reason) { this.reason = reason; }
    }
}
