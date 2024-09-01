package net.openid.conformance.variant;

import com.google.common.collect.ImmutableList;

import java.util.List;

@VariantParameter(
	name = "response_type",
	displayName = "Response Type",
	description = "The Response Type to be used in testing. A separate test plan should be run for each response type that needs to be tested."
)
public enum ResponseType {

	CODE("code"),
	ID_TOKEN("id_token"),
	ID_TOKEN_TOKEN("id_token", "token"),
	CODE_ID_TOKEN("code", "id_token"),
	CODE_TOKEN("code", "token"),
	CODE_ID_TOKEN_TOKEN("code", "id_token", "token");

	private final ImmutableList<String> types;

	private ResponseType(String... responseTypes) {
		this.types = ImmutableList.copyOf(List.of(responseTypes));
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

	public boolean isIdToken() { return types.size() == 1 && includesIdToken(); }

}
