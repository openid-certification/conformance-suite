package net.openid.conformance.openid.federation.client;

import net.openid.conformance.condition.Condition;
import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "openid-federation-client-valid-unknown-client-registration-types-supported",
	displayName = "OpenID Federation RP test: Valid 'unknown' value in client_registration_types_supported in entity configuration",
	summary = "The entity configuration contains openid_provider metadata " +
		"with the value 'unknown' added to the required property client_registration_types_supported array, " +
		"which is allowed according to the specification.",
	profile = "OIDFED"
)
@SuppressWarnings("unused")
public class OpenIDFederationClienValidUnknownClientRegistrationTypesSupportedTest extends OpenIDFederationClientTest {

	@Override
	protected Object entityConfigurationResponse() {
		setStatus(Status.RUNNING);
		// "Additional values MAY be defined and used, without restriction by this specification."
		callAndContinueOnFailure(AddUnknownClientRegistrationTypesSupportedInEntityConfiguration.class, Condition.ConditionResult.FAILURE, "OIDFED-5.1.3");
		setStatus(Status.WAITING);

		Object response = super.entityConfigurationResponse("server", SignEntityStatement.class);
		startWaitingForTimeout();

		return response;
	}

}
