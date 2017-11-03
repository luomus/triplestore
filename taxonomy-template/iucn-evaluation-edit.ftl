<#include "luomus-header.ftl">
<#include "iucn-macro.ftl">

<#list target.groups as groupQname>
	<a href="${baseURL}/iucn/group/${groupQname}/${selectedYear}" class="goBack">
		Takaisin (${taxonGroups[groupQname].name.forLocale("fi")!""})
	</a><br />
</#list>

<h1>Uhanalaisuusarviointi - ${selectedYear} <#if draftYear == selectedYear>(LUONNOS)</#if></h1>

<h2><a href="#"><@printScientificNameAndAuthor taxon /> ${taxon.vernacularName.forLocale("fi")!""}</a></h2>

<@toolbox/>		

<#if successMessage?has_content>
	<p class="successMessage">${successMessage?html}</p>
</#if>
<#if errorMessage?has_content>
	<div class="errorMessage">
		<h4>Tietoja ei tallennettu</h4>
		${errorMessage}
	</div>
	<script>
		var erroreousFields = [];
		<#list erroreousFields as erroreousField>
			erroreousFields.push('${erroreousField}');
		</#list>
	</script>
</#if>

<#macro tree taxon>
	<#if taxon.hasParent()>
		<@tree taxon.parent />
	</#if>
	<li><@printScientificNameAndAuthor taxon /> <span class="vernacularName">${taxon.vernacularName.forLocale("fi")!""}</span></li>
</#macro>

<div id="evaluationMeta">

<div class="taxonInfo">
	<h6>Taksonomia</h6>
	<ul class="taxonTree">
		<@tree taxon/>
	</ul>
</div>

<div class="taxonInfo">
	<h6>Synonyymit</h6>
	<#if taxon.synonyms?has_content>
	<ul>
		<#list taxon.synonyms as synonym>
			<li><@printScientificNameAndAuthor synonym /></li>
		</#list>
	</ul>
	<#else>
		<span class="info">Ei synonyymejä</span>
	</#if>
</div>	

<div class="taxonInfo">
	<h6>Kansankieliset nimet</h6>
	<#list taxon.vernacularName.allTexts?values as name>
		${name}
	</#list>
	<#list taxon.alternativeVernacularNames.allValues as name>
		${name}
	</#list>
</div>

<div class="taxonInfo">
	<h6>Huomioita taksonomiasta</h6>
	<#if taxon.notes?has_content || taxon.privateNotes?has_content>
		<#if taxon.notes?has_content>
			<p class="info">${taxon.notes?html}</p>
		</#if>
		<#if taxon.privateNotes?has_content>
			<p class="info">${taxon.privateNotes?html}</p>
		</#if>
	<#else>
		<span class="info">Ei huomioita</span>
	</#if>
</div>

<div class="taxonInfo">
	<h6>Eliöryhmä</h6>
	<ul>
		<#list target.groups as groupQname>
			<li>
				<a href="${baseURL}/iucn/group/${groupQname}/${selectedYear}">
					${taxonGroups[groupQname].name.forLocale("fi")!""}
				</a>
				<p class="info">Uhanalaisuusarvioijat: <@editors groupQname /></p>
			</li>
		</#list>
	</ul>
</div>

<div class="taxonInfo">
	<h6>Hallinnollinen asema</h6>
	<#if taxon.administrativeStatuses?has_content>
		<ul>
			<#list taxon.administrativeStatuses as adminStatus>
				<li>${properties.getProperty("MX.hasAdminStatus").range.getValueFor(adminStatus).label.forLocale("fi")}</li>				
			</#list>
		</ul>
	<#else>
		<span class="info">Ei hallinnollista asemaa</span>
	</#if>
</div>

<div class="clear"></div>


<#if editHistory?has_content>
	<div class="taxonInfo">
		<h6>Tallennushistoria</h6>
		<table class="iucnSpeciesTable">
			<tr>
				<th>Kommentti</th>
				<th>Pvm</th>
				<th>Muokkaaja</th>
			</tr>
			<#list editHistory.entries as entry>
				<tr>
					<td>${(entry.notes?html)!""}</td>
					<td>${entry.date!""}</td>
					<td>
						<#if entry.editorQname??>
							${persons[entry.editorQname].fullname?html}
						</#if>
					</td>
				</tr>
			</#list>
		</table>
	</div>
