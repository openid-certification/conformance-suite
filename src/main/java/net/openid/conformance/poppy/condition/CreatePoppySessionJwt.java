package net.openid.conformance.poppy.condition;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.ECDSASigner;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.poppy.PoppyConstants;
import net.openid.conformance.testmodule.Environment;
import org.apache.commons.lang3.RandomStringUtils;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.ParseException;
import java.util.Base64;
import java.util.Date;

public class CreatePoppySessionJwt extends AbstractCondition {

	@Override
	@PreEnvironment(required = {"poppy_delegated_key"}, strings = {"client_id", "poppy_issuer", "poppy_sid", "poppy_user_id",
		"poppy_delegation", "protected_resource_url", "poppy_resource_request_method"})
	@PostEnvironment(strings = "poppy_session_jwt")
	public Environment evaluate(Environment env) {
		ECKey key;
		try {
			key = ECKey.parse(env.getObject("poppy_delegated_key").toString());
		} catch (ParseException e) {
			throw error("The delegated key is not a valid EC JWK", e);
		}

		String htu = stripQueryAndFragment(env.getString("protected_resource_url"));

		JWTClaimsSet.Builder claims = new JWTClaimsSet.Builder()
			.issuer(env.getString("client_id"))
			.audience(env.getString("poppy_issuer"))
			.claim("sid", env.getString("poppy_sid"))
			.subject(env.getString("poppy_user_id"))
			.claim("htm", env.getString("poppy_resource_request_method"))
			.claim("htu", htu)
			.issueTime(new Date())
			.jwtID(RandomStringUtils.secure().nextAlphanumeric(24));

		String accessToken = env.getString("access_token", "value");
		if (accessToken != null) {
			claims.claim("ath", athOf(accessToken));
		}

		JWSHeader header = new JWSHeader.Builder(JWSAlgorithm.ES256)
			.type(new JOSEObjectType(PoppyConstants.SESSION_JWT_TYP))
			.keyID(key.getKeyID())
			.customParam("poppy_dlg", env.getString("poppy_delegation"))
			.build();

		JWTClaimsSet claimsSet = claims.build();
		SignedJWT jwt = new SignedJWT(header, claimsSet);
		try {
			jwt.sign(new ECDSASigner(key));
		} catch (JOSEException e) {
			throw error("Failed to sign the Session JWT", e);
		}

		String serialized = jwt.serialize();
		env.putString("poppy_session_jwt", serialized);

		logSuccess("Created the Session JWT", args("session_jwt", serialized, "claims", claimsSet.toJSONObject()));
		return env;
	}

	static String athOf(String token) {
		try {
			byte[] digest = MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.US_ASCII));
			return Base64.getUrlEncoder().withoutPadding().encodeToString(digest);
		} catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException(e);
		}
	}

	private String stripQueryAndFragment(String url) {
		try {
			URI uri = new URI(url);
			return new URI(uri.getScheme(), uri.getRawAuthority(), uri.getPath(), null, null).toString();
		} catch (URISyntaxException e) {
			throw error("protected_resource_url is not a valid URL", e, args("url", url));
		}
	}
}
