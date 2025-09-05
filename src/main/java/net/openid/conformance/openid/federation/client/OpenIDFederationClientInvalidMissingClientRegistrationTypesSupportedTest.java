package net.openid.conformance.openid.federation.client;

import net.openid.conformance.condition.Condition;
import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.testmodule.TestFailureException;
import net.openid.conformance.testmodule.UserFacing;

@PublishTestModule(
	testName = "openid-federation-client-invalid-missing-client-registration-types-support",
	displayName = "openid-federation-client-invalid-missing-client-registration-types-support",
	summary = "openid-federation-client-invalid-missing-client-registration-types-support",
	profile = "OIDFED"
)
@SuppressWarnings("unused")
public class OpenIDFederationClientInvalidMissingClientRegistrationTypesSupportedTest extends OpenIDFederationClientTest {

	@Override
	protected Object entityConfigurationResponse() {
		setStatus(Status.RUNNING);
		callAndContinueOnFailure(RemoveClientRegistrationTypesSupportedFromEntityConfiguration.class, Condition.ConditionResult.FAILURE, "OIDFED-5.1.3");
		setStatus(Status.WAITING);

		Object response = super.entityConfigurationResponse("server", SignEntityStatementWithServerKeys.class);
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
