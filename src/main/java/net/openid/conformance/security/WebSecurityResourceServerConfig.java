package net.openid.conformance.security;

import com.nimbusds.jwt.proc.DefaultJWTProcessor;
import jakarta.servlet.Filter;
import net.openid.conformance.security.idp.RolesAuthoritiesConverter;
import net.openid.conformance.sharing.privatelink.ShareJwtBearerAuthenticationProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.security.oauth2.server.resource.autoconfigure.servlet.JwkSetUriJwtDecoderBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.SupplierJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationProvider;
import org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.context.request.async.WebAsyncManagerIntegrationFilter;
import org.springframework.security.web.savedrequest.NullRequestCache;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.security.web.util.matcher.AndRequestMatcher;
import org.springframework.security.web.util.matcher.OrRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;

import java.util.List;
import java.util.stream.Stream;

@Configuration
@Order(1)
public class WebSecurityResourceServerConfig {

	private static final Logger logger = LoggerFactory.getLogger(WebSecurityResourceServerConfig.class);

	@Value("${fintechlabs.devmode:false}")
	private boolean devmode;

	@Autowired
	private AuthenticationFacade authenticationFacade;


	@Value("${spring.security.oauth2.client.provider.idp.issuerUri}")
	private String issuerUri;

	// The algorithm(s) the IdP signs access tokens with, comma separated.
	@Value("${oidc.idp.access-token-signing-algs:}")
	private String accessTokenSigningAlgs;

	// Same registration the browser login uses: a bearer token is only ours if the
	// IdP minted it for this client. See IdpAudienceValidator.
	@Value("${spring.security.oauth2.client.registration.idp.client-id}")
	private String clientId;

	@Autowired
	private DummyUserFilter dummyUserFilter;

	/**
	 * Resolves the IdP's OIDC discovery document (a network call), so it is a
	 * separate bean rather than inline in the filter chain: tests that exercise
	 * the chain can supply a stub instead of reaching the IdP.
	 */
	@Bean
	public JwtAuthenticationProvider idpJwtAuthenticationProvider(RolesAuthoritiesConverter authoritiesConverter) {
		// Access tokens are not necessarily signed with the same algorithm as ID
		// tokens - Keycloak defaults to RS256 for access tokens even when ID tokens
		// are ES256 - so this is configured separately, and accepts a set. Every
		// accepted algorithm is asymmetric and key-resolved from the IdP's JWKS by
		// kid, so accepting more than one does not enable algorithm confusion.
		var jwsAlgorithms = IdpJwsAlgorithms.parse(accessTokenSigningAlgs);

		// Lazily built: withIssuerLocation(..).build() fetches the IdP's discovery
		// document, and doing that during bean creation would make startup fail
		// whenever the IdP is unreachable - stopping the suite from starting at all,
		// including for the many tests that never authenticate. SupplierJwtDecoder
		// defers it to the first token and does not cache a failed attempt, so an
		// outage heals without a restart. IdpJwtDecoder then turns a resolution
		// failure into a JwtException; without that, an IdP outage makes every
		// unrecognised bearer token escape the filter chain as a server error
		// instead of returning 401.
		JwtDecoder jwtDecoder = new IdpJwtDecoder(new SupplierJwtDecoder(() -> {
			NimbusJwtDecoder decoder = NimbusJwtDecoder.withIssuerLocation(issuerUri)
				.jwsAlgorithms(algorithms -> algorithms.addAll(jwsAlgorithms))
				.build();
			// createDefaultWithIssuer checks iss, exp and nbf only. On its own that
			// accepts any token the IdP ever issued, to any of its clients, as a
			// suite user - so the audience check is not optional here.
			//
			// There is deliberately no check that the token type header is
			// "at+jwt" (RFC 9068 section 4), which would be the clean way to stop
			// an ID token being replayed here: an ID token from this client has
			// the same issuer, the same signing keys, and an aud equal to the
			// client id, so it satisfies both validators below, and its roles
			// claim would grant ROLE_ADMIN. The IdP does not set that header on
			// its access tokens today, so requiring it would reject every real
			// bearer token.
			//
			// What makes this acceptable meanwhile is that the client is
			// confidential and the ID token never reaches the browser: it is
			// exchanged server-side at the token endpoint and kept in the server
			// session, so a caller who could present one already has more access
			// than the bearer token would give them. Add the header check as soon
			// as the IdP emits it.
			OAuth2TokenValidator<Jwt> jwtValidator = new DelegatingOAuth2TokenValidator<>(
				JwtValidators.createDefaultWithIssuer(issuerUri),
				new IdpAudienceValidator(clientId));
			decoder.setJwtValidator(jwtValidator);
			return decoder;
		}));
		var jwtAuthProvider = new JwtAuthenticationProvider(jwtDecoder);
		var jwtAuthConverter = new JwtAuthenticationConverter();
		// Must be the injected bean: a hand-constructed converter never gets its
		// @Value admin-role name populated, so ROLE_ADMIN would never be granted.
		jwtAuthConverter.setJwtGrantedAuthoritiesConverter(authoritiesConverter);
		jwtAuthProvider.setJwtAuthenticationConverter(jwtAuthConverter);
		return jwtAuthProvider;
	}

