<#if error??>Error: ${error}<#else>
	<#include "macro.ftl">
	<tr>
		<td style="display: none;">-1</td>
		<td>
			<a class="predicate" href="${baseURL}/editor/${property.qname}">${property.qname}</a><br />
			<span class="label"><#if property.label??>${(property.label.forLocale("fi")!property.label.forLocale("en")!"")?html}</#if></span>
		</td>
		<#-- -- LITERALS -- -->
		<#if property.isLiteralProperty()>
			<td>
				<span class="literal">literal</span>
				<#if property.hasRange()>
					<a href="${baseURL}/editor/${property.range.qname}">${property.range.qname}</a>
				<#else>
					<span class="error">rdfs:range NOT DEFINED!</span>
				</#if>
			</td>
			<td>
				<#if property.isBooleanProperty()>
					<@booleanValue "" "" />
				<#else>
					<#if property.hasRange()>
						<@literalValue "" "" property.range.qname.toString() />
					<#else>
						<@literalValue "" "" />
					</#if>
				</#if>
			</td>	
		<#-- --RESOURCES -- -->	
		<#else> 
			<td>
				<span class="resource">resource</span> 
				<#if property.hasRange()>
					<a href="${baseURL}/editor/${property.range.qname}">${property.range.qname}</a>
				</#if>
			</td>
			<td>
				<#if property.hasRangeValues()>
					<@resourceValueRangeSelect "" property.range.values />
				<#else>
					<@resourceValue "" />
				</#if>
			</td>
		</#if>
		<td><button class="deleteButton">Delete</button></td>
	</tr>
</#if>