<#include "luomus-header.ftl">

<#if successMessage?has_content>
	<p class="successMessage">${successMessage}</p>
</#if>

<h1>Create and modify publications</h1>

<p><a href="${baseURL}/publications/add">&raquo; Create new publicaton</a></p>

<div id="publications">

<input class="search" placeholder="Rajaa julkaisuluetteloa" />

<table class="resourceListTable">
	<tbody class="list">
	<#list publications?values as publication>
		<tr>
			<td><a href="${baseURL}/publications/${publication.qname}">${publication.qname}</a></td>
			<td class="citation">
				${publication.citation?html}
				<#if publication.getURI()?has_content>
					<a href="${publication.getURI()?html}">Link</a>
				</#if>
			</td>
			<td><a class="button" href="${baseURL}/publications/${publication.qname}">Modify</a></td>
		</tr>
	</#list>
	</tbody>
</table>
</div>

<script>
$(function() {
	new List('publications', { valueNames: [ 'citation' ] });
});
</script>

<#include "luomus-footer.ftl">