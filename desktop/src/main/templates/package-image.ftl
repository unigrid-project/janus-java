<#-- template to create the options file for the jpackage tool to create the application image -->
<#if osName?upper_case?contains("WIN")>
--icon src/main/resources/org/unigrid/janus/view/images/unigrid-256-256.ico
<#elseif osName?upper_case?contains("MAC")>
--icon src/main/resources/org/unigrid/janus/view/images/unigrid-1024x1024.icns
<#else>
--icon src/main/resources/org/unigrid/janus/view/images/unigrid-1024x1024.png
</#if>
--java-options "--add-opens 'java.base/java.lang=ALL-UNNAMED'"
