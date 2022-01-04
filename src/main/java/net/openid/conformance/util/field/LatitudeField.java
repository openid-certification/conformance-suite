package net.openid.conformance.util.field;

public class LatitudeField extends DoubleField {

	private static final String DEFAULT_PATTERN = "^-?\\d{1,2}\\.\\d{1,9}$";
	private static final int DEFAULT_MAX_LENGTH = 13;

	private LatitudeField(boolean optional, boolean nullable, String path, String pattern,
						  int maxLength, int minLength, Double maxValue, Double minValue) {
		super(optional, nullable, path, pattern, maxLength, minLength, minValue, maxValue);
	}

	public static class Builder {

		private boolean optional;
		private boolean nullable;
		private String path = "";
		private String pattern = "";
		private int maxLength;
		private int minLength;
		private Double maxValue;
		private Double minValue;

		public Builder() {
			this.path = "latitude";
			this.pattern = DEFAULT_PATTERN;
			this.maxLength = DEFAULT_MAX_LENGTH;
		}

		public Builder setOptional() {
			this.optional = true;
			return this;
		}

		public Builder setNullable() {
			this.nullable = true;
			return this;
		}

		public Builder setMaxLength(int maxLength) {
			this.maxLength = maxLength;
			return this;
		}

		public Builder setMinLength(int minLength) {
			this.minLength = minLength;
			return this;
		}

		public Builder setPattern(String pattern) {
			this.pattern = pattern;
			return this;
		}

		public Builder setMaxValue(Double maxValue) {
			this.maxValue = maxValue;
			return this;
		}

		public Builder setMinValue(Double minValue) {
			this.minValue = minValue;
			return this;
		}

		public LatitudeField build() {
			return new LatitudeField(this.optional, this.nullable, this.path, this.pattern, this.maxLength,
				this.minLength, this.maxValue, this.minValue);
		}
	}
}
