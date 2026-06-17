package net.openid.conformance.authzen.condition;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.nimbusds.jwt.SignedJWT;
import java.text.ParseException;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

/**
 * Structurally validate the optional {@code signed_metadata} JWT in the PDP
 * discovery metadata (certification profile
 * https://github.com/openid/authzen/issues/433 §6.4 / §6.5).
 *
 * <p>When {@code signed_metadata} is present it MUST be a JWS-signed (or MACed)
 * JWT carrying an {@code iss} claim. This condition checks that it parses as a
 * compact JWS, that the algorithm is a real signing algorithm (not {@code none}),
 * and that the {@code iss} claim is present, and stores the decoded claims in
 * {@code authzen_signed_metadata_claims} for {@link ApplySignedMetadataPrecedence}.
 *
 * <p>The cryptographic signature is verified separately by
 * {@link VerifyAuthzenSignedMetadataSignature} against the PDP key(s) supplied in
 * the test configuration (the AuthZEN metadata defines no {@code jwks_uri}, so the
 * trusted verification key is provided out of band).
 */
public class ValidateDiscoverySignedMetadata extends AbstractCondition {

	@Override
	@PreEnvironment(required = "pdp")
	@PostEnvironment(required = "pdp")
	public Environment evaluate(Environment env) {
		JsonElement signedMetadataElem = env.getElementFromObject("pdp", "signed_metadata");
		if (signedMetadataElem == null || signedMetadataElem.isJsonNull()) {
			logSuccess("Discovery metadata does not contain `signed_metadata`; nothing to validate");
			return env;
		}
		if (!signedMetadataElem.isJsonPrimitive() || !signedMetadataElem.getAsJsonPrimitive().isString()) {
			throw error("`signed_metadata` must be a JWT string", args("signed_metadata", signedMetadataElem));
		}
		String signedMetadata = OIDFJSON.getString(signedMetadataElem);

		SignedJWT jwt;
		try {
			jwt = SignedJWT.parse(signedMetadata);
		} catch (ParseException e) {
			throw error("`signed_metadata` is not a valid JWS-signed JWT", e, args("signed_metadata", signedMetadata));
		}

		if (jwt.getHeader().getAlgorithm() == null || "none".equalsIgnoreCase(jwt.getHeader().getAlgorithm().getName())) {
			throw error("`signed_metadata` MUST be signed or MACed using JWS; the `none` algorithm is not permitted",
				args("alg", jwt.getHeader().getAlgorithm() == null ? null : jwt.getHeader().getAlgorithm().getName()));
		}

		JsonObject claims = JsonParser.parseString(jwt.getPayload().toString()).getAsJsonObject();
		JsonElement issElem = claims.get("iss");
		if (issElem == null || !issElem.isJsonPrimitive() || !issElem.getAsJsonPrimitive().isString()
				|| OIDFJSON.getString(issElem).isEmpty()) {
			throw error("`signed_metadata` MUST contain an `iss` (issuer) claim", args("claims", claims));
		}

		env.putObject("authzen_signed_metadata_claims", claims);
		logSuccess("`signed_metadata` is a valid JWS JWT containing an `iss` claim",
			args("alg", jwt.getHeader().getAlgorithm().getName(), "iss", OIDFJSON.getString(issElem)));
		return env;
	}
}
