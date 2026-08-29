package net.openid.conformance.condition.as;

import com.google.gson.JsonObject;
import net.openid.conformance.testmodule.Environment;

/**
 * The revocation list this test instance serves at the URI in {@link RevocationListReference},
 * stored in {@code served_revocation_list} by the condition that generates it: the signed
 * {@code token}, base64 encoded whichever representation it is in, the {@code content_type} it
 * is served as and a {@code description} for log messages.
 */
public final class ServedRevocationList {

	public static final String ENV_KEY = "served_revocation_list";

	private ServedRevocationList() {
	}

	static void store(Environment env, String description, String contentType, String tokenBase64) {
		JsonObject served = new JsonObject();
		served.addProperty("description", description);
		served.addProperty("content_type", contentType);
		served.addProperty("token", tokenBase64);
		env.putObject(ENV_KEY, served);
	}
}
