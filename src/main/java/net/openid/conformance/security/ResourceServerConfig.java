package net.openid.conformance.security;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.web.util.matcher.AndRequestMatcher;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.OrRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;

import javax.ws.rs.HttpMethod;

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

		http
			.authorizeHttpRequests((authorize) -> authorize
				.requestMatchers(getMatcher()).authenticated()
				.requestMatchers(getPublicMatcher()).permitAll()
			)
			.csrf((csrf) -> csrf.disable())

			.oauth2ResourceServer((oauth2) -> oauth2
				.opaqueToken(t ->
					t.introspector(token -> {
						//throw new OAuth2IntrospectionException("requestEntityConverter returned a null entity");
						return null;
					}))
			)
			.exceptionHandling(ex ->
				ex.authenticationEntryPoint(restAuthenticationEntryPoint()));

//		if (devmode) {
//			http.addFilterBefore(dummyUserFilter, UrlLimitedOAuth2AuthenticationProcessingFilter.class);
//		}
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


	@Bean
	public RestAuthenticationEntryPoint restAuthenticationEntryPoint() {
		return new RestAuthenticationEntryPoint();
	}

}
