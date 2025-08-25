package net.openid.conformance.openid.federation.client;

import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.as.AddInvalidAudValueToIdToken;
import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "openid-federation-client-invalid-aud-in-id-token",
	displayName = "openid-federation-client-invalid-aud-in-id-token",
	summary = "openid-federation-client-invalid-aud-in-id-token",
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
@SuppressWarnings("unused")
public class OpenIDFederationClientInvalidAudInIdTokenTest extends OpenIDFederationClientTest {

	@Override
	protected void beforeSigningIdToken() {
		callAndContinueOnFailure(AddInvalidAudValueToIdToken.class, Condition.ConditionResult.FAILURE);
	}

}