</#if>

<div class="taxonInfo">
	<h6>Arviointihistoria</h6>
	<table class="iucnSpeciesTable">
		<tr>
			<th>Vuosi</th>
			<th>Luokka</th>
			<th>Indeksi</th>
		</tr>
		<#list evaluationYears as year>
			<#if target.hasEvaluation(year)>
				<#assign yearEval = target.getEvaluation(year)> 
				<tr>
					<td><a href="${baseURL}/iucn/species/${target.qname}/${year}">${year}</a></td>
					<td>
						<#if yearEval.hasIucnStatus()>
							${statusProperty.range.getValueFor(yearEval.iucnStatus).label.forLocale("fi")} ${yearEval.externalImpact}
		    			<#else>
		    				-
		    			</#if>
		    		</td>
					<td>
						<#if yearEval.hasIucnStatus()>
							<#if yearEval.hasCorrectedStatusForRedListIndex()>
								${yearEval.calculatedCorrectedRedListIndex!""} (${yearEval.correctedStatusForRedListIndex?replace("MX.iucn", "")}) <span class="correctedIndex">[KORJATTU]</span>
							<#else>
								${yearEval.calculatedRedListIndex!"-"} (${yearEval.iucnStatus?replace("MX.iucn", "")})
							</#if>
						<#else>
							-
						</#if>
					</td>
				</tr>
			<#else>
				<tr>
					<td>${year}</td>
					<td>-</td>
					<td>-</td>
				</tr>
			</#if>
		</#list>
	</table>
</div>


</div>
<div class="clear"></div>

<hr />

<#if evaluation?? && evaluation.locked>

	<div class="warningMessage">Tämä arviointi vuodelle ${selectedYear} on <b>lukittu</b>.</div>
	
	<hr />
	
</#if>

<#if evaluation?? && draftYear != selectedYear && redListIndexPermissions && evaluation.iucnStatus??>
<form id="redListIndexEditForm" action="${baseURL}/iucn/redListIndexCorrection" method="post">
<input type="hidden" name="evaluationId" value="${(evaluation.id)}" />
	<div class="widgetTools ui-widget ui-corner-all">
		<div class="ui-widget-header">Punaisen kirjan indeksin korjaaminen</div>
		<div class="ui-widget-content">
			<p>
				<label>Varsinainen luokka</label>
				${statusProperty.range.getValueFor(evaluation.iucnStatus).label.forLocale("fi")}
			</p>
			<p>
				<label>Valitse korjattu luokka</label>
				<select name="MKV.redListIndexCorrection"  data-placeholder="...">
					<option value="" label=".."></option>
					<#list statusProperty.range.values as enumValue>
						<option value="${enumValue.qname}" <#if (evaluation.correctedStatusForRedListIndex!"") = enumValue.qname>selected="selected"</#if>>${enumValue.label.forLocale("fi")?html}</option>	
					</#list>
				</select>
			</p>
			<p>
				<label>Muistiinpanot</label>
				<textarea name="MKV.redListIndexCorrectionNotes">${(evaluation.getValue("MKV.redListIndexCorrectionNotes")!"")?html}</textarea>
			</p>
			<p>
				<label>&nbsp;</label>
				<input type="submit" value="Päivitä" />
			</p>
		</div>
	</div>
	<hr />
</form>
</#if>

<#if evaluation?? && evaluation.hasRemarks()>
	<h5>Kommentit</h5>
	<ul>
		<#list evaluation.remarkSatements as remark>
			<li class="remark"><pre>${remark.objectLiteral.content?html}</pre> <#if permissions><button class="delete ui-state-error" onclick="deleteRemark(${remark.id});">X</button></#if></li>
		</#list>
	</ul>
</#if>

