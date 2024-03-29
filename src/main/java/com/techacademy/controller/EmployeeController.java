package com.techacademy.controller;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import com.techacademy.constants.ErrorKinds;
import com.techacademy.constants.ErrorMessage;

import com.techacademy.entity.Employee;
import com.techacademy.entity.EmployeeCSV;
import com.techacademy.entity.EmployeeCsvColumn;
import com.techacademy.service.EmployeeService;
import com.techacademy.service.UserDetail;

import uk.gov.nationalarchives.csv.validator.api.java.CsvValidator;
import uk.gov.nationalarchives.csv.validator.api.java.FailMessage;
import uk.gov.nationalarchives.csv.validator.api.java.Substitution;
import uk.gov.nationalarchives.csv.validator.api.java.WarningMessage;

@Controller
@RequestMapping("employees")
public class EmployeeController {

    private final EmployeeService employeeService;

    @Autowired
    public EmployeeController(EmployeeService employeeService) {
        this.employeeService = employeeService;
    }

    // 従業員一覧画面
    @GetMapping
    public String list(Model model, Pageable pageable) {

        Page<Employee> pageList = employeeService.findAllByDeleteFlgFalse(pageable);
        // ビューに渡す際、Page型の変数をそのまま渡しても実装可能だが、記載が複雑になるのでレコード情報だけを別にわたすことで可読性があがる。
        List<Employee> employeeList = pageList.getContent();
        model.addAttribute("pages", pageList);
        model.addAttribute("listSize", employeeService.findAllByDeleteFlgFalse().size());
        model.addAttribute("employeeList", employeeList);
        model.addAttribute("allEmployeeList", employeeService.findAll());

        return "employees/list";
    }

    // 従業員詳細画面
    @GetMapping(value = "/{code}/")
    public String detail(@PathVariable String code, Model model) {

        model.addAttribute("employee", employeeService.findByCode(code));
        return "employees/detail";
    }

    // 従業員新規登録画面
    @GetMapping(value = "/add")
    public String create(@ModelAttribute Employee employee) {

        return "employees/new";
    }

    // 従業員新規登録処理
    @PostMapping(value = "/add")
    public String add(@Validated Employee employee, BindingResult res, Model model) {

        // パスワード空白チェック
        /*
         * エンティティ側の入力チェックでも実装は行えるが、更新の方でパスワードが空白でもチェックエラーを出さずに
         * 更新出来る仕様となっているため上記を考慮した場合に別でエラーメッセージを出す方法が簡単だと判断
         */
        if ("".equals(employee.getPassword())) {
            // パスワードが空白だった場合
            model.addAttribute(ErrorMessage.getErrorName(ErrorKinds.BLANK_ERROR),
                    ErrorMessage.getErrorValue(ErrorKinds.BLANK_ERROR));

            return create(employee);

        }

        // 入力チェック
        if (res.hasErrors()) {
            return create(employee);
        }

        // 論理削除を行った従業員番号を指定すると例外となるためtry~catchで対応
        // (findByIdでは削除フラグがTRUEのデータが取得出来ないため)
        try {
            ErrorKinds result = employeeService.save(employee);

            if (ErrorMessage.contains(result)) {
                model.addAttribute(ErrorMessage.getErrorName(result), ErrorMessage.getErrorValue(result));
                return create(employee);
            }

        } catch (DataIntegrityViolationException e) {
            model.addAttribute(ErrorMessage.getErrorName(ErrorKinds.DUPLICATE_EXCEPTION_ERROR),
                    ErrorMessage.getErrorValue(ErrorKinds.DUPLICATE_EXCEPTION_ERROR));
            return create(employee);
        }

        return "redirect:/employees";
    }

    // 従業員削除処理
    @PostMapping(value = "/{code}/delete")
    public String delete(@PathVariable String code, @AuthenticationPrincipal UserDetail userDetail, Model model) {

        ErrorKinds result = employeeService.delete(code, userDetail);

        if (ErrorMessage.contains(result)) {
            model.addAttribute(ErrorMessage.getErrorName(result), ErrorMessage.getErrorValue(result));
            model.addAttribute("employee", employeeService.findByCode(code));
            return detail(code, model);
        }

        return "redirect:/employees";
    }

