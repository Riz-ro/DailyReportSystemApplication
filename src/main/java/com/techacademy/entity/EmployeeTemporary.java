
package com.techacademy.entity;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import org.hibernate.validator.constraints.Length;

import com.techacademy.entity.Employee.Role;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

@Data
@Entity
@Table(name = "employeesTemporary")
public class EmployeeTemporary {

    // ID
    @Id
    @Column(length = 10)
    @NotEmpty
    @Length(max = 10)
    private String code;

    // 名前
    @Column(length = 20, nullable = false)
    @NotEmpty
    @Length(max = 20)
    private String name;

    // 権限
    @Column(columnDefinition="VARCHAR(10)", nullable = false)
    @Enumerated(EnumType.STRING)
    private Role role;

    // パスワード
    @Column(length = 255, nullable = false)
    private String password;

    // 削除フラグ(論理削除を行うため)
    @Column(columnDefinition="TINYINT", nullable = false)
    private boolean deleteFlg;

    // 登録日時
    @Column
    private LocalDateTime createdAt;

    // 更新日時
    @Column
    private LocalDateTime updatedAt;

    // CSVデータチェックフラグ（入力チェック用）
    @Column(columnDefinition="TINYINT", nullable = false)
    private boolean csvFlg;

    // データ更新有無フラグ
    @Column(columnDefinition="TINYINT", nullable = false)
    private boolean sameFlg;

    // save処理チェックフラグ（新規＆既存更新ありでupdate=true）
    @Column(columnDefinition="TINYINT", nullable = false)
    private boolean updateFlg;

    // name変更箇所チェックフラグ（既存レコード用）
    @Column(columnDefinition="TINYINT", nullable = false)
    private boolean nameChangeFlg;

    // role変更箇所チェックフラグ（既存レコード用）
    @Column(columnDefinition="TINYINT", nullable = false)
    private boolean roleChangeFlg;

    // pass変更箇所チェックフラグ（既存レコード用）
    @Column(columnDefinition="TINYINT", nullable = false)
    private boolean passChangeFlg;

    // deleteFlg変更箇所チェックフラグ（既存レコード用）
    @Column(columnDefinition="TINYINT", nullable = false)
    private boolean deleteFlgChangeFlg;

    // ステータス
    @Column
    private String resultStatus;
    // パスワードステータス
    @Column
    private String passStatus;
    // 削除フラグ変更ステータス
    @Column
    private String deleteStatus;

    // 変更前氏名
    @Column
    private String formerName;
    // 変更前権限
    @Column
    private Role formerRole;

    public LocalDateTime getCreatedAt() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss");
        LocalDateTime date = createdAt;
        String text = date.format(formatter);
        LocalDateTime createdAt = LocalDateTime.parse(text, formatter);
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss");
        LocalDateTime date = updatedAt;
        String text = date.format(formatter);
        LocalDateTime updatedAt = LocalDateTime.parse(text, formatter);
        return updatedAt;
    }
}