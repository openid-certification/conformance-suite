package net.openid.conformance.vci10wallet.condition;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Decides whether the credential issuer metadata is returned signed (application/jwt) or unsigned
 * (application/json). The signed form is returned when the Accept header lists application/jwt with
 * a non-zero weight; wildcards and an absent header get the unsigned form, which OID4VCI 1.0
 * section 12.2.2 requires every issuer to support.
 *
 * <p>The media range of each element is read up to its first semicolon, so an Accept header that
 * separates types with semicolons (which {@link VCICheckCredentialIssuerMetadataRequestAcceptHeaderSyntax}
 * reports) still gets the media type it named first.
 */
public class VCIDetermineCredentialIssuerMetadataResponseFormat extends AbstractCondition {

	public static final String ENV_KEY = "credential_issuer_metadata_response_content_type";

	public static final String SIGNED_MEDIA_TYPE = "application/jwt";

	public static final String UNSIGNED_MEDIA_TYPE = "application/json";

	private static final Pattern WEIGHT = Pattern.compile(";[ \\t]*[qQ]=([0-9.]+)");

	@Override
	@PreEnvironment(required = "incoming_request")
	@PostEnvironment(strings = ENV_KEY)
	public Environment evaluate(Environment env) {
		String accept = env.getString("incoming_request", "headers.accept");
		boolean signed = accept != null && requestsSignedMetadata(accept);
		env.putString(ENV_KEY, signed ? SIGNED_MEDIA_TYPE : UNSIGNED_MEDIA_TYPE);

		if (signed) {
			logSuccess("The wallet's Accept header lists application/jwt, so the signed credential issuer metadata is returned", args("accept", accept));
		} else {
			logSuccess("The wallet's Accept header does not list application/jwt, so the unsigned credential issuer metadata is returned", args("accept", accept));
		}
		return env;
	}

	static boolean requestsSignedMetadata(String accept) {
		for (String element : VCICheckCredentialIssuerMetadataRequestAcceptHeaderSyntax.splitListElements(accept)) {
			int semicolon = element.indexOf(';');
			String mediaRange = (semicolon < 0 ? element : element.substring(0, semicolon)).strip().toLowerCase(Locale.ROOT);
			if (!SIGNED_MEDIA_TYPE.equals(mediaRange)) {
				continue;
			}
			Matcher weight = WEIGHT.matcher(semicolon < 0 ? "" : element.substring(semicolon));
			if (!weight.find() || parseWeight(weight.group(1)) > 0) {
				return true;
			}
		}
		return false;
	}

	private static double parseWeight(String qvalue) {
		try {
			return Double.parseDouble(qvalue);
		} catch (NumberFormatException e) {
			// an unparseable weight is reported by the syntax check; treat the range as acceptable
			return 1;
		}
	}
}
