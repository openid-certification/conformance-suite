package io.fintechlabs.testframework.fapi;

import io.fintechlabs.testframework.condition.Condition;
import io.fintechlabs.testframework.condition.client.CheckDiscEndpointAuthorizationEndpoint;
import io.fintechlabs.testframework.condition.client.CheckDiscEndpointClaimsParameterSupported;
import io.fintechlabs.testframework.condition.client.CheckDiscEndpointRequestObjectSigningAlgValuesSupported;
import io.fintechlabs.testframework.condition.client.CheckDiscEndpointRequestParameterSupported;
import io.fintechlabs.testframework.condition.client.CheckDiscEndpointRequestUriParameterSupported;
import io.fintechlabs.testframework.condition.client.FAPIRWCheckDiscEndpointClaimsSupported;
import io.fintechlabs.testframework.condition.client.FAPIRWCheckDiscEndpointGrantTypesSupported;
import io.fintechlabs.testframework.condition.client.FAPIRWCheckDiscEndpointScopesSupported;
import io.fintechlabs.testframework.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "fapi-rw-id2-discovery-end-point-verification",
	displayName = "FAPI-RW-ID2: Discovery Endpoint Verification",
	summary = "This test ensures that the server's configurations (including scopes, response_types, grant_types etc) is containing the required value in the specification",
	profile = "FAPI-RW-ID2",
	configurationFields = {
		"server.discoveryUrl",
	}
)
public class FAPIRWID2DiscoveryEndpointVerification extends AbstractFAPIDiscoveryEndpointVerification {

	@Override
	public void start() {

		setStatus(Status.RUNNING);

		performEndpointVerification();

		callAndContinueOnFailure(CheckDiscEndpointRequestParameterSupported.class, Condition.ConditionResult.FAILURE);
		callAndContinueOnFailure(CheckDiscEndpointRequestUriParameterSupported.class, Condition.ConditionResult.WARNING, "OB-7.1-1");
		callAndContinueOnFailure(CheckDiscEndpointRequestObjectSigningAlgValuesSupported.class, Condition.ConditionResult.FAILURE);

		callAndContinueOnFailure(CheckDiscEndpointAuthorizationEndpoint.class, Condition.ConditionResult.FAILURE);

		// FAPI-RW ID2 servers are required to return acrs, which means they must support requesting the acr claim,
		// hence must support the claims parameter
		callAndContinueOnFailure(CheckDiscEndpointClaimsParameterSupported.class, Condition.ConditionResult.FAILURE, "OIDCD-3", "FAPI-RW-5.2.3-3");

		performProfileSpecificChecks();

		fireTestFinished();

	}

	protected void performProfileSpecificChecks() {
		callAndContinueOnFailure(FAPIRWCheckDiscEndpointClaimsSupported.class, Condition.ConditionResult.FAILURE);
		callAndContinueOnFailure(FAPIRWCheckDiscEndpointGrantTypesSupported.class, Condition.ConditionResult.FAILURE);
		callAndContinueOnFailure(FAPIRWCheckDiscEndpointScopesSupported.class, Condition.ConditionResult.FAILURE);
	}
}
