package net.openid.conformance.authzen.condition;

import com.google.gson.JsonElement;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

import java.net.URI;
import java.net.URISyntaxException;

/**
 * Validate the optional 'Signed Metadata Issuer' ({@code pdp.metadata_issuer}) test configuration
 * field, which names the party expected to attest to the discovery metadata when that party is not
 * the PDP itself (AuthZEN §11.8).
 *
 * <p>{@link ValidatePDPSignedMetadataIss} compares this value against the {@code iss} claim verbatim,
 * so anything wrong with it surfaces only as an issuer mismatch — which reads as a PDP fault when it
 * is really a configuration one. Checking it here means the tester is told which of the two is broken.
 *
 * <p>RFC 7519 §2 defines {@code iss} as a StringOrURI: an arbitrary string is allowed, but a value
 * containing a colon MUST be a URI, and values are compared as case-sensitive strings with no
 * canonicalization. So a bare name is accepted, anything colon-bearing has to parse as a URI, and
 * surrounding whitespace — which could only ever produce a mismatch — is rejected. Nothing narrower
 * is enforced: unlike the 'PDP Identifier' ({@link ValidatePDPIdentifier}), an attester identifier is
 * not required by any specification to be an https URL.
 */
public class ValidatePDPMetadataIssuer extends AbstractCondition {

	private static final String LABEL = "'Signed Metadata Issuer' field in the test configuration";

	private static final String FALLBACK = "no 'Signed Metadata Issuer' is configured, so the `signed_metadata` "
		+ "`iss` claim will be checked against the 'PDP Identifier'";

	@Override
	@PreEnvironment(required = {"config"})
	public Environment evaluate(Environment env) {
		if (!SignedMetadataIssuerConfig.isConfigured(env)) {
			logSuccess("The metadata is expected to be self-attested: " + FALLBACK);
			return env;
		}

		JsonElement element = SignedMetadataIssuerConfig.element(env);
		if (!element.isJsonPrimitive() || !element.getAsJsonPrimitive().isString()) {
			throw error(LABEL + " must be a string", args("metadata_issuer", element));
		}

		String issuer = OIDFJSON.getString(element);

		if (!issuer.equals(issuer.trim())) {
			throw error(LABEL + " has leading or trailing whitespace, which could never match the `iss` claim of "
				+ "the `signed_metadata` JWT", args("metadata_issuer", issuer));
		}

		// RFC 7519 StringOrURI: arbitrary strings are allowed, but one containing ':' must be a URI.
		if (issuer.contains(":")) {
			try {
				URI uri = new URI(issuer);
				if (!uri.isAbsolute()) {
					throw error(LABEL + " contains a ':', so RFC 7519 requires it to be a URI, but it is not an "
						+ "absolute one", args("metadata_issuer", issuer));
				}
			} catch (URISyntaxException e) {
				throw error(LABEL + " contains a ':', so RFC 7519 requires it to be a URI, but it is not a valid "
					+ "one", e, args("metadata_issuer", issuer));
			}
		}

		logSuccess("The `signed_metadata` `iss` claim will be checked against the configured 'Signed Metadata "
			+ "Issuer', which AuthZEN §11.8 allows to be a party other than the PDP itself",
			args("metadata_issuer", issuer));
		return env;
	}
}
