package net.openid.conformance.util.field;

import java.math.BigDecimal;

public class NumberArrayField extends Field {

	private NumberArrayField(boolean optional, boolean nullable, String path, String pattern,
							 int maxLength, int minLength,
							 int maxItems, int minItems,
							 Number maxValue, Number minValue) {
		super(optional, nullable, path, pattern, maxLength, minLength);
		super.maxItems = maxItems;
		super.minItems = minItems;
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
		private int maxItems;
		private int minItems;
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

		public Builder setPattern(String pattern) {
			this.pattern = pattern;
			return this;
		}

		public Builder setMaxItems(int maxItems) {
			this.maxItems = maxItems;
			return this;
		}

		public Builder setMinItems(int minItems) {
			this.minItems = minItems;
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

		public Builder setMaxValue(Number maxValue) {
			this.maxValue = maxValue;
			return this;
		}

		public Builder setMinValue(Number minValue) {
			this.minValue = minValue;
			return this;
		}

		public NumberArrayField build() {
			return new NumberArrayField(this.optional, this.nullable, this.path, this.pattern,
				this.maxItems, this.minItems,
				this.maxLength, this.minLength,
				this.maxValue, this.minValue);
		}
	}
}
