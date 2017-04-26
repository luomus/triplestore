<#macro toolbox>
		<div id="toolbox" class="iucnToolbox ui-widget ui-corner-all">
			<div class="ui-widget-header noselect" id="toolboxToggle">Työkalut</div>
			<div id="toolBoxContent" class="ui-widget-content">
			
			<div>
				Vaihda arviointikautta: 
				<select name="yearSelector" id="yearSelector" onchange="changeYear()">
					<#list evaluationYears?reverse as year>
						<option value="${year}" <#if selectedYear == year> selected="selected" </#if> >
							${year}
						</option>
					</#list>
				</select>
			</div>
			
			<div id="taxonSearch">
				Etsi lajilla:
				<form onsubmit="searchTaxon(this, true, true); return false;" class="taxonomySearchForm" taxonpageBaseLinkType="iucnEdit">
					<input type="text" placeholder="Kirjoita nimi tai nimen osa"/> <input type="submit" value="Hae" />
					<div class="taxonomySearchResultContainer" style="display: none;">&nbsp;</div>
				</form>
			</div>
			
			</div>
		</div>
</#macro>

<#macro editors groupQname>
	<#if taxonGroupEditors[groupQname]??>
		<#list taxonGroupEditors[groupQname].editors as editor>
			${persons[editor.toString()].fullname}<#if editor_has_next>, </#if>
		</#list>
	<#else>
		Ei vielä määritelty
	</#if>
</#macro>

<#macro speciesRow target year>
	<td>
		${target.orderAndFamily}
	</td>
	<td>
		<a href="${baseURL}/iucn/species/${target.qname}/${year}">
			<span class="scientificName speciesName">${target.scientificName!target.qname}</span>
		</a>
		<span class="synonyms scientificName speciesName">${target.synonymNames}</span>
	</td>
	<td>
		<a href="${baseURL}/iucn/species/${target.qname}/${year}">
			${target.vernacularNameFi!""}
		</a>
	</td>
	<#if target.hasEvaluation(year)>
		<#assign evaluation = target.getEvaluation(year)>
		<td>
			<#if evaluation.ready>
				<span class="state ready">Valmis</span>
			<#else>
				<span class="state started">Aloitettu</span>
			</#if>
		</td>
		<td>${(evaluation.lastModified?string("d.M.yyyy"))!"-"}</td>
		<td>
			<#if evaluation.lastModifiedBy??>
				${persons[evaluation.lastModifiedBy].fullname}
			<#else>
				-
			</#if>
		</td>
		<td>
			<#if evaluation.hasIucnStatus()>
				${statusProperty.range.getValueFor(evaluation.iucnStatus).label.forLocale("fi")} ${evaluation.externalImpact}
		    <#else>
		    	-
		    </#if>
		</td>
		<td class="redListIndexTableField">
			<#if evaluation.hasIucnStatus()>
				<#if evaluation.hasCorrectedStatusForRedListIndex()>
					${evaluation.calculatedCorrectedRedListIndex!""} (${evaluation.correctedStatusForRedListIndex?replace("MX.iucn", "")}) <span class="correctedIndex">[KORJATTU]</span>
				<#else>
					${evaluation.calculatedRedListIndex!"-"} (${evaluation.iucnStatus?replace("MX.iucn", "")})
				</#if>
			<#else>
				-
			</#if>
		</td>
	<#else>
		<td>
			<span class="state notStarted">Ei aloitettu</span> 
			<#if permissions>
				<button class="markNEButton">NE</button>
				<button class="markNAButton">NA</button>
				<#if target.hasPreviousEvaluation(year)>
					<#assign prevEvaluation = target.getPreviousEvaluation(year)>
					<#if prevEvaluation.hasIucnStatus()>
						<#if prevEvaluation.iucnStatus == "MX.iucnLC">
							<button class="markLCButton">LC</button>
						</#if>
					</#if>
				</#if>
			</#if>
		</td>
		<td>-</td>
		<td>-</td>
		<td>-</td>
		<td class="redListIndexTableField">-</td>
	</#if>
	<td>
		<#if target.hasPreviousEvaluation(year)>
			<#assign prevEvaluation = target.getPreviousEvaluation(year)>
			<#if prevEvaluation.hasIucnStatus()>
				${prevEvaluation.iucnStatus?replace("MX.iucn", "")}  ${prevEvaluation.externalImpact}
				(${prevEvaluation.evaluationYear})
			<#else>
				-
			</#if>
		<#else>
			-
		</#if>
	</td>
	<td>
		<#if target.hasEvaluation(year)>
			<#if target.getEvaluation(year).hasRemarks()>
				<span class="ui-icon ui-icon-comment remarks" title="${target.getEvaluation(year).remarks?html}"></span>
			</#if> 
		</#if>
	</td>
