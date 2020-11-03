oc expose svc anchore-engine-anchore-engine-api -n anchore

export ANCHORE_CLI_URL=http://anchore-engine-anchore-engine-api-anchore.apps.cluster-dc8c.dc8c.sandbox235.opentlc.com/v1
export ANCHORE_CLI_USER=admin
export ANCHORE_CLI_PASS=foobar

anchore-cli registry add --insecure  image-registry.openshift-image-registry.svc:5000 admin $(oc whoami -t)

##list Images
anchore-cli image list

anchore-cli policy add my-policy-anchore.json
anchore-cli policy activate 2c53a13c-1765-11e8-82ef-23527761d061