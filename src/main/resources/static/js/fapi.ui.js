var FAPI_UI = {

		logTemplates : {},

		loadHomepageTemplates : function() {
			return $.when(
				$.get('templates/userinfo.html', function(data) {
					FAPI_UI.logTemplates.USER_INFO = _.template(data);
				})
			);
		},

		loadScheduleTestPageTemplates : function() {
			return $.when(
					$.get('templates/testOption.html', function(data) {
						FAPI_UI.logTemplates.TEST_OPTION = _.template(data);
					}),
					$.get('templates/testOptGroup.html', function(data) {
						FAPI_UI.logTemplates.TEST_OPTGROUP = _.template(data);
					}),
					$.get('templates/userinfo.html', function(data) {
						FAPI_UI.logTemplates.USER_INFO = _.template(data);
					})
			);
		},

		loadRunningTestPageTemplates : function() {
			return $.when(
				$.get('templates/runningTest.html', function(data) {
					FAPI_UI.logTemplates.RUNNING_TEST = _.template(data);
				}),
				$.get('templates/owner.html', function(data) {
					FAPI_UI.logTemplates.OWNER = _.template(data);
				}),
				$.get('templates/statusAndResult.html', function(data) {
					FAPI_UI.logTemplates.TEST_STATUS = _.template(data);
				}),
				$.get('templates/userinfo.html', function(data) {
					FAPI_UI.logTemplates.USER_INFO = _.template(data);
				})
			);
		},

		loadLogDetailTemplates: function() {
			return $.when(
					$.get('templates/userinfo.html', function(data) {
						FAPI_UI.logTemplates.USER_INFO = _.template(data);
					}),

					$.get('templates/logHeader.html', function(data) {
						FAPI_UI.logTemplates.LOG_START = _.template(data);
					}),

					$.get('templates/logEntry.html', function(data) {
						FAPI_UI.logTemplates.LOG_DETAIL = _.template(data);
					}),

					$.get('templates/source.html', function(data) {
						FAPI_UI.logTemplates.SOURCE = _.template(data);
					}),

					$.get('templates/message.html', function(data) {
						FAPI_UI.logTemplates.MESSAGE = _.template(data);
					}),

					$.get('templates/requirements.html', function(data) {
						FAPI_UI.logTemplates.REQUIREMENTS = _.template(data);
					}),

					$.get('templates/upload.html', function(data) {
						FAPI_UI.logTemplates.UPLOAD = _.template(data);
					}),

					$.get('templates/owner.html', function(data) {
						FAPI_UI.logTemplates.OWNER = _.template(data);
					}),

					$.get('templates/result.html', function(data) {
						FAPI_UI.logTemplates.RESULT = _.template(data);
					}),

					$.get('templates/time.html', function(data) {
						FAPI_UI.logTemplates.TIME = _.template(data);
					}),

					$.get('templates/more.html', function(data) {
						FAPI_UI.logTemplates.MORE = _.template(data);
					}),

					$.get('templates/moreButton.html', function(data) {
						FAPI_UI.logTemplates.MORE_BUTTON = _.template(data);
					}),

					$.get('templates/exported.html', function(data) {
						FAPI_UI.logTemplates.EXPORTED = _.template(data);
					}),

					$.get('templates/browser.html', function(data) {
						FAPI_UI.logTemplates.BROWSER = _.template(data);
					}),

					$.get('templates/http.html', function(data) {
						FAPI_UI.logTemplates.HTTP = _.template(data);
					}),

					$.get('templates/finalError.html', function(data) {
						FAPI_UI.logTemplates.FINAL_ERROR = _.template(data);
					}),

					$.get('templates/statusAndResult.html', function(data) {
						FAPI_UI.logTemplates.TEST_STATUS = _.template(data);
					}),

					$.get('templates/resultsSummary.html', function(data) {
						FAPI_UI.logTemplates.SUMMARY = _.template(data);
					}),

					$.get('templates/startBlock.html', function(data) {
						FAPI_UI.logTemplates.START_BLOCK = _.template(data);
					}),

					$.get('templates/failureSummary.html', function(data) {
						FAPI_UI.logTemplates.FAILURE_SUMMARY = _.template(data);
					})
			);

		},

		loadLogListTemplates: function() {
			return $.when(
					$.get('templates/configButton.html', function(data) {
						FAPI_UI.logTemplates.CONFIG = _.template(data);
					}),

					$.get('templates/date.html', function(data) {
						FAPI_UI.logTemplates.DATE = _.template(data);
					}),

					$.get('templates/logDetailButton.html', function(data) {
						FAPI_UI.logTemplates.LOG_DETAIL = _.template(data);
					}),

					$.get('templates/owner.html', function(data) {
						FAPI_UI.logTemplates.OWNER = _.template(data);
					}),

					$.get('templates/planDetailButton.html', function(data) {
						FAPI_UI.logTemplates.PLAN_DETAIL = _.template(data);
					}),

					$.get('templates/userinfo.html', function(data) {
						FAPI_UI.logTemplates.USER_INFO = _.template(data);
					})
			);

		},

		loadPlanTemplates: function() {
			return $.when(
					$.get('templates/userinfo.html', function(data) {
						FAPI_UI.logTemplates.USER_INFO = _.template(data);
					}),

					$.get('templates/plan.html', function(data) {
						FAPI_UI.logTemplates.PLAN_START = _.template(data);
					}),

					$.get('templates/owner.html', function(data) {
						FAPI_UI.logTemplates.OWNER = _.template(data);
					}),

					$.get('templates/statusAndResult.html', function(data) {
						FAPI_UI.logTemplates.TEST_STATUS = _.template(data);
					}),

					$.get('templates/testVersion.html', function(data) {
						FAPI_UI.logTemplates.TEST_VERSION = _.template(data);
					})
			);

		},

		loadPlanListTemplates: function() {
			return $.when(
					$.get('templates/configButton.html', function(data) {
						FAPI_UI.logTemplates.CONFIG = _.template(data);
					}),

					$.get('templates/date.html', function(data) {
						FAPI_UI.logTemplates.DATE = _.template(data);
					}),

					$.get('templates/planDetailButton.html', function(data) {
						FAPI_UI.logTemplates.PLAN_DETAIL = _.template(data);
					}),

					$.get('templates/planModules.html', function(data) {
						FAPI_UI.logTemplates.PLAN_MODULES = _.template(data);
					}),

					$.get('templates/owner.html', function(data) {
						FAPI_UI.logTemplates.OWNER = _.template(data);
					}),

					$.get('templates/userinfo.html', function(data) {
						FAPI_UI.logTemplates.USER_INFO = _.template(data);
					})
			);
		},

		loadImageUploadTemplates: function() {
			return $.when(
					$.get('templates/userinfo.html', function(data) {
						FAPI_UI.logTemplates.USER_INFO = _.template(data);
					}),

					$.get('templates/pendingImageUploader.html', function(data) {
						FAPI_UI.logTemplates.PENDING = _.template(data);
					}),

					$.get('templates/existingImage.html', function(data) {
						FAPI_UI.logTemplates.EXISTING = _.template(data);
					}),

					$.get('templates/source.html', function(data) {
						FAPI_UI.logTemplates.SOURCE = _.template(data);
					}),

					$.get('templates/message.html', function(data) {
						FAPI_UI.logTemplates.MESSAGE = _.template(data);
					}),

					$.get('templates/uploadHeader.html', function(data) {
						FAPI_UI.logTemplates.UPLOAD_HEADER = _.template(data);
					})
			);

		},

		loadTokenListTemplates: function() {
			return $.when(
					$.get('templates/date.html', function(data) {
						FAPI_UI.logTemplates.DATE = _.template(data);
					}),

					$.get('templates/tokenTable.html', function(data) {
						FAPI_UI.logTemplates.TOKEN_TABLE = _.template(data);
					}),

					$.get('templates/userinfo.html', function(data) {
						FAPI_UI.logTemplates.USER_INFO = _.template(data);
					})
			);
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
			var done = $.Deferred();
			$.getJSON({
				url: '/api/currentuser',
				context: this
			}).done(function(userInfo) {
				this.currentUser = userInfo;
				$('#userInfoHolder').html(FAPI_UI.logTemplates.USER_INFO({userInfo: userInfo}));
				FAPI_UI.activeTooltip();
			}).fail(function() {
				// User is not logged in; don't fill in the user info holder
			}).always(function() {
				done.resolve();
			});
			return done.promise();
		},

		activeTooltip : function() {
			$('[data-toggle="tooltip"]').tooltip({
				container: 'body'
			});
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
				$('#errorMessage').html(msgHtml);
			} else {
				$('#errorMessage').html('Error from server.');
			}

			FAPI_UI.hideBusy(); // only one modal at a time
			$('#errorModal').modal('show');
		},

		hideError : function() {
			$('#errorModal').modal('hide');
		},

		showBusy : function(label, message) {
			if (!label) {
				label = "Loading...";
			}

			$('#loadingLabel').html(_.escape(label));
			$('#loadingMessage').html(_.escape(message));

			FAPI_UI.hideError(); // only one modal at a time
			$('#loadingModal').modal('show');
		},

		hideBusy : function() {
			$('#loadingModal').modal('hide');
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
			var elements = key.split('.');
			if (elements.length > 1) {
				this.removeFromObject(obj[elements[0]], elements.splice(1).join('.'));
			} else {
				if (obj !== undefined && _.isObject(obj) && _.isArray(elements) && elements.length) {
					delete(obj[elements[0]]);
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
			"BrazilOB-" : "https://openbanking-brasil.github.io/specs-seguranca/open-banking-brasil-financial-api-1_ID1.html#section-",
			"BrazilOBDCR-" : "https://openbanking-brasil.github.io/specs-seguranca/open-banking-brasil-dynamic-client-registration-1_ID1.html#section-",
			"FAPI-R-" : "https://openid.net/specs/openid-financial-api-part-1-ID2.html#rfc.section.",
			"FAPI-RW-" : "https://openid.net/specs/openid-financial-api-part-2-ID2.html#rfc.section.",
			"FAPI1-BASE-" : "https://openid.net/specs/openid-financial-api-part-1-1_0-final.html#rfc.section.",
			"FAPI1-ADV-" : "https://openid.net/specs/openid-financial-api-part-2-1_0-final.html#rfc.section.",
			"CIBA-" : "https://openid.net/specs/openid-client-initiated-backchannel-authentication-core-1_0.html#rfc.section.",
			"FAPI-CIBA-" : "https://openid.net/specs/openid-financial-api-ciba.html#rfc.section.",
			"JARM-" : "https://openid.net//specs/openid-financial-api-jarm-wd-01.html#rfc.section.",
			"OB-" : "https://bitbucket.org/openid/obuk/src/b36035c22e96ce160524066c7fde9a45cbaeb949/uk-openbanking-security-profile.md?at=master&fileviewer=file-view-default#",
			"OBRW-" : "https://openbanking.atlassian.net/wiki/spaces/DZ/pages/1077805207/Read+Write+Data+API+Specification+-+v3.1.2#",
			"OIDCC-" : "https://openid.net/specs/openid-connect-core-1_0.html#rfc.section.",
			"OIDCR-" : "https://openid.net/specs/openid-connect-registration-1_0.html#rfc.section.",
			"OAuth2-FP" : "https://openid.net/specs/oauth-v2-form-post-response-mode-1_0.html#rfc.section.",
			"OAuth2-iss" : "https://tools.ietf.org/html/draft-ietf-oauth-iss-auth-resp-00#section.",
			"RFC6749-" : "https://tools.ietf.org/html/rfc6749#section-",
			"RFC6749A-" : "https://tools.ietf.org/html/rfc6749#appendix-",
			"RFC6819-" : "https://tools.ietf.org/html/rfc6819#section-",
			"RFC7231-" : "https://tools.ietf.org/html/rfc7231#section-",
			"RFC7517-" : "https://tools.ietf.org/html/rfc7517#section-",
			"RFC7518-" : "https://tools.ietf.org/html/rfc7518#section-",
			"RFC7519-" : "https://tools.ietf.org/html/rfc7519#section-",
			"RFC7523-" : "https://tools.ietf.org/html/rfc7523#section-",
			"RFC7591-" : "https://tools.ietf.org/html/rfc7591#section-",
			"RFC8705-" : "https://tools.ietf.org/html/rfc8705#section-",
			"HEART-OAuth2-" : "http://openid.net/specs/openid-heart-oauth2-1_0-2017-05-31.html#rfc.section.",
			"OBSP-" : "https://openbanking.atlassian.net/wiki/spaces/DZ/pages/83919096/Open+Banking+Security+Profile+-+Implementer+s+Draft+v1.1.2#",
			"OAuth2-RT-" : "https://openid.net/specs/oauth-v2-multiple-response-types-1_0.html#rfc.section.",
			"OIDCD-" : "https://openid.net/specs/openid-connect-discovery-1_0.html#rfc.section.",
			"OIDCBCL-" : "https://openid.net/specs/openid-connect-backchannel-1_0.html#rfc.section.",
			"OIDCFCL-" : "https://openid.net/specs/openid-connect-frontchannel-1_0.html#rfc.section.",
			"OIDCSM-" : "https://openid.net/specs/openid-connect-session-1_0.html#rfc.section.",
			"OIDCRIL-" : "https://openid.net/specs/openid-connect-rpinitiated-1_0.html#rfc.section.",
			"BCP195-" : "https://tools.ietf.org/html/bcp195#section-",
			"CDR-" : "https://consumerdatastandardsaustralia.github.io/standards/#",
			"PAR-" : "https://tools.ietf.org/html/draft-ietf-oauth-par#section-",
			"JAR-" : "https://tools.ietf.org/html/draft-ietf-oauth-jwsreq#section-"
		}, // When you add a new value to this list, also update net.openid.conformance.export.LogEntryHelper

		testJSON : {},

		selectedVariant: undefined

};
