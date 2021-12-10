package net.openid.conformance.ekyc.condition.client;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class AddVerifiedClaimsToAuthorizationRequest extends AbstractAddVerifiedClaimsToAuthorizationRequest
{

	/**
	 * verified_claims_to_be_requested must be created elsewhere properly
	 * @param env
	 * @return
	 */
	@Override
	@PreEnvironment(required = {"authorization_endpoint_request", "server", "verified_claims_to_request"})
	@PostEnvironment(required = "authorization_endpoint_request")
	public Environment evaluate(Environment env) {
		/*
		JsonElement verifiedClaimsSupportedElement = env.getElementFromObject("server", "claims_in_verified_claims_supported");
		JsonElement trustFrameworksSupportedElement = env.getElementFromObject("server", "trust_frameworks_supported");
		JsonElement evidenceSupportedElement = env.getElementFromObject("server", "evidence_supported");
		JsonElement idDocumentsSupportedElement = env.getElementFromObject("server", "id_documents_supported");
		JsonElement idDocumentsVerificationMethodsSupportedElement = env.getElementFromObject("server", "id_documents_verification_methods_supported");
		*/
		JsonObject claimsToRequest = env.getObject("verified_claims_to_request");
		for(String claimName : claimsToRequest.keySet()) {
			addClaims(env, claimsToRequest.get(claimName).getAsJsonObject());
		}

		logSuccess("Added verified claims to authorization request",
			args("authorization_endpoint_request", env.getObject("authorization_endpoint_request")));
		return env;
	}
}
