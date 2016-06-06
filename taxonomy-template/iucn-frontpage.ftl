<#include "luomus-header.ftl">
<#include "iucn-macro.ftl">

<h1>Uhanalaisuusarviointi - ${selectedYear} <#if draftYear == selectedYear>(LUONNOS)</#if></h1>

<@toolbox/>		
		
<table class="resourceListTable">
	<thead>
		<tr>
			<th>Eliöryhmä</th>
			<th>Tila</th>
			<th>Uhanalaisuusarvioijat</th>
			<#if user.isAdmin??><th></th></#if>
		</tr>
	</thead>
	<tbody>
		<#list taxonGroups?values as taxonGroup>
		<tr>
			<td>
				<a href="${baseURL}/iucn/group/${taxonGroup.qname}/${selectedYear}">
					${taxonGroup.name.forLocale("fi")!""}
				</a>
			</td>
			<td class="taxonGroupStat" id="${taxonGroup.qname}">
				<@loadingSpinner "" />
			</td>
			<td>
				<@editors taxonGroup.qname.toString() />
			</td>
			<#if user.isAdmin??><td><a class="button" href="${baseURL}/iucn/editors/${taxonGroup.qname}">Modify editors (admin only)</a></td></#if>
		</tr>
		</#list>
	</tbody>
</table>

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