</#macro>

<#macro typeOfOccurrenceInFinland>
	<tr>
		<th>
			<@iucnLabel "MKV.typeOfOccurrenceInFinland" />
			<div class="propertyComments">
				<ul> 
					<li><b>Vakiintunut</b> = luontaisesti levinnyt, lisääntyvä ja alkuperäinen tai uudempi vakiintunut tulokas (tyypillisesti lisääntynyt alueella ainakin 10 v). Myös ennen vuotta 1800 luontoon vakiintunut, ihmisen tuoma laji</li>
					<li><b>Uusi laji</b> = luontaisesti levinnyt, mutta ei vakiintunut (tai vakiintumisesta ei ole varmuutta)</li>
					<li><b>Hävinnyt</b> = laji on hävinnyt Suomesta</li>
					<li><b>Säännöllinen vierailija</b> = ei lisäänny, esiintyminen +/- ennustettavaa</li>
					<li><b>Satunnainen vierailija</b> = ei lisäänny, esiintyminen +/- ennustamatonta</li>
					<li><b>Vieraslaji</b> = laji on tullut alueelle ihmisen avustamana (tahattomasti tai tahallisesti) vuoden 1800 jälkeen</li>
				</ul>
			</div>
		</th>
		<td><@showValue "MKV.typeOfOccurrenceInFinland" comparison occurrenceStatuses /> <@showNotes "MKV.typeOfOccurrenceInFinlandNotes" comparison /></td>
		<td>
			<#if permissions>
				<@iucnInputField "MKV.typeOfOccurrenceInFinland" occurrenceStatuses /> <@editableNotes "MKV.typeOfOccurrenceInFinlandNotes" />
			<#else>
				<@showValue "MKV.typeOfOccurrenceInFinland" evaluation occurrenceStatuses /> <@showNotes "MKV.typeOfOccurrenceInFinlandNotes" evaluation />
			</#if>
		</td>
	</tr>
</#macro>

<#macro iucnSection title>
	<tr class="section">
		<th colspan="3"><label>${title}</label></th>
	</tr>
</#macro>

<#macro iucnInputField fieldName customRange=[]>
	<#assign property = evaluationProperties.getProperty(fieldName)>
	<#assign values = ['']>
	<#if evaluation?? && evaluation.hasValue(fieldName)>
		<#assign values = evaluation.getValues(fieldName)>
	</#if>
	<@iucnInputFieldWithValues property values customRange />
	<#if property.hasUnitOfMeasurement()>
		${property.unitOfMeasurement.label.forLocale("fi")?html}
	</#if>
</#macro>

<#macro iucnInputFieldWithValues property values customRange=[]>
	<#if property.literalProperty && !property.booleanProperty>
		<#list values as value>
			<#if property.hasUnitOfMeasurement() && property.unitOfMeasurement.qname == "MZ.unitOfMeasurementPercent">
				<input class="percentProperty" name="${property.qname}" type="text" value="${value?html}">
			<#elseif property.integerProperty>
				<input class="integerProperty" name="${property.qname}" type="text" value="${value?html}">
			<#elseif property.decimalProperty>
				<input class="decimalProperty" name="${property.qname}" type="text" value="${value?html}">
			<#else>
				<input name="${property.qname}" type="text" value="${value?html}">
			</#if>
			<#if value_has_next><br /></#if>
		</#list>
		<#if property.repeated>
			<button class="add">+ Lisää</button>
		</#if>
	<#elseif property.booleanProperty>
		<#assign value = values?first>
		<select name="${property.qname}" class="shortChosen" data-placeholder="Kyllä/Ei">
			<option value="" label=".."></option>
			<#list property.range.values as optionValue>
				<#if value == optionValue.qname>
					<option value="${optionValue.qname}" selected="selected">${optionValue.label.forLocale("fi")?html}</option>
				<#else>
					<option value="${optionValue.qname}" >${optionValue.label.forLocale("fi")?html}</option>
				</#if>
			</#list>
		</select>
	<#else>
		<select name="${property.qname}"  data-placeholder="..." <#if property.repeated>multiple="multiple"</#if> >
			<option value="" label=".."></option>
			<#assign ranges = property.range.values>
			<#if customRange?has_content> <#assign ranges = customRange> </#if> 
			<#list ranges as enumValue>
				<#assign hasValue = false>
				<#list values as evaluationValue>
					<#if same(enumValue.qname, evaluationValue)><#assign hasValue = true><#break></#if>
				</#list>
				<option value="${enumValue.qname}"  <#if hasValue>selected="selected"</#if> >${enumValue.label.forLocale("fi")?html}</option>	
			</#list>
		</select>
	</#if>
