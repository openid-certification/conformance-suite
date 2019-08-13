package io.fintechlabs.testframework.security;

import javax.servlet.Filter;
import javax.ws.rs.HttpMethod;

import org.mitre.oauth2.introspectingfilter.IntrospectingTokenService;
import org.mitre.oauth2.introspectingfilter.service.IntrospectionAuthorityGranter;
import org.mitre.oauth2.introspectingfilter.service.IntrospectionConfigurationService;
import org.mitre.oauth2.introspectingfilter.service.impl.SimpleIntrospectionAuthorityGranter;
import org.mitre.oauth2.introspectingfilter.service.impl.StaticIntrospectionConfigurationService;
import org.mitre.oauth2.model.RegisteredClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.provider.authentication.OAuth2AuthenticationManager;
import org.springframework.security.oauth2.provider.token.ResourceServerTokenServices;
import org.springframework.security.web.authentication.preauth.AbstractPreAuthenticatedProcessingFilter;
import org.springframework.security.web.util.matcher.AndRequestMatcher;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.OrRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;

import io.fintechlabs.testframework.token.ApiTokenService;

@Configuration
@Order(1)
public class ResourceServerConfig extends WebSecurityConfigurerAdapter {

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

	@Override
	protected void configure(HttpSecurity http) throws Exception {

		// @formatter:off

		http
			.requestMatchers()
				.requestMatchers(getMatcher())
			.and()
				.csrf().disable()
				.sessionManagement().sessionCreationPolicy(SessionCreationPolicy.NEVER)
			.and()
				.authorizeRequests()
					.requestMatchers(getPublicMatcher())
					.permitAll()
			.and()
				.authorizeRequests()
					.requestMatchers(getMatcher())
					.authenticated()
			.and()
				.addFilterBefore(oauth2Filter(), AbstractPreAuthenticatedProcessingFilter.class)
			.exceptionHandling()
				.authenticationEntryPoint(restAuthenticationEntryPoint());

		// @formatter:off

		if (devmode) {
			http.addFilterBefore(dummyUserFilter, UrlLimitedOAuth2AuthenticationProcessingFilter.class);
		}
	}

	/**
	 * @return
	 */
	@Bean
	public Filter oauth2Filter() {

		UrlLimitedOAuth2AuthenticationProcessingFilter filter = new UrlLimitedOAuth2AuthenticationProcessingFilter();
		filter.setAuthenticationManager(oauthAuthenticationManager());
		filter.setAuthenticationEntryPoint(restAuthenticationEntryPoint());
		filter.setMatcher(getMatcher());
		filter.setStateless(false);

		return filter;
	}

	private RequestMatcher getMatcher() {
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
				new AntPathRequestMatcher("/api/info/?*", HttpMethod.GET.toString()),
				new AntPathRequestMatcher("/api/log", HttpMethod.GET.toString()),
				new AntPathRequestMatcher("/api/log/?*", HttpMethod.GET.toString()),
				new AntPathRequestMatcher("/api/log/export/?*", HttpMethod.GET.toString()),
				new AntPathRequestMatcher("/api/plan", HttpMethod.GET.toString()),
				new AntPathRequestMatcher("/api/plan/?*", HttpMethod.GET.toString()),
				new AntPathRequestMatcher("/api/plan/export/?*", HttpMethod.GET.toString())),
			new PublicRequestMatcher());
	}

	/**
	 * @return
	 */
	@Bean
	public AuthenticationManager oauthAuthenticationManager() {
		OAuth2AuthenticationManager oAuth2AuthenticationManager = new OAuth2AuthenticationManager();

		oAuth2AuthenticationManager.setTokenServices(tokenServices());

		return oAuth2AuthenticationManager;

	}

	@Bean
	@Primary
	public ResourceServerTokenServices tokenServices() {

		ApiTokenService tokenService = new ApiTokenService();

		// Static introspection is disabled for now - it can be added to the chain as a fallback here:
		//tokenService.setFallbackService(introspectingTokenServices());

		return tokenService;
	}

	@Bean
	public ResourceServerTokenServices introspectingTokenServices() {

		IntrospectingTokenService tokenService = new IntrospectingTokenService();
		tokenService.setCacheTokens(true);
		tokenService.setCacheNonExpiringTokens(true);
		tokenService.setIntrospectionConfigurationService(introspectionConfiguration());
		tokenService.setIntrospectionAuthorityGranter(introspectionAuthorityGranter());

		return tokenService;
	}

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
