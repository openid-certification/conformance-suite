package net.openid.conformance.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.security.web.util.matcher.OrRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;

import java.util.Arrays;

@Configuration
@EnableWebSecurity
class WebSecurityConfig {

	/**
	 * Order of the static-resources filter chain. Must be lower (higher
	 * precedence) than the API chain (WebSecurityResourceServerConfig,
	 * {@code @Order(1)}) and especially the OIDC login chain
	 * (WebSecurityOidcLoginConfig, {@code @Order(2)}): the static patterns also
	 * satisfy the OIDC chain's "everything non-/api/**" securityMatcher, so this
	 * chain must be consulted first or those requests would fall into the OIDC
	 * chain and get its default no-store headers back. Declared on the @Bean
	 * method (method-level @Order is what Spring uses to order SecurityFilterChain
	 * beans for injection) rather than on the class.
	 */
	static final int STATIC_RESOURCES_CHAIN_ORDER = 0;

	/**
	 * Static asset paths served with a permissive, cache-friendly filter chain
	 * ({@link #filterChainStaticResources}).
	 *
	 * Exposed as a constant (rather than inlined into the bean) so
	 * StaticAssetExemption_UnitTest can assert the configured list against
	 * concrete asset paths — e.g. that /lib/*.js is matched while /api/** is not.
	 */
	static final String[] STATIC_RESOURCE_PATTERNS = {
		"/css/**",
		"/js/**",
		"/vendor/**",
		"/components/**",
		"/lib/**",
		"/images/**",
		"/fonts/**",
		"/templates/**",
		"/favicon.ico"
	};

	/**
	 * Dedicated, highest-priority filter chain for static assets.
	 *
	 * These paths used to be removed from Spring Security entirely via
	 * {@code WebSecurityCustomizer.ignoring()}. Spring Security warns against
	 * that ("You are asking Spring Security to ignore … This is not recommended
	 * -- please use permitAll via HttpSecurity#authorizeHttpRequests instead"),
	 * so they now live in their own {@code permitAll()} chain.
	 *
	 * The reason {@code ignoring()} was originally chosen — keeping the default
	 * {@code HeadersConfigurer} from stamping
	 * "Cache-Control: no-cache, no-store, max-age=0, must-revalidate",
	 * "Pragma: no-cache", "Expires: 0" on every static asset — is preserved here
	 * by disabling the cache-control header writer for this chain. Caching is
	 * owned by the resource handlers in {@link net.openid.conformance.ApplicationConfig};
	 * assets without an explicit Cache-Control header (e.g. /vendor/**,
	 * /images/**, /templates/**, /favicon.ico) keep Last-Modified / ETag
	 * conditional caching instead of being forced to revalidate and never store.
	 *
	 * Unlike {@code ignoring()}, the assets now pass through a (minimal) filter
	 * chain, so they do receive the other default security response headers
	 * (e.g. X-Content-Type-Options: nosniff) — a small, deliberate improvement.
	 *
	 * Ordered ahead of the API chain ({@code @Order(1)}) and the OIDC login
	 * chain ({@code @Order(2)}); the API chain matches only /api/** and the OIDC
	 * chain matches everything non-/api/**, so without an earlier chain these
	 * static requests would fall into the OIDC chain. The OIDC chain still lists
	 * these paths under {@code permitAll()} as a redundant defence-in-depth
	 * fallback.
	 *
	 * Paths that stay inside the other chains:
	 *  - /api/** — needs authentication + CSRF on mutating routes.
	 *  - /login.html, /logout.html — pages users see; should revalidate.
	 *  - /jwks**, /test/**, /.well-known/** — dynamic endpoints.
	 *  - /json-schemas/** — served via a custom ResourceHandler; left in the
	 *    chain for now because it's less obviously static-asset shaped.
	 */
	@Bean
	@Order(STATIC_RESOURCES_CHAIN_ORDER)
	public SecurityFilterChain filterChainStaticResources(HttpSecurity http) throws Exception {
		http.securityMatcher(staticResourceMatcher());

		http.authorizeHttpRequests(requests -> requests.anyRequest().permitAll());

		// Preserve the cache behaviour the previous ignoring() gave these paths:
		// do not let Spring Security force "no-cache, no-store, …" onto static
		// assets (see the class-level javadoc).
		http.headers(headers -> headers.cacheControl(HeadersConfigurer.CacheControlConfig::disable));

		// Static GETs need neither CSRF tokens, saved-request replay, nor a
		// session, so keep the chain lean.
		http.csrf(AbstractHttpConfigurer::disable);
		http.requestCache(AbstractHttpConfigurer::disable);
		http.sessionManagement(sessions -> sessions.sessionCreationPolicy(SessionCreationPolicy.STATELESS));

		return http.build();
	}

	/**
	 * Match {@link #STATIC_RESOURCE_PATTERNS} via {@link PathPatternRequestMatcher}
	 * rather than the {@code securityMatcher(String...)} overload, which builds
	 * MVC matchers requiring a shared {@code HandlerMappingIntrospector} bean —
	 * the same convention the API chain uses (WebSecurityResourceServerConfig).
	 */
	private static RequestMatcher staticResourceMatcher() {
		return new OrRequestMatcher(Arrays.stream(STATIC_RESOURCE_PATTERNS)
			.<RequestMatcher>map(pattern -> PathPatternRequestMatcher.withDefaults().matcher(pattern))
			.toList());
	}
}
