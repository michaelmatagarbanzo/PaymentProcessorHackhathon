package com.ecommerce.sale.presentation.controller;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/public")
public class PublicTimeController {

    @GetMapping("/time")
    public Map<String, Object> getServerTime() {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("time", Instant.now().toString());
        return response;
    }
}
