<#include "iucn-macro.ftl">
<#list targets as target><#if target.hasEvaluation(selectedYear)><#assign evaluation = target.getEvaluation(selectedYear)>
	${target.orderAndFamily},
	${target.scientificName!target.qname},
	${target.synonymNames},
	${target.vernacularNameFi!""},
	<#if evaluation.ready>Valmis<#else>Kesken</#if>,
	${(evaluation.lastModified?string("d.M.yyyy"))!"--"},
	<#if evaluation.lastModifiedBy??>${persons[evaluation.lastModifiedBy].fullname}<#else>--</#if>,
	<#if evaluation.hasIucnStatus()>${evaluation.iucnStatus?replace("MX.iucn","")} ${evaluation.externalImpact}<#else>--</#if>,
	<#if evaluation.hasIucnStatus()>
		<#if evaluation.hasCorrectedStatusForRedListIndex()>
					${evaluation.calculatedCorrectedRedListIndex!""} (${evaluation.correctedStatusForRedListIndex?replace("MX.iucn", "")}) [KORJATTU]
				<#else>
					${evaluation.calculatedRedListIndex!"-"} (${evaluation.iucnStatus?replace("MX.iucn", "")})
				</#if>
			<#else>
				--
	</#if>,
	<#if target.hasPreviousEvaluation(selectedYear)>
		<#assign prevEvaluation = target.getPreviousEvaluation(selectedYear)>
		<#if prevEvaluation.hasIucnStatus()>
			${prevEvaluation.iucnStatus?replace("MX.iucn", "")}  ${prevEvaluation.externalImpact}
			(${prevEvaluation.evaluationYear})
		<#else>
			--
		</#if>
	<#else>
		--
	</#if>,
	<#if target.hasEvaluation(selectedYear)>
		<#if target.getEvaluation(selectedYear).hasRemarks()>
			${target.getEvaluation(selectedYear).remarks?replace("\n","")?replace("\r","")}
		</#if> 
	</#if>,
	joo
</#if><#t></#list>
