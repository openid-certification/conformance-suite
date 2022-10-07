package net.openid.conformance.condition.client;

import com.google.common.base.Strings;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class FAPIBrazilOpenInsuranceVerifyCertificateSubjectOrganizationIdentifier extends AbstractCondition {

	@Override
	@PreEnvironment(required = "certificate_subject")
	public Environment evaluate(Environment env) {
		String subjectDn = env.getString("certificate_subject", "subjectdn");

		String ou = env.getString("certificate_subject", "org_type");
		if (Strings.isNullOrEmpty(ou)) {
			throw error("Certificate organization identifier field not found/empty in the subjectdn within the provided MTLS certificate; a valid OpenInsurance certificate must be used for testing", args("subjectdn", subjectDn));
		}

		String expected = "OPIBR";
		if (!ou.equals(expected)) {
			throw error("Certificate organization identifier field (extracted from the MTLS certificate subjectdn) is not prefixed by '"+expected+"'; a valid OpenInsurance certificate must be used for testing",
				args("org_type", ou, "subjectdn", subjectDn));
		}

		log("Certificate organization identifier field contains expected "+expected, args("org_type", ou, "subjectdn", subjectDn));

		return env;
	}
}
