package net.openid.conformance.util.field;

import java.util.Set;

public class DatetimeField extends Field {
	private static final String DEFAULT_PATTERN = "^[0-9]{4}-[0-9]{2}-[0-9]{2}T[0-9]{2}:[0-9]{2}:[0-9]{2}Z$";
	private static final String ALTERNATIVE_PATTERN = "^(\\d{4})-(1[0-2]|0?[1-9])-(3[01]|[12][0-9]|0?[1-9])T(?:[01]\\d|2[0123]):(?:[012345]\\d):(?:[012345]\\d)Z$";
	private static final int DEFAULT_MAX_LENGTH = 20;

	public DatetimeField(String path) {
		super(path);
		setPattern(DEFAULT_PATTERN);
		setMaxLength(DEFAULT_MAX_LENGTH);
	}

	private DatetimeField(boolean optional, String path, String pattern, int maxLength, int minLength,
						  int maxItems, int minItems, int maxValue, Set<String> enums) {
		super(optional, path, pattern, maxLength, minLength, maxItems, minItems, maxValue, enums);
	}

	public static class Builder extends FieldBuilder {

		public Builder(String path) {
			super(path);
			setPath(path);
			setPattern(DEFAULT_PATTERN);
			setMaxLength(DEFAULT_MAX_LENGTH);
		}

		public DatetimeField build() {
			return new DatetimeField(this.optional, this.path, this.pattern, this.maxLength,
				this.minLength, this.maxItems, this.minItems, this.maxValue, this.enums);
		}
	}
}
