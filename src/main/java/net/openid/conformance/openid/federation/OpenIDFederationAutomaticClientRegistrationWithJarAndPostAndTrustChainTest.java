package net.openid.conformance.openid.federation;

import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.variant.FAPIAuthRequestMethod;
import org.springframework.http.HttpMethod;

@PublishTestModule(
	testName = "openid-federation-automatic-client-registration-with-jar-and-post-and-trust-chain",
	displayName = "OpenID Federation OP test: Automatic client registration with JAR and HTTP POST and " +
		"including the trust_chain as a parameter",
	summary = "The test acts as an RP wanting to perform automatic client registration with an OP, " +
		"with JAR and HTTP POST to the authorization endpoint. The authorization request will contain " +
		"the client trust_chain as a parameter",
	profile = "OIDFED"
)
@SuppressWarnings("unused")
public class OpenIDFederationAutomaticClientRegistrationWithJarAndPostAndTrustChainTest extends OpenIDFederationAutomaticClientRegistrationTest {

	@Override
	protected FAPIAuthRequestMethod getRequestMethod() {
		return FAPIAuthRequestMethod.BY_VALUE;
	}

	@Override
	protected HttpMethod getHttpMethodForAuthorizeRequest() {
		return HttpMethod.POST;
	}

	@Override
	protected void verifyTestConditions() {
		includeTrustChainInAuthorizationRequest = true;
	}

}
