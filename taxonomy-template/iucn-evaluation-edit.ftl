<#include "luomus-header.ftl">
<#include "iucn-macro.ftl">

<#list target.groups as groupQname>
	<a href="${baseURL}/iucn/group/${groupQname}/${selectedYear}" class="goBack">
		Takaisin (${taxonGroups[groupQname].name.forLocale("fi")!""})
	</a><br />
</#list>

<h1>Uhanalaisuusarviointi - ${selectedYear} <#if draftYear == selectedYear>(LUONNOS)</#if></h1>

<h2><@printScientificNameAndAuthor taxon /> ${taxon.getVernacularName("fi")!""}</h2>

<@toolbox/>		

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
				<li>${evaluationProperties.getProperty("MX.hasAdminStatus").range.getValueFor(adminStatus).label.forLocale("fi")}</li>				
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
</#if>

<table class="resourceListTable evaluationEdit">
	<thead>
		<tr>
			<th>Muuttuja</th>
			<th>${(comparison.year)!"Ei edellisiä tietoja"}</th>
			<th>${selectedYear}</th>
		</tr>
	</thead>
	<tbody>
	
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
	<@iucnHabitatPair "MKV.primaryHabitat" "MKV.habitatNotes" />
	<@iucnHabitatPair "MKV.secondaryHabitat" />   
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

	<@iucnSection "Arvioinnissa käytetty taksonominen tulkinta" />
	<@iucnTextarea "MKV.taxonomicNotes" />
		
	<@iucnSection "Vakinaisuus <span>- Huom: Nämä tiedot ovat julkisia ja alla muokataan lajien varsinaisia taksonomiatietoja!</span>" /> 
	<@taxonOccurenceInFinland />
		
	</tbody>
</table>

<#if permissions>
</form>
</#if>

<#macro iucnSection title>
	<tr class="section">
		<th colspan="3"><label>${title}</label></th>
	</tr>
</#macro>

<#macro iucnInputField fieldName>
	<#assign property = evaluationProperties.getProperty(fieldName)>
	<#if property.literalProperty>
		<#if property.integerProperty>
			<input class="integerProperty" name=""${fieldName}" type="text" value="<#if evaluation??>${evaluation.getValue(fieldName)!""}</#if>">
		<#elseif property.booleanProperty>
			<select name="${fieldName}">
				<option value=""></optioin>
				<#list property.range.values as optionValue>
					<option value="${optionValue.qname}" <#if booleanValue(value) == optionValue.qname>selected="selected"</#if>>${optionValue.label.forLocale("fi")}</option>
				</#list>
			</select>
		<#else>
			<input name=""${fieldName}" type="text" value="<#if evaluation??>${evaluation.getValue(fieldName)!""}</#if>">
		</#if>
	<#else>
		<select name="${fieldName}" class="chosen" data-placeholder="...">
			<option value=""></option>
			<#list property.range.values as enumValue>
				<#assign hasValue = false>
				<#if evaluation??>
				<#list evaluation.getValues(fieldName) as evaluationValue>
					<#if same(enumValue.qname, evaluationValue)><#assign hasValue = true><#break></#if>
				</#list>
				</#if>
				<option value="${enumValue.qname}"  <#if hasValue>selected="selected"</#if> >${enumValue.label.forLocale("fi")}</option>	
			</#list>
		</select>
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
		<th>${title}</th>
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
		<th>${areas[areaQname].name.forLocale("fi")}</th>
		<td>comp</td>
		<td>input</td>
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
			<@select "MX.typeOfOccurrenceInFinland" "" />
		</td>
	</tr>
	<tr>
		<th><@label "MX.occurrenceInFinlandPublication" "" "fi" /></th>
		<td colspan="2">
			<table class="publicationSelect">
				<tr>
					<th>Valitse julkaisu</th> 
				</tr>
				<#list taxon.getOccurrenceInFinlandPublicationsSortedByPublication(publications) as publication>
				<tr>
					<td>
						<select name="MX.occurrenceInFinlandPublication" class="chosen" <@checkPermissions/> >
							<option value=""></option>
							<#list publications?keys as publicationQname>
								<option value="${publicationQname}" <#if same(publication.qname, publicationQname)>selected="selected"</#if> >${publications[publicationQname].citation}</option>
							</#list>
						</select>
					</td>
				</tr>
				</#list>
				<tr>
					<td>
						<select name="MX.occurrenceInFinlandPublication" class="chosen" <@checkPermissions/> data-placeholder="Select existing publication" >
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
					<td><input type="text" name="newOccurrenceInFinlandPublicationCitation" id="createNewOccurrenceInFinlandPublicationInput" placeholder="Type citaction, for example 'Hudd, R. & Leskelä, A. 1998. Acidification-induced species shifts in coastal fisheries off the River Kyrönjoki, Finland: A case study. Ambio 27: 535–538.'"/></td>
				</tr>	
			</table>
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
		<th><@label "MX.occurrenceInFinlandPublication" "" "fi" /></th>
		<td colspan="2">
			<#list taxon.getOccurrenceInFinlandPublicationsSortedByPublication(publications) as publication>
				${publication.citation}
				<#if publication_has_next>, </#if>
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

<#macro iucnHabitatPair fieldName notesFieldName="NONE">
	<tr>
		<th><@iucnLabel fieldName /></th>
		<td>comp <@showNotes notesFieldName comparison /></td>
		<td>input <@editableNotes notesFieldName /></td>
	</tr>
</#macro>

<#macro taxonInput fieldName notesFieldName="NONE">
	<tr>
		<th><@iucnLabel fieldName /></th>
		<td>comp <@showNotes notesFieldName comparison /></td>
		<td>input <@editableNotes notesFieldName /></td>
	</tr>
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
								<select name="${fieldName}" class="chosen" >
									<option value=""></option>
									<#list publications?keys as publicationQname>
										<option value="${publicationQname}" <#if same(publication.qname, publicationQname)>selected="selected"</#if> >${publications[publicationQname].citation}</option>
									</#list>
								</select>
							</td>
						</tr>
					</#list>
					</#if>
					<tr>
						<td>
							<select name="${fieldName}" class="chosen" data-placeholder="Valitse julkaisu" >
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
						<td><input type="text" name="newIucnPublicationCitation" id="createNewIucnPublicationCitationInput" placeholder="Kirjoita lähdeviite, for example 'Stubbs & Drake 2001, Stuke 2003' or 'Kasviatlas 2008'"/></td>
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
			<#if property.literalProperty>
				${value}
			<#else>
				${property.range.getValueFor(value).label.forLocale("fi")}
			</#if>
			<#if value_has_next>, </#if>
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
			<button class="closeNoteEditButton">Sulje</button>
		</div>
	</#if>
</#macro>

<script>
$(function() {
	
	$(".notes textarea").each(function() {
		updateNotes($(this));
	});
	
	$(".notes textarea").on('change', function() {
		$(this).closest('.notes').fadeOut('fast', updateNotes( $(this) ));
	});
	
	$(".noteViewer").tooltip();
	
	$(".closeNoteEditButton").on('click', function() {
		$(this).closest('.notes').find('textarea').trigger("change");
	});

});

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