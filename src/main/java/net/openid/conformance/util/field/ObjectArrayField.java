package net.openid.conformance.util.field;

import com.google.gson.JsonObject;

import java.util.function.Consumer;

public class ObjectArrayField extends Field {

	private ObjectArrayField(boolean optional, boolean nullable, String path,
							 Consumer<JsonObject> validator, int minItems, int maxItems) {
		super(optional, nullable, path, validator, minItems, maxItems);
	}

	public static class Builder {

		private boolean optional;
		private boolean nullable;
		private final String path;
		private Consumer<JsonObject> validator;
		private int maxItems;
		private int minItems;

		public Builder(String path) {
			this.path = path;
		}

		public Builder setOptional() {
			this.optional = true;
			return this;
		}

		public Builder setOptional(boolean value) {
			this.optional = value;
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

		public Builder setMaxItems(int maxItems) {
			this.maxItems = maxItems;
			return this;
		}

		public Builder setMinItems(int minItems) {
			this.minItems = minItems;
			return this;
		}

		public ObjectArrayField build() {
			return new ObjectArrayField(this.optional, this.nullable, this.path, this.validator,
				this.minItems, this.maxItems);
		}
	}
}
