package net.openid.conformance.openid.ssf.conditions;

import net.openid.conformance.openid.ssf.conditions.events.OIDSSFSecurityEvent;

public interface EventAckConsumer {

	void accept(String eventId, String jti, OIDSSFSecurityEvent event);
}
