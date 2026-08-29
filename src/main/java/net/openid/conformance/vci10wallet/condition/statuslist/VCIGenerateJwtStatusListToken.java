package net.openid.conformance.vci10wallet.condition.statuslist;

import com.google.gson.JsonObject;
import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jwt.JWTClaimsSet;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.condition.client.AbstractSignJWT;
import net.openid.conformance.oauth.statuslists.EvenOddStatusListContents;
import net.openid.conformance.oauth.statuslists.TokenStatusList;
import net.openid.conformance.testmodule.Environment;

import java.time.Instant;
import java.util.concurrent.TimeUnit;

public class VCIGenerateJwtStatusListToken extends AbstractSignJWT {

	@Override
	@PreEnvironment(required = {"vci", "config"}, strings = {"current_status_list_id"})
	@PostEnvironment(strings = {"current_status_list_jwt"})
	public Environment evaluate(Environment env) {

		String currentStatusListId = env.getString("current_status_list_id");
		int bits = EvenOddStatusListContents.BITS;

		TokenStatusList statusList = EvenOddStatusListContents.create();
		String encodedStatusList = statusList.encodeStatusList();

		String issuerUrl = env.getString("server", "issuer");

		String currentStatusListUri = issuerUrl + "statuslists/" + currentStatusListId;

		Instant iat = Instant.now();
		Instant exp = iat.plusSeconds(10 * 60);

		// Example taken from https://datatracker.ietf.org/doc/html/draft-ietf-oauth-status-list-12#section-4.2

		JsonObject claims = new JsonObject();
		claims.addProperty("sub", currentStatusListUri);
		claims.addProperty("iat", iat.getEpochSecond());
		claims.addProperty("exp", exp.getEpochSecond());
		claims.addProperty("ttl", TimeUnit.MINUTES.toSeconds(12));

		JsonObject statusListObject = new JsonObject();
		statusListObject.addProperty("bits", bits);
		statusListObject.addProperty("lst", encodedStatusList);
		// draft-ietf-oauth-status-list section 4.2: optional pointer to the Status List
		// Aggregation this issuer serves (section 9.3)
		String aggregationUri = env.getString("server", "status_list_aggregation_endpoint");
		if (aggregationUri != null) {
			statusListObject.addProperty("aggregation_uri", aggregationUri);
		}
		claims.add("status_list", statusListObject);

		// TODO clarify, which keys shall we use here?
		// see: https://datatracker.ietf.org/doc/html/draft-ietf-oauth-status-list-12#section-11.3
		JsonObject jwks = env.getObject("server_jwks");

		signJWT(env, claims, jwks, true, false, true, true);

		return env;
	}

	@Override
	protected JOSEObjectType getMediaType() {
		return new JOSEObjectType("statuslist+jwt");
	}

	@Override
	protected void logSuccessByJWTType(Environment env, JWTClaimsSet claimSet, JWK jwk, JWSHeader header, String jws, JsonObject verifiableObj) {
		env.putString("current_status_list_jwt", jws);
		logSuccess("Generated the Status List JWT", args("status_list", verifiableObj));
	}
}
