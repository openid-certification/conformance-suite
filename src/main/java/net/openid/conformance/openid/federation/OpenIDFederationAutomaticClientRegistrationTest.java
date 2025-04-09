package net.openid.conformance.openid.federation;

import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.variant.FAPIAuthRequestMethod;
import org.springframework.http.HttpMethod;

@PublishTestModule(
	testName = "openid-federation-automatic-client-registration",
	displayName = "openid-federation-automatic-client-registration",
	summary = "The test acts as an RP wanting to perform automatic client registration with an OP",
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
	protected void verifyTestConditions() {
	}
}
