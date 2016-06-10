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
				<li>${taxonProperties.getProperty("MX.hasAdminStatus").range.getValueFor(adminStatus).label.forLocale("fi")}</li>				
			</#list>
		</ul>
	</div>
</#if>

<div class="clear"></div>

<form id="evaluationEditForm" action="${baseURL}/iucn/species/${taxon.qname}/${selectedYear}" method="post">
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
	
	<@iucnSection "Taksonomia <span>- Huom: Vakinaisuus ja sen kommentti on julkinen tieto</span>" /> 
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

<#macro iucnSection title>
	<tr class="section">
		<th>&nbsp;</th>
		<th colspan="2">${title}</th>
	</tr>
</#macro>

<#macro iucnInput fieldName notesFieldName="NONE">
	<#assign field = properties[fieldName] />
	<tr>
		<th>${field.label.forLocale("fi")!fieldName}</th>
		<td>comvalue</td>
		<td>input ${notesFieldName}</td>
	</tr>
</#macro>

<#macro iucnTextarea fieldName>
	<#assign field = properties[fieldName]/>
	<tr>
		<th>${field.label.forLocale("fi")!fieldName}</th>
		<td>comvalue</td>
		<td><textarea name="fieldName">value</textarea></td>
	</tr>
</#macro>

<#macro iucnMinMax title fieldNameMin fieldNameMax notesFieldName="NONE">
	<#assign minField = properties[fieldNameMin]/>
	<#assign maxField = properties[fieldNameMax]/>
	<tr>
		<th>${title}</th>
		<td>comvalue - comvalue</td>
		<td>input - input ${notesFieldName}</td>
	</tr>
</#macro>

<#macro iucnOccurrence areaQname>
	<tr>
		<th>${areas[areaQname].name.forLocale("fi")}</th>
		<td>comvalue</td>
		<td>input</td>
	</tr>
</#macro>

<#macro iucnHabitatPair fieldName notesFieldName="NONE">
	<#assign field = properties[fieldName]/>
	<tr>
		<th>${field.label.forLocale("fi")!fieldName}</th>
		<td>comvalue</td>
		<td>input ${notesFieldName}</td>
	</tr>
</#macro>

<#macro iucnPublications fieldName>
	<#assign field = properties[fieldName]/>
	<tr>
		<th>${field.label.forLocale("fi")!fieldName}</th>
		<td>comvalue</td>
		<td>input</td>
	</tr>
</#macro>

<#include "luomus-footer.ftl">