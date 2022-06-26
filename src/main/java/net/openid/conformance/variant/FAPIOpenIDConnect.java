package net.openid.conformance.variant;

@VariantParameter(
	name = "openid",
	displayName = "Test OpenID",
	description = "If your server supports issuing id_tokens, pick 'openid connect'. Otherwise pick plain_oauth."
)
public enum FAPIOpenIDConnect {
	PLAIN_OAUTH,
	OPENID_CONNECT;

	@Override
	public String toString() {
		return name().toLowerCase();
	}

}
