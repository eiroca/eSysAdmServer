/**
 *
 * Copyright (C) 1999-2021 Enrico Croce - AGPL >= 3.0
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the
 * GNU Affero General Public License as published by the Free Software Foundation, either version 3
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License along with this program.
 * If not, see <http://www.gnu.org/licenses/>.
 *
 **/
package net.eiroca.sysadm.tools.sysadmserver.trace.exporter;

import java.text.SimpleDateFormat;
import java.util.Base64;
import java.util.Base64.Encoder;
import java.util.Date;
import java.util.UUID;
import net.eiroca.ext.library.elastic.ElasticBulk;
import net.eiroca.library.config.parameter.IntegerParameter;
import net.eiroca.library.config.parameter.StringParameter;
import net.eiroca.library.core.Helper;
import net.eiroca.library.core.LibStr;
import net.eiroca.library.system.IContext;

public class ElasticTraceExporter extends GenericTraceExporter {

  private static final Encoder BASE64ENCODER = Base64.getEncoder();

  public static final String ID = "elastic".toLowerCase();
  //
  public static StringParameter _elasticURL = new StringParameter(ElasticTraceExporter.config, "elasticURL", null);
  public static StringParameter _elasticUsername = new StringParameter(ElasticTraceExporter.config, "elasticUsername", null);
  public static StringParameter _elasticPassword = new StringParameter(ElasticTraceExporter.config, "elasticPassword", null);
  public static StringParameter _elasticIndex = new StringParameter(ElasticTraceExporter.config, "elasticIndex", "flume-");
  public static StringParameter _indexDateFormat = new StringParameter(ElasticTraceExporter.config, "indexDateFormat", "yyyy.MM.dd");
  public static StringParameter _elasticType = new StringParameter(ElasticTraceExporter.config, "elasticType", "metric");
  public static IntegerParameter _elasticVersion = new IntegerParameter(ElasticTraceExporter.config, "elasticVersion", 7);
  public static StringParameter _elasticPipeline = new StringParameter(ElasticTraceExporter.config, "elasticPipeline", null);
  // Dynamic mapped to parameters
  protected String config_elasticURL;
  protected String config_elasticUsername;
  protected String config_elasticPassword;
  protected String config_elasticIndex;
  protected String config_indexDateFormat;
  protected String config_elasticType;
  protected int config_elasticVersion;
  protected String config_elasticPipeline;
  //
  protected ElasticBulk elasticServer = null;
  protected SimpleDateFormat indexDateFormat;

  public ElasticTraceExporter() {
    super();
  }

  @Override
  public void setup(final IContext context) throws Exception {
    super.setup(context);
    GenericTraceExporter.config.convert(context, GenericTraceExporter.CONFIG_PREFIX, this, "config_");
    indexDateFormat = new SimpleDateFormat(config_indexDateFormat);
    elasticServer = LibStr.isNotEmptyOrNull(config_elasticURL) ? new ElasticBulk(config_elasticURL, config_elasticVersion) : null;
    if (elasticServer != null) {
      if (config_elasticUsername != null) {
        final String credential = config_elasticUsername + ":" + config_elasticPassword;
        final String auth = ElasticTraceExporter.BASE64ENCODER.encodeToString(credential.getBytes());
        elasticServer.setAuthorization("Basic " + auth);
      }
      context.info("Elastic Search: " + config_elasticURL + (config_elasticUsername != null ? " Auth: " + config_elasticUsername : ""));
      elasticServer.open();
    }
  }

  @Override
  public void teardown() throws Exception {
    super.teardown();
    if (elasticServer != null) {
      elasticServer.close();
      elasticServer = null;
    }
  }

  @Override
  public boolean beginBulk() {
    return (elasticServer != null);
  }

  @Override
  public void endBulk() {
    try {
      elasticServer.flush();
    }
    catch (final Exception e) {
      context.error("Error flushing to elastic: " + e.getMessage(), e);
    }
  }

  @Override
  public void process(final String trace) {
    try {
      final String _id = getEventID();
      final String _indexName = getEventIndex();
      elasticServer.add(_indexName, config_elasticType, _id, config_elasticPipeline, trace);
    }
    catch (final Exception e) {
      context.error("Error exporting ", trace, "->", e.getMessage(), " ", Helper.getStackTraceAsString(e));
    }
  }

  private String getEventIndex() {
    String index;
    index = config_elasticIndex + indexDateFormat.format(new Date());
    return index;
  }

  private String getEventID() {
    return UUID.randomUUID().toString();
  }

  @Override
  public String getId() {
    return ElasticTraceExporter.ID;
  }

}
