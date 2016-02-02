<script>

var taxonTreeGraphs;
var showSynonymsModeIsOn = true;
$(function() {
		
	taxonTreeGraphs = jsPlumb.getInstance();
	
	$(window).resize(function(){
		taxonTreeGraphs.repaintEverything();
  	});
	
	var synonymCheckedInitially = <#if synonymsMode == "show">true<#else>false</#if>;
	showSynonymsModeIsOn = synonymCheckedInitially; 
	$("#synonymModeSelectorTool :checkbox").switchButton({
 		on_label: 'Shown',
  		off_label: 'Hidden',
  		checked: synonymCheckedInitially
	});
	
	$("#taxonDragModeSelectorTool :checkbox").switchButton({
 		on_label: 'Enabled',
  		off_label: 'Disabled',
  		checked: false
	});
		  	
});

function treePlusMinusSignClick(e) {
	if ($(e).data("state") === "expanded") {
		collapseTaxon(e);
	} else {
		expandTaxon(e);
	}
}
function collapseTaxonByCloseButton(e) {
	var parentId = $(e).closest('.taxonChilds').attr('id').replace('Children', '');
	var closeButton = $('#'+parentId).find('.treePlusMinusSign').first();
	collapseTaxon(closeButton);
}
function collapseTaxon(e) {
	collapseTree(e);
	$(e).data("state", "collapsed");
	removeEmptyTaxonlevels();
	taxonTreeGraphs.repaintEverything();
	$(e).find("span").toggleClass("ui-icon-minus").toggleClass("ui-icon-plus");
}
function expandTaxon(e) {
	expandTree(e);
	$(e).data("state", "expanded");	
	$(e).find("span").toggleClass("ui-icon-minus").toggleClass("ui-icon-plus");
}

function removeEmptyTaxonlevels() {
	$(".taxonLevel").each(function() {
		var taxonLevel = $(this);
		if (taxonLevel.children().length === 0) {
			taxonLevel.remove();
		}
	});
}

function collapseTree(e) {
	var clickedTaxon = $(e).closest('.taxonWithTools');
	var taxonQnameOfClicked = clickedTaxon.attr('id');
	collapseTreeByTaxonQname(taxonQnameOfClicked);
}
function collapseTreeByTaxonQname(taxonQname) {
	var childrenContainerId = taxonQname + 'Children';
	$("#"+childrenContainerId).find(".taxonWithTools").each(function() {
		collapseTreeByTaxonQname($(this).attr('id'));
	});
	removeTaxonConnection(childrenContainerId);
	$("#"+childrenContainerId).remove();
}

function expandTree(e) {
	var clickedTaxon = $(e).closest('.taxonWithTools');
	var taxonQnameOfClicked = clickedTaxon.attr('id');
	var taxaOfTaxonContainerOfClicked = clickedTaxon.closest('.taxonChilds');  
	var taxonLevelOfClicked = taxaOfTaxonContainerOfClicked.closest('.taxonLevel');
	if (taxonLevelOfClicked.next().length == 0) {
		var newTaxonLevel = $('<div class="taxonLevel"></div>'); 
		$("#taxonTree").append(newTaxonLevel);
	}
	
	var taxonLevelToAddChilrenOfClicked = taxonLevelOfClicked.next();
	var newTaxaOfTaxonContainer = $('<div class="taxonChilds" id="'+taxonQnameOfClicked+'Children"></div>');
	
	var indexAmongSiblings = 1;
	taxaOfTaxonContainerOfClicked.find('.taxonWithTools').each(function() {
		if ($(this).attr('id') === taxonQnameOfClicked) return false;
		indexAmongSiblings++;
	});
		
	var orderString;
	if (taxaOfTaxonContainerOfClicked.data("order") == undefined) {
		orderString = "1";
	} else {
		orderString = taxaOfTaxonContainerOfClicked.data("order") + indexAmongSiblings;
	}
	newTaxaOfTaxonContainer.data("order", orderString);
	
	if (taxonLevelToAddChilrenOfClicked.children().length == 0) {
		taxonLevelToAddChilrenOfClicked.append(newTaxaOfTaxonContainer);
	} else {
		var orderOfTaxonContainerToAdd = parseInt(orderString);
		var added = false;
		taxonLevelToAddChilrenOfClicked.find('.taxonChilds').each(function() {
			var orderOfTaxonContainerOfCompared = parseInt($(this).data("order"));
			if (orderOfTaxonContainerToAdd < orderOfTaxonContainerOfCompared) {
				$(this).before(newTaxaOfTaxonContainer);
				added = true;
				return false;
			} 
		});
		if (!added) {
			taxonLevelToAddChilrenOfClicked.append(newTaxaOfTaxonContainer);
		}
	}
	
	var taxaOfTaxonContainerToAddChildrenOfClicked = $("#"+taxonQnameOfClicked+"Children");
	taxaOfTaxonContainerToAddChildrenOfClicked.html('<@loadingSpinner/>');
	addTaxonConnection(taxaOfTaxonContainerOfClicked.attr('id'), taxonQnameOfClicked+'Children');
	taxonTreeGraphs.repaintEverything();
	var url = '${baseURL}/api/children/'+encodeURIComponent(taxonQnameOfClicked);
	if (!showSynonymsModeIsOn) {
		url += '?synonymsMode=disable';
	}
	$.get(url, function(data) {
		taxaOfTaxonContainerToAddChildrenOfClicked.html(data);
		taxaOfTaxonContainerToAddChildrenOfClicked.find('button, .button').button();
		taxonTreeGraphs.repaintEverything();
  	});
}

