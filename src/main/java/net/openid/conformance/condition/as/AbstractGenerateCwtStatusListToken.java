package net.openid.conformance.condition.as;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.oauth.statuslists.CwtStatusListTokenBuilder;
import net.openid.conformance.oauth.statuslists.EvenOddStatusListContents;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.util.TestKeysAndCerts;
import org.multipaz.crypto.Algorithm;
import org.multipaz.crypto.AsymmetricKey;

import java.time.Duration;
import java.time.Instant;
import java.util.Base64;

/**
 * Generates the Token Status List the test suite serves as a Status List Token in CWT format,
 * the representation ISO/IEC 18013-5 12.3.6.3 requires for an MSO revocation list.
 *
 * <p>Signed with the suite's MSO revocation list signer key, whose certificate is issued by the
 * same IACA root as the document signer certificate of the mdocs the suite creates. That is what
 * 12.3.6.2 requires when — as here — the MSO's status element carries no Certificate element: a
 * party that trusts the suite's IACA root can verify the revocation list with no further
 * configuration.
 *
 * <p>Subclasses say where the list is published, how long it lives and where the token goes.
 */
public abstract class AbstractGenerateCwtStatusListToken extends AbstractCondition {

	/**
	 * @param envKey the environment string the token is stored in, base64 encoded
	 * @param uri the URI the status list is published at, used as the {@code sub} claim
	 * @param lifetime how long after its issuance the token expires
	 * @param ttl the {@code ttl} claim
	 * @param aggregationUri the optional aggregation_uri element of the status_list claim
	 *   (draft-ietf-oauth-status-list section 4.3), or null to omit it
	 */
	protected void generateStatusListToken(Environment env, String envKey, String uri,
			Duration lifetime, Duration ttl, String aggregationUri) {

		byte[] compressedStatusList = EvenOddStatusListContents.create().compressStatusList();

		Instant iat = Instant.now();
		// ISO/IEC 18013-5 12.3.6.3 requires the exp claim to be present
		Instant exp = iat.plus(lifetime);

		AsymmetricKey.X509CertifiedExplicit signingKey = TestKeysAndCerts.getStatusListSignerKey();

		byte[] token;
		try {
			token = CwtStatusListTokenBuilder.build(uri, iat, exp, ttl.toSeconds(),
				EvenOddStatusListContents.BITS, compressedStatusList, signingKey, Algorithm.ES256,
				aggregationUri);
		} catch (Exception e) {
			throw error("Failed to sign the status list token in CWT format", e);
		}

		env.putString(envKey, Base64.getEncoder().encodeToString(token));

		logSuccess("Generated the Status List Token in CWT format",
			args("sub", uri,
				"algorithm", Algorithm.ES256.name(),
				"exp", exp.getEpochSecond(),
				"length", token.length));
	}
}
