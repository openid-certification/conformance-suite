package net.openid.conformance.export;

import net.openid.conformance.info.TestInfo;
import net.openid.conformance.security.AuthenticationFacade;
import org.bson.Document;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

/**
 * Template rendering helper for a test
 * Methods that seem "unused" are used in templates.
 */
public class TestHelper {

	private Date exportedAt;
	private String exportedFrom;
	private String exportedBySub;
	private String exportedByIss;
	private String suiteVersion;
	//TODO it is sometimes TestInfo sometimes Document
	private Document testInfoDocument;
	private TestInfo testInfoObject;
	private List<Document> testResults;
	private List<String> resultHtmls = new LinkedList<>();

	private List<String> failures = new LinkedList<>();
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
	public TestHelper(Map<String, Object> export) {
		this.exportedAt = (Date) export.get("exportedAt");
		this.exportedFrom = (String) export.get("exportedFrom");
		Map<String, String> principal = (Map<String, String>)export.get("exportedBy");
		this.exportedByIss = principal.get("iss");
		this.exportedBySub = principal.get("sub");
		this.suiteVersion = (String)export.get("exportedVersion");
		if(export.get("testInfo") instanceof net.openid.conformance.info.TestInfo) {
			this.testInfoObject = (net.openid.conformance.info.TestInfo)export.get("testInfo");
		} else if(export.get("testInfo") instanceof Document) {
			this.testInfoDocument = (Document) ((Document) export.get("testInfo")).get("testInfo");
		}
		this.testResults = (List<Document>)export.get("results");
		for(Document resultDoc : this.testResults) {
			String resultStr = resultDoc.getString("result");
			if("INFO".equals(resultStr)) {
				infoCount++;
			} else if("SUCCESS".equals(resultStr)) {
				successCount++;
			} else if("FAILURE".equals(resultStr)) {
				failureCount++;
				this.failures.add(resultDoc.getString("msg") +
					//TODO fix this
					(resultDoc.get("requirements")!=null? (" " + resultDoc.get("requirements")):"")
				);
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

	public String getExportedBySub()
	{
		return exportedBySub;
	}

	public void setExportedBySub(String exportedBySub)
	{
		this.exportedBySub = exportedBySub;
	}

	public String getExportedByIss()
	{
		return exportedByIss;
	}

	public void setExportedByIss(String exportedByIss)
	{
		this.exportedByIss = exportedByIss;
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
		TimeZone timeZone = TimeZone.getTimeZone("UTC");
		DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.ENGLISH);
		dateFormat.setTimeZone(timeZone);
		String asISO = dateFormat.format(exportedAt) + " (UTC)";
		return asISO;
	}

	public List<Document> getTestResults()
	{
		return testResults;
	}

	public void setTestResults(List<Document> testResults)
	{
		this.testResults = testResults;
	}

	public void addRenderedResult(String renderedHtml) {
		this.resultHtmls.add(renderedHtml);
	}

	public List<String> getResultHtmls()
	{
		return resultHtmls;
	}

	public String getTestStatusClass() {
		if(this.testInfoDocument!=null) {
			try {
				return "p-1 " + this.testInfoDocument.getString("status").toLowerCase();
			} catch (Exception ex) {
				return "";
			}
		} else{
			return "p-1 " + this.testInfoObject.getStatus().toString().toLowerCase(Locale.ENGLISH);
		}
	}
	public String getTestStatus() {
		if(this.testInfoDocument!=null) {
			return this.testInfoDocument.getString("status");
		} else{
			return this.testInfoObject.getStatus().toString();
		}
	}
	public String getTestResultClass() {
		if(this.testInfoDocument!=null) {
			try {
				return "p-1 " + this.testInfoDocument.getString("result").toLowerCase();
			}
			catch (Exception ex) {
				return "";
			}
		} else{
			return "p-1 " + this.testInfoObject.getResult().toLowerCase(Locale.ENGLISH);
		}
	}
	public String getTestResult() {
		if(this.testInfoDocument!=null) {
			return this.testInfoDocument.getString("result");
		} else{
			return this.testInfoObject.getResult();
		}
	}
	public String getTestName() {
		if(this.testInfoDocument!=null) {
			return this.testInfoDocument.getString("testName");
		} else{
			return this.testInfoObject.getTestName();
		}
	}
	public String getTestVariant() {
		//TODO fix
		if(this.testInfoDocument!=null) {
			return this.testInfoDocument.get("variant").toString();
		} else{
			return this.testInfoObject.getVariant().getVariant().toString();
		}
	}
	public String getTestId() {
		if(this.testInfoDocument!=null) {
			return this.testInfoDocument.getString("testId");
		} else{
			return this.testInfoObject.getTestId();
		}
	}
	public String getTestCreated() {
		if(this.testInfoDocument!=null) {
			return this.testInfoDocument.getString("started");
		} else{
			return this.testInfoObject.getStarted();
		}
	}
	public String getTestDescription() {
		if(this.testInfoDocument!=null) {
			return this.testInfoDocument.getString("description");
		} else{
			return this.testInfoObject.getDescription();
		}
	}
	public String getTestVersion() {
		if(this.testInfoDocument!=null) {
			return this.testInfoDocument.getString("version");
		} else{
			return this.testInfoObject.getVersion();
		}
	}
	public String getTestOwner() {
		//TODO fix
		if(this.testInfoDocument!=null) {
			return this.testInfoDocument.get("owner").toString();
		} else{
			return this.testInfoObject.getOwner().toString();
		}
	}
	public String getTestPlanId() {
		if(this.testInfoDocument!=null) {
			return this.testInfoDocument.getString("planId");
		} else{
			return this.testInfoObject.getPlanId();
		}
	}
	public String getTestSummary() {
		if(this.testInfoDocument!=null) {
			return this.testInfoDocument.getString("summary");
		} else{
			return this.testInfoObject.getSummary();
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
}
