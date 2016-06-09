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
	
	<@section "Taksonomia - Huom: Vakinaisuus on julkinen tieto" />
	<@input "MX.typeOfOccurrenceInFinland" "MX.typeOfOccurrenceInFinlandNotes" />
	<@textarea "MKV.taxonomicNotes" />
	
	<@section "Luokka" />	
	<@input "MKV.redListStatus" "MKV.redListStatusNotes" />
	<@input "MKV.redListIndexCorrection" "MKV.redListIndexCorrectionNotes" />
	<@textarea "MKV.redListStatusAccuracyNotes" />
	<@input "MKV.reasonForStatusChange" "MKV.reasonForStatusChangeNotes" />
	<@minMax "Vaihteluväli" "MKV.redListStatusMin" "MKV.redListStatusMax" />
			
	<@section "Kriteerit" />
	<@input "MKV.criteriaA" "MKV.criteriaANotes" />
	<@input "MKV.criteriaB" "MKV.criteriaBNotes" />
	<@input "MKV.criteriaC" "MKV.criteriaCNotes" />
	<@input "MKV.criteriaD" "MKV.criteriaDNotes" />
	<@input "MKV.criteriaE" "MKV.criteriaENotes" />
	
	<@section "Esiintymisalueet" />
	<@occurence "ML.690" />
	<@occurence "ML.691" />
	<@occurence "ML.692" />
	<@occurence "ML.693" />
	<@occurence "ML.694" />
	<@occurence "ML.695" />
	<@occurence "ML.696" />
	<@occurence "ML.697" />
	<@occurence "ML.698" />
	<@occurence "ML.699" />
	<@occurence "ML.670" />
	<@textarea "MKV.occurrenceNotes" />
	<@minMax "Esiintyminen lkm" "MKV.countOfOccurrencesMin" "MKV.countOfOccurrencesMax" "MKV.countOfOccurrencesNotes" />
	<@minMax "Esiintymiä alussa/lopussa" MKV.countOfOccurrencesPeriodBegining" "MKV.countOfOccurrencesPeriodEnd" "MKV.countOfOccurrencesPeriodNotes" />
	<@input "MKV.decreaseDuringPeriod" "MKV.decreaseDuringPeriodNotes" />
	<@input "MKV.decreaseDuringPeriod" "MKV.decreaseDuringPeriodNotes" />
	<@input "MKV.borderGain" "MKV.borderGainNotes" />
	<@minMax "Levinneisyysalueen koko" "MKV.distributionAreaMin" "MKV.distributionAreaMax" "MKV.distributionAreaNotes" />
	
	<@section "Elinympäristö" />   
	<@habitatPair "MKV.primaryHabitat" "MKV.habitatNotes" />
	<@habitatPair "MKV.secondaryHabitat" />   
	<@input "MKV.fragmentedHabitats" "MKV.fragmentedHabitatsNotes" />
   
	<@section "Kanta" />
	<@minMax "Yksilömäärä" "MKV.individualCountMin" "MKV.individualCountMax" "MKV.individualCountNotes" />
	<@input "MKV.generationAge" "MKV.generationAgeNotes" />
	<@input "MKV.evaluationPeriodLength" "MKV.evaluationPeriodLengthNotes" />
	<@input "MKV.populationVaries" "MKV.populationVariesNotes" />
	
	<@section "Uhanalaisuus" />
	<@input "MKV.endangermentReason" "MKV.endangermentReasonNotes" />
	<@textarea "MKV.actionNotes" />   
	<@input "MKV.lsaRecommendation" "MKV.lsaRecommendationNotes" />
	<@input "MKV.possiblyRE" />
	<@textarea "MKV.lastSightingNotes" />

	<@publications "MKV.publication" />   
	
	</tbody>
</table>

<#include "luomus-footer.ftl">