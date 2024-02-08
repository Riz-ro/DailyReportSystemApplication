package com.techacademy.controller;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
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
import org.springframework.web.bind.annotation.ResponseBody;

import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateDeserializer;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.techacademy.constants.ErrorKinds;
import com.techacademy.constants.ErrorMessage;
import com.techacademy.entity.CSV;
import com.techacademy.entity.CsvColumn;
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

    @PostMapping(value = "/csvoutput", produces = MediaType.APPLICATION_OCTET_STREAM_VALUE
            + "; charset=UTF-8; Content-Disposition: attachment")
        @ResponseBody
        public Object csvDownload(@ModelAttribute("csvForm") CSV records) throws JsonProcessingException {
          List<CsvColumn> csvList = new ArrayList<>();
          for (int i = 0; i < records.getId().size(); i++) {
            csvList.add(new CsvColumn(records.getId().get(i), records.getName().get(i), records.getReportDate().get(i), records.getReportTitle().get(i), records.getReportContent().get(i), records.getReportCreated().get(i), records.getReportUpdated().get(i)));
          }
          CsvMapper mapper = new CsvMapper();
          mapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
          CsvSchema schema = mapper.schemaFor(CsvColumn.class).withHeader();
          JavaTimeModule javaTimeModule = new JavaTimeModule();
          javaTimeModule.addDeserializer(
                  LocalDate.class,
                  new LocalDateDeserializer(DateTimeFormatter.ISO_DATE)
          );
          javaTimeModule.addDeserializer(
                  LocalDateTime.class,
                  new LocalDateTimeDeserializer(DateTimeFormatter.ISO_DATE_TIME)
          );
          mapper.registerModule(javaTimeModule);

          return mapper.writer(schema).writeValueAsString(csvList);
        }

}
