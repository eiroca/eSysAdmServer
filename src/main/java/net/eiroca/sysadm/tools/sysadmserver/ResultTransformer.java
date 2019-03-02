/**
 *
 * Copyright (C) 2001-2019 eIrOcA (eNrIcO Croce & sImOnA Burzio) - AGPL >= 3.0
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
package net.eiroca.sysadm.tools.sysadmserver;

import net.eiroca.library.core.LibStr;
import net.eiroca.library.server.ResultResponse;
import spark.ResponseTransformer;

public class ResultTransformer implements ResponseTransformer {

  boolean quote = false;

  public ResultTransformer(final boolean quote) {
    this.quote = quote;
  }

  @Override
  public String render(final Object model) {
    final ResultResponse response = (ResultResponse)model;
    final StringBuilder sb = new StringBuilder(1024);
    sb.append("{");
    sb.append("\"status\":").append(response.status);
    if (response.message != null) {
      sb.append(",\"message\":");
      LibStr.encodeJson(sb, response.message);
    }
    final Object result = response.getResult();
    if (result != null) {
      sb.append(",\"result\":");
      if (quote) {
        LibStr.encodeJson(sb, String.valueOf(result));
      }
      else {
        sb.append(result);
      }
    }
    sb.append("}");
    return sb.toString();
  }

}
