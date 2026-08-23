package com.chuxi.web;

import com.chuxi.common.ClientIpResolver;
import com.chuxi.common.R;
import com.chuxi.common.RateLimiter;
import com.chuxi.service.AiChatService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping({"/api/front/ai", "/api/ai"})
public class AiController {
    private final AiChatService aiChatService;
    private final ClientIpResolver clientIpResolver;

    public AiController(AiChatService aiChatService, ClientIpResolver clientIpResolver) {
        this.aiChatService = aiChatService;
        this.clientIpResolver = clientIpResolver;
    }

    @PostMapping("/chat")
    public R<AiChatService.ChatResult> chat(@Valid @RequestBody ChatRequest request, HttpServletRequest servletRequest) {
        String ip = clientIpResolver.resolve(servletRequest);
        if (!RateLimiter.tryAcquire("ai:" + ip, 60, 10)) return R.fail("请求过于频繁，请稍后再试");
        List<AiChatService.Message> messages = request.messages() == null ? List.of() : request.messages().stream()
                .map(m -> new AiChatService.Message(m.role(), m.content())).toList();
        if (messages.isEmpty() && request.message() != null) messages = List.of(new AiChatService.Message("user", request.message()));
        if (messages.isEmpty()) throw new IllegalArgumentException("消息不能为空");
        return R.ok(aiChatService.chat(messages));
    }

    public record ChatRequest(@Size(max = 16, message = "消息数量过多") List<@Valid AiMessage> messages,
                              @Size(max = 2000, message = "消息过长") String message) {}

    public record AiMessage(@NotBlank(message = "消息角色不能为空") String role,
                            @NotBlank(message = "消息不能为空") @Size(max = 2000, message = "消息过长") String content) {}
}
