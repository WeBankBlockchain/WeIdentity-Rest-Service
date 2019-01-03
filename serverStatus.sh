#!/bin/sh

# get JAVA_HOME conf from server.env
TEMP_JAVA_HOME=`cat server.env | grep JAVA_HOME | cut -d':' -f4`
# jdk path
JAVA_HOME="${TEMP_JAVA_HOME}"

# Java main
APP_MAIN=com.webank.weid.http.Application

tradePortalPID=0

getTradeProtalPID(){
    javaps=`$JAVA_HOME/bin/jps -l | grep $APP_MAIN`
    if [ -n "$javaps" ]; then
        tradePortalPID=`echo $javaps | awk '{print $1}'`
    else
        tradePortalPID=0
    fi
}

getServerStatus(){
    getTradeProtalPID
    echo "==============================================================================================="
    if [ $tradePortalPID -ne 0 ]; then
        echo "$APP_MAIN is running(PID=$tradePortalPID)"
        echo "==============================================================================================="
    else
        echo "$APP_MAIN is not running"
        echo "==============================================================================================="
    fi
}

getServerStatus