package org.laelith.vtt.api.security

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpMethod
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity
import org.springframework.security.config.web.server.ServerHttpSecurity
import org.springframework.security.converter.RsaKeyConverters
import org.springframework.security.oauth2.jwt.NimbusReactiveJwtDecoder
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder
import org.springframework.security.web.server.SecurityWebFilterChain
import java.io.ByteArrayInputStream
import java.security.interfaces.RSAPublicKey

@Configuration
@EnableWebFluxSecurity
class SecurityConfig(@Value("\${security.jwt.public-key}") val key: String) {
    val rsaPublicKey: RSAPublicKey? = RsaKeyConverters.x509().convert(ByteArrayInputStream(key.toByteArray()))

    @Bean
    fun filterChain(http: ServerHttpSecurity): SecurityWebFilterChain {
        http.csrf().disable()
            .cors()
            .and()
            .authorizeExchange { exchange ->
                exchange.pathMatchers(HttpMethod.OPTIONS).permitAll()
                exchange.pathMatchers(HttpMethod.GET, "/").permitAll()
                exchange.pathMatchers(HttpMethod.GET, "/swagger-ui.html").permitAll()
                exchange.pathMatchers(HttpMethod.GET, "/webjars/swagger-ui/**").permitAll()
                exchange.pathMatchers(HttpMethod.GET, "/v3/api-docs/**").permitAll()
                exchange.pathMatchers(HttpMethod.GET, "/info").permitAll()
                exchange.anyExchange().authenticated()
            }
            .oauth2ResourceServer { oauth2 -> oauth2.jwt { jwt -> jwt.jwtDecoder(jwtDecoder())}}
        return http.build()
    }

    @Bean
    fun jwtDecoder(): ReactiveJwtDecoder {
        return NimbusReactiveJwtDecoder.withPublicKey(rsaPublicKey).build()
    }
}
