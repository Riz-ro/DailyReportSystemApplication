package com.techacademy.controller;

import java.math.BigInteger;
import java.time.format.DateTimeFormatter;

import org.apache.poi.xwpf.usermodel.ParagraphAlignment;
import org.apache.poi.xwpf.usermodel.UnderlinePatterns;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTBody;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTDocument1;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTPageMar;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTPageSz;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTSectPr;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTTblLayoutType;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTTblWidth;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTTc;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTTcPr;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.STPageOrientation;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.STTblLayoutType;
import org.springframework.web.bind.annotation.ModelAttribute;

import com.techacademy.entity.Report;

public class ReportDOCX {
    public static XWPFDocument DOCXCreate(@ModelAttribute Report report) {

        XWPFDocument document = null;

        document = new XWPFDocument();

        CTDocument1 doc1 = document.getDocument();
        CTBody body = doc1.getBody();
        CTSectPr section = (body.isSetSectPr() ? body.getSectPr() : body.addNewSectPr());

        // 用紙設定
        // 用紙サイズ
        CTPageSz pageSize = (section.isSetPgSz() ? section.getPgSz() : section.addNewPgSz());
        pageSize.setOrient(STPageOrientation.PORTRAIT);
        pageSize.setW(BigInteger.valueOf(595 * 20));
        pageSize.setH(BigInteger.valueOf(842 * 20));

        // マージン
        CTPageMar pageMar = (section.isSetPgMar() ? section.getPgMar() : section.addNewPgMar());
        pageMar.setTop(BigInteger.valueOf(2000));
        pageMar.setBottom(BigInteger.valueOf(2000));
        pageMar.setLeft(BigInteger.valueOf(2000));
        pageMar.setRight(BigInteger.valueOf(2000));

        // ヘッダ、フッタ
        pageMar.setHeader(BigInteger.valueOf(851));
        pageMar.setFooter(BigInteger.valueOf(851));

        // 段落作成
        XWPFParagraph paragraph = document.createParagraph();
        paragraph.setAlignment(ParagraphAlignment.CENTER); // センタリング設定
        paragraph.setIndentationLeft(4 * 200); // インデント設定
        XWPFRun run = paragraph.createRun(); // 本文作成
        run.setFontFamily("BIZ UDPゴシック"); // フォント設定
        run.setFontSize(24); // フォントサイズ
        run.setColor("000000"); // 文字色
        run.setBold(true); // 太字
        run.setItalic(false); // イタリック
        run.setUnderline(UnderlinePatterns.SINGLE); // 下線
        run.setText("業務日報");
        run.addCarriageReturn(); // 改行

        XWPFTable table = document.createTable(6, 2); // 縦:6、横:2
        table.setWidth(5000); // テーブル幅（ポイント幅×20）

        CTTblLayoutType type = table.getCTTbl().getTblPr().addNewTblLayout(); // カラム幅設定（最初の行のみでOK）
        type.setType(STTblLayoutType.FIXED); // 固定幅を設定
        CTTc ctTc = table.getRow(0).getCell(0).getCTTc();
        CTTcPr tcPr = (ctTc.isSetTcPr() ? ctTc.getTcPr() : ctTc.addNewTcPr());
        CTTblWidth tblWidth = (tcPr.isSetTcW() ? tcPr.getTcW() : tcPr.addNewTcW());
        tblWidth.setW(BigInteger.valueOf(400));
        table.setCellMargins(20, 20, 20, 20); // マージン

        String[] item = { "日付", "氏名", "タイトル", "内容", "登録日時", "更新日時" };

        String ReportDateStr = report.getReportDate().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
        String CreatedAtStr = report.getCreatedAt().format(DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss"));
        String UpdatedAtStr = report.getUpdatedAt().format(DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss"));
        String[] itemList = { ReportDateStr, report.getEmployee().getName(), report.getTitle(), report.getContent(),
                CreatedAtStr, UpdatedAtStr };

        for (int i = 0; i < 6; i++) {
            for (int j = 0; j < 2; j++) {
                if (j == 0) { // 項目名セット
                    paragraph = table.getRow(i).getCell(j).getParagraphs().get(0);
                    run = paragraph.createRun();
                    run.setFontFamily("BIZ UDPゴシック");
                    run.setText(item[i]);
                } else { // Reportデータ格納
                    paragraph = table.getRow(i).getCell(j).getParagraphs().get(0);
                    run = paragraph.createRun();
                    run.setFontFamily("BIZ UDPゴシック");
                    run.setText(itemList[i]);
                }
            }
        }
        return document;
    }
}
