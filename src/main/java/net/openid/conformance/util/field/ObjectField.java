package net.openid.conformance.util.field;

import com.google.gson.JsonObject;

import java.util.function.Consumer;

public class ObjectField extends Field {

	private ObjectField(boolean optional, boolean nullable, String path, Consumer<JsonObject> validator) {
		super(optional, nullable, path, validator);
	}

	public static class Builder extends FieldBuilder {
		public Builder(String path) {
			super(path);
		}

		@Override
		public ObjectField build() {
			return new ObjectField(this.optional, this.nullable, this.path, this.validator);
		}

	}
}
