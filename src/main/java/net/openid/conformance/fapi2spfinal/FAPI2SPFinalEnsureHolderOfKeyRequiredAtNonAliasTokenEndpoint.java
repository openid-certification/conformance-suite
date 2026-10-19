package net.openid.conformance.fapi2spfinal;

import com.google.common.base.Strings;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.client.BuildRequestObjectByReferenceRedirectToAuthorizationEndpointWithoutDuplicates;
import net.openid.conformance.condition.client.CallTokenEndpointAllowingDpopNonceErrorOrTLSFailureAndReturnFullResponse;
import net.openid.conformance.condition.client.RemoveMTLSCertificates;
import net.openid.conformance.condition.client.SetTokenEndpointToNonMtlsTokenEndpoint;
import net.openid.conformance.sequence.ConditionSequence;
import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.variant.ClientAuthType;

@PublishTestModule(
	testName = "fapi2-security-profile-final-ensure-holder-of-key-required-at-non-alias-token-endpoint",
	displayName = "FAPI2-Security-Profile-Final: ensure TLS client certificate required at the token endpoint outside mtls_endpoint_aliases",
	summary = "This test ensures that the token_endpoint outside mtls_endpoint_aliases returns an error if a valid request is sent without a TLS certificate. The test is skipped if the server does not publish a token endpoint in mtls_endpoint_aliases that differs from token_endpoint. With DPoP sender constraining, the request carries a valid DPoP proof, and the test is skipped if the client presents no TLS certificate at the token endpoint, i.e. it does not use MTLS client authentication and the profile does not require MTLS on every endpoint.",
	profile = "FAPI2-Security-Profile-Final"
)
public class FAPI2SPFinalEnsureHolderOfKeyRequiredAtNonAliasTokenEndpoint extends FAPI2SPFinalEnsureHolderOfKeyRequired {

	@Override
	public void start() {
		setStatus(Status.RUNNING);

		String mtlsTokenEndpoint = env.getString("server", "mtls_endpoint_aliases.token_endpoint");
		if (mtlsTokenEndpoint == null || mtlsTokenEndpoint.equals(env.getString("server", "token_endpoint"))) {
			fireTestSkipped("The server does not publish a token endpoint in mtls_endpoint_aliases that differs from token_endpoint, so there is no separate token endpoint to test.");
			return;
		}

		if (isDpop() && !isClientAuthMtls() && !profileRequiresMtlsEverywhere) {
			fireTestSkipped("The client does not present a TLS certificate at the token endpoint in this configuration: DPoP is used for sender constraining, the client authentication method is not MTLS, and the profile does not require MTLS on every endpoint.");
			return;
		}

		if (clientCredentialsGrant) {
			performCredentialsFlow();
		} else {
			performAuthorizationFlow();
		}
	}

	@Override
	protected void performPARRedirectWithRequestUri() {
		// clients shall only send client_id and request_uri to the authorization endpoint after PAR
		// (FAPI2-SP-FINAL-5.3.3.2-6), as shown in RFC9126 section 4
		eventLog.startBlock(currentClientString() + "Make request to authorization endpoint");
		callAndStopOnFailure(BuildRequestObjectByReferenceRedirectToAuthorizationEndpointWithoutDuplicates.class, "PAR-4");
		performRedirect();
	}

	@Override
	protected void performPostAuthorizationFlow() {
		callAndStopOnFailure(SetTokenEndpointToNonMtlsTokenEndpoint.class, "RFC8705-5");
		super.performPostAuthorizationFlow();
	}

	@Override
	protected void callTokenEndpointWithoutHolderOfKey() {
		if (!isDpop()) {
			super.callTokenEndpointWithoutHolderOfKey();
			return;
		}

		// The certificate is used for MTLS client authentication or because the profile requires MTLS on every
		// endpoint. Send a valid DPoP proof so the missing certificate is the only defect. The AS may issue a DPoP
		// nonce challenge before checking the certificate; client authentication is regenerated for the retry as
		// the AS may reject a reused client assertion jti.
		callAndStopOnFailure(RemoveMTLSCertificates.class);
		for (int i = 0; i < 2; i++) {
			if (i > 0) {
				addClientAuthenticationToTokenEndpointRequest();
			}
			createDpopForTokenEndpoint();
			if (isClientAuthMtls()) {
				callAndStopOnFailure(CallTokenEndpointAllowingDpopNonceErrorOrTLSFailureAndReturnFullResponse.class, Condition.ConditionResult.FAILURE, "RFC8705-2");
			} else {
				// the certificate is required by the profile (MTLS on every endpoint), for which no numbered
				// requirement is available
				callAndStopOnFailure(CallTokenEndpointAllowingDpopNonceErrorOrTLSFailureAndReturnFullResponse.class, Condition.ConditionResult.FAILURE);
			}
			if (env.getBoolean("token_endpoint_response_ssl_error")
				|| Strings.isNullOrEmpty(env.getString("token_endpoint_dpop_nonce_error"))) {
				break;
			}
		}
	}

	@Override
	protected Class<? extends ConditionSequence> getValidateTokenEndpointResponseSteps() {
		if (!isDpop()) {
			return super.getValidateTokenEndpointResponseSteps();
		}
		// without MTLS client authentication the missing certificate may surface as any of invalid_client,
		// invalid_grant or invalid_request, which is what the private_key_jwt sequence accepts
		return isClientAuthMtls() ? ValidateTokenEndpointResponseWithMTLS.class : ValidateTokenEndpointResponseWithPrivateKeyAndMTLSHolderOfKey.class;
	}

	private boolean isClientAuthMtls() {
		return getVariant(ClientAuthType.class) == ClientAuthType.MTLS;
	}
}