var connections = {};

function addTaxonConnection(parent, child) {
	try {
		connections[child] = taxonTreeGraphs.connect({
			source: parent, 
	   		target: child, 			   	
			connector:["Bezier", { curviness:20 }],
	   		endpoint:"Blank",
	   		anchors:["Bottom", "Top"], 
	   		paintStyle:{ 
				lineWidth:3,
				strokeStyle:"rgb(100, 140, 110)"
			},			   
	   		overlays : [
				["Arrow", {
					cssClass:"l1arrow",
					location:1.0, width:15,length:8
				}]
			]
		});
	} catch(e) {
		alert(e.message);
	}
}

function removeTaxonConnection(child) {
	try {
		if (connections[child] != undefined) {
			taxonTreeGraphs.detach(connections[child]);
			connections[child] = undefined;
		}
	} catch(e) {
		alert(child + ' ' + e.message);
	}
}

function confirmChangeOfRoot(e) {
	if ($(e).hasClass('ui-state-disabled')) return false;
	return confirm('Are you sure you want to close all branches and set the selected taxon as the working root?');
}

var originalSortOrder;

function enableSorting(e) {
	disableToolsExcept($(e).closest('.taxonChilds'));
	$(e).fadeTo(300, 0);
	$(e).closest('.taxonChildTools').find('.sortingControls').fadeIn(300);
	$(e).closest('.taxonChilds').find('.childTaxonList').sortable({ 
		axis: "y",
		stop: function() {
			$(e).closest('.taxonChildTools').find('.sortingControls').find('.saveSortingButton').fadeIn(300);
		} 
	}).disableSelection();
	originalSortOrder = new Array();
	$(e).closest('.taxonChilds').find('.taxonWithTools').each(function() {
		originalSortOrder.push($(this).attr('id'));
	});
}
function cancelSorting(e) {
	var list = $(e).closest('.taxonChilds').find('.childTaxonList');
	list.sortable("destroy");
	setTimeout(function() {
		restoreOrder(originalSortOrder, list);
		$(e).closest('.sortingControls').hide(300);
		$(e).closest('.sortingControls').find('.saveSortingButton').hide();
		$(e).closest('.taxonChildTools').find('.enableSortingButton').fadeTo(300, 1);
		endDisableOfTools();
	}, 200);
}
function restoreOrder(originalSortOrder, list) {
	$(originalSortOrder).each(function() {
		var taxon = $("#"+this);
		var listItem = taxon.parent('li');
		list.append(listItem);
	});
}

function saveSorting(e) {
	var list = $(e).closest('.taxonChilds').find('.childTaxonList');
	var order = new Array();
	$(list).find('.taxonWithTools').each(function() {
		order.push($(this).attr('id'));
	});
	list.sortable("destroy");
	$(e).closest('.sortingControls').hide(300);
	$(e).closest('.sortingControls').find('.saveSortingButton').hide();
	$(e).closest('.taxonChildTools').find('.enableSortingButton').fadeTo(300, 1);
	
	saveOrder(order);
	endDisableOfTools();
}

