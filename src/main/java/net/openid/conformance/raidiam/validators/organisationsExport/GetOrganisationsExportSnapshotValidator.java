package net.openid.conformance.raidiam.validators.organisationsExport;

import com.google.common.collect.Sets;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.client.AbstractJsonAssertingCondition;
import net.openid.conformance.logging.ApiName;
import net.openid.conformance.raidiam.validators.CommonFields;
import net.openid.conformance.raidiam.validators.Utils;
import net.openid.conformance.raidiam.validators.CommonParts;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.util.field.BooleanField;
import net.openid.conformance.util.field.IntField;
import net.openid.conformance.util.field.ObjectArrayField;
import net.openid.conformance.util.field.ObjectField;
import net.openid.conformance.util.field.StringArrayField;
import net.openid.conformance.util.field.StringField;

import java.util.Set;

/**
 * Api url: ****
 * Api endpoint: GET /organisations/snapshot
 * Api git hash: ****
 *
 */
@ApiName("Raidiam Directory Get Organisations Export Snapshot")
public class GetOrganisationsExportSnapshotValidator extends AbstractJsonAssertingCondition {

	private final CommonParts parts;
	private static final Set<String> MODE = Sets.newHashSet("Live", "Test");

	public GetOrganisationsExportSnapshotValidator() {
		parts = new CommonParts(this);
	}

