<#include "luomus-header.ftl">
<#include "iucn-macro.ftl">

<#list target.groups as groupQname>
	<a href="${baseURL}/iucn/group/${groupQname}/${selectedYear}" class="goBack">
		Takaisin (${taxonGroups[groupQname].name.forLocale("fi")!""})
	</a><br />
</#list>

<h1>Uhanalaisuusarviointi - ${selectedYear} <#if draftYear == selectedYear>(LUONNOS)</#if></h1>

<h2><a href="#"><@printScientificNameAndAuthor taxon /> ${taxon.getVernacularName("fi")!""}</a></h2>

<@toolbox/>		

<#if successMessage?has_content>
	<p class="successMessage">${successMessage}</p>
</#if>
<#if errorMessage?has_content>
	<div class="errorMessage">${errorMessage}</div>
</#if>

<#macro tree taxon>
	<#if taxon.hasParent()>
		<@tree taxon.parent />
	</#if>
	<li><@printScientificNameAndAuthor taxon /> <span class="vernacularName">${taxon.getVernacularName("fi")!""}</span></li>
</#macro>

<div class="taxonInfo">
	<h6>Taksonomia</h6>
	<ul class="taxonTree">
		<@tree taxon/>
	</ul>
</div>

<#if taxon.synonymTaxons?has_content>
	<div class="taxonInfo">
		<h6>Synonyymit</h6>
		<ul>
			<#list taxon.synonymTaxons as synonym>
				<li><@printScientificNameAndAuthor synonym /></li>
			</#list>
		</ul>
	</div>	
</#if>

<#if taxon.notes?has_content || taxon.privateNotes?has_content>
	<div class="taxonInfo">
		<h6>Huomioita taksonomiasta</h6>
		<#if taxon.notes?has_content>
			<p class="info">${taxon.notes}</p>
		</#if>
		<#if taxon.privateNotes?has_content>
			<p class="info">${taxon.privateNotes}</p>
		</#if>
	</div>
</#if>

<div class="taxonInfo">
	<h6>Elöryhmät</h6>
	<ul>
		<#list target.groups as groupQname>
			<li>
				<a href="${baseURL}/iucn/group/${groupQname}/${selectedYear}">
					${taxonGroups[groupQname].name.forLocale("fi")!""}
				</a>
				<p class="info">Uhanalaisuusarviojat: <@editors groupQname /></p>
			</li>
		</#list>
	</ul>
</div>

<#if taxon.administrativeStatuses?has_content>
	<div class="taxonInfo">
		<h6>Hallinnolliset ominaisuudet</h6>
		<ul>
			<#list taxon.administrativeStatuses as adminStatus>
				<li>${properties.getProperty("MX.hasAdminStatus").range.getValueFor(adminStatus).label.forLocale("fi")}</li>				
			</#list>
		</ul>
	</div>
</#if>

<#if editHistory?has_content>
	<div class="taxonInfo">
		<h6>Tallennushistoria</h6>
		<ul>
			<#list editHistory.entries as entry>
				<li>
					${entry.notes!""}
					<#if entry.editorQname??>
						&mdash; ${persons[entry.editorQname].fullname}
					</#if> 
				</li>				
			</#list>
		</ul>
	</div>
</#if>

<div class="clear"></div>

<#if permissions>
<form id="evaluationEditForm" action="${baseURL}/iucn/species/${taxon.qname}/${selectedYear}" method="post" onsubmit="return false;">
<input type="hidden" name="evaluationId" value="${(evaluation.id)!""}" />
<input type="hidden" name="MKV.evaluatedTaxon" value="${taxon.qname}" />
<input type="hidden" name="MKV.evaluationYear" value="${selectedYear}" />
<input type="hidden" name="MKV.state" id="evaluationState" />
<@submitButtons/>
</#if>

