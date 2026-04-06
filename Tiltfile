allow_k8s_contexts('polar')

# Platform services (PostgreSQL, Redis, RabbitMQ)
k8s_yaml([
    'polar-deployment/kubernetes/platform/development/services/postgresql.yml',
    'polar-deployment/kubernetes/platform/development/services/redis.yml',
    'polar-deployment/kubernetes/platform/development/services/rabbitmq.yml',
    'polar-deployment/kubernetes/platform/development/services/minio.yml',
])
k8s_resource('polar-minio', port_forwards=['9002:9000', '9091:9090'])

# vitale_app (catalog service)
custom_build(
    ref='vitale',
    command='mvn package -pl vitale_app -am -DskipTests && docker build -t $EXPECTED_REF vitale_app',
    deps=['vitale_app/src', 'vitale_app/pom.xml']
)
k8s_yaml(['vitale_app/k8s/deployment.yml', 'vitale_app/k8s/service.yml'])
k8s_resource('vitale', port_forwards=['9001', '8001'])

# edge-service (API gateway)
custom_build(
    ref='edge-service',
    command='mvn package -pl edge-service -am -DskipTests && docker build -t $EXPECTED_REF edge-service',
    deps=['edge-service/src', 'edge-service/pom.xml']
)
k8s_yaml(['edge-service/k8s/deployment.yml', 'edge-service/k8s/service.yml', 'edge-service/k8s/ingress.yml'])
k8s_resource('edge-service', port_forwards=['9005:9000'])

# books-ui (Angular frontend)
custom_build(
    ref='books-ui',
    command='docker build -t $EXPECTED_REF books-ui',
    deps=['books-ui/src', 'books-ui/package.json']
)
k8s_yaml(['books-ui/k8s/deployment.yml', 'books-ui/k8s/service.yml'])
k8s_resource('books-ui', port_forwards=['4200:80'])

# dispatcher-service
custom_build(
    ref='dispatcher-service',
    command='mvn package -pl dispatcher-service -am -DskipTests && docker build -t $EXPECTED_REF dispatcher-service',
    deps=['dispatcher-service/src', 'dispatcher-service/pom.xml']
)
k8s_yaml(['dispatcher-service/k8s/deployment.yml', 'dispatcher-service/k8s/service.yml'])
k8s_resource('dispatcher-service', port_forwards=['9003'])