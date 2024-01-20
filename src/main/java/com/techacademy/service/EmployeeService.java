package com.techacademy.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.techacademy.constants.ErrorKinds;
import com.techacademy.entity.Employee;
import com.techacademy.entity.Report;
import com.techacademy.repository.EmployeeRepository;

import org.springframework.transaction.annotation.Transactional;

@Service
public class EmployeeService {

    private final EmployeeRepository employeeRepository;
    private final PasswordEncoder passwordEncoder;
    private final ReportService reportService;

    @Autowired
    public EmployeeService(EmployeeRepository employeeRepository, PasswordEncoder passwordEncoder, ReportService reportService) {
        this.employeeRepository = employeeRepository;
        this.passwordEncoder = passwordEncoder;
        this.reportService = reportService;
    }

    // 従業員保存
    @Transactional
    public ErrorKinds save(Employee employee) {

        // パスワードチェック
        ErrorKinds result = employeePasswordCheck(employee);
        if (ErrorKinds.CHECK_OK != result) {
            return result;
        }

        // 従業員番号重複チェック
        if (findByCode(employee.getCode()) != null) {
            return ErrorKinds.DUPLICATE_ERROR;
        }

        employee.setDeleteFlg(false);

        LocalDateTime now = LocalDateTime.now();
        employee.setCreatedAt(now);
        employee.setUpdatedAt(now);

        employeeRepository.save(employee);
        return ErrorKinds.SUCCESS;
    }

    // 従業員削除
    @Transactional
    public ErrorKinds delete(String code, UserDetail userDetail) {

        // 自分を削除しようとした場合はエラーメッセージを表示
        if (code.equals(userDetail.getEmployee().getCode())) {
            return ErrorKinds.LOGINCHECK_ERROR;
        }
        Employee employee = findByCode(code);
        LocalDateTime now = LocalDateTime.now();
        employee.setUpdatedAt(now);
        employee.setDeleteFlg(true);

        // 削除対象の従業員（employee）に紐づいている、日報のリスト（reportList）を取得
        List<Report> reportList = reportService.findByEmployee(employee);

        // 日報のリスト（reportList）を拡張for文を使って繰り返し
        for (Report report : reportList) {
            // 日報（report）のIDを指定して、日報情報を削除
            reportService.delete(report.getId());
        }

        /* 削除対象の従業員に紐づいている日報情報の削除：ここまで */

        return ErrorKinds.SUCCESS;
    }

    // 従業員更新
    @Transactional
    public ErrorKinds update(String code ,Employee employee) {
        // フィールド(code,[name],[role],[[password]],delete_flg,created_at,[updated_at])
        // 更新用Employeeに更新元のデータを入れる
        Employee updateEmployee = findByCode(code);

        // フォームのパスワードが空白ならDBのパスワードを、そうでなければフォームのデータを更新用Employeeに入れる（パスワード）
        if ("".equals(employee.getPassword())) {
            // パスワードが空白だった場合
            updateEmployee.setPassword(findByCode(code).getPassword());
        } else {
            updateEmployee.setPassword(employee.getPassword());
            // パスワードチェック
            ErrorKinds result = employeePasswordCheck(employee);
            if (ErrorKinds.CHECK_OK != result) {
                return result;
            }
            updateEmployee.setPassword(employee.getPassword());
        }

        // フォームのデータを更新用Employeeに入れる（名前・権限）
        updateEmployee.setName(employee.getName());
        updateEmployee.setRole(employee.getRole());

        // 更新日時を現在日時に上書き
        LocalDateTime now = LocalDateTime.now();
        updateEmployee.setUpdatedAt(now);

        // 更新用Employeeの内容で保存
        employeeRepository.save(updateEmployee);
        return ErrorKinds.SUCCESS;
    }

    // 従業員一覧表示処理
    public List<Employee> findAll() {
        return employeeRepository.findAll();
    }

    // 1件を検索
    public Employee findByCode(String code) {
        // findByIdで検索
        Optional<Employee> option = employeeRepository.findById(code);
        // 取得できなかった場合はnullを返す
        Employee employee = option.orElse(null);
        return employee;
    }

    // 従業員パスワードチェック
    private ErrorKinds employeePasswordCheck(Employee employee) {

        // 従業員パスワードの半角英数字チェック処理
        if (isHalfSizeCheckError(employee)) {

            return ErrorKinds.HALFSIZE_ERROR;
        }

        // 従業員パスワードの8文字～16文字チェック処理
        if (isOutOfRangePassword(employee)) {

            return ErrorKinds.RANGECHECK_ERROR;
        }

        employee.setPassword(passwordEncoder.encode(employee.getPassword()));

        return ErrorKinds.CHECK_OK;
    }

    // 従業員パスワードの半角英数字チェック処理
    private boolean isHalfSizeCheckError(Employee employee) {

        // 半角英数字チェック
        Pattern pattern = Pattern.compile("^[A-Za-z0-9]+$");
        Matcher matcher = pattern.matcher(employee.getPassword());
        return !matcher.matches();
    }

    // 従業員パスワードの8文字～16文字チェック処理
    public boolean isOutOfRangePassword(Employee employee) {

        // 桁数チェック
        int passwordLength = employee.getPassword().length();
        return passwordLength < 8 || 16 < passwordLength;
    }


}
