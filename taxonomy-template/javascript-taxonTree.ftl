<script>

function startsWith(needle, haystack) {
	return haystack.lastIndexOf(needle, 0) === 0;
}

var taxonTreeGraphs;
var headerPositioned = false;
    
$(function() {
	
	taxonTreeGraphs = jsPlumb.getInstance();
	
	$(window).resize(function(){
		taxonTreeGraphs.repaintEverything();
    	$("#editTaxon").dialog('option', 'height', $(window).height());
  	});
	
	$(window).on('scroll', function() {
 		var scrollTop = $(window).scrollTop();
 		if (scrollTop > 1) {
 			if (!headerPositioned) {
 				$('#toolbox').addClass('positioned');
 				headerPositioned = true;
 			}
 		} else {
 			if (headerPositioned) {
 				$('#toolbox').removeClass('positioned');
 				headerPositioned = false;
 			}
 		}
 	});
	
	$("#taxonDragModeSelectorTool :checkbox").switchButton({
 		on_label: 'Enabled',
  		off_label: 'Disabled',
  		checked: false
	});
	
	$("#taxonEditHeader, #taxonTree, #editTaxon").on('click', '.taxonId, .scinameLink', function() {
		var value = $(this).attr('title');
		if (!value) return false;
		value = value.trim();
  		var temp = $("<input>");
    	$("body").append(temp);
    	temp.val(value).select();
    	document.execCommand("copy");
    	temp.remove();
    	if (value.indexOf(' ') > -1) {
    		$.simplyToast('info', value + '<br/>copied to clipboard');
    	} else {
    		$.simplyToast('info', value + ' copied to clipboard');
    	}
    	return false;
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
		if ($(this).children().length === 0) {
			$(this).remove();
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
	addTaxonConnection(taxonQnameOfClicked, taxonQnameOfClicked+'Children');
	taxonTreeGraphs.repaintEverything();
	var url = '${baseURL}/api/children/'+encodeURIComponent(taxonQnameOfClicked);
	$.get(url, function(data) {
		taxaOfTaxonContainerToAddChildrenOfClicked.html(data);
		taxaOfTaxonContainerToAddChildrenOfClicked.find('button, .button').button();
		taxonTreeGraphs.repaintEverything();
  	});
}

var connections = [];
var connectionSeq = 1;
function addTaxonConnection(parent, child) {
	try {
		if (!connections[child]) {
			connections[child] = [];
		}
		connections[child][connectionSeq++] = taxonTreeGraphs.connect({
			source: parent, 
	   		target: child, 			   	
			connector:["Bezier", { curviness:50 }],
	   		endpoint:"Blank",
	   		anchors:["Left", [0, 0, -1, 0]], 
	   		paintStyle:{ 
				lineWidth:3,
				strokeStyle:"rgb(100, 140, 110)"
			},			   
	   		overlays : [
				["Arrow", {
					cssClass:"l1arrow",
					location:1.0, width:7,length:5
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
			$(connections[child]).each(function() {
				taxonTreeGraphs.detach(this);
			});
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
	$(e).closest('.taxonChilds').find('.childTaxonList').sortable({ axis: "y" }).disableSelection();
	originalSortOrder = new Array();
	$(e).closest('.taxonChilds').find('.taxonWithTools').each(function() {
		originalSortOrder.push($(this).attr('id'));
	});
}
function cancelSorting(e) {
	var list = $(e).closest('.taxonChilds').find('.childTaxonList');
	list.sortable("destroy");
	restoreOrder(originalSortOrder, list);
	$(e).closest('.sortingControls').hide(300);
	$(e).closest('.taxonChildTools').find('.enableSortingButton').fadeTo(300, 1);
	endDisableOfTools();
}
function restoreOrder(originalSortOrder, list) {
	$(originalSortOrder).each(function() {
		var taxon = $("#"+this);
		var listItem = taxon.parent('li');
		list.append(listItem);
	});
}

function sortAlphabetically(e) {
	var list = $(e).closest('.taxonChilds').find('.childTaxonList');
    var listitems = $('li', list);
    listitems.sort(function (a, b) {
		var compA = $(a).find('.taxonWithTools').find('.scientificName').first().text().replace('MX.', 'Ö').replace('×', '').replace(' x ', '');
        var compB = $(b).find('.taxonWithTools').find('.scientificName').first().text().replace('MX.', 'Ö').replace('×', '').replace(' x ', '');
        return (compA < compB) ? -1 : 1;
    });
    list.append(listitems);
    list.sortable("refreshPositions");
    list.sortable("refresh");
}

function saveSorting(e, uiCleanup) {
	var list = $(e).closest('.taxonChilds').find('.childTaxonList');
	var order = new Array();
	$(list).find('.taxonWithTools').each(function() {
		order.push($(this).attr('id'));
	});
	if (uiCleanup) {
		list.sortable("destroy");
		$(e).closest('.sortingControls').hide(300);
		$(e).closest('.taxonChildTools').find('.enableSortingButton').fadeTo(300, 1);
	}
	saveOrder(order);
}

function saveOrder(order) {
	endDisableOfTools();
	blockingSaver();
	var s = ''+order;
	$.ajax({type: 'POST', url: '${baseURL}/api/saveorder/', data: {order: s} }).done(function() {
		endBlockingSaver();
	});
}

function blockingSaver() {
	$("#greyOutElement").text('Saving... Please wait!').show();
}

function endBlockingSaver() {
	$("#greyOutElement").hide().text('');
}

var toolsDisabled = false;

function disableToolsExcept(e) {
	toolsDisabled = true;
	$("#greyOutElement").show();
	$(e).addClass('aboveGreyOutElement');
	$(e).find('.taxonToolButton').find(':visible').addClass('hiddenWhenDisablingTools').hide();
}

function endDisableOfTools() {
	toolsDisabled = false;
	$('.aboveGreyOutElement').removeClass('aboveGreyOutElement');
	$("#greyOutElement").hide();
	$('.hiddenWhenDisablingTools').show();
}

function goToTaxon(id) {
		var url = "${baseURL}/"+id;
		document.location.href = url;
}
function changeRoot(e, url) {
	if(confirmChangeOfRoot(e)) {
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
		taxonTreeGraphs.repaintEverything();
		$(".taxonWithTools").not(".rootTaxon, .synonym").draggable({ revert: "invalid", helper: "clone", cursor: 'move' });
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
				$.get("${baseURL}/api/singleTaxonInfo/"+droppedTaxonId, function(data) {
					droppedTaxon.replaceWith(data);
					droppedTaxon = $("#"+droppedTaxonId);
					droppedTaxon.find('button, .button').button();
					droppedTaxon.fadeIn(function() {
						taxonTreeGraphs.repaintEverything();
					});
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

function addNewChild(e) {
	var parentID = $(e).closest('.taxonChilds').attr('id').replace("Children","");
	$("#addNewTaxonDialog").find(":input").not(":input[type=submit]").val('');
	$("#newTaxonParent").val(parentID);
	var parentName = $("#"+parentID).find(".scientificName").first().text();
	var parentRank = 'MX.' + $("#"+parentID).find(".taxonRank").first().text().replace('[', '').replace(']','');
	$("#newTaxonParentName").text(parentName + " [" + parentRank.replace('MX.','') + ']');
	var insertNewTaxonBelow = $(e).closest('.taxonWithTools').attr('id');	
	if (insertNewTaxonBelow) $("#insertNewTaxonBelow").val(insertNewTaxonBelow);
	
	$(".trimmedTaxonRankSelect").remove();
	$(".speciesQuickButton").remove();
	
	var taxonRankSelect = $("#allTaxonRanksSelect").clone().removeAttr("id");
	taxonRankSelect.addClass('trimmedTaxonRankSelect');
	taxonRankSelect.show();
	var showSpeciesQuickLink = false;
	var belowGenus = false;
	if (parentRank !== 'MX.NO RANK!' && parentRank !== '') {
		var remove = true;
		taxonRankSelect.find('option').each(function() {
			if ($(this).prop('value') === '') return true;
			if ($(this).prop('value') === 'MX.genus') { showSpeciesQuickLink = true; belowGenus = true; }
			if ($(this).prop('value') === 'MX.species') { showSpeciesQuickLink = false; }
			if ($(this).prop('value') === parentRank || $(this).prop('value') === 'MX.species') {
				$(this).remove();
				return false;
			}
    		$(this).remove();
		});
	}
	
	$("#taxonRankSelectPlaceholder").append(taxonRankSelect);
	if (belowGenus) {
		$("#newTaxonScientificName").val(parentName + ' ');
	}
	if (showSpeciesQuickLink) {
		$("#taxonRankSelectPlaceholder")
			.append($('<button class="speciesQuickButton">species</button>').button()
			.on('click', function() {
				$('.trimmedTaxonRankSelect').val('MX.species');
				return false;
			}));
	}
	$("#addNewTaxonDialog").dialog("open");
	var e = $("#newTaxonScientificName"); 
	var temp = e.val();
	e.val('').val(temp).focus(); // setval trick is to get focus to last char
}

function addNewSynonym(e) {
	var synonymOfID = $(e).closest('.taxonWithTools').attr('id');
	$("#addNewSynonymDialog").find(":input").not(":input[type=submit], #synonymType").val('');
	$("#addNewSynonymDialog").find("tbody").find("tr").not(":first").remove();
	
	$("#synonymOfTaxon").val(synonymOfID);
	
	var synonymOfName = $("#"+synonymOfID).find(".scientificName").first().text();
	var synonymOfRank = $("#"+synonymOfID).find(".taxonRank").first().text().replace('[', '').replace(']','');
	$("#synonymOfTaxonName").text(synonymOfName + " [" + synonymOfRank.replace('MX.','') + ']');
	$("#addNewSynonymDialog").find(".taxonRankSelect").val('MX.'+synonymOfRank);
	$(".synonymTaxonIdSelectorIdDisplay").text('');
	$("#addNewSynonymDialog").dialog("open");
}

function hideTaxon(e) {
	var taxonContainer = $(e).closest('.taxonWithTools');
	var taxonId = taxonContainer.attr('id');
	var uri = '${baseURL}/api/hideTaxon/'+taxonId;
	$("#menu").remove();
	$.post(uri, function(data) {
		if (data == "ok") {
			var hideIcon = $('<span class="hiddenTaxon"></span>');
			taxonContainer.find('.taxonInfo').first().find('.taxonId').first().after(hideIcon);
		}
	});
}

function unhideTaxon(e) {
	var taxonContainer = $(e).closest('.taxonWithTools');
	var taxonId = taxonContainer.attr('id');
	var uri = '${baseURL}/api/unhideTaxon/'+taxonId;
	$("#menu").remove();
	$.post(uri, function(data) {
		if (data == "ok") {
			taxonContainer.find('.taxonInfo').first().find('.hiddenTaxon').remove();
		}
	});
}

function sendTaxon(e) {
	var taxonContainer = $(e).closest('.taxonWithTools');
	var taxonId = taxonContainer.attr('id');
	$("#sendTaxonDialog").remove();
	$.get("${baseURL}/api/sendTaxonDialog/"+taxonId, function(data) {
		var dialog = $(data);
		dialog.appendTo("body");
		dialog.find('button, .button').button();
		$("#sendTaxonManageCriticalButton").on('click', function() {
			$("#sendTaxonDialog").remove();
			openCriticalDataDialog(taxonContainer);
		});
		$("#sendTaxonDialogForm").validate({
			ignore: [], // do not ignore hidden elements
			rules: {
				newParentID: { required: true },
				sendAsType: { required: true }
			},
			messages: {
				newParentID: "New parent must be selected. Type the name or part of the name and select a taxon.",
				sendAsType: "Select a valid type. If no options are available, manage critical data."
			},
    		errorLabelContainer: '.errorTxt'
		});
		$("#newParentIDSelector").autocomplete({ 
			minLength: 3, 
			source: autocompleteSourceFunction, 
			select: newParentTaxonSelectedFunction,
			appendTo: "#sendTaxonDialog"
		});
		dialog.dialog({
			modal: true, height: '500', width: 800, position: { my: "center", at: "top+30%" },
			close: function() { 
				$("#sendTaxonDialog").remove(); 
			}
		});	
	});
}

var cache = {}
var autocompleteSourceFunction = function (request, response) {
	var term = request.term;
	if (term in cache) {
		response(cache[term]);
	} else {
		$.getJSON('${baseURL}/api/taxon-search/?q='+encodeURIComponent(term)+'&checklist=${(checklist.qname)!"null"}&format=jsonp&callback=?&v=2', function (data) {
			cache[term] = data.result;
			response(data.result);
		});
	}
};

var newParentTaxonSelectedFunction = function (event, ui) {
	var selectedName = ui.item.label;
	var selectedId =  ui.item.value
	$("#newParentID").val(selectedId);
	$("#newParentIDSelector").val(selectedName);
	$("#newParentIdDisplay").text('('+selectedId+')');
	return false;
};

var newTargetTaxonSelectedFunction = function (event, ui) {
	var selectedName = ui.item.label;
	var selectedId =  ui.item.value
	$("#newTargetID").val(selectedId);
	$("#newTargetIDSelector").val(selectedName);
	$("#newTargetIdDisplay").text('('+selectedId+')');
	return false;
};   

	
function detachTaxon(e) {
	var e = $(e).closest('.taxonWithTools');
	var scientificName = $(e).find(".scientificName").first().text();
	var confirmText = 'Are you sure you want to detach ' + scientificName + ' and make it an orphan taxon?';
	if (confirm(confirmText)) {
		detachTaxonId($(e).attr('id'));
	}
}

function detachTaxonId(detachId) {
	var uri = '${baseURL}/api/detachTaxon/'+detachId;
	var taxonContainer = $("#"+detachId); 
	$.post(uri, function(data) {
		if (data == "ok") {
			if (taxonContainer.find(".treePlusMinusSign").length) {
				collapseTaxon(taxonContainer.find(".treePlusMinusSign"));
			}
			taxonContainer.fadeOut(function() {
				taxonTreeGraphs.repaintEverything();
				taxonContainer.remove();
			});
		}
	});
}

function deleteTaxon(e) {
	var e = $(e).closest('.taxonWithTools');
	var scientificName = $(e).find(".scientificName").first().text();
	var confirmText = 'Are you sure you want to permanently delete ' + scientificName + '?';
	if (confirm(confirmText)) {
		deleteTaxonId($(e).attr('id'));
	}
}

function deleteTaxonId(removedId) {
	var uri = '${baseURL}/api/deleteTaxon/'+removedId;
	var taxonContainer = $("#"+removedId); 
	$.post(uri, function(data) {
		if (data == "ok") {
			if (taxonContainer.find(".treePlusMinusSign").length) {
				collapseTaxon(taxonContainer.find(".treePlusMinusSign"));
			}
			taxonContainer.fadeOut(function() {
				taxonTreeGraphs.repaintEverything();
				taxonContainer.remove();
			});
		}
	});
}

function unlinkSynonym(e) {
	var e = $(e).closest('.taxonWithTools');
	var type = getSynonymType(e);
	var confirmText = getDetachSynonymConfirmText(type);
	if (confirmUnlink(e, confirmText)) {
		removeSynonym(e, type, $(e).attr('id'));
	}
}

function confirmUnlink(e, text) {
	var synonymScientificName = $(e).find(".scientificName").first().text();
	var synonymParentScientificName = getSynonymParentScientificName(e);
	return confirm('Are you sure you want to detach ' + synonymScientificName + text + synonymParentScientificName + ' and make it an orphan taxon?');
}

function getSynonymParentScientificName(e) {
	return $(e).closest(".synonyms").closest(".taxonWithTools").children('.taxonInfo').find(".scientificName").first().text();
}

function removeSynonym(e, synonymType, removedId) {
	var synonymParent = $(e).closest(".synonyms").closest(".taxonWithTools");
	var synonymParentId = synonymParent.attr("id");
	var uri = '${baseURL}/api/removeSynonym?synonymType='+synonymType+'&removedId='+removedId+'&synonymOfTaxon='+synonymParentId;  
	$.post(uri, function(data) {
		if (data == "ok") {
			collapseTaxon(synonymParent.find(".treePlusMinusSign"));
			synonymParent.fadeOut(function() {
				$.get("${baseURL}/api/singleTaxonInfo/"+synonymParentId, function(data) {
					synonymParent.replaceWith(data);
					synonymParent = $("#"+synonymParentId);
					synonymParent.find('button, .button').button();
					synonymParent.fadeIn(function() {
						taxonTreeGraphs.repaintEverything();
					});
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

function openCriticalDataDialog(e) {
	$("#criticalDataDialog").remove();
	var taxonContainer = $(e).closest('.taxonWithTools');
	var taxonId = taxonContainer.attr('id');
	$.get("${baseURL}/api/criticalDataDialog/"+taxonId, function(data) {
		var dialog = $(data);
		dialog.appendTo("body");
		dialog.find('button, .button').button();
		$("#moveEvaluationButton").on('click', function() {
			$("#criticalDataDialog").remove();
			openMoveEvaluationDialog(taxonContainer);
		});
		dialog.dialog({
			modal: true, height: 'auto', width: 600, position: { my: "center", at: "top+30%" },
			close: function() { 
				$("#criticalDataDialog").remove(); 
			}
		});	
	});
}

function openMoveEvaluationDialog(e) {
	$("#moveEvaluationDialog").remove();
	var taxonId = $(e).closest('.taxonWithTools').attr('id');
	$.get("${baseURL}/api/moveEvaluationDialog/"+taxonId, function(data) {
		var dialog = $(data);
		dialog.appendTo("body");
		dialog.find('button, .button').button();
		$("#moveEvaluationDialogForm").validate({
			ignore: [], // do not ignore hidden elements
			rules: {
				newTargetID: { required: true },
				evaluationYears: { required: true }
			},
			messages: {
				newTargetID: "New target must be selected. Type the name or part of the name and select a taxon.",
				evaluationYears: "Select at least one year with an evaluation."
			},
    		errorLabelContainer: '.errorTxt'
		});
		$("#newTargetIDSelector").autocomplete({ 
			minLength: 3, 
			source: autocompleteSourceFunction, 
			select: newTargetTaxonSelectedFunction,
			appendTo: "#moveEvaluationDialog"
		});
		dialog.dialog({
			modal: true, height: 'auto', width: 600, position: { my: "center", at: "top+30%" },
			close: function() { 
				$("#moveEvaluationDialog").remove(); 
			}
		});	
	});
}

$(function() {

	$(document).on('click', '.taxonInfo', function() {
		editTaxon($(this).closest('.taxonInfo'));
	});
	
	$(document).on('click', '.taxonInfo .ui-icon', function(e) {
		e.stopPropagation();
	});
	
	$(document).on('click', '.icons', function() {
		return false;
	});
	
	$(document).on('click', '.taxonToolMenu', function(e) {
		$("#menu").remove();
		
		var taxonId = $(e.target).closest(".taxonWithTools").attr("id");
		var container = $(this);
		
		$.get("${baseURL}/api/taxonToolsMenu/"+taxonId, function(data) {
			var menu = $(data);
			container.after(menu);
			menu.on('click', function() { return false; });
			menu.on('mouseleave', function() { 
				$("#menu").remove(); 
			});
			
			$("#taxonToolMenuEditFull").click(function() {
				editTaxon($(container).closest('.taxonInfo'), true);
				return false;
			});
			
			$("#taxonToolMenuMove").click(function() {
				sendTaxon(this);
				return false;
			});
			
			$("#taxonToolMenuAddChildBelow").click(function() {
				addNewChild(this);
				return false;
			});
			
			$("#taxonToolMenuHide").click(function() {
				hideTaxon(this);
				return false;
			});
			
			$("#taxonToolMenuUnhide").click(function() {
				unhideTaxon(this);
				return false;
			});
			
			$("#taxonToolMenuDetachSynonym").click(function() {
				unlinkSynonym(container);
				return false;
			});
			
			$("#taxonToolMenuDetach").click(function() {
				detachTaxon(container);
				return false;
			});
			
			$("#taxonToolMenuDelete").click(function() {
				deleteTaxon(container);
				return false;
			});
		
			$("#taxonToolMenuCritical").click(function() {
				openCriticalDataDialog(this);
				return false;
			});
		
			taxonTreeGraphs.repaintEverything();
		});
		return false;
	});
	
	$(".taxonDialog").dialog({
 		autoOpen: false,
		modal: true,
		width: 880,
		position: { my: "center", at: "top+50%" }
	});
        
	$("#editTaxon").dialog({
 		autoOpen: false,
		resizable: false,
		left: 0,
		top: 0,
		height: $(document).height(),
      	width: "100%",
      	modal: true,
      	open: function() {
      		$('body').css('overflow','hidden');
      	},
      	beforeClose: function() {
      		$('body').css('overflow', '');
      		if ($(".saveButton").length > 0) {
				if (confirm('This taxon has unsaved changes. Are you sure you want to close without saving?')) {
					$(".saveButton").remove();
					return true;
				}
				return false;
			}
			return true;
      	}
	});
		
	$("#addNewTaxonDialogForm").validate({
		rules: {
			newTaxonScientificName: { required: { depends: function(e) { return ($('#newTaxonAuthor').val() != ""); } } }
		},
		messages: {
			newTaxonScientificName: "Scientific name must be given if author given."
		}
	});
	$("#addNewSynonymDialogForm").validate({
		rules: {
			synonymType: "required"
		},
		messages: {
			synonymType: "You must select type of the new synonym."
		}
	});
	
	$('.addNewSynonymScientificName').each(function() {
    		$(this).rules('add', {
        		required: true,
		        messages: {
            		required:  "Give scientific name of the synonym to add"
        		}
    		});
	});

	function atLeastOneSynonymNameHasValue() {
		console.log('here');
		var found = false;
		$('.addNewSynonymScientificName').each(function() {
			if ($(this).val() != '') found = true;
		});
		console.log('res ' + found);
		return found;
	}
	
	$("#addNewSynonymDialog .addNewSynonymRow").on('click', function() {
		var tableBody = $(this).parent().find('table tbody');
		var e = tableBody.find('tr').last();
		var prevRank = e.find('select').first().val(); 
		var clone =  e.clone();
		clone.find(':input').each(function() {
			var name = $(this).attr("name");
			var field = name.split("___")[0];
			var index =  parseInt(name.split("___")[1]) + 1;
			$(this).attr('name', field + "___" + index);
			$(this).removeAttr('required');
		});
		clone.find('select').val(prevRank);
		tableBody.append(clone);
	});
   
});

function moveEvaluationDialogSubmit(action) {
	var form = $("#moveEvaluationDialogForm");
	if (action == 'delete') {
		$("#newTargetID").val('foo'); // make validation pass
	}
	if (!form.valid()) return false;
	$.ajax({ 
		type: "POST", 
		url: '${baseURL}/api/moveEvaluation?action='+action,
      	data: form.serialize(),
      	success: function(data) {
			$("#moveEvaluationDialog").dialog("close");
      		if (data == "ok") {
      			if (action == 'move')
					$.simplyToast('info', 'Evaluations moved');
				else 
					$.simplyToast('info', 'Evaluations deleted');
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
      	}
    });
}

function addNewChildDialogSubmit() {
	if (!$("#addNewTaxonDialogForm").valid()) return false;
	var parent = $('#newTaxonParent').val();
	var taxaOfTaxonContainerOfParent = $("#"+parent+"Children");

	$.ajax({
		url: '${baseURL}/api/addchild',
		type: 'POST',
		data: {
			checklist:      '<#if checklist??>${checklist.qname}</#if>',
			parent:         parent,
			scientificName: $('#newTaxonScientificName').val(),
			author:         $('#newTaxonAuthor').val(),
			taxonRank:      $('.trimmedTaxonRankSelect').first().val(),
			nameFi:         $('#newTaxonNameFi').val(),
			nameSv:         $('#newTaxonNameSv').val(),
			nameEn:         $('#newTaxonNameEn').val(),
			finnish:        $('#addNewTaxonDialogForm .finnish').val(),
			occurrenceInFinland:        $('#addNewTaxonDialogForm .occurrenceInFinland').val(),
			typeOfoccurrenceInFinland:  $('#addNewTaxonDialogForm .typeOfOccurrenceInFinland').val()
		}
	}).done(function(data) {
		var newTaxon = $('<li>'+data+'</li>');
		var insertNewTaxonBelow = $("#insertNewTaxonBelow").val();
		if (insertNewTaxonBelow) {
			$("#"+insertNewTaxonBelow).closest('li').after(newTaxon);
			saveSorting(newTaxon, false);
		} else {
			taxaOfTaxonContainerOfParent.find('.childTaxonList').append(newTaxon);
		}
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
	var form = $("#addNewSynonymDialogForm");
	if (!form.valid()) return false;
	
	var synonymParentId = $('#synonymOfTaxon').val();
	var synonymParent = $("#"+synonymParentId);
	
	$.ajax({ 
		type: "POST", 
		url: '${baseURL}/api/addSynonym',
      	data: form.serialize(),
      	success: function(data) {
			$("#addNewSynonymDialog").dialog("close");
			$(".addButton").show();
      		if (data == "ok") {
				collapseTaxon(synonymParent.find(".treePlusMinusSign"));
				synonymParent.fadeOut(function() {
					$.get("${baseURL}/api/singleTaxonInfo/"+synonymParentId, function(data) {
						synonymParent.replaceWith(data);
						synonymParent = $("#"+synonymParentId);
						synonymParent.find('button, .button').button();
						synonymParent.fadeIn(function() {
							taxonTreeGraphs.repaintEverything();
						});
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
      	}
    });  	
}

function sendTaxonDialogSubmit() {
	var selectorVal = $("#newParentIDSelector").val().toUpperCase().trim();
	if (startsWith('MX.', selectorVal)) {
		$("#newParentID").val(selectorVal);
	}
	
	var form = $("#sendTaxonDialogForm");
	if (!form.valid()) return false;
	
	var taxonToSendID = $('#taxonToSendID').val();
	var newParentID = $('#newParentID').val();
	var taxonToSendContainer = $('#'+taxonToSendID.replace("MX.", "MX")); 
	var newParentContainer = $('#'+newParentID.replace("MX.", "MX"));
	
	$.ajax({
		type: "POST",
		url: '${baseURL}/api/sendTaxon',
      	data: form.serialize()
	})
	.success(function(data) {
		if (taxonToSendContainer.find(".treePlusMinusSign").length) {
			collapseTaxon(taxonToSendContainer.find(".treePlusMinusSign"));
		}
		taxonToSendContainer.fadeOut(function() {
			taxonToSendContainer.remove();
			taxonTreeGraphs.repaintEverything();
			
			if (newParentContainer.length) {
				if (newParentContainer.find(".treePlusMinusSign").length) {
					collapseTaxon(newParentContainer.find(".treePlusMinusSign"));
				}
				newParentContainer.fadeOut(function() {
					$.get("${baseURL}/api/singleTaxonInfo/"+newParentID, function(data) {
						newParentContainer.replaceWith(data);
						newParentContainer = $('#'+newParentID.replace("MX.", "MX"));
						newParentContainer.find('button, .button').button();
						newParentContainer.fadeIn(function() {
							taxonTreeGraphs.repaintEverything();
						});
					});
				});
			}
		});
		$("#sendTaxonDialog").dialog("close");
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

function getSynonymType(e) {
	if (e.hasClass('BASIONYM')) {
		return 'BASIONYM';
	} else if (e.hasClass('OBJECTIVE')) {
		return 'OBJECTIVE';
	} else if (e.hasClass('SUBJECTIVE')) {
		return 'SUBJECTIVE';
	} else if (e.hasClass('HOMOTYPIC')) {
		return 'HOMOTYPIC';
	} else if (e.hasClass('HETEROTYPIC')) {
		return 'HETEROTYPIC';
	} else if (e.hasClass('SYNONYM')) {
		return 'SYNONYM';
	} else if (e.hasClass('MISSPELLED')) {
		return 'MISSPELLED';
	} else if (e.hasClass('ORTHOGRAPHIC')) {
		return 'ORTHOGRAPHIC';
	} else if (e.hasClass('UNCERTAIN')) {
		return 'UNCERTAIN';
	} else if (e.hasClass('MISAPPLIED')) {
		return 'MISAPPLIED';
	} else if (e.hasClass('ALTERNATIVE')) {
		return 'ALTERNATIVE';
	}
	return 'UNKNOWN';
}

function getDetachSynonymConfirmText(type) {
	return ' as ' + getSynonymLabel(type) + ' of '; 
}

function getSynonymLabel(type) {
	return SYNONYM_LABELS[type]; 
}

var SYNONYM_LABELS = [];
SYNONYM_LABELS['BASIONYM'] = 'basionym';
SYNONYM_LABELS['OBJECTIVE'] = 'objective synonym';
SYNONYM_LABELS['SUBJECTIVE'] = 'subjective synonym';
SYNONYM_LABELS['HOMOTYPIC'] = 'homotypic synonym';
SYNONYM_LABELS['HETEROTYPIC'] = 'heterotypic synonym';
SYNONYM_LABELS['SYNONYM'] = 'synonym';
SYNONYM_LABELS['ALTERNATIVE'] = 'alternative name';
SYNONYM_LABELS['MISSPELLED'] = 'misspelled name';
SYNONYM_LABELS['ORTHOGRAPHIC'] = 'orthographic variant';
SYNONYM_LABELS['UNCERTAIN'] = 'uncertain synonym';
SYNONYM_LABELS['MISAPPLIED'] = 'misapplied name';

</script>