package net.openid.conformance.raidiam.validators;

import net.openid.conformance.util.field.Field;
import com.google.common.collect.Sets;
import net.openid.conformance.util.field.StringField;

import java.util.Set;

public class CommonFields {

	public static final Set<String> STATUS = Sets.newHashSet("Active", "Inactive");

	public static Field getStatus() {
		return new StringField
			.Builder("Status")
			.setEnums(CommonFields.STATUS)
			.setOptional()
			.build();
	}

	public static Field getUserEmail() {
		return new StringField
			.Builder("UserEmail")
			.setOptional()
			.build();
	}

	public static Field getOrganisationId() {
		return new StringField
				.Builder("OrganisationId")
				.setMinLength(1)
				.setMaxLength(40)
				.setOptional()
				.build();
	}
}
