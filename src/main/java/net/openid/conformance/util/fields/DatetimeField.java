package net.openid.conformance.util.fields;

public class DatetimeField extends StringField {
	public static final String DEFAULT_PATTERN = "^[0-9]{4}-[0-9]{2}-[0-9]{2}T[0-9]{2}:[0-9]{2}:[0-9]{2}Z$";
	public static final String ALTERNATIVE_PATTERN = "^(\\d{4})-(1[0-2]|0?[1-9])-(3[01]|[12][0-9]|0?[1-9])T(?:[01]\\d|2[0123]):(?:[012345]\\d):(?:[012345]\\d)Z$";

	public DatetimeField(String path) {
		setPath(path);
		setPattern(DEFAULT_PATTERN);
		setMaxLength(20);
	}

	public static class Builder {
		private DatetimeField datetimeField;

		public Builder(String path) {
			datetimeField = new DatetimeField(path);
		}

		public DatetimeField.Builder setFieldOptional() {
			datetimeField.setOptional();
			return this;
		}

		public DatetimeField.Builder setPattern(String pattern) {
			datetimeField.setPattern(pattern);
			return this;
		}

		public DatetimeField build() {
			return datetimeField;
		}
	}
}
