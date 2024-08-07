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
package net.eiroca.sysadm.tools.sysadmserver.manager;

import java.nio.file.Path;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import inet.ipaddr.IPAddressString;
import net.eiroca.library.core.Helper;
import net.eiroca.library.core.LibStr;
import net.eiroca.library.csv.CSV;
import net.eiroca.library.csv.CSVData;
import net.eiroca.sysadm.tools.sysadmserver.SystemContext;
import net.eiroca.sysadm.tools.sysadmserver.util.MappingRule;
import net.eiroca.sysadm.tools.sysadmserver.util.UserRole;
import spark.Request;

public class RoleManager extends GenericRuleBasedManager<UserRole> {

  private static final String ESYSADM_TOKEN_HEADER = "X-eSysAdm-TOKEN";

  private static final String ANY_VALUE = "*";

  private final List<MappingRule> mapping = new ArrayList<>();

  @Override
  public void start() throws Exception {
    super.start();
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
      if (LibStr.isNotEmptyOrNull(str) && (!str.equals(RoleManager.ANY_VALUE))) {
        network = new IPAddressString(str);
      }
      String token = null;
      str = data[1];
      if (LibStr.isNotEmptyOrNull(str) && (!str.equals(RoleManager.ANY_VALUE))) {
        token = str;
      }
      final String role = data[2];
      MappingRule rule = new MappingRule(network, token, role);
      mapping.add(rule);
      CollectorManager.logger.debug(rule.toString());
    }
  }

  @Override
  public void stop() throws Exception {
    super.stop();
  }

  public UserRole getRole(final Request request) {
    final IPAddressString req_ip = new IPAddressString(request.ip());
    final String req_token = request.headers(RoleManager.ESYSADM_TOKEN_HEADER);
    for (final MappingRule rule : mapping) {
      final IPAddressString network = rule.getNetwork();
      final String token = rule.getToken();
      CollectorManager.logger.debug(MessageFormat.format("Checking {0}/{1} with {2}/{3}", req_ip, req_token, network, token));
      if ((network == null) || (network.contains(req_ip))) {
        if ((token == null) || (token.equals(req_token))) {
          final String role = rule.getRole();
          return roles.get(role);
        }
      }
    }
    return roles.get(SystemContext.config.user_roles_default);
  }

  @Override
  protected void createRule(final Path confPath) {
    try {
      String name = confPath.getFileName().toString();
      name = name.substring(0, name.length() - GenericRuleBasedManager.RULE_FILEEXT.length());
      if (LibStr.isNotEmptyOrNull(name)) {
        final Properties config = Helper.loadProperties(confPath.toString(), false);
        final UserRole role = new UserRole(name, config);
        roles.put(name, role);
      }
    }
    catch (final Exception e) {
      SystemContext.logger.error(confPath + " Error: ", e);
    }
  }

}
