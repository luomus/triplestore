<script>
$(function() {

	$("#toolboxToggle").on('click', function() {
		$("#toolBoxContent").toggle();
	});
	
    $(document).ajaxError(function(event, response, settings, thrownError) {
    	if (settings.suppressErrors) {
        	return;
    	}
    	if (!response.status) {
    		return; // ignore, cancelled request
    	} 
    	if (response.status == 403) {
        	document.location.href = document.location.href;
		} else {
			document.location.href = '${baseURL}/error?error='+encodeURIComponent(settings.url + ': ' + response.status + ' : ' + thrownError);
		}
	});
	
	initColumnsPortlets();
	
});
	
function editTaxon(e) {
	editTaxon(e, false);
}
function editTaxon(e, fullEditMode) {
	if ($("#taxonDragMode").prop('checked') === true) return;
	if (toolsDisabled) return;
	var taxon = $(e).closest(".taxonWithTools").attr('id');
	if (typeof taxon == 'undefined') {
		alert('Taxon to edit has not been defined! Please report how you got this error, I have not been able to reproduce it! -Esko');
		return;
	}
	$("#editTaxonContent").html('<div class="loading">Loading...</div>');
	$("#editTaxon").dialog("open");
	$.get("${baseURL}/api/taxonToEdit/"+taxon+"?fullEditMode="+fullEditMode, function(data) {
		$("#editTaxonContent").html(data);
		initColumnsPortlets();
  	});
}

function initColumnsPortlets() {

	$("#fixTypo").on('click', function() {
		$("#scientificNameToolButtons, #originalNamesView").fadeOut('fast', function() {
			$("#originalNamesInputs").fadeIn('slow', function() {
				$("#scientificNameHelp").html('<p class="info">Type in fixed name and save</p>').fadeIn(function() {
					var e = $("#originalNamesInputs").find("input.scientificName").first(); 
					var temp = e.val();
					e.val('').val(temp).focus(); // setval trick is to get focus to last char
				});
			});
		});
		return false; 
	});
	
	$("#alterScientificName").on('click', function() {
		$("#scientificNameToolButtons, #originalNamesView").fadeOut('fast', function() {
			$("#alteredNamesInputs").fadeIn('slow', function() {
					$("#scientificNameHelp").html('<p class="info">Type in new name/authors and save. A new synonym is automatically created for the old name.</p>').fadeIn(function() {
						$("#alteredScientificName").focus(); 
					});
				});
		});
		return false;
	});
	
	$("#editTaxonContent").find(".chosen").chosen({ search_contains: true, allow_single_deselect: true });
	
	<#if !(noPermissions??)>
	$(".nameDecidedDate").attr('placeholder', 'yyyy-mm-dd').datepicker({ showOn: "both", dateFormat: "yy-mm-dd", defaultDate: 0, firstDay: 1, maxDate: new Date });
	
	$("<button>Me</button>").on('click', function() {
		var myQname = '${user.qname}';
		$("#nameDecidedBy").val(myQname).trigger("chosen:updated").change();
		return false;
	}).insertAfter($("#nameDecidedBy_chosen"));
	</#if>

	$("#editTaxonContent").find("button, .button").button();
	
	$(".taxonEditSection").find(":input").bind("change keyup", function() {
		addSaveButtonTo(this);
	});
	
	<#if !(noPermissions??)>
	$(".multirowSection").each(function() {
		var addNewRowButton = $('<a href="#" class="addNewItem">+ Add new</a>');
		$(this).find('table').after(addNewRowButton);
		addNewRowButton.click(function() {
			var tableToAddNewRow = $(this).parent().find('table'); 
			var rowClone = $(tableToAddNewRow).find('tr').last().clone();
			rowClone.find('.chosen-container').remove();
			var clonedInput = rowClone.find(':input').first().val('').show().removeAttr('display');
			if (clonedInput.hasClass('chosen')) clonedInput.chosen({ search_contains: true, allow_single_deselect: true });
			rowClone.find('.chosen-container').removeAttr('style');
			var name = clonedInput.attr("name");
			if (name.indexOf("___") > -1) {
				name = name.split("___")[0] + "___fi";
				clonedInput.attr('name', name);
				rowClone.find('select').val('fi');
			}
			tableToAddNewRow.append(rowClone);
			addSaveButtonTo(this);
			return false;
		});
	});
	</#if>
	
	function addSaveButtonTo(e) {
		var section = $(e).closest(".taxonEditSection"); 
		if (section.find(".saveButton").length === 0) {
			section.append('<input type="submit" class="saveButton" value="Save" />');
			section.append('<input type="submit" class="saveButton saveAndClose" value="Save and close" />');
			section.find(".saveButton").button().hide().fadeIn('slow');
			section.find(".saveButton").on('click', function() {
				$(this).addClass('clickedSaveButton');
			});
		}
	}
	
	$(".portlet").addClass("ui-widget ui-widget-content ui-helper-clearfix ui-corner-all")
	.find(".portlet-header").addClass("ui-widget-header ui-corner-all")
	.prepend("<span class='ui-icon ui-icon-minusthick'></span>")
	.end().find(".portlet-content");
	
	$(".portlet-header").click(function() {
		$(this).find(".ui-icon").toggleClass("ui-icon-minusthick").toggleClass("ui-icon-plusthick");
		$(this).parent().find(".portlet-content").toggle();
	}).disableSelection();
	
	$(".initiallyClosed").each(function() {
		$(this).find("span")
		.toggleClass("ui-icon-minusthick")
		.toggleClass("ui-icon-plusthick");
		$(this).parents(".portlet:first").find(".portlet-content").hide();
	});
	
	$(".taxonEditSection").submit(function() {
		if (!$(this).valid()) return false;
		var clickedButton = $(this).find('.clickedSaveButton');
		var closeAfter = clickedButton.hasClass('saveAndClose');
		$(this).find(".saveButton").remove();
		submitTaxonEditSection(this, closeAfter);
		return false;
	});
}

