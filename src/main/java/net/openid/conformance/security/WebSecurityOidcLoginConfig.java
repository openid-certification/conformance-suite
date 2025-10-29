package net.openid.conformance.security;

import com.google.common.base.Strings;
import jakarta.servlet.Filter;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import net.openid.conformance.sharing.AssetSharing;
import net.openid.conformance.sharing.SharedAsset;
import net.openid.conformance.sharing.magiclink.CustomOneTimeTokenService;
import net.openid.conformance.sharing.magiclink.MagicLinkOneTimeToken;
import net.openid.conformance.sharing.magiclink.MagicLinkOneTimeTokenAuthenticationProvider;
import net.openid.conformance.sharing.magiclink.MagicLinkUserDetailsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.security.oauth2.client.OAuth2ClientProperties;
import org.springframework.boot.autoconfigure.security.oauth2.client.OAuth2ClientPropertiesMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.ott.OneTimeTokenAuthenticationToken;
import org.springframework.security.authentication.ott.OneTimeTokenService;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.DefaultOAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestCustomizers;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestRedirectFilter;
import org.springframework.security.oauth2.client.web.OAuth2LoginAuthenticationFilter;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.OidcUserInfo;
import org.springframework.security.oauth2.core.oidc.user.OidcUserAuthority;
import org.springframework.security.web.DefaultRedirectStrategy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.security.web.authentication.ott.RedirectOneTimeTokenGenerationSuccessHandler;
import org.springframework.security.web.header.HeaderWriter;
import org.springframework.security.web.header.writers.DelegatingRequestMatcherHeaderWriter;
import org.springframework.security.web.header.writers.frameoptions.XFrameOptionsHeaderWriter;
import org.springframework.security.web.util.matcher.AndRequestMatcher;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.NegatedRequestMatcher;
import org.springframework.security.web.util.matcher.OrRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.util.CollectionUtils;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.util.DefaultUriBuilderFactory;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

@Configuration
@Order(2)
class WebSecurityOidcLoginConfig {

	private static final Logger logger = LoggerFactory.getLogger(DummyUserFilter.class);

	@Value("${fintechlabs.devmode:false}")
	private boolean devmode;

	@Value("${fintechlabs.base_url}")
	private String baseURL;

	@Value("${oidc.google.iss:https://accounts.google.com}")
	private String googleIss;

	// Config for the admin role
	@Value("${oidc.admin.domains:}")
	private String adminDomains;
	@Value("${oidc.admin.group:}")
	private String adminGroup;
	@Value("${oidc.admin.issuer}")
	private String adminIss;

	// Allows to deduce ROLE_ADMIN by gitlab project role
	@Value("#{${oidc.gitlab.admin-group-indicator-claims}}")
	private Map<String, Set<String>> gitlabAdminGroupIndicatorClaims;

	@Autowired
	private DummyUserFilter dummyUserFilter;

	@Autowired(required = false)
	private CorsConfigurable additionalCorsConfiguration;
	@Autowired
	private AuthenticationFacade authenticationFacade;

	@Bean
	public InMemoryClientRegistrationRepository clientRegistrationRepository(OAuth2ClientProperties properties) {
		try {
			List<ClientRegistration> registrations = new ArrayList<>(
				new OAuth2ClientPropertiesMapper(properties).asClientRegistrations().values());
			return new InMemoryClientRegistrationRepository(registrations);
		} catch (Exception e) {
			if (devmode) {
				logger.warn("Failed to load client registrations. Error: {}", e.getMessage());
				return new InMemoryClientRegistrationRepository(Map.of());
			} else {
				throw new RuntimeException(e);
			}
		}
	}

	@Bean
	public LoginUrlAuthenticationEntryPoint authenticationEntryPoint() {
		return new LoginUrlAuthenticationEntryPoint(baseURL + "/login.html");
	}

	@Bean
	public SecurityFilterChain filterChainOidc(HttpSecurity http, ClientRegistrationRepository clientRegistrationRepository, OneTimeTokenService oneTimeTokenService, MagicLinkUserDetailsService magicLinkUserDetailsService) throws Exception {

		http.securityMatcher(request -> {
			// only handle NON-API requests with this filter chain
			return !request.getRequestURI().startsWith("/api/");
		});

		http.csrf(AbstractHttpConfigurer::disable);

		// enforce https
		http.requiresChannel(channelRequest -> {
			channelRequest.anyRequest().requiresSecure().channelProcessors(List.of(new RejectPlainHttpTrafficChannelProcessor()));
		});

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
					"/.well-known/**", //
					"/login/ott" // magic link login
				) //
				.permitAll();

