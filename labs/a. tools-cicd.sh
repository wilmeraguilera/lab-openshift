oc delete project devops
oc delete project anchore
sleep 40
oc new-project devops
oc new-project anchore
oadm policy add-scc-to-user anyuid -z default -n anchore
oadm policy add-scc-to-user anyuid -z default -n devops
oc new-app jenkins-persistent --param ENABLE_OAUTH=true --param MEMORY_LIMIT=2Gi --param VOLUME_CAPACITY=5Gi --param DISABLE_ADMINISTRATIVE_MONITORS=true
oc adm policy add-cluster-role-to-user edit system:serviceaccount:devops:jenkins



#sonar

#oc new-app --template=postgresql-persistent --param POSTGRESQL_USER=sonar --param POSTGRESQL_PASSWORD=sonar --param POSTGRESQL_DATABASE=sonar --param VOLUME_CAPACITY=5Gi --name=sonarqube_db  --labels=app=sonarqube_db


#oc new-app sonarqube --name=sonarqube --env=SONARQUBE_JDBC_USERNAME=sonar --env=SONARQUBE_JDBC_PASSWORD=sonar --env=SONARQUBE_JDBC_URL=jdbc:postgresql://postgresql/sonar --labels=app=sonarqubev
oc new-app sonarqube --name=sonarqube
echo "apiVersion: v1
kind: PersistentVolumeClaim
metadata:
  name: sonarqube-pvc
spec:
  accessModes:
  - ReadWriteOnce
  resources:
    requests:
      storage: 4Gi" | oc create -f -
oc rollout pause dc sonarqube
oc set volume dc/sonarqube --add --overwrite --name=sonarqube-volume-1 --mount-path=/opt/sonarqube/data/ --type persistentVolumeClaim --claim-name=sonarqube-pvc
oc set resources dc/sonarqube --limits=memory=3Gi,cpu=2
oc patch dc sonarqube --patch='{ "spec": { "strategy": { "type": "Recreate" }}}'
oc rollout resume dc sonarqube
oc expose svc sonarqube


#Nexus
oc new-project nexus --display-name "Repositorio Nexus"
oc new-app sonatype/nexus3:3.22.1
oc set resources dc nexus3 --limits=memory=2Gi,cpu=2 --requests=memory=1Gi,cpu=500m
oc expose svc nexus3