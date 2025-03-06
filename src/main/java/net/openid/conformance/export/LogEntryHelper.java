package net.openid.conformance.export;

import com.google.gson.Gson;
import org.bson.Document;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.TimeZone;
import java.util.regex.Pattern;

/**
 * Template rendering helper for a single log entry
 * Methods that seem "unused" are used in templates.
 */
public class LogEntryHelper {
	public static final List<String> blockColors = List.of("#ef476f", "#118ab2", "#073b4c", "#6d597a", "#03045e",
		"#0077b6", "#7209b7", "#3a0ca3", "#8a817c", "#9a031e", "#5f0f40", "#8900f2");
	public static final Set<String> visibleFields = Set.of(
		"_id",
		"_class",	//old test results contain _class elements with 'com.mongodb.BasicDBObject' values
		"msg", "src", "time", "result", "requirements", "upload",
		"testOwner", "testId", "http", "blockId", "startBlock");

	public static final Map<String, String> specLinks;
	static{
		specLinks = new HashMap<>();
		specLinks.put("BrazilOB-", "https://openfinancebrasil.atlassian.net/wiki/spaces/OF/pages/245760001/EN+Open+Finance+Brasil+Financial-grade+API+Security+Profile+1.0+Implementers+Draft+3#section-");
		specLinks.put("BrazilOBDCR-","https://openfinancebrasil.atlassian.net/wiki/spaces/OF/pages/246120449/EN+Open+Finance+Brasil+Financial-grade+API+Dynamic+Client+Registration+2.0+RC1+Implementers+Draft+3#section-");
		specLinks.put("BrazilOPIN-", "https://br-openinsurance.github.io/areadesenvolvedor/files/Controles_técnicos_de_Segurança_da_Informação_3.0.pdf#");
		specLinks.put("BrazilCIBA-", "https://openfinancebrasil.atlassian.net/wiki/spaces/DraftOF/pages/138674330/EN+Open+Banking+Brasil+Financial-grade+CIBA+API+Security+Profile+1.0+Implementers+Draft+3");
		specLinks.put("FAPI-R-", "https://openid.net/specs/openid-financial-api-part-1-ID2.html#rfc.section.");
		specLinks.put("FAPI-RW-", "https://openid.net/specs/openid-financial-api-part-2-ID2.html#rfc.section.");
		specLinks.put("FAPI1-BASE-", "https://openid.net/specs/openid-financial-api-part-1-1_0-final.html#rfc.section.");
		specLinks.put("FAPI1-ADV-", "https://openid.net/specs/openid-financial-api-part-2-1_0-final.html#rfc.section.");
		specLinks.put("FAPI2-SP-ID2-", "https://openid.net/specs/fapi-2_0-security-profile-ID2.html#section-");
		specLinks.put("FAPI2-MS-ID1-", "https://openid.net/specs/fapi-2_0-message-signing-ID1.html#section..");
		specLinks.put("FAPI2-IMP-", "https://openid.bitbucket.io/fapi/fapi-2_0-implementation_advice.html#section-");
		specLinks.put("CIBA-", "https://openid.net/specs/openid-client-initiated-backchannel-authentication-core-1_0.html#rfc.section.");
		specLinks.put("FAPI-CIBA-", "https://openid.net/specs/openid-financial-api-ciba.html#rfc.section.");
		specLinks.put("JARM-", "https://openid.net/specs/oauth-v2-jarm.html#section-");
		specLinks.put("OB-", "https://bitbucket.org/openid/obuk/src/b36035c22e96ce160524066c7fde9a45cbaeb949/uk-openbanking-security-profile.md?at=master&fileviewer=file-view-default#");
		specLinks.put("OBRW-", "https://openbanking.atlassian.net/wiki/spaces/DZ/pages/1077805207/Read+Write+Data+API+Specification+-+v3.1.2#");
		specLinks.put("OIDCC-", "https://openid.net/specs/openid-connect-core-1_0.html#rfc.section.");
		specLinks.put("OIDCR-", "https://openid.net/specs/openid-connect-registration-1_0.html#rfc.section.");
		specLinks.put("OAuth2-FP", "https://openid.net/specs/oauth-v2-form-post-response-mode-1_0.html");
		specLinks.put("OAuth2-iss-", "https://tools.ietf.org/html/rfc9207#section-");
		specLinks.put("RFC3986-", "https://tools.ietf.org/html/rfc3986#section-");
		specLinks.put("RFC6749-", "https://tools.ietf.org/html/rfc6749#section-");
		specLinks.put("RFC6749A-", "https://tools.ietf.org/html/rfc6749#appendix-");
		specLinks.put("RFC6750-", "https://tools.ietf.org/html/rfc6750#section-");
		specLinks.put("RFC6819-", "https://tools.ietf.org/html/rfc6819#section-");
		specLinks.put("RFC7231-", "https://tools.ietf.org/html/rfc7231#section-");
		specLinks.put("RFC7517-", "https://tools.ietf.org/html/rfc7517#section-");
		specLinks.put("RFC7519-", "https://tools.ietf.org/html/rfc7519#section-");
		specLinks.put("RFC7523-", "https://tools.ietf.org/html/rfc7523#section-");
		specLinks.put("RFC7591-", "https://tools.ietf.org/html/rfc7591#section-");
		specLinks.put("RFC7592-", "https://tools.ietf.org/html/rfc7592#section-");
		specLinks.put("RFC7592A-", "https://tools.ietf.org/html/rfc7592#appendix-");
		specLinks.put("RFC7636-", "https://tools.ietf.org/html/rfc7636#section-");
		specLinks.put("RFC8705-", "https://tools.ietf.org/html/rfc8705#section-");
		specLinks.put("RFC8707-", "https://tools.ietf.org/html/rfc8707#section-");
		specLinks.put("RFC8485-", "https://tools.ietf.org/html/rfc8485#section-");
		specLinks.put("RFC9325-", "https://tools.ietf.org/html/rfc9325.html#section-");
		specLinks.put("RFC9325A-", "https://tools.ietf.org/html/rfc9325.html#appendix-");
		specLinks.put("RFC9396-", "https://tools.ietf.org/html/rfc9396#section-");
		specLinks.put("OBSP-", "https://openbanking.atlassian.net/wiki/spaces/DZ/pages/83919096/Open+Banking+Security+Profile+-+Implementer+s+Draft+v1.1.2#");
		specLinks.put("OAuth2-RT-", "https://openid.net/specs/oauth-v2-multiple-response-types-1_0.html#rfc.section.");
		specLinks.put("OID4VP-ID2-", "https://openid.net/specs/openid-4-verifiable-presentations-1_0-ID2.html#section.");
		specLinks.put("OID4VP-ID3-", "https://openid.net/specs/openid-4-verifiable-presentations-1_0-ID3.html#section.");
		specLinks.put("HAIP-", "https://github.com/vcstuff/oid4vc-haip-sd-jwt-vc/blob/main/draft-oid4vc-haip-sd-jwt-vc.md#");
		specLinks.put("OIDCD-", "https://openid.net/specs/openid-connect-discovery-1_0.html#rfc.section.");
		specLinks.put("OIDCBCL-", "https://openid.net/specs/openid-connect-backchannel-1_0.html#rfc.section.");
		specLinks.put("OIDCFCL-", "https://openid.net/specs/openid-connect-frontchannel-1_0.html#rfc.section.");
		specLinks.put("OIDCSM-", "https://openid.net/specs/openid-connect-session-1_0.html#rfc.section.");
		specLinks.put("OIDCRIL-", "https://openid.net/specs/openid-connect-rpinitiated-1_0.html#rfc.section.");
		specLinks.put("BCP195-", "https://tools.ietf.org/html/bcp195#section-");
		specLinks.put("ISO18013-7-", "https://www.iso.org/standard/82772.html#");
		specLinks.put("CDR-", "https://consumerdatastandardsaustralia.github.io/standards/#");
		specLinks.put("PAR-", "https://www.rfc-editor.org/rfc/rfc9126.html#section-");
		specLinks.put("JAR-", "https://www.rfc-editor.org/rfc/rfc9101.html#section-");
		specLinks.put("SDJWT-", "https://www.ietf.org/archive/id/draft-ietf-oauth-selective-disclosure-jwt-14.html#section-");
		specLinks.put("SDJWTVC-", "https://datatracker.ietf.org/doc/draft-ietf-oauth-sd-jwt-vc/#section-");
		specLinks.put("IA-", "https://openid.net/specs/openid-connect-4-identity-assurance-1_0.htm#section-");
		specLinks.put("IAVC-", "https://openid.net/specs/openid-ida-verified-claims-1_0.htm#section-");
		specLinks.put("DPOP-", "https://www.rfc-editor.org/rfc/rfc9449#section-");
		specLinks.put("KSA", "https://ksaob.atlassian.net/wiki/spaces/KS20221101finalerrata1/pages/61014862/API+Security");
		specLinks.put("OIDSSF-", "https://openid.net/specs/openid-sharedsignals-framework-1_0-ID3.html#section-");
		specLinks.put("CAEPIOP-", "https://openid.net/specs/openid-caep-interoperability-profile-1_0-ID1.html#section-");
		specLinks.put("OIDCAEP-", "https://openid.net/specs/openid-caep-1_0-ID2.html#section-");
		specLinks.put("CID-SP-", "https://cdn.connectid.com.au/specifications/digitalid-fapi-profile-01.html#section");
		specLinks.put("CID-IDA-", "https://cdn.connectid.com.au/specifications/digitalid-identity-assurance-profile-06.html#section");
		specLinks.put("CID-PURPOSE-", "https://cdn.connectid.com.au/specifications/oauth2-purpose-01.html#section");
		specLinks.put("OIDFED-", "https://openid.net/specs/openid-federation-1_0-42.html#section-");
	}

