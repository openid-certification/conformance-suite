package net.openid.conformance;

import net.openid.conformance.runner.InMemoryTestRunnerSupport;
import net.openid.conformance.runner.TestRunnerSupport;
import net.openid.conformance.security.KeyManager;
import org.springframework.boot.autoconfigure.web.WebProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.core.io.Resource;
import org.springframework.data.mongodb.core.convert.MongoCustomConversions;
import org.springframework.http.CacheControl;
import org.springframework.http.converter.HttpMessageConverters;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.io.IOException;
import java.io.OutputStream;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;

@Configuration
public class ApplicationConfig implements WebMvcConfigurer {

	/**
	 * Asset directories that get bounded caching in production:
	 * "max-age=300, stale-while-revalidate=86400". Browsers reuse the copy
	 * for up to 5 minutes without asking; after that, for up to a day, a
	 * load uses the cached copy and revalidates in the background, so the
	 * first load after a deploy can still run the previous build's file
	 * (possibly alongside a newer page or vendor library) and the next load
	 * gets the new one. This removes the per-navigation conditional
	 * round-trips these directories used to cost. /vendor/** is deliberately not
	 * here: its URLs are not versioned, and vendored library bumps are the
	 * change most likely to need an immediate, coordinated refresh with the
	 * pages that load them, so it stays in {@link #REVALIDATE_ASSET_PATTERNS}.
	 */
	static final String[] SWR_ASSET_PATTERNS = {"/css/**", "/js/**", "/components/**"};

	/**
	 * Asset directories that, like the page shells, revalidate on every
	 * navigation ("no-cache") so a change is picked up immediately. Without
	 * an explicit policy these fell through to the auto-configured handler
	 * with no Cache-Control at all, which browsers cache heuristically from
	 * the (constant) Last-Modified for a very long time.
	 */
	static final String[] REVALIDATE_ASSET_PATTERNS = {"/vendor/**", "/lib/**", "/images/**", "/templates/**"};

	/**
	 * Content ETags keyed by resource identity. The production jar is built
	 * reproducibly, so every entry reports the same fixed Last-Modified on
	 * every build; a browser revalidating against that date would always be
	 * told "not modified" and keep the previous build's file. The page,
	 * hot-asset and revalidate handlers registered below therefore validate
	 * on this content hash instead and do not emit Last-Modified at all
	 * (/fonts/** is immutable by filename; /json-schemas/** and root files
	 * such as /favicon.ico stay on the auto-configured handler).
	 *
	 * The tag is weak (W/"...") because Tomcat does not gzip a response
	 * that carries a strong ETag; If-None-Match uses weak comparison, so
	 * revalidation is unaffected. The key includes the file's own timestamp
	 * and length so the dev profile's save-and-see source-tree location
	 * rehashes an edited file; the packaged jar's entries never change.
	 */
	private static final ConcurrentMap<String, String> CONTENT_ETAGS = new ConcurrentHashMap<>();

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

	/**
	 * Weak ETag of the resource content's SHA-256, or null (no ETag, hence
	 * no conditional 304) if the resource cannot be read.
	 */
	static String contentEtag(Resource resource) {
		try {
			String key = resource.getURL() + "|" + resource.lastModified() + "|" + resource.contentLength();
			return CONTENT_ETAGS.computeIfAbsent(key, k -> hashContent(resource));
		} catch (IOException | IllegalStateException e) {
			return null;
		}
	}

	private static String hashContent(Resource resource) {
		try (DigestInputStream in = new DigestInputStream(resource.getInputStream(), MessageDigest.getInstance("SHA-256"))) {
			in.transferTo(OutputStream.nullOutputStream());
			return "W/\"" + HexFormat.of().formatHex(in.getMessageDigest().digest()) + "\"";
		} catch (IOException e) {
			throw new IllegalStateException("Unable to hash static resource " + resource, e);
		} catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException(e);
		}
	}

	/** Content-ETag validation for a static handler; see {@link #CONTENT_ETAGS}. */
	private static ResourceHandlerRegistration validateByContent(ResourceHandlerRegistration registration) {
		return registration
			.setUseLastModified(false)
			.setEtagGenerator(ApplicationConfig::contentEtag);
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

		validateByContent(registry.addResourceHandler("/*.html")
			.addResourceLocations(staticLocations))
			.setCacheControl(pageCacheControl(dev));

		for (String pattern : SWR_ASSET_PATTERNS) {
			validateByContent(registry.addResourceHandler(pattern)
				.addResourceLocations(subLocations(staticLocations, pattern)))
				.setCacheControl(assetCacheControl(dev));
		}

		for (String pattern : REVALIDATE_ASSET_PATTERNS) {
			validateByContent(registry.addResourceHandler(pattern)
				.addResourceLocations(subLocations(staticLocations, pattern)))
				.setCacheControl(pageCacheControl(dev));
		}
	}

	/**
	 * "/css/**" resolves relative to the css/ directory inside each static
	 * location, mirroring how the /fonts/** handler points at .../fonts/.
	 */
	private static String[] subLocations(String[] staticLocations, String pattern) {
		String subDir = pattern.substring(1, pattern.length() - "**".length());
		return Arrays.stream(staticLocations)
			.map(location -> location.endsWith("/") ? location + subDir : location + "/" + subDir)
			.toArray(String[]::new);
	}

	// The conformance suite serializes all JSON API responses with GSON: its domain objects are GSON/JsonObject
	// based, and CollapsingGsonHttpMessageConverter additionally collapses the __wrapped_key_element structures
	// added by GsonObjectToBsonDocumentConverter and applies the ConfigMigratingResponse migration.
	//
	// Under Spring Boot 4 a custom `HttpMessageConverters` @Bean is no longer wired into Spring MVC. Spring MVC
	// builds its converter set via HttpMessageConverters.forServer().registerDefaults() and then invokes this hook,
	// so withJsonConverter REPLACES the default Jackson JSON converter for application/json. Without this, Jackson
	// serializes GSON wrappers like ConfigMigratingResponse as raw beans (the "inner" wrapper / expanded-variant
	// regression). (extendMessageConverters/configureMessageConverters(List) are deprecated-for-removal in Spring 7.)
	@Override
	public void configureMessageConverters(HttpMessageConverters.ServerBuilder builder) {
		builder.withJsonConverter(new CollapsingGsonHttpMessageConverter());
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