	@Bean
	protected SecurityFilterChain filterChainResourceServer(HttpSecurity http,
															ApiTokenAuthenticationProvider apiTokenAuthenticationProvider,
															ShareJwtBearerAuthenticationProvider shareJwtBearerAuthenticationProvider,
															JwtAuthenticationProvider jwtAuthProvider) throws Exception {

		http.securityMatcher(request -> {
			// only handle API requests with this filter chain
			return request.getRequestURI().startsWith("/api/");
		});

		http.csrf(AbstractHttpConfigurer::disable);

		// enforce https
		http.addFilterAfter(new RejectPlainHttpTrafficFilter(), WebAsyncManagerIntegrationFilter.class);

		http.sessionManagement(sessions -> sessions.sessionCreationPolicy(SessionCreationPolicy.NEVER));

		// Never save anonymous API requests for post-login replay. Without this,
		// an anonymous fetch() from a public page (e.g. the footer's /api/server
		// version probe on login.html) 401s here, but ExceptionTranslationFilter
		// first writes SPRING_SECURITY_SAVED_REQUEST into the HttpSession shared
		// with the OIDC login chain — whose success handler then "returns" the
		// user to that API URL after OAuth login, landing them on raw JSON at
		// /api/server?continue instead of the plans home. An API URL is never a
		// sensible browser navigation target, so this chain opts out of the
		// request cache entirely. Note SessionCreationPolicy.NEVER above does NOT
		// make this line redundant: the default HttpSessionRequestCache has
		// createSessionAllowed=true and Spring auto-installs a NullRequestCache
		// only for STATELESS. Guarded by ResourceServerRequestCache_UnitTest.
		http.requestCache(cache -> cache.requestCache(new NullRequestCache()));

		http.authorizeHttpRequests(requests -> {
			requests.requestMatchers(request -> {
				if (!authenticationFacade.isPrivateLinkUser()) {
					return false; // not a private link user, don't apply this rule
				}

				// Allow only the specific API endpoints needed for viewing shared results
				String uri = request.getRequestURI();
				String method = request.getMethod();
				if ("GET".equals(method) && (
					uri.matches("/api/plan/[A-Za-z0-9]+") ||
					uri.matches("/api/info/[A-Za-z0-9]+") ||
					uri.matches("/api/log/[A-Za-z0-9]+") ||
					uri.equals("/api/currentuser"))) {
					return false; // allow these
				}
				return true; // deny everything else
			}).denyAll();

			requests.requestMatchers(getPublicMatcher()).permitAll();
			requests.requestMatchers(getApiMatcher()).hasAnyAuthority("ROLE_USER", "ROLE_ADMIN");
			// deny access for any unmatched API routes
			requests.anyRequest().denyAll();
		});
		http.oauth2ResourceServer(oauthResourceServer -> {

			oauthResourceServer.opaqueToken(opaqueTokenConfigurer -> {
				// Order matters for efficiency only: share JWT parse failure is a cheap local
				// check; the opaque-token path may hit the DB via TokenService.findToken.
				// Each provider returns null for tokens it does not recognise, so
				// ProviderManager falls through to the next.
				opaqueTokenConfigurer.authenticationManager(new ProviderManager(List.of(
					shareJwtBearerAuthenticationProvider,
					apiTokenAuthenticationProvider,
					jwtAuthProvider
					)));
			});
		});

		http.exceptionHandling(exceptions -> {
			exceptions.authenticationEntryPoint(restAuthenticationEntryPoint());
		});

		if (devmode) {
			http.addFilterBefore(dummyUserFilter, BearerTokenAuthenticationFilter.class);
		}

		return http.build();
	}

