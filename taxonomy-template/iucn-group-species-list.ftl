<#include "luomus-header.ftl">
<#include "iucn-macro.ftl">

<h1>Uhanalaisuusarviointi - ${selectedYear} <#if draftYear == selectedYear>(LUONNOS)</#if></h1>
<@toolbox/>

<h2>${group.name.forLocale("fi")!""}</h2>
<p class="info">Uhanalaisuusarviojat: <@editors group /></p>


<table class="iucnSpeciesTable">
	<thead>
		<th>Tieteellinen nimi</th>
		<th>Suomenkielinen nimi</th>
		<th>Tila</th>
		<th>Muokattu</th>
		<th>Muokkaaja</th>
		<th>Luokka</th>
		<th>Indeksi</th>
	</thead>
	<tbody>
		<#list yearData.species as species>
			<tr class="iucnTaxonRow" id ="${species.qname}">
				<td>
					<a href="${baseURL}/iucn/species/${species.qname}">
						<span class="scientificName speciesName">${species.scientificName!species.qname}</span>
					</a>
				</td>
				<td>
					<a href="${baseURL}/iucn/species/${species.qname}">
						${species.vernacularNameFi!""}
					</a>
				</td>
				<#if yearData.getEvaluation(species.qname)??>
					<#assign evaluationData = yearData.getEvaluation(species.qname)>
					<td>
						<#if evaluationData.ready>
							<span class="state ready">Valmis</span>
						<#else>
							<span class="state started">Aloitettu</span>
						</#if>
					</td>
					<td>${evaluationData.lastModified!"-"}</td>
					<td>${evaluationData.lastModifiedBy!"-"}</td>
					<td>${evaluationData.iucnClass!"-"}</td>
					<td>
						<#if evaluationData.hasIucnClass()>
							<#if evaluationData.hasCorrectedIndex()>
								${evaluationData.correctedIucnIndex} [KORJATTU]
							<#else>
								${evaluationData.calculatedIucnIndex} [laskettu]
							</#if>
						<#else>
							-
						</#if>
					</td>
				<#else>
					<td><span class="state notStarted">Ei aloitettu</span> <button>NE</button></td>
					<td>-</td>
					<td>-</td>
					<td>-</td>
					<td>-</td>
				</#if>
			</tr>
		</#list>
	</tbody>
</table>

<p class="info">Voit siirtyä katselemaan lajia klikkaamalla lajin nimeä.</p>

<#include "luomus-footer.ftl">