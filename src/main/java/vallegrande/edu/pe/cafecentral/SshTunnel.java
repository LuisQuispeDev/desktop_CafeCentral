package vallegrande.edu.pe.cafecentral;

import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;
import java.util.Properties;

public class SshTunnel implements AutoCloseable {

  private Session session;
  private final Config c;

  public SshTunnel(Config c) {
    this.c = c;
  }

  public void start() throws Exception {
    JSch j = new JSch();
    j.addIdentity(c.get("ssh.key"));

    session = j.getSession(c.get("ssh.user"), c.get("ssh.host"), c.number("ssh.port"));

    Properties p = new Properties();
    p.put("StrictHostKeyChecking", "no");
    session.setConfig(p);

    session.connect(20000);
    session.setPortForwardingL(
            c.number("local.port"),
            c.get("rds.host"),
            c.number("rds.port")
    );
  }

  @Override
  public void close() {
    if (session != null && session.isConnected()) {
      session.disconnect();
    }
  }
}