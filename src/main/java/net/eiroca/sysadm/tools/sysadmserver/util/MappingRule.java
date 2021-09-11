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
package net.eiroca.sysadm.tools.sysadmserver.util;

import inet.ipaddr.IPAddressString;

public class MappingRule {

  IPAddressString network;
  String token;
  String role;

  public IPAddressString getNetwork() {
    return network;
  }

  public String getToken() {
    return token;
  }

  public String getRole() {
    return role;
  }

  public MappingRule(final IPAddressString network, final String token, final String role) {
    super();
    this.network = network;
    this.token = token;
    this.role = role;
  }

}
