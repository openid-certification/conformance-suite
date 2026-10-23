package net.openid.conformance.vci10wallet.condition.statuslist;

import com.google.gson.JsonObject;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSSigner;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.condition.as.AbstractCredentialSigningJwkCondition;
import net.openid.conformance.extensions.MultiJWSSignerFactory;
import net.openid.conformance.fapi2spfinal.VCIClientProfileBehavior;
import net.openid.conformance.oauth.statuslists.EvenOddStatusListContents;
import net.openid.conformance.oauth.statuslists.JwtStatusListTokenClaimsBuilder;
import net.openid.conformance.testmodule.Environment;

import java.text.ParseException;
import java.time.Instant;
import java.util.concurrent.TimeUnit;

/**
 * Generates the emulated credential issuer's Status List Token in JWT format for the status list
 * its SD-JWT VC credentials reference (draft-ietf-oauth-status-list section 5.1).
 *
 * <p>It is signed with the same key as the credentials — the 'Signing JWK' of the 'Credential
 * Issuer' section of the test configuration, or the suite's built-in PID provider certificate —
 * with that key's certificate chain in {@code x5c}, so a wallet that trusts the issuer of the
 * credential verifies the status list without further configuration: the built-in signer's CA is
 * listed under the revocation services of the suite's PID providers list as well as its issuance
 * services (section 11.3 wants the status list's trust resolved like the referenced token's).
 *
 * <p>Stores the token in {@code current_status_list_jwt}.
 */
public class VCIGenerateJwtStatusListToken extends AbstractCredentialSigningJwkCondition {

	@Override
	@PreEnvironment(required = {"vci", "config"}, strings = {"current_status_list_id"})
	@PostEnvironment(strings = {"current_status_list_jwt"})
	public Environment evaluate(Environment env) {

		String currentStatusListId = env.getString("current_status_list_id");
		String currentStatusListUri =
			VCIClientProfileBehavior.getStatusListUrl(env, currentStatusListId);

		Instant iat = Instant.now();
		Instant exp = iat.plusSeconds(10 * 60);

		// draft-ietf-oauth-status-list section 4.2: optional pointer to the Status List
		// Aggregation this issuer serves (section 9.3)
		String aggregationUri = env.getString("server", "status_list_aggregation_endpoint");

		JsonObject claims = JwtStatusListTokenClaimsBuilder.build(currentStatusListUri, iat, exp,
			TimeUnit.MINUTES.toSeconds(12), EvenOddStatusListContents.BITS,
			EvenOddStatusListContents.create().encodeStatusList(), aggregationUri);

		JWK signingJwk = credentialSigningJwk(env);
		JWSAlgorithm alg = signingAlgorithm(signingJwk);

		JWSHeader.Builder headerBuilder = new JWSHeader.Builder(alg)
			.type(new JOSEObjectType(JwtStatusListTokenClaimsBuilder.TYP))
			.keyID(signingJwk.getKeyID());
		if (signingJwk.getX509CertChain() != null) {
			headerBuilder.x509CertChain(signingJwk.getX509CertChain());
		}

		String token;
		try {
			SignedJWT jwt = new SignedJWT(headerBuilder.build(), JWTClaimsSet.parse(claims.toString()));
			JWSSigner signer = MultiJWSSignerFactory.getInstance().createJWSSigner(signingJwk, alg);
			jwt.sign(signer);
			token = jwt.serialize();
		} catch (ParseException | JOSEException e) {
			throw error("Failed to sign the status list token", e,
				args("alg", alg.getName(), "kid", signingJwk.getKeyID()));
		}

		env.putString("current_status_list_jwt", token);
		logSuccess("Generated the Status List JWT", args("status_list", claims, "status_list_token", token,
			"algorithm", alg.getName(), "signing_certificate_in_x5c", signingJwk.getX509CertChain() != null));

		return env;
	}
}