			// Magic Link access restriction - only allow access to resources explicitly shared via the Magic Link
			httpRequests.requestMatchers(new RequestMatcher() {

				@SuppressWarnings("unused")
				@Override
				public boolean matches(HttpServletRequest request) {

					// if we are authenticated via a magic link
					// only allow access to the shared resource
					if (!authenticationFacade.isMagicLinkUser()) {
						return false;
					}

					var requestURI = URI.create(baseURL + request.getRequestURI() + "?" + request.getQueryString());
					MagicLinkOneTimeToken magicOneTimeToken = authenticationFacade.getMagicOneTimeToken();
					SharedAsset sharedAsset = magicOneTimeToken.getSharedAsset();
					var redirectUri = URI.create(sharedAsset.getRedirectUri());

					if (requestURI.equals(redirectUri)) {
						// allow access
						return false;
					}

					return true;
				}
			}).denyAll();

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

		http.oneTimeTokenLogin(ott -> {
			ott.authenticationProvider(new MagicLinkOneTimeTokenAuthenticationProvider(oneTimeTokenService, magicLinkUserDetailsService));
			ott.tokenGenerationSuccessHandler(new RedirectOneTimeTokenGenerationSuccessHandler("/index.html"));
			ott.authenticationSuccessHandler(new AuthenticationSuccessHandler() {
				@Override
				public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {
					OneTimeTokenAuthenticationToken token = (OneTimeTokenAuthenticationToken) authentication;
					MagicLinkOneTimeToken magicLink = (MagicLinkOneTimeToken)token.getDetails();
					new DefaultRedirectStrategy().sendRedirect(request, response, magicLink.getSharedAsset().getRedirectUri());
				}
			});
		});

		// we use oauth2 client login support instead of openIdConnectAuthenticationFilter
		http.oauth2Client(oauth2Client -> {

			// the following is to enable PKCE support for auth-code flow
			var oauth2AuthRequestResolver = new DefaultOAuth2AuthorizationRequestResolver( //
				clientRegistrationRepository, //
				OAuth2AuthorizationRequestRedirectFilter.DEFAULT_AUTHORIZATION_REQUEST_BASE_URI //
			);
			Consumer<OAuth2AuthorizationRequest.Builder> authorizationRequestCustomizer = OAuth2AuthorizationRequestCustomizers.withPkce();
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

					Set<GrantedAuthority> extendedAuthorities = new HashSet<>(authorities);

					authorities.forEach(authority -> {
						if (authority instanceof OidcUserAuthority oidcUserAuthority) {

							OidcIdToken idToken = oidcUserAuthority.getIdToken();
							OidcUserInfo userInfo = oidcUserAuthority.getUserInfo();

							if (adminIss.equals(googleIss) && !Strings.isNullOrEmpty(adminDomains)) {
								// Create an OIDCAuthoritiesMapper that uses the 'hd' field of a
								// Google account's userInfo. hd = Hosted Domain. Use this to filter to
								// any users of a specific domain
								var authoritiesByGoogleClaim = new GoogleHostedDomainAdminAuthoritiesMapper(adminDomains, adminIss).mapAuthorities(idToken, userInfo);
								if (!CollectionUtils.isEmpty(authoritiesByGoogleClaim)) {
									extendedAuthorities.addAll(authoritiesByGoogleClaim);
								}
							} else if (!Strings.isNullOrEmpty(adminGroup)) {
								// use "groups" array from id_token or userinfo for admin access (works with at least gitlab and azure)
								var authoritiesByGroupsClaim = new GroupsAdminAuthoritiesMapper(adminGroup, adminIss).mapAuthorities(idToken, userInfo);
								if (!CollectionUtils.isEmpty(authoritiesByGroupsClaim)) {
									extendedAuthorities.addAll(authoritiesByGroupsClaim);
								}
								// use gitlab specific project role claims to determine admin role
								var authoritiesByGitlabProject = new GitlabProjectAdminAuthoritiesMapper(adminIss, gitlabAdminGroupIndicatorClaims).mapAuthorities(idToken, userInfo);
								if (!CollectionUtils.isEmpty(authoritiesByGitlabProject)) {
									extendedAuthorities.addAll(authoritiesByGitlabProject);
								}
							}
						}
					});

					return extendedAuthorities;
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
	public OneTimeTokenService oneTimeTokenService(AssetSharing assetSharing) {
		return new CustomOneTimeTokenService(assetSharing);
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
		configuration.setAllowedMethods(List.of("GET", "POST"));

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
				Arrays.stream(patterns).map(AntPathRequestMatcher::new).collect(Collectors.toList())),
			new PublicRequestMatcher());
	}

}
