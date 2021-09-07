package net.openid.conformance.util.field;

import com.google.gson.JsonObject;

import java.util.function.Consumer;

public class ObjectArrayField extends Field {

	private ObjectArrayField(boolean optional, boolean nullable, String path, Consumer<JsonObject> validator, int minItems, int maxItems) {
		super(optional, nullable, path, maxItems, minItems, validator);
	}

	public static class Builder extends FieldBuilder {
		public Builder(String path) {
			super(path);
		}

		@Override
		public ObjectArrayField build() {
			return new ObjectArrayField(this.optional, this.nullable, this.path, this.validator,
				this.minItems, this.maxItems);
		}

	}
}
