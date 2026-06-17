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

	// CAEP Interop Profile §2.7.2 reserved OAuth scopes for SSF API access.
	public static final String SCOPE_SSF_READ = "ssf.read";

	public static final String SCOPE_SSF_MANAGE = "ssf.manage";

	public static final Set<String> SSF_SCOPES = Set.of(SCOPE_SSF_READ, SCOPE_SSF_MANAGE);

	public enum StreamStatus {
		enabled, paused, disabled;
	}

	public static String SECURITY_EVENT_TOKEN_CONTENT_TYPE = "application/secevent+jwt";
}
