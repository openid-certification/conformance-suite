package net.openid.conformance.openid.ssf.conditions;

import net.openid.conformance.openid.ssf.conditions.events.OIDSSFSecurityEvent;

@FunctionalInterface
public interface OIDSSFEventAckConsumer {

	void accept(String eventId, String jti, OIDSSFSecurityEvent event);
}
