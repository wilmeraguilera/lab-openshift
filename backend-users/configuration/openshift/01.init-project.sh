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


