package net.openid.conformance.apidoc;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "An existing API token; the token value itself is never returned after creation")
public record TokenSummary(
	@JsonProperty("_id")
	@Schema(description = "Id of the token") String id,
	@Schema(description = "Expiry time in milliseconds since the epoch, or null for a permanent token") Long expires) {
}
