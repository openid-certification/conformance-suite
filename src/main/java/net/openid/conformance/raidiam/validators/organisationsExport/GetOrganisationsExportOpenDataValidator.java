package net.openid.conformance.raidiam.validators.organisationsExport;

import com.google.common.collect.Sets;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.client.AbstractJsonAssertingCondition;
import net.openid.conformance.logging.ApiName;
import net.openid.conformance.raidiam.validators.CommonParts;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.util.field.ObjectArrayField;
import net.openid.conformance.util.field.StringField;

import java.util.Set;

/**
 * Api url: ****
 * Api endpoint: GET /organisations/export/open-data
 * Api git hash: ****
 *
 */
@ApiName("Raidiam Directory Get Organisations Export OpenData")
public class GetOrganisationsExportOpenDataValidator extends AbstractJsonAssertingCondition {

	private final CommonParts parts;
	private static final Set<String> STATUS = Sets.newHashSet("Active", "Pending", "Withdrawn");

	public GetOrganisationsExportOpenDataValidator() {
		parts = new CommonParts(this);
	}

	@Override
	public Environment evaluate(Environment environment) {
		JsonObject body = bodyFrom(environment);
		assertField(body,
			new ObjectArrayField
				.Builder("data")
				.setValidator(this::assertExportOpenData)
				.build());

		return environment;
	}

	private void assertExportOpenData(JsonObject content) {
		assertField(content,
			new StringField
				.Builder("OrganisationId")
				.setMinLength(1)
				.setMaxLength(40)
				.setOptional()
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
				.setOptional()
				.build());

		assertField(content,
			new StringField
				.Builder("CreatedOn")
				.setMaxLength(30)
				.setOptional()
				.build());

		assertField(content,
			new StringField
				.Builder("LegalEntityName")
				.setMaxLength(255)
				.setOptional()
				.build());

		assertField(content,
			new StringField
				.Builder("CountryOfRegistration")
				.setMaxLength(255)
				.setOptional()
				.build());

		assertField(content,
			new StringField
				.Builder("CompanyRegister")
				.setMaxLength(255)
				.setOptional()
				.build());

		assertField(content,
			new StringField
				.Builder("Tag")
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
				.setOptional()
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
				.setOptional()
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
				.setOptional()
				.build());

		assertField(content,
			new StringField
				.Builder("Postcode")
				.setMaxLength(40)
				.setOptional()
				.build());

		assertField(content,
			new StringField
				.Builder("Country")
				.setMaxLength(255)
				.setOptional()
				.build());

		assertField(content,
			new StringField
				.Builder("ParentOrganisationReference")
				.setMaxLength(65535)
				.setOptional()
				.build());

		assertField(content,
			new ObjectArrayField
				.Builder("Contacts")
				.setValidator(parts::assertExportContacts)
				.setOptional()
				.build());

		assertField(content,
			new ObjectArrayField
				.Builder("AuthorisationServers")
				.setValidator(parts::assertAuthorisationServers)
				.setOptional()
				.build());

		assertField(content,
			new ObjectArrayField
				.Builder("OrgDomainClaims")
				.setValidator(parts::assertOrgDomainClaims)
				.setOptional()
				.build());

		assertField(content,
			new ObjectArrayField
				.Builder("OrgDomainRoleClaims")
				.setValidator(parts::assertOrgDomainRoleClaims)
				.setOptional()
				.build());
	}
}
