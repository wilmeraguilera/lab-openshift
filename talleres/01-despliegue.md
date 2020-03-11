# Despliegue

Openshift proporciona varios mecanismos que permiten realizar el despliegue de las aplicaciones sobre la plataforma, con el animo de conocerlos desplegaremos  una aplicacion java que expone un Api Rest construida con Spring Boot.

El repositorio de los fuentes es el siguiente:

https://github.com/wilmeraguilera/lab-openshift

Dentro de este repositorio existe un subdirectorio llamado backend-users que contiene el api que desplegaremos.

La alternativas de despliegue que utilizaremos en el taller son las siguientes:
- Fabric8
- Source2Image
- DockerFile



## Fabric8

Fabric8 es una herramienta que busca facilitar el ciclo de vida de las aplicaciones, con esta utilidad es posible compilar, probar y desplegar nuestras aplicaciones en Openshift o Kubernetes.

Para realizar el uso de esta utilidad dentro de nuestras aplicaciones existe un plugin de maven que debemos agregar y con un par de configuraciones lograremos realizar los despliegues en Openshift.


Realizar la compilación del proyecto de manera local mediente el uso de maven.

```
mvn instal
```

Debemos ver una salida en consola que nos indique que la compilación fué satisfactoria, deberia ser similar a la siguiente:

```
/home/wilmeraguilera/.m2/repository/com/redhat/backend-users/0.0.1-SNAPSHOT/backend-users-0.0.1-SNAPSHOT.jar
[INFO] Installing /home/wilmeraguilera/git/lab-openshift/backend-users/pom.xml to /home/wilmeraguilera/.m2/repository/com/redhat/backend-users/0.0.1-SNAPSHOT/backend-users-0.0.1-SNAPSHOT.pom
[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
[INFO] ------------------------------------------------------------------------
[INFO] Total time:  8.405 s
[INFO] Finished at: 2020-03-11T16:59:44-05:00
[INFO] ------------------------------------------------------------------------
```

