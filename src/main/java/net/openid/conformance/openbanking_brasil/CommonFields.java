package net.openid.conformance.openbanking_brasil;

import net.openid.conformance.util.field.DatetimeField;
import net.openid.conformance.util.field.StringField;

import java.util.Set;


public class CommonFields {

	public static DatetimeField.Builder dataYYYYMMDD(String fieldName) {
		return new DatetimeField
			.Builder(fieldName)
			.setPattern(DatetimeField.PATTERN_YYYY_MM_DD)
			.setMaxLength(10);
	}

	public static StringField.Builder currency() {
		return new StringField
			.Builder("currency")
			.setPattern("^(\\w{3}){1}$")
			.setMaxLength(3);
	}

	public static StringField.Builder consentId() {
		return new StringField
			.Builder("consentId")
			.setPattern("^urn:[a-zA-Z0-9][a-zA-Z0-9-]{0,31}:[a-zA-Z0-9()+,\\-.:=@;$_!*'%\\/?#]+$")
			.setMaxLength(256);
	}

	public static StringField.Builder name() {
		return new StringField
			.Builder("name")
			.setPattern("[\\w\\W\\s]*")
			.setMaxLength(80);
	}

	public static StringField.Builder code() {
		return new StringField
			.Builder("code")
			.setMaxLength(100)
			.setPattern("[\\w\\W\\s]*");
	}

	public static StringField.Builder cnpjNumber() {
		return new StringField
			.Builder("cnpjNumber")
			.setPattern("(\\d{14})$|^NA$")
			.setMaxLength(14);
	}

	public static StringField.Builder urlComplementaryList() {
		return new StringField
			.Builder("urlComplementaryList")
			.setPattern("[\\w\\W\\s]*")
			.setMaxLength(1024)
			.setOptional();
	}

	public static StringField.Builder type(Set<String> types) {
		return new StringField
			.Builder("type")
			.setEnums(types);
	}

	public static StringField.Builder chargingTriggerInfo() {
		return new StringField
			.Builder("chargingTriggerInfo")
			.setPattern("[\\w\\W\\s]*")
			.setMaxLength(2000);
	}

	private static StringField.Builder value(String value) {
		return new StringField
			.Builder(value)
			.setPattern("^((\\d{1,9}\\.\\d{2}){1}|NA)$")
			.setMaxLength(12);
	}

	public static StringField.Builder value() {
		return value("value");
	}

	public static StringField.Builder monthlyFee() {
		return value("monthlyFee");
	}

	public static StringField.Builder rate() {
		return new StringField
			.Builder("rate")
			.setPattern("(^[0-9](\\.[0-9]{4})$|^NA$)")
			.setMaxLength(6);
	}
}
