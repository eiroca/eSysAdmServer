/**
 *
 * Copyright (C) 1999-2019 Enrico Croce - AGPL >= 3.0
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

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import org.apache.commons.codec.binary.Base64;
import net.eiroca.library.csv.CSVMap;
import net.eiroca.library.sysadm.monitoring.sdk.ICredentialProvider;

public class CredentialStore implements ICredentialProvider {

  final private transient Map<String, String> keyStore = new HashMap<>();

  public CredentialStore(final Path defPath) {
    final CSVMap csv = new CSVMap(defPath.toString(), '\t', '"', '#', "UTF-8");
    for (final String user : csv.getKeys()) {
      String pwd = csv.getData(user);
      final Base64 base64 = new Base64();
      pwd = new String(base64.decode(pwd.getBytes()));
      keyStore.put(user, pwd);
    }
  }

  @Override
  public String getPlainPassword(final String key) {
    return keyStore.get(key);
  }

}
