package net.openid.conformance.apidoc;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "The currently authenticated user")
public record CurrentUserResponse(
	@Schema(description = "Issuer of the user's login identity") String iss,
	@Schema(description = "Subject of the user's login identity") String sub,
	@Schema(description = "String rendering of the principal map (informational only; use iss/sub)") String principal,
	@Schema(description = "Display name from the login provider") String displayName,
	@Schema(description = "Whether the user has the admin role") boolean isAdmin,
	@Schema(description = "True for a guest logged in via a private share link") boolean isGuest) {
}
