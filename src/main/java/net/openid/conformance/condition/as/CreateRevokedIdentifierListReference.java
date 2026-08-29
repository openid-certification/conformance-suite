package net.openid.conformance.condition.as;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import org.apache.commons.lang3.RandomStringUtils;

import java.security.SecureRandom;
import java.util.Base64;

/**
 * Allocates the identifier_list reference (ISO/IEC 18013-5 12.3.6.2) for an mdoc the test suite is
 * about to create and present, pointing at the identifier list this test instance serves.
 *
 * <p>The Identifier is random, as 12.3.6.4 suggests so that it cannot be used to correlate
 * presentations. {@link VP1FinalGenerateIdentifierListToken} then puts it on the served list,
 * i.e. the presented credential is revoked.
 *
 * <p>The reference is stored in {@code identifier_list_reference} as the URI plus the base64
 * encoded Identifier; {@link CreateMdocCredential} turns it into the MSO's status element.
 *
 * <p>The URI ends in a random path segment, for the reason given on
 * {@link AbstractCreateStatusListReference}: a verifier that cached the list an earlier run
 * served would not find this run's identifier on it.
 */
public class CreateRevokedIdentifierListReference extends AbstractCondition {

	/**
	 * Prefix of the path, relative to the test instance's base url, that the identifier list is
	 * served from; the random segment that follows it is allocated per test instance.
	 */
	public static final String IDENTIFIER_LIST_PATH_PREFIX = "identifierlists/";

	public static final String ENV_KEY = "identifier_list_reference";

	/** Length of the allocated Identifier; 16 random bytes is well beyond any collision concern. */
	private static final int IDENTIFIER_LENGTH = 16;

	@Override
	@PreEnvironment(strings = "base_url")
	@PostEnvironment(required = ENV_KEY)
	public Environment evaluate(Environment env) {

		byte[] identifier = new byte[IDENTIFIER_LENGTH];
		new SecureRandom().nextBytes(identifier);

		String path = IDENTIFIER_LIST_PATH_PREFIX + RandomStringUtils.secure().nextAlphanumeric(20);

		JsonObject reference = new JsonObject();
		reference.addProperty("path", path);
		reference.addProperty("uri", env.getString("base_url") + "/" + path);
		reference.addProperty("id", Base64.getEncoder().encodeToString(identifier));
		reference.addProperty("revoked", true);
		env.putObject(ENV_KEY, reference);

		logSuccess("Allocated an identifier the served identifier list marks as revoked",
			args("identifier_list_reference", reference));

		return env;
	}
}
