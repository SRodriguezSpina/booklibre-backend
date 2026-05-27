#!/bin/bash
# Evitar que mongod se queje de los permisos del keyFile montado desde Windows
cp /scripts/mongo.key /tmp/mongo.key
chmod 400 /tmp/mongo.key
chown 999:999 /tmp/mongo.key

bash /scripts/auto-init.sh &

# Ejecutar el comando original
exec "$@"
