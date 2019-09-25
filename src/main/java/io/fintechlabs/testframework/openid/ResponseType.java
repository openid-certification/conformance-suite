package io.fintechlabs.testframework.openid;

import java.util.List;

public final class ResponseType {

	public static final ResponseType CODE = new ResponseType("code");
	public static final ResponseType ID_TOKEN = new ResponseType("id_token");
	public static final ResponseType ID_TOKEN_TOKEN = new ResponseType("id_token", "token");
	public static final ResponseType CODE_ID_TOKEN = new ResponseType("code", "id_token");
	public static final ResponseType CODE_TOKEN = new ResponseType("code", "token");
	public static final ResponseType CODE_ID_TOKEN_TOKEN = new ResponseType("code", "id_token", "token");

	private final List<String> values;

	private ResponseType(String... values) {
		this.values = List.of(values);
	}

	public static ResponseType parse(String responseType) {
		return new ResponseType(responseType.split(" "));
	}

	@Override
	public boolean equals(Object obj) {
		return this == obj
				|| ((obj instanceof ResponseType)
						&& this.values.equals(((ResponseType) obj).values));
	}

	public List<String> getValues() {
		return values;
	}

	public boolean includesCode() {
		return values.contains("code");
	}

	public boolean includesIdToken() {
		return values.contains("id_token");
	}

	public boolean includesToken() {
		return values.contains("token");
	}

	@Override
	public String toString() {
		return String.join(" ", values);
	}
}
