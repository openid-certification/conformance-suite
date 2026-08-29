package net.openid.conformance.condition.as;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.oauth.statuslists.CwtIdentifierListTokenBuilder;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;
import net.openid.conformance.util.TestKeysAndCerts;
import org.multipaz.crypto.Algorithm;
import org.multipaz.crypto.AsymmetricKey;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Generates the MSO revocation list for the mdoc the emulated wallet presents when the MSO uses
 * the identifier list mechanism: the identifier list in CWT format that ISO/IEC 18013-5 12.3.6.4
 * requires, signed with the suite's MSO revocation list signer key.
 *
 * <p>That key's certificate is issued by the same IACA root as the mdoc's document signer
 * certificate, which is what 12.3.6.2 requires when - as here - the MSO's status element carries
 * no Certificate element: a verifier that trusts the suite's IACA root can verify the revocation
 * list with no further configuration.
 *
 * <p>The list contains the identifier {@link CreateRevokedIdentifierListReference} allocated, so
 * the presented credential is revoked, plus decoy identifiers so a verifier cannot pass by
 * treating any non-empty list as a match.
 *
 * <p>Stores the token base64 encoded in {@code served_identifier_list_cwt}.
 */
public class VP1FinalGenerateIdentifierListToken extends AbstractCondition {

	public static final String IDENTIFIER_LIST_CWT_CONTENT_TYPE =
		CwtIdentifierListTokenBuilder.IDENTIFIER_LIST_CWT_CONTENT_TYPE;

	public static final String ENV_KEY = "served_identifier_list_cwt";

	private static final int DECOY_IDENTIFIERS = 3;

	private final SecureRandom random = new SecureRandom();

	@Override
	@PreEnvironment(required = { CreateRevokedIdentifierListReference.ENV_KEY })
	@PostEnvironment(strings = { ENV_KEY })
	public Environment evaluate(Environment env) {

		String uri = OIDFJSON.getString(
			env.getElementFromObject(CreateRevokedIdentifierListReference.ENV_KEY, "uri"));
		byte[] identifier = Base64.getDecoder().decode(OIDFJSON.getString(
			env.getElementFromObject(CreateRevokedIdentifierListReference.ENV_KEY, "id")));

		List<byte[]> identifiers = new ArrayList<>();
		identifiers.add(identifier);
		for (int i = 0; i < DECOY_IDENTIFIERS; i++) {
			byte[] decoy = new byte[identifier.length];
			random.nextBytes(decoy);
			identifiers.add(decoy);
		}

		Instant iat = Instant.now();
		// ISO/IEC 18013-5 12.3.6.3 requires the exp claim to be present
		Instant exp = iat.plusSeconds(10 * 60);

		AsymmetricKey.X509CertifiedExplicit signingKey = TestKeysAndCerts.getStatusListSignerKey();

		byte[] token;
		try {
			token = CwtIdentifierListTokenBuilder.build(uri, iat, exp, TimeUnit.MINUTES.toSeconds(12),
				identifiers, signingKey, Algorithm.ES256, signingKey.getCertChain());
		} catch (Exception e) {
			throw error("Failed to sign the MSO revocation list in identifier list CWT format", e);
		}

		env.putString(ENV_KEY, Base64.getEncoder().encodeToString(token));

		logSuccess("Generated the MSO revocation list as an identifier list in CWT format",
			args("sub", uri, "algorithm", Algorithm.ES256.name(), "exp", exp.getEpochSecond(),
				"identifiers", identifiers.size(), "length", token.length));

		return env;
	}
}
