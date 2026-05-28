#!/bin/bash
if [ -z "$RS_NAME" ]; then exit 0; fi

echo "Esperando a que Mongo inicie en localhost..."
until mongosh --port 27017 --quiet --eval "db.adminCommand('ping').ok" 2>/dev/null | grep -q "1"; do sleep 2; done

echo "Mongo esta arriba. Iniciando RS $RS_NAME..."
CONFIG_FLAG=""
if [ "$RS_IS_CONFIG" == "true" ]; then CONFIG_FLAG="configsvr: true,"; fi

MEMBERS_JSON=""
IFS=',' read -ra ADDR <<< "$RS_MEMBERS"
for i in "${!ADDR[@]}"; do
    MEMBERS_JSON="${MEMBERS_JSON}{ _id: $i, host: '${ADDR[$i]}' },"
done

mongosh --port 27017 --quiet --eval "
try {
  rs.initiate({
    _id: '$RS_NAME',
    $CONFIG_FLAG
    members: [ $MEMBERS_JSON ]
  });
  print('Initiate OK');
} catch(e) {
  print('Ignorando error initiate: ' + e);
}
"

echo "Esperando a ser PRIMARY..."
until mongosh --port 27017 --quiet --eval "rs.isMaster().ismaster" 2>/dev/null | grep -q "true"; do sleep 2; done

echo "Creando usuario admin..."
mongosh --port 27017 --quiet --eval "
try {
  db.getSiblingDB('admin').createUser({
    user: 'admin',
    pwd: 'booklibre_admin_secret',
    roles: [{ role: 'root', db: 'admin' }]
  });
  print('Admin creado');
} catch(e) {
  print('Ignorando error user: ' + e);
}
"
