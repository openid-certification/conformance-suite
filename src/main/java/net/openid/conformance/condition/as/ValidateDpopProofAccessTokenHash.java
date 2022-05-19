package net.openid.conformance.condition.as;

import com.google.common.base.Strings;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import org.apache.commons.codec.binary.Base64;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class ValidateDpopProofAccessTokenHash extends AbstractCondition {

	@Override
	@PreEnvironment(required = "incoming_dpop_proof", strings = "incoming_dpop_access_token" )
	public Environment evaluate(Environment env) {

		String dpopAccessToken = env.getString("incoming_dpop_access_token");
		String ath = env.getString("incoming_dpop_proof", "claims.ath");

		if(Strings.isNullOrEmpty(dpopAccessToken)) {
			throw error("DPoP Access Token is not available.");
		}
		if(Strings.isNullOrEmpty(ath)) {
			throw error("DPoP Proof 'ath' claim is not available.");
		}
		byte[] bytes = dpopAccessToken.getBytes(StandardCharsets.US_ASCII);
		MessageDigest md = null;
		try {
			md = MessageDigest.getInstance("SHA-256");
		} catch (NoSuchAlgorithmException e) {
			throw error("No such Algorithm Error",e);
		}
		md.update(bytes, 0, bytes.length);
		byte[] digest = md.digest();
		String expectedAth = Base64.encodeBase64URLSafeString(digest);

		if(!ath.equals(expectedAth)) {
			throw error("Mismatch between DPoP Proof ath and access token", args("incoming_dpop_access_token", dpopAccessToken, "expected", expectedAth, "actual", ath));
		}
		logSuccess("DPoP Proof ath claim matches DPoP access token hash", args("ath", ath));
		return env;

	}

}
