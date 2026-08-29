package net.openid.conformance.condition.as;

import com.google.gson.JsonObject;
import net.openid.conformance.testmodule.Environment;

/**
 * The revocation list this test instance serves at the URI in {@link RevocationListReference},
 * stored in {@code served_revocation_list} by the condition that generates it: the signed
 * {@code token}, whether that is {@code base64_encoded} (a CWT is, a JWT is stored as it is),
 * the {@code content_type} it is served as and a {@code description} for log messages.
 */
public final class ServedRevocationList {

	public static final String ENV_KEY = "served_revocation_list";

	private ServedRevocationList() {
	}

	static void store(Environment env, String description, String contentType, String token,
			boolean base64Encoded) {
		JsonObject served = new JsonObject();
		served.addProperty("description", description);
		served.addProperty("content_type", contentType);
		served.addProperty("token", token);
		served.addProperty("base64_encoded", base64Encoded);
		env.putObject(ENV_KEY, served);
	}
}
