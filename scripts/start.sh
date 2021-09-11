ME=eSysAdm
CONFIG=config_ETL
ETL_DIR=/opt/SP/app/ETL/$ME
ETL_CMD="java -Djava.io.tmpdir=${ETL_DIR}/tmp -Dlog4j.configurationFile=${ETL_DIR}/${CONFIG}/log4j2.xml -Doracle.jdbc.Trace=true -Djava.util.logging.config.file=${ETL_DIR}/config/log.properties -jar ${ETL_DIR}/eSysAdmServer.jar"
echo "`date` $ME begin"
${ETL_CMD} ${CONFIG}
