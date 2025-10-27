package net.openid.conformance.openid.federation.client;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.as.Add30SecondExpValueToIdToken;
import net.openid.conformance.condition.as.AddInvalidSubValueToIdToken;
import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.testmodule.TestFailureException;
import net.openid.conformance.testmodule.UserFacing;

@PublishTestModule(
	testName = "openid-federation-client-invalid-sub-in-entity-configuration",
	displayName = "OpenID Federation client test: Invalid sub in entity configuration",
	summary = "The test deliberately inserts an invalid sub into its entity configuration, " +
		"which must be detected and rejected by the RP.",
	profile = "OIDFED"
)
@SuppressWarnings("unused")
public class OpenIDFederationClientInvalidSubInEntityConfigurationTest extends OpenIDFederationClientTest {

	@Override
	protected Object entityConfigurationResponse() {
		setStatus(Status.RUNNING);
		JsonObject server = env.getObject("server");
		String originalSub = env.getString("server", "sub");

		env.mapKey("id_token_claims", "server");
		callAndContinueOnFailure(Add30SecondExpValueToIdToken.class, Condition.ConditionResult.FAILURE);
		callAndContinueOnFailure(AddInvalidSubValueToIdToken.class, Condition.ConditionResult.FAILURE);
		env.unmapKey("id_token_claims");
		setStatus(Status.WAITING);

		Object response = super.entityConfigurationResponse("server", SignEntityStatementWithServerKeys.class);
		env.putString("server", "sub", originalSub);
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
