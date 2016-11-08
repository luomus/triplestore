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

<#if taxon.synonyms?has_content>
	<div class="taxonInfo">
		<h6>Synonyymit</h6>
		<ul>
			<#list taxon.synonyms as synonym>
				<li><@printScientificNameAndAuthor synonym /></li>
			</#list>
		</ul>
	</div>	
</#if>

<#if taxon.notes?has_content || taxon.privateNotes?has_content>
	<div class="taxonInfo">
		<h6>Huomioita taksonomiasta</h6>
		<#if taxon.notes?has_content>
			<p class="info">${taxon.notes?html}</p>
		</#if>
		<#if taxon.privateNotes?has_content>
			<p class="info">${taxon.privateNotes?html}</p>
		</#if>
	</div>
</#if>

<div class="taxonInfo">
	<h6>Eliöryhmä</h6>
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
					${(entry.notes?html)!""}
					<#if entry.editorQname??>
						&mdash; ${persons[entry.editorQname].fullname?html}
					</#if> 
				</li>				
			</#list>
		</ul>
	</div>
</#if>

</div>
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
			<th>
				<#if comparison??>
					${comparison.evaluationYear} tiedot
					<button id="copyButton">Kopio &raquo;</button>
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
	<@iucnInput "MKV.typeOfOccurrenceInFinland" "MKV.typeOfOccurrenceInFinlandNotes" "" occurrenceStatuses />
	<@iucnMinMax "Levinneisyysalueen koko" "MKV.distributionAreaMin" "MKV.distributionAreaMax" "MKV.distributionAreaNotes" />
    <@iucnMinMax "Esiintymisalueen koko" "MKV.occurrenceAreaMin" "MKV.occurrenceAreaMax" "MKV.occurrenceAreaNotes" />

	<@iucnSection "Esiintymisalueet Suomessa <span> &mdash; Täytettävä jos luokka NT-RE</span>" />
	<#list areas?keys as areaQname>
		<@iucnOccurrence areaQname />
	</#list>
	<@iucnTextarea "MKV.occurrenceNotes" />

	<@iucnSection "Elinympäristö" />   
	<@iucnHabitatFields />   

	<@iucnSection "Arvioinnin perusteet" />
	<@iucnInput "MKV.generationAge" "MKV.generationAgeNotes" />
	<@iucnInput "MKV.evaluationPeriodLength" "MKV.evaluationPeriodLengthNotes" />
	<#-- <@iucnMinMax "Esiintyminen lkm" "MKV.countOfOccurrencesMin" "MKV.countOfOccurrencesMax" "MKV.countOfOccurrencesNotes" /> -->
	<@iucnMinMax "Yksilömäärä" "MKV.individualCountMin" "MKV.individualCountMax" "MKV.individualCountNotes" />
	<@iucnMinMax "Populaation pieneminen tarkastelujakson aikana" "MKV.countOfOccurrencesPeriodBegining" "MKV.countOfOccurrencesPeriodEnd" "MKV.countOfOccurrencesPeriodNotes" />
	<@iucnInput "MKV.decreaseDuringPeriod" "MKV.decreaseDuringPeriodNotes" />
	<@iucnInput "MKV.populationVaries" "MKV.populationVariesNotes" />
	<@iucnInput "MKV.fragmentedHabitats" "MKV.fragmentedHabitatsNotes" />
	<@iucnInput "MKV.borderGain" "MKV.borderGainNotes" />
	<@iucnInput "MKV.endangermentReason" "MKV.endangermentReasonNotes" />
	<@iucnInput "MKV.threats" "MKV.threatNotes" />

	<@iucnSection "Kriteerit" />
	<#list ["A", "B", "C", "D", "E"] as criteria>
		<@iucnInput "MKV.criteria"+criteria "MKV.criteria"+criteria+"Notes" />
		<@iucnInput "MKV.status"+criteria "MKV.status"+criteria+"Notes" />
	</#list>
	
	<@iucnSection "Uhanalaisuus" />	
	<@iucnInput "MKV.redListStatus" "MKV.redListStatusNotes" />
	<#assign ddReasonClass = "ddReasonRow">
	<#if !evaluation.hasValue("MKV.ddReason")><#assign ddReasonClass = "ddReasonRow hidden"></#if>
	<@iucnInput "MKV.ddReason" "MKV.ddReasonNotes" ddReasonClass />
 	<@iucnInput "MKV.criteriaForStatus" "MKV.criteriaForStatusNotes" />
	<@iucnMinMax "Arvioinnin epävarmuuden vaihteluväli" "MKV.redListStatusMin" "MKV.redListStatusMax" />
	<@iucnInput "MKV.reasonForStatusChange" "MKV.reasonForStatusChangeNotes" />
	<@iucnTextarea "MKV.redListStatusAccuracyNotes" />
	<@iucnInput "MKV.lsaRecommendation" "MKV.lsaRecommendationNotes" />
	<@iucnInput "MKV.possiblyRE" />
	<@iucnTextarea "MKV.lastSightingNotes" />
	
	<#if draftYear != selectedYear>
		<@iucnSection "Uhanalaisuusindeksi" />
		<@iucnInput "MKV.redListIndexCorrection" "MKV.redListIndexCorrectionNotes" />
	</#if>
	
	<@iucnSection "Alueellinen uhanalaisuus" />
	<#assign hasRegionalData = false>
	<#list areas?keys as areaQname>
		<#assign fieldName = "MKV.regionalStatus_"+areaQname>
		<#if evaluation.hasValue(fieldName)><#assign hasRegionalData = true></#if>
	</#list>
	<#if !hasRegionalData>
		<tr><td colspan="3"><button id="showRegionalButton">Haluan määritellä alueellisen uhanalaisuuden</button></td></tr>
	</#if>
	<#list areas?keys as areaQname>
		<@iucnRegionalStatus areaQname hasRegionalData />
	</#list>
	
	<@iucnSection "Lähteet" />
	<@iucnPublications "MKV.publication" />   

	</tbody>
