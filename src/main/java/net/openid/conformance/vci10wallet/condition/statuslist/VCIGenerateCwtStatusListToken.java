package net.openid.conformance.vci10wallet.condition.statuslist;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.oauth.statuslists.CwtStatusListTokenBuilder;
import net.openid.conformance.oauth.statuslists.EvenOddStatusListContents;
import net.openid.conformance.oauth.statuslists.TokenStatusList;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.util.TestKeysAndCerts;
import org.multipaz.crypto.Algorithm;
import org.multipaz.crypto.AsymmetricKey;

import java.time.Instant;
import java.util.Base64;
import java.util.concurrent.TimeUnit;

/**
 * Generates the emulated credential issuer's Token Status List as a Status List Token in CWT
 * format, the representation ISO/IEC 18013-5 12.3.6.3 requires for an MSO revocation list.
 *
 * <p>Signed with the suite's MSO revocation list signer key, whose certificate is issued by the
 * same IACA root as the document signer certificate in the issued mdocs' x5chain. That is what
 * 12.3.6.2 requires when — as here — the MSO's status element carries no Certificate element: a
 * wallet that trusts the suite's IACA root can verify the revocation list with no further
 * configuration. The signing JWK in the test configuration signs SD-JWT VCs (and the JWT format
 * status list that goes with them), which is a separate trust chain.
 *
 * <p>The list contents are the same as {@link VCIGenerateJwtStatusListToken} produces; only the
 * representation differs, see {@link CwtStatusListTokenBuilder}.
 *
 * <p>Stores the token base64 encoded in {@code current_status_list_cwt}.
 */
public class VCIGenerateCwtStatusListToken extends AbstractCondition {

	public static final String STATUS_LIST_CWT_CONTENT_TYPE =
		CwtStatusListTokenBuilder.STATUS_LIST_CWT_CONTENT_TYPE;

	@Override
	@PreEnvironment(strings = { "current_status_list_id" })
	@PostEnvironment(strings = { "current_status_list_cwt" })
	public Environment evaluate(Environment env) {

		String currentStatusListId = env.getString("current_status_list_id");
		String issuerUrl = env.getString("server", "issuer");
		String currentStatusListUri = issuerUrl + "statuslists/" + currentStatusListId;

		TokenStatusList statusList = EvenOddStatusListContents.create();
		byte[] compressedStatusList = Base64.getUrlDecoder().decode(statusList.encodeStatusList());

		Instant iat = Instant.now();
		// ISO/IEC 18013-5 12.3.6.3 requires the exp claim to be present
		Instant exp = iat.plusSeconds(10 * 60);

		AsymmetricKey.X509CertifiedExplicit signingKey = TestKeysAndCerts.getStatusListSignerKey();

		// draft-ietf-oauth-status-list section 4.3: optional pointer to the Status List
		// Aggregation this issuer serves (section 9.3)
		String aggregationUri = env.getString("server", "status_list_aggregation_endpoint");

		byte[] token;
		try {
			token = CwtStatusListTokenBuilder.build(currentStatusListUri, iat, exp,
				TimeUnit.MINUTES.toSeconds(12), EvenOddStatusListContents.BITS, compressedStatusList,
				signingKey, Algorithm.ES256, signingKey.getCertChain(), aggregationUri);
		} catch (Exception e) {
			throw error("Failed to sign the status list token in CWT format", e);
		}

		env.putString("current_status_list_cwt", Base64.getEncoder().encodeToString(token));

		logSuccess("Generated the Status List Token in CWT format",
			args("sub", currentStatusListUri,
				"algorithm", Algorithm.ES256.name(),
				"exp", exp.getEpochSecond(),
				"length", token.length));
		return env;
	}
}
