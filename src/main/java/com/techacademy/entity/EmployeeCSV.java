
package com.techacademy.entity;

import java.util.List;

import lombok.Data;

@Data

public class EmployeeCSV {
    // ID（社員コード）
    List<String> code;
    // 名前
    List<String> name;
    // 権限
    List<String> role;
    // パスワード
    List<String> password;
    // 登録日時
    List<String> employeeCreated;
    // 更新日時
    List<String> employeeUpdated;
    // 削除フラグ
    List<String> deleteFlg;
}