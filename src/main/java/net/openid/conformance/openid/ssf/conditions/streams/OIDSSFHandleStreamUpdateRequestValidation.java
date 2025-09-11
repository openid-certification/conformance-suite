package net.openid.conformance.openid.ssf.conditions.streams;

import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class OIDSSFHandleStreamUpdateRequestValidation extends OIDSSFHandleStreamCreateRequestValidation {

	@Override
	protected Set<String> getTransmitterSuppliedProperties() {
		// allow stream_id for updates
		return super.getTransmitterSuppliedProperties().stream().filter(Predicate.not("stream_id"::equals)).collect(Collectors.toSet());
	}
}