<table class="resourceListTable evaluationEdit">
	<thead>
		<tr>
			<th>Muuttuja</th>
			<th><#if comparison??>${comparison.evaluationYear} tiedot<#else>Ei edellisiä tietoja</#if></th>
			<th>${selectedYear} tiedot</th>
		</tr>
	</thead>
	<tbody>
	
	<@iucnSection "Arvioinnissa käytetty taksonominen tulkinta" />
	<@iucnTextarea "MKV.taxonomicNotes" />
		
	<@iucnSection "Luokka" />	
	<@iucnInput "MKV.redListStatus" "MKV.redListStatusNotes" />
	<@iucnInput "MKV.redListIndexCorrection" "MKV.redListIndexCorrectionNotes" />
	<@iucnTextarea "MKV.redListStatusAccuracyNotes" />
	<@iucnInput "MKV.reasonForStatusChange" "MKV.reasonForStatusChangeNotes" />
	<@iucnMinMax "Vaihteluväli" "MKV.redListStatusMin" "MKV.redListStatusMax" />
			
	<@iucnSection "Kriteerit" />
	<@iucnInput "MKV.criteriaA" "MKV.criteriaANotes" />
	<@iucnInput "MKV.criteriaB" "MKV.criteriaBNotes" />
	<@iucnInput "MKV.criteriaC" "MKV.criteriaCNotes" />
	<@iucnInput "MKV.criteriaD" "MKV.criteriaDNotes" />
	<@iucnInput "MKV.criteriaE" "MKV.criteriaENotes" />
	
	<@iucnSection "Esiintymisalueet" />
	<#list areas?keys as areaQname>
		<@iucnOccurrence areaQname />
	</#list>
	<@iucnTextarea "MKV.occurrenceNotes" />

	<@iucnSection "Esiintyminen" />
	<@iucnMinMax "Esiintyminen lkm" "MKV.countOfOccurrencesMin" "MKV.countOfOccurrencesMax" "MKV.countOfOccurrencesNotes" />
	<@iucnMinMax "Esiintymiä alussa/lopussa" "MKV.countOfOccurrencesPeriodBegining" "MKV.countOfOccurrencesPeriodEnd" "MKV.countOfOccurrencesPeriodNotes" />
	<@iucnInput "MKV.decreaseDuringPeriod" "MKV.decreaseDuringPeriodNotes" />
	<@iucnInput "MKV.borderGain" "MKV.borderGainNotes" />
	<@iucnMinMax "Levinneisyysalueen koko" "MKV.distributionAreaMin" "MKV.distributionAreaMax" "MKV.distributionAreaNotes" />
	
	<@iucnSection "Elinympäristö" />   
	<@iucnHabitatFields />   
	<@iucnInput "MKV.fragmentedHabitats" "MKV.fragmentedHabitatsNotes" />
   
	<@iucnSection "Kanta" />
	<@iucnMinMax "Yksilömäärä" "MKV.individualCountMin" "MKV.individualCountMax" "MKV.individualCountNotes" />
	<@iucnInput "MKV.generationAge" "MKV.generationAgeNotes" />
	<@iucnInput "MKV.evaluationPeriodLength" "MKV.evaluationPeriodLengthNotes" />
	<@iucnInput "MKV.populationVaries" "MKV.populationVariesNotes" />
	
	<@iucnSection "Uhanalaisuus" />
	<@iucnInput "MKV.endangermentReason" "MKV.endangermentReasonNotes" />
	<@iucnTextarea "MKV.actionNotes" />   
	<@iucnInput "MKV.lsaRecommendation" "MKV.lsaRecommendationNotes" />
	<@iucnInput "MKV.possiblyRE" />
	<@iucnTextarea "MKV.lastSightingNotes" />

	<@iucnSection "Lähteet" />
	<@iucnPublications "MKV.publication" />   

	<@iucnSection "Vakinaisuus <span>- Huom: Nämä tiedot ovat julkisia ja alla muokataan lajien varsinaisia taksonomiatietoja!</span>" /> 
	<@taxonOccurenceInFinland />
		
	</tbody>
