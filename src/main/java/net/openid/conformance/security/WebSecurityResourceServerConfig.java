package net.openid.conformance.security;

import jakarta.ws.rs.HttpMethod;
import net.openid.conformance.support.mitre.compat.introspect.IntrospectionAuthorityGranter;
import net.openid.conformance.support.mitre.compat.introspect.IntrospectionConfigurationService;
import net.openid.conformance.support.mitre.compat.introspect.ResourceServerTokenServices;
import net.openid.conformance.support.mitre.compat.introspect.SimpleIntrospectionAuthorityGranter;
import net.openid.conformance.support.mitre.compat.introspect.StaticIntrospectionConfigurationService;
import net.openid.conformance.support.mitre.compat.model.RegisteredClient;
import net.openid.conformance.token.ApiTokenService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.util.matcher.AndRequestMatcher;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.OrRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;

@Configuration
@Order(1)
@SuppressWarnings({"deprecation"})
public class WebSecurityResourceServerConfig
//	extends WebSecurityConfigurerAdapter
{

	@Value("${fintechlabs.devmode:false}")
	private boolean devmode;

	// Config for the OAuth introspection filters
	@Value("${oauth.introspection_url}")
	private String introspectionUrl;
	@Value("${oauth.resource_id}")
	private String resourceId;
	@Value("${oauth.resource_secret}")
	private String resourceSecret;

	@Autowired
	private DummyUserFilter dummyUserFilter;

//	@Override
	@Bean
	protected SecurityFilterChain filterChainResourceServer(HttpSecurity http) throws Exception {

		// @formatter:off

		http
			.authorizeHttpRequests(requests -> requests.requestMatchers(getApiMatcher()))
				.csrf(AbstractHttpConfigurer::disable)
				.sessionManagement(sessions -> sessions.sessionCreationPolicy(SessionCreationPolicy.NEVER))
				.authorizeHttpRequests(httpRequests -> {
					httpRequests.requestMatchers(getPublicMatcher()).permitAll();
					httpRequests.requestMatchers(getApiMatcher()).authenticated();
				}) //
//				.addFilterBefore(oauth2Filter(), AbstractPreAuthenticatedProcessingFilter.class)
			.exceptionHandling(exceptions -> exceptions.authenticationEntryPoint(restAuthenticationEntryPoint()));

		// @formatter:off

		if (devmode) {
			// TODO add url limitedoauth filter
//			http.addFilterBefore(dummyUserFilter, UrlLimitedOAuth2AuthenticationProcessingFilter.class);
		}

		return http.build();
	}

//	/**
//	 * @return
//	 */
//	@Bean
//	public Filter oauth2Filter() {
//
//		UrlLimitedOAuth2AuthenticationProcessingFilter filter = new UrlLimitedOAuth2AuthenticationProcessingFilter();
//		filter.setAuthenticationManager(oauthAuthenticationManager());
//		filter.setAuthenticationEntryPoint(restAuthenticationEntryPoint());
//		filter.setMatcher(getMatcher());
//		filter.setStateless(false);
//
//		return filter;
//	}

	private RequestMatcher getApiMatcher() {
		return new OrRequestMatcher(
			new AntPathRequestMatcher("/api/currentuser"),
			new AntPathRequestMatcher("/api/runner/**"),
			new AntPathRequestMatcher("/api/log/**"),
			new AntPathRequestMatcher("/api/info/**"),
			new AntPathRequestMatcher("/api/plan/**"),
			new AntPathRequestMatcher("/api/token/**"),
			new AntPathRequestMatcher("/api/lastconfig")
			);
	}

	private RequestMatcher getPublicMatcher() {
		return new AndRequestMatcher(
			new OrRequestMatcher(
				new AntPathRequestMatcher("/api/info/?*", HttpMethod.GET),
				new AntPathRequestMatcher("/api/log", HttpMethod.GET),
				new AntPathRequestMatcher("/api/log/?*", HttpMethod.GET),
				new AntPathRequestMatcher("/api/log/export/?*", HttpMethod.GET),
				new AntPathRequestMatcher("/api/plan", HttpMethod.GET),
				new AntPathRequestMatcher("/api/plan/?*", HttpMethod.GET),
				new AntPathRequestMatcher("/api/plan/export/?*", HttpMethod.GET)),
			new PublicRequestMatcher());
	}

//	/**
//	 * @return
//	 */
//	@Bean
//	public AuthenticationManager oauthAuthenticationManager(ResourceServerTokenServices tokenServices) {
//		OAuth2AuthenticationManager oAuth2AuthenticationManager = new OAuth2AuthenticationManager();
//
//		oAuth2AuthenticationManager.setTokenServices(tokenServices);
//
//		return oAuth2AuthenticationManager;
//
//	}

	@Bean
	@Primary
	public ResourceServerTokenServices tokenServices() {

		ApiTokenService tokenService = new ApiTokenService();

		// Static introspection is disabled for now - it can be added to the chain as a fallback here:
		//tokenService.setFallbackService(introspectingTokenServices());

		return tokenService;
	}

//	@Bean
//	public ResourceServerTokenServices introspectingTokenServices() {
//
//		IntrospectingTokenService tokenService = new IntrospectingTokenService();
//		tokenService.setCacheTokens(true);
//		tokenService.setCacheNonExpiringTokens(true);
//		tokenService.setIntrospectionConfigurationService(introspectionConfiguration());
//		tokenService.setIntrospectionAuthorityGranter(introspectionAuthorityGranter());
//
//		return tokenService;
//	}

	@Bean
	public IntrospectionAuthorityGranter introspectionAuthorityGranter() {
		SimpleIntrospectionAuthorityGranter authorityGranter = new SimpleIntrospectionAuthorityGranter();

		// Default authorities, so access via oauth will map to a normal user (no admin permissions)

		return authorityGranter;
	}

	@Bean
	public IntrospectionConfigurationService introspectionConfiguration() {
		StaticIntrospectionConfigurationService config = new StaticIntrospectionConfigurationService();

		config.setClientConfiguration(getResource());

		// set this value to the introspection endpoint of this system's trusted AS
		config.setIntrospectionUrl(introspectionUrl);

		return config;
	}

	@Bean
	public RegisteredClient getResource() {
		RegisteredClient resource = new RegisteredClient();

		// set these values to the keys needed to call the introspection endpoint
		resource.setClientId(resourceId);
		resource.setClientSecret(resourceSecret);

		return resource;

	}

	@Bean
	public RestAuthenticationEntryPoint restAuthenticationEntryPoint() {
		return new RestAuthenticationEntryPoint();
	}

}
