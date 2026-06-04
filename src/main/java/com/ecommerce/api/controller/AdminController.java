package com.ecommerce.api.controller;

import com.ecommerce.api.dto.admin.AdminStatsResponse;
import com.ecommerce.api.dto.admin.AuditLogResponse;
import com.ecommerce.api.dto.admin.ChartDataResponse;
import com.ecommerce.api.dto.product.ProductResponse;
import com.ecommerce.api.service.AdminService;
import com.ecommerce.api.service.AuditService;
import com.ecommerce.api.service.StockAlertService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Admin")
public class AdminController {

    private final AdminService adminService;
    private final StockAlertService stockAlertService;
    private final AuditService auditService;

    @GetMapping("/stats")
    @Operation(summary = "Dashboard statistics")
    public AdminStatsResponse stats() {
        return adminService.getStats();
    }

    @GetMapping("/charts/orders")
    @Operation(summary = "Monthly order chart data")
    public ChartDataResponse charts(@RequestParam(defaultValue = "6") int months) {
        return adminService.getOrderCharts(months);
    }

    @GetMapping("/stock-alerts")
    @Operation(summary = "Products with low stock")
    public List<ProductResponse> stockAlerts() {
        return stockAlertService.getLowStockProducts();
    }

    @GetMapping("/audit-logs")
    @Operation(summary = "Recent audit logs")
    public List<AuditLogResponse> auditLogs() {
        return auditService.recentLogs();
    }
}