function saveOrder(order) {
	$.post('${baseURL}/api/saveorder?order='+order);
}

function disableToolsExcept(e) {
	$("#greyOutElement").show();
	$(e).addClass('aboveGreyOutElement');
	$(e).find('.taxonToolButton').find(':visible').addClass('hiddenWhenDisablingTools').hide();
}

function endDisableOfTools() {
	$('.aboveGreyOutElement').removeClass('aboveGreyOutElement');
	$("#greyOutElement").hide();
	$('.hiddenWhenDisablingTools').show();
}
function changeSynonymMode() {
	var sliderPositionOn = $("#synonymMode").prop('checked');
	if (showSynonymsModeIsOn && sliderPositionOn === false) {
		disableShowSynonymsMode();
	}
	if (!showSynonymsModeIsOn && sliderPositionOn === true) {
		changeToShowSynonymsMode();
	}
}
function disableShowSynonymsMode() {
	showSynonymsModeIsOn = false;
	$(".synonyms").fadeOut('slow', function() {taxonTreeGraphs.repaintEverything();});
}
function changeToShowSynonymsMode() {
	var url = location.href.split("?")[0] + "?synonymsMode=show";
	document.location.href = url;
}
function goUp() {
	<#if root.hasParent()>
			var url = "${baseURL}/${root.parent.qname}";
			if (!showSynonymsModeIsOn) {
				url += "?synonymsMode=disable";
			}
			document.location.href = url;
	</#if> 
}
function changeRoot(e, url) {
	if(confirmChangeOfRoot(e)) {
		if (!showSynonymsModeIsOn) {
			url += "?synonymsMode=disable";
		}
		document.location.href = url;
	}
}

function changeTaxonDragMode() {
	if ($("#taxonDragMode").prop('checked') === true) {
		$("#taxonTree").find("button, .button, .ui-button").prop("disabled", true).addClass("ui-state-disabled");
		$(".taxonChilds").not(".rootTaxonChilds").each(function() {
			var dropContainer = $('<div class="taxonDropArea ui-widget ui-widget-header"><p>Drop taxon here</p></div>');
			dropContainer.disableSelection();
			dropContainer.droppable({
				activeClass: "ui-state-hover",
				hoverClass: "ui-state-active",
				accept: validateTaxonDrop,
				drop: taxonDropHandler,
				tolerance: "pointer"
			});
			$(this).append(dropContainer);
		});
		$(".synonyms").each(function() {
			var dropContainer = $('<div class="taxonDropArea ui-widget ui-widget-header"><p>Drop taxon here as a synonym</p></div>');
			dropContainer.disableSelection();
			dropContainer.droppable({
				activeClass: "ui-state-hover",
				hoverClass: "ui-state-active",
				accept: validateTaxonSynonymDrop,
				drop: taxonSynonymDropHandler,
				tolerance: "pointer" 
			});
			$(this).append(dropContainer);
		});
		
		taxonTreeGraphs.repaintEverything();
		
		$(".taxonWithTools").not(".rootTaxon").draggable({ revert: "invalid", helper: "clone", cursor: 'move' });
	} else {
		$("#taxonTree").find("button, .button, .ui-button").prop("disabled", false).removeClass("ui-state-disabled");
		$(".taxonDropArea").remove();
		$(".ui-draggable").draggable("destroy");
		taxonTreeGraphs.repaintEverything();
	}
}

function validateTaxonDrop(e) {
	var taxonID = $(e).attr("id");
  	var newParentChildrenContainerID = $(this).closest('.taxonChilds').attr("id");
  	var originalParentChildrenContainerID = $(e).closest('.taxonChilds').attr("id");
	if (newParentChildrenContainerID == originalParentChildrenContainerID) {
		return false;
	}
	if (childContainerIsChildOfTaxon(taxonID, newParentChildrenContainerID)) {
		return false;
	}
	return true;
}
function childContainerIsChildOfTaxon(taxonID, childContainerID) {
	if (childContainerID === 'rootTaxonContainer') return false; 
	var childrenOfID = childContainerID.replace("Children","");
	if (taxonID === childrenOfID) return true;
	var childContainerParentContainer = $("#"+childrenOfID).closest('.taxonChilds');
	if (childContainerParentContainer.length === 0) return false;
	return childContainerIsChildOfTaxon(taxonID, childContainerParentContainer.attr('id')); 
}
function validateTaxonSynonymDrop(e) {
	return !e.hasClass('hasChildren');
}

