package net.openid.conformance.condition.as;

import com.google.common.base.Strings;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import com.nimbusds.jose.jwk.JWK;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;
import net.openid.conformance.util.JWKUtil;

import java.text.ParseException;
import java.time.Instant;
import java.util.Date;

import static java.time.temporal.ChronoUnit.DAYS;

public abstract class AbstractValidateDpopProof extends AbstractCondition {

	// TODO: make this configurable
	private int timeSkewMillis = 5 * 60 * 1000; // 5 minute allowable skew for testing

	// Common method to validate DPoP Proofs
	// isTokenRequest is used to indicate whether the DPoP proof is used to request an access token
	// or whether it's used along with an constrained access token to request resource access.
	// Token requests checks to make sure 'ath' claim in not included whereas resource requests checks
	// to make sure 'ath' claim is included.
	protected Environment validateDpopProof(Environment env, boolean isTokenRequest) {
		Instant now = Instant.now(); // to check timestamps

		// Check Header claims
		JsonElement typ = env.getElementFromObject("incoming_dpop_proof", "header.typ");
		if(typ  == null) {
			throw error("'typ' claim in DPoP Proof header is missing");
		}
		if(!("dpop+jwt").equals(OIDFJSON.getString(typ))) {
			throw error("Invalid DPoP Proof 'typ' header", args("expected", "dpop+jwt", "actual", OIDFJSON.getString(typ)));
		}
		JsonElement jsonJwk = env.getElementFromObject("incoming_dpop_proof", "header.jwk");
		if (jsonJwk == null) {
			throw error("'jwk' claim in DPoP Proof is missing");
		}

		try {
			JWK jwk = JWK.parse(jsonJwk.toString());
			if(jwk.isPrivate()) {
				throw error("DPoP Proof jwk contains private key information", args("jwk", jsonJwk.toString()));
			}
		}
		catch(ParseException e) {
			throw error("Invalid DPoP Proof jwk", args("jwk", jsonJwk.toString()));
		}
		JsonElement alg = env.getElementFromObject("incoming_dpop_proof", "header.alg");
		if(alg  == null) {
			throw error("'alg' claim in DPoP Proof header is missing");
		}

		JsonArray supportedAlgs = new JsonArray();
		supportedAlgs.add("PS256");
		supportedAlgs.add("ES256");
		if(!supportedAlgs.contains(alg)) {
			throw error("Unsupported 'alg' claim in DPoP Proof header ", args("expected one of ", supportedAlgs.toString(), "actual", OIDFJSON.getString(alg)));
		}

		// Check payload claims
		JsonElement jti = env.getElementFromObject("incoming_dpop_proof", "claims.jti");
		if(jti  == null) {
			throw error("'jti' claim in DPoP Proof is missing");
		}
		String jtiStr = OIDFJSON.getString(jti);
		if(jtiStr.isEmpty()) {
			throw error("'jti' claim in DPoP Proof is blank");
		} else {
			// TODO check jti unique across requests
		}

		JsonElement htm = env.getElementFromObject("incoming_dpop_proof", "claims.htm");
		if(htm  == null) {
			throw error("'htm' claim in DPoP Proof is missing");
		}
		String expectedMethod = env.getString("incoming_request", "method");
		if(!expectedMethod.equals(OIDFJSON.getString(htm))) {
			throw error("Unexpected 'htm' in DPoP Proof", args("expected", expectedMethod, "actual", OIDFJSON.getString(htm)));
		}

		String expectedUrl = env.getString("incoming_request", "request_url");
		JsonElement htu = env.getElementFromObject("incoming_dpop_proof", "claims.htu");
		if(htu  == null) {
			throw error("'htu' claim in DPoP Proof is missing");
		}
		if(!expectedUrl.equals(OIDFJSON.getString(htu))) {
			throw error("Unexpected 'htu' in DPoP Proof", args("expected", expectedUrl, "actual", OIDFJSON.getString(htu)));
		}

		Long iat = env.getLong("incoming_dpop_proof", "claims.iat");
		if (iat == null) {
			throw error("'iat' claim in DPoP Proof is missing");
		}

		if (now.plusMillis(timeSkewMillis).isBefore(Instant.ofEpochSecond(iat))) {
			throw error("DPoP Proof 'iat' in the future", args("issued-at", new Date(iat * 1000L), "now", now));
		}
		if (now.minusMillis(timeSkewMillis).isAfter(Instant.ofEpochSecond(iat))) {
			// as per OIDCC, the client can reasonably assume servers send iat values that match the current time:
			// "The iat Claim can be used to reject tokens that were issued too far away from the current time, limiting
			// the amount of time that nonces need to be stored to prevent attacks. The acceptable range is Client specific."
			throw error("DPoP Proof  'iat' more than 5 minutes in the past", args("issued-at", new Date(iat * 1000L), "now", now));
		}

		// nbf - not actually part of spec; but JWT defines known behaviour that really should be followed
		Long nbf = env.getLong("incoming_dpop_proof", "claims.nbf");
		if (nbf != null) {
			if (now.plusMillis(timeSkewMillis).isBefore(Instant.ofEpochSecond(nbf))) {
				// this is just something to log, it doesn't make the token invalid
				log("DPoP Proof has future not-before", args("not-before", new Date(nbf * 1000L), "now", now));
			}
		}

		// exp - not actually part of spec; but JWT defines known behaviour that really should be followed
		Long exp = env.getLong("incoming_dpop_proof", "claims.exp");
		if (exp != null) {
			if (now.minusMillis(timeSkewMillis).isAfter(Instant.ofEpochSecond(exp))) {
				// this is just something to log, it doesn't make the token invalid
				log("DPoP Proof has expired", args("exp", new Date(exp * 1000L), "now", now));
			}
		}

		JsonElement ath = env.getElementFromObject("incoming_dpop_proof", "claims.ath");
		if(isTokenRequest) { // DPoP Request, ensure no 'ath' claim
			if(ath != null) {
				throw error("DPoP Proof request contains 'ath' claim");
			}
		} else { // Resource request, ensure 'ath' is available
			if(ath == null) {
				throw error("'ath' claim in DPoP Proof is missing");
			}
		}


		String expectedNonce = env.getString("dpop_nonce"); // check for server side saved nonce

		// check for incoming nonce
		JsonElement incomingNonce = env.getElementFromObject("incoming_dpop_proof", "claims.nonce");

		if(null == expectedNonce) { // server did not set nonce
			if(incomingNonce  != null) {
				throw error("DPoP proof contains unexpected nonce", args("nonce", OIDFJSON.getString(incomingNonce)));
			}
		} else { // server set nonce
			if(null == incomingNonce) {
				throw error("DPoP Proof does not contain an expected nonce", args("expected", expectedNonce));
			} else if(!expectedNonce.equals(OIDFJSON.getString(incomingNonce))) {
				throw error("DPoP Proof contains an invalid nonce", args("nonce", OIDFJSON.getString(incomingNonce), "expected", expectedNonce));
			}
		}

		logSuccess("DPoP Proof type, alg, jwk, jti, htm, htu, iat, exp, nbf, nonce passed validation checks");
		return env;
	}
}
