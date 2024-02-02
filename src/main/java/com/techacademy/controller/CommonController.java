package com.techacademy.controller;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ModelAttribute;

import com.techacademy.entity.Employee;
import com.techacademy.service.UserDetail;

// サイドバーにログインユーザー氏名を表示させるために作ったけれどEmployeeControllerReportControllerに直接記載している
public class CommonController {
    @ModelAttribute
    public void addAttributes(@ModelAttribute Employee employee, @AuthenticationPrincipal UserDetail userDetail,
            Model model) {
        model.addAttribute("employeeName", userDetail.getEmployee().getName());
    }
}