</#macro>

<#macro iucnInput fieldName notesFieldName="NONE" additionalClass="" customRange=[]>
	<tr class="${additionalClass}">
		<th><@iucnLabel fieldName /></th>
		<td><@showValue fieldName comparison customRange /> <@showNotes notesFieldName comparison /></td>
		<td>
			<#if permissions>
				<@iucnInputField fieldName customRange /> <@editableNotes notesFieldName />
			<#else>
				<@showValue fieldName evaluation customRange /> <@showNotes notesFieldName evaluation />
			</#if>
		</td>
	</tr>
</#macro>

<#macro iucnTextarea fieldName additionalClass="" notesFieldName="NONE">
	<tr class="${additionalClass}">
		<th><@iucnLabel fieldName /></th>
		<td><@showValue fieldName comparison /> <@showNotes notesFieldName comparison /></td>
		<td>
			<#if permissions>
				<textarea name="${fieldName}"><#if evaluation??>${(evaluation.getValue(fieldName)?html)!""}</#if></textarea> <@editableNotes notesFieldName />
			<#else>
				<@showValue fieldName evaluation />  <@showNotes notesFieldName evaluation />
			</#if>
		</td>
	</tr>
</#macro>

<#macro iucnMinMax title minFieldName maxFieldName notesFieldName="NONE">
	<#assign property = evaluationProperties.getProperty(minFieldName)>
	<tr class="minMax">
		<th>
			<label>${title?html}</label> 
			<#if property.integerProperty><span class="unitOfMeasurement">(kokonaisluku)</span></#if>
			<#if property.decimalProperty><span class="unitOfMeasurement">(desimaaliluku)</span></#if>
			<#if (property.comments.forLocale("fi"))??>
				<div class="propertyComments">${property.comments.forLocale("fi")}</div>
			</#if>
		</th>
		<td>
			<#if comparison?? && (comparison.hasValue(minFieldName) || comparison.hasValue(maxFieldName))>
				<@showValue minFieldName comparison /> - <@showValue maxFieldName comparison />
			</#if>
			<@showNotes notesFieldName comparison />
		</td>
		<td>
			<#if permissions>
				<@iucnInputField minFieldName /> - <@iucnInputField maxFieldName /> <@editableNotes notesFieldName />
			<#else>
				<#if evaluation?? && (evaluation.hasValue(minFieldName) || evaluation.hasValue(maxFieldName))>
					<@showValue minFieldName evaluation /> - <@showValue maxFieldName evaluation /> <@showNotes notesFieldName evaluation />
				</#if>
			</#if>
		</td>
	</tr>
</#macro>

<#macro iucnOccurrence areaQname>	
	<tr>
		<th><label>${areas[areaQname].name.forLocale("fi")?html}</label></th>
		<td>
			<#if comparison?? && comparison.hasOccurrence(areaQname)>
				<#assign areaStatus = comparison.getOccurrence(areaQname).status>
				<#list regionalOccurrenceStatuses as status>
					<#if status.qname == areaStatus>
						${status.label.forLocale("fi")?html}
						<#break>
					</#if>
				</#list>
				<#if comparison.getOccurrence(areaQname).threatened!false>
					&nbsp; RT
				</#if>
			</#if>
		</td>
		<td>
			<#if permissions>
				<#if areaQname = "ML.690">
					<div class="buttonContainer"> 
						<button id="markAllDoesNotOccurButton">Merkitse kaikkiin 'Ei havaintoja vyöhykkeeltä'</button>
						<button id="revealRegionalThreatenedButton">Määritä alueellinen uhanalaisuus</button>
					</div>
				</#if>
				<select name="MKV.hasOccurrence___${areaQname}___status" data-placeholder="..." class="regionalOccurrence">
					<option value="" label=".."></option>
					<#list regionalOccurrenceStatuses as prop>
						<#if evaluation?? && evaluation.hasOccurrence(areaQname) && ((evaluation.getOccurrence(areaQname).status.toString())!"") == prop.qname.toString()>
							<option value="${prop.qname}" selected="selected">${prop.label.forLocale("fi")?html}</option>
						<#else>
							<option value="${prop.qname}">${prop.label.forLocale("fi")?html}</option>
						</#if>
					</#list>
				</select>
				<span class="regionalThreatened">
					RT &nbsp; <input type="checkbox" name="MKV.hasOccurrence___${areaQname}___threatened" value="RT"
						<#if evaluation?? && evaluation.hasOccurrence(areaQname) && evaluation.getOccurrence(areaQname).threatened!false>
							checked="checked"
						</#if>
					   />
				</span>
			<#else>
				<#if evaluation?? && evaluation.hasOccurrence(areaQname)>
					<#assign areaStatus = evaluation.getOccurrence(areaQname).status>
					<#list regionalOccurrenceStatuses as status>
						<#if status.qname == areaStatus>
							${status.label.forLocale("fi")?html}
							<#break>
						</#if>
					</#list>
					<#if evaluation.getOccurrence(areaQname).threatened!false>
						&nbsp; RT
					</#if>
				</#if>
			</#if>
		</td>
	</tr>
