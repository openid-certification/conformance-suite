package net.openid.conformance.openid.federation.client;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.as.Add30SecondExpValueToIdToken;
import net.openid.conformance.condition.as.AddInvalidIssValueToIdToken;
import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.testmodule.TestFailureException;
import net.openid.conformance.testmodule.UserFacing;

@PublishTestModule(
	testName = "openid-federation-client-invalid-iss-in-entity-configuration",
	displayName = "OpenID Federation client test: Invalid iss in entity configuration",
	summary = "The test deliberately inserts an invalid iss into its entity configuration, " +
		"which must be detected and rejected by the RP.",
	profile = "OIDFED"
)
@SuppressWarnings("unused")
public class OpenIDFederationClientInvalidIssInEntityConfigurationTest extends OpenIDFederationClientTest {

	@Override
	protected Object entityConfigurationResponse() {
		setStatus(Status.RUNNING);
		JsonObject server = env.getObject("server");
		String originalIss = env.getString("server", "iss");

		env.mapKey("id_token_claims", "server");
		callAndContinueOnFailure(Add30SecondExpValueToIdToken.class, Condition.ConditionResult.FAILURE);
		callAndContinueOnFailure(AddInvalidIssValueToIdToken.class, Condition.ConditionResult.FAILURE, "OIDFED-3");
		env.unmapKey("id_token_claims");
		setStatus(Status.WAITING);

		Object response = super.entityConfigurationResponse("server", SignEntityStatementWithServerKeys.class);
		env.putString("server", "iss", originalIss);

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
