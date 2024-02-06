
package com.techacademy.entity;

import java.util.List;

import lombok.Data;

@Data

public class CSV {
    // ID
    List<Integer> id;
    // 社員氏名
    List<String> name;
    // 日付
    List<String> reportDate;
    // タイトル
    List<String> reportTitle;
    // 内容
    List<String> reportContent;
    // 登録日時
    List<String> reportCreated;
    // 更新日時
    List<String> reportUpdated;
}