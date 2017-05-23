<#include "luomus-header.ftl">
<#include "macro.ftl">

<@editorTools />

<h1>Information Systems</h1>

<ul class="servicelist">
	<li><a href="${baseURL}/it?view=sixfold">Sixfold</a></li>
	<li><a href="${baseURL}/it?view=dependency-graph">Dependency graph</a></li>
</ul>



<p>Active total: <span style="font-size: 150%">${activeCount}</span></p>

<table id="systemsTable">
	<tr>
		<td colspan="2"> <@printSystems "INTERNAL_PRODUCTION" "LUOMUS only - Production" /> </td>
		<td colspan="2"> <@printSystems "PUBLIC_PRODUCTION" "Public for all - Production" /> </td>
	</tr>
	<tr>
		<td colspan="2"> <@printSystems "INTERNAL_DEVELOPMENT" "LUOMUS only - Development" /> </td>
		<td colspan="2"> <@printSystems "PUBLIC_DEVELOPMENT" "Public for all - Development" /> </td>
	</tr>
	<tr>
		<td colspan="3">  <@printSystems "ADMIN_PRODUCTION" "Administration tools - Production" /> </td>
		<td colspan="1">  <@printSystems "ADMIN_DEVELOPMENT" "Administration tools - Development" /> </td>
	</tr>
	<tr>
		<td colspan="4"> <@printSystems "ABANDONED" "Abandoned" /> </td>
	</tr>
	<tr>
		<td colspan="4"> <@printSystems "UKNOWN" "Unknown?!" /> </td>
	</tr>
	<tr style="opacity: 0.0">
		<td>1</td> <td>2</td> <td>3</td> <td>4</td>
	</tr>
</table>

<#macro printSystems type title>
	<#if systems[type]??>
		<h2>${title} (${systems[type]?size})</h2>
		<#list systems[type] as system>
			<#assign itSystemType = "" />
			<#if system.hasStatements("KE.type")>
				<#assign itSystemType = system.getStatements("KE.type")?first.objectResource.qname />
			</#if>
			<div class="system <#if itSystemType?has_content>${itSystemType?replace("KE.", "")}<#else>other</#if>">
				<a class="edit" href="${baseURL}/editor/${system.subject.qname}">Edit</a>
				
				<#if (system.developmentStatus!"") == "UNDER_ACTIVE_DEVELOPMENT">
					<img class="icon" src="/InformationSystemsViewer/static/gears.gif" title="Under active development" alt="Under active development" />
				</#if>
				
				<#if system.documentLink??><div class="info"><a href="${system.documentLink}" class="documentation"><img src="/InformationSystemsViewer/static/doc.png" alt="DOC" /></a></div></#if>
				<#list properties.allProperties as property>
					<#if system.hasStatements(property.qname)>
						<#list system.getStatements(property.qname) as statement>
							<#if statement.predicate.qname == "KE.name">
								<h3>${statement.objectLiteral.content} - ${system.subject.qname}</h3>
							<#else>
								<#if statement.literalStatement>
									<div class="hidden infofield">
										<label>${property.label.forLocale("en")!property.qname}</label>
										<#if statement.objectLiteral.content?starts_with("http")>
											<a href="${statement.objectLiteral.content}" target="_blank"> ${statement.objectLiteral.content} </a>
										<#else>
											${statement.objectLiteral.content}
										</#if>
									</div>
								<#elseif property.hasRange() && property.range.qname.toString() == "MA.person">
									<div class="hidden infofield">
										<label>${property.label.forLocale("en")!property.qname}</label>
										<#if property.range.hasValue(statement.objectResource.qname)>
											${property.range.getValueFor(statement.objectResource.qname).label.forLocale("en")}
										<#else>
											${statement.objectResource.qname}
										</#if>
									</div>
								</#if>
							</#if>
						</#list>
					</#if>
				</#list>
				<#if itSystemType?has_content>
					<span class="systemType">[${properties.getProperty("KE.type").range.getValueFor(itSystemType).label.forLocale("en")}]</span>
				</#if>
			</div>
		</#list>
	<#else>
		<h2>${title} (0)</h2>
		No systems.
	</#if>
</#macro>


<script>
$(function() {
	$(".system").on('click', function() {
		$(".system").not(this).find('.infofield').hide(100);
		$(this).find('.infofield').show(200);
	});
});
</script>

<div style="height: 400px;"></div>
<#include "luomus-footer.ftl">