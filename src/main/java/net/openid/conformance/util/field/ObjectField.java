package net.openid.conformance.util.field;

import com.google.gson.JsonObject;

import java.util.function.Consumer;

public class ObjectField extends Field {

	private ObjectField(boolean optional, boolean nullable, String path, Consumer<JsonObject> validator) {
		super(optional, nullable, path, validator);
	}

	public static class Builder {

		private boolean optional;
		private boolean nullable;
		private final String path;
		private Consumer<JsonObject> validator;

		public Builder(String path) {
			this.path = path;
		}

		public Builder setOptional() {
			this.optional = true;
			return this;
		}

		public Builder setNullable() {
			this.nullable = true;
			return this;
		}

		public Builder setValidator(Consumer<JsonObject> validator) {
			this.validator = validator;
			return this;
		}

		public ObjectField build() {
			return new ObjectField(this.optional, this.nullable, this.path, this.validator);
		}
	}
}
