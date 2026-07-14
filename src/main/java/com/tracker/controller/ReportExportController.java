package com.tracker.controller;

import com.tracker.dto.ReportRequest;
import com.tracker.dto.ReportResponse;
import com.tracker.model.User;
import com.tracker.security.SecurityUtils;
import com.tracker.service.ReportExportService;
import com.tracker.service.ReportService;
import com.tracker.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/reports/export")
@RequiredArgsConstructor
@Tag(name = "Reports Export", description = "Export reports dynamically as PDF, Excel, or CSV")
@SecurityRequirement(name = "bearerAuth")
public class ReportExportController {

    private final ReportService reportService;
    private final ReportExportService reportExportService;
    private final UserService userService;

    @GetMapping
    @Operation(summary = "Export any report dynamically by specifying type, format and filters")
    public ResponseEntity<byte[]> exportReport(
            ReportRequest request,
            @RequestParam(defaultValue = "PDF") String format) {
        
        Long currentUserId = SecurityUtils.getCurrentUserId();
        User currentUser = userService.getUserEntity(currentUserId);

        // Generate report JSON payload
        ReportResponse report = reportService.generateReport(request, currentUser);
        
        try {
            byte[] fileBytes;
            String filename = "report_" + report.getType().toLowerCase() + "_" + System.currentTimeMillis();
            String contentType;
            HttpHeaders headers = new HttpHeaders();

            switch (format.toUpperCase().trim()) {
                case "EXCEL":
                case "XLSX":
                    fileBytes = reportExportService.exportToExcel(report);
                    contentType = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
                    filename += ".xlsx";
                    break;
                case "CSV":
                    fileBytes = reportExportService.exportToCsv(report);
                    contentType = "text/csv; charset=UTF-8";
                    filename += ".csv";
                    break;
                case "PDF":
                default:
                    fileBytes = reportExportService.exportToPdf(report);
                    contentType = "application/pdf";
                    filename += ".pdf";
                    break;
            }

            headers.setContentType(MediaType.parseMediaType(contentType));
            headers.setContentDispositionFormData("attachment", filename);
            headers.setCacheControl("must-revalidate, post-check=0, pre-check=0");

            return ResponseEntity.ok()
                    .headers(headers)
                    .body(fileBytes);

        } catch (Exception e) {
            log.error("Failed to export report of type={} in format={}: {}", request.getType(), format, e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }
}
