package com.techacademy.controller;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import com.techacademy.service.UserDetail;

// サイドバーにログインユーザー氏名を表示させるために作ったけれどEmployeeControllerReportControllerに直接記載している

@ControllerAdvice
public class CommonControllerAdvice {
    @ModelAttribute
    public void addAttributes(@AuthenticationPrincipal UserDetail userDetail, Model model) {
        if(userDetail != null) model.addAttribute("loginUser", userDetail.getEmployee().getName());
    }
}