<#if draftYear == selectedYear && evaluation?? && evaluation.id??>
<form id="remarksEditForm" action="${baseURL}/iucn/remarks" method="post">
<input type="hidden" name="evaluationId" value="${(evaluation.id)}" />
	<div class="widgetTools ui-widget ui-corner-all">
		<div class="ui-widget-header">Kommentit</div>
		<div class="ui-widget-content">
			<p class="info">Tässä voi kommentoida arviointia. Arvioijat voivat halutessaan poistaa kommentit jotka on jo otettu huomioon arvioinnissa.</p>
			<p><textarea id="remarksField" name="MKV.remarks"></textarea></p>
			<p><input type="submit" value="Tallenna kommentti" /></p>
		</div>
	</div>
	<hr />
</form>
</#if>

<#if permissions>

<#if comparison?? && !evaluation??>
	<div class="widgetTools ui-widget ui-corner-all">
		<div class="ui-widget-header">Kopiointi</div>
		<div class="ui-widget-content">
			<p>Voit kopioida tiettyjen määriteltyjen kenttien tiedot edellisestä arvioinnista tähän arviointiin:</p>
			<button id="copyButton">
				Kopioi vuoden ${comparison.evaluationYear} arvioinnin tiedot
			</button>
			<span class="info">Huom: Tämän voi tehdä ainoastaan kerran ennen kuin mitään tietoja on syötetty. Alle tehdyt muutokset menetetään!</span>			
		</div>
	</div>
</#if>


<form id="evaluationEditForm" action="${baseURL}/iucn/species/${taxon.qname}/${selectedYear}" method="post" onsubmit="return false;">
<input type="hidden" name="evaluationId" value="${(evaluation.id)!""}" />
<input type="hidden" name="MKV.evaluatedTaxon" value="${taxon.qname}" />
<input type="hidden" name="MKV.evaluationYear" value="${selectedYear}" />
<input type="hidden" name="MKV.state" id="evaluationState" />

</#if>

