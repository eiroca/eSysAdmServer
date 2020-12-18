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

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;

public class Configuration {

  private static final String APPEND_SUFFIX = "[]";
  final private Map<String, Properties> configuration = new HashMap<>();

  public Configuration(final String prefix, final Properties systemConfig) {
    readConfiguration(prefix, systemConfig);
  }

  public void readConfiguration(final String prefix, final Properties systemConfig) {
    configuration.clear();
    for (final Entry<Object, Object> entry : systemConfig.entrySet()) {
      final String name = String.valueOf(entry.getKey());
      final Object value = entry.getValue();
      if ((prefix != null) && !name.startsWith(prefix)) {
        continue;
      }
      String configSection = (prefix != null) ? name.substring(prefix.length()) : name;
      String configName;
      final int pos = configSection.indexOf('.');
      if (pos > 0) {
        configName = configSection.substring(pos + 1);
        configSection = configSection.substring(0, pos);
      }
      else {
        configName = configSection;
        configSection = "*";
      }
      boolean append;
      if (configName.endsWith(Configuration.APPEND_SUFFIX)) {
        append = true;
        configName = configName.substring(0, configName.length() - Configuration.APPEND_SUFFIX.length());
      }
      else {
        append = false;
      }
      final Properties props = get(configSection, true);
      setProperty(props, configName, value, append ? " " : null);
    }
  }

  public Properties get(final String section) {
    return configuration.get(section);
  }

  public synchronized Properties get(final String section, final boolean create) {
    Properties p = configuration.get(section);
    if ((p == null) && create) {
      p = new Properties();
      configuration.put(section, p);
    }
    return p;
  }

  public void setProperty(final Properties props, final String key, final Object value, final String sep) {
    if (sep == null) {
      if (value == null) {
        props.remove(key);
      }
      else {
        props.put(key, value);
      }
    }
    else {
      Object old = props.get(key);
      if ((old == null) || (value == null)) {
        old = value;
      }
      else {
        old = String.valueOf(old) + sep + String.valueOf(value);
      }
      if (old == null) {
        props.remove(key);
      }
      else {
        props.put(key, old);
      }
    }
  }

  public void update(final String section, final Properties config) {
    final Properties p = configuration.get(section);
    if (p != null) {
      // Step 1 - add missing
      for (final Entry<Object, Object> entry : p.entrySet()) {
        final String name = String.valueOf(entry.getKey());
        final Object value = entry.getValue();
        if (value == null) {
          continue;
        }
        if (!config.containsKey(name)) {
          config.setProperty(name, value.toString());
        }
      }
      // Step 2 - resolve array append
      for (final Entry<Object, Object> entry : config.entrySet()) {
        final String name = String.valueOf(entry.getKey());
        if (name.endsWith(Configuration.APPEND_SUFFIX)) {
          final String key = name.substring(0, name.length() - Configuration.APPEND_SUFFIX.length());
          final Object value = entry.getValue();
          if (value == null) {
            continue;
          }
          setProperty(config, key, value, " ");
        }
      }
    }
  }

}
