package io.fintechlabs.testframework.fapiciba;

import io.fintechlabs.testframework.condition.client.RemoveAudFromRequestObject;
import io.fintechlabs.testframework.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "fapi-ciba-ping-with-mtls-ensure-request-object-missing-aud-fails",
	displayName = "FAPI-CIBA: Ping mode - missing aud value in request object, should return an error (MTLS client authentication)",
	summary = "This test should return an error that the aud value in request object from back channel authentication endpoint request is missing",
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
public class FAPICIBAPingWithMTLSEnsureRequestObjectMissingAudFails extends AbstractFAPICIBAWithMTLSEnsureRequestObjectFails {

	@Override
	protected void buildRequestObject() {
		super.buildRequestObject();
		callAndStopOnFailure(RemoveAudFromRequestObject.class, "CIBA-7.1.1");
	}

}
