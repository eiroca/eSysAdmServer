#!/bin/bash

ME=eSysAdm
CONFIG=config
ETL_DIR=/opt/SP/app/ETL/$ME
ETL_TMP=/opt/SP/app/ETL/$ME/tmp

cd $ETL_DIR
mkdir -p $ETL_TMP

ETL_OPTS="-Djava.io.tmpdir=$ETL_TMP"
ETL_OPTS="$ETL_OPTS -Dlog4j.configurationFile=${CONFIG}/log4j2.xml"
ETL_OPTS="$ETL_OPTS -Djava.util.logging.config.file=${CONFIG}/log.properties"
ETL_OPTS="$ETL_OPTS -Doracle.jdbc.Trace=true"

ETL_CMD="/usr/bin/java $ETL_OPTS -jar eSysAdmServer.jar $CONFIG"

echo "`date` $ME begin"

${ETL_CMD} >> $ETL_TMP/$ME.log 2>&1 &
