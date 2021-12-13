package net.openid.conformance.util.field;

import java.util.Set;

public class IntField extends Field {

	private IntField(boolean optional, boolean nullable, String path, String pattern, int maxLength, int minLength,
					 int maxItems, int minItems, int maxValue, Set<String> enums, int minimum) {
		super(optional, nullable, path, pattern, maxLength, minLength, maxItems, minItems,
			maxValue, enums, minimum);
	}

	public static class Builder extends FieldBuilder {

		public Builder(String path) {
			super(path);
		}

		@Override
		public IntField build() {
			return new IntField(this.optional, this.nullable, this.path, this.pattern, this.maxLength,
				this.minLength, this.maxItems, this.minItems, this.maxValue, this.enums,
				this.minimum);
		}
	}
}
