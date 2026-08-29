package net.openid.conformance.condition.client;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.util.MdocUtil;
import org.multipaz.cbor.Cbor;
import org.multipaz.cbor.CborMap;
import org.multipaz.cbor.DataItem;
import org.multipaz.cbor.Tagged;
import org.multipaz.cbor.Tstr;
import org.multipaz.mdoc.mso.MobileSecurityObject;

import java.time.Instant;
import java.time.LocalDate;
import java.time.Period;
import java.time.ZoneOffset;
import java.time.format.DateTimeParseException;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Base class for checks that an mdoc's age attestation data elements agree with its birth_date.
 *
 * ISO/IEC 18013-5 13.4.6 and ISO/IEC TS 23220-2 6.3.2.2 both say the value of an age_over_NN
 * element "shall be calculated by the issuing authority infrastructure to be valid at the value
 * of the timestamp in the validFrom element in the MSO", which also makes validFrom the natural
 * anchor for age_in_years. Anchoring at validFrom rather than at the time of checking means a
 * credential that has aged since issuance is still judged against the ages that were correct
 * when it was issued.
 *
 * birth_date is a date with no timezone while validFrom is an instant, so the holder's age is
 * computed at a day either side of validFrom as well, and a value consistent with any of the
 * three is accepted.
 *
 * The check runs for every namespace containing a birth_date element, whatever the docType, and
 * checks nothing when birth_date or the age elements are absent — in a presentation selective
 * disclosure can legitimately omit any of them. A birth_date or age element whose encoding is
 * invalid is skipped here; the element value checks report those.
 */
public abstract class AbstractEnsureMdocAgeElementsConsistentWithBirthDate extends AbstractCondition {

	/**
	 * Checks this subclass's age elements against the holder's age range at the MSO validFrom,
	 * adding a description per inconsistent element to problems, and returns how many elements
	 * it checked.
	 */
	protected abstract int checkAgeElements(String namespace, Map<String, DataItem> elements,
		LocalDate birthDate, int minAge, int maxAge, Map<String, String> problems);

	/** The elements the subclass checks, for log messages, e.g. "age_over_NN". */
	protected abstract String getElementsDescription();

	@Override
	@PreEnvironment(strings = "mdoc_credential_cbor")
	public Environment evaluate(Environment env) {

		DataItem issuerSigned;
		MobileSecurityObject mso;
		Map<String, List<DataItem>> namespaces;
		try {
			issuerSigned = Cbor.INSTANCE.decode(Base64.getDecoder().decode(env.getString("mdoc_credential_cbor")));
			mso = MdocUtil.parseMso(issuerSigned);
			namespaces = MdocUtil.getIssuerSignedItems(issuerSigned);
		} catch (MdocUtil.MdocParseException e) {
			throw error(e.getMessage(), e);
		} catch (Exception e) {
			throw error("Failed to parse the mdoc credential", e);
		}

		LocalDate validFrom = LocalDate.ofInstant(
			Instant.ofEpochSecond(mso.getValidFrom().getEpochSeconds()), ZoneOffset.UTC);

		Map<String, String> problems = new TreeMap<>();
		int checked = 0;

		for (Map.Entry<String, List<DataItem>> entry : namespaces.entrySet()) {
			Map<String, DataItem> elements = new LinkedHashMap<>();
			for (DataItem issuerSignedItemBytes : entry.getValue()) {
				DataItem item = issuerSignedItemBytes.getAsTaggedEncodedCbor();
				elements.put(item.getOrNull("elementIdentifier").getAsTstr(), item.getOrNull("elementValue"));
			}
			LocalDate birthDate = parseBirthDate(elements.get("birth_date"));
			if (birthDate == null) {
				continue;
			}
			int minAge = Period.between(birthDate, validFrom.minusDays(1)).getYears();
			int maxAge = Period.between(birthDate, validFrom.plusDays(1)).getYears();
			checked += checkAgeElements(entry.getKey(), elements, birthDate, minAge, maxAge, problems);
		}

		if (!problems.isEmpty()) {
			throw error("The mdoc contains " + getElementsDescription() + " data elements that do not "
					+ "agree with its birth_date for the holder's age at the MSO validFrom",
				args("problems", problems, "valid_from", validFrom.toString()));
		}

		logSuccess("The mdoc's " + getElementsDescription() + " data elements agree with its birth_date",
			args("elements_checked", checked, "valid_from", validFrom.toString()));

		return env;
	}

	/**
	 * The birth date from a full-date element or the ISO/IEC TS 23220-2 birth_date structure, or
	 * null if it is absent or not validly encoded.
	 */
	private LocalDate parseBirthDate(DataItem birthDate) {
		if (birthDate instanceof CborMap) {
			birthDate = birthDate.getOrNull("birth_date");
		}
		if (!(birthDate instanceof Tagged tagged) || tagged.getTagNumber() != Tagged.FULL_DATE_STRING
				|| !(tagged.getTaggedItem() instanceof Tstr)) {
			return null;
		}
		try {
			return LocalDate.parse(tagged.getTaggedItem().getAsTstr());
		} catch (DateTimeParseException e) {
			return null;
		}
	}
}
