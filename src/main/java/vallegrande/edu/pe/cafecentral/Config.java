package vallegrande.edu.pe.cafecentral;

import java.io.InputStream;
import java.util.Properties;

public class Config {

  private final Properties p = new Properties();

  public Config() throws Exception {
    try (InputStream in = getClass().getClassLoader().getResourceAsStream("config.properties")) {
      if (in == null) {
        throw new IllegalStateException(
                "Copia config.properties.example como config.properties y completa tus datos."
        );
      }
      p.load(in);
    }
  }

  public String get(String k) {
    String v = p.getProperty(k);
    if (v == null || v.isBlank()) {
      throw new IllegalArgumentException("Falta configurar " + k);
    }
    return v.trim();
  }

  public int number(String k) {
    return Integer.parseInt(get(k));
  }
}