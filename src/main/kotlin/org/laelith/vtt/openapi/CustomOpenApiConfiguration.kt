package org.laelith.vtt.openapi

import io.swagger.v3.oas.models.Components
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.security.OAuthFlow
import io.swagger.v3.oas.models.security.OAuthFlows
import io.swagger.v3.oas.models.security.Scopes
import io.swagger.v3.oas.models.security.SecurityRequirement

@Configuration
class CustomOpenApiConfiguration {
    @Bean
    fun customOpenApi(): OpenAPI {
        val openapi = OpenAPI()
        val authorizationCodeFlow =
            OAuthFlows()
                .authorizationCode(
                    OAuthFlow()
                        .scopes(Scopes()
                            .addString("openid", "Open Id")
                        )
                        .tokenUrl("https://id.laelith.com/api/realms/laelith/token")
                        .refreshUrl("https://id.laelith.com/api/realms/laelith/authorize")
                        .authorizationUrl("https://id.laelith.com/api/realms/laelith/authorize"))
        openapi.components = Components()
        openapi.components.addSecuritySchemes("oAuth2AuthCode",
            io.swagger.v3.oas.models.security.SecurityScheme()
                .type(io.swagger.v3.oas.models.security.SecurityScheme.Type.OAUTH2)
                .flows(
        authorizationCodeFlow))
        val securityRequirement = SecurityRequirement().addList("oAuth2AuthCode")
        openapi.addSecurityItem(securityRequirement)
        return openapi
    }
}