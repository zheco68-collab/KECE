package com.kece.common.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/** JWT 配置项，绑定 application.yml 中 {@code kece.jwt.*}。 */
@Data
@Component
@ConfigurationProperties(prefix = "kece.jwt")
public class JwtProperties {

    /** 签名密钥（须 >= 32 字节，满足 HS256）。 */
    private String secret;

    /** 访问令牌有效期（毫秒）。 */
    private long expiration = 86400000L;

    /** 刷新令牌有效期（毫秒）。 */
    private long refreshExpiration = 604800000L;
}