	private Document logEntry;
	private Map<String, Object> more = new LinkedHashMap<>();
	private Object stackTrace = null;
	private Object causeStackTrace = null;
	private boolean doubleStackTrace;

	private Pattern jwtPattern = Pattern.compile("^(e[yw][a-zA-Z0-9_-]+)\\.([a-zA-Z0-9_-]+)\\.([a-zA-Z0-9_-]+)(\\.([a-zA-Z0-9_-]+)\\.([a-zA-Z0-9_-]+))?$");
	private Gson gson;

	public LogEntryHelper(Document logEntry, Gson collapsingGsonHttpMessageConverter) {
		this.logEntry = logEntry;
		this.gson = collapsingGsonHttpMessageConverter;
		for(String field : logEntry.keySet()) {
			if(!visibleFields.contains(field)) {
				//assume that there can be only 1 stack trace and 1 cause_stacktrace
				if("stacktrace".equals(field)) {
					this.stackTrace = logEntry.get(field);
				} else if("cause_stacktrace".equals(field)) {
					this.causeStackTrace = logEntry.get(field);
				}
				this.more.put(field, logEntry.get(field));
			}
		}
		if(this.stackTrace!=null && this.causeStackTrace!=null) {
			this.doubleStackTrace = true;
			//otherwise they would be printed twice
			this.more.remove("stacktrace");
			this.more.remove("cause_stacktrace");
		}
	}

