package net.openid.conformance.util.field;

import java.util.Set;

public class LongitudeField extends DoubleField {

	private static final String DEFAULT_PATTERN = "^-?\\d{1,3}\\.\\d{1,8}$";
	private static final int DEFAULT_MAX_LENGTH = 13;

	private LongitudeField(boolean optional, String path, String pattern, int maxLength, int minLength,
						  int maxItems, int minItems, int maxValue, Set<String> enums) {
		super(optional, path, pattern, maxLength, minLength, maxItems, minItems, maxValue, enums);
	}

	public static class Builder extends FieldBuilder {

		public Builder() {
			super("longitude");
			setPattern(DEFAULT_PATTERN);
			setMaxLength(DEFAULT_MAX_LENGTH);
		}

		public LongitudeField build() {
			return new LongitudeField(this.optional, this.path, this.pattern, this.maxLength,
				this.minLength, this.maxItems, this.minItems, this.maxValue, this.enums);
		}
	}
}
