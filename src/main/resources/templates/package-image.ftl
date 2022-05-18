<#-- template to create the options file for the jpackage tool to create the application image -->
<#if osName?upper_case?contains("WIN")>
--icon src/main/resources/org/unigrid/janus/view/images/unigrid-256-256.ico
<#elseif osName?upper_case?contains("MAC")>
--icon src/main/resources/org/unigrid/janus/view/images/unigrid-1024x1024.icns 
--input target/dependency 
--main-jar janus-0.9.0.jar
--main-class org.unigrid.janus.Janus
<#else>
--icon src/main/resources/org/unigrid/janus/view/images/unigrid-1024x1024.png
</#if>
--java-options "--add-opens 'java.base/java.lang=ALL-UNNAMED'"