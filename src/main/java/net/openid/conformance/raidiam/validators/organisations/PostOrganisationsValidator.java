package net.openid.conformance.raidiam.validators.organisations;

import com.google.common.collect.Sets;
import com.google.gson.JsonElement;
import net.openid.conformance.condition.client.jsonAsserting.AbstractJsonAssertingCondition;
import net.openid.conformance.logging.ApiName;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.util.field.BooleanField;
import net.openid.conformance.util.field.StringArrayField;
import net.openid.conformance.util.field.StringField;

import java.util.Set;

/**
 * Api url: ****
 * Api endpoint: POST /organisations
 * Api git hash: ****
 *
 */
@ApiName("Raidiam Directory Post Organisations")
public class PostOrganisationsValidator extends AbstractJsonAssertingCondition {

	private static final Set<String> STATUS = Sets.newHashSet("Active", "Pending", "Withdrawn");

	@Override
	public Environment evaluate(Environment environment) {
		JsonElement content = bodyFrom(environment);
		assertField(content,
			new StringField
				.Builder("OrganisationId")
				.setMinLength(1)
				.setMaxLength(40)
				.build());

		assertField(content,
			new StringField
				.Builder("Status")
				.setEnums(STATUS)
				.setOptional()
				.build());

		assertField(content,
			new StringField
				.Builder("OrganisationName")
				.setMaxLength(255)
				.setMinLength(1)
				.build());

		assertField(content,
			new StringField
				.Builder("LegalEntityName")
				.setMaxLength(255)
				.setMinLength(1)
				.build());

		assertField(content,
			new StringField
				.Builder("CountryOfRegistration")
				.setMaxLength(255)
				.setMinLength(2)
				.build());

		assertField(content,
			new StringField
				.Builder("CompanyRegister")
				.setMaxLength(255)
				.setMinLength(1)
				.build());

		assertField(content,
			new StringArrayField
				.Builder("Tags")
				.setOptional()
				.build());

		assertField(content,
			new StringField
				.Builder("Size")
				.setMaxLength(255)
				.setOptional()
				.build());

		assertField(content,
			new StringField
				.Builder("RegistrationNumber")
				.setMaxLength(255)
				.setMinLength(1)
				.build());

		assertField(content,
			new StringField
				.Builder("RegistrationId")
				.setMaxLength(40)
				.setOptional()
				.build());

		assertField(content,
			new StringField
				.Builder("RegisteredName")
				.setMaxLength(255)
				.setOptional()
				.build());

		assertField(content,
			new StringField
				.Builder("AddressLine1")
				.setMaxLength(255)
				.setMinLength(5)
				.build());

		assertField(content,
			new StringField
				.Builder("AddressLine2")
				.setMaxLength(255)
				.setOptional()
				.build());

		assertField(content,
			new StringField
				.Builder("City")
				.setMaxLength(255)
				.setMinLength(2)
				.build());

		assertField(content,
			new StringField
				.Builder("Postcode")
				.setMaxLength(40)
				.setMinLength(3)
				.build());

		assertField(content,
			new StringField
				.Builder("Country")
				.setMaxLength(255)
				.setMinLength(2)
				.build());

		assertField(content,
			new StringField
				.Builder("ParentOrganisationReference")
				.setMaxLength(65535)
				.setOptional()
				.build());

		assertField(content,
			new BooleanField
				.Builder("RequiresParticipantTermsAndConditionsSigning")
				.setOptional()
				.build());

		return environment;
	}
}
