package net.openid.conformance.util.field;

import java.util.Set;

public class StringField extends Field {

	private StringField(boolean optional, boolean nullable, String path, String pattern, int maxLength, int minLength,
						int maxItems, int minItems, int maxValue, Set<String> enums) {
		super(optional, nullable, path, pattern, maxLength, minLength, maxItems, minItems, maxValue, enums);
	}

	public static class Builder extends FieldBuilder {

		public Builder(String path) {
			super(path);
		}

		@Override
		public StringField build() {
			return new StringField(this.optional, this.nullable, this.path, this.pattern, this.maxLength,
				this.minLength, this.maxItems, this.minItems, this.maxValue, this.enums);
		}
	}
}
