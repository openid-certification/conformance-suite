package net.openid.conformance.condition.as;

import com.authlete.sd.Disclosure;
import com.authlete.sd.SDJWT;
import com.authlete.sd.SDObjectBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSSigner;
import com.nimbusds.jose.crypto.ECDSASigner;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.produce.JWSSignerFactory;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.client.ValidateSdJwtKbSdHash;
import net.openid.conformance.extensions.MultiJWSSignerFactory;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

import java.security.NoSuchAlgorithmException;
import java.text.ParseException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public abstract class AbstractCreateSdJwtCredential extends AbstractCondition {

	protected final Map<String, Object> additionalClaims;

	protected AbstractCreateSdJwtCredential() {
		this(null);
	}

	protected AbstractCreateSdJwtCredential(Map<String, Object> additionalClaims) {
		this.additionalClaims = additionalClaims;
	}

	public String keyBindingJwt(ECKey privateKey, String aud, String nonce, String sdHash) {
		return keyBindingJwtWithIat(privateKey, aud, nonce, sdHash, Instant.now().getEpochSecond());
	}

	public String keyBindingJwtWithIat(ECKey privateKey, String aud, String nonce, String sdHash, long iat) {
		// as per https://www.ietf.org/archive/id/draft-ietf-oauth-selective-disclosure-jwt-14.html#section-4.3
		JWSHeader header = new JWSHeader.Builder(JWSAlgorithm.ES256).type(new JOSEObjectType("kb+jwt")).build();

		Map<String, Object> claims = new HashMap<>();
		claims.put("iat", iat);
		claims.put("aud", aud);
		claims.put("nonce", nonce);
		claims.put("sd_hash", sdHash);

		JWTClaimsSet claimsSet;
		try {
			claimsSet = JWTClaimsSet.parse(claims);
		} catch (ParseException e) {
			throw error("Failed to parse key binding JWT claims", e);
		}

		SignedJWT jwt = new SignedJWT(header, claimsSet);

		try {
			JWSSigner signer = new ECDSASigner(privateKey);
			jwt.sign(signer);
		} catch (JOSEException e) {
			throw error("Failed to sign key binding JWT", e);
		}

		return jwt.serialize();
	}

	protected String createSdJwt(Environment env, JWK publicJWK, ECKey privateKey, String credentialType) {
		JsonElement credentialSigningJwkEl = env.getElementFromObject("config", "credential.signing_jwk");
		if (credentialSigningJwkEl == null) {
			throw error("Credential Signing JWK missing from configuration");
		}
		JWK credentialSigningJwk = null;
		try {
			credentialSigningJwk = JWK.parse(credentialSigningJwkEl.toString());
		} catch (ParseException e) {
			throw error("Failed to create JWK from credential signing_jwk", e, args("signing_jwk", credentialSigningJwkEl));
		}

		// tries to generate a credential that's valid as per https://bmi.usercontent.opencode.de/eudi-wallet/eidas-2.0-architekturkonzept/functions/00-pid-issuance-and-presentation/#pid-contents

		SDObjectBuilder builder = new SDObjectBuilder();
		ArrayList<Disclosure> disclosures = new ArrayList<>();

		builder.putClaim("vct", credentialType);

		switch (credentialType) {
			case "urn:openid:example:certification:1":
				disclosures.add(builder.putSDClaim("product", "Some Product"));
				disclosures.add(builder.putSDClaim("version", "1.2.3"));
				disclosures.add(builder.putSDClaim("issuance_date", LocalDate.now(ZoneId.of("UTC")).toString()));
				break;
			// as per https://github.com/eu-digital-identity-wallet/eudi-doc-attestation-rulebooks-catalog/blob/main/rulebooks/pid/pid-rulebook.md#42-note-on-vct
			case "urn:eudi:pid:1": // fall-through
			default:
			{
				/*
				 * contents as per https://github.com/eu-digital-identity-wallet/eudi-doc-attestation-rulebooks-catalog/blob/main/rulebooks/pid/pid-rulebook.md#41-encoding-of-pid-attributes-and-metadata
				 * mandatory elements are defined here:
				 * https://github.com/eu-digital-identity-wallet/eudi-doc-attestation-rulebooks-catalog/blob/main/rulebooks/pid/pid-rulebook.md#22-mandatory-attributes-specified-in-cir-20242977
				 *
				 * We aim to include all mandatory elements
				 */

				disclosures.add(builder.putSDClaim("given_name", "Jean"));
				disclosures.add(builder.putSDClaim("family_name", "Dupont"));
				disclosures.add(builder.putSDClaim("birthdate", "1980-05-23"));

				Disclosure disclosure0 = new Disclosure("FR");
				disclosures.add(disclosure0);
				Map<String, Object> element = disclosure0.toArrayElement();
				disclosures.add(builder.putSDClaim("nationalities", List.of(element)));

				SDObjectBuilder pobBuilder = new SDObjectBuilder();
				disclosures.add(pobBuilder.putSDClaim("country", "DD"));
				Map<String, Object> placeOfBirth = pobBuilder.build();

				disclosures.add(builder.putSDClaim("place_of_birth", placeOfBirth));
			}
		}


		// Only add cnf claim if cryptographic binding is required (publicJWK is not null)
		if (publicJWK != null) {
			Map<String, Object> cnf = new HashMap<>();
			cnf.put("jwk", publicJWK.toJSONObject());
			builder.putClaim("cnf", cnf);
		}

		builder.putClaim("iat", Instant.now().getEpochSecond());
		builder.putClaim("exp", Instant.now().plus(14, ChronoUnit.DAYS).getEpochSecond());
		String baseUrl = env.getString("base_url");
		builder.putClaim("iss", baseUrl);

		// add support for adding additional claims from env if present
		if (additionalClaims != null) {
			for(var additionalClaim : additionalClaims.entrySet()) {
				builder.putClaim(additionalClaim.getKey(), additionalClaim.getValue());
			}
		}

		builder.putDecoyDigests(3);

		Map<String, Object> claims = builder.build();
		JWSAlgorithm signingAlgorithm = getSigningAlgorithm(credentialSigningJwk);
		JWSHeader.Builder headerBuilder = new JWSHeader.Builder(signingAlgorithm)
			.type(new JOSEObjectType("dc+sd-jwt"));
		if (credentialSigningJwk.getX509CertChain() != null) {
			headerBuilder.x509CertChain(credentialSigningJwk.getX509CertChain());
		}
		JWSHeader header =
			headerBuilder.build();

		JWTClaimsSet claimsSet;
		try {
			claimsSet = JWTClaimsSet.parse(claims);
		} catch (ParseException e) {
			throw error("Failed to parse SD-JWT claims", e);
		}

		SignedJWT jwt = new SignedJWT(header, claimsSet);

		try {
			JWSSignerFactory signerFactory = MultiJWSSignerFactory.getInstance();
			JWSSigner signer = signerFactory.createJWSSigner(credentialSigningJwk, signingAlgorithm);
			jwt.sign(signer);
		} catch (JOSEException e) {
			throw error("Failed to sign SD-JWT credential", e, args("signing_jwk", credentialSigningJwkEl));
		}

		// Filter disclosures to only include claims requested in the DCQL query (data minimization)
		List<Disclosure> filteredDisclosures = filterDisclosuresToDcqlRequest(env, disclosures);

		String bindingJwt = null;

		if (privateKey != null) {
			String aud = env.getString("client", "client_id");
			String sd_hash = null;
			try {
				sd_hash = ValidateSdJwtKbSdHash.getCalculatedSdHash(new SDJWT(jwt.serialize(), filteredDisclosures).toString());
			} catch (NoSuchAlgorithmException e) {
				throw error("Failed to create hash", e);
			}
			String nonce = env.getString("nonce");
			bindingJwt = keyBindingJwt(privateKey, aud, nonce, sd_hash);
		}
		SDJWT sdJwt = new SDJWT(jwt.serialize(), filteredDisclosures, bindingJwt);

		return sdJwt.toString();
	}

	/**
	 * Filter disclosures to only include claims requested in the DCQL query.
	 * If no DCQL query is present, returns all disclosures. If the DCQL credential
	 * query omits claims entirely, returns no selectively-disclosable claims.
	 *
	 * Reachability-based: starts from object property disclosures whose claim name is
	 * requested, then transitively keeps any nested SD claim or array element disclosure
	 * referenced from a kept disclosure's value. Array element disclosures whose parent
	 * was filtered out are dropped, since leaking them without their parent is itself an
	 * over-disclosure.
	 */
	private List<Disclosure> filterDisclosuresToDcqlRequest(Environment env, List<Disclosure> disclosures) {
		JsonObject dcqlQuery = env.getObject(ExtractDCQLQueryFromAuthorizationRequest.ENV_KEY);
		if (dcqlQuery == null) {
			// No DCQL in env: VCI issuance flows have no presentation request, and VPID3 with the
			// Presentation Exchange query language never extracts a DCQL object. Return the full set.
			return disclosures;
		}

		Set<String> requestedClaims = new HashSet<>();
		JsonArray credentials = dcqlQuery.getAsJsonArray("credentials");
		if (credentials != null) {
			for (JsonElement credEl : credentials) {
				JsonArray claimsArray = credEl.getAsJsonObject().getAsJsonArray("claims");
				if (claimsArray != null) {
					for (JsonElement claimEl : claimsArray) {
						JsonArray path = claimEl.getAsJsonObject().getAsJsonArray("path");
						if (path != null && !path.isEmpty() && path.get(0).isJsonPrimitive()) {
							requestedClaims.add(OIDFJSON.getString(path.get(0)));
						}
					}
				}
			}
		}

		if (requestedClaims.isEmpty()) {
			log("DCQL query did not request any claims, omitting all selectively-disclosable disclosures",
				args("total_disclosures", disclosures.size()));
			return List.of();
		}

		Map<String, Disclosure> byDigest = new HashMap<>();
		for (Disclosure d : disclosures) {
			byDigest.put(d.digest(), d);
		}

		Set<Disclosure> kept = new LinkedHashSet<>();
		Deque<Disclosure> toScan = new ArrayDeque<>();
		for (Disclosure d : disclosures) {
			String claimName = d.getClaimName();
			if (claimName != null && requestedClaims.contains(claimName)) {
				kept.add(d);
				toScan.add(d);
			}
		}

		while (!toScan.isEmpty()) {
			Disclosure current = toScan.poll();
			Set<String> referencedDigests = new HashSet<>();
			collectReferencedDigests(current.getClaimValue(), referencedDigests);
			for (String digest : referencedDigests) {
				Disclosure child = byDigest.get(digest);
				if (child != null && kept.add(child)) {
					toScan.add(child);
				}
			}
		}

		List<Disclosure> filtered = new ArrayList<>();
		for (Disclosure d : disclosures) {
			if (kept.contains(d)) {
				filtered.add(d);
			}
		}

		log("Filtered SD-JWT disclosures to DCQL requested claims",
			args("requested_claims", requestedClaims,
				"total_disclosures", disclosures.size(),
				"filtered_disclosures", filtered.size()));

		return filtered;
	}

	@SuppressWarnings("unchecked")
	private static void collectReferencedDigests(Object value, Set<String> digests) {
		if (value instanceof Map<?, ?> map) {
			for (Map.Entry<?, ?> entry : map.entrySet()) {
				Object key = entry.getKey();
				Object v = entry.getValue();
				if ("_sd".equals(key) && v instanceof List<?> list) {
					for (Object item : list) {
						if (item instanceof String s) {
							digests.add(s);
						}
					}
				} else if ("...".equals(key) && v instanceof String s) {
					digests.add(s);
				} else {
					collectReferencedDigests(v, digests);
				}
			}
		} else if (value instanceof List<?> list) {
			for (Object item : list) {
				collectReferencedDigests(item, digests);
			}
		}
	}

	private JWSAlgorithm getSigningAlgorithm(JWK signingJwk) {
		if (signingJwk.getAlgorithm() != null) {
			return JWSAlgorithm.parse(signingJwk.getAlgorithm().getName());
		}

		// Keep historical behavior for EC signing keys if alg is omitted.
		if (signingJwk instanceof ECKey) {
			return JWSAlgorithm.ES256;
		}

		throw error("No signing algorithm specified in credential.signing_jwk",
			args("kty", signingJwk.getKeyType().getValue(), "kid", signingJwk.getKeyID()));
	}
}