	@Override
	public Environment evaluate(Environment environment) {
		JsonObject body = bodyFrom(environment);
		Utils.convertJsonMapToJsonArray(body, "");
		assertField(body,
			new ObjectArrayField
				.Builder("data")
				.setValidator(this::assertSnapshot)
				.build());

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
				.setValidator(this::assertCertificates)
				.setOptional()
				.build());
	}

	protected void assertSoftwareStatements(JsonObject softwareStatements) {
		assertField(softwareStatements,
			new ObjectField
				.Builder("SoftwareDetails")
				.setValidator(this::assertSoftwareDetails)
				.setOptional()
				.build());

		assertField(softwareStatements,
			new ObjectArrayField
				.Builder("SoftwareAuthorityClaims")
				.setValidator(this::assertSoftwareAuthorityClaims)
				.setOptional()
				.build());

		assertField(softwareStatements,
		new ObjectArrayField
			.Builder("SoftwareCertificates")
			.setValidator(this::assertCertificates)
			.setOptional()
			.build());
	}

	protected void assertCertificates(JsonObject softwareCertificates) {
		assertField(softwareCertificates, CommonFields.getOrganisationId());

		assertField(softwareCertificates,
			new StringArrayField
				.Builder("SoftwareStatementIds")
				.setMaxLength(40)
				.setOptional()
				.build());

		assertField(softwareCertificates,
			new StringField
				.Builder("ClientName")
				.setMaxLength(40)
				.setOptional()
				.build());

		assertField(softwareCertificates,
			new StringField
				.Builder("Status")
				.setMaxLength(40)
				.setOptional()
				.build());

		assertField(softwareCertificates,
			new StringField
				.Builder("ValidFromDateTime")
				.setMaxLength(30)
				.setOptional()
				.build());

		assertField(softwareCertificates,
			new StringField
				.Builder("ExpiryDateTime")
				.setMaxLength(30)
				.setOptional()
				.build());

		assertField(softwareCertificates,
			new StringField
				.Builder("e")
				.setMaxLength(255)
				.setOptional()
				.build());

		assertField(softwareCertificates,
			new StringField
				.Builder("keyType")
				.setMaxLength(255)
				.setOptional()
				.build());

		assertField(softwareCertificates,
			new StringField
				.Builder("kid")
				.setMaxLength(255)
				.setOptional()
				.build());

		assertField(softwareCertificates,
			new StringField
				.Builder("kty")
				.setMaxLength(255)
				.setOptional()
				.build());

		assertField(softwareCertificates,
			new StringField
				.Builder("n")
				.setMaxLength(255)
				.setOptional()
				.build());

		assertField(softwareCertificates,
			new StringField
				.Builder("use")
				.setMaxLength(255)
				.setOptional()
				.build());

		assertField(softwareCertificates,
			new StringArrayField
				.Builder("x5c")
				.setMaxLength(255)
				.setOptional()
				.build());

		assertField(softwareCertificates,
			new StringField
				.Builder("x5t")
				.setMaxLength(255)
				.setOptional()
				.build());

		assertField(softwareCertificates,
			new StringField
				.Builder("x5thashS256")
				.setMaxLength(255)
				.setOptional()
				.build());

		assertField(softwareCertificates,
			new StringField
				.Builder("x5u")
				.setMaxLength(255)
				.setOptional()
				.build());

		assertField(softwareCertificates,
			new StringField
				.Builder("SignedCertPath")
				.setMaxLength(255)
				.setOptional()
				.build());

		assertField(softwareCertificates,
			new StringField
				.Builder("JwkPath")
				.setMaxLength(255)
				.setOptional()
				.build());

		assertField(softwareCertificates,
			new StringField
				.Builder("OrgJwkPath")
				.setMaxLength(255)
				.setOptional()
				.build());
	}

	private void assertSoftwareAuthorityClaims(JsonObject authorityClaims) {
		assertField(authorityClaims,
			new StringField
				.Builder("SoftwareStatementId")
				.setMaxLength(40)
				.setOptional()
				.build());

		assertField(authorityClaims,
			new StringField
				.Builder("SoftwareAuthorityClaimId")
				.setMaxLength(40)
				.setMinLength(1)
				.setOptional()
				.build());

		assertField(authorityClaims, CommonFields.getStatus());

		assertField(authorityClaims,
			new StringField
				.Builder("AuthorisationDomain")
				.setMaxLength(30)
				.setOptional()
				.build());

		assertField(authorityClaims,
			new StringField
				.Builder("Role")
				.setMaxLength(10)
				.setOptional()
				.build());
	}

	private void assertSoftwareDetails(JsonObject softwareDetails) {
		assertField(softwareDetails, CommonFields.getStatus());

		assertField(softwareDetails,
			new StringField
				.Builder("ClientId")
				.setMaxLength(40)
				.setOptional()
				.build());

		assertField(softwareDetails,
			new StringField
				.Builder("ClientName")
				.setMaxLength(255)
				.setOptional()
				.build());

		assertField(softwareDetails,
			new StringField
				.Builder("Description")
				.setMaxLength(255)
				.setOptional()
				.build());

		assertField(softwareDetails,
			new StringField
				.Builder("Environment")
				.setMaxLength(40)
				.setOptional()
				.build());

		assertField(softwareDetails, CommonFields.getOrganisationId());

		assertField(softwareDetails,
			new StringField
				.Builder("SoftwareStatementId")
				.setMaxLength(40)
				.setOptional()
				.build());

		assertField(softwareDetails,
			new StringField
				.Builder("Mode")
				.setMaxLength(8)
				.setEnums(MODE)
				.setOptional()
				.build());

		assertField(softwareDetails,
			new BooleanField
				.Builder("RtsClientCreated")
				.setOptional()
				.build());

		assertField(softwareDetails,
			new StringField
				.Builder("OnBehalfOf")
				.setMaxLength(255)
				.setOptional()
				.build());

		assertField(softwareDetails,
			new StringField
				.Builder("PolicyUri")
				.setMaxLength(255)
				//.setPattern("^(http:\\/\\/|https:\\/\\/).*")
				.setOptional()
				.build());

		assertField(softwareDetails,
			new StringField
				.Builder("ClientUri")
				.setMaxLength(255)
				//.setPattern("^(http:\\/\\/|https:\\/\\/).*")
				.setOptional()
				.build());

		assertField(softwareDetails,
			new StringField
				.Builder("LogoUri")
				.setMaxLength(255)
				//.setPattern("^(http:\\/\\/|https:\\/\\/).*")
				.setOptional()
				.build());

		assertField(softwareDetails,
			new StringArrayField
				.Builder("RedirectUri")
				//.setPattern("^(http:\\/\\/|https:\\/\\/).*")
				.setMaxLength(255)
				.setOptional()
				.build());

		assertField(softwareDetails,
			new StringField
				.Builder("TermsOfServiceUri")
				.setMaxLength(255)
				//.setPattern("^(http:\\/\\/|https:\\/\\/).*")
				.setOptional()
				.build());

		assertField(softwareDetails,
			new IntField
				.Builder("Version")
				.setMaxLength(40)
				.setOptional()
				.build());

		assertField(softwareDetails,
			new BooleanField
				.Builder("Locked")
				.setMaxLength(40)
				.setOptional()
				.build());

		assertField(softwareDetails,
			new StringField
				.Builder("AdditionalSoftwareMetadata")
				.setMaxLength(255)
				.setOptional()
				.build());
	}
}
