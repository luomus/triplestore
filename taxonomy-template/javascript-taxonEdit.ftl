<script>
$(function() {

	$("#toolboxToggle").on('click', function() {
		$("#toolBoxContent").toggle();
	});
	
    $(document).ajaxError(function(event, response, settings, thrownError) {
    	if (!response.status) {
    		// ignore, cancelled request
    		alert('I ignored this! Well, I would if you remove this line!');
    	} else if (response.status == 403) {
        	document.location.href = document.location.href;
		} else {
			document.location.href = '${baseURL}/error?error='+encodeURIComponent(settings.url + ': ' + response.status + ' : ' + thrownError);
		}
	});
	
	initColumnsPortlets();
	
});
	
function editTaxon(e) {
	if ($(".saveButton").length > 0) {
		if (!confirm('This taxon has unsaved changes. Are you sure you want to change to a different taxon?')) return;
	}
	if ($("#taxonDragMode").prop('checked') === true) return;
	if (toolsDisabled) return;
	var taxon = $(e).closest(".taxonWithTools").attr('id');
	if (typeof taxon == 'undefined') {
		alert('Taxon to edit has not been defined! Please report how you got this error, I have not been able to reproduce it! -Esko');
		return;
	}
	$("#editTaxonContent").html('<div class="loading">Loading...</div>');
	$.get("${baseURL}/api/taxonToEdit/"+taxon, function(data) {
		$("html").animate({
        	scrollTop: $("#editTaxon").offset().top
    	}, 200, "swing", function() {
    		$("#editTaxonContent").html(data);
			initColumnsPortlets();
    	});
  	});
}

function initColumnsPortlets() {

	$("#editTaxonContent").find(".chosen").chosen({ search_contains: true, allow_single_deselect: true });
	
	$(".nameDecidedDate").attr('placeholder', 'yyyy-mm-dd').datepicker({ showOn: "both", dateFormat: "yy-mm-dd", defaultDate: 0, firstDay: 1 });
	
	$("#editTaxonContent").find("button, .button").button();
	
	//$(".taxonEditSection").validate();
	
	$(".taxonEditSection").find(":input").bind("change keyup", function() {
		addSaveButtonTo(this);
	});
	
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
	
	function addSaveButtonTo(e) {
		var section = $(e).closest(".taxonEditSection"); 
		if (section.find(".saveButton").length === 0) {
			section.append('<input type="submit" class="saveButton" value="Save changes" />');
			section.find(".saveButton").button().hide().fadeIn('slow');
		}
	}
	
	$(".taxonEditSection").submit(function() {
		if (!$(this).valid()) return false;
		$(this).find(".saveButton").remove();
		if ($(this).hasClass("scientificNameSection")) {
			var qname = $(this).find(".taxonQname").first().val();
			var taxonRank = $(this).find(".taxonRank").first().val();
			var scientificName = $(this).find(".scientificName").first().val();
			var author = $(this).find(".scientificNameAuthorship").first().val();
        	updateRankScientificNameAndAuthorToTree(qname, taxonRank, scientificName, author);
        	updateRankScientificNameAndAuthorToEditHeader(scientificName, author);
        } 
        else if ($(this).hasClass("primaryVernacularNameSection")) {
        	var qname = $(this).find(".taxonQname").first().val();
			var finnishName = $(this).find(".vernacularName___fi").first().val();
			updateFinnishNameToTree(qname, finnishName);
        }
		submitTaxonEditSection(this); 
		return false;
	});
	
	
	function updateRankScientificNameAndAuthorToTree(qname, taxonRank, scientificName, author) {
		var taxon = $("#"+qname.replace("MX.", "MX"));
		taxon.find(".taxonRank").first().text("["+taxonRank.replace("MX.", "")+"]");
		taxon.find(".scientificName").first().text(scientificName);
		taxon.find(".author").first().text(author);
	}
	
	function updateFinnishNameToTree(qname, finnishName) {
		var taxon = $("#"+qname.replace("MX.", "MX"));
		taxon.find(".vernacularNameFI").first().text(finnishName);
	}
	
	function updateRankScientificNameAndAuthorToEditHeader(scientificName, author) {
		var header = $("#taxonEditHeader");
		header.find(".scientificName").first().text(scientificName);
		header.find(".author").first().text(author);
	}
	
	
	$(".portlet").addClass("ui-widget ui-widget-content ui-helper-clearfix ui-corner-all")
	.find(".portlet-header").addClass("ui-widget-header ui-corner-all")
	.prepend("<span class='ui-icon ui-icon-minusthick'></span>")
	.end().find(".portlet-content");
	
	$(".portlet-header .ui-icon").click(function() {
		$(this).toggleClass("ui-icon-minusthick").toggleClass("ui-icon-plusthick");
		$(this).parents(".portlet:first").find(".portlet-content").toggle();
	});

	$(".initiallyClosed").each(function() {
		$(this).find("span")
		.toggleClass("ui-icon-minusthick")
		.toggleClass("ui-icon-plusthick");
		$(this).parents(".portlet:first").find(".portlet-content").hide();
	});
}

function submitTaxonEditSection(section) {
	var values = $(section).serialize();
	$.ajax({
        url: "${baseURL}/api/taxonEditSectionSubmit",
        type: "post",
        data: values,
        success: function(data) {
        	showSuccess(section, data);
        }
    });
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


</script>