</table>

<#if permissions>
<@submitButtons/>
</form>
</#if>

<#macro submitButtons>
	<div class="submitButtonContainer">
		<textarea placeholder="Tallennuskommentit" class="editNotesInput" name="MKV.editNotes">${(editNotes!"")?html}</textarea>
		<button class="saveButton">Tallenna</button>
		<button class="ready readyButton">Arviointi valmis</button>
	</div>
</#macro>

<#macro iucnSection title>
	<tr class="section">
		<th colspan="3"><label>${title}</label></th>
	</tr>
</#macro>

<#macro iucnInputField fieldName customRange=[]>
	<#assign property = evaluationProperties.getProperty(fieldName)>
	<#assign values = ['']>
	<#if evaluation?? && evaluation.hasValue(fieldName)>
		<#assign values = evaluation.getValues(fieldName)>
	</#if>
	<@iucnInputFieldWithValues property values customRange />
	<#if property.hasUnitOfMeasurement()>
		${property.unitOfMeasurement.label.forLocale("fi")?html}
	</#if>
</#macro>

<#macro iucnInputFieldWithValues property values customRange=[]>
	<#if property.literalProperty && !property.booleanProperty>
		<#list values as value>
			<#if property.hasUnitOfMeasurement() && property.unitOfMeasurement.qname == "MZ.unitOfMeasurementPercent">
				<input class="percentProperty" name="${property.qname}" type="text" value="${value?html}">
			<#elseif property.integerProperty>
				<input class="integerProperty" name="${property.qname}" type="text" value="${value?html}">
			<#else>
				<input name="${property.qname}" type="text" value="${value?html}">
			</#if>
			<#if value_has_next><br /></#if>
		</#list>
		<#if property.repeated>
			<button class="add">+ Lisää</button>
		</#if>
	<#elseif property.booleanProperty>
		<#assign value = values?first>
		<select name="${property.qname}" class="shortChosen" data-placeholder="Kyllä/Ei">
			<option value="" label=".."></option>
			<#list property.range.values as optionValue>
				<#if value == optionValue.qname>
					<option value="${optionValue.qname}" selected="selected">${optionValue.label.forLocale("fi")?html}</option>
				<#else>
					<option value="${optionValue.qname}" >${optionValue.label.forLocale("fi")?html}</option>
				</#if>
			</#list>
		</select>
	<#else>
		<select name="${property.qname}"  data-placeholder="..." <#if property.repeated>multiple="multiple"</#if> >
			<option value="" label=".."></option>
			<#assign ranges = property.range.values>
			<#if customRange?has_content> <#assign ranges = customRange> </#if> 
			<#list ranges as enumValue>
				<#assign hasValue = false>
				<#list values as evaluationValue>
					<#if same(enumValue.qname, evaluationValue)><#assign hasValue = true><#break></#if>
				</#list>
				<option value="${enumValue.qname}"  <#if hasValue>selected="selected"</#if> >${enumValue.label.forLocale("fi")?html}</option>	
			</#list>
		</select>
		<#if property.repeated>(voi valita useita)</#if>
	</#if>
</#macro>

