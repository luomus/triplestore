<script>

function startsWith(needle, haystack) {
	return haystack.lastIndexOf(needle, 0) === 0;
}

var taxonTreeGraphs;
var showSynonymsModeIsOn = true;
var headerPositioned = false;

$(function() {
		
	taxonTreeGraphs = jsPlumb.getInstance();
	
	$(window).resize(function(){
		taxonTreeGraphs.repaintEverything();
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
	if (!showSynonymsModeIsOn) {
		url += '?synonymsMode=disable';
	}
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
		var compA = $(a).find('.taxonWithTools').find('.scientificName').first().text().replace('MX.', 'Ö');
        var compB = $(b).find('.taxonWithTools').find('.scientificName').first().text().replace('MX.', 'Ö');
        return (compA < compB) ? -1 : 1;
    });
    list.append(listitems);
    list.sortable("refreshPositions");
    list.sortable("refresh");
}

function saveSorting(e) {
	var list = $(e).closest('.taxonChilds').find('.childTaxonList');
	var order = new Array();
	$(list).find('.taxonWithTools').each(function() {
		order.push($(this).attr('id'));
	});
	list.sortable("destroy");
	$(e).closest('.sortingControls').hide(300);
	$(e).closest('.taxonChildTools').find('.enableSortingButton').fadeTo(300, 1);
	
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
function goToTaxon(id) {
		var url = "${baseURL}/"+id;
		if (!showSynonymsModeIsOn) {
			url += "?synonymsMode=disable";
		}
		document.location.href = url;
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

function taxonDropHandler(event, ui) {
	var droppedTaxon = ui.draggable;
	var newParentChildrenContainer = $(this).closest('.taxonChilds');
	var droppedTaxonId = droppedTaxon.attr('id');
	var newParentId = newParentChildrenContainer.attr('id').replace("Children","");
	if (!confirmChangeOfParent(droppedTaxonId, newParentId)) return;
	$.post('${baseURL}/api/changeparent?taxon='+encodeURIComponent(droppedTaxonId)+"&newParent="+encodeURIComponent(newParentId), function(data) {
		if (data == "ok") {
			reloadTaxon(droppedTaxon);
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
	
	$(".trimmedTaxonRankSelect").remove();
	$(".speciesQuickButton").remove();
	
	var taxonRankSelect = $("#allTaxonRanksSelect").clone().removeAttr("id");
	taxonRankSelect.addClass('trimmedTaxonRankSelect');
	taxonRankSelect.show();
	var showSpeciesQuickLink = false;
	var bellowGenus = false;
	if (parentRank !== 'MX.NO RANK!' && parentRank !== '') {
		var remove = true;
		taxonRankSelect.find('option').each(function() {
			if ($(this).prop('value') === '') return true;
			if ($(this).prop('value') === 'MX.genus') { showSpeciesQuickLink = true; bellowGenus = true; }
			if ($(this).prop('value') === 'MX.species') { showSpeciesQuickLink = false; }
			if ($(this).prop('value') === parentRank || $(this).prop('value') === 'MX.species') {
				$(this).remove();
				return false;
			}
    		$(this).remove();
		});
	}
	
	$("#taxonRankSelectPlaceholder").append(taxonRankSelect);
	if (bellowGenus) {
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
	
	$("#synonymOfTaxon").val(synonymOfID);
	
	var synonymOfName = $("#"+synonymOfID).find(".scientificName").first().text();
	var synonymOfRank = $("#"+synonymOfID).find(".taxonRank").first().text().replace('[', '').replace(']','');
	$("#synonymOfTaxonName").text(synonymOfName + " [" + synonymOfRank.replace('MX.','') + ']');
	$("#addNewSynonymDialog").find(".taxonRankSelect").val('MX.'+synonymOfRank);
	$(".synonymTaxonIdSelectorIdDisplay").text('');
	$("#addNewSynonymDialog").dialog("open");
}

function sendTaxon(e) {
	var taxonToSendID = $(e).closest('.taxonWithTools').attr('id');
	$("#sendTaxonDialog").find(":input").not(":input[type=submit]").val('');
	$("#newParentIdDisplay").text('');
	$("#taxonToSendID").val(taxonToSendID);
	
	var taxonToSendName = $("#"+taxonToSendID).find(".scientificName").first().text();
	$("#taxonToSendName").text(taxonToSendName);
	$("#sendTaxonDialog").dialog("open");
} 

function splitTaxon(e) {
	$("#splitTaxonDialog").find(":input").not(":input[type=submit], .rootTaxonId").val('');
	$("#splitTaxonDialog").find("select").val('MX.species');
	var taxonToSplitID = $(e).closest('.taxonWithTools').attr('id');
	var taxonToSplitName = $("#"+taxonToSplitID).find(".scientificName").first().text();
	$("#taxonToSplitID").val(taxonToSplitID);
	$("#taxonToSplitName").text(taxonToSplitName);
	$("#splitTaxonDialog").dialog("open");
} 

function mergeTaxon(e) {
	$("#mergeTaxonDialog").find(":input").not(":input[type=submit], .rootTaxonId").val('');
	var taxonToMergeID = $(e).closest('.taxonWithTools').attr('id');
	var taxonToMergeName = $("#"+taxonToMergeID).find(".scientificName").first().text();
	$("#initialTaxonToMergeId").val(taxonToMergeID);
	$("#initialTaxonToMergeName").text(taxonToMergeName);
	$(".taxonToMergeIdSelectorIdDisplay").text('');
	$("#mergeTaxonDialog").dialog("open");
}


function confirmUnlink(e, text) {
	var synonymScientificName = $(e).closest(".taxonWithTools").find(".scientificName").text();
	var synonymParentScientificName = $(e).closest(".synonyms").closest(".taxonWithTools").children('.taxonInfo').find(".scientificName").text();
	return confirm('Are you sure you want to remove ' + synonymScientificName + text + synonymParentScientificName + '?');
}

function unlinkNormalSynonym(e) {
	if (!confirmUnlink(e, ' as synonym of ')) return;
	removeSynonym(e, "SYNONYM", e.closest('.taxonWithTools').attr('id'));
}

function unlinkMisappliedSynonym(e) {
	if (!confirmUnlink(e, ' as misapplied name of ')) return;
	removeSynonym(e, "MISAPPLIED", e.closest('.taxonWithTools').attr('id'));
	
}

function unlinkUncertainSynonym(e) {
	if (!confirmUnlink(e, ' as uncertain synonym of ')) return;
	removeSynonym(e, "UNCERTAIN", e.closest('.taxonWithTools').attr('id'));
}

function unlinkIncludesConceptLink(e) {
	if (!confirmConceptUnlink(e, 'including')) return;
	removeSynonym(e, "INCLUDES", e.closest('.taxaOfConcept').attr('id'));
	
}

function unlinkIncludedinConceptLink(e) {
	if (!confirmConceptUnlink(e, 'included')) return;
	removeSynonym(e, "INCLUDED_IN", e.closest('.taxaOfConcept').attr('id'));	
}

function confirmConceptUnlink(e, text) {
	var synonymParentScientificName = $(e).closest(".synonyms").closest(".taxonWithTools").children('.taxonInfo').find(".scientificName").text();
	return confirm('Are you sure you want to unlink this '+text+' taxon concept from the concept of ' + synonymParentScientificName + '?');
}

function removeSynonym(e, synonymType, removedId) {
	/* XXX
	var synonymOfTaxon = "";  
	$.post('${baseURL}/api/changeparent?taxon='+encodeURIComponent(droppedTaxonId)+"&newParent="+encodeURIComponent(newParentId), function(data) {
		if (data == "ok") {
			reloadTaxon(droppedTaxon);
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
	*/
}

$(function() {

	$(document).on('click', '.taxonInfo', function() {
		editTaxon($(this).closest('.taxonInfo'));
	});
	
	$(document).on('click', '.taxonInfo .ui-icon', function(e) {
		e.stopPropagation();
	});
	
	$(document).on('click', '.taxonInfo .taxonToolButton', function(e) {
		$("#menu").remove();
		var menu = $('<ul id="menu"></ul>');
		menu.on('click', function() { return false; });
		var menuMove = $('<li id="menuMove">Move</li>');
		menuMove.on('click', function() {
			sendTaxon(this);
			return false;
		});
		menu.append(menuMove);
		var hasCriticalData = $(this).closest('.taxonInfo').find('.criticalData').length > 0;
		var allowsAlterationsByUser = $(this).hasClass('allowsAlterationsByUser');
		if (!hasCriticalData && allowsAlterationsByUser) {
			var menuSplit = $('<li>Split</li>');
			menuSplit.on('click', function() {
				splitTaxon(this);
				return false;
			});
			menu.append(menuSplit);
			var menuMerge = $('<li>Merge</li>');
			menuMerge.on('click', function() {
				mergeTaxon(this);
				return false;
			});
			menu.append(menuMerge);
		} else {
			menu.append('<li id="menuSplit" class="ui-state-disabled">Split</li>');
			menu.append('<li id="menuMerge" class="ui-state-disabled">Merge</li>');
		}
		menu.menu();
		$(this).after(menu);
		taxonTreeGraphs.repaintEverything();
		return false;
	});
	
	$(".taxonDialog").dialog({
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
		// TODO joko qname tai yksi uusi nimi
	});
	$("#sendTaxonDialogForm").validate({
		ignore: [], // do not ignore hidden elements
		rules: {
			newParentID: { required: true }
		},
		messages: {
			newParentID: "New parent must be selected. Type the name or part of the name and select a taxon."
		}
	});
	$("#splitTaxonDialogForm").validate({
		rules: {
			newParentID: { required: true }
		},
		messages: {
			newParentID: "New parent must be selected. Type the name or part of the name and select a taxon."
		}
	});
	$("#mergeTaxonDialogForm").validate({
		ignore: [], // do not ignore hidden elements
	});
	
	$("#splitTaxonDialog .addNewItem, #addNewSynonymDialog .addNewSynonymRow").on('click', function() {
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
	
	$("#mergeTaxonDialog .addNewItem").on('click', function() {
		var toClone = $(this).parent().find('.taxonToMergeIdSelectorContainer').last();
		var clone =  toClone.clone();
		clone.find(':input').val('');
		clone.find('.taxonToMergeIdSelector').autocomplete({ minLength: 3, source: autocompleteSourceFunction, select: taxonToMergeSelectedFunction });
		toClone.after(clone);
		clone.find('.taxonToMergeIdSelector').rules("add", {required:true});
	});
	
	$("#addNewSynonymDialog .addNewSynonymId").on('click', function() {
		var toClone = $(this).parent().find('.synonymTaxonIdSelectorContainer').last();
		var clone =  toClone.clone();
		clone.find(':input').val('');
		clone.find('.synonymTaxonIdSelector').autocomplete({ minLength: 3, source: synonymAutocompleteSourceFunction, select: synonymTaxonSelectedFunction });
		toClone.after(clone);
	});
	
	
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
	
	var taxonToMergeSelectedFunction = function (event, ui) {
		var element = $(event.target).parent();
		var selectedName = ui.item.label;
		var selectedId =  ui.item.value
		element.find('.taxonToMergeId').val(selectedId);
		element.find('.taxonToMergeIdSelector').val(selectedName);
		element.find('.taxonToMergeIdSelectorIdDisplay').text('('+selectedId+')');
		return false;
	};
	
	var synonymTaxonSelectedFunction = function (event, ui) {
		var element = $(event.target).parent();
		var selectedName = ui.item.label;
		var selectedId =  ui.item.value
		element.find('.synonymTaxonId').val(selectedId);
		element.find('.synonymTaxonIdSelector').val(selectedName);
		element.find('.synonymTaxonIdSelectorIdDisplay').text('('+selectedId+')');
		return false;
	};
	
	var synonymAutocompleteSourceFunction = function (request, response) {
		var term = request.term;
		if (term.startsWith('MX.')) return response([term]);
		var checklist = $("#synonymChecklistSelector").val();
		if (!checklist) return false;
		$.getJSON('${baseURL}/api/taxon-search/?q='+encodeURIComponent(term)+'&checklist='+checklist+'&format=jsonp&callback=?&v=2', function (data) {
			response(data.result);
		});
    };
    
	$("#newParentIDSelector").autocomplete({ minLength: 3, source: autocompleteSourceFunction, select: newParentTaxonSelectedFunction });
	$(".taxonToMergeIdSelector").autocomplete({ minLength: 3, source: autocompleteSourceFunction, select: taxonToMergeSelectedFunction });
	$(".synonymTaxonIdSelector").autocomplete({ minLength: 3, source: synonymAutocompleteSourceFunction, select: synonymTaxonSelectedFunction });
	    
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
	var form = $("#addNewSynonymDialogForm");
	if (!form.valid()) return;
	
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

function sendTaxonAsChildDialogSubmit() {
	if (!$("#sendTaxonDialogForm").valid()) return;
	
	var taxonToSendID = $('#taxonToSendID').val();
	var newParentID = $('#newParentID').val();	
	$.ajax({
		type: "POST",
		url: '${baseURL}/api/sendtaxon?taxonToSendID='+encodeURIComponent(taxonToSendID)+'&newParentID='+encodeURIComponent(newParentID),
		suppressErrors: true 
	})
	.done(function(data) {
		$('#'+taxonToSendID).remove();
		taxonTreeGraphs.repaintEverything();
		$("#sendTaxonDialog").dialog("close");
		$(".addButton").show();
  	})
  	.fail(function(xhr, status, error) {
  		$(".addButton").show();
  		alert(xhr.responseText);
  		return false;
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


</script>