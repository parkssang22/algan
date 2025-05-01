package com.capstone.Algan.utils

import android.content.Context
import android.util.Log
import com.capstone.Algan.WorkTime
import com.itextpdf.io.font.PdfEncodings
import com.itextpdf.kernel.font.PdfFont
import com.itextpdf.kernel.font.PdfFontFactory
import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.kernel.pdf.PdfWriter
import com.itextpdf.layout.Document
import com.itextpdf.layout.element.Cell
import com.itextpdf.layout.element.Paragraph
import com.itextpdf.layout.element.Table
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

fun pdfWordRecord(context: Context, workTimeList: List<WorkTime>): String {
    // 1. 파일 경로 설정
    val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
    val fileName = "WorkRecords_$timeStamp.pdf"
    val filePath = File(context.getExternalFilesDir(null), fileName)

    try {
        // 2. PDF Writer 생성
        val pdfWriter = PdfWriter(filePath)
        val pdfDocument = PdfDocument(pdfWriter)
        val document = Document(pdfDocument)

        // 3. 폰트 설정 - 기본 폰트로 시작하고 한글 폰트 시도
        var pdfFont: PdfFont

        try {
            // 캐시 디렉토리 생성 확인
            val fontCacheDir = File(context.cacheDir, "fonts")
            if (!fontCacheDir.exists()) {
                fontCacheDir.mkdirs()
            }

            // 나눔고딕 폰트 임시 파일로 복사
            val tempFontFile = File(fontCacheDir, "NanumGothic-Regular.ttf")
            context.assets.open("fonts/NanumGothic-Regular.ttf").use { input ->
                tempFontFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }

            // 한글 폰트 생성
            pdfFont = PdfFontFactory.createFont(
                tempFontFile.absolutePath,
                PdfEncodings.IDENTITY_H,
                PdfFontFactory.EmbeddingStrategy.FORCE_EMBEDDED
            )

            Log.d("PDFGenerator", "한글 폰트 로드 성공")
        } catch (e: Exception) {
            // 폰트 로드 실패 시 기본 폰트 사용
            Log.e("PDFGenerator", "한글 폰트 로드 실패: ${e.message}")
            e.printStackTrace()
            pdfFont = PdfFontFactory.createFont()
        }

        // 4. 제목 추가
        document.add(Paragraph("출퇴근 기록").setBold().setFont(pdfFont).setFontSize(18f))
        document.add(Paragraph("생성 날짜: $timeStamp\n\n").setFont(pdfFont))

        // 5. 테이블 생성 (7열)
        val columnWidths = floatArrayOf(80f, 100f, 100f, 100f, 100f, 80f, 80f)
        val table = Table(columnWidths)

        // 6. 테이블 헤더 추가
        val headers = listOf("UID", "사용자명", "날짜", "출근 시간", "퇴근 시간", "근무 시간", "근무 타입")
        for (header in headers) {
            table.addHeaderCell(Cell().add(Paragraph(header).setBold().setFont(pdfFont)))
        }

        // 7. 테이블 데이터 추가
        for (workTime in workTimeList) {
            // 각 셀에 폰트 적용
            table.addCell(Cell().add(Paragraph(workTime.uid ?: "-").setFont(pdfFont)))
            table.addCell(Cell().add(Paragraph(workTime.userName ?: "-").setFont(pdfFont)))
            table.addCell(Cell().add(Paragraph(workTime.date ?: "-").setFont(pdfFont)))
            table.addCell(Cell().add(Paragraph(workTime.clockIn ?: "-").setFont(pdfFont)))
            table.addCell(Cell().add(Paragraph(workTime.clockOut ?: "-").setFont(pdfFont)))
            table.addCell(Cell().add(Paragraph(workTime.workedHours ?: "-").setFont(pdfFont)))
            table.addCell(Cell().add(Paragraph(workTime.worktype ?: "-").setFont(pdfFont)))
        }

        // 8. 테이블 문서에 추가
        document.add(table)

        // 9. 문서 닫기
        document.close()

        Log.d("PDFGenerator", "PDF 생성 완료: ${filePath.absolutePath}")
        return filePath.absolutePath

    } catch (e: IOException) {
        Log.e("PDFGenerator", "PDF 생성 오류: ${e.message}")
        e.printStackTrace()
        return "PDF 생성 실패: ${e.message}"
    } catch (e: Exception) {
        Log.e("PDFGenerator", "예상치 못한 오류: ${e.message}")
        e.printStackTrace()
        return "PDF 생성 실패: ${e.message}"
    }
}