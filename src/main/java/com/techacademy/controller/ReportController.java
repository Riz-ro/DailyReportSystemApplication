package com.techacademy.controller;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.net.URLEncoder;

import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.springframework.beans.factory.annotation.Autowired;
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
import com.techacademy.entity.ReportCSV;
import com.techacademy.entity.ReportCsvColumn;
import com.techacademy.entity.Employee;
import com.techacademy.entity.Report;
import com.techacademy.service.ReportService;
import com.techacademy.service.UserDetail;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

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
    public String list(@AuthenticationPrincipal UserDetail userDetail, Model model, Pageable pageable) {

        // 【表示制御】
        // 全件検索できるのは管理者権限ユーザーのみ。ifで分岐させ、一般権限ユーザーは自身の一覧データのみを表示させる。
        if (Employee.Role.ADMIN.equals(userDetail.getEmployee().getRole())) {
            // 管理者権限ユーザーは全件取得(ページ情報つきの検索)
            Page<Report> pageList = reportService.findAll(pageable);
            // ビューに渡す際、Page型の変数をそのまま渡しても実装可能だが、記載が複雑になるのでレコード情報だけを別にわたすことで可読性があがる。
            List<Report> reportList = pageList.getContent();
            model.addAttribute("pages", pageList);
            model.addAttribute("listSize", reportService.findAll().size());
            model.addAttribute("reportList", reportList);
            model.addAttribute("allReportList", reportService.findAll());
        } else {
            // 一般ユーザーは自身のデータのみ取得
            Page<Report> pageList = reportService.findByEmployee(userDetail, pageable);
            List<Report> reportList = pageList.getContent();
            model.addAttribute("pages", pageList);
            model.addAttribute("listSize", reportService.findByEmployee(userDetail).size());
            model.addAttribute("reportList", reportList);
            model.addAttribute("allReportList", reportService.findByEmployee(userDetail));
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
    public String add(@Validated Report report, BindingResult res, @AuthenticationPrincipal UserDetail userDetail,
            Model model) {

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
    public String update(@Validated Report report, BindingResult res, @AuthenticationPrincipal UserDetail userDetail,
            Model model, @PathVariable int id) {

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

    // CSV出力処理
    @PostMapping(value = "/csvexport", produces = MediaType.APPLICATION_OCTET_STREAM_VALUE
            + "; charset=UTF-8; Content-Disposition: attachment")
    @ResponseBody
    public Object csvExport(@ModelAttribute("csvForm") ReportCSV records) throws JsonProcessingException {
        List<ReportCsvColumn> csvList = reportService.csvExport(records);
        CsvMapper mapper = new CsvMapper();
        mapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
        CsvSchema schema = mapper.schemaFor(ReportCsvColumn.class).withHeader();
        return mapper.writer(schema).writeValueAsString(csvList);
    }

    // CSV入力用
    @PostMapping(value = "/csvimport")
    public String csvImport(@RequestParam("file") MultipartFile file) {
        reportService.csvImport(file);
        return "redirect:/reports";
    }

    // Word出力用
    @PostMapping(value = "/docx", produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    @ResponseBody
    public void DOCXWrite(@ModelAttribute Report report, Map<String, Object> model, HttpServletRequest request,
            HttpServletResponse response) throws Exception {

        final String encodedFilename = URLEncoder.encode("report.docx", "UTF-8");
        response.setHeader("Content-Disposition", "attachment; filename*=UTF-8''" + encodedFilename);

        XWPFDocument document = ReportDOCX.DOCXCreate(report);
        try {
            response.setContentType("application/msword");
            document.write(response.getOutputStream());
        } finally {
            if (document != null) {
                try {
                    document.close();
                } catch (IOException e) {
                }
            }
        }
    }
}