<table class="evaluationEdit">
	<thead>
		<tr>
			<th>Muuttuja</th>
			<th>
				<#if comparison??>
					${comparison.evaluationYear} tiedot
				<#else>
					Ei edellisiä tietoja
				</#if>
			</th>
			<th>${selectedYear} tiedot</th>
		</tr>
	</thead>
	<tbody>
	
	<@iucnSection "Arvioinnissa käytetty taksonominen tulkinta" />
	<@iucnTextarea "MKV.taxonomicNotes" />
	
	<@iucnSection "Esiintymistä koskevat tiedot" />
	<@typeOfOccurrenceInFinland />
	<@iucnMinMax "Levinneisyysalueen koko" "MKV.distributionAreaMin" "MKV.distributionAreaMax" "MKV.distributionAreaNotes" />
    <@iucnMinMax "Esiintymisalueen koko" "MKV.occurrenceAreaMin" "MKV.occurrenceAreaMax" "MKV.occurrenceAreaNotes" />
	<@iucnTextarea "MKV.occurrenceNotes" />
	
	<@iucnSection "Esiintymisalueet ja alueellinen uhanalaisuus <span> &mdash; Täytettävä luokille NT-CR&nbsp;&nbsp; Alueellinen uhanalaisuus vain luokille LC ja NT</span> &nbsp;&nbsp; <a href=\"${staticURL}/img/Aluellisen arvioinnin tarkastelualueet.jpg\" target=\"_staticmap\">&raquo; Alueet</a> " />
	<#list areas?keys as areaQname>
		<@iucnOccurrence areaQname />
	</#list>
	<@iucnTextarea "MKV.occurrenceRegionsNotes" "" "MKV.occurrenceRegionsPrivateNotes" />
	<@iucnTextarea "MKV.regionallyThreatenedNotes" "regionalThreatened" "MKV.regionallyThreatenedPrivateNotes" />

	<@iucnSection "Elinympäristö <span> &mdash; Ensisijainen on täytettävä luokille LC-CR</span> &nbsp;&nbsp; <a href=\"${staticURL}/Elinympäristöluokkien_tulkintaohje_v1_1.pdf\" target=\"_staticmap\">&raquo; Elinympäristöluokkien tulkintaohje</a> " /> 
	
	<@iucnHabitatFields />   
	<@iucnTextarea "MKV.habitatGeneralNotes" />
	
	<@iucnSection "Arvioinnin perusteet" />
	<@iucnInput "MKV.generationAge" "MKV.generationAgeNotes" />
	<@iucnInput "MKV.evaluationPeriodLength" "MKV.evaluationPeriodLengthNotes" />
	<@iucnMinMax "Yksilömäärä" "MKV.individualCountMin" "MKV.individualCountMax" "MKV.individualCountNotes" />
	<@iucnInput "MKV.populationSizePeriodBeginning" "MKV.populationSizePeriodNotes" />
	<@iucnInput "MKV.populationSizePeriodEnd" />
	<@iucnInput "MKV.decreaseDuringPeriod" "MKV.decreaseDuringPeriodNotes" />
	<@iucnInput "MKV.populationVaries" "MKV.populationVariesNotes" />
	<@iucnInput "MKV.fragmentedHabitats" "MKV.fragmentedHabitatsNotes" />
	<@iucnInput "MKV.borderGain" "MKV.borderGainNotes" />
	<@iucnEndangermentObject "MKV.hasEndangermentReason" "MKV.endangermentReasonNotes" />
	<@iucnEndangermentObject "MKV.hasThreat" "MKV.threatNotes" />
	<@iucnTextarea "MKV.groundsForEvaluationNotes" />
	
	<@iucnSection "Kriteerit" />
	<#list ["A", "B", "C", "D", "E"] as criteria>
		<@iucnInput "MKV.criteria"+criteria "MKV.criteria"+criteria+"Notes" "criteriaRow" />
		<@iucnInput "MKV.status"+criteria "MKV.status"+criteria+"Notes" "criteriaStatusRow" /> 
	</#list>
	<@iucnTextarea "MKV.criteriaNotes" />
	
	<@iucnSection "Uhanalaisuus" />	
	<@iucnInput "MKV.redListStatus" "MKV.redListStatusNotes" />

	<#assign ddReasonClass = "ddReasonRow">
	<#if !evaluation?? || !evaluation.hasValue("MKV.ddReason")><#assign ddReasonClass = "ddReasonRow hidden"></#if>
	<@iucnInput "MKV.ddReason" "MKV.ddReasonNotes" ddReasonClass />

 	<@iucnInput "MKV.criteriaForStatus" "MKV.criteriaForStatusNotes" />
	<@iucnMinMax "Arvioinnin epävarmuuden vaihteluväli" "MKV.redListStatusMin" "MKV.redListStatusMax" />
	
	<@iucnInput "MKV.exteralPopulationImpactOnRedListStatus" "MKV.exteralPopulationImpactOnRedListStatusNotes" />
	
	<@iucnInput "MKV.reasonForStatusChange" "MKV.reasonForStatusChangeNotes" />
	
	<#assign vulnerableClass = "vulnerableRow hidden">
	<#if evaluation?? && evaluation.vulnerable>
		<#assign vulnerableClass = "">
	</#if>

	<@iucnInput "MKV.possiblyRE" "MKV.possiblyRENotes" vulnerableClass />
	<@iucnTextarea "MKV.lastSightingNotes" vulnerableClass />
	<@iucnTextarea "MKV.redListStatusAccuracyNotes" />
	
	<@iucnInput "MKV.lsaRecommendation" "MKV.lsaRecommendationNotes" vulnerableClass />
	<tr class="${vulnerableClass}">
		<th><label>Nykyinen LSA-status</label></th>
		<td colspan="2">
			<#list taxon.administrativeStatuses as adminStatus>
				<#if adminStatus == "MX.finlex160_1997_appendix4_specialInterest" || adminStatus == "MX.finlex160_1997_appendix4">
					${properties.getProperty("MX.hasAdminStatus").range.getValueFor(adminStatus).label.forLocale("fi")}<br />
				</#if>
			</#list>
		</td>
	</tr>
	
	<@iucnInput "MKV.percentageOfGlobalPopulation" "MKV.percentageOfGlobalPopulationNotes" />
	
	
	<@iucnSection "Lähteet" />
	<@iucnPublications "MKV.publication" />   
	<@iucnTextarea "MKV.otherSources" />
	
	</tbody>
</table>

