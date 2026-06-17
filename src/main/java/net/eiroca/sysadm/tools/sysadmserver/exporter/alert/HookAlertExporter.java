/**
 *
 * Copyright (C) 1999-2026 Enrico Croce - AGPL >= 3.0
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
package net.eiroca.sysadm.tools.sysadmserver.exporter.alert;

import java.util.ArrayList;
import java.util.Collection;
import org.apache.http.Header;
import org.apache.http.HttpHost;
import org.apache.http.entity.ContentType;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.message.BasicHeader;
import net.eiroca.ext.library.http.HttpClientHelper;
import net.eiroca.library.config.parameter.IntegerParameter;
import net.eiroca.library.config.parameter.StringParameter;
import net.eiroca.library.core.Helper;
import net.eiroca.sysadm.tools.sysadmserver.event.Alert;
import net.eiroca.sysadm.tools.sysadmserver.handler.AlertHandlerContext;
import net.eiroca.sysadm.tools.sysadmserver.manager.CollectorManager;

public class HookAlertExporter extends GenericAlertExporter {

  public static final String ID = "hook".toLowerCase();

  protected static transient StringParameter _hookUrl = new StringParameter(config, "url", null);
  protected static transient StringParameter _hookType = new StringParameter(config, "type", "application/json");
  protected static transient StringParameter _hookToken = new StringParameter(config, "token", null);
  protected static transient StringParameter _hookHeader = new StringParameter(config, "header", "Authorization");
  protected static transient StringParameter _hookProxyHost = new StringParameter(config, "proxyhost", null);
  protected static transient IntegerParameter _hookProxyPort = new IntegerParameter(config, "proxypost", 8080);

  public String config_type;
  public String config_url;
  public String config_token;
  public String config_header;
  public String config_proxyhost;
  public int config_proxyport;

  public transient HttpHost hookProxy = null;

  public HookAlertExporter(String prefix) {
    super(prefix);
  }

  @Override
  public String getId() {
    return ID;
  }

  @Override
  public void setup(final AlertHandlerContext context) throws Exception {
    super.setup(context);
    if (config_proxyhost != null) {
      hookProxy = new HttpHost(config_proxyhost, config_proxyport);
    }
    else {
      hookProxy = null;
    }
  }

  @Override
  public void process(final Alert a) {
    final String msg = getTextFromAlert(a);
    Collection<Header> headers = null;
    if (config_token != null) {
      headers = new ArrayList<>();
      headers.add(new BasicHeader(config_header, config_token));
    }
    final CloseableHttpClient client = HttpClientHelper.getHttpClient(hookProxy, headers);
    CollectorManager.logger.debug("alert webhook()");
    final String _doc = msg;
    final String url = config_url;
    final String r = HttpClientHelper.POST(client, url, _doc, ContentType.parse(config_type));
    CollectorManager.logger.debug("Doc: " + _doc);
    CollectorManager.logger.debug("POST " + url + " --> " + r);
    Helper.close(client);
  }

}
