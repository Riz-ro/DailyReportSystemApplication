package com.techacademy.service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.techacademy.constants.ErrorKinds;
import com.techacademy.entity.Employee;
import com.techacademy.entity.Employee.Role;
import com.techacademy.entity.EmployeeCSV;
import com.techacademy.entity.EmployeeCsvColumn;
import com.techacademy.repository.EmployeeRepository;

import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class EmployeeService {

    private final EmployeeRepository employeeRepository;
    private final PasswordEncoder passwordEncoder;

    @Autowired
    public EmployeeService(EmployeeRepository employeeRepository, PasswordEncoder passwordEncoder) {
        this.employeeRepository = employeeRepository;
        this.passwordEncoder = passwordEncoder;
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

        //// 従業員を削除したら対象従業員の日報削除…は現実的でないので日報は削除しない2024/02/25
        /*
        // 削除対象の従業員（employee）に紐づいている、日報のリスト（reportList）を取得
        List<Report> reportList = reportService.findByEmployee(employee);

        // 日報のリスト（reportList）を拡張for文を使って繰り返し
        for (Report report : reportList) {
            // 日報（report）のIDを指定して、日報情報を削除
            reportService.delete(report.getId());
        }
        */

        return ErrorKinds.SUCCESS;
    }

    // 従業員更新
    @Transactional
    public ErrorKinds update(String code, Employee employee) {
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

    // 従業員一覧表示処理＆削除フラグ検索
    public List<Employee> findAllByDeleteFlgFalse() {
        return employeeRepository.findAllByDeleteFlgFalse();
    }

    // 日報一覧表示処理ページング処理追加
    public Page<Employee> findAll(Pageable pageable) {
        return employeeRepository.findAll(pageable);
    }

    // 日報一覧表示処理ページング処理追加＆削除フラグ検索
    public Page<Employee> findAllByDeleteFlgFalse(Pageable pageable) {
        return employeeRepository.findAllByDeleteFlgFalse(pageable);
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

    // ReportCSV入力用
    public Employee findByEmployee(String code) {
        // findByIdで検索
        Optional<Employee> option = employeeRepository.findById(code);
        // 取得できなかった場合はnullを返す
        Employee employee = option.orElse(null);
        return employee;
    }

    // CSV出力処理
    public List<EmployeeCsvColumn> csvExport(EmployeeCSV records) throws JsonProcessingException {
        List<EmployeeCsvColumn> csvList = new ArrayList<>();
        for (int i = 0; i < records.getCode().size(); i++) {
            String strEmployeeCreated = records.getEmployeeCreated().get(i).replace("-", "/").replace("T", " ");
            int RClength = strEmployeeCreated.length();
            if (RClength == 16) {
                strEmployeeCreated = strEmployeeCreated + ":00";
            }
            String strEmployeeUpdated = records.getEmployeeUpdated().get(i).replace("-", "/").replace("T", " ");
            int RUlength = strEmployeeUpdated.length();
            if (RUlength == 16) {
                strEmployeeUpdated = strEmployeeUpdated + ":00";
            }
            String strDeleteFlg = String.valueOf(records.getDeleteFlg().get(i));  // true or false
            csvList.add(new EmployeeCsvColumn(records.getCode().get(i), records.getName().get(i),
                    records.getRole().get(i), null, strEmployeeCreated, strEmployeeUpdated ,strDeleteFlg));
        }
        return csvList;
    }

    // CSV入力用
    @Transactional
    public void csvImport(MultipartFile file) {
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
                if (findByCode(csvSplit[0]) == null) {  // 新規登録
                    Employee employee = new Employee();
                    employee.setCode(csvSplit[0]);
                    employee.setName(csvSplit[1]);
                    String strRole = csvSplit[2];
                    employee.setRole(Role.valueOf(strRole));
                    if (csvSplit[3] != "") {
                        employee.setPassword(csvSplit[3]);
                    } else {
                        employee.setPassword("testpass");
                    }
                    // 第一段階なのでパスワードチェックはせずにエンコーダーを通すのみ
                    employee.setPassword(passwordEncoder.encode(employee.getPassword()));
                    // 登録日時・更新日時は現在値を投入
                    LocalDateTime now = LocalDateTime.now();
                    employee.setCreatedAt(now);
                    employee.setUpdatedAt(now);
                    employee.setDeleteFlg(false);
                    // 第一段階なのでエラー確認せずに登録まで進める
                    employeeRepository.save(employee);
                } else {    // 上書き登録
                    // フィールド(code,[name],[role],[[password]],delete_flg,created_at,[updated_at])
                    // 更新用Employeeに更新元のデータを入れる
                    Employee updateEmployee = findByCode(csvSplit[0]);
                    updateEmployee.setName(csvSplit[1]);
                    String strRole = csvSplit[2];
                    updateEmployee.setRole(Role.valueOf(strRole));
                    if (csvSplit[3] != "") {
                        // 第一段階なのでパスワードチェックはせずにエンコーダーを通すのみ
                        updateEmployee.setPassword(csvSplit[3]);
                        updateEmployee.setPassword(passwordEncoder.encode(updateEmployee.getPassword()));
                    } else {
                        updateEmployee.setPassword(findByCode(csvSplit[0]).getPassword());
                    }
                    // 更新日時のみ現在値を投入
                    LocalDateTime now = LocalDateTime.now();
                    updateEmployee.setUpdatedAt(now);
                    // 削除フラグ更新
                    boolean booDeleteFlg = Boolean.valueOf(csvSplit[6]);  // true or false
                    updateEmployee.setDeleteFlg(booDeleteFlg);
                    // 更新用Employeeの内容で保存
                    employeeRepository.save(updateEmployee);
                }
            }
            br.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // CSV入力用、登録済みのIDチェック ※使用していない
    public boolean existsByCode(String code) {
        return employeeRepository.existsByCode(code);
    }

}
