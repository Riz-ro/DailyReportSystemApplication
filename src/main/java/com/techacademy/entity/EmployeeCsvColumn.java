
package com.techacademy.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import lombok.Data;

@Data
@JsonPropertyOrder({"ID", "名前", "権限", "パスワード", "登録日時", "更新日時", "削除フラグ"})

public class EmployeeCsvColumn {
    @JsonProperty("ID")
    private String code;
    @JsonProperty("名前")
    private String name;
    @JsonProperty("権限")
    private String role;
    @JsonProperty("パスワード")
    private String password;
    @JsonProperty("登録日時")
    private String employeeCreated;
    @JsonProperty("更新日時")
    private String employeeUpdated;
    @JsonProperty("削除フラグ")
    private String deleteFlg;

    public EmployeeCsvColumn () {}

    public EmployeeCsvColumn (String code, String name, String role, String password, String employeeCreated, String employeeUpdated,String deleteFlg) {
      this.code = code;
      this.name = name;
      this.role = role;
      this.password = password;
      this.employeeCreated = employeeCreated;
      this.employeeUpdated = employeeUpdated;
      this.deleteFlg = deleteFlg;
    }
}