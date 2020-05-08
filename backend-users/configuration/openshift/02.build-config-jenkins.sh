
#El siguiente script
#Los siguientes comandos tienen como propósito asociar el pipeline de Jenkins al Minishift (Corresponde a un ejercicio Local)
oc project jenkins
echo "apiVersion: v1
items:
- kind: "BuildConfig"
  apiVersion: "v1"
  metadata:
    name: "backend-users"
  spec:
    source:
      type: "Git"
      git:
        uri: "https://github.com/wilmeraguilera/lab-openshift.git"
        ref: "master"
      contextDir: "backend-users"
    strategy:
      type: "JenkinsPipeline"
      jenkinsPipelineStrategy:
        jenkinsfilePath: Jenkinsfile
kind: List
metadata: []" | oc create -f - -n jenkins


#oc secrets new-basicauth github-secret --username=wilmeraguilerab@gmail.com --password=123456789*Agh -n jenkins
#oc set build-secret --source bc/backend-users github-secret -n jenkins