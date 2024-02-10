
package com.techacademy.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import lombok.Data;

@Data

public class CSV {
    // ID
    List<Integer> id;
    // 社員コード
    List<String> code;
    // 社員氏名
    List<String> name;
    // 日付
    List<LocalDate> reportDate;
    // タイトル
    List<String> reportTitle;
    // 内容
    List<String> reportContent;
    // 登録日時
    List<LocalDateTime> reportCreated;
    // 更新日時
    List<LocalDateTime> reportUpdated;
}