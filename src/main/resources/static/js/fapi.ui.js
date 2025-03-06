var FAPI_UI = {

		logTemplates : {},

		loadHomepageTemplates : function() {
			return fetch('templates/userinfo.html')
				.then((response) => response.text())
				.then((data) => {
					FAPI_UI.logTemplates.USER_INFO = _.template(data);
				});
		},

		loadScheduleTestPageTemplates : function() {
			const p1 = fetch('templates/testOption.html')
				.then((response) => response.text())
				.then((data) => {
					FAPI_UI.logTemplates.TEST_OPTION = _.template(data);
				});

			const p2 = fetch('templates/testOptGroup.html')
				.then((response) => response.text())
				.then((data) => {
					FAPI_UI.logTemplates.TEST_OPTGROUP = _.template(data);
				});

			const p3 = fetch('templates/userinfo.html')
				.then((response) => response.text())
				.then((data) => {
					FAPI_UI.logTemplates.USER_INFO = _.template(data);
				});

			const promises = [p1, p2, p3];

			return Promise.allSettled(promises);
		},

		loadRunningTestPageTemplates : function() {
			const p1 = fetch('templates/runningTest.html')
				.then((response) => response.text())
				.then((data) => {
					FAPI_UI.logTemplates.RUNNING_TEST = _.template(data);
				});

			const p2 = fetch('templates/owner.html')
				.then((response) => response.text())
				.then((data) => {
					FAPI_UI.logTemplates.OWNER = _.template(data);
				});

			const p3 = fetch('templates/statusAndResult.html')
				.then((response) => response.text())
				.then((data) => {
					FAPI_UI.logTemplates.TEST_STATUS = _.template(data);
				});

			const p4 = fetch('templates/userinfo.html')
				.then((response) => response.text())
				.then((data) => {
					FAPI_UI.logTemplates.USER_INFO = _.template(data);
				});

			const promises = [p1, p2, p3, p4];

			return Promise.allSettled(promises);
		},

		loadLogDetailTemplates: function() {
			const p1 = fetch('templates/userinfo.html')
				.then((response) => response.text())
				.then((data) => {
					FAPI_UI.logTemplates.USER_INFO = _.template(data);
				});

			const p2 = fetch('templates/logHeader.html')
				.then((response) => response.text())
				.then((data) => {
					FAPI_UI.logTemplates.LOG_START = _.template(data);
				});

			const p3 = fetch('templates/logEntry.html')
				.then((response) => response.text())
				.then((data) => {
					FAPI_UI.logTemplates.LOG_DETAIL = _.template(data);
				});

			const p4 = fetch('templates/source.html')
				.then((response) => response.text())
				.then((data) => {
					FAPI_UI.logTemplates.SOURCE = _.template(data);
				});

			const p5 = fetch('templates/message.html')
				.then((response) => response.text())
				.then((data) => {
					FAPI_UI.logTemplates.MESSAGE = _.template(data);
				});

			const p6 = fetch('templates/requirements.html')
				.then((response) => response.text())
				.then((data) => {
					FAPI_UI.logTemplates.REQUIREMENTS = _.template(data);
				});

			const p7 = fetch('templates/upload.html')
				.then((response) => response.text())
				.then((data) => {
					FAPI_UI.logTemplates.UPLOAD = _.template(data);
				});

			const p8 = fetch('templates/owner.html')
				.then((response) => response.text())
				.then((data) => {
					FAPI_UI.logTemplates.OWNER = _.template(data);
				});

			const p9 = fetch('templates/result.html')
				.then((response) => response.text())
				.then((data) => {
					FAPI_UI.logTemplates.RESULT = _.template(data);
				});

			const p10 = fetch('templates/time.html')
				.then((response) => response.text())
				.then((data) => {
					FAPI_UI.logTemplates.TIME = _.template(data);
				});

			const p11 = fetch('templates/more.html')
				.then((response) => response.text())
				.then((data) => {
					FAPI_UI.logTemplates.MORE = _.template(data);
				});

			const p12 = fetch('templates/moreButton.html')
				.then((response) => response.text())
				.then((data) => {
					FAPI_UI.logTemplates.MORE_BUTTON = _.template(data);
				});

			const p13 = fetch('templates/exported.html')
				.then((response) => response.text())
				.then((data) => {
					FAPI_UI.logTemplates.EXPORTED = _.template(data);
				});

			const p14 = fetch('templates/browser.html')
				.then((response) => response.text())
				.then((data) => {
					FAPI_UI.logTemplates.BROWSER = _.template(data);
				});

			const p15 = fetch('templates/http.html')
				.then((response) => response.text())
				.then((data) => {
					FAPI_UI.logTemplates.HTTP = _.template(data);
				});

			const p16 = fetch('templates/finalError.html')
				.then((response) => response.text())
				.then((data) => {
					FAPI_UI.logTemplates.FINAL_ERROR = _.template(data);
				});

			const p17 = fetch('templates/statusAndResult.html')
				.then((response) => response.text())
				.then((data) => {
					FAPI_UI.logTemplates.TEST_STATUS = _.template(data);
				});

			const p18 = fetch('templates/resultsSummary.html')
				.then((response) => response.text())
				.then((data) => {
					FAPI_UI.logTemplates.SUMMARY = _.template(data);
				});

			const p19 = fetch('templates/startBlock.html')
				.then((response) => response.text())
				.then((data) => {
					FAPI_UI.logTemplates.START_BLOCK = _.template(data);
				});

			const p20 = fetch('templates/failureSummary.html')
				.then((response) => response.text())
				.then((data) => {
					FAPI_UI.logTemplates.FAILURE_SUMMARY = _.template(data);
				});

			const promises = [p1, p2, p3, p4. p5, p6, p7, p8, p9, p10, p11, p12, p13, p14, p15, p16, p17, p18, p19, p20];

			return Promise.allSettled(promises);
		},

		loadLogListTemplates: function() {
			const p1 = fetch('templates/configButton.html')
				.then((response) => response.text())
				.then((data) => {
					FAPI_UI.logTemplates.CONFIG = _.template(data);
				});

			const p2 = fetch('templates/date.html')
				.then((response) => response.text())
				.then((data) => {
					FAPI_UI.logTemplates.DATE = _.template(data);
				});

			const p3 = fetch('templates/logDetailButton.html')
				.then((response) => response.text())
				.then((data) => {
					FAPI_UI.logTemplates.LOG_DETAIL = _.template(data);
				});

			const p4 = fetch('templates/owner.html')
				.then((response) => response.text())
				.then((data) => {
					FAPI_UI.logTemplates.OWNER = _.template(data);
				});

			const p5 = fetch('templates/planDetailButton.html')
				.then((response) => response.text())
				.then((data) => {
					FAPI_UI.logTemplates.PLAN_DETAIL = _.template(data);
				});

			const p6 = fetch('templates/userinfo.html')
				.then((response) => response.text())
				.then((data) => {
					FAPI_UI.logTemplates.USER_INFO = _.template(data);
				});

			const promises = [p1, p2, p3, p4. p5, p6];

			return Promise.allSettled(promises);
		},

		loadPlanTemplates: function() {
			const p1 = fetch('templates/userinfo.html')
				.then((response) => response.text())
				.then((data) => {
					FAPI_UI.logTemplates.USER_INFO = _.template(data);
				});

			const p2 = fetch('templates/plan.html')
				.then((response) => response.text())
				.then((data) => {
					FAPI_UI.logTemplates.PLAN_START = _.template(data);
				});

			const p3 = fetch('templates/owner.html')
				.then((response) => response.text())
				.then((data) => {
					FAPI_UI.logTemplates.OWNER = _.template(data);
				});

			const p4 = fetch('templates/statusAndResult.html')
				.then((response) => response.text())
				.then((data) => {
					FAPI_UI.logTemplates.TEST_STATUS = _.template(data);
				});

			const p5 = fetch('templates/testVersion.html')
				.then((response) => response.text())
				.then((data) => {
					FAPI_UI.logTemplates.TEST_VERSION = _.template(data);
				});

			const promises = [p1, p2, p3, p4. p5];

			return Promise.allSettled(promises);
		},

		loadPlanListTemplates: function() {
			const p1 = fetch('templates/configButton.html')
				.then((response) => response.text())
				.then((data) => {
					FAPI_UI.logTemplates.CONFIG = _.template(data);
				});

			const p2 = fetch('templates/date.html')
				.then((response) => response.text())
				.then((data) => {
					FAPI_UI.logTemplates.DATE = _.template(data);
				});

			const p3 = fetch('templates/planDetailButton.html')
				.then((response) => response.text())
				.then((data) => {
					FAPI_UI.logTemplates.PLAN_DETAIL = _.template(data);
				});

			const p4 = fetch('templates/planModules.html')
				.then((response) => response.text())
				.then((data) => {
					FAPI_UI.logTemplates.PLAN_MODULES = _.template(data);
				});

			const p5 = fetch('templates/owner.html')
				.then((response) => response.text())
				.then((data) => {
					FAPI_UI.logTemplates.OWNER = _.template(data);
				});

			const p6 = fetch('templates/userinfo.html')
				.then((response) => response.text())
				.then((data) => {
					FAPI_UI.logTemplates.USER_INFO = _.template(data);
				});

			const promises = [p1, p2, p3, p4. p5, p6];

			return Promise.allSettled(promises);
		},

		loadImageUploadTemplates: function() {
			const p1 = fetch('templates/userinfo.html')
				.then((response) => response.text())
				.then((data) => {
					FAPI_UI.logTemplates.USER_INFO = _.template(data);
				});

			const p2 = fetch('templates/pendingImageUploader.html')
				.then((response) => response.text())
				.then((data) => {
					FAPI_UI.logTemplates.PENDING = _.template(data);
				});

			const p3 = fetch('templates/existingImage.html')
				.then((response) => response.text())
				.then((data) => {
					FAPI_UI.logTemplates.EXISTING = _.template(data);
				});

			const p4 = fetch('templates/source.html')
				.then((response) => response.text())
				.then((data) => {
					FAPI_UI.logTemplates.SOURCE = _.template(data);
				});

			const p5 = fetch('templates/message.html')
				.then((response) => response.text())
				.then((data) => {
					FAPI_UI.logTemplates.MESSAGE = _.template(data);
				});

			const p6 = fetch('templates/uploadHeader.html')
				.then((response) => response.text())
				.then((data) => {
					FAPI_UI.logTemplates.UPLOAD_HEADER = _.template(data);
				});

			const promises = [p1, p2, p3, p4. p5, p6];

			return Promise.allSettled(promises);
		},

		loadTokenListTemplates: function() {
			const p1 = fetch('templates/date.html')
				.then((response) => response.text())
				.then((data) => {
					FAPI_UI.logTemplates.DATE = _.template(data);
				});

			const p2 = fetch('templates/tokenTable.html')
				.then((response) => response.text())
				.then((data) => {
					FAPI_UI.logTemplates.TOKEN_TABLE = _.template(data);
				});

			const p3 = fetch('templates/userinfo.html')
				.then((response) => response.text())
				.then((data) => {
					FAPI_UI.logTemplates.USER_INFO = _.template(data);
				});

			const promises = [p1, p2, p3];

			return Promise.allSettled(promises);
		},
		//when you add a new value to this list also update net.openid.conformance.export.LogEntryHelper
		visibleFields : ["msg", "src", "time", "result", "requirements", "upload", "testOwner", "testId", "http", "blockId", "startBlock"],

		availableTests : {},

		availablePlans : {},

		running: false,

		status: 'unknown',

		latestTestEntry: undefined,

		reloadPause: 100,

		maxReloadPause: 5000, // cap at ~5s

		resetReloadPause : function() {
			FAPI_UI.reloadPause = 100; // start at 100ms on reset
		},

		incrementReloadPause : function() {
			if (FAPI_UI.reloadPause < FAPI_UI.maxReloadPause) {
				FAPI_UI.reloadPause += Math.floor(FAPI_UI.reloadPause / 4); // increment by 25%
			}
		},

		getUserInfo : function() {
			// get the current user info
			return fetch("/api/currentuser")
				.then((response) => {
					if (! response.ok) {
						throw new Error("Network response was not OK");
					}

					return response.json();
				})
				.then((userInfo) => {
					this.currentUser = userInfo;
					const elem = document.getElementById('userInfoHolder');
					elem.innerHTML = FAPI_UI.logTemplates.USER_INFO({userInfo: userInfo});
					FAPI_UI.activeTooltip();
				})
				.catch((error) => {
					// User is not logged in; don't fill in the user info holder
				});

		},

		activeTooltip : function() {

			const tooltipTriggerList = document.querySelectorAll('[data-bs-toggle="tooltip"]')
			const tooltipList = [...tooltipTriggerList].map(tooltipTriggerEl => new bootstrap.Tooltip(tooltipTriggerEl))

		},

		getStatusHelp : function(value) {
			switch (value ? value.toLowerCase() : undefined) {
				case "waiting":
					return "The test is waiting for something to happen. For example, for the authorization server to redirect back to it, for the user to visit a link or for the user to upload an image (see the test description for details). In some cases this means the authorization server did not redirect back to the conformance suite, indicating that the test failed.";
				case "configured":
					return "The test has successfully setup the initial environment. Please read the test description and when ready press the 'START' button to begin the test.";
				case "interrupted":
					return "The test failed to run to completion as a critical element failed. Please see the log, fix the error and run the test again to get a complete set of results.";
				case "finished":
					return "The test has run to completion.";
				case "running":
					return "The test is actively executing. Reload this page to see the latest status.";
				default:
					return "";
			}
		},

		getResultHelp : function(value) {
			switch (value ? value.toLowerCase() : undefined) {
				case "passed":
					return "The test has passed all conditions.";
				case "failed":
					return "The test has failed at least one critical condition. This means an important error has been detected and the system under test cannot be certified.";
				case "warning":
					return "The test has generated some warnings during its execution, see the log for details. Test results with warnings are accepted for certification, but they generally indicate that the software under test is behaving unexpected or not following recommendations, and the tester should check the results to ensure any warnings are expected behaviour of the software being tested.";
				case "review":
					return "The test requires manual review, for example it contains images that need to be manually checked. These images will be checked by the certification team when a certification request is submitted.";
				case "skipped":
					return "The test could not be completed due to configuration or optional features. Please check if the feature being tested is supported, if it is please check the configuration of the test and of the software under test. If the feature being tested is not supported by the software under test then skipped tests do not prevent certification.";
				default:
					return "";

			}
		},

		/**
		 * Takes in a JSON object representing the error from the server and shows an error display
		 */
		showError : function(error) {
			const elem = document.getElementById('errorMessage');

			if (error != null) {
				var msg = error.error || error.code;
				if (/^\d+$/.test(msg)) {
					// Tomcat considers that HTTP status messages should not be sent,
					// so we get unhelpful responses like "HTTP/1.1 404 404". Make it
					// clear that this is an HTTP error code.
					msg = "HTTP Error " + msg;
				}
				if (error.message) {
					msg += " : " + error.message
				}
				var msgHtml = _.escape(msg);
				if (error.error == "Unauthorized") {
					msgHtml += "<br><br>Refresh the page to renew your session";
				}
				elem.innerHTML = msgHtml;
			} else {
				elem.innerHTML = 'Error from server.';
			}

			FAPI_UI.hideBusy(); // only one modal at a time
			var myModalEl = document.getElementById('errorModal');
			var modal     = bootstrap.Modal.getOrCreateInstance(myModalEl);
			modal.show();
		},

		hideError : function() {
			var myModalEl = document.getElementById('errorModal');
			var modal     = bootstrap.Modal.getInstance(myModalEl);

			if (modal != null) {
			    modal.hide();
			}
		},

		showBusy : function(label, message) {
			if (!label) {
				label = "Loading...";
			}

			var elem = document.getElementById('loadingLabel');
			elem.innerHTML = _.escape(label);
			var elem = document.getElementById('loadingMessage');
			elem.innerHTML = _.escape(message);

			FAPI_UI.hideError(); // only one modal at a time

			var myModalEl = document.getElementById('loadingModal');
			var modal     = bootstrap.Modal.getOrCreateInstance(myModalEl);
			modal.show();
		},

		hideBusy : function() {
			var myModalEl = document.getElementById('loadingModal');
			var modal     = bootstrap.Modal.getInstance(myModalEl);

			if (modal != null) {
			    modal.hide();
			}
		},

		// responsible for converting any dot syntax in our key parameter into object refs
		prop : function(obj, prop, val){
			var props = prop.split('.')
			, final = props.pop(), p;
			while(p = props.shift()){
				if (typeof obj[p] === 'undefined') {
					obj[p] = {}; // create the object
				}

				obj = obj[p];
			}

			return val ? (obj[final] = val) : obj[final];
		},

		removeFromObject : function(obj, key) {
			if (obj !== undefined) {
				var elements = key.split('.');
				if (elements.length > 1) {
					this.removeFromObject(obj[elements[0]], elements.splice(1).join('.'));
				} else {
					if (_.isObject(obj) && _.isArray(elements) && elements.length) {
						delete(obj[elements[0]]);
					}
				}
			}
		},

		formatVariant : function(v) {
			if (!v) {
				return "";
			} else if (typeof(v) === 'string') {
				return v;
			} else {
				return Object.entries(v).map(function(val, index, arr) {
					return val.join('=');
				}).join(', ');
			}
		},

	    // Hash to map requirement strings onto url
	    // Each url must have the fragment necessary to form a link to a particular section of document
		// When you add a new value to this list, also update net.openid.conformance.export.LogEntryHelper
		specLinks : {
			"BrazilOB-" : "https://openfinancebrasil.atlassian.net/wiki/spaces/OF/pages/245760001/EN+Open+Finance+Brasil+Financial-grade+API+Security+Profile+1.0+Implementers+Draft+3#section-",
			"BrazilOBDCR-" : "https://openfinancebrasil.atlassian.net/wiki/spaces/OF/pages/246120449/EN+Open+Finance+Brasil+Financial-grade+API+Dynamic+Client+Registration+2.0+RC1+Implementers+Draft+3#section-",
			"BrazilOPIN-" : "https://br-openinsurance.github.io/areadesenvolvedor/files/Controles_técnicos_de_Segurança_da_Informação_3.0.pdf#",
			"BrazilCIBA-" : "https://openfinancebrasil.atlassian.net/wiki/spaces/DraftOF/pages/138674330/EN+Open+Banking+Brasil+Financial-grade+CIBA+API+Security+Profile+1.0+Implementers+Draft+3",
			"FAPI-R-" : "https://openid.net/specs/openid-financial-api-part-1-ID2.html#rfc.section.",
			"FAPI-RW-" : "https://openid.net/specs/openid-financial-api-part-2-ID2.html#rfc.section.",
			"FAPI1-BASE-" : "https://openid.net/specs/openid-financial-api-part-1-1_0-final.html#rfc.section.",
			"FAPI1-ADV-" : "https://openid.net/specs/openid-financial-api-part-2-1_0-final.html#rfc.section.",
			"FAPI2-SP-ID2-" : "https://openid.net/specs/fapi-2_0-security-profile-ID2.html#section-",
			"FAPI2-MS-ID1-" : "https://openid.net/specs/fapi-2_0-message-signing-ID1.html#section.", // note that MS ID1 builds upon SP ID2
			"FAPI2-IMP-" : "https://openid.bitbucket.io/fapi/fapi-2_0-implementation_advice.html#section-",
			"CIBA-" : "https://openid.net/specs/openid-client-initiated-backchannel-authentication-core-1_0.html#rfc.section.",
			"FAPI-CIBA-" : "https://openid.net/specs/openid-financial-api-ciba.html#rfc.section.",
			"JARM-" : "https://openid.net/specs/oauth-v2-jarm.html#section-",
			"OB-" : "https://bitbucket.org/openid/obuk/src/b36035c22e96ce160524066c7fde9a45cbaeb949/uk-openbanking-security-profile.md?at=master&fileviewer=file-view-default#",
			"OBRW-" : "https://openbanking.atlassian.net/wiki/spaces/DZ/pages/1077805207/Read+Write+Data+API+Specification+-+v3.1.2#",
			"OIDCC-" : "https://openid.net/specs/openid-connect-core-1_0.html#rfc.section.",
			"OIDCR-" : "https://openid.net/specs/openid-connect-registration-1_0.html#rfc.section.",
			"OAuth2-FP" : "https://openid.net/specs/oauth-v2-form-post-response-mode-1_0.html#rfc.section.",
			"OAuth2-iss" : "https://tools.ietf.org/html/rfc9207#section-",
			"RFC3986-" :"https://tools.ietf.org/html/rfc3986#section-",
			"RFC6749-" : "https://tools.ietf.org/html/rfc6749#section-",
			"RFC6749A-" : "https://tools.ietf.org/html/rfc6749#appendix-",
			"RFC6750-" : "https://tools.ietf.org/html/rfc6750#section-",
			"RFC6819-" : "https://tools.ietf.org/html/rfc6819#section-",
			"RFC7231-" : "https://tools.ietf.org/html/rfc7231#section-",
			"RFC7517-" : "https://tools.ietf.org/html/rfc7517#section-",
			"RFC7518-" : "https://tools.ietf.org/html/rfc7518#section-",
			"RFC7519-" : "https://tools.ietf.org/html/rfc7519#section-",
			"RFC7523-" : "https://tools.ietf.org/html/rfc7523#section-",
			"RFC7591-" : "https://tools.ietf.org/html/rfc7591#section-",
			"RFC7592-" : "https://tools.ietf.org/html/rfc7592#section-",
			"RFC7592A-" : "https://tools.ietf.org/html/rfc7592#appendix-",
			"RFC7636-" : "https://tools.ietf.org/html/rfc7636#section-",
			"RFC8705-" : "https://tools.ietf.org/html/rfc8705#section-",
			"RFC8707-" :  "https://tools.ietf.org/html/rfc8707#section-",
			"RFC8485-" :  "https://tools.ietf.org/html/rfc8485#section-",
			"RFC9396-" :  "https://tools.ietf.org/html/rfc9396#section-",
			"RFC9325-": "https://tools.ietf.org/html/rfc9325.html#section-",
			"RFC9325A-": "https://tools.ietf.org/html/rfc9325.html#appendix-",
			"OBSP-" : "https://openbanking.atlassian.net/wiki/spaces/DZ/pages/83919096/Open+Banking+Security+Profile+-+Implementer+s+Draft+v1.1.2#",
			"OAuth2-RT-" : "https://openid.net/specs/oauth-v2-multiple-response-types-1_0.html#rfc.section.",
			"HAIP-" : "https://github.com/vcstuff/oid4vc-haip-sd-jwt-vc/blob/main/draft-oid4vc-haip-sd-jwt-vc.md#",
			"OID4VP-ID2-" : "https://openid.net/specs/openid-4-verifiable-presentations-1_0-ID2.html#section.",
			"OID4VP-ID3-" : "https://openid.net/specs/openid-4-verifiable-presentations-1_0-ID3.html#section.",
			"OIDCD-" : "https://openid.net/specs/openid-connect-discovery-1_0.html#rfc.section.",
			"OIDCBCL-" : "https://openid.net/specs/openid-connect-backchannel-1_0.html#rfc.section.",
			"OIDCFCL-" : "https://openid.net/specs/openid-connect-frontchannel-1_0.html#rfc.section.",
			"OIDCSM-" : "https://openid.net/specs/openid-connect-session-1_0.html#rfc.section.",
			"OIDCRIL-" : "https://openid.net/specs/openid-connect-rpinitiated-1_0.html#rfc.section.",
			"BCP195-" : "https://tools.ietf.org/html/bcp195#section-",
			"ISO18013-7-" : "https://www.iso.org/standard/82772.html#",
			"CDR-" : "https://consumerdatastandardsaustralia.github.io/standards/#",
			"PAR-" : "https://www.rfc-editor.org/rfc/rfc9126.html#section-",
			"JAR-" : "https://www.rfc-editor.org/rfc/rfc9101.html#section-",
			"SDJWT-" : "https://www.ietf.org/archive/id/draft-ietf-oauth-selective-disclosure-jwt-14.html#section-",
			"SDJWTVC-" : "https://datatracker.ietf.org/doc/draft-ietf-oauth-sd-jwt-vc/#section-",
			"IA-" : "https://openid.net/specs/openid-connect-4-identity-assurance-1_0.htm#section-",
			"IAVC-" : "https://openid.net/specs/openid-ida-verified-claims-1_0.htm#section-",
			"DPOP-" : "https://www.rfc-editor.org/rfc/rfc9449#section-",
			"KSA" : "https://ksaob.atlassian.net/wiki/spaces/KS20221101finalerrata1/pages/61014862/API+Security",
			"OIDSSF-": "https://openid.net/specs/openid-sharedsignals-framework-1_0-ID3.html#section-",
			"OIDCAEP-": "https://openid.net/specs/openid-caep-1_0-ID2.html#section-",
			"CAEPIOP-": "https://openid.net/specs/openid-caep-interoperability-profile-1_0-ID1.html#section-",
			"CID-SP-" : "https://cdn.connectid.com.au/specifications/digitalid-fapi-profile-01.html#section",
			"CID-IDA-" : "https://cdn.connectid.com.au/specifications/digitalid-identity-assurance-profile-06.html#section",
			"CID-PURPOSE-": "https://cdn.connectid.com.au/specifications/oauth2-purpose-01.html#section",
			"OIDFED-": "https://openid.net/specs/openid-federation-1_0-42.html#section-",
			"CBUAE": "https://openfinanceuae.atlassian.net/wiki/spaces/standardsv1final/pages/151846988/Security+Profile+-+FAPI"
		}, // When you add a new value to this list, also update net.openid.conformance.export.LogEntryHelper

		testJSON : {},

		selectedVariant: undefined

};
