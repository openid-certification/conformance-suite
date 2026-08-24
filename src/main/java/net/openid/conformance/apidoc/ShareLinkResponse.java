package net.openid.conformance.apidoc;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "A private link for sharing read-only access")
public record ShareLinkResponse(
	@Schema(description = "Browser URL that logs a guest in via a one-time token") String link,
	@Schema(description = "The JWT on its own, usable as 'Authorization: Bearer <token>' on the read-only endpoints") String token,
	@Schema(description = "Informational notice when the private-link signing key is not persistently configured; empty string otherwise") String message) {
}
