package cn.oa.utils;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.write.style.column.LongestMatchColumnWidthStyleStrategy;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Excel export utility using EasyExcel.
 * Moved from oa-common to oa-web — Excel export is a web-layer concern.
 */
public class ExcelExportUtil {

    private static final DateTimeFormatter FILE_NAME_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");

    public static <T> void export(HttpServletResponse response, String baseFileName, Class<T> head, List<T> data) throws IOException {
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setCharacterEncoding("utf-8");

        // 遵循命名规范：文件名_年月日_时分秒
        String timestamp = LocalDateTime.now().format(FILE_NAME_FORMATTER);
        String finalFileName = baseFileName + "_" + timestamp;

        String encodedName = URLEncoder.encode(finalFileName, StandardCharsets.UTF_8).replaceAll("\\+", "%20");
        response.setHeader("Content-Disposition", "attachment;filename=" + encodedName + ".xlsx");
        response.setHeader("Access-Control-Expose-Headers", "Content-Disposition"); // 暴露 Header 供前端提取文件名

        EasyExcel.write(response.getOutputStream(), head)
                .registerWriteHandler(new LongestMatchColumnWidthStyleStrategy())
                .sheet(baseFileName)
                .doWrite(data);
    }
}
