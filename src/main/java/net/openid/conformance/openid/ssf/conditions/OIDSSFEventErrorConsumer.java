package net.openid.conformance.openid.ssf.conditions;

import com.google.gson.JsonObject;

/**
 * Callback invoked when a receiver reports an error for a delivered SET via
 * the {@code setErrs} member of a poll request (RFC 8936 2.4).
 */
@FunctionalInterface
public interface OIDSSFEventErrorConsumer {

	void accept(String streamId, String jti, JsonObject error);
}
