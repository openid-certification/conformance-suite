package net.openid.conformance.util.field;

import java.math.BigDecimal;

public class DoubleField extends Field {
	private static final String DEFAULT_REGEXP = "(-?\\d{1,15}(.?\\d{0,4}?))$";
	private static final int DEFAULT_MAX_LENGTH = 20;

	DoubleField(boolean optional, boolean nullable, String path, String pattern, int maxLength, int minLength,
						  Double minValue, Double maxValue) {
		super(optional, nullable, path, pattern, maxLength, minLength);
		if (maxValue != null) {
			super.maxValue = new BigDecimal(maxValue.toString());
		}
		if (minValue != null) {
			super.minValue = new BigDecimal(minValue.toString());
		}
	}

	public static class Builder {

		private boolean optional;
		private boolean nullable;
		private String path = "";
		private String pattern = "";
		private Double maxValue;
		private Double minValue;
		private int maxLength;
		private int minLength;

		public Builder(String path) {
			this.path = path;
			this.pattern = DEFAULT_REGEXP;
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

		public DoubleField build() {
			return new DoubleField(this.optional, this.nullable, this.path, this.pattern, this.maxLength,
				this.minLength, this.minValue, this.maxValue);
		}
	}
}
