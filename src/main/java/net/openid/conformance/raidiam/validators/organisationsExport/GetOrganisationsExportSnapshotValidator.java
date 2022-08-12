package net.openid.conformance.raidiam.validators.organisationsExport;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.client.jsonAsserting.AbstractJsonAssertingCondition;
import net.openid.conformance.logging.ApiName;
import net.openid.conformance.raidiam.validators.CommonParts;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.util.field.ObjectArrayField;
import net.openid.conformance.util.field.ObjectField;

/**
 * Api url: ****
 * Api endpoint: GET /organisations/snapshot
 * Api git hash: ****
 *
 */
@ApiName("Raidiam Directory Get Organisations Export Snapshot")
public class GetOrganisationsExportSnapshotValidator extends AbstractJsonAssertingCondition {

	private final CommonParts parts;

	public GetOrganisationsExportSnapshotValidator() {
		parts = new CommonParts(this);
	}

	@Override
	public Environment evaluate(Environment environment) {
		JsonElement body = bodyFrom(environment);
		for (String key : body.getAsJsonObject().keySet()) {
			assertField(body,
				new ObjectField
					.Builder(key)
					.setValidator(this::assertSnapshot)
					.build());
		}
		return environment;
	}

	private void assertSnapshot(JsonObject snapshot) {
		assertField(snapshot,
			new ObjectField
				.Builder("OrganisationDetails")
				.setValidator(parts::organisationContent)
				.setOptional()
				.build());

		assertField(snapshot,
			new ObjectArrayField
				.Builder("Contacts")
				.setValidator(parts::assertExportContacts)
				.setOptional()
				.build());

		assertField(snapshot,
			new ObjectArrayField
				.Builder("AuthorisationServers")
				.setValidator(parts::assertAuthorisationServers)
				.setOptional()
				.build());

		assertField(snapshot,
			new ObjectArrayField
				.Builder("OrgDomainClaims")
				.setValidator(parts::assertOrgDomainClaims)
				.setOptional()
				.build());

		assertField(snapshot,
			new ObjectArrayField
				.Builder("OrgDomainRoleClaims")
				.setValidator(parts::assertOrgDomainRoleClaims)
				.setOptional()
				.build());

		assertField(snapshot,
			new ObjectField
				.Builder("SoftwareStatements")
				.setValidator(this::assertSoftwareStatements)
				.setOptional()
				.build());

		assertField(snapshot,
			new ObjectArrayField
				.Builder("OrganisationCertificates")
				.setValidator(parts::assertCertificates)
				.setOptional()
				.build());
	}

	protected void assertSoftwareStatements(JsonObject softwareStatements) {
		for (String key : softwareStatements.keySet()) {
			assertField(softwareStatements,
				new ObjectField
					.Builder(key)
					.setValidator(data -> {
						assertField(data,
							new ObjectField
								.Builder("SoftwareDetails")
								.setValidator(parts::assertSoftwareDetails)
								.setOptional()
								.build());

						assertField(data,
							new ObjectArrayField
								.Builder("SoftwareAuthorityClaims")
								.setValidator(parts::assertSoftwareAuthorityClaims)
								.setOptional()
								.build());

						assertField(data,
							new ObjectArrayField
								.Builder("SoftwareCertificates")
								.setValidator(parts::assertCertificates)
								.setOptional()
								.build());
					})
					.setOptional()
					.build());
		}
	}
}
