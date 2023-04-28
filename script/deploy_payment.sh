#!/bin/bash

JAVA_HOME="$JAVA_HOME"

if [ -f conf/payment.properties ];then
    echo "Err: the conf/payment.properties is exists"
    exit 1
fi

key=$(cat keys/priv/private_key)

${JAVA_HOME}/bin/java -classpath "./conf:./lib/*" com.webank.payment.contract.deploy.DeployService ${key}
if [[ $? -ne 0 ]];then
    echo "deploy contract failed."
    exit 1
fi

if [ -f payment.properties ];then
    cp payment.properties ./conf/
    rm payment.properties
    rm private_key
fi
