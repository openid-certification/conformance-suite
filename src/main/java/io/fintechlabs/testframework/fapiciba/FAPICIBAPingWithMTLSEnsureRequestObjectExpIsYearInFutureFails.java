package io.fintechlabs.testframework.fapiciba;

import com.google.gson.JsonObject;
import io.fintechlabs.testframework.condition.ConditionError;
import io.fintechlabs.testframework.condition.client.AddExpValueIsYearInFutureToRequestObject;
import io.fintechlabs.testframework.testmodule.PublishTestModule;
import org.apache.http.HttpStatus;

@PublishTestModule(
	testName = "fapi-ciba-ping-with-mtls-ensure-request-object-exp-is-year-in-future-fails",
	displayName = "FAPI-CIBA: Ping mode - 'exp' value in request object is a year in the future, should return an error (MTLS client authentication)",
	summary = "This test should return an error that the 'exp' value in request object from back channel authentication endpoint request is a year in the future",
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
public class FAPICIBAPingWithMTLSEnsureRequestObjectExpIsYearInFutureFails extends AbstractFAPICIBAWithMTLSEnsureRequestObjectFails {

	@Override
	protected void performPostAuthorizationResponse() {

		Integer httpStatus = env.getInteger("backchannel_authentication_endpoint_response_http_status");
		if (httpStatus != HttpStatus.SC_OK) {
			// validate error and then finish the test
			super.performPostAuthorizationResponse();
		} else {
			callAutomatedEndpoint();
		}
	}

	@Override
	protected void createAuthorizationRequestObject() {

		super.createAuthorizationRequestObject();

		callAndStopOnFailure(AddExpValueIsYearInFutureToRequestObject.class, "CIBA-7.1.1");

	}
}
