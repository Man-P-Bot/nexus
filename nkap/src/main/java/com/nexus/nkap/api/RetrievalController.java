package com.nexus.nkap.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.nexus.nkap.common.Result;
import com.nexus.nkap.modules.retrieval.RetrievalResult;
import com.nexus.nkap.modules.retrieval.RetrievalService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.List;
import java.util.UUID;

/**
 * 检索控制器（MVP）。
 */
@RestController
@RequestMapping("/api/v1/retrieval")
public class RetrievalController {

    private final RetrievalService retrievalService;

    public RetrievalController(RetrievalService retrievalService) {
        this.retrievalService = retrievalService;
    }

    /**
     * 关键词检索。
     */
    @PostMapping("/search")
    public Mono<Result<List<RetrievalResult>>> search(@RequestBody SearchRequest request) {
        // 处理 limit 为空的情况，默认给 5
        int limit = request.limit() != null ? request.limit() : 5;

        return Mono.fromCallable(() -> Result.success(
                        retrievalService.search(request.query(), request.tenantId(), request.spaceId(), limit)))
                .subscribeOn(Schedulers.boundedElastic());
    }

    /**
     * 检索请求体。
     * 使用 @JsonProperty 注解，使得前端可以继续传下划线风格的字段（tenant_id, space_id）。
     */
    public record SearchRequest(
            String query,
            @JsonProperty("tenant_id") UUID tenantId,
            @JsonProperty("space_id") UUID spaceId,
            Integer limit
    ) {
    }
}