</#macro>

<#macro iucnEndangermentObject fieldName notesFieldName>
	<tr>
		<th><@iucnLabel fieldName /></th>
		<td>
			<#if comparison??>
				<#if fieldName = "MKV.hasEndangermentReason">
					<#assign comparisonValues = comparison.endangermentReasons>
				<#else>
					<#assign comparisonValues = comparison.threats>
				</#if>
				<#list comparisonValues as reason>
						${endangermentObjectProperties.getProperty("MKV.endangerment").range.getValueFor(reason.endangerment).label.forLocale("fi")?html}
					<#if reason_has_next><br /></#if>
				</#list>
				<@showNotes notesFieldName comparison />
			</#if>
		</td>
		<td>
			<#if permissions>
				<#if evaluation??>
					<#if fieldName = "MKV.hasEndangermentReason">
						<#assign givenReasons = evaluation.endangermentReasons>
					<#else>
						<#assign givenReasons = evaluation.threats>
					</#if>
					<#list givenReasons as reason>
						<select name ="${fieldName}___${reason_index}" data-placeholder="...">
							<option value="" label=".."></option>
							<#list endangermentObjectProperties.getProperty("MKV.endangerment").range.values as value>
								<option value="${value.qname}" <#if reason.endangerment == value.qname>selected="selected"</#if>>
									${value.label.forLocale("fi")?html}
								</option>
							</#list>
						</select>
						<#if reason_has_next><br /></#if>
					</#list>
					<br />
					<select name ="${fieldName}___0" data-placeholder="...">
						<option value="" label=".."></option>
						<#list endangermentObjectProperties.getProperty("MKV.endangerment").range.values as value>
							<option value="${value.qname}">${value.label.forLocale("fi")?html}</option>
						</#list>
					</select>
				<#else>
					<select name ="${fieldName}___0" data-placeholder="...">
						<option value="" label=".."></option>
						<#list endangermentObjectProperties.getProperty("MKV.endangerment").range.values as value>
							<option value="${value.qname}">${value.label.forLocale("fi")?html}</option>
						</#list>
					</select>
				</#if>
				<button class="add">+ Lisää</button>
			<#else>
				<#if evaluation??>
					<#if fieldName = "MKV.hasEndangermentReason">
						<#assign givenReasons = evaluation.endangermentReasons>
					<#else>
						<#assign givenReasons = evaluation.threats>
					</#if>
					<#list givenReasons as reason>
						${endangermentObjectProperties.getProperty("MKV.endangerment").range.getValueFor(reason.endangerment).label.forLocale("fi")?html}
						<#if reason_has_next><br /></#if>
					</#list>
					<@showNotes notesFieldName evaluation />
				</#if>	
			</#if>
		</td>
	</tr>
</#macro>

