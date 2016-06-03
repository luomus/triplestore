<#include "luomus-header.ftl">
<#include "iucn-macro.ftl">

<h1>Uhanalaisuusarviointi - ${selectedYear} <#if draftYear == selectedYear>(LUONNOS)</#if></h1>
<@toolbox/>

<h2>${group.name.forLocale("fi")!""}</h2>
<p class="info">Uhanalaisuusarviojat: <@editors group /></p>



<p class="info">Voit siirtyä katselemaan lajia klikkaamalla lajin nimeä.</p>

<#include "luomus-footer.ftl">