package net.openid.conformance.util.fields;

public class DoubleField extends Field {

	public static class Builder {
		private DoubleField doubleField;

		public Builder(String path) {
			doubleField = new DoubleField();
			doubleField.setPath(path);
		}

		public Builder setPattern(String pattern) {
			doubleField.setPattern(pattern);
			return this;
		}

		public Builder setFieldOptional(boolean isOptional) {
			doubleField.setOptional(isOptional);
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
