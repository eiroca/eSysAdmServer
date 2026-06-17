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
package net.eiroca.sysadm.tools.sysadmserver.handler;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import inet.ipaddr.IPAddressString;
import io.javalin.http.Context;
import net.eiroca.library.core.LibStr;
import net.eiroca.library.csv.CSV;
import net.eiroca.library.csv.CSVData;
import net.eiroca.sysadm.tools.sysadmserver.SystemContext;
import net.eiroca.sysadm.tools.sysadmserver.collector.GenericRuleBasedHandler;
import net.eiroca.sysadm.tools.sysadmserver.manager.CollectorManager;

public class UserRoleHandler extends GenericRuleBasedHandler<UserRoleConfig> {

  private static final String ESYSADM_TOKEN_HEADER = "X-eSysAdm-TOKEN";

  private static final String ANY_VALUE = "*";

  private final List<UserRoleMapping> mapping = new ArrayList<>();

  @Override
  public void init(final Properties config) throws Exception {
    loadRules(SystemContext.config.user_roles_path);
    readMapping();
  }

  private void readMapping() {
    final CSVData mappingCSV = new CSVData(SystemContext.config.user_roles_mapping_path.toString(), '\t', CSV.QUOTE, CSV.COMMENT, CSV.ENCODING);
    for (int i = 0; i < mappingCSV.size(); i++) {
      final String[] data = mappingCSV.getData(i);
      String str;
      IPAddressString network = null;
      str = data[0];
      if (LibStr.isNotEmptyOrNull(str) && (!str.equals(UserRoleHandler.ANY_VALUE))) {
        network = new IPAddressString(str);
      }
      String token = null;
      str = data[1];
      if (LibStr.isNotEmptyOrNull(str) && (!str.equals(UserRoleHandler.ANY_VALUE))) {
        token = str;
      }
      final String role = data[2];
      final UserRoleMapping rule = new UserRoleMapping(network, token, role);
      mapping.add(rule);
      CollectorManager.logger.debug(rule.toString());
    }
  }

  public UserRoleConfig getRole(final Context ctx) {
    final IPAddressString req_ip = new IPAddressString(ctx.ip());
    final String req_token = ctx.req().getHeader(UserRoleHandler.ESYSADM_TOKEN_HEADER);
    for (final UserRoleMapping rule : mapping) {
      final IPAddressString network = rule.getNetwork();
      final String token = rule.getToken();
      CollectorManager.logger.debug(MessageFormat.format("Checking {0}/{1} with {2}/{3}", req_ip, req_token, network, token));
      if ((network == null) || (network.contains(req_ip))) {
        if ((token == null) || (token.equals(req_token))) {
          final String role = rule.getRole();
          final UserRoleConfig userRule = rules.get(role);
          CollectorManager.logger.debug(MessageFormat.format("{0}/{1} -> {2}", req_ip, req_token, role));
          CollectorManager.logger.debug("Rule: " + userRule);
          return userRule;

        }
      }
    }
    return rules.get(SystemContext.config.user_roles_default);
  }

  @Override
  protected UserRoleConfig createRule(final String name, final Properties config) {
    return new UserRoleConfig(name, config);
  }

}
