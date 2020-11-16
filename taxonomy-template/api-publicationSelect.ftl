<#if fieldName == "originalPublication">
		<select id="originalPublicationSelector" name="MX.originalPublication" class="chosen" data-placeholder="Select publication" multiple="multiple">
			<option value=""></option>
			<#list publications?keys as publicationQname>
				<option value="${publicationQname}" <#if taxon.hasExplicitlySetOriginalPublication(publicationQname)>selected="selected"</#if> >${publications[publicationQname].citation}</option>
			</#list>
		</select>
</#if>
<#if fieldName == "occurrenceInFinlandPublication">
		<select id="occurrenceInFinlandPublicationSelector" name="MX.occurrenceInFinlandPublication" multiple="multiple" data-placeholder="Select publication" class="chosen">
			<option value=""></option>
			<#list publications?keys as publicationQname>
				<option value="${publicationQname}" <#if taxon.hasExplicitlySetOccurrenceInFinlandPublication(publicationQname)>selected="selected"</#if> >${publications[publicationQname].citation}</option>
			</#list>
		</select>
</#if>
<#if fieldName == "originalDescription">
		<select id="originalDescriptionSelector" name="MX.originalDescription" class="chosen" data-placeholder="Select publication">
			<option value=""></option>
			<#list publications?keys as publicationQname>
				<option value="${publicationQname}" <#if taxon.hasOriginalDescription(publicationQname)>selected="selected"</#if> >${publications[publicationQname].citation}</option>
			</#list>
		</select>
</#if>