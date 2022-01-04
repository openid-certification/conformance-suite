package net.openid.conformance.util.field;

import java.math.BigDecimal;

public class IntField extends Field {

	private IntField(boolean optional, boolean nullable, String path, String pattern,
					 int maxLength, int minLength, Integer maxValue, Integer minValue) {
		super(optional, nullable, path, pattern, maxLength, minLength);
		if (maxValue != null) {
			super.maxValue = new BigDecimal(maxValue);
		}
		if (minValue != null) {
			super.minValue = new BigDecimal(minValue);
		}
	}

	public static class Builder {

		private final String path;
		private String pattern = "";
		private boolean optional;
		private boolean nullable;
		private int maxLength;
		private int minLength;
		private Integer maxValue;
		private Integer minValue;

		public Builder(String path) {
			this.path = path;
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

		public Builder setMinLength(Integer minLength) {
			this.minLength = minLength;
			return this;
		}

		public Builder setMaxValue(Integer maxValue) {
			this.maxValue = maxValue;
			return this;
		}

		public Builder setMinValue(int minValue) {
			this.minValue = minValue;
			return this;
		}

		public Builder setPattern(String pattern) {
			this.pattern = pattern;
			return this;
		}

		public IntField build() {
			return new IntField(this.optional, this.nullable, this.path, this.pattern, this.maxLength,
				this.minLength, this.maxValue, this.minValue);
		}
	}
}
