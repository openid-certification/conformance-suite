package net.openid.conformance.util.field;

import java.math.BigDecimal;

public class NumberField extends Field {

	private NumberField(boolean optional, boolean nullable, String path, String pattern,
						int maxLength, int minLength,Number maxValue, Number minValue) {
		super(optional, nullable, path, pattern, maxLength, minLength);
		if (maxValue != null) {
			super.maxValue = new BigDecimal(maxValue.toString());
		}

		if (minValue != null) {
			super.minValue = new BigDecimal(minValue.toString());
		}
	}

	public static class Builder {

		private final String path;
		private String pattern = "";
		private boolean optional;
		private boolean nullable;
		private int maxLength;
		private int minLength;
		private Number minValue = null;
		private Number maxValue;

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

		public Builder setMaxValue(Number maxValue) {
			this.maxValue = maxValue;
			return this;
		}

		public Builder setMinValue(Number minValue) {
			this.minValue = minValue;
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

		public NumberField build() {
			return new NumberField(this.optional, this.nullable, this.path, this.pattern,
				this.maxLength, this.minLength, this.maxValue, this.minValue);
		}
	}
}
