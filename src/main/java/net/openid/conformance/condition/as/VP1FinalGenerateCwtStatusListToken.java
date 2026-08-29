package net.openid.conformance.condition.as;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.oauth.statuslists.CwtStatusListTokenBuilder;
import net.openid.conformance.oauth.statuslists.EvenOddStatusListContents;
import net.openid.conformance.oauth.statuslists.TokenStatusList;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;
import net.openid.conformance.util.TestKeysAndCerts;
import org.multipaz.crypto.Algorithm;
import org.multipaz.crypto.AsymmetricKey;

import java.time.Instant;
import java.util.Base64;
import java.util.concurrent.TimeUnit;

/**
 * Generates the MSO revocation list for the mdoc the emulated wallet presents: the Status List
 * Token in CWT format that ISO/IEC 18013-5 12.3.6.3 requires, signed with the suite's MSO
 * revocation list signer key.
 *
 * <p>That key's certificate is issued by the same IACA root as the mdoc's document signer
 * certificate, which is what 12.3.6.2 requires when — as here — the MSO's status element carries
 * no Certificate element: a verifier that trusts the suite's IACA root can verify the revocation
 * list with no further configuration.
 *
 * <p>Stores the token base64 encoded in {@code served_status_list_cwt}.
 */
public class VP1FinalGenerateCwtStatusListToken extends AbstractCondition {

	public static final String STATUS_LIST_CWT_CONTENT_TYPE =
		CwtStatusListTokenBuilder.STATUS_LIST_CWT_CONTENT_TYPE;

	public static final String ENV_KEY = "served_status_list_cwt";

	@Override
	@PreEnvironment(required = { CreateRevokedStatusListReference.ENV_KEY })
	@PostEnvironment(strings = { ENV_KEY })
	public Environment evaluate(Environment env) {

		String uri = OIDFJSON.getString(
			env.getElementFromObject(CreateRevokedStatusListReference.ENV_KEY, "uri"));

		TokenStatusList statusList = EvenOddStatusListContents.create();
		byte[] compressedStatusList = Base64.getUrlDecoder().decode(statusList.encodeStatusList());

		Instant iat = Instant.now();
		// ISO/IEC 18013-5 12.3.6.3 requires the exp claim to be present
		Instant exp = iat.plusSeconds(10 * 60);

		AsymmetricKey.X509CertifiedExplicit signingKey = TestKeysAndCerts.getStatusListSignerKey();

		byte[] token;
		try {
			token = CwtStatusListTokenBuilder.build(uri, iat, exp, TimeUnit.MINUTES.toSeconds(12),
				EvenOddStatusListContents.BITS, compressedStatusList, signingKey, Algorithm.ES256,
				signingKey.getCertChain());
		} catch (Exception e) {
			throw error("Failed to sign the MSO revocation list in CWT format", e);
		}

		env.putString(ENV_KEY, Base64.getEncoder().encodeToString(token));

		logSuccess("Generated the MSO revocation list as a Status List Token in CWT format",
			args("sub", uri, "algorithm", Algorithm.ES256.name(), "exp", exp.getEpochSecond(),
				"length", token.length));

		return env;
	}
}
