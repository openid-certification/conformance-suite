package net.openid.conformance;

import net.openid.conformance.runner.InMemoryTestRunnerSupport;
import net.openid.conformance.runner.TestRunnerSupport;
import net.openid.conformance.security.KeyManager;
import org.springframework.boot.autoconfigure.http.HttpMessageConverters;
import org.springframework.boot.autoconfigure.web.WebProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.data.mongodb.core.convert.MongoCustomConversions;
import org.springframework.http.CacheControl;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.GsonHttpMessageConverter;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.TimeUnit;

@Configuration
public class ApplicationConfig implements WebMvcConfigurer {

	/**
	 * Asset directories that get bounded caching in production:
	 * "max-age=300, stale-while-revalidate=86400". Browsers reuse the copy
	 * for up to 5 minutes, then serve stale while revalidating in the
	 * background for up to a day — removing the per-navigation conditional
	 * 304 round-trips these directories used to cost, at a worst-case
	 * staleness of 5 minutes after a deploy. /vendor/** is deliberately not
	 * here yet: its URLs are not versioned, and vendored library bumps are
	 * the change most likely to need an immediate, coordinated refresh with
	 * the pages that load them.
	 */
	static final String[] SWR_ASSET_PATTERNS = {"/css/**", "/js/**", "/components/**"};

	private final Environment environment;
	private final WebProperties webProperties;

	public ApplicationConfig(Environment environment, WebProperties webProperties) {
		this.environment = environment;
		this.webProperties = webProperties;
	}

	/**
	 * Cache policy for the HTML page shells (/plans.html etc.).
	 *
	 * Production: "no-cache" — browsers must revalidate before reuse (a
	 * deploy is picked up on the next navigation), but unlike the previous
	 * "no-store" (Spring Security's blanket default) the document may enter
	 * the back/forward cache, so history traversals restore instantly. The
	 * shells are static files from a public repository with no user data
	 * rendered into them — personalisation arrives via /api fetches — so
	 * there is nothing in them to keep out of caches. The complementary
	 * logout hardening lives in WebSecurityOidcLoginConfig: a
	 * Clear-Site-Data: "cache" header on logout evicts cached/bfcached
	 * pages so Back cannot restore an authenticated-looking shell.
	 *
	 * Dev: keep "no-store", matching the spring-boot-devtools default the
	 * auto-configured handler uses. The save-and-see loop must never serve
	 * a stale copy, and Last-Modified has one-second granularity — two
	 * saves within the same second could otherwise yield a false 304.
	 */
	static CacheControl pageCacheControl(boolean devProfile) {
		return devProfile ? CacheControl.noStore() : CacheControl.noCache();
	}

	/** See {@link #SWR_ASSET_PATTERNS}; dev keeps no-store for save-and-see. */
	static CacheControl assetCacheControl(boolean devProfile) {
		return devProfile
			? CacheControl.noStore()
			: CacheControl.maxAge(5, TimeUnit.MINUTES).staleWhileRevalidate(1, TimeUnit.DAYS);
	}

	// `/` and the legacy `/index.html` are owned by the auth-aware
	// net.openid.conformance.ui.HomeController (anonymous -> /login.html,
	// authenticated -> /plans.html). They used to be unconditional
	// addViewControllers redirects to /plans.html here, but view-controller
	// mappings cannot read the SecurityContext, so the redirect moved into a
	// @Controller. Annotated controllers take precedence over Spring Boot's
	// static welcome-page mapping, and static/index.html no longer exists.

	@Override
	public void addResourceHandlers(ResourceHandlerRegistry registry) {
		registry.addResourceHandler("/json-schemas/**")
			.addResourceLocations("classpath:json-schemas/");

		// Self-hosted font assets are content-addressed by filename (e.g.
		// Inter-Variable.woff2). The woff2 itself is already Brotli-compressed,
		// so no further server-side compression is configured. Long-lived
		// immutable caching means a font upgrade requires changing the filename.
		registry.addResourceHandler("/fonts/**")
			.addResourceLocations("classpath:/static/fonts/")
			.setCacheControl(CacheControl.maxAge(365, TimeUnit.DAYS).cachePublic().immutable());

		// Cache policy for pages and the hot asset directories. These custom
		// handlers exist to carry explicit Cache-Control headers; handler-set
		// headers also make Spring Security's CacheControlHeadersWriter skip
		// its blanket "no-cache, no-store, …" default for the in-chain page
		// routes (it only writes when no cache header is present).
		//
		// Locations are derived from spring.web.resources.static-locations so
		// the dev profile's save-and-see source-tree location
		// (file:src/main/resources/static/, application-dev.properties) keeps
		// working: a more specific handler pattern would otherwise shadow the
		// auto-configured "/**" handler and serve stale classpath copies.
		boolean dev = environment.acceptsProfiles(Profiles.of("dev"));
		String[] staticLocations = webProperties.getResources().getStaticLocations();

		registry.addResourceHandler("/*.html")
			.addResourceLocations(staticLocations)
			.setCacheControl(pageCacheControl(dev));

		for (String pattern : SWR_ASSET_PATTERNS) {
			// "/css/**" resolves relative to the css/ directory inside each
			// static location, mirroring how the /fonts/** handler points at
			// .../fonts/.
			String subDir = pattern.substring(1, pattern.length() - "**".length());
			String[] subLocations = Arrays.stream(staticLocations)
				.map(location -> location.endsWith("/") ? location + subDir : location + "/" + subDir)
				.toArray(String[]::new);
			registry.addResourceHandler(pattern)
				.addResourceLocations(subLocations)
				.setCacheControl(assetCacheControl(dev));
		}
	}

	@Bean
	public HttpMessageConverters customConverters() {

		Collection<HttpMessageConverter<?>> messageConverters = new ArrayList<>();

		// wire in the special GSON converter to the HTTP message outputs, will automatically handle all __wrapped_key_element structures added by GsonObjectToBsonDocumentConverter
		GsonHttpMessageConverter gsonHttpMessageConverter = new CollapsingGsonHttpMessageConverter();
		messageConverters.add(gsonHttpMessageConverter);

		return new HttpMessageConverters(true, messageConverters);
	}

	@Bean
	public TestRunnerSupport testRunnerSupport() {
		return new InMemoryTestRunnerSupport();
	}

	@Bean
	public MongoCustomConversions mongoCustomConversions() {
		return MongoConversionSupport.createMongoCustomConversions();
	}

	@Bean
	public KeyManager keyManager() {
		return new KeyManager();
	}
}
