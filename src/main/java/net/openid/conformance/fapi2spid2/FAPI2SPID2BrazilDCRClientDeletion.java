package net.openid.conformance.fapi2spid2;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.client.CallClientConfigurationEndpoint;
import net.openid.conformance.condition.client.CheckErrorFromTokenEndpointResponseErrorInvalidClient;
import net.openid.conformance.condition.client.CheckForAccessTokenValue;
import net.openid.conformance.condition.client.CheckIfTokenEndpointResponseError;
import net.openid.conformance.condition.client.CheckNoClientIdFromClientConfigurationEndpoint;
import net.openid.conformance.condition.client.CheckTokenEndpointHttpStatus200;
import net.openid.conformance.condition.client.CheckTokenEndpointHttpStatus400or401;
import net.openid.conformance.condition.client.CreateTokenEndpointRequestForClientCredentialsGrant;
import net.openid.conformance.condition.client.EnsureHttpStatusCodeIs401;
import net.openid.conformance.condition.client.SetConsentsScopeOnTokenEndpointRequest;
import net.openid.conformance.condition.client.SetPaymentsScopeOnTokenEndpointRequest;
import net.openid.conformance.condition.client.UnregisterDynamicallyRegisteredClient;
import net.openid.conformance.condition.client.UnregisterDynamicallyRegisteredClientExpectingFailure;
import net.openid.conformance.condition.client.ValidateErrorFromTokenEndpointResponseError;
import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "fapi2-security-profile-id2-brazildcr-client-delete",
	displayName = "FAPI2-Security-Profile-ID2: Brazil DCR client deletion",
	summary = "Obtain a software statement from the Brazil directory (using the client MTLS certificate and directory client id provided in the test configuration), register a new client on the target authorization server then check behaviour of GET/DELETE operations after client deletion.",
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
public class FAPI2SPID2BrazilDCRClientDeletion extends AbstractFAPI2SPID2BrazilDCR {

	protected void performClientCredentialsGrant() {
		callAndStopOnFailure(CreateTokenEndpointRequestForClientCredentialsGrant.class);
		if (brazilPayments) {
			callAndStopOnFailure(SetPaymentsScopeOnTokenEndpointRequest.class);
		} else {
			callAndStopOnFailure(SetConsentsScopeOnTokenEndpointRequest.class);
		}
		call(sequence(addTokenEndpointClientAuthentication));
		callSenderConstrainedTokenEndpointAndStopOnFailure();
	}

	@Override
	public void start() {
		setStatus(Status.RUNNING);

		call(exec().startBlock("Verify that client_credentials grant can be used"));

		performClientCredentialsGrant();
		callAndContinueOnFailure(CheckTokenEndpointHttpStatus200.class, Condition.ConditionResult.FAILURE);
		callAndStopOnFailure(CheckIfTokenEndpointResponseError.class);
		callAndStopOnFailure(CheckForAccessTokenValue.class);


		eventLog.startBlock("Deleting client then expecting GET / DELETE on configuration endpoint to fail");
		callAndContinueOnFailure(UnregisterDynamicallyRegisteredClient.class, Condition.ConditionResult.FAILURE, "BrazilOBDCR-9.3.2-4", "RFC7592-2.3");

		callAndStopOnFailure(CallClientConfigurationEndpoint.class, "OIDCD-4.2");

		call(exec().mapKey("endpoint_response", "registration_client_endpoint_response"));

		callAndContinueOnFailure(EnsureHttpStatusCodeIs401.class, Condition.ConditionResult.FAILURE, "RFC7592-2.1");
		callAndContinueOnFailure(CheckNoClientIdFromClientConfigurationEndpoint.class, Condition.ConditionResult.FAILURE);

		call(exec().unmapKey("endpoint_response"));

		callAndContinueOnFailure(UnregisterDynamicallyRegisteredClientExpectingFailure.class, Condition.ConditionResult.FAILURE, "BrazilOBDCR-9.3.2-4", "RFC7592-2.3");

		call(exec().startBlock("Verify that client_credentials grant fails now client has been deleted"));

		performClientCredentialsGrant();

		callAndContinueOnFailure(ValidateErrorFromTokenEndpointResponseError.class, Condition.ConditionResult.FAILURE);
		callAndContinueOnFailure(CheckTokenEndpointHttpStatus400or401.class, Condition.ConditionResult.FAILURE);
		callAndContinueOnFailure(CheckErrorFromTokenEndpointResponseErrorInvalidClient.class, Condition.ConditionResult.FAILURE);

		// we already deregistered the client, so prevent cleanup from trying to do so again
		JsonObject client = env.getObject("client");
		client.remove("registration_client_uri");
		client.remove("registration_access_token");

		fireTestFinished();
	}

}
