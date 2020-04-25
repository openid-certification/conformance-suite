package net.openid.conformance.security;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import org.mitre.oauth2.model.ClientDetailsEntity;
import org.mitre.oauth2.model.RegisteredClient;
import org.mitre.openid.connect.client.OIDCAuthenticationFilter;
import org.mitre.openid.connect.client.OIDCAuthenticationProvider;
import org.mitre.openid.connect.client.service.RegisteredClientService;
import org.mitre.openid.connect.client.service.impl.DynamicServerConfigurationService;
import org.mitre.openid.connect.client.service.impl.HybridClientConfigurationService;
import org.mitre.openid.connect.client.service.impl.HybridIssuerService;
import org.mitre.openid.connect.client.service.impl.PlainAuthRequestUrlBuilder;
import org.mitre.openid.connect.client.service.impl.StaticAuthRequestOptionsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.security.web.authentication.preauth.AbstractPreAuthenticatedProcessingFilter;
import org.springframework.security.web.util.matcher.AndRequestMatcher;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.OrRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
@EnableWebSecurity
public class OIDCConfig extends WebSecurityConfigurerAdapter {

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

	private Set<String> scopes = ImmutableSet.of("openid", "email", "address", "profile", "phone");
	private ClientDetailsEntity.AuthMethod authMethod = ClientDetailsEntity.AuthMethod.SECRET_BASIC;

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
	@Value("${oidc.admin.domains}")
	private String admin_domains;
	@Value("${oidc.admin.issuer}")
	private String admin_iss;

	@Autowired
	private DummyUserFilter dummyUserFilter;

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
		rc.setScope(ImmutableSet.of("openid", "email", "profile"));
		rc.setRedirectUris(ImmutableSet.of(redirectURI));
		return rc;
	}

	// Create a partially filled in RegisteredClient to use as a template when performing Dynamic Registration
	private RegisteredClient getClientTemplate() {
		RegisteredClient clientTemplate = new RegisteredClient();
		clientTemplate.setClientName(clientName);
		clientTemplate.setScope(scopes);
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
	public PlainAuthRequestUrlBuilder authRequestUrlBuilder() {
		return new PlainAuthRequestUrlBuilder();
	}

	@Bean
	public OIDCAuthenticationFilter openIdConnectAuthenticationFilter() throws Exception {
		OIDCAuthenticationFilter oidcaf = new OIDCAuthenticationFilter();
		oidcaf.setIssuerService(issuerService());
		oidcaf.setServerConfigurationService(serverConfigurationService());
		oidcaf.setClientConfigurationService(clientConfigurationService());
		oidcaf.setAuthRequestOptionsService(new StaticAuthRequestOptionsService());
		oidcaf.setAuthRequestUrlBuilder(authRequestUrlBuilder());
		oidcaf.setAuthenticationManager(authenticationManager());
		return oidcaf;
	}

	@Bean
	public AuthenticationProvider configureOIDCAuthenticationProvider() {
		OIDCAuthenticationProvider authenticationProvider = new OIDCAuthenticationProvider();

		// Create an OIDCAuthoritiesMapper that uses the 'hd' field of a
		//       Google account's userInfo. hd = Hosted Domain. Use this to filter to
		//       Any users of a specific domain (fintechlabs.com)
		authenticationProvider.setAuthoritiesMapper(new GoogleHostedDomainAdminAuthoritiesMapper(admin_domains, admin_iss));

		// This default provider will set everyone to have the role "USER". To change this
		// behavior, wire in a custom OIDCAuthoritiesMapper here
		//
		//   authenticationProvider.setAuthoritiesMapper(OIDCAuthoritiesMapper);
		//

		return authenticationProvider;
	}

	// This sets Spring Security up so that it can use the OIDC tokens etc.
	@Autowired
	public void configureGlobal(AuthenticationManagerBuilder auth) {
		auth.authenticationProvider(configureOIDCAuthenticationProvider());
	}

	@Override
	protected void configure(HttpSecurity http) throws Exception {

		// @formatter:off

		http.csrf().disable()
				.authorizeRequests()
					.antMatchers("/login.html", "/css/**", "/js/**", "/images/**", "/templates/**", "/favicon.ico", "/test-mtls/**", "/test/**", "/jwks**", "/logout.html", "/robots.txt")
					.permitAll()
				.and().authorizeRequests()
					.requestMatchers(publicRequestMatcher("/log-detail.html", "/logs.html", "/plan-detail.html", "/plans.html"))
					.permitAll()
				.anyRequest()
					.authenticated()
				.and()
					.addFilterBefore(openIdConnectAuthenticationFilter(), AbstractPreAuthenticatedProcessingFilter.class)
				.exceptionHandling()
					.authenticationEntryPoint(authenticationEntryPoint())
				.and()
					.sessionManagement().sessionCreationPolicy(SessionCreationPolicy.ALWAYS)
				.and()
					.logout()
					.logoutSuccessUrl("/login.html")
				.and()
					//added to allow check_session_endpoint and such.
					//TODO disable it only for required paths?
					.headers().frameOptions().disable()
				.and()
					.cors().configurationSource(getCorsConfigurationSource());

		// @formatter:off


		if (devmode) {
			logger.warn("\n***\n*** Starting application in Dev Mode, injecting dummy user into requests.\n***\n");
			http.addFilterBefore(dummyUserFilter, OIDCAuthenticationFilter.class);
		}
	}

	protected CorsConfigurationSource getCorsConfigurationSource() {
		CorsConfiguration configuration = new CorsConfiguration();
		configuration.setAllowedOrigins(Arrays.asList("*"));
		configuration.setAllowedMethods(Arrays.asList("GET","POST"));
		UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
		source.registerCorsConfiguration("/**", configuration);
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