</table>

<#if permissions>
<@submitButtons/>
</form>
</#if>

<#macro submitButtons>
	<div class="submitButtonContainer">
		<button class="saveButton">Tallenna</button>
		<button class="ready readyButton">Arviointi valmis</button>
		<textarea placeholder="Tallennuskommentit" class="editNotesInput" name="MKV.editNotes"></textarea>
	</div>
</#macro>

<#macro iucnSection title>
	<tr class="section">
		<th colspan="3"><label>${title}</label></th>
	</tr>
</#macro>

<#macro iucnInputField fieldName>
	<#assign property = evaluationProperties.getProperty(fieldName)>
	<#assign values = ['']>
	<#if evaluation?? && evaluation.hasValue(fieldName)>
		<#assign values = evaluation.getValues(fieldName)>
	</#if>
	<@iucnInputFieldWithValues property values />
</#macro>

<#macro iucnInputFieldWithValues property values>
	<#if property.literalProperty && !property.booleanProperty>
		<#list values as value>
			<#if property.integerProperty>
				<input class="integerProperty" name="${property.qname}" type="text" value="${value}">
			<#else>
				<input name="${property.qname}" type="text" value="${value}">
			</#if>
			<#if value_has_next><br /></#if>
		</#list>
		<#if property.repeated>
			<button class="add">+ Lisää</button>
		</#if>
	<#elseif property.booleanProperty>
		<#assign value = values?first>
		<select name="${property.qname}" class="shortChosen" data-placeholder="Kyllä/Ei">
			<option value=""></option>
			<#list property.range.values as optionValue>
				<#if value == optionValue.qname>
					<option value="${optionValue.qname}" selected="selected">${optionValue.label.forLocale("fi")}</option>
				<#else>
					<option value="${optionValue.qname}" >${optionValue.label.forLocale("fi")}</option>
				</#if>
			</#list>
		</select>
	<#else>
		<select name="${property.qname}"  data-placeholder="..." <#if property.repeated>multiple="multiple"</#if> >
			<option value=""></option>
			<#list property.range.values as enumValue>
				<#assign hasValue = false>
				<#list values as evaluationValue>
					<#if same(enumValue.qname, evaluationValue)><#assign hasValue = true><#break></#if>
				</#list>
				<option value="${enumValue.qname}"  <#if hasValue>selected="selected"</#if> >${enumValue.label.forLocale("fi")}</option>	
			</#list>
		</select>
		<#if property.repeated>(voi valita useita)</#if>
	</#if>
</#macro>

<#macro iucnInput fieldName notesFieldName="NONE">
	<tr>
		<th><@iucnLabel fieldName /></th>
		<td><@showValue fieldName comparison /> <@showNotes notesFieldName comparison /></td>
		<td>
			<#if permissions>
				<@iucnInputField fieldName /> <@editableNotes notesFieldName />
			<#else>
				<@showValue fieldName evaluation /> <@showNotes notesFieldName evaluation />
			</#if>
		</td>
	</tr>
</#macro>

<#macro iucnTextarea fieldName>
	<tr>
		<th><@iucnLabel fieldName /></th>
		<td><@showValue fieldName comparison /></td>
		<td>
			<#if permissions>
				<textarea name="${fieldName}"><#if evaluation??>${evaluation.getValue(fieldName)!""}</#if></textarea>
			<#else>
				<@showValue fieldName evaluation />
			</#if>
		</td>
	</tr>
</#macro>

