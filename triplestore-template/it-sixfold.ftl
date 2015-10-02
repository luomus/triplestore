<#include "luomus-header.ftl">

<h1>Information Systems</h1>

<ul class="servicelist">
	<li><a href="${baseURL}/it?view=sixfold">Sixfold</li>
	<li><a href="${baseURL}/it?view=dependency-graph">Dependency graph</li>
</ul>


<#macro printSystems type>
	<#if systems[type]??>
		<div class="count">${systems[type]?size}</div>
		<#list systems[type] as system>
			<div class="system ${system.type.className}">
				<#if (system.developmentStatus!"") == "UNDER_ACTIVE_DEVELOPMENT">
					<img class="icon" src="/InformationSystemsViewer/static/gears.gif" title="Under active development" alt="Under active development" />
				</#if>
				<#if system.uri??>
					<h3><a href="${system.uri}">${system.name}</a></h3>
				<#else>
					<h3>${system.name!""}</h3>
				</#if>
				<#if system.descriptions["en"]??>
					<p>${system.descriptions["en"]}</p>
				<#else>
					<#if system.descriptions["undefined"]??>
						<p>${system.descriptions["undefined"]}</p>
					<#else>
						<#if system.descriptions["fi"]??><p>${system.descriptions["fi"]}</p></#if>
					</#if>
				</#if>
				
				<div class="info"><a class="edit" href="/triplestore/edit/${system.idURI}">edit</a></div>
				<div class="info"><#list system.persons as person>${persons[person]!""}<#if person_has_next>, </#if></#list></div>
				<#if system.documentLink??><div class="info"><a href="${system.documentLink}" class="documentation"><img src="/InformationSystemsViewer/static/doc.png" alt="DOC" /></a></div></#if>
			</div>
		</#list>
	<#else>
		No systems.
	</#if>
</#macro>

<p>Total: ${count}</p>

<table>
	<caption>Legend TODO labels for types</caption>
	<tr>
		<td>
			<div class="system webApplication"> webApplication </div>
			<div class="system webService"> webAPI </div>
			<div class="system softwareComponent"> softwareComponent </div>
			<div class="system program"> program </div>
			<div class="system database"> database </div>
			<div class="system hardware"> hardware </div>
			<div class="system server"> server TODO </div>
			<div class="system other"> other </div>
		</td>
	</tr>

PUBLIC_DEVELOPMENT, PUBLIC_PRODUCTION, INTERNAL_DEVELOPMENT, INTERNAL_PRODUCTION, ADMIN, ABANDONED, UKNOWN

<table id="systemsTable">
<tbody>
<tr>
<td>
	<div>
		<h2>Internal - Production</h2>
		<@printSystems "INTERNAL-PRODUCTION" />
	</div>
</td>
<td>
	<div>
		<h2>Public - Production</h2>
		<@printSystems "PUBLIC-PRODUCTION" />
	</div>
</td>
</tr>
<tr>
<td>
	<div>
		<h2>Internal - Development</h2>
		<@printSystems "INTERNAL-DEVELOPMENT" />
	</div>
</td>
<td>
	<div>
		<h2>Public - Development</h2>
		<@printSystems "PUBLIC-DEVELOPMENT" />
	</div>
</td>
</tr>
<tr>
<td colspan="2">
	<div>
		<h2>IT Administration Tools</h2>
		<@printSystems "ADMIN" />
	</div>
</td>
</tr>
<tr>
<td colspan="2">
	<div>
		<h2>Others</h2>
		<@printSystems "OTHERS" />
	</div>
</td>
</tr>
<tr>
<td colspan="2">
	<div>
		<h2>Retired</h2>
		<@printSystems "ABANDONED" />
	</div>
</td>
</tr>
</tbody>
</table>

<#include "luomus-footer.ftl">