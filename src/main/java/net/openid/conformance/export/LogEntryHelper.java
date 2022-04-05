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
		specLinks.put("BrazilOB-", "https://openbanking-brasil.github.io/specs-seguranca/open-banking-brasil-financial-api-1_ID1.html#section-");
		specLinks.put("BrazilOBDCR-","https://openbanking-brasil.github.io/specs-seguranca/open-banking-brasil-dynamic-client-registration-1_ID1.html#section-");
		specLinks.put("FAPI-R-", "https://openid.net/specs/openid-financial-api-part-1-ID2.html#rfc.section.");
		specLinks.put("FAPI-RW-", "https://openid.net/specs/openid-financial-api-part-2-ID2.html#rfc.section.");
		specLinks.put("FAPI1-BASE-", "https://openid.net/specs/openid-financial-api-part-1-1_0-final.html#rfc.section.");
		specLinks.put("FAPI1-ADV-", "https://openid.net/specs/openid-financial-api-part-2-1_0-final.html#rfc.section.");
		specLinks.put("FAPI2-BASE-", "https://openid.bitbucket.io/fapi/fapi-2_0-baseline.html#section."); // not a stable version - update to implementers draft 2 when available
		specLinks.put("FAPI2-ADV-", "https://openid.bitbucket.io/fapi/fapi-2_0-advanced.html#section."); // not a stable version - update to implementers draft when available
		specLinks.put("CIBA-", "https://openid.net/specs/openid-client-initiated-backchannel-authentication-core-1_0.html#rfc.section.");
		specLinks.put("FAPI-CIBA-", "https://openid.net/specs/openid-financial-api-ciba.html#rfc.section.");
		specLinks.put("JARM-", "https://openid.net//specs/openid-financial-api-jarm-wd-01.html#rfc.section.");
		specLinks.put("OB-", "https://bitbucket.org/openid/obuk/src/b36035c22e96ce160524066c7fde9a45cbaeb949/uk-openbanking-security-profile.md?at=master&fileviewer=file-view-default#");
		specLinks.put("OBRW-", "https://openbanking.atlassian.net/wiki/spaces/DZ/pages/1077805207/Read+Write+Data+API+Specification+-+v3.1.2#");
		specLinks.put("OIDCC-", "https://openid.net/specs/openid-connect-core-1_0.html#rfc.section.");
		specLinks.put("OIDCR-", "https://openid.net/specs/openid-connect-registration-1_0.html#rfc.section.");
		specLinks.put("OAuth2-FP", "https://openid.net/specs/oauth-v2-form-post-response-mode-1_0.html");
		specLinks.put("OAuth2-iss", "ttps://tools.ietf.org/html/rfc9207#section-");
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
		specLinks.put("RFC8705-", "https://tools.ietf.org/html/rfc8705#section-");
		specLinks.put("OBSP-", "https://openbanking.atlassian.net/wiki/spaces/DZ/pages/83919096/Open+Banking+Security+Profile+-+Implementer+s+Draft+v1.1.2#");
		specLinks.put("OAuth2-RT-", "https://openid.net/specs/oauth-v2-multiple-response-types-1_0.html#rfc.section.");
		specLinks.put("OIDCD-", "https://openid.net/specs/openid-connect-discovery-1_0.html#rfc.section.");
		specLinks.put("OIDCBCL-", "https://openid.net/specs/openid-connect-backchannel-1_0.html#rfc.section.");
		specLinks.put("OIDCFCL-", "https://openid.net/specs/openid-connect-frontchannel-1_0.html#rfc.section.");
		specLinks.put("OIDCSM-", "https://openid.net/specs/openid-connect-session-1_0.html#rfc.section.");
		specLinks.put("OIDCRIL-", "https://openid.net/specs/openid-connect-rpinitiated-1_0.html#rfc.section.");
		specLinks.put("BCP195-", "https://tools.ietf.org/html/bcp195#section-");
		specLinks.put("CDR-","https://consumerdatastandardsaustralia.github.io/standards/#");
		specLinks.put("PAR-", "https://www.rfc-editor.org/rfc/rfc9126.html#section-");
		specLinks.put("JAR-", "https://www.rfc-editor.org/rfc/rfc9101.html#section-");
		specLinks.put("IA-", "https://openid.net/specs/openid-connect-4-identity-assurance-1_0-ID3.html#section-");
		specLinks.put("DPOP-", "https://datatracker.ietf.org/doc/html/draft-ietf-oauth-dpop#section-");
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
		if(fieldValue instanceof Document) {
			Document doc = (Document) fieldValue;
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
