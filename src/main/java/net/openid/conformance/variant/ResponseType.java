package net.openid.conformance.variant;

import java.util.List;

@VariantParameter(
	name = "response_type",
	displayName = "Response Type",
	description = "The Response Type request parameter response_type informs the Authorization Server of the desired authorization processing flow."
)
public enum ResponseType {

	CODE("code"),
	ID_TOKEN("id_token"),
	ID_TOKEN_TOKEN("id_token", "token"),
	CODE_ID_TOKEN("code", "id_token"),
	CODE_TOKEN("code", "token"),
	CODE_ID_TOKEN_TOKEN("code", "id_token", "token");

	private final List<String> types;

	private ResponseType(String... responseTypes) {
		this.types = List.of(responseTypes);
	}

	@Override
	public String toString() {
		return String.join(" ", types);
	}

	public boolean includesCode() {
		return types.contains("code");
	}

	public boolean includesIdToken() {
		return types.contains("id_token");
	}

	public boolean includesToken() {
		return types.contains("token");
	}

}
