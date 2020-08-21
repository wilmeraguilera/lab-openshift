export NAMESPACE_DEV=dev-admin-users
export NAMESPACE_QA=qa-admin-users
export NAMESPACE_JENKIS=jenkins
export NAME_APP=api-users
export CONFIG_FILE_NAME=application.yml

##Proyecto de Ejemplo Spring-boot pipelines para dev
oc new-project "$NAMESPACE_DEV" --display-name "$NAMESPACE_DEV"

#Adicionar permisos al service-account del namespace de Jenkins
oc policy add-role-to-user edit system:serviceaccount:"$NAMESPACE_JENKIS":jenkins -n "$NAMESPACE_DEV"

#Crear BuildConfig de tipo binario y referenciando la imagen bade de Java
oc new-build --binary=true --name="$NAME_APP" openshift/java:8  -n "$NAMESPACE_DEV"

#Crear el DeploymentConfig
oc new-app "$NAMESPACE_DEV"/"$NAME_APP":latest --name="$NAME_APP" --allow-missing-imagestream-tags=true -n "$NAMESPACE_DEV"

#Desactivar triggers en la app para evitar el build y el deploy  automatico (Se quiere que el proceso lo controle jenkins)
oc set triggers dc/"$NAME_APP" --remove-all -n "$NAMESPACE_DEV"

#Creación del configmap
oc create configmap config-"$NAME_APP" -n "$NAMESPACE_DEV"

#Asociación del config map al DeploymentConfig
oc set volume dc/"$NAME_APP" --add --name=map-"$NAME_APP" --mount-path=/deployments/config/"$CONFIG_FILE_NAME" --sub-path="$CONFIG_FILE_NAME" --configmap-name=config-"$NAME_APP" -n "$NAMESPACE_DEV"

#Crear el service a partir del deploymentConfig, en este caso el puerto 8080
oc expose dc "$NAME_APP" --port 8080 -n "$NAMESPACE_DEV"

#Crear el route a partir del servicio
oc expose svc "$NAME_APP" -n "$NAMESPACE_DEV"





#Ambiente de QA

##Proyecto de Ejemplo Spring-boot pipelines para dev
oc new-project "$NAMESPACE_QA" --display-name "$NAMESPACE_QA"

#Asinar permisos para que el namespace de qa vea imagenes del name space de dev
oc policy add-role-to-user system:image-puller system:serviceaccount:"$NAMESPACE_QA":default  -n "$NAMESPACE_DEV"

#Adicionar permisos al service-account del namespace de Jenkins
oc policy add-role-to-user edit system:serviceaccount:"$NAMESPACE_JENKIS":jenkins -n "$NAMESPACE_QA"

#Crear el DeploymentConfig
oc new-app "$NAMESPACE_DEV"/"$NAME_APP":latest --name="$NAME_APP" --allow-missing-imagestream-tags=true -n "$NAMESPACE_QA"

#Desactivar triggers en la app para evitar el build y el deploy  automatico (Se quiere que el proceso lo controle jenkins)
oc set triggers dc/"$NAME_APP" --remove-all -n "$NAMESPACE_QA"

#Creación del configmap
oc create configmap config-"$NAME_APP" -n "$NAMESPACE_QA"

#Asociación del config map al DeploymentConfig
oc set volume dc/"$NAME_APP" --add --name=map-"$NAME_APP" --mount-path=/deployments/config/"$CONFIG_FILE_NAME" --sub-path="$CONFIG_FILE_NAME" --configmap-name=config-"$NAME_APP" -n "$NAMESPACE_QA"

#Crear el service a partir del deploymentConfig, en este caso el puerto 8080
oc expose dc "$NAME_APP" --port 8080 -n "$NAMESPACE_QA"

#Crear el route a partir del servicio
oc expose svc "$NAME_APP" -n "$NAMESPACE_QA"








##Proyecto de Ejemplo Spring-boot pipelines para dev
oc new-project "$NAMESPACE_DEV" --display-name "$NAMESPACE_DEV"

#Adicionar permisos al service-account del namespace de Jenkins
oc policy add-role-to-user edit system:serviceaccount:jenkins-shared:jenkins -n "$NAMESPACE_DEV"

#Crear BuildConfig de tipo binario y referenciando la imagen bade de Java
oc new-build --binary=true --name="api-users" openshift/java:8  -n "$NAMESPACE_DEV"

#Crear el DeploymentConfig
oc new-app "$NAMESPACE_DEV"/api-users:latest --name=api-users --allow-missing-imagestream-tags=true -n "$NAMESPACE_DEV"

#De manera opcional se pueden configurar los Limites de recursos para la aplicación
oc set resources dc api-users --limits=memory=800Mi,cpu=1000m --requests=memory=600Mi,cpu=500m

#Desactivar triggers en la app para evitar el build y el deploy  automatico (Se quiere que el proceso lo controle jenkins)
oc set triggers dc/api-users --remove-all -n "$NAMESPACE_DEV"

#Creación del configmap
oc create configmap config-backend-users

#Asociación del config map al DeploymentConfig
oc set volume dc/api-users --add --name=map-application --mount-path=/deployments/config/application.properties --sub-path=application.properties --configmap-name=config-backend-users

#Crear el service a partir del deploymentConfig, en este caso el puerto 8080
oc expose dc api-users --port 8080 -n "$NAMESPACE_DEV"

#Crear el route a partir del servicio
oc expose svc api-users -n "$NAMESPACE_DEV"

#Configurar Health-check
oc set probe dc/api-users -n "$NAMESPACE_DEV" --readiness --failure-threshold 3 --initial-delay-seconds 20 --get-url=http://:8080/healthcheck



#Ambiente de QA

##Proyecto de Ejemplo Spring-boot pipelines para dev
oc new-project "$NAMESPACE_QA" --display-name "$NAMESPACE_QA"

#Asinar permisos para que el namespace de qa vea imagenes del name space de dev
oc policy add-role-to-user system:image-puller system:serviceaccount:"$NAMESPACE_QA":default  -n "$NAMESPACE_DEV"

#Adicionar permisos al service-account del namespace de Jenkins
oc policy add-role-to-user edit system:serviceaccount:jenkins-shared:jenkins -n "$NAMESPACE_QA"

#Crear el DeploymentConfig
oc new-app "$NAMESPACE_DEV"/api-users:latest --name=api-users --allow-missing-imagestream-tags=true -n "$NAMESPACE_QA"

#De manera opcional se pueden configurar los Limites de recursos para la aplicación
oc set resources dc api-users --limits=memory=800Mi,cpu=1000m --requests=memory=600Mi,cpu=500m

#Desactivar triggers en la app para evitar el build y el deploy  automatico (Se quiere que el proceso lo controle jenkins)
oc set triggers dc/api-users --remove-all -n "$NAMESPACE_QA"

#Creación del configmap
oc create configmap config-backend-users

#Asociación del config map al DeploymentConfig
oc set volume dc/api-users --add --name=map-application --mount-path=/deployments/config/application.properties --sub-path=application.properties --configmap-name=config-backend-users

#Crear el service a partir del deploymentConfig, en este caso el puerto 8080
oc expose dc api-users --port 8080 -n "$NAMESPACE_QA"

#Crear el route a partir del servicio
oc expose svc api-users -n "$NAMESPACE_QA"

#Configurar Health-check
oc set probe dc/api-users -n "$NAMESPACE_QA" --readiness --failure-threshold 3 --initial-delay-seconds 20 --get-url=http://:8080/healthcheck

