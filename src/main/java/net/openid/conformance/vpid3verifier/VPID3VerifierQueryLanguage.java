package net.openid.conformance.vpid3verifier;

import net.openid.conformance.variant.VariantParameter;

@VariantParameter(
	name = "query_language",
	displayName = "Query Language",
	description = "Whether Presentation Exchange or Digital Credentials Query Language (DCQL) is used for selecting the returned credential."
)
public enum VPID3VerifierQueryLanguage
{
	PRESENTATION_EXCHANGE("presentation_exchange"),
	DCQL("dcql");

	private final String requestMethod;

	private VPID3VerifierQueryLanguage(String requestMethodIn) {
		requestMethod = requestMethodIn;
	}

	@Override
	public String toString() {
		return requestMethod;
	}
}
