package net.openid.conformance.security;

import net.openid.conformance.security.idp.RolesAuthoritiesConverter;
import net.openid.conformance.sharing.privatelink.ShareJwtBearerAuthenticationProvider;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.mock.web.MockServletContext;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationProvider;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;

/**
 * oidc.idp.* is optional and has no defaults, so that a deployment does not trust
 * an IdP nobody chose: without an issuer the API accepts no IdP tokens. An issuer
 * without an API audience stops startup.
 */
public class IdpBearerTokenConfiguration_UnitTest {

	@Configuration
	@EnableConfigurationProperties(IdpProperties.class)
	static class ConditionalBeanConfig {
		@Bean
		@Conditional(WebSecurityResourceServerConfig.IdpIssuerConfigured.class)
		String idpProvider() {
			return "provider";
		}
	}

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner(() -> {
			// Without the process environment, where oidc.idp.iss may be set.
			AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
			context.setEnvironment(new MockEnvironment());
			return context;
		})
		.withUserConfiguration(ConditionalBeanConfig.class);

	@Test
	public void without_an_issuer_the_provider_is_not_defined() {
		contextRunner.run(context -> Assertions.assertFalse(context.containsBean("idpProvider")));
		contextRunner.withPropertyValues("oidc.idp.iss=", "oidc.idp.api-audience=conformance-suite-api")
			.run(context -> Assertions.assertFalse(context.containsBean("idpProvider")));
		contextRunner.withPropertyValues("oidc.idp.iss=  ")
			.run(context -> Assertions.assertFalse(context.containsBean("idpProvider")));
	}

	@Test
	public void with_an_issuer_the_provider_is_defined() {
		contextRunner.withPropertyValues("oidc.idp.iss=https://idp.example.com")
			.run(context -> Assertions.assertTrue(context.containsBean("idpProvider")));
	}

	@Test
	public void the_properties_bind_from_their_kebab_case_names() {
		contextRunner.withPropertyValues("oidc.idp.iss=https://idp.example.com",
				"oidc.idp.api-audience=conformance-suite-api", "oidc.idp.access-token-signing-algs=ES256")
			.run(context -> Assertions.assertEquals(
				new IdpProperties("https://idp.example.com", "conformance-suite-api", "ES256"),
				context.getBean(IdpProperties.class)));
	}

	@Configuration
	@EnableWebSecurity
	@Import(WebSecurityResourceServerConfig.class)
	static class ResourceServerConfig {
		@Bean
		AuthenticationFacade authenticationFacade() {
			return Mockito.mock(AuthenticationFacade.class);
		}

		@Bean
		DummyUserFilter dummyUserFilter() {
			return Mockito.mock(DummyUserFilter.class);
		}

		@Bean
		ApiTokenAuthenticationProvider apiTokenAuthenticationProvider() {
			return Mockito.mock(ApiTokenAuthenticationProvider.class);
		}

		@Bean
		ShareJwtBearerAuthenticationProvider shareJwtBearerAuthenticationProvider() {
			return Mockito.mock(ShareJwtBearerAuthenticationProvider.class);
		}

		@Bean
		RolesAuthoritiesConverter rolesAuthoritiesConverter() {
			return new RolesAuthoritiesConverter("conformance-admin");
		}
	}

	/**
	 * With an issuer the real resource-server configuration defines the IdP decoder
	 * and provider. Building them must not contact the IdP: port 1 on the loopback
	 * address refuses connections.
	 */
	@Test
	public void the_resource_server_configuration_starts_with_an_issuer() {
		try (var context = new AnnotationConfigWebApplicationContext()) {
			context.setServletContext(new MockServletContext());
			context.setEnvironment(new MockEnvironment()
				.withProperty("oidc.idp.iss", "http://127.0.0.1:1")
				.withProperty("oidc.idp.api-audience", "conformance-suite-api"));
			context.register(ResourceServerConfig.class);
			context.refresh();

			Assertions.assertNotNull(context.getBean(IdpJwtDecoder.class));
			Assertions.assertNotNull(context.getBean(JwtAuthenticationProvider.class));
		}
	}

	@Test
	public void an_issuer_without_an_api_audience_does_not_start() {
		for (String audience : new String[] {null, "", " "}) {
			IllegalStateException e = Assertions.assertThrows(IllegalStateException.class,
				() -> IdpJwtDecoder.forIssuer(new IdpProperties("https://idp.example.com", audience, "")));

			Assertions.assertTrue(e.getMessage().contains("OIDC_IDP_API_AUDIENCE"), e.getMessage());
		}
	}

	/**
	 * Building the decoder must not contact the IdP, or the suite could not start
	 * while the IdP is down. Port 1 on the loopback address refuses connections.
	 */
	@Test
	public void a_configured_decoder_is_built_without_contacting_the_idp() {
		Assertions.assertNotNull(IdpJwtDecoder.forIssuer(
			new IdpProperties("http://127.0.0.1:1", "conformance-suite-api", "")));
	}

	@Test
	public void an_invalid_signing_algorithm_does_not_start() {
		Assertions.assertThrows(IllegalStateException.class,
			() -> IdpJwtDecoder.forIssuer(new IdpProperties("https://idp.example.com", "conformance-suite-api", "HS256")));
	}
}
