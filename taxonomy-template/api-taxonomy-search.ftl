<#if response?? && response.rootNode.hasChildNodes()>
	<#list response.rootNode.childNodes as matches>
		<h3>${text[matches.name]}</h3>
		<#list matches.childNodes as match>
			<p>
				<a href="${taxonpageBaseLinkURL}/${match.name}<#if taxonpageURLPostfix?has_content>${taxonpageURLPostfix}</#if>">${match.getAttribute("matchingName")}</a>
				<#if match.hasAttribute("scientificName")>
					&nbsp; - &nbsp; ${match.getAttribute("scientificName")}<#if match.hasAttribute("scientificNameAuthorship")>,</#if>
				</#if>
				<#if match.hasAttribute("scientificNameAuthorship")>
					${match.getAttribute("scientificNameAuthorship")}
				</#if> 
				[<#if match.hasAttribute("taxonRank")>${match.getAttribute("taxonRank")?replace("MX.", "")}<#else>unranked</#if>]
				<#if match.hasChildNodes("informalGroups")>
					&nbsp; - &nbsp;
					<#list match.getNode("informalGroups").getChildNodes() as group>
						${group.getAttribute("en")}<#if group_has_next>, </#if>
					</#list>
				</#if>
			</p>
		</#list>
	</#list>
<#else>
	<p>No results!</p>
</#if>