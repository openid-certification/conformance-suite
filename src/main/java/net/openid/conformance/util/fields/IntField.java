package net.openid.conformance.util.fields;

public class IntField extends Field {

	public static class Builder {
		private IntField intField;

		public Builder(String path) {
			intField = new IntField();
			intField.setPath(path);
		}

		public Builder setPattern(String pattern) {
			intField.setPattern(pattern);
			return this;
		}

		public Builder setFieldOptional(boolean isOptional) {
			intField.setOptional(isOptional);
			return this;
		}

		public Builder setMaxLength(int maxLength) {
			intField.setMaxLength(maxLength);
			return this;
		}

		public Builder setMinLength(int minLength) {
			intField.setMinLength(minLength);
			return this;
		}

		public IntField build() {
			return intField;
		}
	}
}