<#if permissions>
	<div class="submitButtonContainer">
		<textarea placeholder="Tallennuskommentit" class="editNotesInput" name="MKV.editNotes">${(editNotes!"")?html}</textarea>
		<button id="saveButton">Tallenna</button>
		<button id="readyForCommentsButton">Valmis kommentoitavaksi</button>
		<button id="readyButton" class="ready">Valmis</button>
		<button id="cancelButton">Peruuta kaikki muutokset</button>
	</div>
</form>
</#if>

<@propertyCommentsScript />

<script>
function startsWith(needle, haystack) {
	return haystack.lastIndexOf(needle, 0) === 0;
}

function cancelComment() {
	$("#makingComment").remove();
	$("#evaluationEditForm :input").prop('disabled', false);
	$("#evaluationEditForm").fadeTo(0, 1);
	$("#remarksField").val('');
	makingComment = false;
}

var makingComment = false;
var commentingStillPossible = true;

$(function() {
	$("#evaluationEditForm :input").on('change', function() {
		if (!commentingStillPossible) return;
		$("#remarksEditForm :input").prop('disabled', true);
		$("#remarksEditForm").fadeTo(0, 0.5);
		commentingStillPossible = false;
	});
	$("#remarksField").on('focus', function() {
		if (makingComment) return;
		$("#evaluationEditForm :input").prop('disabled', true);
		$("#evaluationEditForm").before('<p id="makingComment" class="info">Olet tekemässä kommenttia. Muutoksia arvioinnin tietoihin ei voi tallentaa yhtä aikaa. <button onclick="cancelComment();">Peruuta</button></p>');
		$("#evaluationEditForm").fadeTo(0, 0.5);
		makingComment = true;
	});
		
	$("select").each(function() {
		var name = $(this).attr('name');
		if (!name) return;
		if (!startsWith("MKV.status", name)) return;
		$(this).addClass('criteriaStatusSelect');
		$(this).on('change', criteriaStatusChanged);
	});
		
	$("select")
		.not(".regionalStatusRow select")
		.not(".ddReasonRow select")
		.not(".vulnerableRow select")
		.chosen({ allow_single_deselect:true });
	
	$("label").tooltip();
	
	$(".notes textarea").each(function() {
		updateNotes($(this));
	});
	
	$(".notes textarea").on('change', function() {
		$(this).closest('.notes').fadeOut('fast', updateNotes( $(this) ));
	});
	
	$(".noteViewer").tooltip();
	
	$(".closeNoteEditButton").on('click', function() {
		$(this).closest('.notes').find('textarea').trigger('change');
	});
 	
 	$("button.add").on('click', function() {
 		var countOfExisting = $(this).parent().find(':input').size();
 		var input = $(this).prevAll(":input").first();
 		var clone = input.clone().val('').show().removeAttr('display');
 		var newNameAttribute = clone.attr('name').replace('___0', '___'+(countOfExisting+1));
		clone.attr('name', newNameAttribute);  
 		$(this).before('<br />').before(clone);
 		clone.chosen({ allow_single_deselect:true });
 	});
 	
 	$("button.addHabitatPair").on('click', function() {
 		var countOfExistingPairs = $(this).parent().find('.habitatPair').size();
 		var pair = $(this).parent().find('.habitatPair').first();
 		var clone = $('<div class="habitatPair"></div>');
 		pair.find("select").each(function() {
 			var clonedSelect = $(this).clone().val('').show().removeAttr('display');
 			var newNameAttribute = clonedSelect.attr('name').replace('___0___', '___'+(countOfExistingPairs+1)+'___');
 			clonedSelect.attr('name', newNameAttribute);  
 			clone.append(clonedSelect);
 		});
 		$(this).before(clone);
 		clone.find('select').chosen({ allow_single_deselect:true });
 	});
 	
 	$(window).on('scroll', function() {
 		var scrollTop = $(window).scrollTop();
 		if (scrollTop > 300) {
 			if (!headerAbsolute) {
 				$('h2').addClass('floatingHeader');
 				$('.errorMessage').addClass('floatingErrorMessage');
 				headerAbsolute = true;
 			}
 		} else {
 			if (headerAbsolute) {
 				$('h2').removeClass('floatingHeader');
 				$('.errorMessage').removeClass('floatingErrorMessage');
 				headerAbsolute = false;
 			}
 		}
 	});
 	
 	$("#saveButton").on('click', function() {
 		$("#saveButton, #readyButton, #readyForCommentsButton").prop("disabled", 'disabled');
 		$("#evaluationState").val("MKV.stateStarted");
 		document.getElementById("evaluationEditForm").submit();
 	});
 	$("#readyButton").on('click', function() {
 		$("#saveButton, #readyButton, #readyForCommentsButton").prop("disabled", 'disabled');
 		$("#evaluationState").val("MKV.stateReady");
 		document.getElementById("evaluationEditForm").submit();
 	});
 	$("#readyForCommentsButton").on('click', function() {
 		$("#saveButton, #readyButton, #readyForCommentsButton").prop("disabled", 'disabled');
 		$("#evaluationState").val("MKV.stateReadyForComments");
 		document.getElementById("evaluationEditForm").submit();
 	});
 	$("#cancelButton").on('click', function() {
 		if (confirm("Peruuta muutokset?")) {
 			location.href = location.href;
 		}
 	});
 	
 	$(".integerProperty").on('change', function() {
 		var val = $(this).val();
 		if (val == '' || isPositiveInteger(val)) {
 			$(this).removeClass('validationError');
 		} else {
 			$(this).addClass('validationError');
 		}
 	});
 	
 	$(".decimalProperty").on('change', function() {
 		var val = $(this).val();
 		if (val == '' || isPositiveDecimal(val)) {
 			$(this).removeClass('validationError');
 		} else {
 			$(this).addClass('validationError');
 		}
 	});
 	
 	$(".percentProperty").on('change', function() {
 		var val = $(this).val();
 		if (val == '' || isInteger(val)) {
 			$(this).removeClass('validationError');
 		} else {
 			$(this).addClass('validationError');
 		}
 	});
 	
 	$('#evaluationEditForm').find('input,select').keydown(function(event){
        if ( event.keyCode == 13 ){
            event.preventDefault();
        }
    });
    
    $("h2 a").on('click', function() {
    	$(window).scrollTop(0);
    	return false;
    });
    
    $("select[name='MKV.redListStatus']").on('change', function() {
    	var status = $(this).val(); 
    	if (status == 'MX.iucnDD') {
    		$(".ddReasonRow").show().find('select').chosen({ allow_single_deselect:true });
    	}
    	var statusOrder = statusComparator[status];
    	if (statusOrder && statusOrder >= 3) {
    		$(".vulnerableRow").show().find('select').chosen({ allow_single_deselect:true });
    	}
    });
    
    $("input[name='MKV.populationSizePeriodEnd'], input[name='MKV.populationSizePeriodBeginning']").on('change', function() {
    	var end = $("input[name='MKV.populationSizePeriodEnd']").val();
    	var beginning = $("input[name='MKV.populationSizePeriodBeginning']").val();
    	if (isPositiveInteger(end) && isPositiveInteger(beginning)) {
    		if (~~Number(end) < ~~Number(beginning)) {
    			var change = beginning - end;
    			var changePercentage = change / beginning * 100;
    			changePercentage = Math.floor(changePercentage);
    			$("input[name='MKV.decreaseDuringPeriod']").val(changePercentage); 
    		} else {
    			$("input[name='MKV.decreaseDuringPeriod']").val('');
    		}
    	}
    });
    
    $("#markAllDoesNotOccurButton").on('click', function() {
    	$("select.regionalOccurrence").each(function() {
    		if ($(this).val() == '') {
    			$(this).val('MX.doesNotOccur');
    			$(this).trigger("chosen:updated").change();
    		}
    	});
    }); 
    
    $("#revealRegionalThreatenedButton").on('click', function() {
    	$(".regionalThreatened").show();
    });
    
    $("#copyButton").on('click', function() {
    	$(this).prop('disabled','disabled');
    	window.location.href = '${baseURL}/iucn/species/${taxon.qname}/${selectedYear}?copy=true';
    });

	<#if evaluation?? && evaluation.hasRegionalThreatenedData()>
		$(".regionalThreatened").show();
	</#if>

});