<#macro iucnMinMax title minFieldName maxFieldName notesFieldName="NONE">
	<tr class="minMax">
		<th><label>${title}</label></th>
		<td>
			<#if comparison?? && (comparison.hasValue(minFieldName) || comparison.hasValue(maxFieldName))>
				<@showValue minFieldName comparison /> - <@showValue maxFieldName comparison />
			</#if>
			<@showNotes notesFieldName comparison />
		</td>
		<td>
			<#if permissions>
				<@iucnInputField minFieldName /> - <@iucnInputField maxFieldName /> <@editableNotes notesFieldName />
			<#else>
				<#if evaluation?? && (evaluation.hasValue(minFieldName) || evaluation.hasValue(maxFieldName))>
					<@showValue minFieldName evaluation /> - <@showValue maxFieldName evaluation /> <@showNotes notesFieldName evaluation />
				</#if>
			</#if>
		</td>
	</tr>
</#macro>

<#macro iucnOccurrence areaQname>
	<tr>
		<th><label>${areas[areaQname].name.forLocale("fi")}</label></th>
		<td>
			<#if comparison?? && comparison.hasOccurrence(areaQname)>
				${occurrenceProperties.getProperty("MO.status").range.getValueFor(comparison.getOccurrence(areaQname).status.toString()).label.forLocale("fi")}
			</#if>
		</td>
		<td>
			<#if permissions>
				<select name="MKV.hasOccurrence___${areaQname}" data-placeholder="...">
					<option value=""></option>
					<#list occurrenceProperties.getProperty("MO.status").range.values as prop>
						<#if evaluation?? && evaluation.hasOccurrence(areaQname) && evaluation.getOccurrence(areaQname).status.toString() == prop.qname.toString()>
							<option value="${prop.qname}" selected="selected">${prop.label.forLocale("fi")}</option>
						<#else>
							<option value="${prop.qname}">${prop.label.forLocale("fi")}</option>
						</#if>
					</#list>
				</select>
			<#else>
				<#if evaluation?? && evaluation.hasOccurrence(areaQname)>
					${occurrenceProperties.getProperty("MO.status").range.getValueFor(evaluation.getOccurrence(areaQname).status.toString()).label.forLocale("fi")}
				</#if>
			</#if>
		</td>
	</tr>
</#macro>

<#macro taxonOccurenceInFinland>
<#if permissions>
	<tr>
		<th><@label "MX.typeOfOccurrenceInFinland" "" "fi" /></th>
		<td colspan="2">
			<#list taxon.typesOfOccurrenceInFinland as type>
				<@select "MX.typeOfOccurrenceInFinland" type />
			</#list>
			<button class="add">+ Lisää</button>
		</td>
	</tr>
	<tr>
		<th><@label "MX.typeOfOccurrenceInFinlandNotes" "" "fi" /></th>
		<td colspan="2">
			<@textarea "MX.typeOfOccurrenceInFinlandNotes" />
		</td>
	</tr>
<#else>
	<tr>
		<th><@label "MX.typeOfOccurrenceInFinland" "" "fi" /></th>
		<td colspan="2">
			<#list taxon.typesOfOccurrenceInFinland as type>
				${properties.getProperty("MX.typeOfOccurrenceInFinland").range.getValueFor(type).label.forLocale("fi")}
				<#if type_has_next>, </#if>
			</#list>
		</td>
	</tr>
	<tr>
		<th><@label "MX.typeOfOccurrenceInFinlandNotes" "" "fi" /></th>
		<td colspan="2">
			${taxon.typeOfOccurrenceInFinlandNotes!""}
		</td>
	</tr>
</#if>
</#macro>

