package net.openid.conformance.condition.as;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.jwk.Curve;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.KeyType;
import com.nimbusds.jose.jwk.OctetKeyPair;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

import java.text.ParseException;
import java.time.Instant;
import java.util.Date;

public abstract class AbstractValidateDpopProof extends AbstractCondition {

	// TODO: make this configurable
	public static final int timeSkewMillis = 5 * 60 * 1000; // 5 minute allowable skew for testing

	// Common method to validate DPoP Proofs
	// isResourceRequest is used to indicate whether the DPoP proof is used for a resource endpoint request
	// Resource endpoint request checks to make sure 'ath' claim is included whereas other requests e.g.
	// Token requests checks to make sure 'ath' claim in not included
	protected Environment validateDpopProof(Environment env, boolean isResourceRequest) {
		Instant now = Instant.now(); // to check timestamps

		// Check Header claims
		JsonElement typ = env.getElementFromObject("incoming_dpop_proof", "header.typ");
		if(typ  == null) {
			throw error("'typ' claim in DPoP Proof header is missing");
		}
		if(!"dpop+jwt".equals(OIDFJSON.getString(typ))) {
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
			if(jwk.getKeyType().equals(KeyType.OKP)) {
				if(!Curve.Ed25519.equals(((OctetKeyPair)jwk).getCurve())) {
					throw error("Unsupported curve for EdDSA alg", args("JWK", jsonJwk.toString(), "curve", ((OctetKeyPair)jwk).getCurve().getName()));
				}
			}
			// compute and save jkt to avoid parsing JWK again when needed
			env.putString("incoming_dpop_proof", "computed_dpop_jkt", jwk.computeThumbprint().toString());
		}
		catch(ParseException e) {
			throw error("Invalid DPoP Proof jwk", args("jwk", jsonJwk.toString()));
		}
		catch(JOSEException e) {
			throw error("DPoP JOSEException",  e);
		}
		JsonElement alg = env.getElementFromObject("incoming_dpop_proof", "header.alg");
		if(alg  == null) {
			throw error("'alg' claim in DPoP Proof header is missing");
		}

		JsonArray supportedAlgs = new JsonArray();
		supportedAlgs.add("PS256");
		supportedAlgs.add("ES256");
		supportedAlgs.add("EdDSA");
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
		}
		// check jti unique across requests in EnsureDpopProofJtiNotUsed

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
		String htuStr = OIDFJSON.getString(htu);
		int index = htuStr.indexOf('?'); // look for '?' query and chop off
		if( index != -1) {
			htuStr = htuStr.substring(0, index);
		}
		index = htuStr.indexOf('#'); // look for '#' fragment and chop off
		if( index != -1) {
			htuStr = htuStr.substring(0, index);
		}
		if(!expectedUrl.equals(htuStr)) {
			// https://datatracker.ietf.org/doc/html/rfc9449#section-4.3
			// Will not perform normalization comparison since
			// Servers SHOULD try URL syntax normalization
			throw error("Unexpected 'htu' in DPoP Proof", args("expected", expectedUrl, "actual", OIDFJSON.getString(htu)));
		}

		Long iat = env.getLong("incoming_dpop_proof", "claims.iat");
		if (iat == null) {
			throw error("'iat' claim in DPoP Proof is missing");
		}

		// Validate iat values in ValidateDpopProofIat

		// nbf - not actually part of spec; but JWT defines known behaviour that really should be followed
		// Validate nbf in ValidateDpopProofNbf

		// exp - not actually part of spec; but JWT defines known behaviour that really should be followed
		Long exp = env.getLong("incoming_dpop_proof", "claims.exp");
		if (exp != null) {
			if (now.minusMillis(timeSkewMillis).isAfter(Instant.ofEpochSecond(exp))) {
				throw error("DPoP Proof has expired", args("exp", new Date(exp * 1000L), "now", now));
			}
		}

		JsonElement ath = env.getElementFromObject("incoming_dpop_proof", "claims.ath");
		if(!isResourceRequest) { // DPoP Request, ensure no 'ath' claim
			if(ath != null) {
				throw error("DPoP Proof request contains 'ath' claim");
			}
		} else { // Resource request, ensure 'ath' is available
			if(ath == null) {
				throw error("'ath' claim in DPoP Proof is missing");
			}
		}

		logSuccess("DPoP Proof type, alg, jwk, jti, htm, htu, iat, exp, nbf passed validation checks");
		return env;
	}
}
