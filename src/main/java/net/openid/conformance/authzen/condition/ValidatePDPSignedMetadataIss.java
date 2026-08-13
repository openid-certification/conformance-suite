package net.openid.conformance.authzen.condition;

import com.google.common.base.Strings;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

/**
 * Check the {@code iss} (issuer) claim of the PDP discovery {@code signed_metadata}
 * JWT against the issuer the tester expects.
 *
 * <p>AuthZEN §9.1.3 defines {@code iss} as the party attesting to the signed claims,
 * and §11.8 explicitly allows that party to be someone other than the PDP itself
 * (leaving the trust decision out of scope). So the expected value is configurable:
 *
 * <ul>
 *   <li>when the 'Signed Metadata Issuer' field ({@code pdp.metadata_issuer}) is set,
 *       {@code iss} MUST equal it — this is how a third-party metadata attester is
 *       declared as trusted for the test;</li>
 *   <li>when it is blank, {@code iss} MUST equal the 'PDP Identifier'
 *       ({@code pdp.policy_decision_point}), i.e. the metadata is expected to be
 *       self-attested by the PDP.</li>
 * </ul>
 *
 * <p>The separate §9.2.3 requirement that {@code policy_decision_point} matches the URL
 * the metadata was discovered from is checked by
 * {@link EnsurePolicyDecisionPointMatchesIssuer}, and the cryptographic trust binding to
 * the attesting party is the out-of-band 'PDP JWK Set' that
 * {@link VerifyAuthzenSignedMetadataSignature} verifies the signature against.
 */
public class ValidatePDPSignedMetadataIss extends AbstractCondition {

	@Override
	@PreEnvironment(required = {"pdp_signed_metadata", "config"})
	public Environment evaluate(Environment env) {
		String actualIssuer = env.getString("pdp_signed_metadata", "claims.iss");
		if(Strings.isNullOrEmpty(actualIssuer)) {
			throw error("PDP `signed_metadata` MUST contain an `iss` (issuer) claim", args("issuer", actualIssuer));
		}

		String configuredIssuer = SignedMetadataIssuerConfig.value(env);
		boolean usingConfiguredIssuer = configuredIssuer != null;
		String expectedIssuer = usingConfiguredIssuer
			? configuredIssuer
			: env.getString("config", "pdp.policy_decision_point");
		// Which field the tester should go and look at if this check fails.
		String expectedIssuerField = usingConfiguredIssuer ? "Signed Metadata Issuer" : "PDP Identifier";

		if (Strings.isNullOrEmpty(expectedIssuer)) {
			throw error("The 'Signed Metadata Issuer' and 'PDP Identifier' fields are both empty or missing from the "
				+ "'AuthZEN' section in the test configuration; one of them is needed to check the `iss` claim of the "
				+ "discovery metadata `signed_metadata` JWT");
		}

		if (!expectedIssuer.equals(actualIssuer)) {
			throw error("PDP issuer mismatch in signed_metadata",
				args("expected", expectedIssuer, "actual", actualIssuer, "compared_against", expectedIssuerField));
		}

		logSuccess("Found matching issuer in PDP `signed_metadata`",
			args("expected", expectedIssuer, "actual", actualIssuer, "compared_against", expectedIssuerField));
		return env;
	}

}
