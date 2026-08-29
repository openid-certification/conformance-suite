package net.openid.conformance.condition.as;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

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
 * <p>The reference is stored as a {@link RevocationListReference};
 * {@link CreateMdocCredential} turns it into the MSO's status element.
 */
public class CreateRevokedIdentifierListReference extends AbstractCondition {

	/** Length of the allocated Identifier; 16 random bytes is well beyond any collision concern. */
	private static final int IDENTIFIER_LENGTH = 16;

	@Override
	@PreEnvironment(strings = "base_url")
	@PostEnvironment(required = RevocationListReference.ENV_KEY)
	public Environment evaluate(Environment env) {

		byte[] identifier = new byte[IDENTIFIER_LENGTH];
		new SecureRandom().nextBytes(identifier);

		JsonObject reference = RevocationListReference.allocate(env,
			RevocationListReference.MECHANISM_IDENTIFIER_LIST,
			RevocationListReference.IDENTIFIER_LIST_PATH_PREFIX);
		reference.addProperty("id", Base64.getEncoder().encodeToString(identifier));
		env.putObject(RevocationListReference.ENV_KEY, reference);

		logSuccess("Allocated an identifier the served identifier list marks as revoked",
			args("revocation_list_reference", reference));

		return env;
	}
}
