package com.nexus.nkap;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 星枢 Nexus（NKAP）应用启动类。
 *
 * <p>Step 0 骨架占位：仅保证应用可启动、健康检查返回 200。
 * 后续按 Step 1 ~ Step 5 逐步填充 modules 下各业务包。
 */
@SpringBootApplication
public class KapApplication {

    public static void main(String[] args) {
        SpringApplication.run(KapApplication.class, args);
    }
}
