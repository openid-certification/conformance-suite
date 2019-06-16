package io.fintechlabs.testframework.fapiciba;

import io.fintechlabs.testframework.condition.client.AddIatValueIsWeekInPastToRequestObject;
import io.fintechlabs.testframework.testmodule.PublishTestModule;
import io.fintechlabs.testframework.testmodule.Variant;

@PublishTestModule(
	testName = "fapi-ciba-poll-with-mtls-ensure-request-object-iat-is-week-in-past-fails",
	displayName = "FAPI-CIBA: Poll mode - 'iat' value in request object is a week in the past, should return an error (MTLS client authentication)",
	summary = "This test should return an error that the 'iat' value in request object from back channel authentication endpoint request is a week in the past",
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
public class FAPICIBAPollWithMTLSEnsureRequestObjectIatIsWeekInPastFails extends AbstractFAPICIBAWithMTLSEnsureRequestObjectFails {

	@Variant(name = FAPICIBA.variant_poll_mtls)
	public void setupPollMTLS() {
		// FIXME: add other variants
		super.setupPollMTLS();
	}

	@Override
	protected void cleanupAfterBackchannelRequestShouldHaveFailed() {
		pollCleanupAfterBackchannelRequestShouldHaveFailed();
	}

	@Override
	protected void createAuthorizationRequestObject() {

		super.createAuthorizationRequestObject();

		callAndStopOnFailure(AddIatValueIsWeekInPastToRequestObject.class, "CIBA-7.1.1");

	}

	@Override
	protected void modeSpecificAuthorizationEndpointRequest() {
		/* Nothing to do */
	}

}
