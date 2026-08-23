package com.chuxi.web;

import com.chuxi.common.R;
import com.chuxi.service.AiConfigService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/** AI 后台配置接口，鉴权由 AdminAuthInterceptor 统一处理。 */
@RestController
@RequestMapping("/api/admin/ai")
public class AiAdminController {
    private final AiConfigService service;

    public AiAdminController(AiConfigService service) {
        this.service = service;
    }

    @GetMapping("/config")
    public R<Map<String, Object>> getConfig() {
        return R.ok(service.getConfig());
    }

    @PutMapping("/config")
    public R<Map<String, Object>> updateConfig(@RequestBody Map<String, Object> body) {
        return R.ok(service.updateConfig(body));
    }
}
