package net.openid.conformance;

import net.openid.conformance.security.AuthenticationFacade;
import org.apache.coyote.CompressionConfig;
import org.apache.coyote.Request;
import org.apache.coyote.Response;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.boot.autoconfigure.web.WebProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockServletContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import java.io.InputStream;
import java.security.MessageDigest;
import java.util.HexFormat;

/**
 * Drives the real static resource handlers registered by
 * {@link ApplicationConfig#addResourceHandlers} and pins how a browser
 * revalidates a cached page shell or asset after a deploy.
 *
 * <p>The production jar is built reproducibly, so every entry carries the
 * same fixed Last-Modified on every build. Revalidating against that date
 * can never notice a changed file: the server always answers 304 and the
 * browser keeps the previous build's JS/CSS. The handlers must therefore
 * validate on a content-derived ETag and not emit Last-Modified at all.
 * The ETag must be weak: Tomcat does not gzip a response carrying a
 * strong ETag, and server compression is enabled for these types.
 */
public class ApplicationConfigStaticRevalidation_UnitTest {

	private static final String ASSET = "/components/cts-navbar.js";
	private static final String PAGE = "/plans.html";

	/** A date later than any build timestamp: with Last-Modified validation this always yields 304. */
	private static final String FAR_FUTURE = "Fri, 01 Jan 2100 00:00:00 GMT";

	private AnnotationConfigWebApplicationContext context;
	private MockMvc mockMvc;

	@Configuration
	@EnableWebMvc
	@Import(ApplicationConfig.class)
	static class StaticHandlersConfig {
		@Bean
		WebProperties webProperties() {
			return new WebProperties();
		}

		/** Only needed because ApplicationConfig also declares the test-runner bean, which autowires it. */
		@Bean
		AuthenticationFacade authenticationFacade() {
			return Mockito.mock(AuthenticationFacade.class);
		}
	}

	@BeforeEach
	public void setUp() {
		context = new AnnotationConfigWebApplicationContext();
		context.setServletContext(new MockServletContext());
		context.register(StaticHandlersConfig.class);
		context.refresh();
		mockMvc = MockMvcBuilders.webAppContextSetup(context).build();
	}

	@AfterEach
	public void tearDown() {
		context.close();
	}

	private MockHttpServletResponse get(String path, String header, String value) throws Exception {
		var builder = MockMvcRequestBuilders.get(path);
		if (header != null) {
			builder = builder.header(header, value);
		}
		return mockMvc.perform(builder).andReturn().getResponse();
	}

	private String etagOf(String path) throws Exception {
		MockHttpServletResponse response = get(path, null, null);
		Assertions.assertEquals(200, response.getStatus(), path);
		String etag = response.getHeader(HttpHeaders.ETAG);
		Assertions.assertNotNull(etag, path + " must carry a content ETag so revalidation can see a changed file");
		Assertions.assertTrue(etag.startsWith("W/\"") && etag.endsWith("\""), "weak, quoted ETag: " + etag);
		return etag;
	}

	@Test
	public void asset_validates_on_etag_not_last_modified() throws Exception {
		MockHttpServletResponse response = get(ASSET, null, null);
		Assertions.assertEquals(200, response.getStatus());
		Assertions.assertNotNull(response.getHeader(HttpHeaders.ETAG), "asset must carry an ETag");
		Assertions.assertNull(response.getHeader(HttpHeaders.LAST_MODIFIED),
			"asset must not advertise Last-Modified: the reproducible build pins it to a constant date");
	}

	@Test
	public void page_validates_on_etag_not_last_modified() throws Exception {
		MockHttpServletResponse response = get(PAGE, null, null);
		Assertions.assertEquals(200, response.getStatus());
		Assertions.assertNotNull(response.getHeader(HttpHeaders.ETAG), "page shell must carry an ETag");
		Assertions.assertNull(response.getHeader(HttpHeaders.LAST_MODIFIED),
			"page shell must not advertise Last-Modified: the reproducible build pins it to a constant date");
	}

	@Test
	public void vendor_library_revalidates_on_etag_every_navigation() throws Exception {
		// Vendored library URLs are not versioned, so a bump must be picked up
		// on the next navigation: no-cache, validated by content ETag.
		MockHttpServletResponse response = get("/vendor/lit/lit.js", null, null);
		Assertions.assertEquals(200, response.getStatus());
		Assertions.assertEquals("no-cache", response.getHeader(HttpHeaders.CACHE_CONTROL));
		Assertions.assertNotNull(response.getHeader(HttpHeaders.ETAG), "vendor asset must carry an ETag");
		Assertions.assertNull(response.getHeader(HttpHeaders.LAST_MODIFIED));
	}

	@Test
	public void matching_etag_revalidates_to_304() throws Exception {
		String etag = etagOf(ASSET);
		MockHttpServletResponse response = get(ASSET, HttpHeaders.IF_NONE_MATCH, etag);
		Assertions.assertEquals(304, response.getStatus());
	}

	@Test
	public void stale_etag_from_a_previous_build_gets_the_new_file() throws Exception {
		MockHttpServletResponse response = get(ASSET, HttpHeaders.IF_NONE_MATCH, "\"etag-of-a-previous-build\"");
		Assertions.assertEquals(200, response.getStatus());
	}

	@Test
	public void if_modified_since_can_no_longer_mask_a_changed_file() throws Exception {
		// A browser holding the previous build sends the fixed build date back;
		// any date-based comparison says "unchanged". Only an ETag can tell.
		MockHttpServletResponse response = get(ASSET, HttpHeaders.IF_MODIFIED_SINCE, FAR_FUTURE);
		Assertions.assertEquals(200, response.getStatus(),
			"a date-only conditional request must be answered with the current file");
	}

	@Test
	public void etag_is_the_sha256_of_the_served_bytes() throws Exception {
		byte[] content;
		try (InputStream in = new ClassPathResource("static" + ASSET).getInputStream()) {
			content = in.readAllBytes();
		}
		String expected = "W/\"" + HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(content)) + "\"";
		Assertions.assertEquals(expected, etagOf(ASSET));
		Assertions.assertEquals(expected, etagOf(ASSET), "ETag must be stable across requests");
	}

	@Test
	public void etag_does_not_stop_tomcat_compressing_the_response() throws Exception {
		// Tomcat skips gzip for any response with a strong ETag; this pins that
		// the generated tag keeps assets compressible under the bundled Tomcat.
		CompressionConfig compression = new CompressionConfig();
		compression.setCompression("on");
		compression.setCompressionMinSize(1);
		compression.setCompressibleMimeType("text/javascript");

		Request request = new Request();
		request.getMimeHeaders().addValue("Accept-Encoding").setString("gzip");
		Response response = new Response();
		response.setRequest(request);
		response.setContentType("text/javascript");
		response.setContentLength(5000);
		String etag = get(ASSET, null, null).getHeader(HttpHeaders.ETAG);
		Assertions.assertNotNull(etag);
		response.getMimeHeaders().addValue("ETag").setString(etag);

		Assertions.assertTrue(compression.useCompression(request, response),
			"the static-asset ETag must not disable Tomcat gzip compression");
	}
}
