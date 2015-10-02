<#include "luomus-header.ftl">

<h1>Information Systems</h1>

<ul class="servicelist">
	<li><a href="${baseURL}/it?view=sixfold">Sixfold</a></li>
	<li><a href="${baseURL}/it?view=dependency-graph">Dependency graph</a></li>
</ul>



<p>Total: ${count}</p>

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
			<div class="system other"> Other </div>
		</td>
	</tr>

<table id="systemsTable">
<tbody>
<tr>
<td>
	<div>
		<h2>Internal - Production</h2>
		<@printSystems "INTERNAL_PRODUCTION" />
	</div>
</td>
<td>
	<div>
		<h2>Public - Production</h2>
		<@printSystems "PUBLIC_PRODUCTION" />
	</div>
</td>
</tr>
<tr>
<td>
	<div>
		<h2>Internal - Development</h2>
		<@printSystems "INTERNAL_DEVELOPMENT" />
	</div>
</td>
<td>
	<div>
		<h2>Public - Development</h2>
		<@printSystems "PUBLIC_DEVELOPMENT" />
	</div>
</td>
</tr>
<tr>
<td colspan="2">
	<div>
		<h2>Administration tools</h2>
		<@printSystems "ADMIN" />
	</div>
</td>
</tr>
<tr>
<td colspan="2">
	<div>
		<h2>Abandoned</h2>
		<@printSystems "ABANDONED" />
	</div>
</td>
</tr>
<tr>
<td colspan="2">
	<div>
		<h2>Unknown?!</h2>
		<@printSystems "UKNOWN" />
	</div>
</td>
</tr>
</tbody>
</table>


<#macro printSystems type>
	<#if systems[type]??>
		<div class="count">${systems[type]?size}</div>
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
							<#if statement.literalStatement>
								<label>${property.label.forLocale("en")!property.qname}</label>
									<#if statement.objectLiteral.content?starts_with("http")>
										<a href="${statement.objectLiteral.content}" target="_blank">
											${statement.objectLiteral.content}
										</a>
									<#else>
										${statement.objectLiteral.content}
									</#if>
								<br />
							<#elseif property.hasRange() && property.range.qname.toString() == "MA.person">
								<label>${property.label.forLocale("en")!property.qname}</label>
								${property.range.getValueFor(statement.objectResource.qname).label.forLocale("en")}
								<br />
							</#if>
						</#list>
					</#if>
				</#list> 
			</div>
		</#list>
	<#else>
		No systems.
	</#if>
</#macro>



<#include "luomus-footer.ftl">