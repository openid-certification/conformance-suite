package net.openid.conformance.security;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import net.openid.conformance.support.mitre.compat.clients.DynamicServerConfigurationService;
import net.openid.conformance.support.mitre.compat.clients.HybridClientConfigurationService;
import net.openid.conformance.support.mitre.compat.clients.RegisteredClientService;
import net.openid.conformance.support.mitre.compat.issuer.HybridIssuerService;
import net.openid.conformance.support.mitre.compat.model.RegisteredClient;
import net.openid.conformance.support.mitre.compat.spring.ClientDetailsEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.security.web.header.HeaderWriter;
import org.springframework.security.web.header.writers.DelegatingRequestMatcherHeaderWriter;
import org.springframework.security.web.header.writers.frameoptions.XFrameOptionsHeaderWriter;
import org.springframework.security.web.util.matcher.AndRequestMatcher;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.NegatedRequestMatcher;
import org.springframework.security.web.util.matcher.OrRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.web.cors.CorsConfiguration;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Configuration
@Order(2)
@SuppressWarnings({"deprecation"})
public class WebSecurityOidcLoginConfig
//	extends WebSecurityConfigurerAdapter
{

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


	private final ClientDetailsEntity.AuthMethod authMethod = ClientDetailsEntity.AuthMethod.SECRET_BASIC;

	// Specifics for setting up a Static Client for Google
	@Value("${oidc.google.clientid}")
	private String googleClientId;

	@Value("${oidc.google.secret}")
	private String googleClientSecret;

	@Value("${oidc.google.iss:https://accounts.google.com}")
	private String googleIss;

	// Static Client for gitlab
	@Value("${oidc.gitlab.clientid}")
	private String gitlabClientId;

	@Value("${oidc.gitlab.secret}")
	private String gitlabClientSecret;

	@Value("${oidc.gitlab.iss:https://gitlab.com}")
	private String gitlabIss;

	// Config for the admin role
	@Value("${oidc.admin.domains:}")
	private String adminDomains;
	@Value("${oidc.admin.group:}")
	private String adminGroup;
	@Value("${oidc.admin.issuer}")
	private String adminIss;

	@Autowired
	private DummyUserFilter dummyUserFilter;

	@Autowired(required = false)
	private CorsConfigurable additionalCorsConfiguration;

	private RegisteredClient googleClientConfig() {
		RegisteredClient rc = new RegisteredClient();
		rc.setClientId(googleClientId);
		rc.setClientSecret(googleClientSecret);
		rc.setScope(ImmutableSet.of("openid", "email", "profile"));
		rc.setRedirectUris(ImmutableSet.of(redirectURI));
		return rc;
	}

	private RegisteredClient gitlabClientConfig() {
		RegisteredClient rc = new RegisteredClient();
		rc.setClientId(gitlabClientId);
		rc.setClientSecret(gitlabClientSecret);
		// email is only asked for to make it clear to the user which account they're logged into, if they have multiple gitlab ones
		rc.setScope(ImmutableSet.of("openid", "email"));
		rc.setRedirectUris(ImmutableSet.of(redirectURI));
		return rc;
	}

	// Create a partially filled in RegisteredClient to use as a template when performing Dynamic Registration
	private RegisteredClient getClientTemplate() {
		RegisteredClient clientTemplate = new RegisteredClient();
		clientTemplate.setClientName(clientName);
		clientTemplate.setScope(AuthRequestUrlBuilderWithFixedScopes.SCOPES);
		clientTemplate.setTokenEndpointAuthMethod(authMethod);
		clientTemplate.setRedirectUris(ImmutableSet.of(redirectURI));
		return clientTemplate;
	}

	// Bean to set up the server configuration service. We're only doing dynamic setup.
	@Bean
	public DynamicServerConfigurationService serverConfigurationService() {
		return new DynamicServerConfigurationService();
	}

	// Service to store/retrieve persisted information for dynamically registered clients.
	@Bean
	public RegisteredClientService registeredClientService() {

		MongoDBRegisteredClientService registeredClientService = new MongoDBRegisteredClientService();
		return registeredClientService;
	}

	// Client Configuration Service. We're using a Hybrid one to allow statically defined clients (i.e. Google)
	//   and dynamically registered clients.
	@Bean
	public HybridClientConfigurationService clientConfigurationService() {
		HybridClientConfigurationService clientConfigService = new HybridClientConfigurationService();

		// set up the static clients. (i.e. Google)
		clientConfigService.setClients(ImmutableMap.of(googleIss, googleClientConfig(), gitlabIss, gitlabClientConfig()));

		// Setup template for dynamic registration
		clientConfigService.setTemplate(getClientTemplate());

		// set the RegisteredClientService for storing/retriving Dynamically created clients
		clientConfigService.setRegisteredClientService(registeredClientService());

		return clientConfigService;
	}

	@Bean
	public LoginUrlAuthenticationEntryPoint authenticationEntryPoint() {
		return new LoginUrlAuthenticationEntryPoint(baseURL + "/openid_connect_login");
	}

	@Bean
	public HybridIssuerService issuerService() {
		HybridIssuerService his = new HybridIssuerService();
		his.setLoginPageUrl(baseURL + "/login.html");
		return his;
	}

	@Bean
	public AuthRequestUrlBuilderWithFixedScopes authRequestUrlBuilder() {
		return new AuthRequestUrlBuilderWithFixedScopes();
	}

//	@Bean
//	public OIDCAuthenticationFilter openIdConnectAuthenticationFilter() throws Exception {
//		OIDCAuthenticationFilter oidcaf = new OIDCAuthenticationFilter();
//		oidcaf.setIssuerService(issuerService());
//		oidcaf.setServerConfigurationService(serverConfigurationService());
//		oidcaf.setClientConfigurationService(clientConfigurationService());
//		oidcaf.setAuthRequestOptionsService(new StaticAuthRequestOptionsService());
//		oidcaf.setAuthRequestUrlBuilder(authRequestUrlBuilder());
//		oidcaf.setAuthenticationManager(authenticationManager());
//		oidcaf.setAuthenticationFailureHandler(new AuthenticationFailureHandler() {
//			@Override
//			public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response, AuthenticationException exception) throws IOException, ServletException {
//				String newUrl = new DefaultUriBuilderFactory()
//					.uriString("/login.html")
//					.queryParam("error", exception.getMessage())
//					.build()
//					.toString();
//
//				response.sendRedirect(newUrl);
//			}
//		});
//
//		return oidcaf;
//	}

//	@Bean
//	public AuthenticationProvider configureOIDCAuthenticationProvider() {
//		OIDCAuthenticationProvider authenticationProvider = new OIDCAuthenticationProvider();
//
//		if (adminIss.equals(googleIss) && !Strings.isNullOrEmpty(adminDomains)) {
//			// Create an OIDCAuthoritiesMapper that uses the 'hd' field of a
//			// Google account's userInfo. hd = Hosted Domain. Use this to filter to
//			// any users of a specific domain
//			authenticationProvider.setAuthoritiesMapper(new GoogleHostedDomainAdminAuthoritiesMapper(adminDomains, adminIss));
//		} else if (!Strings.isNullOrEmpty(adminGroup)) {
//			// use "groups" array from id_token or userinfo for admin access (works with at least gitlab and azure)
//			authenticationProvider.setAuthoritiesMapper(new GroupsAdminAuthoritiesMapper(adminGroup, adminIss));
//		}
//
//		return authenticationProvider;
//	}

//	// This sets Spring Security up so that it can use the OIDC tokens etc.
//	@Override
//	public void configure(AuthenticationManagerBuilder auth) {
//		auth.authenticationProvider(configureOIDCAuthenticationProvider());
//	}

	@Bean
	public SecurityFilterChain filterChainOidc(HttpSecurity http) throws Exception {

		// @formatter:off

		http.csrf(AbstractHttpConfigurer::disable)
				.authorizeHttpRequests(httpRequests -> {
					httpRequests //
						.requestMatchers("/login.html", "/css/**", "/js/**", "/images/**", "/templates/**", "/favicon.ico", "/test-mtls/**", "/test/**", "/jwks**", "/logout.html", "/robots.txt", "/.well-known/**") //
						.permitAll();

					httpRequests.requestMatchers(publicRequestMatcher("/log-detail.html", "/logs.html", "/plan-detail.html", "/plans.html"))
						.permitAll();

					// for other requests we require authentication
					httpRequests.anyRequest().authenticated();
				}) //
//					.addFilterBefore(openIdConnectAuthenticationFilter(), AbstractPreAuthenticatedProcessingFilter.class)
				.exceptionHandling(Customizer.withDefaults())
			.oauth2Client(oauth2Client -> {
				// TODO configure oauth2 client login
			})
//					.authenticationEntryPoint(authenticationEntryPoint())
//				.and()
					.sessionManagement(sessions -> sessions.sessionCreationPolicy(SessionCreationPolicy.ALWAYS))
					.logout(logout -> logout.logoutSuccessUrl("/login.html"))
					//added to disable x-frame-options only for certain paths
					.headers(headers -> {
						headers.frameOptions(HeadersConfigurer.FrameOptionsConfig::disable);
						headers.addHeaderWriter(getXFrameOptionsHeaderWriter());
						}
					)
					.cors(cors -> cors.configurationSource(getCorsConfigurationSource()));

		// @formatter:on

		if (devmode) {
			logger.warn("\n***\n*** Starting application in Dev Mode, injecting dummy user into requests.\n***\n");
			// TODO FIXME add filter
//			http.addFilterBefore(dummyUserFilter, OIDCAuthenticationFilter.class);
		}

		return http.build();
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
		configuration.setAllowedOrigins(List.of("*"));
		configuration.setAllowedMethods(Arrays.asList("GET","POST"));

		AdditiveUrlBasedCorsConfigurationSource source = new AdditiveUrlBasedCorsConfigurationSource();
		source.registerCorsConfiguration("/**/check_session_iframe", configuration);
		source.registerCorsConfiguration("/**/get_session_state", configuration);

		if (additionalCorsConfiguration != null) {
			additionalCorsConfiguration.getCorsConfigurations().forEach(source::registerCorsConfiguration);
		}

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
