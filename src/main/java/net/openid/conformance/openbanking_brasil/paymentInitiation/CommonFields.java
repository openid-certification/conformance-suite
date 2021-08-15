package net.openid.conformance.openbanking_brasil.paymentInitiation;

import net.openid.conformance.util.field.Field;
import net.openid.conformance.util.field.StringField;

public class CommonFields {

	public static Field consentId() {
		return new StringField
			.Builder("consentId")
			.setPattern("^urn:[a-zA-Z0-9][a-zA-Z0-9-]{0,31}:[a-zA-Z0-9()+,\\-.:=@;$_!*'%\\/?#]+$")
			.setMaxLength(256)
			.build();
	}
}
