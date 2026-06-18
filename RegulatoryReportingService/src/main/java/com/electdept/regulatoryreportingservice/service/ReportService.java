package com.electdept.regulatoryreportingservice.service;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class ReportService {

    private final JdbcTemplate jdbc;

    public ReportService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** Aggregate stock on hand per SKU across all warehouses/bins. */
    public List<Map<String, Object>> stockSummary() {
        return jdbc.queryForList(
            "SELECT sl.sku_id, COALESCE(s.sku_code, '?') AS sku_code, " +
            "       SUM(sl.qty_on_hand) AS total_qty " +
            "FROM inventory.stock_level sl " +
            "LEFT JOIN catalog.sku s ON s.id = sl.sku_id " +
            "GROUP BY sl.sku_id, s.sku_code " +
            "ORDER BY total_qty DESC"
        );
    }

    /** PO and SO counts and totals by status. */
    public Map<String, Object> ordersSummary() {
        List<Map<String, Object>> poStatuses = jdbc.queryForList(
            "SELECT status, COUNT(*) AS cnt, COALESCE(SUM(total_amount), 0) AS total " +
            "FROM procurement.purchase_order GROUP BY status ORDER BY status"
        );
        List<Map<String, Object>> soStatuses = jdbc.queryForList(
            "SELECT status, COUNT(*) AS cnt, COALESCE(SUM(total_amount), 0) AS total " +
            "FROM sales.sales_order GROUP BY status ORDER BY status"
        );
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("purchaseOrders", poStatuses);
        out.put("salesOrders", soStatuses);
        return out;
    }

    /** Per-user activity counts from audit + auth. */
    public List<Map<String, Object>> activityByUser() {
        return jdbc.queryForList(
            "SELECT u.id AS user_id, u.email, COUNT(a.id) AS event_count " +
            "FROM auth.app_user u " +
            "LEFT JOIN audit.audit_event a ON a.user_id = u.id " +
            "GROUP BY u.id, u.email " +
            "ORDER BY event_count DESC"
        );
    }

    /** Recent ledger entries with denormalized SKU code + user email. */
    public List<Map<String, Object>> recentLedger(int limit) {
        return jdbc.queryForList(
            "SELECT sl.id, sl.created_at, sl.delta, sl.reason_code, sl.ref_doc_type, sl.ref_doc_id, " +
            "       COALESCE(s.sku_code, '?') AS sku_code, COALESCE(u.email, '?') AS user_email " +
            "FROM inventory.stock_ledger sl " +
            "LEFT JOIN catalog.sku s ON s.id = sl.sku_id " +
            "LEFT JOIN auth.app_user u ON u.id = sl.user_id " +
            "ORDER BY sl.created_at DESC " +
            "LIMIT ?", limit
        );
    }

    /** Composite dashboard — one call returns everything. */
    public Map<String, Object> dashboard() {
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("stockSummary", stockSummary());
        out.put("ordersSummary", ordersSummary());
        out.put("activityByUser", activityByUser());
        out.put("recentLedger", recentLedger(10));
        return out;
    }
}
