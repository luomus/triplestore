<#include "luomus-header.ftl">
<#include "iucn-macro.ftl">

<h1>Uhanalaisuusarviointi - ${selectedYear} <#if draftYear == selectedYear>(LUONNOS)</#if></h1>

<p><a href="${staticURL}/IUCN-käyttöohje.pdf" target="_blank">Lataa käyttöohje</a></p>

<@toolbox/>		

<div style="border: 2px dotted green; background-color: rgb(200,255,200); font-size: 22px; margin: 1em; padding: 0.5em;">
	Testidatat poistettu. Tuotantodatat latauksessa. Testailua ei enää saa tehdä keksityillä datoilla.
</div>

<table class="resourceListTable informalGroupsTable">
	<thead>
		<tr>
			<th>Eliöryhmä</th>
			<th>Tila</th>
			<th>Uhanalaisuusarvioijat</th>
			<#if user.isAdmin??><th></th></#if>
		</tr>
	</thead>
	<tbody>
		<#list taxonGroupRoots as rootQname>
			<#if (rootQname_index) % 2 == 0>
				<@printGroup taxonGroups[rootQname] 0 "odd" />
			<#else>
				<@printGroup taxonGroups[rootQname] 0 "even" />
			</#if>
		</#list>
	</tbody>
</table>

<#macro printGroup taxonGroup indent evenOdd>
	<tr class="indent_${indent} ${evenOdd}">
		<#if taxonGroupEditors[taxonGroup.qname.toString()]??>
			<td> 
				<span class="indent">&mdash;</span>
				<a href="${baseURL}/iucn/group/${taxonGroup.qname}/${selectedYear}">
					${taxonGroup.name.forLocale("fi")!""}
				</a>
			</td>
			<td class="taxonGroupStat" id="${taxonGroup.qname}">
				<@loadingSpinner "" />
			</td>
			<td class="groupEditors">
				<@editors taxonGroup.qname.toString() />
			</td>
		<#else>
			<td> 
				<span class="indent">&mdash;</span>
				${taxonGroup.name.forLocale("fi")!""}
			</td>
			<td> &nbsp; </td>
			<td> &nbsp; </td>
		</#if>
		<#if user.admin><td><a class="button" href="${baseURL}/iucn/editors/${taxonGroup.qname}">Modify editors (admin only)</a></td></#if>
	</tr>
	<#if !taxonGroupEditors[taxonGroup.qname.toString()]??>
		<#list taxonGroups?values as subGroupCandidate>
			<#if taxonGroup.hasSubGroup(subGroupCandidate.qname)>
				<@printGroup subGroupCandidate indent + 1 evenOdd />
			</#if>
		</#list>
	</#if>
</#macro>

<p class="info">Voit siirtyä arvioitavien lajien luetteloon klikkaamalla eliöryhmän nimeä.</p>

<script>
$(function() {
	$(".taxonGroupStat").each(function() {
		var groupQname = $(this).attr("id");
		var groupStatElement = $(this);
		$.get("${baseURL}/api/iucn-stat/"+groupQname+"?year=${selectedYear}", function(data) {
			groupStatElement.html(data);
		});
	});
});
</script>

<#include "luomus-footer.ftl">