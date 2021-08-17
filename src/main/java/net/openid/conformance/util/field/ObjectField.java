package net.openid.conformance.util.field;

import com.google.gson.JsonObject;

import java.util.function.Consumer;

public class ObjectField extends Field {
	private Consumer<JsonObject> validator;

	private ObjectField(boolean optional, boolean nullable, String path, Consumer<JsonObject> validator) {
		super(optional, nullable, path);
		this.validator = validator;
	}

	public Consumer<JsonObject> getValidator() {
		return this.validator;
	}


	public static class Builder extends FieldBuilder {
		protected Consumer<JsonObject> validator;

		public Builder(String path) {
			super(path);
		}

		public ObjectField build() {
			return new ObjectField(this.optional, this.nullable, this.path, this.validator);
		}

		public FieldBuilder setValidator(Consumer<JsonObject> validator) {
			this.validator = validator;
			return this;
		}

	}
}
