package net.openid.conformance.openid.ssf;

import java.util.Set;

public final class SsfConstants {

	private SsfConstants() {}

	public static final String DELIVERY_METHOD_PUSH_RFC_8935_URI = "urn:ietf:rfc:8935";

	public static final String DELIVERY_METHOD_POLL_RFC_8936_URI = "urn:ietf:rfc:8936";

	public static Set<String> STANDARD_DELIVERY_METHODS = Set.of(
		DELIVERY_METHOD_PUSH_RFC_8935_URI,
		DELIVERY_METHOD_POLL_RFC_8936_URI
	);
}
