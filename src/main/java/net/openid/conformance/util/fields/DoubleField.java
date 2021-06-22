package net.openid.conformance.util.fields;

public class DoubleField extends Field {
	public static final String DEFAULT_REGEXP = "(-?\\d{1,15}(.?\\d{0,4}?))$";
	public static final int DEFAULT_MAX_LENGTH = 20;

	public static class Builder {
		private DoubleField doubleField;

		public Builder(String path) {
			doubleField = new DoubleField();
			doubleField.setPath(path);
			doubleField.setPattern(DEFAULT_REGEXP);
			doubleField.setMaxLength(DEFAULT_MAX_LENGTH);
		}

		public Builder setPattern(String pattern) {
			doubleField.setPattern(pattern);
			return this;
		}

		public Builder setFieldOptional() {
			doubleField.setOptional();
			return this;
		}

		public Builder setMaxLength(int maxLength) {
			doubleField.setMaxLength(maxLength);
			return this;
		}

		public Builder setMinLength(int minLength) {
			doubleField.setMinLength(minLength);
			return this;
		}

		public DoubleField build() {
			return doubleField;
		}
	}
}
