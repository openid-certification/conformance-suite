package io.fintechlabs.testframework.logging;

import java.io.IOException;
import java.io.OutputStream;
import java.security.Signature;
import java.security.SignatureException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.mongodb.BasicDBList;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorOutputStream;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.util.Base64Utils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import com.google.common.collect.ImmutableMap;
import com.google.gson.Gson;
import com.mongodb.BasicDBObjectBuilder;
import com.mongodb.DBObject;

import io.fintechlabs.testframework.CollapsingGsonHttpMessageConverter;
import io.fintechlabs.testframework.info.DBTestInfoService;
import io.fintechlabs.testframework.security.AuthenticationFacade;
import io.fintechlabs.testframework.security.KeyManager;

/**
 * @author jricher
 *
 */
@Controller
public class LogApi {

	@Value("${fintechlabs.base_url:http://localhost:8080}")
	private String baseUrl;

	@Value("${fintechlabs.version}")
	private String version;

	@Autowired
	private MongoTemplate mongoTemplate;

	@Autowired
	private AuthenticationFacade authenticationFacade;

	@Autowired
	private KeyManager keyManager;

	private Gson gson = CollapsingGsonHttpMessageConverter.getDbObjectCollapsingGson();

	/** VISIBLE_FIELDS */
	private static final Set<String> VISIBLE_FIELDS = new HashSet<>(Arrays.asList(
		"msg", "src", "time", "result", "requirements", "upload", "testOwner", "testId", "http", "blockId", "startBlock"));

	/** SPEC_LINKS_VALUE_MAP */
	private static final Map<String, String> SPEC_LINKS_VALUE_MAP;
	static {
		SPEC_LINKS_VALUE_MAP = new LinkedHashMap<>();
		SPEC_LINKS_VALUE_MAP.put("FAPI-R-", "https://openid.net/specs/openid-financial-api-part-1-ID2.html");
		SPEC_LINKS_VALUE_MAP.put("FAPI-RW-", "https://openid.net/specs/openid-financial-api-part-2-ID2.html");
		SPEC_LINKS_VALUE_MAP.put("OB-", "https://bitbucket.org/openid/obuk/src/b36035c22e96ce160524066c7fde9a45cbaeb949/uk-openbanking-security-profile.md?at=master&fileviewer=file-view-default");
		SPEC_LINKS_VALUE_MAP.put("OIDCC-", "https://openid.net/specs/openid-connect-core-1_0.html");
		SPEC_LINKS_VALUE_MAP.put("RFC6749-", "https://tools.ietf.org/html/rfc6749");
		SPEC_LINKS_VALUE_MAP.put("RFC6819-", "https://tools.ietf.org/html/rfc6819");
		SPEC_LINKS_VALUE_MAP.put("RFC7231-", "https://tools.ietf.org/html/rfc7231");
		SPEC_LINKS_VALUE_MAP.put("HEART-OAuth2-", "http://openid.net/specs/openid-heart-oauth2-1_0-2017-05-31.html");
	}

