package net.openid.conformance.openid.federation;

import com.google.gson.JsonObject;
import com.nimbusds.jose.Algorithm;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSSigner;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.produce.JWSSignerFactory;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import net.openid.conformance.extensions.MultiJWSSignerFactory;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.TestFailureException;
import net.openid.conformance.util.JWKUtil;
import net.openid.conformance.util.JWTUtil;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.text.ParseException;

// Contains utility methods that don't use conditions and stuff
// that require that the test module lock is obtained
public class NonBlocking {

	public static Object entityConfigurationResponse(Environment env, String testId) {
		JsonObject entityConfigurationClaims = env.getObject("entity_configuration_claims");
		JsonObject jwks = env.getObject("entity_configuration_claims_jwks");
		String entityConfiguration = signClaims(testId, entityConfigurationClaims, jwks);
		return ResponseEntity
			.status(HttpStatus.OK)
			.contentType(EntityUtils.ENTITY_STATEMENT_JWT)
			.body(entityConfiguration);
	}

	public static Object trustAnchorFetchResponse(Environment env, String testId, String requestId) {
		env.mapKey("incoming_request", requestId);

		String sub;
		if (env.getString("incoming_request", "method").equalsIgnoreCase("POST")) {
			sub = env.getString("incoming_request", "body_form_params.sub");
		} else {
			sub = env.getString("incoming_request", "query_string_params.sub");
		}

		String federationEndpointUrl = EntityUtils.appendWellKnown(sub);

		String result = new RestTemplate().getForObject(federationEndpointUrl, String.class);
		JsonObject claims;
		try {
			claims = JWTUtil.jwtStringToJsonObjectForEnvironment(result).getAsJsonObject("claims");
		} catch (ParseException e) {
			throw new TestFailureException(testId, e.getMessage(), e);
		}

		claims.remove("authority_hints");
		claims.remove("trust_mark_issuers");
		claims.remove("trust_mark_owners");
		claims.addProperty("iss", env.getString("trust_anchor"));
		claims.addProperty("source_endpoint", env.getString("federation_fetch_endpoint"));

		JsonObject jwks = env.getObject("trust_anchor_jwks");
		String federationFetchResponse = signClaims(testId, claims, jwks);

		ResponseEntity<Object> response = ResponseEntity
			.status(200)
			.contentType(EntityUtils.ENTITY_STATEMENT_JWT)
			.body(federationFetchResponse);

		env.unmapKey("incoming_request");

		return response;
	}

	/*
	public static Object trustAnchorResolveResponse(Environment env, String testId, String requestId) {

		//callAndContinueOnFailure(ExtractParametersForTrustAnchorResolveEndpoint.class, Condition.ConditionResult.FAILURE);

		// Validate the sub parameter
		String sub = env.getString("resolve_endpoint_parameters", "sub");
		JsonArray immediateSubordinates = env.getElementFromObject("config", "federation_trust_anchor.immediate_subordinates").getAsJsonArray();
		List<String> immediateSubordinatesList = OIDFJSON.convertJsonArrayToList(immediateSubordinates);
		if (!immediateSubordinatesList.contains(sub)) {
			return errorResponse(
				"invalid_subject",
				"Parameter sub %s not found in the test configuration (Federation trust anchor -> immediate_subordinates)".formatted(sub),
				HttpStatus.NOT_FOUND.value()
			);
		}

		String trustAnchor = env.getString("resolve_endpoint_parameters", "trust_anchor");
		if (trustAnchor == null || !trustAnchor.equals(env.getString("trust_anchor"))) {
			return errorResponse(
				"invalid_trust_anchor",
				"Only trust_anchor %s is supported".formatted(trustAnchor),
				HttpStatus.NOT_FOUND.value()
			);
		}

		// Keep only the metadata that matches the entity type parameter(s)
		JsonArray entityTypes = env.getElementFromObject("resolve_endpoint_parameters", "entity_types").getAsJsonArray();
		List<String> entityTypesList = OIDFJSON.convertJsonArrayToList(entityTypes);
		JsonObject metadata = env.getElementFromObject("primary_entity_statement_jwt", "claims.metadata").getAsJsonObject();
		JsonObject filteredMetadata = filterMetadataForEntityTypes(metadata, entityTypesList);

		env.putString("federation_endpoint_url", EntityUtils.appendWellKnown(sub));
		callAndStopOnFailure(ValidateFederationUrl.class, Condition.ConditionResult.FAILURE, "OIDFED-1.2");
		fetchAndVerifyEntityStatement();
		callAndContinueOnFailure(SetPrimaryEntityStatement.class, Condition.ConditionResult.FAILURE);

		JsonArray trustChain;
		try {
			List<String> trustChainList = findPath(sub, trustAnchor);
			if (trustChainList.isEmpty()) {
				throw new TestFailureException(getId(), "Could not build a trust chain from the RP %s to trust anchor %s".formatted(sub, trustAnchor));
			}
			trustChain = buildTrustChain(trustChainList);

		} catch (AbstractOpenIDFederationTest.CyclicPathException e) {
			throw new TestFailureException(getId(), e.getMessage(), e);
		}

		JsonObject resolveResponse = EntityUtils.createBasicClaimsObject(trustAnchor, sub);

		resolveResponse.add("metadata", filteredMetadata);
		resolveResponse.add("trust_chain", trustChain);
		env.putObject("federation_resolve_response", resolveResponse);

		env.mapKey("entity_configuration_claims", "federation_resolve_response");
		//callAndStopOnFailure(SignEntityStatementWithTrustAnchorKeys.class);
		env.unmapKey("entity_configuration_claims");
		String federationResolveResponse = env.getString("signed_entity_statement");
		ResponseEntity<Object> response = ResponseEntity
			.status(200)
			.contentType(EntityUtils.RESOLVE_RESPONSE_JWT)
			.body(federationResolveResponse);

		return response;
	}
	*/

	protected static String signClaims(String testId, JsonObject claims, JsonObject jwks) {

		if (claims == null) {
			throw new TestFailureException(testId, "Couldn't find claims");
		}

		if (jwks == null) {
			throw new TestFailureException(testId, "Couldn't find jwks");
		}

		try {
			JWK signingJwk = JWKUtil.getSigningKey(jwks);
			Algorithm algorithm = signingJwk.getAlgorithm();
			if (algorithm == null) {
				throw new TestFailureException(testId, "No 'alg' field specified in key; please add 'alg' field in the configuration");
			}
			JWSAlgorithm alg = JWSAlgorithm.parse(algorithm.getName());

			JWSSignerFactory jwsSignerFactory = MultiJWSSignerFactory.getInstance();
			JWSSigner signer = jwsSignerFactory.createJWSSigner(signingJwk, alg);

			JWSHeader.Builder builder = new JWSHeader.Builder(alg);
			builder.keyID(signingJwk.getKeyID());
			JWSHeader header = builder.build();

			return performSigning(header, claims, signer);
		} catch (ParseException | IllegalArgumentException | JOSEException e) {
			throw new TestFailureException(testId, "Error while signing claims: %s".formatted(e.getMessage()), e);
		}
	}

	protected static String performSigning(JWSHeader header, JsonObject claims, JWSSigner signer) throws JOSEException, ParseException {
		JWTClaimsSet claimSet = JWTClaimsSet.parse(claims.toString());

		SignedJWT signJWT = new SignedJWT(header, claimSet);

		signJWT.sign(signer);

		return signJWT.serialize();
	}
}
