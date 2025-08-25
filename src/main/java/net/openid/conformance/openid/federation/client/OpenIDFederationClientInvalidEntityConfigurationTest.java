package net.openid.conformance.openid.federation.client;

import com.google.gson.JsonObject;
import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "openid-federation-client-invalid-entity-configuration",
	displayName = "openid-federation-client-invalid-entity-configuration",
	summary = "openid-federation-client-invalid-entity-configuration",
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
public class OpenIDFederationClientInvalidEntityConfigurationTest extends OpenIDFederationClientTest {

	@Override
	protected Object entityConfigurationResponse() {
		JsonObject server = env.getObject("server");

		Object response = super.entityConfigurationResponse("server", SignEntityStatementWithServerKeys.class);
		startWaitingForTimeout();

		return response;
	}


}