function submitTaxonEditSection(section, closeAfter) {
	var values = $(section).serialize();
	var classes = $(section).attr("class").toString();
	$.ajax({
        url: "${baseURL}/api/taxonEditSectionSubmit?classes="+classes,
        type: "post",
        data: values,
        success: function(data) {
        	showSuccess(section, data);
        	afterTaxonEditSectionSubmit(section, closeAfter);
        }
    });
}

function afterTaxonEditSectionSubmit(section, closeAfter) {
	var section = $(section);
	if (section.hasClass("scientificNameSection")) {
		var qname = section.find("input.taxonQname").first().val();
		var taxonRank = section.find("select.taxonRank").first().val();
		var scientificName = section.find("input.scientificName").first().val();
		var author = section.find("input.scientificNameAuthorship").first().val();
		var alteredScientificName = $("#alteredScientificName").val();
		var alteredAuthor = $("#alteredAuthor").val();
		scientificName = (alteredScientificName) ? alteredScientificName : scientificName;
		author = (alteredAuthor) ? alteredAuthor : author;
			
       	updateRankScientificNameAndAuthorToEditHeader(scientificName, author);
       	updateRankScientificNameAndAuthorToEditSection(scientificName, author);
        	
       	$("#scientificNameToolButtons, #originalNamesView").fadeIn();
       	$("#scientificNameHelp, #alteredNamesInputs, #originalNamesInputs").fadeOut();
        	
   		var taxon = $("#"+qname.replace("MX.", "MX"));
		$.get("${baseURL}/api/singleTaxonInfo/"+qname, function(data) {
			taxon.replaceWith(data);
			taxon = $("#"+qname.replace("MX.", "MX"));
			taxon.find('button, .button').button();
			taxonTreeGraphs.repaintEverything();
		});				
    } 
    else if (section.hasClass("primaryVernacularNameSection")) {
      	var qname = section.find(".taxonQname").first().val();
		var nameFi = section.find(".vernacularName___fi").first().val();
		var nameSv = section.find(".vernacularName___sv").first().val();
		var nameEn = section.find(".vernacularName___en").first().val();
		updateVernacularNameToTree(qname, nameFi, 'FI');
		updateVernacularNameToTree(qname, nameSv, 'SV');
		updateVernacularNameToTree(qname, nameEn, 'EN');
    }
    if (closeAfter) {
		$("#editTaxon").dialog("close");
	}
}

function updateRankScientificNameAndAuthorToEditSection(scientificName, author) {
   	$("#originalNamesView").find('.scientificName').first().text(scientificName);
   	$("#originalNamesView").find('.author').first().text(author);
}
	
function updateRankScientificNameAndAuthorToTree(qname, taxonRank, scientificName, author) {
	var taxon = $("#"+qname.replace("MX.", "MX"));
	taxon.find(".taxonRank").first().text("["+taxonRank.replace("MX.", "")+"]");
	taxon.find(".scientificName").first().text(scientificName);
	taxon.find(".author").first().text(author);
}
	
function updateVernacularNameToTree(qname, name, langcode) {
	var taxon = $("#"+qname.replace("MX.", "MX"));
	taxon.find(".vernacularName"+langcode).first().text(name);
}

function updateRankScientificNameAndAuthorToEditHeader(scientificName, author) {
	var header = $("#taxonEditHeader");
	header.find(".scientificName").first().text(scientificName);
	header.find(".author").first().text(author);
}
	
function showSuccess(section, data) {
	$(".success").remove();
	var successText = $('<div class="success">Saved!</div>');
	$(section).append(successText);
	$(section).append(data);
	if ($("#validationDialog").length == 0) {
		successText.hide().fadeIn('fast', function(){successText.fadeOut(2000,function() {successText.remove()})});
	} else {
		$("#validationDialog").dialog({
			modal: true, height: 'auto', width: 600, 
			close: function() { 
				$("#validationDialog").remove();
				successText.hide().fadeIn('fast', function(){successText.fadeOut(2000,function() {successText.remove()})});
			}
		});
	}
}

function languageChanged(languageSelector) {
	var selectedLangcode = $(languageSelector).val();
	var input = $(languageSelector).closest('tr').find('input[type="text"]');
	var name = input.attr("name");
	name = name.split("___")[0] + "___" + selectedLangcode;
	input.attr("name", name);
}



$(window).on('beforeunload', function() {
	if ($(".saveButton").length > 0) {
		return "foo";
	}
	return undefined;
});

function addedOccurrenceInFinlandPublication(qname, citation) {
	publicationAdded(qname, citation, '#occurrenceInFinlandPublicationSelector');
}
function addedPublication(qname, citation) {
	publicationAdded(qname, citation, '#originalPublicationSelector');
}

function publicationAdded(qname, citation, selector) {
	$(selector).append('<option value="'+qname+'" selected="selected">'+citation+'</option>');
	$(selector).trigger("chosen:updated");
	$(selector).closest('.taxonEditSection').find('.newPublicationInput').val('');
}

</script>