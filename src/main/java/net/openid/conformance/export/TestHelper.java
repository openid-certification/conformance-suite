package net.openid.conformance.export;

import net.openid.conformance.info.PublicTestInfo;
import net.openid.conformance.info.TestInfo;
import org.bson.Document;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

/**
 * Template rendering helper for a test
 * Methods that seem "unused" are used in templates.
 */
public class TestHelper {

	private String suiteBaseUrl;
	private Date exportedAt;
	private String exportedFrom;
	private Map<String, String> exportedBy;
	private String suiteVersion;
	private Document testInfoDocument;
	private TestInfo testInfoObject;
	private PublicTestInfo publicTestInfo;
	private List<Document> testResults;
	private List<LogEntryHelper> logEntryHelpers = new ArrayList<>();

	private List<String> failures = new ArrayList<>();
	private int successCount;
	private int failureCount;
	private int warningCount;
	private int reviewCount;
	private int infoCount;

	/**
	 *
	 * @param export see LogApi.putTestResultToExport
	 */
	@SuppressWarnings("unchecked")
	public TestHelper(TestExportInfo export, String suiteBaseUrl) {
		this.suiteBaseUrl = suiteBaseUrl;
		this.exportedAt = export.getExportedAt();
		this.exportedFrom = export.getExportedFrom();
		this.exportedBy = export.getExportedBy();
		this.suiteVersion = export.getExportedVersion();
		if(export.getTestInfo() instanceof TestInfo) {
			this.testInfoObject = (TestInfo) export.getTestInfo();
		} else if(export.getTestInfo() instanceof PublicTestInfo) {
			this.publicTestInfo = (PublicTestInfo)export.getTestInfo();
		} else if(export.getTestInfo() instanceof Document) {
			this.testInfoDocument = (Document) ((Document) export.getTestInfo()).get("testInfo");
		} else {
			throw new RuntimeException("Unexpected testInfo object type: " + export.getTestInfo().getClass());
		}
		this.testResults = (List<Document>)export.getResults();
		for(Document resultDoc : this.testResults) {
			String resultStr = resultDoc.getString("result");
			if("INFO".equals(resultStr)) {
				infoCount++;
			} else if("SUCCESS".equals(resultStr)) {
				successCount++;
			} else if("FAILURE".equals(resultStr)) {
				failureCount++;
				this.failures.add(resultDoc.getString("msg"));
			} else if("WARNING".equals(resultStr)) {
				warningCount++;
			} else if("REVIEW".equals(resultStr)) {
				reviewCount++;
			}
		}
	}

	public Date getExportedAt()
	{
		return exportedAt;
	}

	public void setExportedAt(Date exportedAt)
	{
		this.exportedAt = exportedAt;
	}

	public String getExportedFrom()
	{
		return exportedFrom;
	}

	public void setExportedFrom(String exportedFrom)
	{
		this.exportedFrom = exportedFrom;
	}

	public String getExportedBy() {
		return this.exportedBy.get("sub") + " " + this.exportedBy.get("iss");
	}

	public String getExportedBySub()
	{
		return exportedBy.get("sub");
	}


	public String getExportedByIss()
	{
		return exportedBy.get("iss");
	}

	public String getSuiteVersion()
	{
		return suiteVersion;
	}

	public void setSuiteVersion(String suiteVersion)
	{
		this.suiteVersion = suiteVersion;
	}



