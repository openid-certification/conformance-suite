package net.openid.conformance.condition.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.nimbusds.jose.JWSAlgorithm;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class AddSdJwtClientMetadataToAuthorizationRequest extends AbstractCondition {

	@Override
	@PreEnvironment(required = { "authorization_endpoint_request"})
	public Environment evaluate(Environment env) {

		// as per https://openid.github.io/oid4vc-haip-sd-jwt-vc/openid4vc-high-assurance-interoperability-profile-sd-jwt-vc-wg-draft.html#section-7.2.7

		JsonObject authorizationEndpointRequest = env.getObject("authorization_endpoint_request");

		JsonObject clientMetaData = new JsonObject();

		JsonArray algValues = new JsonArray();
		for (JWSAlgorithm alg : JWSAlgorithm.Family.SIGNATURE) {
			algValues.add(alg.getName());
		}
		JsonObject sdJwtContents = new JsonObject();
		sdJwtContents.add("sd-jwt_alg_values", algValues);
		sdJwtContents.add("kb-jwt_alg_values", algValues);
		JsonObject sdJwt = new JsonObject();
		sdJwt.add("vc+sd-jwt", sdJwtContents);
		clientMetaData.add("vp_formats", sdJwt);

		authorizationEndpointRequest.add("client_metadata", clientMetaData);

		log("Added client_metadata to authorization endpoint request", args("client_metadata", clientMetaData));

		return env;
	}
}
