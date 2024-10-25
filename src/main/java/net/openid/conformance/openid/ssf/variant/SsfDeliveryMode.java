package net.openid.conformance.openid.ssf.variant;

import net.openid.conformance.variant.VariantParameter;

@VariantParameter(
	name = "ssf_delivery_mode",
	displayName = "Delivery Mode",
	description = "Whether the server supports PUSH or PULL based delivery mode."
)
public enum SsfDeliveryMode {

	/**
	 * The Poll delivery mode, see: https://openid.net/specs/openid-sharedsignals-framework-1_0-ID3.html#section-10.3.1.2
	 */
	PUSH,

	/**
	 * The PUSH delivery mode, see: https://openid.net/specs/openid-sharedsignals-framework-1_0-ID3.html#section-10.3.1.1
	 */
	POLL;

	@Override
	public String toString() {
		return name().toLowerCase();
	}
}
