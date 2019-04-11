package net.eiroca.sysadm.tools.sysadmserver.util.params;

import java.net.InetAddress;
import net.eiroca.library.config.Parameters;
import net.eiroca.library.config.parameter.StringParameter;

public final class HostnameParameter extends StringParameter {

  public HostnameParameter(final Parameters owner, final String paramName, final String paramDef) {
    super(owner, paramName, paramDef);
  }

  @Override
  public String getDefault() {
    try {
      return InetAddress.getLocalHost().getHostName();
    }
    catch (final Exception e) {
      return super.getDefault();
    }
  }
}
