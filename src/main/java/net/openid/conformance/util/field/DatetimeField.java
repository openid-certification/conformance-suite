package net.openid.conformance.util.field;

import java.util.Set;

public class DatetimeField extends Field {
	public static final String DEFAULT_PATTERN = "^[0-9]{4}-[0-9]{2}-[0-9]{2}T[0-9]{2}:[0-9]{2}:[0-9]{2}Z$";
	public static final String ALTERNATIVE_PATTERN = "^(\\d{4})-(1[0-2]|0?[1-9])-(3[01]|[12][0-9]|0?[1-9])T(?:[01]\\d|2[0123]):(?:[012345]\\d):(?:[012345]\\d)Z$";
	private static final int DEFAULT_MAX_LENGTH = 20;

	private int daysOlderAccepted;
	private int secondsOlderThanSeconds;
	private String secondsOlderThanString;

	public DatetimeField(String path) {
		super(path);
		setPattern(DEFAULT_PATTERN);
		setMaxLength(DEFAULT_MAX_LENGTH);
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

	private DatetimeField(boolean optional, boolean nullable, String path, String pattern, int maxLength, int minLength,
						  int maxItems, int minItems, int maxValue, Set<String> enums, int daysOlderAccepted, int secondsOlderThanSeconds, String secondsOlderThanString) {
		super(optional, nullable, path, pattern, maxLength, minLength, maxItems, minItems, maxValue, enums);
		this.daysOlderAccepted = daysOlderAccepted;
		this.secondsOlderThanSeconds = secondsOlderThanSeconds;
		this.secondsOlderThanString = secondsOlderThanString;
	}

	public static class Builder extends FieldBuilder {

		protected int daysOlderAccepted;
		protected int secondsOlderThanSeconds;
		protected String secondsOlderThanString;

		public FieldBuilder setDaysOlderAccepted(int daysOlderAccepted) {
			this.daysOlderAccepted = daysOlderAccepted;
			return this;
		}

		public FieldBuilder setSecondsOlderThan(int secondsOlderThanSeconds, String secondsOlderThanString) {
			this.secondsOlderThanSeconds = secondsOlderThanSeconds;
			this.secondsOlderThanString = secondsOlderThanString;
			return this;
		}

		public Builder(String path) {
			super(path);
			setPath(path);
			setPattern(DEFAULT_PATTERN);
			setMaxLength(DEFAULT_MAX_LENGTH);
		}

		@Override
		public DatetimeField build() {
			return new DatetimeField(this.optional, this.nullable, this.path, this.pattern, this.maxLength,
				this.minLength, this.maxItems, this.minItems, this.maxValue, this.enums, this.daysOlderAccepted, this.secondsOlderThanSeconds, this.secondsOlderThanString);
		}
	}
}
