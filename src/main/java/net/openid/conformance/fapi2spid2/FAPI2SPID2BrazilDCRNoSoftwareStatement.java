package net.openid.conformance.fapi2spid2;

import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.client.CallDynamicRegistrationEndpoint;
import net.openid.conformance.condition.client.CheckDynamicRegistrationEndpointReturnedError;
import net.openid.conformance.condition.client.EnsureContentTypeJson;
import net.openid.conformance.condition.client.EnsureHttpStatusCodeIs400;
import net.openid.conformance.condition.client.FAPIBrazilAddValuesFromSoftwareStatementToDynamicRegistrationRequest;
import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "fapi2-security-profile-id2-brazil-dcr-no-software-statement",
	displayName = "FAPI2-Security-Profile-ID2: Brazil DCR no software statement",
	summary = "Perform the DCR flow, but without including a software statement (the values in the software statement are added to the body of the request) - the server must reject the registration attempt.",
	profile = "FAPI2-Security-Profile-ID2",
	configurationFields = {
		"server.discoveryUrl",
		"client.scope",
		"client.jwks",
		"mtls.key",
		"mtls.cert",
		"mtls.ca",
		"directory.discoveryUrl",
		"directory.client_id",
		"directory.apibase",
		"resource.resourceUrl"
	}
)
public class FAPI2SPID2BrazilDCRNoSoftwareStatement extends AbstractFAPI2SPID2BrazilDCR {

	@Override
	protected void addSoftwareStatementToRegistrationRequest() {
		callAndStopOnFailure(FAPIBrazilAddValuesFromSoftwareStatementToDynamicRegistrationRequest.class);
	}

	@Override
	protected void setupResourceEndpoint() {
		// not needed as resource endpoint won't be called
	}

	@Override
	protected void callRegistrationEndpoint() {
		callAndStopOnFailure(CallDynamicRegistrationEndpoint.class);

		env.mapKey("endpoint_response", "dynamic_registration_endpoint_response");
		callAndContinueOnFailure(EnsureContentTypeJson.class, Condition.ConditionResult.FAILURE, "RFC7591-3.2.2");
		callAndContinueOnFailure(EnsureHttpStatusCodeIs400.class, Condition.ConditionResult.FAILURE, "RFC7591-3.2.2");
		// an error to be returned in this case doesn't really seem to be defined by RFC7591, so allow any error
		callAndContinueOnFailure(CheckDynamicRegistrationEndpointReturnedError.class, Condition.ConditionResult.FAILURE, "RFC7591-3.2.2");
	}

	@Override
	public void start() {
		fireTestFinished();
	}
}
