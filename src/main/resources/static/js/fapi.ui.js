var FAPI_UI = {
		
	visibleFields : ["msg", "src", "time", "result", "requirements", "upload"],

	goHome : function() {
		window.location.replace("logs.html");
	},

	/**
	 * 
	 */
	loadAvailableLogs : function(url) {
		$.ajax({ 
	        type: 'GET', 
	        url: "log", 
	        data: {}, 
	        success: function (data) { 
	        	$.each(data, function(i, item) {
	        		$("#logs .content").append("<div class='link'><a href='log-detail.html?log="+item+"' class='item'>"+item+"</a></div>");
	        	});
	        }
	    });
	},

	/**
	 * 
	 */
	showLogDetail : function(fileId) {

		$.ajax({ 
	        type: 'GET', 
	        url: "log/" + encodeURIComponent(fileId), 
	        data: {}, 
	        success: function (data) { 

	            $.each(data, function(i, item) {

	            	var itemDisp = $('<div class="item">' + 
	            			'<div class="defaultdisplay"></div>' + 
	            			'<button class="btn btn-default btn-xs pull-right"><span class="badge"></span> More <span class="glyphicon glyphicon-chevron-down" aria-hidden="true"></span></button>' + 
	            			'<div class="moreinfo collapse"></div>' + 
	            			'<div class="clearfix"></div>' +
	            			'</div>');
	            	
	            	var hidden = 0;
	            	
	            	$.each(item, function(key, val) {
	 
	            		if (!key.startsWith("_") // filter out anything starting with an underscore as it's injected by Mongo
	            				&& key != 'testId' // we also don't need to display the test ID
	            				) {

	            			// build the key/value display object
	            			var wordwrap = $('<div class="wordwrap"></div>');
	            			
	            			wordwrap.append($('<span class="key">' + _.escape(key) + '</span><span class="hypen"> : </span>'));
	            			
	            			if (key == "time") {
	            				// format it as a timestamp value
	            				wordwrap.append($("<span class='value'>" + new Date(val) + "</span>"));
	            			} else if (key == "result") {
	            				// format it as a result value
	            				if (val == 'SUCCESS') {
	            					wordwrap.append($('<span class="value"><span class="label label-success">' + _.escape(val) + "</span></span>"));
	            				} else if (val == 'FAILURE') {
	            					wordwrap.append($('<span class="value"><span class="label label-danger">' + _.escape(val) + "</span></span>"));
	            				} else if (val == 'WARNING') {
	            					wordwrap.append($('<span class="value"><span class="label label-warning">' + _.escape(val) + "</span></span>"));
	            				} else if (val == 'REVIEW') {
	            					wordwrap.append($('<span class="value"><span class="label label-info">' + _.escape(val) + "</span></span>"));
	            				} else {
	            					wordwrap.append($('<span class="value"><span class="label label-primary">' + _.escape(val) + "</span></span>"));
	            				}
	            			} else if (key == "requirements") {
	            				_.each(val, function(req){
	            					wordwrap.append($('<span class="value"><span class="label label-default">' + _.escape(val)  + "</span></span>"))
	            				});
	            			} else if (key == 'img') {
	            				wordwrap.append($('<span class="value"><img src="' + _.escape(val) + '"></span>'));
	            			} else if (key == 'upload') {
	            				wordwrap.append($('<span class="value"><span class="label label-warning">IMAGE REQUIRED</span> <a class="btn btn-primary" href="/upload.html?log=' + encodeURIComponent(fileId) + '&placeholder=' + encodeURIComponent(val) + '">Attach image...</a></span>'))
	            			} else {
	            				// default formatting
	            				if (_.isString(val)) {
	            					wordwrap.append($('<span class="value">' + _.escape(val) + "</span>"));
	            				} else {
	            					wordwrap.append($('<span class="value">' + _.escape(JSON.stringify(val)) + "</span>"));
	            				}
	            			}

	            			if (_.contains(FAPI_UI.visibleFields, key)) {
	            				// these fields are visible right away
	            				$('.defaultdisplay', itemDisp).append(wordwrap);
	            			} else {
	            				// anything else goes into the "moreinfo" block
	            				$('.moreinfo', itemDisp).append(wordwrap);	   
	            				hidden += 1;
	            			}
	            		}
	            	});
	            	
	            	$("#logDetail .content").append(itemDisp);
	            	
	            	if (hidden > 0) {
	            		$('button .badge', itemDisp).text(hidden);
	            		
	            		// wire up the "more" button
	            		$('button', itemDisp).click(function(evt) {
	            			if ($(this).hasClass('activated')) {
	            				// it's already been activated, need to hide things
	            				$('.moreinfo', itemDisp).hide(); // hide the content
	            				$('.glyphicon', this).removeClass('glyphicon-chevron-up').addClass('glyphicon-chevron-down');
	            				$(this).removeClass('activated');
	            			} else {
	            				// need to show the collapsed entity
	            				$('.moreinfo', itemDisp).show(); // show the content
	            				$('.glyphicon', this).removeClass('glyphicon-chevron-down').addClass('glyphicon-chevron-up');
	            				$(this).addClass('activated');
	            			}
	            			
	            		});
	            	} else {
	            		// hide the button if we don't need it
	            		$('button', itemDisp).hide();
	            	}


	            });

	        }
	    });

	}
	
}

