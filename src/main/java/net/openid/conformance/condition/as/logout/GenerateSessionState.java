package net.openid.conformance.condition.as.logout;

import com.nimbusds.jose.util.Base64URL;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.condition.as.CreateEffectiveAuthorizationRequestParameters;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.util.JWAUtil;
import org.apache.commons.lang3.RandomStringUtils;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Locale;

/**
 * https://openid.net/specs/openid-connect-session-1_0.html#CreatingUpdatingSessions
 * 		The Session State value is initially calculated on the server. The same Session State value
 * 		is also recalculated by the OP iframe in the browser client. The generation of suitable
 * 		Session State values is specified in Section 4.2, and is based on a salted cryptographic
 * 		hash of Client ID, origin URL, and OP browser state. For the origin URL, the server can
 * 		use the origin URL of the Authentication Response, following the algorithm specified in
 * 		Section 4 of RFC 6454 [RFC6454].
 *
 * This class generates it as follows:
 * base64url_encode(sha256_hash(client_id + ' ' + origin + ' ' + random_state_value + ' ' + random_salt))
 */
public class GenerateSessionState extends AbstractCondition {

	@Override
	@PreEnvironment(required = {"client", CreateEffectiveAuthorizationRequestParameters.ENV_KEY})
	@PostEnvironment(strings = { "session_state", "session_state_salt", "op_browser_state"})
	public Environment evaluate(Environment env) {

		String salt = RandomStringUtils.randomAlphanumeric(50);
		//this is actually a session id but the spec calls it "OP browser state"
		String opBrowserState = RandomStringUtils.randomAlphanumeric(50);
		String sessionState;
		String clientId = env.getString("client", "client_id");
		String origin = getOrigin(env.getString(CreateEffectiveAuthorizationRequestParameters.ENV_KEY, CreateEffectiveAuthorizationRequestParameters.REDIRECT_URI));
		MessageDigest digester;
		try {
			digester = MessageDigest.getInstance("SHA-256");
			String stringToHash = clientId + " " + origin + " " + opBrowserState + " " + salt;
			byte[] digestBytes = digester.digest(stringToHash.getBytes(StandardCharsets.UTF_8));
			sessionState = Base64URL.encode(digestBytes) + "." + salt;
		} catch (NoSuchAlgorithmException e) {
			throw error("Unsupported digest algorithm", e);
		}

		log("Generated session_state", args("session_state_salt", salt, "session_state", sessionState,
												"origin", origin, "op_browser_state", opBrowserState));

		env.putString("op_browser_state", opBrowserState);
		env.putString("session_state", sessionState);
		env.putString("session_state_salt", salt);
		return env;

	}

	protected String getOrigin(String redirectUri) {
		try {
			URI uri = new URI(redirectUri);
			String origin = uri.getScheme().toLowerCase() + "://" + uri.getHost().toLowerCase(Locale.ENGLISH);
			if(uri.getPort()==-1) {
				if("http".equalsIgnoreCase(uri.getScheme())) {
					origin += ":80";
				} else {
					origin +=":443";
				}
			} else {
				origin +=":" + uri.getPort();
			}
			return origin;
		} catch (URISyntaxException e) {
			throw error("Unable to extract origin from redirect_uri, invalid redirect_uri", args("redirect_uri", redirectUri));
		}
	}

}
