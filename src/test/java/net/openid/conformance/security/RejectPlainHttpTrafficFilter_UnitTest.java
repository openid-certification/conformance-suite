package net.openid.conformance.security;

import net.openid.conformance.util.MdocIacaCrl;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * The filter rejects every plain-http request except the mdoc IACA CRL, which is served on
 * plain http because CRL distribution points are http URLs by PKI convention and the CRL
 * authenticates itself by the IACA's signature.
 */
public class RejectPlainHttpTrafficFilter_UnitTest {

	private final RejectPlainHttpTrafficFilter filter = new RejectPlainHttpTrafficFilter();

	@Test
	public void plainHttpRequestForTheCrlPassesThrough() throws Exception {
		MockFilterChain chain = new MockFilterChain();
		filter.doFilter(request("http", MdocIacaCrl.PATH), new MockHttpServletResponse(), chain);
		assertThat(chain.getRequest()).isNotNull();
	}

	@Test
	public void plainHttpRequestForAnyOtherPathIsRejected() {
		assertThatThrownBy(() -> filter.doFilter(request("http", "/mdoc-iaca-root.pem"),
			new MockHttpServletResponse(), new MockFilterChain()))
			.isInstanceOf(RuntimeException.class)
			.hasMessageContaining("non-https");
	}

	@Test
	public void httpsRequestPassesThrough() throws Exception {
		MockFilterChain chain = new MockFilterChain();
		filter.doFilter(request("https", "/mdoc-iaca-root.pem"), new MockHttpServletResponse(), chain);
		assertThat(chain.getRequest()).isNotNull();
	}

	private static MockHttpServletRequest request(String scheme, String path) {
		MockHttpServletRequest request = new MockHttpServletRequest("GET", path);
		request.setScheme(scheme);
		request.setSecure("https".equals(scheme));
		return request;
	}
}
