package net.openid.conformance.raidiam.validators.organisationsExport;

import com.google.gson.JsonObject;
import net.openid.conformance.logging.ApiName;
import net.openid.conformance.raidiam.validators.CommonParts;
import net.openid.conformance.raidiam.validators.Utils;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.util.field.ObjectArrayField;
import net.openid.conformance.util.field.ObjectField;

/**
 * This class corresponds to {@link GetOrganisationsExportSnapshotValidator}
 * Api url: ****
 * Api endpoint: GET /organisations/{OrganisationId}/snapshot
 * Api git hash: ****
 *
 */
@ApiName("Raidiam Directory Get Organisations Export Snapshot By OrganisationId")
public class GetOrganisationsExportSnapshotByOrganisationIdValidator extends GetOrganisationsExportSnapshotValidator {

	private final CommonParts parts;

	public GetOrganisationsExportSnapshotByOrganisationIdValidator() {
		parts = new CommonParts(this);
	}

	@Override
	public Environment evaluate(Environment environment) {
		JsonObject snapshot = bodyFrom(environment);

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

		Utils.convertJsonMapToJsonArray(snapshot, "SoftwareStatements");

		assertField(snapshot,
			new ObjectArrayField
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

		return  environment;
	}
}
