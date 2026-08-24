package net.openid.conformance.apidoc;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "A newly created API token. This is the only response that ever contains "
	+ "the token value itself — store it; it cannot be retrieved again.")
public record TokenCreatedResponse(
	@JsonProperty("_id")
	@Schema(description = "Id of the token (for listing and deletion, not for authentication)") String id,
	@Schema(description = "Owner of the token") OwnerId owner,
	@Schema(description = "Reserved; currently always null") Object info,
	@Schema(description = "The bearer token value to use in the Authorization header") String token,
	@Schema(description = "Expiry time in milliseconds since the epoch, or null for a permanent token") Long expires) {
}
