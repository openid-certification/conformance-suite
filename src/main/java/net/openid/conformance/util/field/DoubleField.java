package net.openid.conformance.util.field;

import java.util.Set;

public class DoubleField extends Field {
	private static final String DEFAULT_REGEXP = "(-?\\d{1,15}(.?\\d{0,4}?))$";
	private static final int DEFAULT_MAX_LENGTH = 20;

	protected DoubleField(boolean optional, boolean nullable, String path, String pattern, int maxLength, int minLength,
						  int maxItems, int minItems, int maxValue, Set<String> enums) {
		super(optional, nullable, path, pattern, maxLength, minLength, maxItems, minItems, maxValue, enums);
	}

	public static class Builder extends FieldBuilder {

		public Builder(String path) {
			super(path);
			setPattern(DEFAULT_REGEXP);
			setMaxLength(DEFAULT_MAX_LENGTH);
		}

		@Override
		public DoubleField build() {
			return new DoubleField(this.optional, this.nullable, this.path, this.pattern, this.maxLength,
				this.minLength, this.maxItems, this.minItems, this.maxValue, this.enums);
		}
	}
}