function taxonDropHandler(event, ui) {
	var droppedTaxon = ui.draggable;
	var newParentChildrenContainer = $(this).closest('.taxonChilds');
	var droppedTaxonId = droppedTaxon.attr('id');
	var newParentId = newParentChildrenContainer.attr('id').replace("Children","");
	if (!confirmChangeOfParent(droppedTaxonId, newParentId)) return;
	$.post('${baseURL}/api/changeparent?taxon='+encodeURIComponent(droppedTaxonId)+"&newParent="+encodeURIComponent(newParentId), function(data) {
		if (data == "ok") {
			collapseTaxon(droppedTaxon.find(".treePlusMinusSign"));
			droppedTaxon.fadeOut(function() {
  				$('<li></li>').html(droppedTaxon).appendTo(newParentChildrenContainer.find('.childTaxonList'));
				droppedTaxon.fadeIn(function() {
					taxonTreeGraphs.repaintEverything();
				});
			});
		} else {
			var validationDialog = $('<div id="validationDialog"><h2>Validation error</h2><p class="errorMessage">'+data+'</p></div>');
			validationDialog.appendTo("body");
			validationDialog.dialog({
				modal: true, height: 'auto', width: 600, 
				close: function() { 
					$("#validationDialog").remove(); 
				}
			});
		}
	});
}
function confirmChangeOfParent(droppedTaxonId, newParentId) {
	var droppedTaxonName = $('#'+droppedTaxonId).find('.scientificName').first().text();
	var newParentName = $('#'+newParentId).find('.scientificName').first().text();
	return confirm('Please confirm: set ' + droppedTaxonName + ' to be part of ' + newParentName + '?');
}

function taxonSynonymDropHandler(event, ui) {
	var droppedTaxon = ui.draggable;
	var droppedTaxonId = droppedTaxon.attr('id');
	
	
	var newSynonymParentSynonymContainer = $(this).closest('.synonyms');
	var newSynonymParentId = newSynonymParentSynonymContainer.attr('id').replace("Synonyms","");
	
	if (!confirmSynonymDrop(droppedTaxonId, newSynonymParentId)) return;
	$.post('${baseURL}/api/setAsSynonym?taxon='+encodeURIComponent(droppedTaxonId)+"&newSynonymParent="+encodeURIComponent(newSynonymParentId), function(data) {
		if (data == "ok") {
			collapseTaxon(droppedTaxon.find(".treePlusMinusSign"));
			droppedTaxon.fadeOut(function() {
				newSynonymParentSynonymContainer.prepend(droppedTaxon);
				newSynonymParentSynonymContainer.find('.noSynonymsText').remove(); 
				droppedTaxon.find('.synonyms').remove();
				droppedTaxon.find('.showChildrenTools').remove();
				taxonTreeGraphs.repaintEverything();
				droppedTaxon.fadeIn(function() {
					taxonTreeGraphs.repaintEverything();
				});
			});
		} else {
			var validationDialog = $('<div id="validationDialog"><h2>Validation error</h2><p class="errorMessage">'+data+'</p></div>');
			validationDialog.appendTo("body");
			validationDialog.dialog({
				modal: true, height: 'auto', width: 600, 
				close: function() { 
					$("#validationDialog").remove(); 
				}
			});
		}
	});
}
function confirmSynonymDrop(droppedTaxonId, newSynonymParentId) {
	var droppedTaxonName = $('#'+droppedTaxonId).find('.scientificName').first().text();
	var newSynonymParentName = $('#'+newSynonymParentId).find('.scientificName').first().text();
	return confirm('Please confirm: set ' + droppedTaxonName + ' to be synonym of ' + newSynonymParentName + '?');
}

