package net.openid.conformance.openbanking_brasil.testmodules.support;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

public enum EnumResourcesStatus {
	AVAILABLE, UNAVAILABLE, TEMPORARILY_UNAVAILABLE, PENDING_AUTHORISATION;

	public static Set<String> allStatuses() {
		return Arrays.stream(values())
			.map(Enum::name)
			.collect(Collectors.toSet());
	}
}