	/**
	 * Placeholder that keeps Spring Boot's resource-server auto-configuration from
	 * contributing a JwtDecoder of its own (every one of its variants is
	 * {@code @ConditionalOnMissingBean(JwtDecoder.class)}).
	 * <p>
	 * This chain does not use {@code oauth2ResourceServer().jwt(..)} - it wires its
	 * own providers through {@code opaqueToken(..)} - so no auto-configured decoder
	 * would ever be consulted, and one built from half-set properties would only be
	 * a confusing second source of truth. Deliberately keyless: nothing should
	 * decode with it, and if anything ever does, failing loudly beats verifying
	 * against a key source nobody configured.
	 */
	@Bean
	public NimbusJwtDecoder jwtDecoder() {
		return new NimbusJwtDecoder(new DefaultJWTProcessor<>());
	}

	@Bean
	public JwkSetUriJwtDecoderBuilderCustomizer jwtDecoderBuilderCustomizer() {
		return builder -> {
			logger.debug("Customize JWT Decoder here");
		};
	}

	@Bean
	@Lazy(false)
	@Profile("dev")
	public ApplicationRunner printResourceServerFilterChain(SecurityFilterChain filterChainResourceServer) {
		return args -> {
			List<Filter> filters = filterChainResourceServer.getFilters();
			logger.debug("### Resource Server Filter chain");
			for (int i = 0; i < filters.size(); i++) {
				Filter filter = filters.get(i);
				logger.debug("FilterChain entry [{}] {}", i, filter.getClass());
			}
		};
	}

	private RequestMatcher getApiMatcher() {
		return new OrRequestMatcher(Stream.of( //
			"/api/server", //
			"/api/currentuser", //
			"/api/runner/**", //
			"/api/log/**", //
			"/api/info/**", //
			"/api/plan/**", //
			"/api/token/**", //
			"/api/lastconfig", //
			"/api/favorite-plans", //
			"/api/favorite-plans/**" //
			).<RequestMatcher>map(pattern -> PathPatternRequestMatcher.withDefaults().matcher(pattern)).toList());
	}

	/**
	 * GET path patterns that may be accessed anonymously when the ?public query parameter
	 * requests published data (see getPublicMatcher). Also consumed by SwaggerConfig so the
	 * API documentation's security requirements stay in sync with this configuration.
	 */
	public static final List<String> PUBLIC_GET_PATHS = List.of(
		"/api/ui/?*",
		"/api/info/?*",
		"/api/log",
		"/api/log/?*",
		"/api/log/export/?*",
		"/api/plan",
		"/api/plan/?*",
		"/api/plan/export/?*");

	private RequestMatcher getPublicMatcher() {
		// Matches following paths IIF the ?public query parameter is present
		return new AndRequestMatcher( //
			new OrRequestMatcher( //
				PUBLIC_GET_PATHS.stream() //
					.<RequestMatcher>map(path -> PathPatternRequestMatcher.withDefaults().matcher(HttpMethod.GET, path)).toList()), //
			new PublicRequestMatcher());
	}

	@Bean
	public RestAuthenticationEntryPoint restAuthenticationEntryPoint() {
		return new RestAuthenticationEntryPoint();
	}

}
