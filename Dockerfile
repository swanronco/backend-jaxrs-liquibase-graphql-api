# On écrit de manière explicite et immuable la version du serveur
FROM tomcat:10.1-jdk17

# On automatise la copie du fichier généré
COPY build/libs/jaxrs-liquibase-graphql-api.war /usr/local/tomcat/webapps/