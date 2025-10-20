import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.*;
import java.sql.*;
import java.time.*;
import java.time.format.DateTimeFormatter;
import com.itextpdf.text.*;
import com.itextpdf.text.pdf.PdfWriter;
import java.io.FileOutputStream;

public class PharmacyInventorySystem extends JFrame {

    private JPanel contentPane;
    private JTable stockTable;
    private DefaultTableModel stockModel;
    private JTextField tfName, tfBatch, tfQuantity, tfPrice, tfSearch;
    private JFormattedTextField tfExpiry;
    private Connection con;

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new PharmacyInventorySystem().setVisible(true));
    }

    public PharmacyInventorySystem() {
        setTitle("💊 Pharmacy Inventory System");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setBounds(100, 100, 1100, 650);
        contentPane = new JPanel();
        contentPane.setBorder(new EmptyBorder(15, 15, 15, 15));
        contentPane.setLayout(new BorderLayout(15, 15));
        contentPane.setBackground(new Color(245, 245, 250));
        setContentPane(contentPane);

        connectDatabase();
        initUI();
        loadStockTable();
    }

    private void connectDatabase() {
        try {
            con = DriverManager.getConnection(
                    "jdbc:mysql://localhost:3306/pharmacy_db",
                    "root",
                    "Haunted@123"
            );
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Database connection failed: " + e.getMessage());
            System.exit(1);
        }
    }

    private void initUI() {
        // ==================== LEFT PANEL: Add Medicine ====================
        JPanel addPanel = new JPanel();
        addPanel.setLayout(new GridBagLayout());
        addPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(new Color(100, 149, 237), 2),
                "➕ Add New Medicine", 0, 0, new java.awt.Font("Segoe UI", java.awt.Font.BOLD, 16), new Color(25, 25, 112)));
        addPanel.setBackground(new Color(230, 240, 255));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8,8,8,8);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        tfName = new JTextField(); tfBatch = new JTextField(); tfQuantity = new JTextField(); tfPrice = new JTextField();
        tfExpiry = new JFormattedTextField(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        tfExpiry.setValue(LocalDate.now());

        String[] labels = {"Name:", "Batch No:", "Expiry (yyyy-MM-dd):", "Quantity:", "Price:"};
        JComponent[] fields = {tfName, tfBatch, tfExpiry, tfQuantity, tfPrice};

        for (int i=0; i<labels.length; i++) {
            gbc.gridx = 0; gbc.gridy = i; addPanel.add(new JLabel(labels[i]), gbc);
            gbc.gridx = 1; gbc.gridy = i; addPanel.add(fields[i], gbc);
            fields[i].setFont(new java.awt.Font("Segoe UI", java.awt.Font.PLAIN, 14));
        }

        JButton btnAdd = new JButton("Add Medicine");
        btnAdd.setBackground(new Color(65,105,225)); btnAdd.setForeground(Color.white);
        btnAdd.setFocusPainted(false); btnAdd.setFont(new java.awt.Font("Segoe UI", java.awt.Font.BOLD, 14));
        btnAdd.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnAdd.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) { btnAdd.setBackground(new Color(100,149,237)); }
            public void mouseExited(java.awt.event.MouseEvent evt) { btnAdd.setBackground(new Color(65,105,225)); }
        });

        gbc.gridx = 0; gbc.gridy = labels.length; gbc.gridwidth = 2;
        addPanel.add(btnAdd, gbc);
        btnAdd.addActionListener(e -> addMedicine());

        contentPane.add(addPanel, BorderLayout.WEST);

        // ==================== CENTER PANEL: Stock Table ====================
        JPanel centerPanel = new JPanel(new BorderLayout(10,10));
        centerPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(new Color(100,149,237), 2),
                "📦 Stock", 0, 0, new java.awt.Font("Segoe UI", java.awt.Font.BOLD, 16), new Color(25,25,112)));
        centerPanel.setBackground(new Color(245,245,250));

        stockModel = new DefaultTableModel(new Object[]{"ID","Name","Batch","Expiry","Qty","Price"},0) {
            public boolean isCellEditable(int row, int column){ return false; }
        };
        stockTable = new JTable(stockModel);
        stockTable.setRowHeight(28);
        stockTable.setFont(new java.awt.Font("Segoe UI", java.awt.Font.PLAIN, 14));
        stockTable.getTableHeader().setFont(new java.awt.Font("Segoe UI", java.awt.Font.BOLD, 15));
        stockTable.getTableHeader().setBackground(new Color(65,105,225));
        stockTable.getTableHeader().setForeground(Color.white);
        stockTable.setGridColor(new Color(200,200,200));

        stockTable.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                                                           boolean isSelected, boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                int qty = Integer.parseInt(table.getValueAt(row,4).toString());
                LocalDate expiry = LocalDate.parse(table.getValueAt(row,3).toString());
                if (qty <= 5) c.setBackground(new Color(255,200,200)); // low stock
                else if (!expiry.isAfter(LocalDate.now().plusDays(30))) c.setBackground(new Color(255,255,180)); // near expiry
                else if(row%2==0) c.setBackground(new Color(245,245,250));
                else c.setBackground(new Color(230,230,250));
                return c;
            }
        });

        JScrollPane scrollPane = new JScrollPane(stockTable);
        centerPanel.add(scrollPane, BorderLayout.CENTER);

        // Search bar
        JPanel searchPanel = new JPanel(new BorderLayout(5,5));
        searchPanel.setBackground(new Color(245,245,250));
        tfSearch = new JTextField(); tfSearch.setFont(new java.awt.Font("Segoe UI", java.awt.Font.PLAIN, 14));
        JButton btnSearch = new JButton("🔍 Search"); btnSearch.setBackground(new Color(65,105,225)); btnSearch.setForeground(Color.white);
        btnSearch.setFocusPainted(false); btnSearch.setCursor(new Cursor(Cursor.HAND_CURSOR));
        searchPanel.add(tfSearch, BorderLayout.CENTER); searchPanel.add(btnSearch, BorderLayout.EAST);
        centerPanel.add(searchPanel, BorderLayout.NORTH);

        tfSearch.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { searchStock(); }
            public void removeUpdate(DocumentEvent e) { searchStock(); }
            public void changedUpdate(DocumentEvent e) { searchStock(); }
        });

        btnSearch.addActionListener(e -> searchStock());

        contentPane.add(centerPanel, BorderLayout.CENTER);

        // ==================== BOTTOM PANEL: POS & Report ====================
        JPanel bottomPanel = new JPanel();
        bottomPanel.setBackground(new Color(245,245,250));
        JButton btnSell = new JButton("💵 Sell Selected");
        JButton btnReport = new JButton("📄 Export PDF");

        Color btnColor = new Color(34,139,34);
        btnSell.setBackground(btnColor); btnSell.setForeground(Color.white);
        btnReport.setBackground(new Color(255,140,0)); btnReport.setForeground(Color.white);

        btnSell.setFocusPainted(false); btnReport.setFocusPainted(false);
        btnSell.setFont(new java.awt.Font("Segoe UI", java.awt.Font.BOLD, 14)); 
        btnReport.setFont(new java.awt.Font("Segoe UI", java.awt.Font.BOLD, 14));
        btnSell.setCursor(new Cursor(Cursor.HAND_CURSOR)); 
        btnReport.setCursor(new Cursor(Cursor.HAND_CURSOR));

        bottomPanel.add(btnSell); bottomPanel.add(btnReport);
        contentPane.add(bottomPanel, BorderLayout.SOUTH);

        btnSell.addActionListener(e -> sellSelected());
        btnReport.addActionListener(e -> exportStockPDF());
    }

    // ==================== DATA FUNCTIONS ====================
    private void addMedicine() {
        try {
            String name = tfName.getText();
            String batch = tfBatch.getText();
            LocalDate expiry = LocalDate.parse(tfExpiry.getText());
            int qty = Integer.parseInt(tfQuantity.getText());
            double price = Double.parseDouble(tfPrice.getText());

            PreparedStatement pst = con.prepareStatement(
                    "INSERT INTO medicines(name,batch,expiry,quantity,price) VALUES(?,?,?,?,?)"
            );
            pst.setString(1,name);
            pst.setString(2,batch);
            pst.setDate(3,java.sql.Date.valueOf(expiry));
            pst.setInt(4,qty);
            pst.setDouble(5,price);
            pst.executeUpdate();
            JOptionPane.showMessageDialog(this,"✅ Medicine added!");
            clearFields();
            loadStockTable();
        } catch(Exception e) {
            JOptionPane.showMessageDialog(this,"❌ Error: "+e.getMessage());
        }
    }

    private void clearFields() {
        tfName.setText(""); tfBatch.setText(""); tfQuantity.setText(""); tfPrice.setText(""); tfExpiry.setValue(LocalDate.now());
    }

    private void loadStockTable() {
        try {
            stockModel.setRowCount(0);
            Statement st = con.createStatement();
            ResultSet rs = st.executeQuery("SELECT * FROM medicines");
            while(rs.next()) {
                stockModel.addRow(new Object[]{
                        rs.getInt("id"),
                        rs.getString("name"),
                        rs.getString("batch"),
                        rs.getDate("expiry").toLocalDate(),
                        rs.getInt("quantity"),
                        rs.getDouble("price")
                });
            }
        } catch(Exception e) { JOptionPane.showMessageDialog(this,"Load error: "+e.getMessage()); }
    }

    private void searchStock() {
        String keyword = tfSearch.getText().toLowerCase();
        TableRowSorter<DefaultTableModel> tr = new TableRowSorter<>(stockModel);
        stockTable.setRowSorter(tr);
        tr.setRowFilter(RowFilter.regexFilter("(?i)"+keyword));
    }

    private void sellSelected() {
        int row = stockTable.getSelectedRow();
        if (row == -1) { JOptionPane.showMessageDialog(this,"Select a medicine!"); return; }
        int id = (int) stockTable.getValueAt(row,0);
        int qtyAvailable = (int) stockTable.getValueAt(row,4);
        String qtyStr = JOptionPane.showInputDialog(this,"Enter quantity to sell:");
        if(qtyStr == null) return;
        int qtySell = Integer.parseInt(qtyStr);
        if(qtySell>qtyAvailable){ JOptionPane.showMessageDialog(this,"Not enough stock!"); return; }
        try {
            PreparedStatement pst = con.prepareStatement("UPDATE medicines SET quantity=? WHERE id=?");
            pst.setInt(1,qtyAvailable-qtySell);
            pst.setInt(2,id);
            pst.executeUpdate();
            PreparedStatement salePst = con.prepareStatement(
                    "INSERT INTO sales(medicine_id,quantity,sale_date) VALUES(?,?,?)"
            );
            salePst.setInt(1,id);
            salePst.setInt(2,qtySell);
            salePst.setDate(3,java.sql.Date.valueOf(LocalDate.now()));
            salePst.executeUpdate();
            JOptionPane.showMessageDialog(this,"✅ Sale recorded!");
            loadStockTable();
        } catch(Exception e){ JOptionPane.showMessageDialog(this,"Sell error: "+e.getMessage()); }
    }

    private void exportStockPDF() {
        try {
            Document doc = new Document();
            PdfWriter.getInstance(doc,new FileOutputStream("StockReport.pdf"));
            doc.open();
            doc.add(new Paragraph("💊 Pharmacy Stock Report"));
            doc.add(new Paragraph("Generated on: "+LocalDate.now()));
            doc.add(new Paragraph(" "));
            for(int i=0;i<stockModel.getRowCount();i++){
                doc.add(new Paragraph(
                        stockModel.getValueAt(i,1)+" | "+
                        stockModel.getValueAt(i,2)+" | "+
                        stockModel.getValueAt(i,3)+" | "+
                        stockModel.getValueAt(i,4)+" units | Rs."+stockModel.getValueAt(i,5)
                ));
            }
            doc.close();
            JOptionPane.showMessageDialog(this,"PDF exported as StockReport.pdf");
        } catch(Exception e){ JOptionPane.showMessageDialog(this,"PDF error: "+e.getMessage()); }
    }
}
