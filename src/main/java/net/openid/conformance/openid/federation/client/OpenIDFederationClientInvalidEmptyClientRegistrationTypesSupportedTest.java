package net.openid.conformance.openid.federation.client;

import net.openid.conformance.condition.Condition;
import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.testmodule.TestFailureException;
import net.openid.conformance.testmodule.UserFacing;

@PublishTestModule(
	testName = "openid-federation-client-invalid-empty-client-registration-types-supported",
	displayName = "OpenID Federation client test: Invalid empty client_registration_types_supported in entity configuration",
	summary = "The entity configuration contains openid_provider metadata " +
		"with an empty array as the value for the required property client_registration_types_supported, " +
		"which the RP must detect.",
	profile = "OIDFED"
)
@SuppressWarnings("unused")
public class OpenIDFederationClientInvalidEmptyClientRegistrationTypesSupportedTest extends OpenIDFederationClientTest {

	@Override
	protected Object entityConfigurationResponse() {
		setStatus(Status.RUNNING);
		callAndContinueOnFailure(AddEmptyClientRegistrationTypesSupportedInEntityConfiguration.class, Condition.ConditionResult.FAILURE, "OIDFED-5.1.3");
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
