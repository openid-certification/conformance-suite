package net.openid.conformance.util.field;

import java.util.Set;

public class LatitudeField extends DoubleField {

	private static final String DEFAULT_PATTERN = "^-?\\d{1,2}\\.\\d{1,9}$";
	private static final int DEFAULT_MAX_LENGTH = 13;

	private LatitudeField(boolean optional, boolean nullable, String path, String pattern, int maxLength, int minLength,
						  int maxItems, int minItems, int maxValue, Set<String> enums) {
		super(optional, nullable, path, pattern, maxLength, minLength, maxItems, minItems, maxValue, enums);
	}

	public static class Builder extends FieldBuilder {

		public Builder() {
			super("latitude");
			setPattern(DEFAULT_PATTERN);
			setMaxLength(DEFAULT_MAX_LENGTH);
		}

		@Override
		public LatitudeField build() {
			return new LatitudeField(this.optional, this.nullable, this.path, this.pattern, this.maxLength,
				this.minLength, this.maxItems, this.minItems, this.maxValue, this.enums);
		}
	}
}