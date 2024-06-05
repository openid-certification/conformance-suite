package net.openid.conformance.security;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import jakarta.servlet.Filter;
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
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.DefaultOAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestCustomizers;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestRedirectFilter;
import org.springframework.security.oauth2.client.web.OAuth2LoginAuthenticationFilter;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.OidcUserInfo;
import org.springframework.security.oauth2.core.oidc.user.OidcUserAuthority;
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
import org.springframework.web.util.DefaultUriBuilderFactory;

import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

@Configuration
@Order(2)
@SuppressWarnings({"deprecation"})
class WebSecurityOidcLoginConfig
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
	@Value("${spring.security.oauth2.client.registration.google.client-id}")
	private String googleClientId;

	@Value("${spring.security.oauth2.client.registration.google.client-secret}")
	private String googleClientSecret;

	@Value("${oidc.google.iss:https://accounts.google.com}")
	private String googleIss;

	// Static Client for gitlab
	@Value("${spring.security.oauth2.client.registration.gitlab.client-id}")
	private String gitlabClientId;

	@Value("${spring.security.oauth2.client.registration.gitlab.client-secret")
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
		return new LoginUrlAuthenticationEntryPoint(baseURL + "/login.html");
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

	@Bean
	public SecurityFilterChain filterChainOidc(HttpSecurity http, ClientRegistrationRepository clientRegistrationRepository) throws Exception {

		http.csrf(AbstractHttpConfigurer::disable);
		http.authorizeHttpRequests(httpRequests -> {
			httpRequests //
				.requestMatchers( //
					"/login.html",  //
					"/css/**",  //
					"/js/**",  //
					"/images/**", //
					"/templates/**", //
					"/favicon.ico",  //
					"/test-mtls/**",  //
					"/test/**",  //
					"/jwks**",  //
					"/logout.html", //
					"/robots.txt",  //
					"/.well-known/**" //
				) //
				.permitAll();

			httpRequests.requestMatchers( //
					publicRequestMatcher( //
						"/log-detail.html", //
						"/logs.html",  //
						"/plan-detail.html", //
						"/plans.html" //
					)) //
				.permitAll();

			// for other requests we require authentication
			httpRequests.anyRequest() //
				.authenticated();
		}); //

		// we use oauth2 client login support instead of openIdConnectAuthenticationFilter
		http.oauth2Client(oauth2Client -> {

			// the following is to enable PKCE support for auth-code flow
			var oauth2AuthRequestResolver = new DefaultOAuth2AuthorizationRequestResolver( //
				clientRegistrationRepository, //
				OAuth2AuthorizationRequestRedirectFilter.DEFAULT_AUTHORIZATION_REQUEST_BASE_URI //
			);
			Consumer<OAuth2AuthorizationRequest.Builder> authorizationRequestCustomizer = OAuth2AuthorizationRequestCustomizers.withPkce()
				.andThen(authzUrlBuilder -> {
					authzUrlBuilder.attributes(attrs -> {

						// TODO handle custom client parameters here as previously in AuthRequestUrlBuilderWithFixedScopes
						// registration_id -> indicator for custom client registration
						String registrationId = (String)attrs.get("registration_id");
//						ClientRegistration clientRegistration = clientRegistrationRepository.findByRegistrationId(registrationId);

						logger.debug("configure authorizationRequest for {}", registrationId);
					});
				});
			oauth2AuthRequestResolver.setAuthorizationRequestCustomizer(authorizationRequestCustomizer);
			oauth2Client.authorizationCodeGrant(codeGrant -> {
				codeGrant.authorizationRequestResolver(oauth2AuthRequestResolver);
			});
		});

		http.oauth2Login(oauth2Login -> {
			oauth2Login.failureHandler((request, response, exception) -> {
				String newUrl = new DefaultUriBuilderFactory()
					.uriString("/login.html")
					.queryParam("error", exception.getMessage())
					.build()
					.toString();

				response.sendRedirect(newUrl);
			});
			oauth2Login.userInfoEndpoint(userInfoCustomization -> {
				userInfoCustomization.userAuthoritiesMapper(authorities -> {

					authorities.forEach(authority -> {
						if (authority instanceof OidcUserAuthority oidcUserAuthority) {

							OidcIdToken idToken = oidcUserAuthority.getIdToken();
							OidcUserInfo userInfo = oidcUserAuthority.getUserInfo();

							if (adminIss.equals(googleIss) && !Strings.isNullOrEmpty(adminDomains)) {
								// Create an OIDCAuthoritiesMapper that uses the 'hd' field of a
								// Google account's userInfo. hd = Hosted Domain. Use this to filter to
								// any users of a specific domain
								new GoogleHostedDomainAdminAuthoritiesMapper(adminDomains, adminIss).mapAuthorities(idToken, userInfo);
							} else if (!Strings.isNullOrEmpty(adminGroup)) {
								// use "groups" array from id_token or userinfo for admin access (works with at least gitlab and azure)
								new GroupsAdminAuthoritiesMapper(adminGroup, adminIss).mapAuthorities(idToken, userInfo);
							}
						}
					});

					return authorities;
				});
			});
		});

		http.exceptionHandling(exceptions -> {
			exceptions.authenticationEntryPoint(authenticationEntryPoint());
		});

		http.sessionManagement(sessions -> {
			sessions.sessionCreationPolicy(SessionCreationPolicy.ALWAYS);
		});

		http.logout(logout -> {
			logout.logoutSuccessUrl("/login.html");
		});

		//added to disable x-frame-options only for certain paths
		http.headers(headers -> {
			headers.frameOptions(HeadersConfigurer.FrameOptionsConfig::disable);
			headers.addHeaderWriter(getXFrameOptionsHeaderWriter());
		});

		http.cors(cors -> {
			cors.configurationSource(getCorsConfigurationSource());
		});

		if (devmode) {
			logger.warn("\n***\n*** Starting application in Dev Mode, injecting dummy user into requests.\n***\n");
			http.addFilterBefore(dummyUserFilter, OAuth2LoginAuthenticationFilter.class);
		}

		return http.build();
	}

	@Bean
	@Lazy(false)
	@Profile("dev")
	public ApplicationRunner printFilterChainOidc(SecurityFilterChain filterChainOidc) {
		return args -> {
			List<Filter> filters = filterChainOidc.getFilters();
			logger.debug("### OIDC Filter chain");
			for (int i = 0; i < filters.size(); i++) {
				Filter filter = filters.get(i);
				logger.debug("FilterChain entry [{}] {}", i, filter.getClass());
			}
		};
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
		configuration.setAllowedMethods(Arrays.asList("GET", "POST"));

		AdditiveUrlBasedCorsConfigurationSource source = new AdditiveUrlBasedCorsConfigurationSource();
		source.registerCorsConfiguration("/**/check_session_iframe", configuration);
		source.registerCorsConfiguration("/**/get_session_state", configuration);

		if (additionalCorsConfiguration != null) {
			additionalCorsConfiguration.getCorsConfigurations().forEach(source::registerCorsConfiguration);
		}

		return source;
	}

	private RequestMatcher publicRequestMatcher(String... patterns) {

		return new AndRequestMatcher(new OrRequestMatcher(Arrays.stream(patterns).map(AntPathRequestMatcher::new).collect(Collectors.toList())), new PublicRequestMatcher());
	}

}