	@GetMapping(value = "/log", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<List<DBObject>> getAllTests() {

		DBObject queryFilter;
		if (authenticationFacade.isAdmin()) {
			queryFilter = BasicDBObjectBuilder.start().get();
		} else {
			ImmutableMap<String, String> owner = authenticationFacade.getPrincipal();
			queryFilter = BasicDBObjectBuilder.start().add("testOwner", owner).get();
		}

		@SuppressWarnings("unchecked")
		List<String> testIds = mongoTemplate.getCollection(DBEventLog.COLLECTION).distinct("testId", queryFilter);

		List<DBObject> results = new ArrayList<>(testIds.size());

		for (String testId : testIds) {
			// fetch the test object from the info log if available
			DBObject testInfo = mongoTemplate.getCollection(DBTestInfoService.COLLECTION).findOne(testId);

			if (testInfo == null) {
				// make a fake document with just the ID
				results.add(BasicDBObjectBuilder.start("_id", testId).get());
			} else {
				// otherwise, add everything
				results.add(testInfo);
			}
		}

		return new ResponseEntity<>(results, HttpStatus.OK);

	}

	@GetMapping(value = "/log/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<List<DBObject>> getLogResults(@PathVariable("id") String id, @RequestParam(value = "since", required = false) Long since) {
		List<DBObject> results = getTestResults(id, since);

		return ResponseEntity.ok().body(results);
	}

	@GetMapping(value = "/log/export/{id}", produces = "application/x-gtar")
	public ResponseEntity<StreamingResponseBody> export(@PathVariable("id") String id) {
		List<DBObject> results = getTestResults(id);

		DBObject testInfo = null;
		if (authenticationFacade.isAdmin()) {
			testInfo = mongoTemplate.getCollection(DBTestInfoService.COLLECTION).findOne(id);
		} else {
			ImmutableMap<String, String> owner = authenticationFacade.getPrincipal();
			if (owner != null) {
				testInfo = mongoTemplate.getCollection(DBTestInfoService.COLLECTION).findOne(BasicDBObjectBuilder.start().add("_id", id).add("owner", owner).get());
			}
		}

		if (testInfo == null) {
			return new ResponseEntity<>(HttpStatus.NOT_FOUND);
		}

		HttpHeaders headers = new HttpHeaders();

		headers.add("Content-Disposition", "attachment; filename=\"test-log-" + id + ".tar.bz2\"");

		final Map<String, Object> export = new HashMap<>();

		export.put("exportedAt", new Date());
		export.put("exportedFrom", baseUrl);
		export.put("exportedBy", authenticationFacade.getPrincipal());
		export.put("exportedVersion", version);
		export.put("testInfo", testInfo);
		export.put("results", results);

		StreamingResponseBody responseBody = new StreamingResponseBody() {

			@Override
			public void writeTo(OutputStream out) throws IOException {

				try {
					BZip2CompressorOutputStream compressorOutputStream = new BZip2CompressorOutputStream(out);

					TarArchiveOutputStream archiveOutputStream = new TarArchiveOutputStream(compressorOutputStream);

					TarArchiveEntry testLog = new TarArchiveEntry("test-log-" + id + ".json");

					Signature signature = Signature.getInstance("SHA1withRSA");
					signature.initSign(keyManager.getSigningPrivateKey());

					SignatureOutputStream signatureOutputStream = new SignatureOutputStream(archiveOutputStream, signature);

					String json = gson.toJson(export);

					testLog.setSize(json.getBytes().length);
					archiveOutputStream.putArchiveEntry(testLog);

					signatureOutputStream.write(json.getBytes());

					signatureOutputStream.flush();
					signatureOutputStream.close();

					archiveOutputStream.closeArchiveEntry();

					TarArchiveEntry signatureFile = new TarArchiveEntry("test-log-" + id + ".sig");

					String encodedSignature = Base64Utils.encodeToUrlSafeString(signature.sign());
					signatureFile.setSize(encodedSignature.getBytes().length);

					archiveOutputStream.putArchiveEntry(signatureFile);

					archiveOutputStream.write(encodedSignature.getBytes());

					archiveOutputStream.closeArchiveEntry();

					archiveOutputStream.close();
				} catch (Exception ex) {
					throw new IOException(ex);
				}
			}
		};

		return ResponseEntity.ok().headers(headers).body(responseBody);
	}

	/**
	 * Export test log as human readable plain text
	 * @param id
	 * @param timezone
	 * @return
	 */
	@GetMapping(value = "/api/log/exportLegibleLog/{id}", produces = "text/plain; charset=UTF-8")
	public ResponseEntity<StreamingResponseBody> exportLegibleLog(@PathVariable("id") String id, @RequestParam(value = "timezone", required = false) String timezone) {
		List<DBObject> results = getTestResults(id);

		DBObject testInfo = getTestInfoById(id);
		if (testInfo == null) {
			return new ResponseEntity<>(HttpStatus.NOT_FOUND);
		}
		HttpHeaders headers = new HttpHeaders();
		headers.add("Content-Disposition", "attachment; filename=\"test-log-" + id + ".txt\"");

		final Map<String, Object> export = new HashMap<>();
		try {
			// Test Info
			export.put("exportedAt", new Date());
			export.put("exportedFrom", baseUrl);
			export.put("exportedBy", authenticationFacade.isAdmin() ? "ADMIN" : authenticationFacade.getPrincipal());
			export.put("exportedVersion", version);
			export.put("status", testInfo.get("status"));
			export.put("result", testInfo.get("result"));
			export.put("testName", testInfo.get("testName"));
			export.put("testId", testInfo.get("testId"));
			export.put("description", testInfo.get("description") != null ? testInfo.get("description") : "");
			export.put("planId", testInfo.get("planId"));

			DBObject owner = (DBObject) testInfo.get("owner");
			if (owner != null) {
				String iss = owner.get("iss").toString();
				iss = iss.replaceFirst("^(http[s]?://www\\.|http[s]?://|www\\.)", "");
				export.put("testOwner", owner.get("sub") + "@" + iss);
			}
			// Setup timezone UTC
			SimpleDateFormat utcTimeFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
			utcTimeFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
			DateFormat createTimeFormat = new SimpleDateFormat("EEE MMM dd yyyy HH:mm:ss 'GMT'Z (zzzz)");

			// Setup clientTimeZone
			TimeZone clientTimeZone = TimeZone.getTimeZone(timezone);
			createTimeFormat.setTimeZone(clientTimeZone);
			DateFormat executeTimeFormat = new SimpleDateFormat("hh:mm:ss a");
			executeTimeFormat.setTimeZone(clientTimeZone);

			Date date = utcTimeFormat.parse(testInfo.get("started").toString());
			export.put("created", createTimeFormat.format(date));

			// Setup a JWT pattern object
			String jwtRe = "^(e[yw][a-zA-Z0-9_-]+)\\.([a-zA-Z0-9_-]+)\\.([a-zA-Z0-9_-]+)(\\.([a-zA-Z0-9_-]+)\\.([a-zA-Z0-9_-]+))?$";
			Pattern jwtPatternCompile = Pattern.compile(jwtRe);

			// Setup string builder to present the result detail
			StringBuilder sbDetail = new StringBuilder();
			int successNumber = 0, failureNumber = 0, warningNumber = 0, reviewNumber = 0, infoNumber = 0;

			// Prepare the Results Detail
			for (DBObject item : results) {
				if (item == null) continue;
				if (item.get("result") != null) {
					String testResult = item.get("result").toString();
					if (testResult != null) {
						if ("SUCCESS".equals(testResult)) {
							successNumber += 1;
						} else if ("FAILURE".equals(testResult)) {
							failureNumber += 1;
						} else if ("WARNING".equals(testResult)) {
							warningNumber += 1;
						} else if ("REVIEW".equals(testResult)) {
							reviewNumber += 1;
						} else if ("INFO".equals(testResult)) {
							infoNumber += 1;
						}
					}
				}
				// time / result & http / source / owner
				if (item.get("time") != null) {
					Date logTime = new Date((long) item.get("time"));
					sbDetail.append(executeTimeFormat.format(logTime));
				}
				if (item.get("result") != null) {
					sbDetail.append(" ").append(item.get("result").toString().toUpperCase());
				}
				if (item.get("http") != null) {
					sbDetail.append(" ").append(item.get("http").toString().toUpperCase());
				}
				if (item.get("upload") != null) {
					sbDetail.append(" IMAGE REQUIRED");
				}
				if (item.get("img") != null) {
					sbDetail.append(" IMAGE");
				}
				if (item.get("src") != null) {
					sbDetail.append(" ").append(item.get("src"));
				}
				DBObject testOwner = (DBObject) item.get("testOwner");
				if (testOwner != null
					&& (!testOwner.get("iss").equals(owner.get("iss"))
					|| !testOwner.get("sub").equals(owner.get("sub")))) {
					sbDetail.append(" ").append(item.get("sub")).append(" ").append(item.get("iss"));
				}
				// requirements / message
				if (item.get("msg") != null) {
					sbDetail.append("\r\n").append(item.get("msg"));
				}
				if (item.get("upload") != null) {
					sbDetail.append("\r\n").append("Attach image to log file...")
						.append("(/upload.html?log=").append(testInfo.get("testId")).append(")");
				}
				if (item.get("requirements") != null) {
					BasicDBList requirements = (BasicDBList) item.get("requirements");
					if (requirements != null) {
						for (Object req : requirements) {
							sbDetail.append("\r\n").append(req);
							// link to the spec if we have it
							for (String key : SPEC_LINKS_VALUE_MAP.keySet()) {
								int index = req.toString().indexOf(key);
								if (index != -1) {
									sbDetail.append(" (").append(SPEC_LINKS_VALUE_MAP.get(key)).append(")");
								}
							}
						}
					}
				}
				// more info
				for (String key : item.keySet()) {
					if (!VISIBLE_FIELDS.contains(key) && !key.startsWith("_")) {
						Object value = item.get(key);
						if (value == null) continue;
						sbDetail.append("\r\n").append(key).append(": ");
						if ("img".equals(key)) {
							sbDetail.append(value.toString());
						} else if ("stacktrace".equals(key)) {
							BasicDBList stacktrace = (BasicDBList) value;//item.get("stacktrace");
							if (stacktrace != null) {
								for (Object st : stacktrace) {
									sbDetail.append("\r\n").append(st);
								}
							}
						} else {
							Matcher jwt = jwtPatternCompile.matcher(value.toString());
							if (jwt.find()) {
								// jwtHeader
								sbDetail.append(jwt.group(1))
									// jwtPayload
									.append(".").append(jwt.group(2))
									// jwtSignature
									.append(".").append(jwt.group(3));
								if (jwt.groupCount() > 4 && jwt.group(4) != null) {
									// jweCypher
									sbDetail.append(".").append(jwt.group(5))
										// jweTag
										.append(".").append(jwt.group(6));
								}
							} else {
								if ((value instanceof String)
									|| (value instanceof Number)
									|| (value instanceof Boolean)) {
									sbDetail.append(value);
								} else if (value instanceof BasicDBList) {
									BasicDBList ls = (BasicDBList) value;
									sbDetail.append("\r\n").append("[");
									if (ls != null) {
										for (Object st : ls) {
											sbDetail.append("\r\n  ").append(st).append(",");
										}
										sbDetail.append("\r\n").append("]");
									}
								} else {
									String json = gson.toJson(value);
									sbDetail.append(formatString(json));
								}
							}
						}
					}
				}
				// next item detail
				sbDetail.append("\r\n\r\n");
			}
			export.put("successNumber", successNumber);
			export.put("failureNumber", failureNumber);
			export.put("warningNumber", warningNumber);
			export.put("reviewNumber", reviewNumber);
			export.put("infoNumber", infoNumber);
			export.put("logDetail", sbDetail);
		} catch (Exception ex) {
			ex.printStackTrace();
			return new ResponseEntity<>(HttpStatus.NOT_FOUND);
		}
		StreamingResponseBody responseBody = out -> {
			try {
				StringBuilder sb = new StringBuilder();
				sb.append("Export of ")
					.append(export.get("exportedFrom")).append("/log-detail.html?log=").append(id)
					.append(" by ").append(export.get("exportedBy")).append(" ").append(authenticationFacade.getDisplayName())
					.append("\r\n")
					//Test status
					.append("Test status: ").append(export.get("status")).append("\r\n")
					.append("Test result: ").append(export.get("result")).append("\r\n")
					.append("Test Name: ").append(export.get("testName")).append("\r\n")
					.append("Test ID: ").append(export.get("testId")).append("\r\n")
					.append("Created: ").append(export.get("created")).append("\r\n")
					.append("Description: ").append(export.get("description")!=null?export.get("description"):"").append("\r\n")
					.append("Test Owner: ").append(export.get("testOwner")).append("\r\n")
					.append("Plan ID: ").append(export.get("planId")).append("\r\n")
					.append("Results: SUCCESS ").append(export.get("successNumber"))
					.append(" FAILURE ").append(export.get("failureNumber"))
					.append(" WARNING ").append(export.get("warningNumber"))
					.append(" REVIEW ").append(export.get("reviewNumber"))
					.append(" INFO ").append(export.get("infoNumber"))
					.append("\r\n\r\n").append(export.get("logDetail"));
				String result = sb.toString();
				out.write(result.getBytes());
				out.flush();
				out.close();
			} catch (Exception ex) {
				throw new IOException(ex);
			}
		};
		return ResponseEntity.ok().headers(headers).body(responseBody);
	}

	/**
	 * Get Test Info ById
	 * @param id
	 * @return
	 */
	private DBObject getTestInfoById(String id) {
		if (authenticationFacade.isAdmin()) {
			return mongoTemplate.getCollection(DBTestInfoService.COLLECTION).findOne(id);
		} else {
			ImmutableMap<String, String> owner = authenticationFacade.getPrincipal();
			if (owner != null) {
				return mongoTemplate.getCollection(DBTestInfoService.COLLECTION).findOne(BasicDBObjectBuilder.start().add("_id", id).add("owner", owner).get());
			}
		}
		return null;
	}

	/**
	 * @param id
	 * @return
	 */
	private List<DBObject> getTestResults(String id) {
		return getTestResults(id, null);
	}

	private List<DBObject> getTestResults(String id, Long since) {
		Criteria criteria = new Criteria();
		criteria.and("testId").is(id);

		if (!authenticationFacade.isAdmin()) {
			criteria.and("testOwner").is(authenticationFacade.getPrincipal());
		}

		if (since != null) {
			criteria.and("time").gt(since);
		}

		List<DBObject> results = mongoTemplate.getCollection(DBEventLog.COLLECTION).find(criteria.getCriteriaObject())
			.sort(BasicDBObjectBuilder.start()
				.add("time", 1)
				.get())
			.toArray();
		return results;
	}

	private static class SignatureOutputStream extends OutputStream {

		private OutputStream target;
		private Signature sig;

		/**
		 * creates a new SignatureOutputStream which writes to
		 * a target OutputStream and updates the Signature object.
		 */
		public SignatureOutputStream(OutputStream target, Signature sig) {
			this.target = target;
			this.sig = sig;
		}

		public void write(int b)
			throws IOException
		{
			write(new byte[]{(byte)b});
		}

		public void write(byte[] b)
			throws IOException
		{
			write(b, 0, b.length);
		}

		public void write(byte[] b, int offset, int len)
			throws IOException
		{
			target.write(b, offset, len);
			try {
				sig.update(b, offset, len);
			}
			catch(SignatureException ex) {
				throw new IOException(ex);
			}
		}

		public void flush()
			throws IOException
		{
			target.flush();
		}

		public void close()
			throws IOException
		{
			// we don't close the target stream when we're done because we might keep writing to it later
			//target.close();
		}
	}
	/**
	 * Format a json String as pretty format
	 * @param text
	 * @return
	 */
	private static String formatString(String text){
		StringBuilder sb = new StringBuilder();
		String indentString = "";
		for (int i = 0; i < text.length(); i++) {
			char letter = text.charAt(i);
			switch (letter) {
				case '{':
				case '[':
					sb.append("\r\n").append(indentString).append(letter).append("\r\n");
					indentString = indentString + "  ";
					sb.append(indentString);
					break;
				case '}':
				case ']':
					indentString = indentString.replaceFirst("  ", "");
					sb.append("\r\n").append(indentString).append(letter);
					break;
				case ',':
					sb.append(letter).append("\r\n").append(indentString);
					break;
				default:
					sb.append(letter);
					break;
			}
		}
		return sb.toString();
	}
}
