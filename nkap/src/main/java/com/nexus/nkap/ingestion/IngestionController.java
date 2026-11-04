package com.nexus.nkap.ingestion;

import com.nexus.nkap.common.BusinessException;
import com.nexus.nkap.common.Result;
import com.nexus.nkap.common.ResultCode;
import com.nexus.nkap.ingestion.dto.UploadResponse;
import org.springframework.http.MediaType;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

/**
 * 文档上传控制器（阶段 1）。
 *
 * <p>接口：{@code POST /api/v1/ingestion/upload}（multipart/form-data）。
 * {@code file} 为二进制文件字段，其余为表单字段（对齐《11 接口设计》§3.1 口径）。
 *
 * <p>MVP 阶段暂未接入鉴权，{@code tenant_id} / {@code space_id} 以表单字段传入；
 * 正式鉴权（JWT）在 Step 5 补齐后改为从认证主体获取。
 *
 * <p>WebFlux 非阻塞处理：文件先 {@code transferTo} 落临时文件，
 * 再在弹性线程池中执行同步的哈希计算 / MinIO 上传 / 落库（§2.2）。
 */
@RestController
@RequestMapping("/api/v1/ingestion")
public class IngestionController {

    private final IngestionService ingestionService;

    public IngestionController(IngestionService ingestionService) {
        this.ingestionService = ingestionService;
    }

    /**
     * 上传文档：接收文件 → 去重 → 落 MinIO → 落库 → 返回 document_id / version_id。
     */
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Mono<Result<UploadResponse>> upload(
            @RequestPart("file") FilePart file,
            @RequestPart("tenant_id") String tenantId,
            @RequestPart("space_id") String spaceId) {
        UUID tenant = parseUuid(tenantId, "tenant_id");
        UUID space = parseUuid(spaceId, "space_id");

        Path tempFile = tempUploadPath(file.filename());
        return file.transferTo(tempFile)
                .subscribeOn(Schedulers.boundedElastic())
                .then(Mono.fromCallable(() -> Result.success(ingestionService.upload(
                        tempFile, file.filename(), contentTypeOf(file), tenant, space))))
                .doFinally(signal -> deleteQuietly(tempFile));
    }

    /**
     * 生成临时上传文件路径（避免在事件循环线程执行阻塞 I/O）。
     */
    private Path tempUploadPath(String filename) {
        String suffix = (filename != null && filename.contains("."))
                ? filename.substring(filename.lastIndexOf('.')) : "";
        return Path.of(System.getProperty("java.io.tmpdir"),
                "nkap-upload-" + UUID.randomUUID() + suffix);
    }

    private String contentTypeOf(FilePart file) {
        var contentType = file.headers().getContentType();
        return contentType == null ? null : contentType.toString();
    }

    private void deleteQuietly(Path path) {
        try {
            Files.deleteIfExists(path);
        } catch (IOException ignored) {
            // 临时文件清理失败不影响主流程
        }
    }

    private UUID parseUuid(String raw, String field) {
        try {
            return UUID.fromString(raw);
        } catch (IllegalArgumentException e) {
            throw new BusinessException(ResultCode.BAD_REQUEST, field + " must be a valid UUID");
        }
    }
}
