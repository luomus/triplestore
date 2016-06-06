<#include "luomus-header.ftl">
<#include "iucn-macro.ftl">

<#list target.groups as groupQname>
	<a href="${baseURL}/iucn/group/${groupQname}/${selectedYear}" class="goBack">
		Takaisin
		<#if (target.groups?size > 1)>
			(${taxonGroups[groupQname].name.forLocale("fi")!""})
		</#if>
	</a>
</#list>

<h1>Uhanalaisuusarviointi - ${selectedYear} <#if draftYear == selectedYear>(LUONNOS)</#if></h1>

<@toolbox/>		

<#macro tree taxon>
	<#if taxon.hasParent()>
		<@tree taxon.parent />
	</#if>
	<li><@printScientificNameAndAuthor taxon /></li>
</#macro>

<ul class="taxonTree">
	<@tree taxon/>
</ul>

<#include "luomus-footer.ftl">