<#if stat.speciesOfGroupCount != 0>
	<#assign percent = (stat.readyCount/stat.speciesOfGroupCount*100)?floor>
<#else>
	<#assign percent = 0>
</#if>
<span class="stat ready" title="Valmiina">${stat.readyCount}</span>
<span class="stat started" title="Aloitettuna">(${stat.startedCount})</span>  
/
<span class="stat <#if (percent>=100)>ready<#else>total</#if>" title="Lajeja yhteensä">${stat.speciesOfGroupCount}</span>
<span class="stat <#if (percent>=100)>ready<#else>total</#if>" title="Prosenttia valmiina">${percent}%</span>

