package com.nexus.nkap.api;

import com.nexus.nkap.common.BusinessException;
import com.nexus.nkap.common.Result;
import com.nexus.nkap.common.ResultCode;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 健康检查控制器（Step 1 验收用）。
 *
 * <p>提供两个端点：
 * <ul>
 *   <li>{@code GET /api/v1/health-check}：正常返回统一响应结构；</li>
 *   <li>{@code GET /api/v1/health-check/error}：抛出 {@link BusinessException}，
 *       用于验证 {@code GlobalExceptionHandler} 的全局异常拦截是否生效。</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/v1/health-check")
public class HealthCheckController {

    /**
     * 正常健康检查：返回统一响应结构。
     */
    @GetMapping
    public Result<String> healthCheck() {
        return Result.success("health check ok");
    }

    /**
     * 故意抛出业务异常，验证全局异常处理器的拦截与转换。
     */
    @GetMapping("/error")
    public Result<String> error() {
        throw new BusinessException(ResultCode.NOT_FOUND, "simulated business error for validation");
    }
}