	public String getExportedTime(){
		if(exportedAt==null) {
			return "";
		}
		TimeZone timeZone = TimeZone.getTimeZone("UTC");
		DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.ENGLISH);
		dateFormat.setTimeZone(timeZone);
		String formatted = dateFormat.format(exportedAt) + " (UTC)";
		return formatted;
	}

	public List<Document> getTestResults()
	{
		return testResults;
	}

	public void setTestResults(List<Document> testResults)
	{
		this.testResults = testResults;
	}

	public void addLogEntryHelper(LogEntryHelper logEntryHelper) {
		this.logEntryHelpers.add(logEntryHelper);
	}

	public List<LogEntryHelper> getLogEntryHelpers()
	{
		return logEntryHelpers;
	}

	/**
	 * use only for css class names. returns unknown by default
	 * @param theObject
	 * @return
	 */
	private String toLowerCaseIfNotNull(Object theObject) {
		if(theObject==null) {
			return "unknown";
		}
		String str = theObject.toString().toLowerCase(Locale.ENGLISH);
		return str;
	}

	public String getTestStatusClass() {
		if(this.testInfoDocument!=null) {
			try {
				return "label testStatus-" + toLowerCaseIfNotNull(this.testInfoDocument.getString("status"));
			} catch (Exception ex) {
				return "";
			}
		} else if(this.publicTestInfo!=null) {
			return "label testStatus-" + toLowerCaseIfNotNull(this.publicTestInfo.getStatus());
		} else if(this.testInfoObject!=null) {
			return "label testStatus-" + toLowerCaseIfNotNull(this.testInfoObject.getStatus());
		} else {
			return "";
		}
	}
	public String getTestStatus() {
		String status = null;
		if(this.testInfoDocument!=null) {
			status = this.testInfoDocument.getString("status");
		} else if(this.publicTestInfo!=null) {
			status = this.publicTestInfo.getStatus().toString();
		} else if(this.testInfoObject!=null) {
			status = this.testInfoObject.getStatus().toString();
		} else {
			throw new RuntimeException("ERROR IN EXPORT RENDERER. This is a bug in the suite.");
		}
		if(status==null || status.isEmpty()) {
			return "UNKNOWN";
		}
		return status.toUpperCase(Locale.ENGLISH);
	}
	public String getTestResultClass() {
		if(this.testInfoDocument!=null) {
			return "label testResult-" + toLowerCaseIfNotNull(this.testInfoDocument.getString("result"));
		} else if(this.publicTestInfo!=null) {
			return "label testResult-" + toLowerCaseIfNotNull(this.publicTestInfo.getResult());
		} else if(this.testInfoObject!=null) {
			return "label testResult-" + toLowerCaseIfNotNull(this.testInfoObject.getResult());
		} else {
			throw new RuntimeException("ERROR IN EXPORT RENDERER. This is a bug in the suite.");
		}
	}
	public String getTestResult() {
		String result = null;
		if(this.testInfoDocument!=null) {
			result = this.testInfoDocument.getString("result");
		} else if(this.publicTestInfo!=null) {
			result = this.publicTestInfo.getResult();
		} else if(this.testInfoObject!=null) {
			result = this.testInfoObject.getResult();
		} else {
			throw new RuntimeException("ERROR IN EXPORT RENDERER. This is a bug in the suite.");
		}
		if(result==null || result.isEmpty()) {
			return "UNKNOWN";
		}
		return result.toUpperCase(Locale.ENGLISH);
	}
	public String getTestName() {
		if(this.testInfoDocument!=null) {
			return this.testInfoDocument.getString("testName");
		} else if(this.publicTestInfo!=null) {
			return this.publicTestInfo.getTestName();
		} else if(this.testInfoObject!=null) {
			return this.testInfoObject.getTestName();
		} else {
			throw new RuntimeException("ERROR IN EXPORT RENDERER. This is a bug in the suite.");
		}
	}
	public String getTestVariant() {
		if (this.testInfoDocument != null) {
			Document variantDoc = (Document) this.testInfoDocument.get("variant");
			return variantDoc.toJson();
		} else if(this.publicTestInfo!=null) {
			if(this.publicTestInfo.getVariant()!=null && this.publicTestInfo.getVariant().getVariant()!=null) {
				return this.publicTestInfo.getVariant().getVariantAsKeyPairString();
			} else {
				return "";
			}
		} else if(this.testInfoObject!=null) {
			if(this.testInfoObject.getVariant()!=null && this.testInfoObject.getVariant().getVariant()!=null) {
				return this.testInfoObject.getVariant().getVariantAsKeyPairString();
			} else {
				return "";
			}
		}
		throw new RuntimeException("ERROR IN EXPORT RENDERER. This is a bug in the suite.");
	}
	public String getTestId() {
		if(this.testInfoDocument!=null) {
			return this.testInfoDocument.getString("testId");
		} else if(this.publicTestInfo!=null) {
			return this.publicTestInfo.getTestId();
		} else if(this.testInfoObject!=null) {
			return this.testInfoObject.getTestId();
		} else {
			throw new RuntimeException("ERROR IN EXPORT RENDERER. This is a bug in the suite.");
		}
	}

	public String getTestLink() {
		return this.suiteBaseUrl + "/log-detail.html?public=true&log=" + getTestId();
	}

	public String getTestCreated() {
		if(this.testInfoDocument!=null) {
			return this.testInfoDocument.getString("started");
		} else if(this.publicTestInfo!=null) {
			return this.publicTestInfo.getStarted();
		} else if(this.testInfoObject!=null) {
			return this.testInfoObject.getStarted();
		} else {
			throw new RuntimeException("ERROR IN EXPORT RENDERER. This is a bug in the suite.");
		}
	}
	public String getTestDescription() {
		if(this.testInfoDocument!=null) {
			return this.testInfoDocument.getString("description");
		} else if(this.publicTestInfo!=null) {
			return this.publicTestInfo.getDescription();
		} else if(this.testInfoObject!=null) {
			return this.testInfoObject.getDescription();
		} else {
			throw new RuntimeException("ERROR IN EXPORT RENDERER. This is a bug in the suite.");
		}
	}
	public String getTestVersion() {
		if(this.testInfoDocument!=null) {
			return this.testInfoDocument.getString("version");
		} else if(this.publicTestInfo!=null) {
			return this.publicTestInfo.getVersion();
		} else if(this.testInfoObject!=null) {
			return this.testInfoObject.getVersion();
		} else {
			throw new RuntimeException("ERROR IN EXPORT RENDERER. This is a bug in the suite.");
		}
	}
	public String getTestOwner() {
		if (this.testInfoDocument != null) {
			Document ownerDoc = (Document) this.testInfoDocument.get("owner");
			return ownerDoc.getString("sub") + " " + ownerDoc.getString("iss");
		} else if (this.publicTestInfo != null) {
			return this.publicTestInfo.getOwner().get("sub") + " " + this.publicTestInfo.getOwner().get("iss");
		} else if (this.testInfoObject != null) {
			return this.testInfoObject.getOwner().get("sub") + " " + this.testInfoObject.getOwner().get("iss");
		} else {
			throw new RuntimeException("ERROR IN EXPORT RENDERER. This is a bug in the suite.");
		}
	}
	public String getTestPlanId() {
		if(this.testInfoDocument!=null) {
			return this.testInfoDocument.getString("planId");
		} else if(this.publicTestInfo!=null) {
			return this.publicTestInfo.getPlanId();
		} else if(this.testInfoObject!=null) {
			return this.testInfoObject.getPlanId();
		} else {
			throw new RuntimeException("ERROR IN EXPORT RENDERER. This is a bug in the suite.");
		}
	}
	public String getPlanLink() {
		return this.suiteBaseUrl + "/plan-detail.html?public=true&plan=" + getTestPlanId();
	}

	public String getTestSummary() {
		if(this.testInfoDocument!=null) {
			return this.testInfoDocument.getString("summary");
		} else if(this.publicTestInfo!=null) {
			return this.publicTestInfo.getSummary();
		} else if(this.testInfoObject!=null) {
			return this.testInfoObject.getSummary();
		} else {
			throw new RuntimeException("ERROR IN EXPORT RENDERER. This is a bug in the suite.");
		}
	}
	public int getSuccessCount() {
		return successCount;
	}
	public int getFailureCount() {
		return failureCount;
	}
	public int getWarningCount() {
		return warningCount;
	}
	public int getReviewCount() {
		return reviewCount;
	}
	public int getInfoCount() {
		return infoCount;
	}
	public boolean hasAnyFailures() {
		return (failureCount>0);
	}
	public List<String> getFailures() {
		return failures;
	}

	public String getLocalTestLink() {
		return generateHtmlFileName(getTestName(), getTestId());
	}

	public static String generateHtmlFileName(Object testModuleName, Object testId) {
		return "test-log-" + testModuleName + "-" + testId + ".html";
	}
	public static String generateSigFileName(Object testModuleName, Object testId) {
		return "test-log-" + testModuleName + "-" + testId + ".html.sig";
	}
}
