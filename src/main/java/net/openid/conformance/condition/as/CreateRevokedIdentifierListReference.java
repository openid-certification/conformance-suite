package net.openid.conformance.condition.as;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.testmodule.Environment;

import java.security.SecureRandom;
import java.util.Base64;

/**
 * Allocates the identifier_list reference (ISO/IEC 18013-5 12.3.6.2) for an mdoc the test suite is
 * about to create and present, pointing at the identifier list this test instance serves.
 *
 * <p>12.3.6.4 recommends the Identifier be unique per MSO so it cannot be used to correlate
 * presentations, "this could be achieved by using a random identifier per MSO", which is what is
 * allocated here. {@link VP1FinalGenerateIdentifierListToken} then puts this identifier on the
 * served list, i.e. the presented credential is revoked.
 *
 * <p>The reference is stored in {@code identifier_list_reference} as the URI plus the base64
 * encoded Identifier; {@link CreateMdocCredential} turns it into the MSO's status element.
 */
public class CreateRevokedIdentifierListReference extends AbstractCondition {

	/** Path, relative to the test instance's base url, that the identifier list is served from. */
	public static final String IDENTIFIER_LIST_PATH = "identifierlists/1";

	public static final String ENV_KEY = "identifier_list_reference";

	/** Length of the allocated Identifier; 16 random bytes is well beyond any collision concern. */
	private static final int IDENTIFIER_LENGTH = 16;

	private final SecureRandom random = new SecureRandom();

	@Override
	@PostEnvironment(required = ENV_KEY)
	public Environment evaluate(Environment env) {

		String baseUrl = env.getString("base_url");
		if (baseUrl == null) {
			throw error("The test instance has no base url, so the identifier list the credential"
				+ " references cannot be published");
		}

		byte[] identifier = new byte[IDENTIFIER_LENGTH];
		random.nextBytes(identifier);

		String uri = baseUrl + "/" + IDENTIFIER_LIST_PATH;

		JsonObject reference = new JsonObject();
		reference.addProperty("uri", uri);
		reference.addProperty("id", Base64.getEncoder().encodeToString(identifier));
		env.putObject(ENV_KEY, reference);

		logSuccess("Allocated an identifier the served identifier list marks as revoked",
			args("uri", uri,
				"id", Base64.getUrlEncoder().withoutPadding().encodeToString(identifier)));

		return env;
	}
}
