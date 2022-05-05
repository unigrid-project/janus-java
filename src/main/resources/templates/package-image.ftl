<#-- template to create the options file for the jpackage tool to create the application image -->
<#if osName?upper_case?contains("WIN")>
--icon src/main/resources/org/unigrid/janus/view/images/unigrid-round_77x77.ico
<#elseif osName?upper_case?contains("MAC")>
--icon src/main/resources/org/unigrid/janus/view/images/unigrid-round_77x77.png
<#else>
--icon src/main/resources/org/unigrid/janus/view/images/unigrid-round_77x77.png
</#if>
--java-options "'--add-opens java.base/java.lang=ALL-UNNAMED'"
