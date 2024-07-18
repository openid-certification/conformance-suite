package net.openid.conformance.security;

import com.nimbusds.jwt.proc.DefaultJWTProcessor;
import jakarta.servlet.Filter;
import jakarta.ws.rs.HttpMethod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.security.oauth2.resource.servlet.JwkSetUriJwtDecoderBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.util.matcher.AndRequestMatcher;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.OrRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;

import java.util.List;

@Configuration
@Order(1)
public class WebSecurityResourceServerConfig {

	private static final Logger logger = LoggerFactory.getLogger(WebSecurityResourceServerConfig.class);

	@Value("${fintechlabs.devmode:false}")
	private boolean devmode;

	@Autowired
	private DummyUserFilter dummyUserFilter;

	@Bean
	protected SecurityFilterChain filterChainResourceServer(HttpSecurity http, ApiTokenAuthenticationProvider apiTokenAuthenticationProvider) throws Exception {

		http.securityMatcher(request -> {
			// only handle API requests with this filter chain
			return request.getRequestURI().startsWith("/api/");
		});

		http.csrf(AbstractHttpConfigurer::disable);

		// enforce https
		http.requiresChannel(channelRequest -> {
			channelRequest.anyRequest().requiresSecure().channelProcessors(List.of(new RejectPlainHttpTrafficChannelProcessor()));
		});

		http.sessionManagement(sessions -> sessions.sessionCreationPolicy(SessionCreationPolicy.NEVER));

		http.authorizeHttpRequests(requests -> {
			requests.requestMatchers(getPublicMatcher()).permitAll();
			requests.requestMatchers(getApiMatcher()).authenticated();
			// deny access for any unmatched API routes
			requests.anyRequest().denyAll();
		});

		http.oauth2ResourceServer(oauthResourceServer -> {
			oauthResourceServer.opaqueToken(opaqueTokenConfigurer -> {
				opaqueTokenConfigurer.authenticationManager(new ProviderManager(List.of(apiTokenAuthenticationProvider)));
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
		return new OrRequestMatcher(new AntPathRequestMatcher("/api/server"), new AntPathRequestMatcher("/api/currentuser"), new AntPathRequestMatcher("/api/runner/**"), new AntPathRequestMatcher("/api/log/**"), new AntPathRequestMatcher("/api/info/**"), new AntPathRequestMatcher("/api/plan/**"), new AntPathRequestMatcher("/api/token/**"), new AntPathRequestMatcher("/api/lastconfig"));
	}

	private RequestMatcher getPublicMatcher() {
		// Matches following paths IIF the ?public query parameter is present
		return new AndRequestMatcher(new OrRequestMatcher(new AntPathRequestMatcher("/api/info/?*", HttpMethod.GET), new AntPathRequestMatcher("/api/log", HttpMethod.GET), new AntPathRequestMatcher("/api/log/?*", HttpMethod.GET), new AntPathRequestMatcher("/api/log/export/?*", HttpMethod.GET), new AntPathRequestMatcher("/api/plan", HttpMethod.GET), new AntPathRequestMatcher("/api/plan/?*", HttpMethod.GET), new AntPathRequestMatcher("/api/plan/export/?*", HttpMethod.GET)), new PublicRequestMatcher());
	}

	@Bean
	public RestAuthenticationEntryPoint restAuthenticationEntryPoint() {
		return new RestAuthenticationEntryPoint();
	}

}