<#macro iucnInput fieldName notesFieldName="NONE" additionalClass="" customRange=[]>
	<tr class="${additionalClass}">
		<th><@iucnLabel fieldName /></th>
		<td><@showValue fieldName comparison /> <@showNotes notesFieldName comparison /></td>
		<td>
			<#if permissions>
				<@iucnInputField fieldName customRange /> <@editableNotes notesFieldName />
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
				<textarea name="${fieldName}"><#if evaluation??>${(evaluation.getValue(fieldName)?html)!""}</#if></textarea>
			<#else>
				<@showValue fieldName evaluation />
			</#if>
		</td>
	</tr>
</#macro>

<#macro iucnMinMax title minFieldName maxFieldName notesFieldName="NONE">
	<#assign property = evaluationProperties.getProperty(minFieldName)>
	<tr class="minMax">
		<th>
			<label>${title?html}</label> 
			<#if property.integerProperty><span class="unitOfMeasurement">(kokonaisluku)</span></#if>
			<#if (property.comments.forLocale("fi"))??>
				<div class="propertyComments">${property.comments.forLocale("fi")}</div>
			</#if>
		</th>
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


<#macro iucnRegionalStatus areaQname hasRegionalData>
	<#assign fieldName = "MKV.regionalStatus_"+areaQname>
	<#assign notesFieldName = "MKV.regionalStatus_"+areaQname+"_Notes">
	<tr class="regionalStatusRow <#if !hasRegionalData>hidden</#if>">
		<th><label>${areas[areaQname].name.forLocale("fi")?html}</label></th>
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

<#macro iucnOccurrence areaQname>	<tr>
		<th><label>${areas[areaQname].name.forLocale("fi")?html}</label></th>
		<td>
			<#if comparison?? && comparison.hasOccurrence(areaQname)>
				${occurrenceProperties.getProperty("MO.status").range.getValueFor(comparison.getOccurrence(areaQname).status.toString()).label.forLocale("fi")?html}
				<span class="hidden copyValue copyValue_MKV.hasOccurrence___${areaQname}">${comparison.getOccurrence(areaQname).status}</span>
			</#if>
		</td>
		<td>
			<#if permissions>
				<select name="MKV.hasOccurrence___${areaQname}" data-placeholder="...">
					<option value="" label=".."></option>
					<#list regionalOccurrenceStatuses as prop>
						<#if evaluation?? && evaluation.hasOccurrence(areaQname) && evaluation.getOccurrence(areaQname).status.toString() == prop.qname.toString()>
							<option value="${prop.qname}" selected="selected">${prop.label.forLocale("fi")?html}</option>
						<#else>
							<option value="${prop.qname}">${prop.label.forLocale("fi")?html}</option>
						</#if>
					</#list>
				</select>
			<#else>
				<#if evaluation?? && evaluation.hasOccurrence(areaQname)>
					${occurrenceProperties.getProperty("MO.status").range.getValueFor(evaluation.getOccurrence(areaQname).status.toString()).label.forLocale("fi")?html}
				</#if>
			</#if>
		</td>
	</tr>
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
			<option value="" label=".."></option>
			<#list habitatObjectProperties.getProperty("MKV.habitat").range.values as value>
				<option value="${value.qname}" <#if habitatObject != "NONE" && habitatObject.habitat == value.qname>selected="selected"</#if>>
					${value.label.forLocale("fi")?html}
				</option>
			</#list>
		</select>
		<select name ="${fieldName}___${index}___MKV.habitatSpecificType" data-placeholder="Tarkenteet" multiple="multiple">
			<option value="" label=".."></option>
			<#list habitatObjectProperties.getProperty("MKV.habitatSpecificType").range.values as value>
				<option value="${value.qname}" <#if habitatObject != "NONE" && habitatObject.habitatSpecificTypes?seq_contains(value.qname)>selected="selected"</#if>>
					${value.label.forLocale("fi")?html}
				</option>
			</#list>
		</select>
	</div>
</#macro>

<#macro showHabitatPairValue habitatObject>
	${habitatObjectProperties.getProperty("MKV.habitat").range.getValueFor(habitatObject.habitat).label.forLocale("fi")?html}
	<span class="hidden copyValue copyValue_MKV.habitat">${habitatObject.habitat?html}</span>
	<br />
	<#list habitatObject.habitatSpecificTypes as type>
		&nbsp; &nbsp; ${habitatObjectProperties.getProperty("MKV.habitatSpecificType").range.getValueFor(type).label.forLocale("fi")?html}
		<span class="hidden copyValue copyValue_MKV.habitatSpecificType">${type?html}</span>
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
									<option value="" label=".."></option>
									<#list publications?keys as publicationQname>
										<option value="${publicationQname}" <#if same(publication, publicationQname)>selected="selected"</#if> >${publications[publicationQname].citation?html}</option>
									</#list>
								</select>
							</td>
						</tr>
					</#list>
					</#if>
					<tr>
						<td>
							<select name="${fieldName}"  data-placeholder="Valitse julkaisu" >
								<option value="" label=".."></option>
								<#list publications?keys as publicationQname>
									<option value="${publicationQname}">${publications[publicationQname].citation?html}</option>
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
	<label>${(property.label.forLocale("fi")?html)!fieldName} <#if property.required><span class="required" title="Pakollinen tieto">*</span></#if></label>
	<#if property.integerProperty>
		<span class="unitOfMeasurement">(kokonaisluku)</span>
	</#if>
	<#if (property.comments.forLocale("fi"))??>
		<div class="propertyComments">${property.comments.forLocale("fi")}</div>
	</#if>
