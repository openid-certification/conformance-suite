package net.openid.conformance.openid.federation;

import net.openid.conformance.variant.FAPIAuthRequestMethod;
import org.springframework.http.HttpMethod;

/*
@PublishTestModule(
	testName = "openid-federation-automatic-client-registration-test",
	displayName = "openid-federation-automatic-client-registration-test",
	summary = "openid-federation-automatic-client-registration-test",
	profile = "OIDFED",
	configurationFields = {
		"federation.authority_hints",
		"federation.immediate_subordinates",
		"federation_trust_anchor.immediate_subordinates",
		"federation_trust_anchor.trust_anchor_jwks",
		"federation.entity_identifier_host_override",
		"client.entity_identifier",
		"client.trust_anchor",
		"client.jwks",
		"server.jwks",
		"internal.op_to_rp_mode",
		"internal.ignore_exp_iat"
	}
)
*/
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
