package net.openid.conformance.openid.federation;

import com.google.gson.JsonElement;
import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.variant.FAPIAuthRequestMethod;
import org.springframework.http.HttpMethod;

@PublishTestModule(
		testName = "openid-federation-automatic-client-registration-with-jar-and-get-and-trust-chain",
		displayName = "openid-federation-automatic-client-registration-with-jar-and-get-and-trust-chain",
		summary = "The test acts as an RP wanting to perform automatic client registration with an OP, " +
			"with JAR and HTTP GET to the authorization endpoint. The authorization request will contain " +
			"the client trust_chain in the test configuration.",
		profile = "OIDFED"
)
@SuppressWarnings("unused")
public class OpenIDFederationAutomaticClientRegistrationWithJarAndGetAndTrustChainTest extends OpenIDFederationAutomaticClientRegistrationTest {

	@Override
	protected FAPIAuthRequestMethod getRequestMethod() {
		return FAPIAuthRequestMethod.BY_VALUE;
	}

	@Override
	protected HttpMethod getHttpMethodForAuthorizeRequest() {
		return HttpMethod.GET;
	}

	@Override
	protected void verifyTestConditions() {
		JsonElement trustChain = env.getElementFromObject("config", "client.trust_chain");
		if (trustChain == null) {
			fireTestSkipped("The client trust_chain is not provided in the test configuration");
		}
		includeTrustChainInAuthorizationRequest = true;
	}

}
