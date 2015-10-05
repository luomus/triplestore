<#include "luomus-header.ftl">

<@editorTools />

<h1>Information Systems</h1>

<ul class="servicelist">
	<li><a href="${baseURL}/it?view=sixfold">Sixfold</a></li>
	<li><a href="${baseURL}/it?view=dependency-graph">Dependency graph</a></li>
</ul>



<p>Active total: <span style="font-size: 150%">${activeCount}</span></p>

<table>
	<caption>Legend TODO used types dynamically in order from common to least common; labels for types</caption>
	<tr>
		<td>
			<div class="system webApplication"> Web application </div>
			<div class="system webService"> HTTP API </div>
			<div class="system softwareComponent"> Component </div>
			<div class="system program"> Stand-alone program </div>
			<div class="system database"> Database </div>
			<div class="system hardware"> Hardware </div>
			<div class="system server"> Server </div>
			<div class="system other"> Unknown!? </div>
		</td>
	</tr>
</table>

<table id="systemsTable">
	<tr>
		<td> <@printSystems "INTERNAL_PRODUCTION" "LUOMUS only - Production" /> </td>
		<td> <@printSystems "PUBLIC_PRODUCTION" "Public for all - Production" /> </td>
	</tr>
	<tr>
		<td> <@printSystems "INTERNAL_DEVELOPMENT" "LUOMUS only - Development" /> </td>
		<td> <@printSystems "PUBLIC_DEVELOPMENT" "Public for all - Development" /> </td>
	</tr>
	<tr>
		<td colspan="2">  <@printSystems "ADMIN" "Administration tools" /> </td>
	</tr>
	<tr>
		<td colspan="2"> <@printSystems "ABANDONED" "Abandoned" /> </td>
	</tr>
	<tr>
		<td colspan="2"> <@printSystems "UKNOWN" "Unknown?!" /> </td>
	</tr>
</table>>


<#macro printSystems type title>
	<#if systems[type]??>
		<h2>${title} (${systems[type]?size})</h2>
		<#list systems[type] as system>
			<div class="system <#if system.hasStatements("KE.type")>${system.getStatements("KE.type")?first.objectResource.qname?replace("KE.", "")}<#else>other</#if>">
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
										${property.range.getValueFor(statement.objectResource.qname).label.forLocale("en")}
									</div>
								</#if>
							</#if>
						</#list>
					</#if>
				</#list> 
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