package io.fintechlabs.testframework.fapiciba;

import io.fintechlabs.testframework.testmodule.PublishTestModule;
import io.fintechlabs.testframework.testmodule.Variant;

@PublishTestModule(
	testName = "fapi-ciba-poll-ensure-authorization-request-with-potentially-bad-binding-message-with-mtls",
	displayName = "FAPI-CIBA: Poll mode - test with a potentially bad binding message, the server should authenticate successfully or return the invalid_binding_message error (MTLS client authentication)",
	summary = "This test tries sending a potentially bad binding message to authorization endpoint request. The server should either authenticate successfully showing the correct binding message (a screenshot/photo of which should be uploaded) or return the invalid_binding_message error.",
	profile = "FAPI-CIBA",
	configurationFields = {
		"server.discoveryUrl",
		"client.client_id",
		"client.scope",
		"client.jwks",
		"client.hint_type",
		"client.hint_value",
		"mtls.key",
		"mtls.cert",
		"mtls.ca",
		"client2.client_id",
		"client2.scope",
		"client2.jwks",
		"mtls2.key",
		"mtls2.cert",
		"mtls2.ca",
		"resource.resourceUrl"
	}
)
public class FAPICIBAPollEnsureAuthorizationRequestWithPotentiallyBadBindingMessageWithMTLS extends AbstractFAPICIBAEnsureAuthorizationRequestWithPotentiallyBadBindingMessageWithMTLS {
	@Variant(name = FAPICIBA.variant_poll_mtls)
	public void setupPollMTLS() {
		// FIXME: add other variants
		super.setupPollMTLS();
	}

	@Override
	protected void waitForAuthenticationToComplete(long delaySeconds) {
		waitForPollingAuthenticationToComplete(delaySeconds);
	}

	@Override
	protected void modeSpecificAuthorizationEndpointRequest() {
		/* Nothing to do */
	}

}
