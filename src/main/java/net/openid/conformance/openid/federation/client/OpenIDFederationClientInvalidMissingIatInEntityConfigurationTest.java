package net.openid.conformance.openid.federation.client;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.as.RemoveIatFromIdToken;
import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.testmodule.TestFailureException;
import net.openid.conformance.testmodule.UserFacing;

@PublishTestModule(
	testName = "openid-federation-client-invalid-missing-iat-in-entity-configuration",
	displayName = "OpenID Federation client test: Missing iat in entity configuration",
	summary = "The test deliberately removes the iat from its entity configuration, " +
		"which must be detected and rejected by the RP.",
	profile = "OIDFED"
)
@SuppressWarnings("unused")
public class OpenIDFederationClientInvalidMissingIatInEntityConfigurationTest extends OpenIDFederationClientTest {

	@Override
	protected Object entityConfigurationResponse() {
		setStatus(Status.RUNNING);
		JsonObject server = env.getObject("server");

		env.mapKey("id_token_claims", "server");
		callAndContinueOnFailure(RemoveIatFromIdToken.class, Condition.ConditionResult.FAILURE, "OIDFED-3");
		env.unmapKey("id_token_claims");
		setStatus(Status.WAITING);

		env.mapKey("entity_configuration_claims", "server");
		env.mapKey("entity_configuration_claims_jwks", "op_ec_jwks");
		Object response = super.entityConfigurationResponse("server", SignEntityStatement.class);
		env.unmapKey("entity_configuration_claims");
		env.unmapKey("entity_configuration_claims_jwks");

		startWaitingForTimeout();

		return response;
	}


	@Override
	@UserFacing
	protected Object authorizeResponse(String requestId) {
		setStatus(Status.RUNNING);
		call(exec().startBlock("Authorization endpoint").mapKey("incoming_request", requestId));
		throw new TestFailureException(getId(), "This test deliberately returned an invalid entity configuration. " +
			"In such cases, the RP must not attempt further interaction with a federation entity OP.");
	}

}
