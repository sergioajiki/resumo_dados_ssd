@echo off
:: Script de conveniência para executar o Maven sem instalação global
:: Usa o Maven disponível no wrapper local do usuário
set MAVEN_HOME=%USERPROFILE%\.m2\wrapper\dists\apache-maven-3.9.11-bin\6mqf5t809d9geo83kj4ttckcbc\apache-maven-3.9.11
"%MAVEN_HOME%\bin\mvn.cmd" %*
