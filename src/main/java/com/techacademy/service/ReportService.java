package com.techacademy.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.data.domain.Pageable;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.techacademy.constants.ErrorKinds;
import com.techacademy.entity.ReportCSV;
import com.techacademy.entity.ReportCsvColumn;
import com.techacademy.entity.Employee;
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
    public ErrorKinds save(Report report, UserDetail userDetail) {

        // 入力チェック（ログインユーザーが同一の日付で登録をしようとしていないか）
        if (existsByEmployeeAndReportDate(userDetail, report)) {
            return ErrorKinds.DATECHECK_ERROR;
        }

        // 社員番号（ログイン中の従業員の社員番号）
        report.setEmployee(userDetail.getEmployee());

        report.setDeleteFlg(false);

        LocalDateTime now = LocalDateTime.now();
        report.setCreatedAt(now);
        report.setUpdatedAt(now);

        reportRepository.save(report);
        return ErrorKinds.SUCCESS;
    }

    // 日報削除
    @Transactional
    public ErrorKinds delete(int id) {

        Report report = findById(id);
        LocalDateTime now = LocalDateTime.now();
        report.setUpdatedAt(now);
        report.setDeleteFlg(true);

        return ErrorKinds.SUCCESS;
    }

    // 日報更新
    @Transactional
    public ErrorKinds update(int id, Report report, UserDetail userDetail) {

        // 更新用Reportに更新元のデータを入れる
        Report updateReport = findById(id);

        // フォームのデータを更新用Reportに入れる（日付・タイトル・内容）
        updateReport.setReportDate(report.getReportDate());
        updateReport.setTitle(report.getTitle());
        updateReport.setContent(report.getContent());

        // 更新日時を現在日時に上書き
        LocalDateTime now = LocalDateTime.now();
        updateReport.setUpdatedAt(now);

        // 更新用Reportの内容で保存
        reportRepository.save(updateReport);
        return ErrorKinds.SUCCESS;
    }

    // 日報一覧表示処理
    public List<Report> findAll() {
        return reportRepository
                .findAll(Sort.by(Sort.Direction.DESC, "reportDate"));
    }

    // 日報一覧表示処理ページング処理追加
    public Page<Report> findAll(Pageable pageable) {
        return reportRepository
                .findAllByOrderByReportDateDesc(pageable);
    }

    // 日報一覧表示処理（ログインユーザーのみ）
    public List<Report> findByEmployee(UserDetail userDetail) {
        return reportRepository.findByEmployeeOrderByReportDateDesc(userDetail.getEmployee());
    }

    // 日報一覧表示処理（ログインユーザーのみ）ページング処理追加
    public Page<Report> findByEmployee(UserDetail userDetail, Pageable pageable) {
        return reportRepository.findByEmployeeOrderByReportDateDesc(userDetail.getEmployee(), pageable);
    }

    // 日報一覧表示処理（従業員削除用）
    public List<Report> findByEmployee(Employee employee) {
        return reportRepository.findByEmployee(employee);
    }

    // 1件を検索
    public Report findById(int id) {
        // findByIdで検索
        Optional<Report> option = reportRepository.findById(id);
        // 取得できなかった場合はnullを返す
        Report report = option.orElse(null);
        return report;
    }
    // CSV出力処理
    public List<ReportCsvColumn> csvExport(ReportCSV records) throws JsonProcessingException {
        List<ReportCsvColumn> csvList = new ArrayList<>();
        for (int i = 0; i < records.getId().size(); i++) {
            String strReportDate = records.getReportDate().get(i).replace("-", "/");
            String strReportCreated = records.getReportCreated().get(i).replace("-", "/").replace("T", " ");
            int RClength = strReportCreated.length();
            if (RClength == 16) {
                strReportCreated = strReportCreated + ":00";
            }
            String strReportUpdated = records.getReportUpdated().get(i).replace("-", "/").replace("T", " ");
            int RUlength = strReportUpdated.length();
            if (RUlength == 16) {
                strReportUpdated = strReportUpdated + ":00";
            }
            csvList.add(new ReportCsvColumn(records.getId().get(i), records.getCode().get(i), records.getName().get(i),
                    strReportDate, records.getReportTitle().get(i), records.getReportContent().get(i),
                    strReportCreated, strReportUpdated));
        }
        return  csvList;
    }

    // ログイン中のユーザー かつ 入力した日付 の日報データが日報テーブルにないかの確認
    public boolean existsByEmployeeAndReportDate(UserDetail userDetail, Report report) {
        return reportRepository.existsByEmployeeAndReportDate(userDetail.getEmployee(), report.getReportDate());
    }

}