</#macro>

<#macro showValue fieldName data="NONE">
	<#if data != "NONE">
		<#assign property = evaluationProperties.getProperty(fieldName)>
		<#list data.getValues(fieldName) as value>
			<#if property.literalProperty && !property.booleanProperty>
				${value?html}
			<#else>
				${property.range.getValueFor(value).label.forLocale("fi")?html}
			</#if>
			<span class="hidden copyValue copyValue_${fieldName}">${value?html}</span>
			<#if value_has_next><br /><br /></#if>
		</#list>
	</#if>
</#macro>

<#macro showNotes notesFieldName data="NONE">
	<#if data != "NONE" && notesFieldName != "NONE" && data.hasValue(notesFieldName)>
		<div class="noteViewer"><span class="ui-icon ui-icon-comment" title="${data.getValue(notesFieldName)?html}"></span></div>
	</#if>
</#macro>

<#macro editableNotes notesFieldName>
	<#if notesFieldName != "NONE">
		<div class="notes hidden">
			<@iucnLabel notesFieldName />
			<textarea name="${notesFieldName}"><#if evaluation??>${(evaluation.getValue(notesFieldName)?html)!""}</#if></textarea>
			<br />
			<button class="closeNoteEditButton">Tallenna ja sulje</button>
		</div>
	</#if>
</#macro>

<script>
$(function() {
	
	$("select").each(function() {
		var name = $(this).attr('name');
		if (!name.startsWith("MKV.status")) return;
		$(this).addClass('criteriaStatusSelect');
		$(this).on('change', criteriaStatusChanged);
	});
		
	$("select")
		.not(".regionalStatusRow select")
		.not(".ddReasonRow select")
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
 	
 	$(".propertyComments").each(function() {
 		var comments = $(this); 
 		comments.hide();
 		var info = $('<span class="ui-icon ui-icon-info" />');
 		$(this).before(info);
 		info.on('mouseover click', function() {
 			comments.addClass('modalInfoContainer');
 			comments.fadeIn(500);
 		});
 		info.on('mouseleave', function() {
 			$('.modalInfoContainer').fadeOut(200);
 		});
 	});
 	
 	$(".integerProperty").on('change', function() {
 		var val = $(this).val();
 		if (val == '' || isPositiveInteger(val)) {
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
    
    $('#copyButton').on('click', function() {
    	$(".copyValue").each(function() {
    		// XXX
    		//var targetFieldName = $(this).
    		//var targetContainer = $(this).closest('tr').find('td').last();
    		
    	});
    });
    
    $("#showRegionalButton").on('click', function() {
    	$(this).hide();
    	$(".regionalStatusRow").fadeIn('fast');
    });
    
    $("select[name='MKV.redListStatus']").on('change', function() {
    	if ($(this).val() == 'MX.iucnDD') {
    		$(".ddReasonRow").show().find('select').chosen();
    	}
    });
});

function isPositiveInteger(str) {
    var n = ~~Number(str);
    return String(n) === str && n >= 0;
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
		noteViewerContent += '<a href="#">kommentoi</a>';
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
	"MX.iucnLC": 1
};

function criteriaStatusChanged() {
	var statuses = new Array();
	$(".criteriaStatusSelect").each(function() {
		var status = $(this).val();	
		if (status) statuses.push(status);
	});
	if (statuses.length < 1) return;
	
	var highestStatus = getHighestStatus(statuses);
	var lowestStatus = getLowestStatus(statuses);
	if (!highestStatus) return;
	
	changeIfNotSet($("select[name='MKV.redListStatusMin']"), lowestStatus);
	changeIfNotSet($("select[name='MKV.redListStatusMax']"), highestStatus);
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
		e.val(val).trigger("chosen:updated");
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

function getLowestStatus(statuses) {
	var lowest = 99;
	var lowestStatus;
	for (var i in statuses) {
		var status = statuses[i];
		var order = statusComparator[status];
		lowest = Math.min(order, lowest);
		if (order == lowest) lowestStatus = status;
	}
	return lowestStatus;
}

</script>

<#include "luomus-footer.ftl">