package net.openid.conformance.openid.federation;

import com.google.gson.JsonElement;
import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.variant.FAPIAuthRequestMethod;
import org.springframework.http.HttpMethod;

@PublishTestModule(
		testName = "openid-federation-automatic-client-registration-with-jar-and-post-and-trust-chain",
		displayName = "openid-federation-automatic-client-registration-with-jar-and-post-and-trust-chain",
		summary = "The test acts as an RP wanting to perform automatic client registration with an OP, " +
			"with JAR and HTTP POST to the authorization endpoint. The authorization request will contain " +
			"the client trust_chain in the test configuration.",
		profile = "OIDFED",
		configurationFields = {
			"client.jwks",
			"client.trust_chain",
			"federation.entity_identifier",
			"federation.trust_anchor",
			"federation.trust_anchor_jwks",
			"federation.authority_hints",
			"internal.op_to_rp_mode"
		}
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
		JsonElement trustChain = env.getElementFromObject("config", "client.trust_chain");
		if (trustChain == null) {
			fireTestSkipped("The client trust_chain is not provided in the test configuration");
		}
		includeTrustChainInAuthorizationRequest = true;
	}

}
