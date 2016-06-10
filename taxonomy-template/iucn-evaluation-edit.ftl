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
				<li>${properties["MX.hasAdminStatus"].range.getValueFor(adminStatus).label.forLocale("fi")}</li>				
			</#list>
		</ul>
	</div>
</#if>

<div class="clear"></div>

<form id="evaluationEditForm" action="${baseURL}/iucn/species/${taxon.qname}/${selectedYear}" method="post" onsubmit="return false;">
<input type="hidden" name="evaluationId" value="${(evaluation.id)!""}" />
<input type="hidden" name="MKV.evaluatedTaxon" value="${taxon.qname}" />
<input type="hidden" name="MKV.evaluationYear" value="${selectedYear}" />

<table class="resourceListTable evaluationEdit">
	<thead>
		<tr>
			<th>Muuttuja</th>
			<th>${(comparison.year)!"Ei edellisiä tietoja"}</th>
			<th>${selectedYear}</th>
		</tr>
	</thead>
	<tbody>
	
	<@iucnSection "Taksonomia <span>- Huom: Esiintymisen tila ja sen kommentti on julkinen tieto</span>" /> 
	<@iucnInput "MX.typeOfOccurrenceInFinland" "MX.typeOfOccurrenceInFinlandNotes" />
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
	
	</tbody>
</table>

</form>

<#macro iucnSection title>
	<tr class="section">
		<th>&nbsp;</th>
		<th colspan="2">${title}</th>
	</tr>
</#macro>

<#macro iucnInputField fieldName>
	input
</#macro>

<#macro iucnInput fieldName notesFieldName="NONE">
	<#assign field = properties[fieldName] />
	<tr>
		<th>${field.label.forLocale("fi")!fieldName}</th>
		<td><@comparisonValue fieldName /> <@comparisonNotes notesFieldName /></td>
		<td><@iucnInputField fieldName /> <@notes notesFieldName /></td>
	</tr>
</#macro>

<#macro iucnTextarea fieldName>
	<#assign field = properties[fieldName]/>
	<tr>
		<th>${field.label.forLocale("fi")!fieldName}</th>
		<td><@comparisonValue fieldName /></td>
		<td><textarea name="${fieldName}"><#if evaluation??>${evaluation.getValue(fieldName)}</#if></textarea></td>
	</tr>
</#macro>

<#macro iucnMinMax title fieldNameMin fieldNameMax notesFieldName="NONE">
	<#assign minField = properties[fieldNameMin] />
	<#assign maxField = properties[fieldNameMax] />
	<tr>
		<th>${title}</th>
		<td><#if comparison??><@comparisonValue minField /> - <@comparisonValue maxField /> <@comparisonNotes notesFieldName /></#if></td>
		<td><@iucnInputField minField /> - <@iucnInputField maxField /> <@notes notesFieldName /></td>
	</tr>
</#macro>

<#macro iucnOccurrence areaQname>
	<tr>
		<th>${areas[areaQname].name.forLocale("fi")}</th>
		<td>comp</td>
		<td>input</td>
	</tr>
</#macro>

<#macro iucnHabitatPair fieldName notesFieldName="NONE">
	<#assign field = properties[fieldName]/>
	<tr>
		<th>${field.label.forLocale("fi")!fieldName}</th>
		<td>comp <@comparisonNotes notesFieldName /></td>
		<td>input <@notes notesFieldName /></td>
	</tr>
</#macro>

<#macro iucnPublications fieldName>
	<#assign field = properties[fieldName]/>
	<tr>
		<th>${field.label.forLocale("fi")!fieldName}</th>
		<td><@comparisonValue fieldName /></td>
		<td>
			<label>Valitse julkaisu</label>
			<#if evaluation??>
			<#list evaluation.getValues(fieldName) as publication>
				<select name="${fieldName}" class="chosen" >
					<option value=""></option>
					<#list publications?keys as publicationQname>
						<option value="${publicationQname}" <#if same(publication.qname, publicationQname)>selected="selected"</#if> >${publications[publicationQname].citation}</option>
					</#list>
				</select>
			</#list>
			</#if>
			<select name="${fieldName}" class="chosen" >
				<option value=""></option>
				<#list publications?keys as publicationQname>
					<option value="${publicationQname}">${publications[publicationQname].citation}</option>
				</#list>
			</select>
			<label>Tai luo uusi julkaisu</label>
			<input type="text" name="newPublicationCitation" id="createNewPublicationInput" placeholder="Type citaction, for example 'Stubbs & Drake 2001, Stuke 2003' or 'Kasviatlas 2008'"/>
		</td>
	</tr>
</#macro>

<#macro comparisonValue fieldName>
	<#if comparison??>
		<#list comparison.getValues(fieldName) as value>
			${value}
			<#if statement_has_next>, </#if>
		</#list>
	</#if>
</#macro>

<#macro comparisonNotes notesFieldName>
	<#if comparison?? && notesFieldName != "NONE">
		<div class="noteViewer"><span class="ui-icon ui-icon-comment" title="${comparison.getValue(notesFieldName)}"></span></div>
	</#if>
</#macro>

<#macro notes notesFieldName>
	<#if notesFieldName != "NONE">
		<div class="notes hidden">
			<p><label>${properties[notesFieldName].label.forLocale("fi")!notesFieldName}</label></p>
			<textarea name="${notesFieldName}"><#if evaluation??>${evaluation.getValue(notesFieldName)}</#if></textarea>
			<button class="closeNoteEditButton">Sulje kommentti</button>
		</div>
	</#if>
</#macro>

<script>
$(function() {
	
	$(".notes textarea").each(function() {
		updateNotes($(this));
	});
	
	$(".notes textarea").on('change', function() {
		$(this).closest('.notes').fadeOut('slow', updateNotes( $(this) ));
	});
	
	$(".noteViewer").tooltip();
	
	$(".closeNoteEditButton").on('click', function() {
		$(this).closest('.notes').find('textarea').trigger("change");
	});

});


function updateNotes(noteInput) {
	noteInput.closest('td').find('.noteViewer').remove();
	var noteViewerContent = '<div class="noteViewer noteViewerEditable">';
	if (noteInput.val()) {
		noteViewerContent += '<span class="ui-icon ui-icon-comment" title="'+noteInput.val()+'"></span>';
	} else {
		noteViewerContent += '<a href="#">kommentoi</a>';
	}
	noteViewerContent += '</div>'
	var noteViewer = $(noteViewerContent);
	noteInput.closest('td').append(noteViewer);
	noteViewer.tooltip();
	noteViewer.on('click', function() {
		var notesContainer = $(this).closest('td').find('.notes').first();
		$(this).fadeOut('slow', function() {
			notesContainer.fadeIn('slow');
		});
		return false;
	});
}

</script>

<#include "luomus-footer.ftl">