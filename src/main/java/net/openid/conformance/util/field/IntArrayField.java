package net.openid.conformance.util.field;

import java.math.BigDecimal;

public class IntArrayField extends Field {

	private IntArrayField(boolean optional, boolean nullable, String path, String pattern, int maxLength, int minLength,
							 int maxItems, int minItems, Integer maxValue, Integer minValue) {
		super(optional, nullable, path, pattern, maxLength, minLength);
		super.maxItems = maxItems;
		super.minItems = minItems;
		if (maxValue != null) {
			super.maxValue = new BigDecimal(maxValue);
		}
		if (minValue != null) {
			super.minValue = new BigDecimal(minValue);
		}
	}

	public static class Builder {

		private boolean optional;
		private boolean nullable;
		private String path = "";
		private String pattern = "";
		private int maxLength;
		private int minLength;
		private int maxItems;
		private int minItems;
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

		public Builder setMaxValue(int maxValue) {
			this.maxValue = maxValue;
			return this;
		}

		public Builder setMinValue(int minValue) {
			this.minValue = minValue;
			return this;
		}

		public IntArrayField build() {
			return new IntArrayField(this.optional, this.nullable, this.path, this.pattern,
				this.maxLength, this.minLength,
				this.maxItems, this.minItems,
				this.maxValue, this.minValue);
		}
	}
}
