#!/usr/bin/env sh

FALLIDO=0

for i in ./src/main/resources/assets/dragonminez/lang/*.json
do
    if ! jq . "${i}" > /dev/null
    then
        echo "El Archivo ${i} es un JSON con formato incorrecto."
        FALLIDO=1
    fi
done

if [ "${FALLIDO}" -ne 0 ]
then
    exit 1
else
    echo "Archivos de idioma perfectos pa"
fi
