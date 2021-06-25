package net.openid.conformance.util.field;

import java.util.Set;

public class IntField extends Field {

	private IntField(boolean optional, String path, String pattern, int maxLength, int minLength,
					 int maxItems, int minItems, int maxValue, Set<String> enums) {
		super(optional, path, pattern, maxLength, minLength, maxItems, minItems, maxValue, enums);
	}

	public static class Builder extends FieldBuilder {

		public Builder(String path) {
			super(path);
		}

		public IntField build() {
			return new IntField(this.optional, this.path, this.pattern, this.maxLength,
				this.minLength, this.maxItems, this.minItems, this.maxValue, this.enums);
		}
	}
}
