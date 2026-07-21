package davi.spf.supportflow.common.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.tags.Tag;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    public static final String SECURITY_SCHEME_NAME = "bearerAuth";

    @Bean
    OpenAPI supportFlowOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("SupportFlow API")
                        .description("API REST para gestão interna de chamados/helpdesk.")
                        .version("1.0.0")
                )
                .tags(List.of(
                        new Tag()
                                .name("1. Auth")
                                .description("Autenticação e usuário autenticado"),
                        new Tag()
                                .name("2. Users")
                                .description("Administração de usuários"),
                        new Tag()
                                .name("3. Categories")
                                .description("Administração de categorias de chamados"),
                        new Tag()
                                .name("4. Tickets")
                                .description("Gestão de chamados, comentários, histórico e avaliações"),
                        new Tag()
                                .name("5. Dashboard")
                                .description("Indicadores resumidos dos chamados")
                ))
                .components(new Components()
                        .addSecuritySchemes(SECURITY_SCHEME_NAME, new SecurityScheme()
                                .name(SECURITY_SCHEME_NAME)
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                        )
                );
    }
}
