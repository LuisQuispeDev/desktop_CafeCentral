package vallegrande.edu.pe.cafecentral;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.List;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;
import javax.swing.table.DefaultTableModel;

public class Main {

  private final Config config;
  private final SshTunnel tunnel;
  private final Database db;

  private JFrame frame;
  private JTable table;
  private DefaultTableModel model;

  private Main() throws Exception {
    config = new Config();
    tunnel = new SshTunnel(config);
    tunnel.start();
    db = new Database(config);
    SwingUtilities.invokeLater(this::ui);
  }

  private void ui() {
    frame = new JFrame("Café Central - Gestión de pedidos");
    frame.setSize(920, 540);
    frame.setLocationRelativeTo(null);
    frame.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
    frame.addWindowListener(new WindowAdapter() {
      @Override
      public void windowClosing(WindowEvent e) {
        tunnel.close();
        frame.dispose();
        System.exit(0);
      }
    });

    JLabel title = new JLabel("  Café Central | Pedidos");
    title.setFont(new Font("Arial", Font.BOLD, 22));
    title.setForeground(new Color(23, 107, 69));

    model = new DefaultTableModel(
            new Object[]{"ID", "Cliente", "Producto", "ID producto", "Cantidad", "Total", "Estado"},
            0
    ) {
      @Override
      public boolean isCellEditable(int r, int c) {
        return false;
      }
    };

    table = new JTable(model);
    table.setRowHeight(25);

    JButton add = new JButton("Nuevo pedido");
    JButton edit = new JButton("Editar seleccionado");
    JButton del = new JButton("Eliminar seleccionado");
    JButton refresh = new JButton("Actualizar / Refrescar tabla");

    add.addActionListener(e -> addOrder());
    edit.addActionListener(e -> editOrder());
    del.addActionListener(e -> deleteOrder());
    refresh.addActionListener(e -> refresh());

    JPanel buttons = new JPanel();
    buttons.add(add);
    buttons.add(edit);
    buttons.add(del);
    buttons.add(refresh);

    frame.add(title, BorderLayout.NORTH);
    frame.add(new JScrollPane(table), BorderLayout.CENTER);
    frame.add(buttons, BorderLayout.SOUTH);

    refresh();
    frame.setVisible(true);
  }

  private void refresh() {
    try {
      model.setRowCount(0);
      for (Database.Order o : db.orders()) {
        model.addRow(new Object[]{
                o.id(),
                o.client(),
                o.product(),
                o.productId(),
                o.qty(),
                o.total(),
                o.status()
        });
      }
    } catch (Exception e) {
      error(e);
    }
  }

  private JComboBox<Database.Product> productBox(List<Database.Product> ps) {
    JComboBox<Database.Product> b = new JComboBox<>(ps.toArray(new Database.Product[0]));
    b.setRenderer((l, v, i, s, f) ->
            new JLabel(v == null ? "" : v.name() + " (ID " + v.id() + ") · S/ " + v.price())
    );
    return b;
  }

  private void addOrder() {
    try {
      List<Database.Product> ps = db.products();
      if (ps.isEmpty()) {
        throw new Exception("No hay productos con stock.");
      }

      JTextField client = new JTextField();
      JTextField qty = new JTextField("1");
      JComboBox<Database.Product> prod = productBox(ps);
      JComboBox<String> status = new JComboBox<>(
              new String[]{"Pendiente", "En preparación", "Entregado", "Cancelado"}
      );

      Object[] f = {
              "Cliente", client,
              "Producto", prod,
              "Cantidad", qty,
              "Estado", status
      };

      if (JOptionPane.showConfirmDialog(frame, f, "Nuevo pedido", JOptionPane.OK_CANCEL_OPTION) == JOptionPane.OK_OPTION) {
        db.add(
                client.getText().trim(),
                ((Database.Product) prod.getSelectedItem()).id(),
                Integer.parseInt(qty.getText()),
                (String) status.getSelectedItem()
        );
        refresh();
      }
    } catch (Exception e) {
      error(e);
    }
  }

  private void editOrder() {
    int r = table.getSelectedRow();
    if (r < 0) {
      JOptionPane.showMessageDialog(frame, "Selecciona un pedido.");
      return;
    }

    try {
      int id = (int) model.getValueAt(r, 0);
      String oldClient = (String) model.getValueAt(r, 1);
      int oldProduct = (int) model.getValueAt(r, 3);
      int oldQty = (int) model.getValueAt(r, 4);
      String oldStatus = (String) model.getValueAt(r, 6);

      List<Database.Product> ps = db.products();
      JTextField client = new JTextField(oldClient);
      JTextField qty = new JTextField("" + oldQty);
      JComboBox<Database.Product> prod = productBox(ps);

      for (int i = 0; i < prod.getItemCount(); i++) {
        if (prod.getItemAt(i).id() == oldProduct) {
          prod.setSelectedIndex(i);
          break;
        }
      }

      JComboBox<String> status = new JComboBox<>(
              new String[]{"Pendiente", "En preparación", "Entregado", "Cancelado"}
      );
      status.setSelectedItem(oldStatus);

      Object[] f = {
              "Cliente", client,
              "Producto", prod,
              "Cantidad", qty,
              "Estado", status
      };

      if (JOptionPane.showConfirmDialog(frame, f, "Editar pedido", JOptionPane.OK_CANCEL_OPTION) == JOptionPane.OK_OPTION) {
        db.edit(
                id,
                client.getText().trim(),
                ((Database.Product) prod.getSelectedItem()).id(),
                Integer.parseInt(qty.getText()),
                (String) status.getSelectedItem()
        );
        refresh();
      }
    } catch (Exception e) {
      error(e);
    }
  }

  private void deleteOrder() {
    int r = table.getSelectedRow();
    if (r < 0) {
      JOptionPane.showMessageDialog(frame, "Selecciona un pedido.");
      return;
    }

    int id = (int) model.getValueAt(r, 0);
    if (JOptionPane.showConfirmDialog(
            frame,
            "¿Eliminar pedido #" + id + "?",
            "Confirmar",
            JOptionPane.YES_NO_OPTION
    ) == JOptionPane.YES_OPTION) {
      try {
        db.delete(id);
        refresh();
      } catch (Exception e) {
        error(e);
      }
    }
  }

  private void error(Exception e) {
    JOptionPane.showMessageDialog(frame, e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
  }

  public static void main(String[] args) {
    try {
      new Main();
    } catch (Exception e) {
      JOptionPane.showMessageDialog(null, e.getMessage(), "No se pudo iniciar", JOptionPane.ERROR_MESSAGE);
      System.exit(1);
    }
  }
}