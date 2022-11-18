package net.openid.conformance.security;

import net.openid.conformance.runner.TestDispatcher;
import org.apache.catalina.filters.CorsFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.security.web.header.HeaderWriter;
import org.springframework.security.web.header.writers.DelegatingRequestMatcherHeaderWriter;
import org.springframework.security.web.header.writers.frameoptions.XFrameOptionsHeaderWriter;
import org.springframework.security.web.util.matcher.*;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.cors.CorsConfiguration;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Configuration
@EnableWebSecurity
public class WebSecurityConfiguration {

	private static final Logger logger = LoggerFactory.getLogger(DummyUserFilter.class);

	@Value("${fintechlabs.devmode:false}")
	private boolean devmode;

	@Value("${fintechlabs.base_url}")
	private String baseURL;

	// Client name to use when dynamically registering as a client
	@Value("${oidc.clientname}")
	private String clientName;

	// Redirect URI to use
	@Value("${oidc.redirecturi}")
	private String redirectURI;


	// Specifics for setting up a Static Client for Google


	// Config for the admin role
	@Value("${oidc.admin.domains:}")
	private String adminDomains;
	@Value("${oidc.admin.group:}")
	private String adminGroup;
	@Value("${oidc.admin.issuer}")
	private String adminIss;

	@Value("${oidc.google.clientid:googleClientId}")
	private String googleClientId;

	@Value("${oidc.google.secret}")
	private String googleClientSecret;

	@Value("${oidc.google.iss:https://accounts.google.com}")
	private String googleIss;

	// Static Client for gitlab
	@Value("${oidc.gitlab.clientid:gitlabClientId}")
	private String gitlabClientId;

	@Value("${oidc.gitlab.secret}")
	private String gitlabClientSecret;

	@Value("${oidc.gitlab.iss:https://gitlab.com}")
	private String gitlabIss;


	@Autowired
	private DummyUserFilter dummyUserFilter;


	@Bean
	public LoginUrlAuthenticationEntryPoint authenticationEntryPoint() {
		return new LoginUrlAuthenticationEntryPoint(baseURL + "/openid_connect_login");
	}


	@Bean
	public ClientRegistrationRepository clientRegistrationRepository() {
		return new MongoDBRegisteredClientService(googleClientId, googleClientSecret, redirectURI,
			gitlabClientId, gitlabClientSecret, gitlabIss, clientName);
	}


	protected void configure(HttpSecurity http) throws Exception {

		http
			.authorizeHttpRequests((authz) -> authz
				.antMatchers("/login.html", "/css/**", "/js/**", "/images/**", "/templates/**", "/favicon.ico", "/test-mtls/**", "/test/**", "/jwks**", "/logout.html", "/robots.txt", "/.well-known/**")
				.permitAll()
				.requestMatchers(publicRequestMatcher("/log-detail.html", "/logs.html", "/plan-detail.html", "/plans.html"))
				.permitAll()
				.anyRequest()
				.authenticated()
			)
			.csrf(csrf -> csrf.disable())
			.sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.ALWAYS))
			.logout(l -> l.logoutSuccessUrl("/login.html"))
			.headers(h -> h
				.frameOptions(f -> f.disable())
				.addHeaderWriter(getXFrameOptionsHeaderWriter())
			)
			.cors(c -> c.configurationSource(getCorsConfigurationSource()))
			.exceptionHandling(e -> e.authenticationEntryPoint(authenticationEntryPoint()))
			.oauth2Login(o -> o
				.userInfoEndpoint(userInfo -> userInfo
					.oidcUserService(createOidcUserService())
				));

//		http.csrf().disable()
//			.authorizeRequests()
//			.antMatchers("/login.html", "/css/**", "/js/**", "/images/**", "/templates/**", "/favicon.ico", "/test-mtls/**", "/test/**", "/jwks**", "/logout.html", "/robots.txt", "/.well-known/**")
//			.permitAll()
//			.and().authorizeRequests()
//			.requestMatchers(publicRequestMatcher("/log-detail.html", "/logs.html", "/plan-detail.html", "/plans.html"))
//			.permitAll()
//			.anyRequest()
//			.authenticated()
//			.and()
//			.addFilterBefore(openIdConnectAuthenticationFilter(), AbstractPreAuthenticatedProcessingFilter.class)
//			.exceptionHandling()
//			.authenticationEntryPoint(authenticationEntryPoint())
//			.and()
//			.sessionManagement().sessionCreationPolicy(SessionCreationPolicy.ALWAYS)
//			.and()
//			.logout()
//			.logoutSuccessUrl("/login.html")
//			.and()
//			//added to disable x-frame-options only for certain paths
//			.headers().frameOptions().disable()
//			.and()
//			.headers().addHeaderWriter(getXFrameOptionsHeaderWriter())
//			.and()
//			.cors().configurationSource(getCorsConfigurationSource());


		if (devmode) {
			logger.warn("\n***\n*** Starting application in Dev Mode, injecting dummy user into requests.\n***\n");
			http.addFilterAfter(dummyUserFilter, CorsFilter.class);
		}
	}

	private OAuth2UserService<OidcUserRequest, OidcUser> createOidcUserService() {
		return new OidcUserService(adminIss, "groups", adminGroup);
	}


	protected HeaderWriter getXFrameOptionsHeaderWriter() {

		AntPathRequestMatcher checkSessionIframeMatcher = new AntPathRequestMatcher("/**/check_session_iframe");
		AntPathRequestMatcher getSessionStateMatcher = new AntPathRequestMatcher("/**/get_session_state");
		RequestMatcher orRequestMatcher = new OrRequestMatcher(checkSessionIframeMatcher, getSessionStateMatcher);

		NegatedRequestMatcher negatedRequestMatcher = new NegatedRequestMatcher(orRequestMatcher);
		//default to SAMEORIGIN except the above endpoints
		XFrameOptionsHeaderWriter xFrameOptionsHeaderWriter = new XFrameOptionsHeaderWriter(XFrameOptionsHeaderWriter.XFrameOptionsMode.SAMEORIGIN);
		DelegatingRequestMatcherHeaderWriter writer = new DelegatingRequestMatcherHeaderWriter(negatedRequestMatcher, xFrameOptionsHeaderWriter);

		return writer;
	}

	// For more info regarding the CORS handling in the conformance suite, please refer to
	// https://gitlab.com/openid/conformance-suite/-/merge_requests/1175#note_1020913221
	protected AdditiveUrlBasedCorsConfigurationSource getCorsConfigurationSource() {

		CorsConfiguration configuration = new CorsConfiguration();
		configuration.setAllowedOrigins(Arrays.asList("*"));
		configuration.setAllowedMethods(Arrays.asList("GET", "POST"));
		configuration.setAllowedHeaders(List.of(CorsConfiguration.ALL));
		configuration.setExposedHeaders(List.of("WWW-Authenticate", "DPoP-Nonce"));

		AdditiveUrlBasedCorsConfigurationSource source = new AdditiveUrlBasedCorsConfigurationSource();
		source.setPathMatcher(new AntPathMatcher());
		source.registerCorsConfiguration(TestDispatcher.TEST_PATH + "**", configuration);
		source.registerCorsConfiguration(TestDispatcher.TEST_MTLS_PATH + "**", configuration);

		return source;
	}

	private RequestMatcher publicRequestMatcher(String... patterns) {

		return new AndRequestMatcher(
			new OrRequestMatcher(
				Arrays.asList(patterns)
					.stream()
					.map(AntPathRequestMatcher::new)
					.collect(Collectors.toList())),
			new PublicRequestMatcher());
	}

}
