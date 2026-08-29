package net.openid.conformance.condition.client;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.util.MdocUtil;
import net.openid.conformance.util.MdocValueConstraint;
import org.multipaz.cbor.Cbor;
import org.multipaz.cbor.DataItem;
import org.multipaz.cbor.Tagged;

import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Checks the timestamps in the MSO's ValidityInfo structure are encoded as ISO/IEC 18013-5
 * 12.3.4 and ISO/IEC TS 23220-4 A.1.2.4.2 both require: "The timestamps in the ValidityInfo
 * structure shall not use fractions of seconds and shall use a UTC offset of 00:00, as indicated
 * by the character 'Z'". Works on the raw CBOR, since parsing the MSO into date objects would
 * hide how the timestamps were represented.
 *
 * Applies to every mdoc docType — both specifications state the rule for their MSO.
 */
public class ValidateMdocMsoValidityInfoTimestamps extends AbstractCondition {

	@Override
	@PreEnvironment(strings = "mdoc_credential_cbor")
	public Environment evaluate(Environment env) {

		DataItem validityInfo;
		try {
			DataItem issuerSigned = Cbor.INSTANCE.decode(
				Base64.getDecoder().decode(env.getString("mdoc_credential_cbor")));
			validityInfo = MdocUtil.parseMsoDataItem(issuerSigned).getOrNull("validityInfo");
		} catch (MdocUtil.MdocParseException e) {
			throw error(e.getMessage(), e);
		} catch (Exception e) {
			throw error("Failed to parse the mdoc credential", e);
		}

		if (validityInfo == null) {
			throw error("The MSO does not contain the validityInfo structure the specification requires");
		}

		Map<String, String> problems = new TreeMap<>();
		Map<String, String> timestamps = new LinkedHashMap<>();

		for (String name : List.of("signed", "validFrom", "validUntil", "expectedUpdate")) {
			DataItem timestamp = validityInfo.getOrNull(name);
			if (timestamp == null) {
				// only expectedUpdate is optional in the ValidityInfo CDDL
				if (!"expectedUpdate".equals(name)) {
					problems.put(name, "is missing");
				}
				continue;
			}
			String problem = MdocValueConstraint.tdate().check(timestamp);
			if (problem == null) {
				String text = ((Tagged) timestamp).getTaggedItem().getAsTstr();
				timestamps.put(name, text);
				if (text.contains(".")) {
					problem = "'" + text + "' uses fractions of seconds";
				} else if (!text.endsWith("Z") && !text.endsWith("z")) {
					problem = "'" + text + "' does not use the UTC offset 'Z'";
				}
			}
			if (problem != null) {
				problems.put(name, problem);
			}
		}

		if (!problems.isEmpty()) {
			throw error("The timestamps in the MSO validityInfo structure must be tdate values with "
					+ "no fractions of seconds and a UTC offset of 'Z'",
				args("problems", problems));
		}

		logSuccess("The MSO validityInfo timestamps are tdate values with no fractions of seconds "
			+ "and a UTC offset of 'Z'", args("timestamps", timestamps));

		return env;
	}
}
