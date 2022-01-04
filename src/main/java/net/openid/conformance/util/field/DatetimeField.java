package net.openid.conformance.util.field;

public class DatetimeField extends Field {
	public static final String DEFAULT_PATTERN = "^[0-9]{4}-[0-9]{2}-[0-9]{2}T[0-9]{2}:[0-9]{2}:[0-9]{2}Z$";
	public static final String ALTERNATIVE_PATTERN = "^(\\d{4})-(1[0-2]|0?[1-9])-(3[01]|[12][0-9]|0?[1-9])T(?:[01]\\d|2[0123]):(?:[012345]\\d):(?:[012345]\\d)Z$";
	public static final String PATTERN_YYYY_MM_DD = "^(\\d{4})-(1[0-2]|0?[1-9])-(3[01]|[12][0-9]|0?[1-9])$";
	private static final int DEFAULT_MAX_LENGTH = 20;

	private int daysOlderAccepted;
	private int secondsOlderThanSeconds;
	private String secondsOlderThanString;

	private DatetimeField(boolean optional, boolean nullable, String path, String pattern, int maxLength, int minLength,
						  int daysOlderAccepted, int secondsOlderThanSeconds, String secondsOlderThanString) {
		super(optional, nullable, path, pattern, maxLength, minLength);
		this.daysOlderAccepted = daysOlderAccepted;
		this.secondsOlderThanSeconds = secondsOlderThanSeconds;
		this.secondsOlderThanString = secondsOlderThanString;
	}

	public int getDaysOlderAccepted() {
		return daysOlderAccepted;
	}

	public void setDaysOlderAccepted(int daysOlderAccepted) {
		this.daysOlderAccepted = daysOlderAccepted;
	}

	public int getSecondsOlderThanSeconds() { return secondsOlderThanSeconds; }
	public String getSecondsOlderThanString() { return secondsOlderThanString; }

	public void setSecondsOlderThan(int secondsOlderThanSeconds, String secondsOlderThanString) { this.secondsOlderThanSeconds = secondsOlderThanSeconds; this.secondsOlderThanString = secondsOlderThanString; }

	public static class Builder {

		private final String path;
		private String pattern = "";
		private boolean optional;
		private boolean nullable;
		private int maxLength;
		private int minLength;
		protected int daysOlderAccepted;
		protected int secondsOlderThanSeconds;
		protected String secondsOlderThanString;

		public Builder(String path) {
			this.pattern = DEFAULT_PATTERN;
			this.maxLength = DEFAULT_MAX_LENGTH;
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

		public Builder setDaysOlderAccepted(int daysOlderAccepted) {
			this.daysOlderAccepted = daysOlderAccepted;
			return this;
		}

		public Builder setSecondsOlderThan(int secondsOlderThanSeconds, String secondsOlderThanString) {
			this.secondsOlderThanSeconds = secondsOlderThanSeconds;
			this.secondsOlderThanString = secondsOlderThanString;
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

		public DatetimeField build() {
			return new DatetimeField(this.optional, this.nullable, this.path, this.pattern, this.maxLength,
				this.minLength, this.daysOlderAccepted, this.secondsOlderThanSeconds, this.secondsOlderThanString);
		}
	}
}