function isPositiveInteger(str) {
    var n = ~~Number(str);
    return String(n) === str && n >= 0;
}

function isPositiveDecimal(str) {
	str = str.replace(/,/g , ".");
    if (!isNaN(parseFloat(str)) && isFinite(str)) {
    	return parseFloat(str) > 0; 
    }
    return false;
}

function isInteger(str) {
	var n = ~~Number(str);
    return String(n) === str;
}

var headerAbsolute = false;

function shorten(text) {
	if (text.length <= 35) return text;
	return text.substring(0, 32) + '...';
}
function updateNotes(noteInput) {
	noteInput.closest('td').find('.noteViewer').remove();
	var noteViewerContent = '<div class="noteViewer noteViewerEditable">';
	if (noteInput.val()) {
		var shortText = shorten(noteInput.val());
		noteViewerContent += '<span title="'+noteInput.val()+'"><span class="ui-icon ui-icon-comment"></span>'+shortText+'</span>';
	} else {
		noteViewerContent += '<a href="#">muistiinpanot</a>';
	}
	noteViewerContent += '</div>'
	var noteViewer = $(noteViewerContent);
	noteInput.closest('td').append(noteViewer);
	noteViewer.tooltip();
	noteViewer.on('click', function() {
		var notesContainer = $(this).closest('td').find('.notes').first();
		$(this).fadeOut('fast', function() {
			notesContainer.fadeIn('fast', function() {
				$(this).find('textarea').first().focus();
			});
		});
		return false;
	});
}

