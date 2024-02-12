package com.techacademy.controller;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.net.URLEncoder;

import org.apache.poi.xwpf.usermodel.ParagraphAlignment;
import org.apache.poi.xwpf.usermodel.UnderlinePatterns;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.apache.poi.xwpf.usermodel.XWPFTable;
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
        if(Employee.Role.ADMIN.equals(userDetail.getEmployee().getRole())){
            // 管理者権限ユーザーは全件取得(ページ情報つきの検索)
            Page<Report> pageList = reportService.findAll(pageable);
            // ビューに渡す際、Page型の変数をそのまま渡しても実装可能だが、記載が複雑になるのでレコード情報だけを別にわたすことで可読性があがる。
            List<Report> reportList = pageList.getContent();
            model.addAttribute("pages", pageList);
            model.addAttribute("listSize", reportService.findAll().size());
            model.addAttribute("reportList", reportList);
        } else {
            // 一般ユーザーは自身のデータのみ取得
            Page<Report> pageList = reportService.findByEmployee(userDetail, pageable);
            List<Report> reportList = pageList.getContent();
            model.addAttribute("pages", pageList);
            model.addAttribute("listSize", reportService.findByEmployee(userDetail).size());
            model.addAttribute("reportList", reportList);
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

    // CSV出力処理
    @PostMapping(value = "/csvexport", produces = MediaType.APPLICATION_OCTET_STREAM_VALUE
            + "; charset=UTF-8; Content-Disposition: attachment")
        @ResponseBody
        public Object csvExport(@ModelAttribute("csvForm") CSV records) throws JsonProcessingException {
          List<CsvColumn> csvList = new ArrayList<>();
          for (int i = 0; i < records.getId().size(); i++) {
            csvList.add(new CsvColumn(records.getId().get(i), records.getCode().get(i), records.getName().get(i), records.getReportDate().get(i), records.getReportTitle().get(i), records.getReportContent().get(i), records.getReportCreated().get(i), records.getReportUpdated().get(i)));
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

    // CSV入力用
    @PostMapping(value = "/csvimport")
    public String csvImport(@RequestParam("file") MultipartFile file) {
        try (InputStream inputStream = file.getInputStream();
                BufferedReader br = new BufferedReader(new InputStreamReader(inputStream))) {

            //読み取ったCSVの行を入れるための変数を作成
            String line;
            //ヘッダーレコードを飛ばすためにあらかじめ１行だけ読み取っておく（ない場合は不要）
            line = br.readLine();
            //行がNULL（CSVの値がなくなる）になるまで処理を繰り返す
            while ((line = br.readLine()) != null) {
            //Stringのsplitメソッドを使用してカンマごとに分割して配列にいれる
                String[] csvSplit = line.split(",");
                Report report = new Report();
                report.setId(Integer.parseInt(csvSplit[0]));
                Employee employee = new Employee();
                employee.setCode(csvSplit[1]);
                report.setEmployee(employee);
                report.setReportDate(LocalDate.parse(csvSplit[3],DateTimeFormatter.ofPattern("yyyy-[]M-[]d")));
                report.setTitle(csvSplit[4]);
                report.setContent(csvSplit[5]);
                report.setCreatedAt(LocalDateTime.parse(csvSplit[6],DateTimeFormatter.ofPattern("yyyy-[]M-[]d'T'[]H:[]m:[]s")));
                report.setUpdatedAt(LocalDateTime.parse(csvSplit[7],DateTimeFormatter.ofPattern("yyyy-[]M-[]d'T'[]H:[]m:[]s")));
                report.setDeleteFlg(false);
                reportService.save(report);
            }
            br.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return "redirect:/reports";
    }

    // Word出力用
    @PostMapping(value = "/docx", produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    @ResponseBody
    public void DOCXWrite(@ModelAttribute Report report, Map<String, Object> model, HttpServletRequest request,
            HttpServletResponse response) throws Exception{

        final String encodedFilename = URLEncoder.encode("report.docx", "UTF-8");
        response.setHeader("Content-Disposition", "attachment; filename*=UTF-8''" + encodedFilename);

        XWPFDocument document = null;

        try {
            document = new XWPFDocument();
            XWPFParagraph paragraph = document.createParagraph();
            paragraph.setAlignment(ParagraphAlignment.CENTER);
            paragraph.setIndentationLeft(4*200);
            XWPFRun run = paragraph.createRun();
            run.setFontFamily("BIZ UDPゴシック");
            run.setFontSize(22);  // フォントサイズ
            run.setColor("ff0000");  // 文字色
            run.setBold(true);  // 太字
            run.setItalic(true);  // イタリック
            run.setUnderline(UnderlinePatterns.SINGLE);  // 下線
            run.setText("日報 詳細");
            run.addCarriageReturn();    //改行

            XWPFTable table = document.createTable(6, 2); // 縦:6、横:2
            String[] item = {"日付","氏名","タイトル","内容","登録日時","更新日時"};

            String ReportDateStr = report.getReportDate().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            String CreatedAtStr = report.getCreatedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss"));
            String UpdatedAtStr = report.getUpdatedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss"));
            String[] itemList = {ReportDateStr, report.getEmployee().getName(), report.getTitle(), report.getContent(), CreatedAtStr, UpdatedAtStr};

            for (int i = 0; i < 6; i++) {
                for (int j = 0; j < 2; j++) {
                    if (j == 0) {   // 項目名セット
                        paragraph = table.getRow(i).getCell(j).addParagraph();
                        run = paragraph.createRun();
                        run.setFontFamily("BIZ UDPゴシック");
                        run.setText(item[i]);
                    } else {        // Reportデータ格納
                        paragraph = table.getRow(i).getCell(j).addParagraph();
                        run = paragraph.createRun();
                        run.setFontFamily("BIZ UDPゴシック");
                        run.setText(itemList[i]);
                    }
                }
            }
            //ファイル出力
            response.setContentType("application/msword");
            document.write(response.getOutputStream());
        }
        finally {
            if (document != null) {
                try {
                    document.close();
                }
                catch (IOException e) {
                }
            }
        }
    }
}