<#macro iucnHabitatFields>
	<tr class="primaryHabitatRow">
		<th><@iucnLabel "MKV.primaryHabitat" /></th>
		<td>
			<#if comparison?? && comparison.primaryHabitat??>
				<@showHabitatPairValue comparison.primaryHabitat />
			</#if>
			<@showNotes "MKV.habitatNotes" comparison />
		</td>
		<td>
			<#if permissions>
				<#if evaluation?? && evaluation.primaryHabitat??>
					<@editableHabitatPair "MKV.primaryHabitat" evaluation.primaryHabitat />
				<#else>
					<@editableHabitatPair "MKV.primaryHabitat" "NONE" />
				</#if>
				<@editableNotes "MKV.habitatNotes" />
			<#else>
				<#if evaluation?? && evaluation.primaryHabitat??>
					<@showHabitatPairValue evaluation.primaryHabitat />
				</#if>
				<@showNotes "MKV.habitatNotes" evaluation />
			</#if>
		</td>
	</tr>
	<tr class="secondaryHabitatRow">
		<th><@iucnLabel "MKV.secondaryHabitat" /></th>
		<td>
			<#if comparison??>
				<#list comparison.secondaryHabitats as habitat>
					<@showHabitatPairValue habitat />
					<#if habitat_has_next><br /></#if>
				</#list>
			</#if>
		</td>
		<td>
			<#if permissions>
				<#if evaluation??>
					<#list evaluation.secondaryHabitats as habitat>
						<@editableHabitatPair "MKV.secondaryHabitat" habitat habitat_index />
						<#if habitat_has_next><br /></#if>
					</#list>
				</#if>
				<#if !(evaluation??) || !(evaluation.secondaryHabitats?has_content)>
					<@editableHabitatPair "MKV.secondaryHabitat" "NONE" />
				</#if>
				<button class="addHabitatPair">+ Lisää</button>
			<#else>
				<#if evaluation??>
					<#list evaluation.secondaryHabitats as habitat>
						<@showHabitatPairValue habitat />
					</#list>
				</#if>	
			</#if>
		</td>
	</tr>
</#macro>

<#macro editableHabitatPair fieldName habitatObject index=0>
	<div class="habitatPair">
		<select name ="${fieldName}___${index}___MKV.habitat" data-placeholder="...">
			<option value="" label=".."></option>
			<#list habitatObjectProperties.getProperty("MKV.habitat").range.values as value>
				<option value="${value.qname}" <#if habitatObject != "NONE" && habitatObject.habitat == value.qname>selected="selected"</#if>>
					${habitatLabelIndentator.indent(value.qname)}
				</option>
			</#list>
		</select>
		<select name ="${fieldName}___${index}___MKV.habitatSpecificType" data-placeholder="Lisämerkinnät" multiple="multiple">
			<option value="" label=".."></option>
			<#list habitatObjectProperties.getProperty("MKV.habitatSpecificType").range.values as value>
				<option value="${value.qname}" <#if habitatObject != "NONE" && habitatObject.habitatSpecificTypes?seq_contains(value.qname)>selected="selected"</#if>>
					${value.label.forLocale("fi")?html}
				</option>
			</#list>
		</select>
	</div>
</#macro>

<#macro showHabitatPairValue habitatObject>
	${habitatObjectProperties.getProperty("MKV.habitat").range.getValueFor(habitatObject.habitat).label.forLocale("fi")?html}
	<br />
	<#list habitatObject.habitatSpecificTypes as type>
		&nbsp; &nbsp; ${habitatObjectProperties.getProperty("MKV.habitatSpecificType").range.getValueFor(type).label.forLocale("fi")?html}
		<#if type_has_next><br /></#if>
	</#list>
	<br />
</#macro>

<#macro csvHabitatPairValue habitatObject><@compress single_line=true>
	${habitatObject.habitat?replace("MKV.habitat","")}
	<#list habitatObject.habitatSpecificTypes as type>
		${type?replace("MKV.habitatSpecificType","")?lower_case}
	</#list>
</@compress><#t></#macro>

<#macro csvExteralPopulationImpactOnRedListStatus evaluation>
	<#switch evaluation.getValue("MKV.exteralPopulationImpactOnRedListStatus")!"">
		<#case "MKV.exteralPopulationImpactOnRedListStatusEnumMinus1">-1<#break>
		<#case "MKV.exteralPopulationImpactOnRedListStatusEnumMinus2">-2<#break>
		<#case "MKV.exteralPopulationImpactOnRedListStatusEnumPlus1">°<#break>
		<#case "MKV.exteralPopulationImpactOnRedListStatusEnumPlus2">°°<#break>		
	</#switch>
</#macro>

