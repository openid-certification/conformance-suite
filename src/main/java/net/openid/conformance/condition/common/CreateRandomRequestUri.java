package net.openid.conformance.condition.common;

import com.google.common.base.Strings;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import org.apache.commons.lang3.RandomStringUtils;

/**
 * Creates a URL to retrieve a request object from based on the base_url environment value
 */
public class CreateRandomRequestUri extends AbstractCondition {

//	private String base64UrlEncodedSha256(String text) {
//		MessageDigest md = null;
//		try {
//			md = MessageDigest.getInstance("SHA-256");
//		} catch (NoSuchAlgorithmException e) {
//			throw new RuntimeException("MessageDigest.getInstance(\"SHA-256\") threw NoSuchAlgorithmException", e);
//		}
//		byte[] digest = md.digest(text.getBytes(StandardCharsets.US_ASCII));
//
//		return Base64.encodeBase64URLSafeString(digest);
//	}

	@Override
	@PreEnvironment(strings = "base_url")
	@PostEnvironment(required = "request_uri", strings = "request_uri")
	public Environment evaluate(Environment env) {
		String baseUrl = env.getString("base_url");

		if (baseUrl.isEmpty()) {
			throw error("Base URL is empty");
		}

		// see https://gitlab.com/openid/conformance-suite/wikis/Developers/Build-&-Run#ciba-notification-endpoint
		String externalUrlOverride = env.getString("external_url_override");
		if (!Strings.isNullOrEmpty(externalUrlOverride)) {
			baseUrl = externalUrlOverride;
		}

		// create a random URL
		//
		// - spec requires full url to be no more than 512 characters
		//
		// - spec does not have any obvious restriction on character set (random alphanumeric used for consistency with
		// python suite)
		//
		// - spec allows a fragment: "If the contents of the referenced resource could ever change, the URI SHOULD
		// include the base64url encoded SHA-256 hash of the referenced resource contents as the fragment component
		// of the URI"; we don't include one for consistency with python
		//
		// spec says "As such, the request_uri MUST have appropriate entropy for its lifetime"; python used 8 characters
		// which is ~48 bits of entropy which is not very much
		//
		// (64 is a relatively arbitrary choice that lies between ~21 characters having a reasonable amount of entropy
		// and the 512 byte upper limit, and appears to match what python does. The spec says clients mustn't use more
		// than 512, but doesn't say servers have to support 512.)
		String path = "requesturi/" + RandomStringUtils.randomAlphanumeric(96); // FIXME hack if I have it shorter than this then the QR code generation fails on demo - see if there's a later version of qrcode library maybe?

		// actual content of request object not used as it's not available prior to client registration
//		String fragment = base64UrlEncodedSha256(RandomStringUtils.randomAlphanumeric(64));
// FIXME remove fragment at least for VCI; it's only in OIDC, not in JAR
		JsonObject o = new JsonObject();
		o.addProperty("path", path);
		String fullUrl = baseUrl + "/" + path;
		o.addProperty("fullUrl", fullUrl);

		env.putObject("request_uri", o);
		env.putString("request_uri", fullUrl);

		log("Created random URL for request_uri",
			args("request_uri", o));

		return env;
	}

}
