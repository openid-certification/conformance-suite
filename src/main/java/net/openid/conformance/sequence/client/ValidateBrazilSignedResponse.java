package net.openid.conformance.sequence.client;

import net.openid.conformance.condition.Condition.ConditionResult;
import net.openid.conformance.condition.client.EnsureContentTypeApplicationJwt;
import net.openid.conformance.condition.client.EnsureHttpStatusCodeIs201;
import net.openid.conformance.condition.client.ExtractSignedJwtFromResourceResponse;
import net.openid.conformance.condition.client.FAPIBrazilGetKeystoreJwksUri;
import net.openid.conformance.condition.client.FAPIBrazilValidateResourceResponseSigningAlg;
import net.openid.conformance.condition.client.FAPIBrazilValidateResourceResponseTyp;
import net.openid.conformance.condition.client.FetchServerKeys;
import net.openid.conformance.condition.client.ValidateResourceResponseJwtClaims;
import net.openid.conformance.condition.client.ValidateResourceResponseSignature;
import net.openid.conformance.sequence.AbstractConditionSequence;

public class ValidateBrazilSignedResponse extends AbstractConditionSequence {

	private final String responseFullKey;
	private final boolean fetchOrganisationJwksUri;

	private ValidateBrazilSignedResponse(String responseFullKey, boolean fetchOrganisationJwksUri) {
		this.responseFullKey = responseFullKey;
		this.fetchOrganisationJwksUri = fetchOrganisationJwksUri;
	}

	/**
	 * For the post-authentication payment-initiation response stored under
	 * {@code resource_endpoint_response_full}. Assumes the organisation jwks_uri has already been
	 * resolved during pre-auth.
	 */
	public static ValidateBrazilSignedResponse forResourceResponse() {
		return new ValidateBrazilSignedResponse("resource_endpoint_response_full", false);
	}

	/**
	 * For the pre-authentication consent-endpoint response stored under
	 * {@code consent_endpoint_response_full}. Resolves the organisation's jwks_uri inline via
	 * {@link FAPIBrazilGetKeystoreJwksUri} since no prior step has done so.
	 */
	public static ValidateBrazilSignedResponse forConsentResponse() {
		return new ValidateBrazilSignedResponse("consent_endpoint_response_full", true);
	}

	@Override
	public void evaluate() {
		call(exec().mapKey("endpoint_response", responseFullKey));
		call(exec().mapKey("endpoint_response_jwt", "consent_endpoint_response_jwt"));
		callAndContinueOnFailure(EnsureContentTypeApplicationJwt.class, ConditionResult.FAILURE, "BrazilOB-6.1");
		callAndContinueOnFailure(EnsureHttpStatusCodeIs201.class, ConditionResult.FAILURE);

		callAndStopOnFailure(ExtractSignedJwtFromResourceResponse.class, "BrazilOB-6.1");

		callAndContinueOnFailure(FAPIBrazilValidateResourceResponseSigningAlg.class, ConditionResult.FAILURE, "BrazilOB-6.1");

		callAndContinueOnFailure(FAPIBrazilValidateResourceResponseTyp.class, ConditionResult.FAILURE, "BrazilOB-6.1");

		if (fetchOrganisationJwksUri) {
			callAndStopOnFailure(FAPIBrazilGetKeystoreJwksUri.class, ConditionResult.FAILURE);
		}

		call(exec().mapKey("server", "org_server"));
		call(exec().mapKey("server_jwks", "org_server_jwks"));
		callAndStopOnFailure(FetchServerKeys.class);
		call(exec().unmapKey("server"));
		call(exec().unmapKey("server_jwks"));

		callAndContinueOnFailure(ValidateResourceResponseSignature.class, ConditionResult.FAILURE, "BrazilOB-6.1");

		callAndContinueOnFailure(ValidateResourceResponseJwtClaims.class, ConditionResult.FAILURE, "BrazilOB-6.1");

		call(exec().unmapKey("endpoint_response"));
		call(exec().unmapKey("endpoint_response_jwt"));
	}
}
