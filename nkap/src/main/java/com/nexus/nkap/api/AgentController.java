package com.nexus.nkap.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.nexus.nkap.common.Result;
import com.nexus.nkap.modules.agent.RagAgentService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/agent")
public class AgentController {

    private final RagAgentService ragAgentService;

    public AgentController(RagAgentService ragAgentService) {
        this.ragAgentService = ragAgentService;
    }

    @PostMapping("/chat")
    public Mono<Result<String>> chat(@RequestBody ChatRequest request) {
        return Mono.fromCallable(() -> Result.success(
                        ragAgentService.chat(request.query(), request.tenantId(), request.spaceId())))
                .subscribeOn(Schedulers.boundedElastic());
    }

    public record ChatRequest(
            String query,
            @JsonProperty("tenant_id") UUID tenantId,
            @JsonProperty("space_id") UUID spaceId
    ) {
    }
}