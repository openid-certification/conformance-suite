package net.openid.conformance.settings;

import org.bson.Document;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;

/**
 * The stored Chile CMF Directorio section of the server settings, exactly as saved: the client
 * secret, the JWKS with its private members and the private-key PEMs are all in here, so an
 * instance must never reach a browser or a test log. {@link CMFChileSettingsView} is the browser's
 * form and {@link CMFChileDirectorySettings} the test modules'.
 *
 * @param clientJwks the JWKS as a JSON string
 */
public record CMFChileSettings(
	String directoryTokenEndpoint,
	String softwareStatementEndpoint,
	String clientId,
	String clientSecret,
	String clientJwks,
	List<CertificateEntry> positiveCertificates,
	List<CertificateEntry> negativeCertificates,
	Instant updatedAt,
	String updatedBy) {

	public static final String SECTION_ID = "cmf-chile";

	public static final String DIRECTORY_TOKEN_ENDPOINT = "directoryTokenEndpoint";
	public static final String SOFTWARE_STATEMENT_ENDPOINT = "softwareStatementEndpoint";
	public static final String CLIENT_ID = "clientId";
	public static final String CLIENT_SECRET = "clientSecret";
	public static final String CLIENT_JWKS = "clientJwks";
	public static final String POSITIVE_CERTIFICATES = "positiveCertificates";
	public static final String NEGATIVE_CERTIFICATES = "negativeCertificates";
	public static final String UPDATED_AT = "updatedAt";
	public static final String UPDATED_BY = "updatedBy";

	private static final String ENTRY_ID = "id";
	private static final String ENTRY_LABEL = "label";
	private static final String ENTRY_CERTIFICATE_CHAIN = "certificateChainPem";
	private static final String ENTRY_PRIVATE_KEY = "privateKeyPem";

	public CMFChileSettings {
		positiveCertificates = positiveCertificates == null ? List.of() : List.copyOf(positiveCertificates);
		negativeCertificates = negativeCertificates == null ? List.of() : List.copyOf(negativeCertificates);
	}

	public static CMFChileSettings empty() {
		return new CMFChileSettings(null, null, null, null, null, List.of(), List.of(), null, null);
	}

	public CMFChileSettings withAudit(Instant when, String who) {
		return new CMFChileSettings(directoryTokenEndpoint, softwareStatementEndpoint, clientId, clientSecret, clientJwks,
			positiveCertificates, negativeCertificates, when, who);
	}

	/**
	 * @return the names of the configurable fields whose values differ from {@code previous}
	 */
	public List<String> changedFields(CMFChileSettings previous) {
		List<String> changed = new ArrayList<>();
		addIfChanged(changed, DIRECTORY_TOKEN_ENDPOINT, directoryTokenEndpoint, previous.directoryTokenEndpoint);
		addIfChanged(changed, SOFTWARE_STATEMENT_ENDPOINT, softwareStatementEndpoint, previous.softwareStatementEndpoint);
		addIfChanged(changed, CLIENT_ID, clientId, previous.clientId);
		addIfChanged(changed, CLIENT_SECRET, clientSecret, previous.clientSecret);
		addIfChanged(changed, CLIENT_JWKS, clientJwks, previous.clientJwks);
		addIfChanged(changed, POSITIVE_CERTIFICATES, positiveCertificates, previous.positiveCertificates);
		addIfChanged(changed, NEGATIVE_CERTIFICATES, negativeCertificates, previous.negativeCertificates);
		return changed;
	}

	private static void addIfChanged(List<String> changed, String field, Object current, Object previous) {
		if (!Objects.equals(current, previous)) {
			changed.add(field);
		}
	}

	public Document toDocument() {
		return new Document("_id", SECTION_ID)
			.append(DIRECTORY_TOKEN_ENDPOINT, directoryTokenEndpoint)
			.append(SOFTWARE_STATEMENT_ENDPOINT, softwareStatementEndpoint)
			.append(CLIENT_ID, clientId)
			.append(CLIENT_SECRET, clientSecret)
			.append(CLIENT_JWKS, clientJwks)
			.append(POSITIVE_CERTIFICATES, entriesToDocuments(positiveCertificates))
			.append(NEGATIVE_CERTIFICATES, entriesToDocuments(negativeCertificates))
			.append(UPDATED_AT, updatedAt == null ? null : Date.from(updatedAt))
			.append(UPDATED_BY, updatedBy);
	}

	public static CMFChileSettings fromDocument(Document document) {
		Date updatedAt = document.getDate(UPDATED_AT);
		return new CMFChileSettings(
			document.getString(DIRECTORY_TOKEN_ENDPOINT),
			document.getString(SOFTWARE_STATEMENT_ENDPOINT),
			document.getString(CLIENT_ID),
			document.getString(CLIENT_SECRET),
			document.getString(CLIENT_JWKS),
			entriesFromDocuments(document.getList(POSITIVE_CERTIFICATES, Document.class)),
			entriesFromDocuments(document.getList(NEGATIVE_CERTIFICATES, Document.class)),
			updatedAt == null ? null : updatedAt.toInstant(),
			document.getString(UPDATED_BY));
	}

	private static List<Document> entriesToDocuments(List<CertificateEntry> entries) {
		return entries.stream()
			.map(entry -> new Document(ENTRY_ID, entry.id())
				.append(ENTRY_LABEL, entry.label())
				.append(ENTRY_CERTIFICATE_CHAIN, entry.certificateChainPem())
				.append(ENTRY_PRIVATE_KEY, entry.privateKeyPem()))
			.toList();
	}

	private static List<CertificateEntry> entriesFromDocuments(List<Document> documents) {
		if (documents == null) {
			return List.of();
		}
		return documents.stream()
			.map(document -> new CertificateEntry(
				document.getString(ENTRY_ID),
				document.getString(ENTRY_LABEL),
				document.getString(ENTRY_CERTIFICATE_CHAIN),
				document.getString(ENTRY_PRIVATE_KEY)))
			.toList();
	}

	@Override
	public String toString() {
		// the generated toString would print the client secret and private keys
		return "CMFChileSettings[directoryTokenEndpoint=" + directoryTokenEndpoint
			+ ", softwareStatementEndpoint=" + softwareStatementEndpoint
			+ ", clientId=" + clientId
			+ ", positiveCertificates=" + positiveCertificates
			+ ", negativeCertificates=" + negativeCertificates
			+ ", updatedAt=" + updatedAt
			+ ", updatedBy=" + updatedBy + "]";
	}
}