<#macro iucnPublications fieldName>
	<tr>
		<th>
			<@iucnLabel fieldName />
			<p class="info">(Huom: Samaan tietokantaan ei kannata viitata kovin monella eri latauspäivämäärällä; esimerkiksi yksi viittaus per arviointikausi riittänee.)</p> 
		</th>
		<td>
			<@showPublications fieldName comparison />
			<#if comparison??>${(comparison.getValue("MKV.legacyPublications")!"")?html}</#if>
		</td>
		<td>
			<#if permissions>
				<table class="publicationSelect">
					<tr>
						<th>Valitse julkaisu</th> 
					</tr>
					<#if evaluation??>
					<#list evaluation.getValues(fieldName) as publication>
						<tr>
							<td>
								<select name="${fieldName}">
									<option value="" label=".."></option>
									<#list publications?keys as publicationQname>
										<option value="${publicationQname}" <#if same(publication, publicationQname)>selected="selected"</#if> >${publications[publicationQname].citation?html}</option>
									</#list>
								</select>
							</td>
						</tr>
					</#list>
					</#if>
					<tr>
						<td>
							<select name="${fieldName}"  data-placeholder="Valitse julkaisu" >
								<option value="" label=".."></option>
								<#list publications?keys as publicationQname>
									<option value="${publicationQname}">${publications[publicationQname].citation?html}</option>
								</#list>
							</select>
							<button class="add">+ Lisää</button>
						</td>
					</tr>
					<tr>
						<th>Tai luo uusi julkaisu</th> 
					</tr>
					<tr>
						<td><textarea class="createPublication" name="newIucnPublicationCitation" id="createNewIucnPublicationCitationInput" placeholder="Kirjoita viite"></textarea></td>
					</tr>	
				</table>
			<#else>
				<@showPublications fieldName evaluation />
			</#if>
		</td>
	</tr>
</#macro>

<#macro showPublications fieldName data="NONE">
	<#if data != "NONE">
		<#list data.getValues(fieldName) as value>
			<#if publications[value]??>
				${publications[value].citation?html}
			<#else>
				${value?html}
			</#if>
			<#if value_has_next><br /><br /></#if>
		</#list>
	</#if>
</#macro>

<#macro iucnLabel fieldName>
	<#assign property = evaluationProperties.getProperty(fieldName)> 
	<label>${(property.label.forLocale("fi")?html)!fieldName}</label>
	<#if property.integerProperty>
		<span class="unitOfMeasurement">(kokonaisluku)</span>
	</#if>
	<#if property.decimalProperty>
		<span class="unitOfMeasurement">(desimaaliluku)</span>
	</#if>
	<#if (property.comments.forLocale("fi"))??>
		<div class="propertyComments">${property.comments.forLocale("fi")}</div>
	</#if>
</#macro>

<#macro showValue fieldName data="NONE" customRanges=[]><@compress single_line=true>
	<#if data != "NONE">
		<#assign property = evaluationProperties.getProperty(fieldName)>
		<#list data.getValues(fieldName) as value>
			<#if property.literalProperty && !property.booleanProperty>
				${value?html}
			<#else>
				<#if customRanges?has_content>
					<#list customRanges as customRange>
						<#if customRange.qname == value>
							${customRange.label.forLocale("fi")?html}
							<#break>
						</#if>
					</#list>
				<#else>
					${property.range.getValueFor(value).label.forLocale("fi")?html} 
				</#if>
			</#if>
			<#if value_has_next><br /><br /></#if>
		</#list>
	</#if>
</@compress><#t></#macro>

<#macro showNotes notesFieldName data="NONE">
	<#if data != "NONE" && notesFieldName != "NONE" && data.hasValue(notesFieldName)>
		<div class="noteViewer"><span class="ui-icon ui-icon-comment" title="${data.getValue(notesFieldName)?html}"></span></div>
	</#if>
</#macro>

<#macro editableNotes notesFieldName>
	<#if notesFieldName != "NONE">
		<div class="notes hidden">
			<@iucnLabel notesFieldName />
			<textarea name="${notesFieldName}"><#if evaluation??>${(evaluation.getValue(notesFieldName)?html)!""}</#if></textarea>
			<br />
			<button class="closeNoteEditButton">Sulje</button>
		</div>
	</#if>
</#macro>

<#macro propertyCommentsScript>
<script>
$(function() {
 	$(".propertyComments").each(function() {
 		var comments = $(this); 
 		comments.hide();
 		var info = $('<span class="ui-icon ui-icon-info" />');
 		$(this).before(info);
 		info.on('mouseover click', function() {
 			comments.addClass('modalInfoContainer');
 			comments.fadeIn(500);
 		});
 		info.on('mouseleave', function() {
 			$('.modalInfoContainer').fadeOut(200);
 		});
 	});
 });
 </script>
</#macro>