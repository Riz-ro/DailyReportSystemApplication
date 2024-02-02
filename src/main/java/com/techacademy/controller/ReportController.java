package com.techacademy.controller;

import org.springframework.beans.factory.annotation.Autowired;
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

import com.techacademy.constants.ErrorKinds;
import com.techacademy.constants.ErrorMessage;
import com.techacademy.entity.Employee;
import com.techacademy.entity.Report;
import com.techacademy.service.ReportService;
import com.techacademy.service.UserDetail;

@Controller
@RequestMapping("reports")
public class ReportController {

    private final ReportService reportService;

    @Autowired
    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    @ModelAttribute
    public void addAttributes(@ModelAttribute Employee employee, @AuthenticationPrincipal UserDetail userDetail,
            Model model) {
        model.addAttribute("employeeName", userDetail.getEmployee().getName());
    }

    // 日報一覧画面
    @GetMapping
    public String list(@AuthenticationPrincipal UserDetail userDetail, Model model) {

        // 【表示制御】
        // 全件検索できるのは管理者権限ユーザーのみ。ifで分岐させ、一般権限ユーザーは自身の一覧データのみを表示させる。
        if(Employee.Role.ADMIN.equals(userDetail.getEmployee().getRole())){
            // 管理者権限ユーザーは全件取得
            model.addAttribute("listSize", reportService.findAll().size());
            model.addAttribute("reportList", reportService.findAll());
        } else {
            // 一般ユーザーは自身のデータのみ取得
            model.addAttribute("listSize", reportService.findByEmployee(userDetail).size());
            model.addAttribute("reportList", reportService.findByEmployee(userDetail));
        }

        return "reports/list";
    }

    // 日報詳細画面
    @GetMapping(value = "/{id}/")
    public String detail(@PathVariable int id, Model model) {

        model.addAttribute("report", reportService.findById(id));
        return "reports/detail";
    }

    // 日報新規登録画面
    @GetMapping(value = "/add")
    public String create(@ModelAttribute Report report, @AuthenticationPrincipal UserDetail userDetail, Model model) {

        model.addAttribute("employeeName", userDetail.getEmployee().getName());
        return "reports/new";
    }

    // 日報新規登録処理
    @PostMapping(value = "/add")
    public String add(@Validated Report report, BindingResult res, @AuthenticationPrincipal UserDetail userDetail, Model model) {

        // 入力チェック
        if (res.hasErrors()) {
            return create(report, userDetail, model);
        }

        ErrorKinds result = reportService.save(report, userDetail);

        if (ErrorMessage.contains(result)) {
            model.addAttribute(ErrorMessage.getErrorName(result), ErrorMessage.getErrorValue(result));
            return create(report, userDetail, model);
        }

        return "redirect:/reports";
    }

    // 日報削除処理
    @PostMapping(value = "/{id}/delete")
    public String delete(@PathVariable int id, Model model) {

        ErrorKinds result = reportService.delete(id);

        if (ErrorMessage.contains(result)) {
            model.addAttribute(ErrorMessage.getErrorName(result), ErrorMessage.getErrorValue(result));
            model.addAttribute("report", reportService.findById(id));
            return detail(id, model);
        }

        return "redirect:/reports";
    }

    // 日報更新画面 追加
    @GetMapping(value = "/{id}/update")
    public String edit(@PathVariable int id, @AuthenticationPrincipal UserDetail userDetail, Model model) {

        model.addAttribute("employeeName", userDetail.getEmployee().getName());

        if (id != 0) {
            model.addAttribute("report", reportService.findById(id));
        }

        return "reports/update";
    }

    // 日報更新処理 追加
    @PostMapping(value = "/{id}/update")
    public String update(@Validated Report report, BindingResult res, @AuthenticationPrincipal UserDetail userDetail, Model model, @PathVariable int id) {

        // 入力チェック
        if (res.hasErrors()) {
            id = 0;
            return edit(id, userDetail, model);
        }

        ErrorKinds result = reportService.update(id, report, userDetail);

        if (ErrorMessage.contains(result)) {
            model.addAttribute(ErrorMessage.getErrorName(result), ErrorMessage.getErrorValue(result));
            id = 0;
            return edit(id, userDetail, model);
        }

        return "redirect:/reports";
    }

}
