# On écrit de manière explicite et immuable la version du serveur
FROM tomcat:10.1-jdk17

# On automatise la copie du fichier généré.
# Déployé en ROOT.war -> servi à la racine "/" (endpoint /api/graphql, sans préfixe de contexte).
COPY build/libs/jaxrs-liquibase-graphql-api.war /usr/local/tomcat/webapps/ROOT.war