<#macro iucnHabitatFields>
	<tr>
		<th><@iucnLabel "MKV.primaryHabitat" /></th>
		<td>
			<#if comparison?? && comparison.primaryHabitat??>
				<@showHabitatPairValue comparison.primaryHabitat />
			</#if>
			<@showNotes "MKV.habitatNotes" comparison />
		</td>
		<td>
			<#if permissions>
				<#if evaluation?? && evaluation.primaryHabitat??>
					<@editableHabitatPair "MKV.primaryHabitat" evaluation.primaryHabitat />
				<#else>
					<@editableHabitatPair "MKV.primaryHabitat" "NONE" />
				</#if>
				<@editableNotes "MKV.habitatNotes" />
			<#else>
				<#if evaluation?? && evaluation.primaryHabitat??>
					<@showHabitatPairValue evaluation.primaryHabitat />
				</#if>
				<@showNotes "MKV.habitatNotes" evaluation />
			</#if>
		</td>
	</tr>
	<tr>
		<th><@iucnLabel "MKV.secondaryHabitat" /></th>
		<td>
			<#if comparison??>
				<#list comparison.secondaryHabitats as habitat>
					<@showHabitatPairValue habitat />
					<#if habitat_has_next><br /></#if>
				</#list>
			</#if>
		</td>
		<td>
			<#if permissions>
				<#if evaluation??>
					<#list evaluation.secondaryHabitats as habitat>
						<@editableHabitatPair "MKV.secondaryHabitat" habitat habitat_index />
						<#if habitat_has_next><br /></#if>
					</#list>
				</#if>
				<#if !(evaluation??) || !(evaluation.secondaryHabitats?has_content)>
					<@editableHabitatPair "MKV.secondaryHabitat" "NONE" />
				</#if>
				<button class="addHabitatPair">+ Lisää</button>
			<#else>
				<#if evaluation??>
					<#list evaluation.secondaryHabitats as habitat>
						<@showHabitatPairValue habitat />
					</#list>
				</#if>	
			</#if>
		</td>
	</tr>
</#macro>

<#macro editableHabitatPair fieldName habitatObject index=0>
	<div class="habitatPair">
		<select name ="${fieldName}___${index}___MKV.habitat" data-placeholder="...">
			<option value=""></option>
			<#list habitatObjectProperties.getProperty("MKV.habitat").range.values as value>
				<option value="${value.qname}" <#if habitatObject != "NONE" && habitatObject.habitat == value.qname>selected="selected"</#if>>
					${value.label.forLocale("fi")}
				</option>
			</#list>
		</select>
		<select name ="${fieldName}___${index}___MKV.habitatSpecificType" data-placeholder="Tarkenteet" multiple="multiple">
			<option value=""></option>
			<#list habitatObjectProperties.getProperty("MKV.habitatSpecificType").range.values as value>
				<option value="${value.qname}" <#if habitatObject != "NONE" && habitatObject.habitatSpecificTypes?seq_contains(value.qname)>selected="selected"</#if>>
					${value.label.forLocale("fi")}
				</option>
			</#list>
		</select>
	</div>
</#macro>

<#macro showHabitatPairValue habitatObject>
	${habitatObjectProperties.getProperty("MKV.habitat").range.getValueFor(habitatObject.habitat).label.forLocale("fi")}
	<br />
	<#list habitatObject.habitatSpecificTypes as type>
		&nbsp; &nbsp; ${habitatObjectProperties.getProperty("MKV.habitatSpecificType").range.getValueFor(type).label.forLocale("fi")}
		<#if type_has_next><br /></#if>
	</#list>
	<br />
</#macro>

<#macro iucnPublications fieldName>
	<tr>
		<th><@iucnLabel fieldName /></th>
		<td><@showValue fieldName comparison /></td>
		<td>
			<#if permissions>
				<table class="publicationSelect">
					<tr>
						<th>Valitse julkaisu</th> 
					</tr>
					<#if evaluation??>
					<#list evaluation.getValues(fieldName) as publication>
						<tr>
							<td>
								<select name="${fieldName}">
									<option value=""></option>
									<#list publications?keys as publicationQname>
										<option value="${publicationQname}" <#if same(publication, publicationQname)>selected="selected"</#if> >${publications[publicationQname].citation}</option>
									</#list>
								</select>
							</td>
						</tr>
					</#list>
					</#if>
					<tr>
						<td>
							<select name="${fieldName}"  data-placeholder="Valitse julkaisu" >
								<option value=""></option>
								<#list publications?keys as publicationQname>
									<option value="${publicationQname}">${publications[publicationQname].citation}</option>
								</#list>
							</select>
						</td>
					</tr>
					<tr>
						<th>Tai luo uusi julkaisu</th> 
					</tr>
					<tr>
						<td><input class="createPublication" type="text" name="newIucnPublicationCitation" id="createNewIucnPublicationCitationInput" placeholder="Esim. 'Stubbs & Drake 2001, Stuke 2003' tai 'Kasviatlas 2008'"/></td>
					</tr>	
				</table>
			<#else>
				<@showValue fieldName evaluation />
			</#if>
		</td>
	</tr>
