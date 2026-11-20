package vallegrande.edu.pe.cafecentral;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class Database {

  private final Config c;

  public Database(Config c) {
    this.c = c;
  }

  private Connection open() throws SQLException {
    String url = "jdbc:mysql://127.0.0.1:" + c.number("local.port") + "/" + c.get("db.name") + "?serverTimezone=UTC";
    return DriverManager.getConnection(url, c.get("db.user"), c.get("db.password"));
  }

  public record Product(int id, String name, BigDecimal price) {}

  public record Order(int id, String client, String product, int productId, int qty, BigDecimal total, String status) {}

  public List<Product> products() throws SQLException {
    List<Product> out = new ArrayList<>();
    String q = "SELECT id_producto, nombre, precio FROM productos WHERE stock > 0 ORDER BY nombre";
    try (Connection x = open();
         PreparedStatement s = x.prepareStatement(q);
         ResultSet r = s.executeQuery()) {
      while (r.next()) {
        out.add(new Product(r.getInt(1), r.getString(2), r.getBigDecimal(3)));
      }
    }
    return out;
  }

  public List<Order> orders() throws SQLException {
    List<Order> out = new ArrayList<>();
    String q = "SELECT p.id_pedido, p.cliente, pr.nombre, p.id_producto, p.cantidad, p.total, p.estado "
            + "FROM pedidos p JOIN productos pr ON pr.id_producto = p.id_producto ORDER BY p.id_pedido DESC";
    try (Connection x = open();
         PreparedStatement s = x.prepareStatement(q);
         ResultSet r = s.executeQuery()) {
      while (r.next()) {
        out.add(new Order(
                r.getInt(1),
                r.getString(2),
                r.getString(3),
                r.getInt(4),
                r.getInt(5),
                r.getBigDecimal(6),
                r.getString(7)
        ));
      }
    }
    return out;
  }

  public void add(String client, int product, int qty, String status) throws SQLException {
    try (Connection x = open()) {
      x.setAutoCommit(false);
      try {
        BigDecimal price;
        int stock;
        try (PreparedStatement s = x.prepareStatement("SELECT precio, stock FROM productos WHERE id_producto = ? FOR UPDATE")) {
          s.setInt(1, product);
          try (ResultSet r = s.executeQuery()) {
            if (!r.next()) {
              throw new SQLException("Producto no encontrado");
            }
            price = r.getBigDecimal(1);
            stock = r.getInt(2);
          }
        }
        if (qty < 1 || qty > stock) {
          throw new SQLException("Cantidad inválida o stock insuficiente");
        }
        try (PreparedStatement s = x.prepareStatement("INSERT INTO pedidos(cliente, id_producto, cantidad, total, estado) VALUES(?, ?, ?, ?, ?)")) {
          s.setString(1, client);
          s.setInt(2, product);
          s.setInt(3, qty);
          s.setBigDecimal(4, price.multiply(BigDecimal.valueOf(qty)));
          s.setString(5, status);
          s.executeUpdate();
        }
        try (PreparedStatement s = x.prepareStatement("UPDATE productos SET stock = stock - ? WHERE id_producto = ?")) {
          s.setInt(1, qty);
          s.setInt(2, product);
          s.executeUpdate();
        }
        x.commit();
      } catch (Exception e) {
        x.rollback();
        throw e;
      }
    }
  }

  public void edit(int id, String client, int product, int qty, String status) throws SQLException {
    if (qty < 1) {
      throw new SQLException("Cantidad debe ser mayor a cero");
    }
    String q = "UPDATE pedidos p JOIN productos pr ON pr.id_producto = ? "
            + "SET p.cliente = ?, p.id_producto = ?, p.cantidad = ?, p.total = pr.precio * ?, p.estado = ? "
            + "WHERE p.id_pedido = ?";
    try (Connection x = open(); PreparedStatement s = x.prepareStatement(q)) {
      s.setInt(1, product);
      s.setString(2, client);
      s.setInt(3, product);
      s.setInt(4, qty);
      s.setInt(5, qty);
      s.setString(6, status);
      s.setInt(7, id);
      if (s.executeUpdate() == 0) {
        throw new SQLException("Pedido no encontrado");
      }
    }
  }

  public void delete(int id) throws SQLException {
    try (Connection x = open(); PreparedStatement s = x.prepareStatement("DELETE FROM pedidos WHERE id_pedido = ?")) {
      s.setInt(1, id);
      s.executeUpdate();
    }
  }
}