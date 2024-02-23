package com.techacademy.service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
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
import com.techacademy.entity.CSV;
import com.techacademy.entity.CsvColumn;
import com.techacademy.entity.Employee;
import com.techacademy.entity.Report;
import com.techacademy.repository.ReportRepository;

import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

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
    public List<CsvColumn> csvExport(CSV records) throws JsonProcessingException {
        List<CsvColumn> csvList = new ArrayList<>();
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
            csvList.add(new CsvColumn(records.getId().get(i), records.getCode().get(i), records.getName().get(i),
                    strReportDate, records.getReportTitle().get(i), records.getReportContent().get(i),
                    strReportCreated, strReportUpdated));
        }
        return  csvList;
    }

    // CSV入力用
    @Transactional
    public void csvImport(MultipartFile file){
        try (InputStream inputStream = file.getInputStream();
                BufferedReader br = new BufferedReader(new InputStreamReader(inputStream))) {

            // 読み取ったCSVの行を入れるための変数を作成
            String line;
            // ヘッダーレコードを飛ばすためにあらかじめ１行だけ読み取っておく（ない場合は不要）
            line = br.readLine();
            // 行がNULL（CSVの値がなくなる）になるまで処理を繰り返す
            while ((line = br.readLine()) != null) {
                // Stringのsplitメソッドを使用してカンマごとに分割して配列にいれる
                String[] csvSplit = line.split(",");
                Report report = new Report();
                report.setId(Integer.parseInt(csvSplit[0]));
                Employee employee = new Employee();
                employee.setCode(csvSplit[1]);
                report.setEmployee(employee);
                report.setReportDate(LocalDate.parse(csvSplit[3], DateTimeFormatter.ofPattern("yyyy/[]M/[]d")));
                report.setTitle(csvSplit[4]);
                report.setContent(csvSplit[5].replaceAll("^\"" , "").replaceAll("\"$" , ""));
                String strReportCreated = csvSplit[6].replaceAll("^\"" , "").replaceAll("\"$" , "");
                report.setCreatedAt(
                        LocalDateTime.parse(strReportCreated, DateTimeFormatter.ofPattern("yyyy/[]M/[]d []H:[]m:[]s")));
                String strReportUpdated = csvSplit[7].replaceAll("^\"" , "").replaceAll("\"$" , "");
                report.setUpdatedAt(
                        LocalDateTime.parse(strReportUpdated, DateTimeFormatter.ofPattern("yyyy/[]M/[]d []H:[]m:[]s")));
                report.setDeleteFlg(false);
                reportRepository.save(report);
            }
            br.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    // 日報インポート
    @Transactional
    public void save(Report report) {
        Report importReport = report;
        reportRepository.save(importReport);
    }

    // ログイン中のユーザー かつ 入力した日付 の日報データが日報テーブルにないかの確認
    public boolean existsByEmployeeAndReportDate(UserDetail userDetail, Report report) {
        return reportRepository.existsByEmployeeAndReportDate(userDetail.getEmployee(), report.getReportDate());
    }

}
