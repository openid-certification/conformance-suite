package net.openid.conformance.openid.federation.client;

import net.openid.conformance.condition.Condition;
import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "openid-federation-client-valid-unknown-client-registration-types-support",
	displayName = "openid-federation-client-valid-unknown-client-registration-types-support",
	summary = "openid-federation-client-valid-unknown-client-registration-types-support",
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

		Object response = super.entityConfigurationResponse("server", SignEntityStatementWithServerKeys.class);
		startWaitingForTimeout();

		return response;
	}

}
