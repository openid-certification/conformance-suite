package io.fintechlabs.testframework.security;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import org.mitre.oauth2.model.ClientDetailsEntity;
import org.mitre.oauth2.model.RegisteredClient;
import org.mitre.openid.connect.client.OIDCAuthenticationFilter;
import org.mitre.openid.connect.client.OIDCAuthenticationProvider;
import org.mitre.openid.connect.client.service.RegisteredClientService;
import org.mitre.openid.connect.client.service.impl.*;
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
import org.springframework.security.web.authentication.AbstractAuthenticationProcessingFilter;
import org.springframework.security.web.authentication.Http403ForbiddenEntryPoint;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.security.web.authentication.preauth.AbstractPreAuthenticatedProcessingFilter;

import java.util.Set;

@Configuration
@EnableWebSecurity
public class OIDCConfig extends WebSecurityConfigurerAdapter {

	private static Logger logger = LoggerFactory.getLogger(DummyUserFilter.class);

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

	private Set<String> scopes = ImmutableSet.of("openid","email","address","profile","phone");
	private ClientDetailsEntity.AuthMethod authMethod = ClientDetailsEntity.AuthMethod.SECRET_BASIC;

	// Specifcs for setting up a Static Client for Google
	@Value("${oidc.google.clientid}")
	private String googleClientId;

	@Value("${oidc.google.secret}")
	private String googleClientSecret;

	@Value("${oidc.google.iss:https://accounts.google.com}")
	private String googleIss;

	// Config for the admin role
	@Value("${oidc.admin.domain}")
	private String admin_domain;
	@Value("${oidc.admin.issuer}")
	private String admin_iss;

	private RegisteredClient googleClientConfig(){
		RegisteredClient rc = new RegisteredClient();
		rc.setClientId(googleClientId);
		rc.setClientSecret(googleClientSecret);
		rc.setScope(ImmutableSet.of("openid","email","profile"));
		rc.setRedirectUris(ImmutableSet.of(redirectURI));
		return rc;
	}

	// Create a partially filled in RegisteredClient to use as a template when performing Dynamic Registration
	private RegisteredClient getClientTemplate(){
		RegisteredClient clientTemplate = new RegisteredClient();
		clientTemplate.setClientName(clientName);
		clientTemplate.setScope(scopes);
		clientTemplate.setTokenEndpointAuthMethod(authMethod);
		clientTemplate.setRedirectUris(ImmutableSet.of(redirectURI));
		return clientTemplate;
	}

	// Bean to set up the server configuration service. We're only doing dynamic setup.
	@Bean
	public DynamicServerConfigurationService serverConfigurationService(){
		return new DynamicServerConfigurationService();
	}

	// Service to store/retrieve persisted information for dynamically registered clients.
	@Bean
	public RegisteredClientService registeredClientService(){

		MongoDBRegisteredClientService registeredClientService = new MongoDBRegisteredClientService();
		return registeredClientService;
	}

	// Client Configuration Service. We're using a Hybrid one to allow statically defined clients (i.e. Google)
	//   and dynamically registered clients.
	@Bean
	public HybridClientConfigurationService clientConfigurationService(){
		HybridClientConfigurationService clientConfigService = new HybridClientConfigurationService();

		// set up the static clients. (i.e. Google)
		clientConfigService.setClients(ImmutableMap.of(googleIss, googleClientConfig()));

		// Setup template for dynamic registration
		clientConfigService.setTemplate(getClientTemplate());

		// set the RegisteredClientService for storing/retriving Dynamically created clients
		clientConfigService.setRegisteredClientService(registeredClientService());

		return clientConfigService;
	}

	@Bean
	public LoginUrlAuthenticationEntryPoint authenticationEntryPoint(){
		return new LoginUrlAuthenticationEntryPoint(baseURL +"/openid_connect_login");
	}

	@Bean
	public HybridIssuerService issuerService(){
		HybridIssuerService his = new HybridIssuerService();
		his.setLoginPageUrl(baseURL + "/login");
		return his;
	}

	@Bean
	public PlainAuthRequestUrlBuilder authRequestUrlBuilder(){
		return new PlainAuthRequestUrlBuilder();
	}

	@Bean
	public OIDCAuthenticationFilter openIdConnectAuthenticationFilter() throws Exception{
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
	public DummyUserFilter dummyUserFilter() {
		return new DummyUserFilter();
	}

	@Bean
	public AuthenticationProvider configureOIDCAuthenticationProvider(){
		OIDCAuthenticationProvider authenticationProvider = new OIDCAuthenticationProvider();

		// Create an OIDCAuthoritiesMapper that uses the 'hd' field of a
		//       Google account's userInfo. hd = Hosted Domain. Use this to filter to
		//       Any users of a specific domain (fintechlabs.com)
		authenticationProvider.setAuthoritiesMapper(new GoogleHostedDomainAdminAuthoritiesMapper(admin_domain, admin_iss));

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
		http.csrf().disable()
				.authorizeRequests()
					.antMatchers("/login")
					.permitAll()
				.anyRequest()
					.authenticated()
				.and()
				.addFilterBefore(openIdConnectAuthenticationFilter(), AbstractPreAuthenticatedProcessingFilter.class)
				.exceptionHandling()
				.authenticationEntryPoint(authenticationEntryPoint())
				.and()
				.logout()
				.logoutSuccessUrl("/login")
				.permitAll();

		if(devmode) {
			logger.warn("\n***\n* Starting application in Dev Mode, injecting dummy user into requests.\n***\n");
			http.addFilterBefore(dummyUserFilter(), OIDCAuthenticationFilter.class);
		}
	}

}
