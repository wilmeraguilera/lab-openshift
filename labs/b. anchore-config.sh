oc expose svc anchore-engine-anchore-engine-api -n anchore

export ANCHORE_CLI_URL=http://anchore-engine-anchore-engine-api-anchore.apps.cluster-59bf.59bf.sandbox902.opentlc.com/v1
export ANCHORE_CLI_USER=admin
export ANCHORE_CLI_PASS=foobar

anchore-cli registry add --insecure  image-registry.openshift-image-registry.svc:5000 admin $(oc whoami -t)

##list Images
anchore-cli image list