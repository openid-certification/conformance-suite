package net.openid.conformance.apidoc;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Identity of the user that owns the resource")
public record OwnerId(
	@Schema(description = "Issuer of the owner's login identity") String iss,
	@Schema(description = "Subject of the owner's login identity") String sub) {
}
