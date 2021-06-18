package net.openid.conformance.util.fields;

public class LongField extends Field {

	public static class Builder {
		private LongField longField;

		public Builder(String path) {
			longField = new LongField();
			longField.setPath(path);
		}

		public Builder setPattern(String pattern) {
			longField.setPattern(pattern);
			return this;
		}

		public Builder setFieldOptional() {
			longField.setOptional();
			return this;
		}

		public Builder setMaxLength(int maxLength) {
			longField.setMaxLength(maxLength);
			return this;
		}

		public Builder setMinLength(int minLength) {
			longField.setMinLength(minLength);
			return this;
		}

		public LongField build() {
			return longField;
		}
	}
}
