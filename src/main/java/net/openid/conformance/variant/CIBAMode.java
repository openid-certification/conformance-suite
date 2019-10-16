package net.openid.conformance.variant;

@VariantParameter("ciba_mode")
public enum CIBAMode {

	POLL,
	PING,
	PUSH;

	@Override
	public String toString() {
		return name().toLowerCase();
	}

}
