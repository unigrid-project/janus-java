<#-- template to create the options file for the jpackage tool to create the installer -->
<#if osName?upper_case?contains("WIN")>
--win-menu
--win-menu-group Unigrid
--win-shortcut
--win-per-user-install
<#elseif osName?upper_case?contains("MAC")>
--mac-package-identifier Janus
--mac-package-name Janus

<#else>
--linux-menu-group unigrid
--linux-shortcut
--linux-package-name janus
</#if>
--description "JavaFX wallet for the Unigrid Network"
--vendor "UGD software AB, Sweden"
--dest target/release