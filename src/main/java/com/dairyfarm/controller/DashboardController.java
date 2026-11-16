package com.dairyfarm.controller;

import com.dairyfarm.service.DashboardService;
import java.util.Map;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {
    private final DashboardService dashboard;
    public DashboardController(DashboardService dashboard) { this.dashboard = dashboard; }
    @GetMapping
    public Map<String, Object> summary(@RequestParam(required = false) UUID farmUuid) {
        return dashboard.summary(farmUuid);
    }
}
