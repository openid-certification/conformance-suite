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

public class ValidateBrazilPaymentInitiationSignedResponse extends AbstractConditionSequence {

	private final String responseFullKey;
	private final boolean fetchOrganisationJwksUri;

	/**
	 * Validates the post-authentication payment-initiation response stored under
	 * {@code resource_endpoint_response_full}. Assumes the organisation jwks_uri has
	 * already been resolved during pre-auth.
	 */
	public ValidateBrazilPaymentInitiationSignedResponse() {
		this("resource_endpoint_response_full", false);
	}

	/**
	 * @param responseFullKey env key holding the response under validation (full HTTP response object)
	 * @param fetchOrganisationJwksUri when true, resolves the organisation's jwks_uri via
	 *   {@link FAPIBrazilGetKeystoreJwksUri} before fetching the org's jwks. Use for the pre-auth
	 *   consent-endpoint flow where no prior step has resolved it.
	 */
	public ValidateBrazilPaymentInitiationSignedResponse(String responseFullKey, boolean fetchOrganisationJwksUri) {
		this.responseFullKey = responseFullKey;
		this.fetchOrganisationJwksUri = fetchOrganisationJwksUri;
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

		// signature needs to be validated against the organisation jwks
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
