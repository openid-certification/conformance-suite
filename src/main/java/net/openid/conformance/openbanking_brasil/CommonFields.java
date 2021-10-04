package net.openid.conformance.openbanking_brasil;

import net.openid.conformance.util.field.DatetimeField;
import net.openid.conformance.util.field.Field;
import net.openid.conformance.util.field.StringField;


public class CommonFields {

	public static Field.FieldBuilder dataYYYYMMDD(String fieldName) {
		return new DatetimeField
			.Builder(fieldName)
			.setPattern(DatetimeField.PATTERN_YYYY_MM_DD)
			.setMaxLength(10);
	}

	public static Field.FieldBuilder currency() {
		return new StringField
			.Builder("currency")
			.setPattern("^(\\w{3}){1}$")
			.setMaxLength(3);
	}
}
