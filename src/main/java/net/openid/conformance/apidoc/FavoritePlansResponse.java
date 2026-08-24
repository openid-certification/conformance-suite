package net.openid.conformance.apidoc;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "The current user's favorited plan names")
public record FavoritePlansResponse(
	@Schema(description = "Plan names, in the order they were favorited") List<String> plans) {
}