function addNewChild(e) {
	var parentID = $(e).closest('.taxonChilds').attr('id').replace("Children","");
	$("#addNewTaxonDialog").find(":input").not(":input[type=submit]").val('');
	$("#newTaxonParent").val(parentID);
	var parentName = $("#"+parentID).find(".scientificName").first().text();
	var parentRank = 'MX.' + $("#"+parentID).find(".taxonRank").first().text().replace('[', '').replace(']','');
	$("#newTaxonParentName").text(parentName);
	
	$(".trimmedTaxonRankSelect").remove();
	var taxonRankSelect = $("#allTaxonRanksSelect").clone().removeAttr("id");
	taxonRankSelect.addClass('trimmedTaxonRankSelect');
	taxonRankSelect.show();
	if (parentRank !== 'MX.NO RANK!' && parentRank !== '') {
		var remove = true;
		taxonRankSelect.find('option').each(function() {
			if ($(this).prop('value') === '') return true;
			if ($(this).prop('value') === parentRank || $(this).prop('value') === 'MX.species') {
				$(this).remove();
				return false;
			}
    		$(this).remove();
		});
	}
	
	$("#taxonRankSelectPlaceholder").append(taxonRankSelect);
	
	$("#addNewTaxonDialog").dialog("open");
}

function addNewSynonym(e) {
	var synonymOfID = $(e).closest('.taxonWithTools').attr('id');
	$("#addNewSynonymDialog").find(":input").not(":input[type=submit]").val('');
	$("#synonymOfTaxon").val(synonymOfID);
	var synonymOfName = $("#"+synonymOfID).find(".scientificName").first().text();
	$("#synonymOfTaxonName").text(synonymOfName);
	$("#addNewSynonymDialog").dialog("open");
}

$(function() {
	$("#addNewTaxonDialog, #addNewSynonymDialog").dialog({
 		autoOpen: false,
		modal: true,
		width: 550
	});
	$("#addNewTaxonDialogForm").validate({
		rules: {
			newTaxonScientificName: { required: { depends: function(e) { return ($('#newTaxonAuthor').val() != ""); }} }
		},
		messages: {
			newTaxonScientificName: "Scientific name must be given if author given."
		}
	});
	$("#addNewSynonymDialogForm").validate({
		rules: {
			newSynonymScientificName: { required: { depends: function(e) { return ($('#newSynonymAuthor').val() != ""); }} }
		},
		messages: {
			newSynonymScientificName: "Scientific name must be given if author given."
		}
	});
});

function addNewChildDialogSubmit() {
	if (!$("#addNewTaxonDialogForm").valid()) return;
	
	var checklist = '<#if checklist??>${checklist.qname}</#if>';
	var parent = $('#newTaxonParent').val();
	var scientificName = $('#newTaxonScientificName').val();
	var author = $('#newTaxonAuthor').val();
	var taxonRank = $('.trimmedTaxonRankSelect').first().val();
	
	var taxaOfTaxonContainerOfParent = $("#"+parent+"Children");

	$.post('${baseURL}/api/addchild?checklist='+encodeURIComponent(checklist)+'&parent='+encodeURIComponent(parent)+'&scientificName='+encodeURIComponent(scientificName)+'&author='+encodeURIComponent(author)+'&taxonRank='+encodeURIComponent(taxonRank), function(data) {
		var newTaxon = $('<li>'+data+'</li>');
		taxaOfTaxonContainerOfParent.find('.childTaxonList').append(newTaxon); 
		newTaxon.find('button, .button').button();
		taxonTreeGraphs.repaintEverything();
		$("#addNewTaxonDialog").dialog("close");
		$(".addButton").show();
		$("#validationDialog").dialog({
			modal: true, height: 600, width: 600, 
			close: function() { $("#validationDialog").remove(); }
		});
  	});
}

