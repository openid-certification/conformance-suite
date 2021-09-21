package net.openid.conformance.openbanking_brasil.productsNServices;

import net.openid.conformance.util.field.Field;
import net.openid.conformance.util.field.StringField;

import java.util.Set;

public class CommonFields extends net.openid.conformance.openbanking_brasil.CommonFields {

	public static Field.FieldBuilder consentId() {
		return new StringField
			.Builder("consentId")
			.setPattern("^urn:[a-zA-Z0-9][a-zA-Z0-9-]{0,31}:[a-zA-Z0-9()+,\\-.:=@;$_!*'%\\/?#]+$")
			.setMaxLength(256);
	}

	public static Field.FieldBuilder name() {
		return new StringField
			.Builder("name")
			.setPattern("[\\w\\W\\s]*")
			.setMaxLength(80);
	}

	public static Field.FieldBuilder code() {
		return new StringField
			.Builder("code")
			.setMaxLength(100)
			.setPattern("[\\w\\W\\s]*");
	}

	public static Field.FieldBuilder cnpjNumber() {
		return new StringField
			.Builder("cnpjNumber")
			.setPattern("(\\d{14})$|^NA$")
			.setMaxLength(14);
	}

	public static Field.FieldBuilder urlComplementaryList() {
		return new StringField
			.Builder("urlComplementaryList")
			.setPattern("[\\w\\W\\s]*")
			.setMaxLength(1024)
			.setOptional();
	}

	public static Field.FieldBuilder type(Set<String> types) {
		return new StringField
			.Builder("type")
			.setEnums(types);
	}

	public static Field.FieldBuilder chargingTriggerInfo() {
		return new StringField
			.Builder("chargingTriggerInfo")
			.setPattern("[\\w\\W\\s]*")
			.setMaxLength(2000);
	}

	private static Field.FieldBuilder value(String value) {
		return new StringField
			.Builder(value)
			.setPattern("^((\\d{1,9}\\.\\d{2}){1}|NA)$")
			.setMaxLength(12);
	}

	public static Field.FieldBuilder value() {
		return value("value");
	}

	public static Field.FieldBuilder monthlyFee() {
		return value("monthlyFee");
	}

	public static Field.FieldBuilder rate() {
		return new StringField
			.Builder("rate")
			.setPattern("(^[0-9](\\.[0-9]{4})$|^NA$)")
			.setMaxLength(6);
	}
}
