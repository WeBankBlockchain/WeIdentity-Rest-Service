#!/bin/bash

JAVA_HOME="$JAVA_HOME"

${JAVA_HOME}/bin/java -classpath "./conf:./lib/*" com.webank.payment.contract.deploy.DeployService
if [[ $? -ne 0 ]];then
    echo "deploy contract failed."
    exit 1
fi