function addNewSynonymDialogSubmit() {
	if (!$("#addNewSynonymDialogForm").valid()) return;
	
	var synonymOf = $('#synonymOfTaxon').val();
	var scientificName = $('#newSynonymScientificName').val();
	var author = $('#newSynonymAuthor').val();
	var taxonRank = $('#newSynonymTaxonrank').val();
	
	var synonymContainer = $("#"+synonymOf+"Synonyms");
	
	$.post('${baseURL}/api/addsynonym?synonymOf='+encodeURIComponent(synonymOf)+'&scientificName='+encodeURIComponent(scientificName)+'&author='+encodeURIComponent(author)+'&taxonRank='+encodeURIComponent(taxonRank), function(data) {
		var newSynonym = $('<div/>').html(data);
		synonymContainer.prepend(newSynonym);
		synonymContainer.find('.noSynonymsText').remove(); 
		taxonTreeGraphs.repaintEverything();
		$("#addNewSynonymDialog").dialog("close");
		$(".addButton").show();
  	});
}

function changeChecklist() {
	var selectedChecklistQname = $("#checklistSelector").val();
	document.location.href = '${baseURL}/' + selectedChecklistQname;
}

function clearCaches() {
	if (confirm('Clear caches?'))
	$.post('${baseURL}/api/clear-caches', function(data) {
		alert('Caches cleared... Page will be reloaded.');
		location.reload(true);
  	});
}

$(function() {
	$(".rootTaxon").find(".treePlusMinusSign").click();
});

function searchTaxon(taxonSearchForm) {
	var taxon = $(taxonSearchForm).find('input:first').val();
	taxon = taxon.replace("%", "");
	var taxonpageBaseLinkType = $(taxonSearchForm).attr('taxonpageBaseLinkType');
	var taxonpageBaseLinkURL = '${(baseURL + "/orphan")?url}';
	if (taxonpageBaseLinkType === "taxonTree") {
		taxonpageBaseLinkURL = '${(baseURL)?url}';
	} else if (taxonpageBaseLinkType === "taxonDescriptions") {
		taxonpageBaseLinkURL = '${(baseURL + "/taxon-descriptions")?url}';
	}
	var resultViewContainer = $(taxonSearchForm).find('div:first');
	resultViewContainer.html('').removeClass('collapse');
	resultViewContainer.hide();
	$("body").css("cursor", "progress");
	$.get('${baseURL}/api/taxonomy-search-content/'+encodeURIComponent(taxon)+'?locale=en<#if checklist??>&checklist=${checklist.qname}</#if>&taxonpageBaseLinkURL='+taxonpageBaseLinkURL, function(data) {
		resultViewContainer.html(data);
		resultViewContainer.fadeIn(1000);
		setTimeout(function() {
			if (resultViewContainer.height() > 200) {
				resultViewContainer.collapse({showMoreText: '+ Show more'});
			}
	  		$("body").css("cursor", "default");
	  	}, 50);
  	});
}

$(function() {
	var cache = {}
	var autocompleteSourceFunction = function (request, response) {
		var term = request.term;
		if (term in cache) {
			response(cache[term]);
		} else {
			$.getJSON('${baseURL}/api/taxon-search/'+term+'?<#if checklist??>checklist=${checklist.qname}&</#if>format=jsonp&callback=?', function (data) {
				cache[term] = data.result;
				response(data.result);
			});
		}
    };
	var exactMatchTaxonSelectedFunction = function (event, ui) {
		var selectedName = ui.item.value; 
		$.getJSON('${baseURL}/api/taxonomy-search-exact-match-selected?selectedTaxonName='+encodeURIComponent(selectedName)+'<#if checklist??>&checklist=${checklist.qname}</#if>', function (data) {
			if (data.exactMatch) {
				var taxonpageBaseLinkType = $(".taxonomySearchForm").attr('taxonpageBaseLinkType');
				var taxonpageBaseLinkURL = '${baseURL + "/orphan"}';
				if (taxonpageBaseLinkType === "taxonTree") {
					taxonpageBaseLinkURL = '${baseURL}';
				} else if (taxonpageBaseLinkType === "taxonDescriptions") {
					taxonpageBaseLinkURL = '${baseURL + "/taxon-descriptions"}';
				}
				document.location.href = taxonpageBaseLinkURL+'/'+data.qname
			} else {
				searchTaxon($(event.target).parent());
			}
		});
	};
	$('.taxonomySearchForm').find('input:first').autocomplete({ minLength: 3, source: autocompleteSourceFunction, select: exactMatchTaxonSelectedFunction });
});

</script>