
package com.techacademy.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import lombok.Data;

@Data
@JsonPropertyOrder({"ID", "社員コード", "社員氏名", "日付", "タイトル", "内容", "登録日時", "更新日時"})

public class ReportCsvColumn {
    @JsonProperty("ID")
    private Integer id;
    @JsonProperty("社員コード")
    private String code;
    @JsonProperty("社員氏名")
    private String name;
    @JsonProperty("日付")
    private String reportDate;
    @JsonProperty("タイトル")
    private String reportTitle;
    @JsonProperty("内容")
    private String reportContent;
    @JsonProperty("登録日時")
    private String reportCreated;
    @JsonProperty("更新日時")
    private String reportUpdated;

    public ReportCsvColumn () {}

    public ReportCsvColumn (Integer id, String code, String name, String reportDate, String reportTitle, String reportContent, String reportCreated, String reportUpdated) {
      this.id = id;
      this.code = code;
      this.name = name;
      this.reportDate = reportDate;
      this.reportTitle = reportTitle;
      this.reportContent = reportContent;
      this.reportCreated = reportCreated;
      this.reportUpdated = reportUpdated;
    }
}