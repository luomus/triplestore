<#include "iucn-macro.ftl">

<p class="info">Huomaa, että kun käytät tätä pikatoimintoa, mitään tietoja ei kopioida edellisestä arvioinnista!</p>
	
<table>
	<tr class="primaryHabitatRow">
		<th><@iucnLabel "MKV.primaryHabitat" /></th>
		<td>
			<#if taxon.primaryHabitat??>
				<@editableHabitatPair "MKV.primaryHabitat" taxon.primaryHabitat />
			</#if>
		</td>
	</tr>
</table>
<p><button>Merkitse LC-luokkaan</button></td></p>
<@propertyCommentsScript />