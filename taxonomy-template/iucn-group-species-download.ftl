<#include "iucn-macro.ftl"><@compress single_line=true>
"Lahko, Heimo","Tieteellinen nimi","Synonyymit","Kansankieliset nimet","Taksonomiatietokannan kommentit","Arvioinnin kommentit taksonomiasta","Hallinnollinen asema","Arvioinnin tila","Muokattu","Muokkaaja",<#t>
"Luokka","RLI","Edellinen luokka","Kommentit arviosta","ARVIOINNIN TIEDOT ALKAVAT",<#t>
"Vakinaisuus","Levinneisuusalueen koko","..muistiinpanot","Esiinymisalueen koko","..muistiinpanot","Kommentit esiintymisestä",<#t>
<#list areas?keys as areaQname>
"${areas[areaQname].name.forLocale("fi")}",<#t>
</#list>
</@compress>
"Kommentit esiintymisalueista (julkinen)","Muistiinpanot esiintymisalueista",<#t>
"Ensisijainen elinympäristö","Muut elinympäristöt","Muistiinpanot elinympäristöistä","Kommentit elinympäristöstä (julkinen)",<#t>
"Sukupolvi","..muistiinpanot","Tarkastelujakson pituus","..muistiinpanot","Yksilömäärä","..muistiinpanot","Pop.koko alussa","Pop.koko lopussa","..muistiinpanot","Pop. väheneminen","..muistiinpanot",<#t>
"Kannanvaihtelut","..muistiinpanot","Pop. pirstoutunut","..muistiinpanot","Rajantakainen vahvistus","Uhanalaisuuden syyt","..muistiinpanot","Uhkatekijät","..muistiinpanot","Kommentit arvioinnin perusteista",<#t>
<#list ["A", "B", "C", "D", "E"] as criteria>
"${criteria} kriteerit","..muistiinpanot","${criteria} luokka","..muistiinpanot",<#t>
</#list>
"Kommentit kriteereistä (julkinen)","Luokka","Kriteerit","Vaihteluväli","+/-","Muutoksen syy","DD-syy",<#t>
"Mahd. hävinnyt","Viimeisin havainto","LSA ehd.",<#t>
"Luokka muistiinpanot","Kriteerit muistiinpanot","+/- muistiinpanot","Muutoksen syy muistiinpanot","DD-syy muistiinpanot","Mahd. hävinnyt muistiinpanot",<#t>
"Kommentit arv. tarkkuus/luotettavuus (julkinen)","LSA ehd. muistiinpanot","Osuus glob.pop.","..muistiinpanot",<#t>
"Julkaisut","Muut lähteet"
<#list targets as target><#if target.hasEvaluation(selectedYear)><#assign evaluation = target.getEvaluation(selectedYear)><@compress single_line=true>
"${target.orderAndFamily}",<#t>
"${target.scientificName!target.qname}",<#t>
"${target.synonymNames}",<#t>
"<#list target.taxon.vernacularName.allTexts?values as name>
${name}
</#list>
<#list target.taxon.alternativeVernacularNames.allValues as name>
${name}
</#list>",<#t>
"${target.taxon.notes!""} ${target.taxon.privateNotes!""}",<#t>
"<@showValue "MKV.taxonomicNotes" evaluation occurrenceStatuses />",<#t>
"<#list target.taxon.administrativeStatuses as adminStatus>
${properties.getProperty("MX.hasAdminStatus").range.getValueFor(adminStatus).label.forLocale("fi")}
</#list>",<#t>
"<#if evaluation.ready>Valmis<#else>Kesken</#if>",<#t>
"${(evaluation.lastModified?string("d.M.yyyy"))!"--"}",<#t>
"<#if evaluation.lastModifiedBy??>${persons[evaluation.lastModifiedBy].fullname}<#else>--</#if>",<#t>
"<#if evaluation.hasIucnStatus()>${evaluation.iucnStatus?replace("MX.iucn","")} ${evaluation.externalImpact}<#else>--</#if>",<#t>
"<#if evaluation.hasIucnStatus()>
<#if evaluation.hasCorrectedStatusForRedListIndex()>
${evaluation.calculatedCorrectedRedListIndex!""} (${evaluation.correctedStatusForRedListIndex?replace("MX.iucn", "")}) [KORJATTU]
<#else>
${evaluation.calculatedRedListIndex!"-"}
</#if>
<#else>
--
</#if>",<#t>
"<#if target.hasPreviousEvaluation(selectedYear)>
<#assign prevEvaluation = target.getPreviousEvaluation(selectedYear)>
<#if prevEvaluation.hasIucnStatus()>
${prevEvaluation.iucnStatus?replace("MX.iucn", "")}  ${prevEvaluation.externalImpact}
(${prevEvaluation.evaluationYear})
<#else>
--
</#if>
<#else>
--
</#if>",<#t>
"<#if target.hasEvaluation(selectedYear)>
<#if target.getEvaluation(selectedYear).hasRemarks()>
${target.getEvaluation(selectedYear).remarks?replace("\n","")?replace("\r","")}
</#if> 
</#if>",<#t>
" -> ",<#t>
"<@showValue "MKV.typeOfOccurrenceInFinland" evaluation occurrenceStatuses />",<#t>
"<@showValue "MKV.distributionAreaMin" evaluation /> - <@showValue "MKV.distributionAreaMax" evaluation />",<#t>
"<@showValue "MKV.distributionAreaNotes" evaluation />",<#t>
"<@showValue "MKV.occurrenceAreaMin" evaluation /> - <@showValue "MKV.occurrenceAreaMax" evaluation />",<#t>
"<@showValue "MKV.occurrenceAreaNotes" evaluation />",<#t>
"<@showValue "MKV.occurrenceNotes" evaluation />",<#t>
<#list areas?keys as areaQname>
"<#if evaluation.hasOccurrence(areaQname)>${areaStatusesDownload[evaluation.getOccurrence(areaQname).status.toString()]!evaluation.getOccurrence(areaQname).status}<#if evaluation.getOccurrence(areaQname).threatened!false> (RT)</#if></#if>",<#t>
</#list>
"<@showValue "MKV.occurrenceRegionsNotes" evaluation />",<#t>
"<@showValue "MKV.occurrenceRegionsPrivateNotes" evaluation />",<#t>
"<#if evaluation.primaryHabitat??>
<@csvHabitatPairValue evaluation.primaryHabitat />
</#if>",<#t>
"<#list evaluation.secondaryHabitats as habitat>
<@csvHabitatPairValue habitat /><#if habitat_has_next>, </#if>
</#list>",<#t>
"<@showValue "MKV.habitatNotes" evaluation />",<#t>
"<@showValue "MKV.habitatGeneralNotes" evaluation />",<#t>
"<@showValue "MKV.generationAge" evaluation />",<#t>
"<@showValue "MKV.generationAgeNotes" evaluation />",<#t>
"<@showValue "MKV.evaluationPeriodLength" evaluation />",<#t>
"<@showValue "MKV.evaluationPeriodLengthNotes" evaluation />",<#t>
"<@showValue "MKV.individualCountMin" evaluation /> - <@showValue "MKV.individualCountMax" evaluation />",<#t>
"<@showValue "MKV.individualCountNotes" evaluation />",<#t>
"<@showValue "MKV.populationSizePeriodBeginning" evaluation />",<#t>
"<@showValue "MKV.populationSizePeriodEnd" evaluation />",<#t>
"<@showValue "MKV.populationSizePeriodNotes" evaluation />",<#t>
"<@showValue "MKV.decreaseDuringPeriod" evaluation />",<#t>
"<@showValue "MKV.decreaseDuringPeriodNotes" evaluation />",<#t>
"<@showValue "MKV.populationVaries" evaluation />",<#t>
"<@showValue "MKV.populationVariesNotes" evaluation />",<#t>
"<@showValue "MKV.fragmentedHabitats" evaluation />",<#t>
"<@showValue "MKV.fragmentedHabitatsNotes" evaluation />",<#t>
"<@showValue "MKV.borderGain" evaluation />",<#t>
"<#list evaluation.endangermentReasons as reason>
${reason.endangerment?replace("MKV.endangermentReason","")}<#if reason_has_next>, </#if>
</#list>",<#t>
"<@showValue "MKV.endangermentReasonNotes" evaluation />",<#t>
"<#list evaluation.threats as reason>
${reason.endangerment?replace("MKV.endangermentReason","")}<#if reason_has_next>, </#if>
</#list>",<#t>
"<@showValue "MKV.threatNotes" evaluation />",<#t>
"<@showValue "MKV.groundsForEvaluationNotes" evaluation />",<#t>
<#list ["A", "B", "C", "D", "E"] as criteria>
"<@showValue "MKV.criteria"+criteria evaluation />",<#t>
"<@showValue "MKV.criteria"+criteria+"Notes" evaluation />",<#t>
"${(evaluation.getValue("MKV.status"+criteria)!"")?replace("MX.iucn","")}",<#t>
"<@showValue "MKV.status"+criteria+"Notes" evaluation />",<#t>
</#list>
"<@showValue "MKV.criteriaNotes" evaluation />",<#t>
"${(evaluation.getValue("MKV.redListStatus")!"")?replace("MX.iucn","")} <@csvExteralPopulationImpactOnRedListStatus evaluation />",<#t>
"<@showValue "MKV.criteriaForStatus" evaluation />",<#t>
"${(evaluation.getValue("MKV.redListStatusMin")!"")?replace("MX.iucn","")} - ${(evaluation.getValue("MKV.redListStatusMax")!"")?replace("MX.iucn","")}",<#t>
"<@showValue "MKV.exteralPopulationImpactOnRedListStatus" evaluation />",<#t>
"<@showValue "MKV.reasonForStatusChange" evaluation />",<#t>
"<@showValue "MKV.ddReason" evaluation />",<#t>
"${(evaluation.getValue("MKV.possiblyRE")!"")?replace("MX.iucn","")}",<#t>
"<@showValue "MKV.lastSightingNotes" evaluation />",<#t>
"<@showValue "MKV.lsaRecommendation" evaluation />",<#t>
"<@showValue "MKV.redListStatusNotes" evaluation />",<#t>
"<@showValue "MKV.criteriaForStatusNotes" evaluation />",<#t>
"<@showValue "MKV.exteralPopulationImpactOnRedListStatusNotes" evaluation />",<#t>
"<@showValue "MKV.reasonForStatusChangeNotes" evaluation />",<#t>
"<@showValue "MKV.ddReasonNotes" evaluation />",<#t>
"<@showValue "MKV.possiblyRENotes" evaluation />",<#t>
"<@showValue "MKV.redListStatusAccuracyNotes" evaluation />",<#t>
"<@showValue "MKV.lsaRecommendationNotes" evaluation />",<#t>
"<@showValue "MKV.percentageOfGlobalPopulation" evaluation />",<#t>
"<@showValue "MKV.percentageOfGlobalPopulationNotes" evaluation />",<#t>
"<#list evaluation.getValues("MKV.publication") as value>
	<#if publications[value]??>
		${publications[value].citation}
	<#else>
		${value}
	</#if>
	
</#list>",<#t>
"<@showValue "MKV.otherSources" evaluation />"<#t>
</@compress></#if>

</#list>