package net.openid.conformance.openid.federation;

import net.openid.conformance.variant.FAPIAuthRequestMethod;
import org.springframework.http.HttpMethod;

public class OpenIDFederationAutomaticClientRegistrationTest extends AbstractOpenIDFederationAutomaticClientRegistrationTest {

	@Override
	protected FAPIAuthRequestMethod getRequestMethod() {
		return null;
	}

	@Override
	protected HttpMethod getHttpMethodForAuthorizeRequest() {
		return null;
	}

	@Override
	protected void verifyTestConditions() { }

	@Override
	protected void redirect(HttpMethod method) {
		performRedirect(method.name());
	}
}
