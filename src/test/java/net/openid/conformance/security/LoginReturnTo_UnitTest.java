package net.openid.conformance.security;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.TestingAuthenticationToken;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class LoginReturnTo_UnitTest {

	private static final String PLAN_PAGE = "/plan-detail.html?public=true&plan=cGg1bfTQntq0x&_ga=2.55110446.448497559";

	@Test
	void onlyTheSuitesOwnPagesAreAcceptedAsAPlaceToReturnTo() {
		assertThat(LoginReturnTo.isSuitePage(PLAN_PAGE)).isTrue();
		assertThat(LoginReturnTo.isSuitePage("/plans.html")).isTrue();
		assertThat(LoginReturnTo.isSuitePage("/log-detail.html?log=abc#LOG-0001")).isFalse();

		assertThat(LoginReturnTo.isSuitePage(null)).isFalse();
		assertThat(LoginReturnTo.isSuitePage("")).isFalse();
		assertThat(LoginReturnTo.isSuitePage("https://evil.example/plans.html")).isFalse();
		assertThat(LoginReturnTo.isSuitePage("//evil.example/plans.html")).isFalse();
		assertThat(LoginReturnTo.isSuitePage("/\\evil.example/x.html")).isFalse();
		assertThat(LoginReturnTo.isSuitePage("plans.html")).isFalse();
		assertThat(LoginReturnTo.isSuitePage("/api/plan")).isFalse();
		assertThat(LoginReturnTo.isSuitePage("/x/../plans.html")).isFalse();
		assertThat(LoginReturnTo.isSuitePage("/plans.html?a=b\r\nLocation: https://evil.example")).isFalse();
		assertThat(LoginReturnTo.isSuitePage("/plans.html?" + "a".repeat(3000))).isFalse();
	}

	@Test
	void aLoginStartedFromAPageReturnsToIt() throws Exception {
		MockHttpSession session = new MockHttpSession();
		startLogin(session, PLAN_PAGE);

		MockHttpServletResponse response = completeLogin(session);

		assertThat(response.getRedirectedUrl()).isEqualTo(PLAN_PAGE);
		// used once: a later login without a page goes to the default
		assertThat(completeLogin(session).getRedirectedUrl()).isEqualTo("/");
	}

	@Test
	void anUnsafePlaceToReturnToIsIgnored() throws Exception {
		MockHttpSession session = new MockHttpSession();
		startLogin(session, "https://evil.example/");

		assertThat(completeLogin(session).getRedirectedUrl()).isEqualTo("/");
	}

	@Test
	void aLoginStartedWithoutAPageForgetsOneAnEarlierLoginAskedFor() throws Exception {
		MockHttpSession session = new MockHttpSession();
		startLogin(session, PLAN_PAGE);
		startLogin(session, null);

		assertThat(completeLogin(session).getRedirectedUrl()).isEqualTo("/");
	}

	@Test
	void aFailedLoginSendsThePageBackToTheLoginPageSoARetryReturnsToIt() throws Exception {
		MockHttpSession session = new MockHttpSession();
		startLogin(session, PLAN_PAGE);

		MockHttpServletResponse failure = failLogin(session);

		assertThat(failure.getRedirectedUrl())
			.isEqualTo("/login.html?error=" + URLEncoder.encode("bad login", StandardCharsets.UTF_8)
				+ "&" + LoginReturnTo.PARAMETER + "=" + URLEncoder.encode(PLAN_PAGE, StandardCharsets.UTF_8));
		// the login page passes returnTo on to the retry, which remembers it afresh
		assertThat(session.getAttribute(LoginReturnTo.SESSION_ATTRIBUTE)).isNull();
		startLogin(session, PLAN_PAGE);
		assertThat(completeLogin(session).getRedirectedUrl()).isEqualTo(PLAN_PAGE);
	}

	@Test
	void aFailedLoginStartedWithoutAPageGoesToTheLoginPageAlone() throws Exception {
		MockHttpSession session = new MockHttpSession();
		startLogin(session, null);

		assertThat(failLogin(session).getRedirectedUrl())
			.isEqualTo("/login.html?error=" + URLEncoder.encode("bad login", StandardCharsets.UTF_8));
	}

	@Test
	void onlyTheLoginStartIsRemembered() throws Exception {
		MockHttpSession session = new MockHttpSession();
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/plans.html");
		request.setSession(session);
		request.setParameter(LoginReturnTo.PARAMETER, PLAN_PAGE);
		new LoginReturnTo.RememberFilter().doFilter(request, new MockHttpServletResponse(), new MockFilterChain());

		assertThat(session.getAttribute(LoginReturnTo.SESSION_ATTRIBUTE)).isNull();
	}

	private static void startLogin(MockHttpSession session, String returnTo) throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/oauth2/authorization/google");
		request.setSession(session);
		if (returnTo != null) {
			request.setParameter(LoginReturnTo.PARAMETER, returnTo);
		}
		MockFilterChain chain = new MockFilterChain();
		new LoginReturnTo.RememberFilter().doFilter(request, new MockHttpServletResponse(), chain);
		assertThat(chain.getRequest()).isNotNull();
	}

	private static MockHttpServletResponse failLogin(MockHttpSession session) throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/login/oauth2/code/google");
		request.setSession(session);
		MockHttpServletResponse response = new MockHttpServletResponse();
		new LoginReturnTo.FailureHandler().onAuthenticationFailure(request, response,
			new BadCredentialsException("bad login"));
		return response;
	}

	private static MockHttpServletResponse completeLogin(MockHttpSession session) throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/login/oauth2/code/google");
		request.setSession(session);
		MockHttpServletResponse response = new MockHttpServletResponse();
		new LoginReturnTo.SuccessHandler().onAuthenticationSuccess(request, response,
			new TestingAuthenticationToken("user", "password"));
		return response;
	}
}