</#macro>

<#macro iucnLabel fieldName>
	<#assign property = evaluationProperties.getProperty(fieldName)> 
	<label>${property.label.forLocale("fi")!fieldName} <#if property.required><span class="required" title="Pakollinen tieto">*</span></#if></label>
</#macro>

<#macro showValue fieldName data="NONE">
	<#if data != "NONE">
		<#assign property = evaluationProperties.getProperty(fieldName)>
		<#list data.getValues(fieldName) as value>
			<#if property.literalProperty && !property.booleanProperty>
				${value}
			<#else>
				${property.range.getValueFor(value).label.forLocale("fi")}
			</#if>
			<#if value_has_next><br /><br /></#if>
		</#list>
	</#if>
</#macro>

<#macro showNotes notesFieldName data="NONE">
	<#if data != "NONE" && notesFieldName != "NONE" && data.hasValue(notesFieldName)>
		<div class="noteViewer"><span class="ui-icon ui-icon-comment" title="${data.getValue(notesFieldName)}"></span></div>
	</#if>
</#macro>

<#macro editableNotes notesFieldName>
	<#if notesFieldName != "NONE">
		<div class="notes hidden">
			<@iucnLabel notesFieldName />
			<textarea name="${notesFieldName}"><#if evaluation??>${evaluation.getValue(notesFieldName)!""}</#if></textarea>
			<br />
			<button class="emptyNoteButton">Tyhjennä</button>
			<button class="closeNoteEditButton">Sulje</button>
		</div>
	</#if>
</#macro>

<script>
$(function() {
	
	$("select").chosen();
	
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
 	
 	$(".emptyNoteButton").on('click', function() {
 		$(this).closest('.notes').find('textarea').val('').trigger('change');
 	});
 	
 	$("button.add").on('click', function() {
 		var input = $(this).prevAll(":input").first();
 		var clone = input.clone();
 		clone.val('');
 		$(this).before('<br />').before(clone);
 		clone.hide();
 		clone.fadeIn('fast');
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
 		clone.find('select').chosen();
 	});
 	
 	$(window).on('scroll', function() {
 		var scrollTop = $(window).scrollTop();
 		if (scrollTop > 350) {
 			if (!headerAbsolute) {
 				$('h2').addClass('floatingHeader');
 				headerAbsolute = true;
 			}
 		} else {
 			if (headerAbsolute) {
 				$('h2').removeClass('floatingHeader');
 				headerAbsolute = false;
 			}
 		}
 	});
 	
 	$(".saveButton").on('click', function() {
 		$("#evaluationState").val("MKV.stateStarted");
 		document.getElementById("evaluationEditForm").submit();
 	});
 	$(".readyButton").on('click', function() {
 		$("#evaluationState").val("MKV.stateReady");
 		document.getElementById("evaluationEditForm").submit();
 	});
 	
});

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
		noteViewerContent += '<a href="#">kommentoi</a>';
	}
	noteViewerContent += '</div>'
	var noteViewer = $(noteViewerContent);
	noteInput.closest('td').append(noteViewer);
	noteViewer.tooltip();
	noteViewer.on('click', function() {
		var notesContainer = $(this).closest('td').find('.notes').first();
		$(this).fadeOut('fast', function() {
			notesContainer.fadeIn('fast');
		});
		return false;
	});
}

</script>

<#include "luomus-footer.ftl">