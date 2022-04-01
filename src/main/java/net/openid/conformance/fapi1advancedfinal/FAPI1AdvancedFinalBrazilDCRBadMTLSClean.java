package net.openid.conformance.fapi1advancedfinal;

import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.client.*;
import net.openid.conformance.sequence.client.CallDynamicRegistrationEndpointAndVerifySuccessfulResponse;
import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "fapi1-advanced-final-brazil-dcr-bad-mtls-clean",
	displayName = "FAPI1-Advanced-Final: Brazil DCR bad MTLS",
	summary = "Perform the DCR flow, but presenting a TLS client certificate that should not be trusted - the server must reject the registration attempt, either by refusing the TLS negotiation or returning a valid error response. The client configuration endpoint GET and DELETE methods are called with a bad TLS certificate and must be rejected.",
	profile = "FAPI1-Advanced-Final",
	configurationFields = {
		"server.discoveryUrl",
		"client.jwks",
		"mtls.key",
		"mtls.cert",
		"mtls.ca",
		"directory.discoveryUrl",
		"directory.client_id",
		"directory.apibase"
	}
)
public class FAPI1AdvancedFinalBrazilDCRBadMTLSClean extends FAPI1AdvancedFinalBrazilDCRBadMTLS{
	@Override
	protected void setupResourceEndpoint() {
		// not needed as resource endpoint won't be called
	}

	@Override
	protected boolean scopeContains(String requiredScope) {
		// Not needed as scope field is optional
		return false;
	}

	@Override
	protected void validateDcrResponseScope() {
		// Not needed as scope field is optional
	}

	@Override
	protected void callRegistrationEndpoint() {
		callAndStopOnFailure(GenerateFakeMTLSCertificate.class);

		callAndStopOnFailure(GenerateFakeMTLSCertificate.class);

		mapToWrongMTLS();

		eventLog.startBlock("Call dynamic client registration endpoint with no/bad certificate");

		callAndStopOnFailure(CallDynamicRegistrationEndpointAllowingTLSFailure.class);

		boolean sslError = env.getBoolean(CallDynamicRegistrationEndpointAllowingTLSFailure.RESPONSE_SSL_ERROR_KEY);
		if (sslError) {
			// the ssl connection was dropped; that's an acceptable way for a server to indicate that a TLS client cert
			// is required, so there's no further checks to do
		} else {
			env.mapKey("endpoint_response", "dynamic_registration_endpoint_response");
			callAndContinueOnFailure(EnsureContentTypeJson.class, Condition.ConditionResult.WARNING, "RFC7591-3.2.2");
			callAndContinueOnFailure(EnsureHttpStatusCodeIs400or401.class, Condition.ConditionResult.FAILURE, "RFC7591-3.2.2");
			if (env.getBoolean(EnsureContentTypeJson.endpointResponseWasJsonKey)) {
				// an error to be returned in this case doesn't really seem to be defined anywhere, so allow any error
				callAndContinueOnFailure(CheckDynamicRegistrationEndpointReturnedError.class, Condition.ConditionResult.FAILURE, "RFC7591-3.2.2");
				callAndContinueOnFailure(CheckNoClientIdFromDynamicRegistrationEndpoint.class, Condition.ConditionResult.FAILURE, "RFC7591-3.2.2");
			}
		}

		env.unmapKey("mutual_tls_authentication");

		eventLog.startBlock("Call dynamic client registration endpoint with correct certificate");

		call(sequence(CallDynamicRegistrationEndpointAndVerifySuccessfulResponse.class));
		callAndContinueOnFailure(ClientManagementEndpointAndAccessTokenRequired.class, Condition.ConditionResult.FAILURE, "BrazilOBDCR-7.1", "RFC7592-2");
		validateDcrResponseScope();
		eventLog.endBlock();
	}
}