    // 従業員更新画面 追加
    @GetMapping(value = "/{code}/update")
    public String edit(@PathVariable String code, Model model) {

        if (code != null) {
            model.addAttribute("employee", employeeService.findByCode(code));
        }

        return "employees/update";
    }

    // 従業員更新処理 追加
    @PostMapping(value = "/{code}/update")
    public String update(@Validated Employee employee, BindingResult res, Model model,
            @PathVariable String code) {

        // 入力チェック
        if (res.hasErrors()) {
            code = null;
            return edit(code, model);
        }

        ErrorKinds result = employeeService.update(code,employee);

        if (ErrorMessage.contains(result)) {
            model.addAttribute(ErrorMessage.getErrorName(result), ErrorMessage.getErrorValue(result));
            return edit(code, model);
        }

        return "redirect:/employees";
    }

    // CSV出力処理
    @PostMapping(value = "/csvexport", produces = MediaType.APPLICATION_OCTET_STREAM_VALUE
            + "; charset=UTF-8; Content-Disposition: attachment")
    @ResponseBody
    public Object csvExport(@ModelAttribute("csvForm") EmployeeCSV records) throws JsonProcessingException {
        List<EmployeeCsvColumn> csvList = employeeService.csvExport(records);
        CsvMapper mapper = new CsvMapper();
        mapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
        CsvSchema schema = mapper.schemaFor(EmployeeCsvColumn.class).withHeader();
        return mapper.writer(schema).writeValueAsString(csvList);
    }

    // CSV入力用
    @PostMapping(value = "/csvimport")
    public String csvImport(@RequestParam("file") MultipartFile file, Model model, Pageable pageable) {
        // CSV入力チェック
        List<Substitution> pathSubstitutions = new ArrayList<>();
        // 未処理：importFileを下記フォルダに格納させる処理を追加させる
        List<FailMessage> messages = CsvValidator.validate(
                "src/main/resources/files/csvexport.csv",
                "src/main/resources/files/data-schema.csvs", // CSVスキーマファイル
                false, // 最初にエラーが発生したら処理を中断するか
                pathSubstitutions, // ファイルパスの格納用
                true // ファイルパスの厳格なチェック
                );
        if (messages.isEmpty()) {
            System.out.println("Completed validation OK");
        } else {
            for (FailMessage message : messages) {
                if (message instanceof WarningMessage) {
                    System.out.println("[WARN] " + message.getMessage());
                } else {
                    System.out.println("[ERROR] " + message.getMessage());
                }
                model.addAttribute("errorMessage",  "CSVデータ内容に問題があるため登録できません：" + message.getMessage());
                return list(model, pageable);
            }
        }
        // 入力チェック後インポート処理
        employeeService.csvImport(file);
        model.addAttribute("resultList", employeeService.findAllTemporary());
        model.addAttribute("listSize", employeeService.findAllTemporary().size());

        return "employees/resultlist";
    }

    // CSV入力結果確認画面
    @GetMapping(value = "/resultlist")
    public String resultList(@ModelAttribute List<List<String>> resultList) {

        return "employees/resultlist";
    }

    // CSV一括登録実行
    @GetMapping(value = "/importaction")
    public String importAction() {
        employeeService.importAction();
        return "redirect:/employees";
    }

    // 削除済従業員一覧画面
    @GetMapping(value = "/deletelist")
    public String deleteList(Model model, Pageable pageable) {

        Page<Employee> pageList = employeeService.findAllByDeleteFlgTrue(pageable);
        // ビューに渡す際、Page型の変数をそのまま渡しても実装可能だが、記載が複雑になるのでレコード情報だけを別にわたすことで可読性があがる。
        List<Employee> employeeList = pageList.getContent();
        model.addAttribute("pages", pageList);
        model.addAttribute("listSize", employeeService.findAllByDeleteFlgTrue().size());
        model.addAttribute("employeeList", employeeList);

        return "employees/deletelist";
    }

    // 削除従業員詳細画面
    @GetMapping(value = "/delete/{code}/")
    public String deleteDetail(@PathVariable String code, Model model) {

        model.addAttribute("employee", employeeService.findByCode(code));
        return "employees/deletedetail";
    }

    // 削除済従業員取消処理
    @GetMapping(value = "/delete/{code}/deleteupdate")
    public String deleteUpdate(Model model, @PathVariable String code) {

        employeeService.deleteUpdate(code);

        return "redirect:/employees/deletelist";
    }

}
