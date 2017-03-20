<#include "macro.ftl">

<@showValidationResults />

<#if addedPublication??>
	<script>addedPublication('${addedPublication.qname}', '${addedPublication.citation?html}');</script>
</#if>
<#if addedOccurrenceInFinlandPublication??>
	<script>addedOccurrenceInFinlandPublication('${addedOccurrenceInFinlandPublication.qname}', '${addedOccurrenceInFinlandPublication.citation?html}');</script>
</#if>