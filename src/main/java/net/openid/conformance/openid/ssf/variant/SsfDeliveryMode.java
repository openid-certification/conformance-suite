package net.openid.conformance.openid.ssf.variant;

import net.openid.conformance.variant.VariantParameter;

@VariantParameter(
	name = "ssf_delivery_mode",
	displayName = "SSF Delivery Mode",
	description = "Whether the SSF server supports PUSH or PULL based delivery mode."
)
public enum SsfDeliveryMode {

	/**
	 * The Poll delivery mode, see: https://openid.net/specs/openid-sharedsignals-framework-1_0-ID3.html#section-10.3.1.2
	 */
	PUSH("urn:ietf:rfc:8935"),

	/**
	 * The PUSH delivery mode, see: https://openid.net/specs/openid-sharedsignals-framework-1_0-ID3.html#section-10.3.1.1
	 */
	POLL("urn:ietf:rfc:8936");

	@Override
	public String toString() {
		return name().toLowerCase();
	}

	private final String alias;

	SsfDeliveryMode(String alias) {
		this.alias = alias;
	}

	public String getAlias() {
		return alias;
	}
}