	public Object get(String key) {
		return logEntry.get(key);
	}

	public String getMoreFieldType(String key, Object fieldValue) {
		if("img".equals(key)) {
			return "img";
		}
		if(fieldValue==null) {
			return "";
		}

		if(!isDoubleStackTrace() && ("stacktrace".equals(key) || "cause_stacktrace".equals(key))) {
			return "exception";
		}
		if(fieldValue instanceof Document doc) {
			if(doc.containsKey("verifiable_jws")) {
				return "verifiable_jws";
			}
			return "json";
		}
		if(fieldValue instanceof String) {
			if (isJwt(fieldValue))
			{
				return "jwt";
			}
			return "text";
		}
		if(fieldValue instanceof Number) {
			return "text";
		}
		//json encode by default
		return "json";
	}

	public boolean isJwt(Object value) {
		String str = String.valueOf(value);
		return jwtPattern.matcher(str).matches();
	}

	public String[] splitJwt(String jwtString) {
		return jwtString.split("\\.");
	}

	public void setMore(Map<String, Object> more)
	{
		this.more = more;
	}

	public Object getStackTrace()
	{
		return stackTrace;
	}

	public void setStackTrace(Object stackTrace)
	{
		this.stackTrace = stackTrace;
	}

	public Object getCauseStackTrace()
	{
		return causeStackTrace;
	}

	public void setCauseStackTrace(Object causeStackTrace)
	{
		this.causeStackTrace = causeStackTrace;
	}

	public boolean isDoubleStackTrace()
	{
		return doubleStackTrace;
	}

	public void setDoubleStackTrace(boolean doubleStackTrace)
	{
		this.doubleStackTrace = doubleStackTrace;
	}

	public Map<String, Object> getMore()
	{
		return more;
	}

	public String getTime() {
		Object timeObject = logEntry.get("time");
		Date timeAsDate = new Date(Long.valueOf(String.valueOf(timeObject)));
		TimeZone timeZone = TimeZone.getTimeZone("UTC");
		DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.ENGLISH);
		dateFormat.setTimeZone(timeZone);
		String formatted = dateFormat.format(timeAsDate);
		return formatted;
	}

	public String formatJson(Object object) {
		return gson.toJson(object).trim();
	}

	public String getRequirementLink(String requirement) {
		for(String key : specLinks.keySet()) {
			if(requirement.startsWith(key)) {
				return specLinks.get(key) + requirement.substring(key.length());
			}
		}
		return "";
	}

	public boolean isBeginNewBlock() {
		return logEntry.containsKey("blockId") && logEntry.containsKey("startBlock") && logEntry.getBoolean("startBlock");
	}

	public String getBlockColor() {
		Random random = new Random();
		int index = random.nextInt(blockColors.size());
		return blockColors.get(index);
	}

	public String getLogEntryResultClass() {
		String result = logEntry.getString("result");
		if(result==null || result.isEmpty()) {
			return "label result-unknown";
		}
		return "label result-" + result.toLowerCase(Locale.ENGLISH);
	}

	public String getLogEntryResult() {
		String result = logEntry.getString("result");
		if(result==null || result.isEmpty()) {
			return null;
		}
		return result;
	}
}
