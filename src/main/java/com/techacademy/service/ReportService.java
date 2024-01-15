package com.techacademy.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.techacademy.constants.ErrorKinds;
import com.techacademy.entity.Report;
import com.techacademy.repository.ReportRepository;


import org.springframework.transaction.annotation.Transactional;

@Service
public class ReportService {

    private final ReportRepository reportRepository;

    @Autowired
    public ReportService(ReportRepository reportRepository) {
        this.reportRepository = reportRepository;
    }

    // 日報保存
    @Transactional
    public ErrorKinds save(Report report) {

        // 日報番号重複チェック
        /*if (findByCode(report.getCode()) != null) {
            return ErrorKinds.DUPLICATE_ERROR;
        }
        */

        report.setDeleteFlg(false);

        LocalDateTime now = LocalDateTime.now();
        report.setCreatedAt(now);
        report.setUpdatedAt(now);

        reportRepository.save(report);
        return ErrorKinds.SUCCESS;
    }

    // 日報削除
    @Transactional
    public ErrorKinds delete(int id, UserDetail userDetail) {

        Report report = findById(id);
        LocalDateTime now = LocalDateTime.now();
        report.setUpdatedAt(now);
        report.setDeleteFlg(true);

        return ErrorKinds.SUCCESS;
    }

    // 日報更新  名前空欄→エラーコメント出ずに更新画面に戻る
    @Transactional
    public ErrorKinds update(int id ,Report Report) {
        // フィールド(code,[name],[role],[[password]],delete_flg,created_at,[updated_at])
        // 更新用Reportに更新元のデータを入れる
        Report updateReport = findById(id);


        /* フォームのデータを更新用Reportに入れる（名前・権限）
        updateReport.setName(report.getName());
        updateReport.setRole(report.getRole());
        */

        // 更新日時を現在日時に上書き
        LocalDateTime now = LocalDateTime.now();
        updateReport.setUpdatedAt(now);

        // 更新用Reportの内容で保存
        reportRepository.save(updateReport);
        return ErrorKinds.SUCCESS;
    }

    // 日報一覧表示処理
    public List<Report> findAll() {
        return reportRepository.findAll();
    }
   /*
    // 日報一覧表示処理
    public List<Report> findByEmployee(UserDetail userDetail) {
        List<Report> reportList = reportService.findByEmployee(employee);
        return reportRepository.findByEmployee(employee);
    }
*/

    // 1件を検索
    public Report findById(int id) {
        // findByIdで検索
        Optional<Report> option = reportRepository.findById(id);
        // 取得できなかった場合はnullを返す
        Report report = option.orElse(null);
        return report;
    }


}
