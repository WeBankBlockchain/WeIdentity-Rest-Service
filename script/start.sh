#!/bin/sh

# get JAVA_HOME conf from server.env
#TEMP_JAVA_HOME=`cat server.env | grep JAVA_HOME | cut -d':' -f4`
# jdk path
JAVA_HOME="$JAVA_HOME"

CURRENT_DIR=`pwd`
APP_NAME=`basename ${CURRENT_DIR}`

CONF_DIR=./app/${APP_NAME}/conf
LOG_DIR=./logs/${APP_NAME}

# Java main
APP_MAIN=com.webank.weid.http.Application
CLASSPATH='conf/:apps/*:lib/*'

tradePortalPID=0

getTradeProtalPID(){
    javaps=`$JAVA_HOME/bin/jps -l | grep $APP_MAIN`
    if [ -n "$javaps" ]; then
        tradePortalPID=`echo $javaps | awk '{print $1}'`
    else
        tradePortalPID=0
    fi
}

JAVA_OPTS=" -Dfile.encoding=UTF-8"
JAVA_OPTS+=" -Dlog4j.configurationfile=${CONF_DIR}/log4j2.xml -Dindex.log.home=${LOG_DIR} -Dconfig=${CONF_DIR}/"
JAVA_OPTS+=" -server -Xmx1024m -Xms1024m -XX:NewSize=256m -XX:MaxNewSize=256m -XX:PermSize=128m -XX:MaxPermSize=128m"
JAVA_OPTS+=" -XX:+UseConcMarkSweepGC -XX:+UseCMSCompactAtFullCollection -XX:CMSInitiatingOccupancyFraction=70"
JAVA_OPTS+=" -XX:+CMSParallelRemarkEnabled -XX:SoftRefLRUPolicyMSPerMB=0 -XX:+CMSClassUnloadingEnabled -XX:+DisableExplicitGC -XX:SurvivorRatio=8"
JAVA_OPTS+=" -XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=${LOG_DIR}/ -XX:ErrorFile=${LOG_DIR}/heap_error.log"

startup(){
    getTradeProtalPID
    echo "==============================================================================================="
    if [ $tradePortalPID -ne 0 ]; then
        echo "$APP_MAIN already started(PID=$tradePortalPID)"
        echo "==============================================================================================="
    else
        echo -n "Starting $APP_MAIN ..."
        nohup $JAVA_HOME/bin/java ${JAVA_OPTS} -cp $CLASSPATH $APP_MAIN >> /dev/null 2>&1 &
        getTradeProtalPID
        if [ $tradePortalPID -ne 0 ]; then
            echo "(PID=$tradePortalPID)...[Success]"
            echo "==============================================================================================="
        else
            echo "[Failed]"
            echo "==============================================================================================="
        fi
    fi
}

startup