var statusComparator = {
	"MX.iucnEX": 8,
	"MX.iucnEW": 7,
	"MX.iucnRE": 6,
	"MX.iucnCR": 5,
	"MX.iucnEN": 4,
	"MX.iucnVU": 3,
	"MX.iucnNT": 2,
	"MX.iucnLC": 1,
	"MX.iucnDD": -1,
	"MX.iucnNA": -1,
	"MX.iucnNE": -1
};

function criteriaStatusChanged() {
	var statuses = new Array();
	$(".criteriaStatusSelect").each(function() {
		var status = $(this).val();	
		if (status) statuses.push(status);
	});
	if (statuses.length < 1) return;
	
	var highestStatus = getHighestStatus(statuses);
	if (!highestStatus) return;
	
	changeIfNotSet($("select[name='MKV.redListStatus']"), highestStatus);

	var criteriaText = "";
	$(".criteriaStatusSelect").each(function() {
		var status = $(this).val();	
		if (status === highestStatus) {
			var thisStatusCriteriaText = $(this).closest('tr').prev().find('input').val();
			if (thisStatusCriteriaText) criteriaText += thisStatusCriteriaText + "; "; 
		}
	});
	if (criteriaText.endsWith("; ")) criteriaText = criteriaText.substring(0, criteriaText.length - 2);
	$("input[name='MKV.criteriaForStatus']").val(criteriaText);
}

var originalValues = {};
$(function() {
	$("select").each(function() {
		var name = $(this).attr('name');
		originalValues[name] = $(this).val();
	});
});

function changeIfNotSet(e, val) {
	var originalVal = originalValues[e.attr('name')];
	if (!originalVal) {
		e.val(val).trigger("chosen:updated").change();
	}
}

function getHighestStatus(statuses) {
	var highest = -1;
	var highestStatus;
	for (var i in statuses) {
		var status = statuses[i];
		var order = statusComparator[status];
		highest = Math.max(order, highest);
		if (order == highest) highestStatus = status;
	}
	return highestStatus;
}

<#if evaluation??>
function deleteRemark(statementId) {
	if (!confirm("Poistetaanko kommentti?")) return false;
	var delform = $('<form>', {
        action: '${baseURL}/iucn/remarks?delete='+statementId+'&evaluationId=${evaluation.id}',
        method: 'post'
    });
   	delform.appendTo($(document.body)).submit();
}
</#if>
</script>

<script>
$(function() {
    if (typeof erroreousFields !== 'undefined') {
    	for (i in erroreousFields) {
    		var erroreousField = erroreousFields[i];
    		$('input[name*="'+erroreousField+'"], select[name*="'+erroreousField+'"]').addClass('validationError').each(function() {
    			$(this).parent().find('div').find('span').addClass('validationError');
    		});
    	}
    }
});
</script>

<#include "luomus-footer.ftl">