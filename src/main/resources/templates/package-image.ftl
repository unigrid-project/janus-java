<#-- template to create the options file for the jpackage tool to create the application image -->
<#if osName?upper_case?contains("WIN")>

<#elseif osName?upper_case?contains("MAC")>

<#else>

</#if>
--icon src/main/resources/org/unigrid/janus/view/images/unigrid-round_77x77.ico
