import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.Socket;
import java.nio.file.Files;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

public class ClientApp extends JFrame {

    // ── Color palette ──────────────────────────────────────────────
    static final Color BG      = new Color(18, 18, 30);
    static final Color PANEL   = new Color(28, 28, 45);
    static final Color CARD    = new Color(38, 38, 58);
    static final Color ACCENT  = new Color(99, 102, 241);
    static final Color ACCENT2 = new Color(16, 185, 129);
    static final Color ACCENT3 = new Color(245, 158, 11);
    static final Color TXT     = new Color(240, 240, 255);
    static final Color TXT_DIM = new Color(150, 150, 180);
    static final Color ROW_ALT = new Color(44, 44, 66);

    // ── Widgets ─────────────────────────────────────────────────────
    private JTextField txtFile;
    private JTextField txtServerIP;
    private JSpinner   spinTongCanBo;     // số lượng cán bộ tổng cộng lấy từ file
    private JSpinner   spinPhongThi;      // số phòng thi
    private JSpinner   spinCa;            // số ca thi
    private JTextArea  txtLog;
    private JTable     tblCoiThi, tblGiamSat;
    private DefaultTableModel mdlCoiThi, mdlGiamSat;
    private JLabel     lblCoiThiCount, lblGiamSatCount;
    private JButton    btnXuat;

    private List<SharedModels.KetQuaPhanCong> lastResult;

    // ── Constructor ─────────────────────────────────────────────────
    public ClientApp() {
        setTitle("Client – Phân Công Coi Thi");
        setSize(1200, 760);
        setMinimumSize(new Dimension(960, 640));
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        getContentPane().setBackground(BG);
        setLayout(new BorderLayout(0, 0));
        buildUI();
    }

    // ── UI Construction ─────────────────────────────────────────────
    private void buildUI() {
        add(buildHeader(), BorderLayout.NORTH);
        add(buildCenter(), BorderLayout.CENTER);
        add(buildFooter(), BorderLayout.SOUTH);
    }

    private JPanel buildHeader() {
        JPanel p = new JPanel(new BorderLayout(0, 8));
        p.setBackground(PANEL);
        p.setBorder(new EmptyBorder(12, 20, 12, 20));

        // ── Dòng 1: Tiêu đề ──
        JLabel title = new JLabel("⚡ Phân Công Coi Thi");
        title.setFont(new Font("Segoe UI", Font.BOLD, 20));
        title.setForeground(TXT);

        // ── Dòng 2: File + Server IP ──
        JPanel row1 = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        row1.setBackground(PANEL);

        txtFile     = styledField(28);
        txtServerIP = styledField(12);
        txtServerIP.setText("localhost");
        txtServerIP.setToolTipText("Nhập IP của máy Server (ví dụ: 192.168.1.5)");
        JButton btnChon = accentBtn("📂 Chọn File", ACCENT);

        row1.add(label("Server IP:"));
        row1.add(txtServerIP);
        row1.add(Box.createHorizontalStrut(8));
        row1.add(label("File xlsx:"));
        row1.add(txtFile);
        row1.add(btnChon);

        // ── Dòng 3: Số lượng + Gửi + Xuất ──
        JPanel row2 = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        row2.setBackground(PANEL);

        spinTongCanBo = new JSpinner(new SpinnerNumberModel(220, 1, 1000, 1));
        styleSpinner(spinTongCanBo);
        spinPhongThi = new JSpinner(new SpinnerNumberModel(100, 1, 200, 1));
        styleSpinner(spinPhongThi);
        spinCa = new JSpinner(new SpinnerNumberModel(1, 1, 20, 1));
        styleSpinner(spinCa);

        JButton btnGui  = accentBtn("🚀 Gửi Server", ACCENT2);
        btnXuat = accentBtn("💾 Xuất Excel", ACCENT3);
        btnXuat.setEnabled(false);

        row2.add(label("Số lượng CB:"));
        row2.add(spinTongCanBo);
        row2.add(Box.createHorizontalStrut(6));
        row2.add(label("Số phòng thi:"));
        row2.add(spinPhongThi);
        row2.add(Box.createHorizontalStrut(6));
        row2.add(label("Số ca:"));
        row2.add(spinCa);
        row2.add(Box.createHorizontalStrut(12));
        row2.add(btnGui);
        row2.add(btnXuat);

        JPanel rows = new JPanel(new GridLayout(2, 1, 0, 4));
        rows.setBackground(PANEL);
        rows.add(row1);
        rows.add(row2);

        btnChon.addActionListener(e -> chonFile());
        btnGui.addActionListener(e  -> guiServer());
        btnXuat.addActionListener(e -> xuatExcel());

        p.add(title, BorderLayout.NORTH);
        p.add(rows,  BorderLayout.CENTER);
        return p;
    }

    private JPanel buildCenter() {
        JPanel p = new JPanel(new GridLayout(1, 2, 8, 0));
        p.setBackground(BG);
        p.setBorder(new EmptyBorder(10, 10, 0, 10));

        // ── Table: Cán bộ COI THI ──
        String[] colsCT = {"Ca", "Phòng", "Mã GV", "Họ và tên", "Nhiệm vụ"};
        mdlCoiThi = tableModel(colsCT);
        tblCoiThi = styledTable(mdlCoiThi, ACCENT);
        lblCoiThiCount = countLabel();
        p.add(tableCard("🎓 Danh sách Cán bộ Coi Thi", tblCoiThi, lblCoiThiCount, ACCENT));

        // ── Table: Cán bộ GIÁM SÁT ──
        String[] colsGS = {"Ca", "Mã GV", "Họ và tên"};
        mdlGiamSat = tableModel(colsGS);
        tblGiamSat = styledTable(mdlGiamSat, ACCENT2);
        lblGiamSatCount = countLabel();
        p.add(tableCard("🛡 Danh sách Cán bộ Giám Sát", tblGiamSat, lblGiamSatCount, ACCENT2));

        return p;
    }

    private JPanel buildFooter() {
        JPanel p = new JPanel(new BorderLayout());
        p.setBackground(BG);
        p.setBorder(new EmptyBorder(8, 10, 10, 10));

        txtLog = new JTextArea(5, 0);
        txtLog.setEditable(false);
        txtLog.setBackground(new Color(10, 10, 18));
        txtLog.setForeground(new Color(100, 220, 140));
        txtLog.setFont(new Font("Consolas", Font.PLAIN, 12));
        txtLog.setBorder(new EmptyBorder(6, 10, 6, 10));

        JScrollPane sp = new JScrollPane(txtLog);
        sp.setBorder(BorderFactory.createLineBorder(CARD, 1));
        p.add(sp, BorderLayout.CENTER);
        return p;
    }

    // ── Table card helper ────────────────────────────────────────────
    private JPanel tableCard(String title, JTable table, JLabel count, Color accent) {
        JPanel card = new JPanel(new BorderLayout(0, 6));
        card.setBackground(CARD);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(accent, 1),
                new EmptyBorder(8, 8, 8, 8)));

        JPanel hdr = new JPanel(new BorderLayout());
        hdr.setBackground(CARD);
        JLabel lbl = new JLabel(title);
        lbl.setFont(new Font("Segoe UI", Font.BOLD, 13));
        lbl.setForeground(accent);
        hdr.add(lbl,   BorderLayout.WEST);
        hdr.add(count, BorderLayout.EAST);

        JScrollPane sp = new JScrollPane(table);
        sp.setBorder(BorderFactory.createLineBorder(new Color(60, 60, 90), 1));
        sp.getViewport().setBackground(new Color(22, 22, 36));

        card.add(hdr, BorderLayout.NORTH);
        card.add(sp,  BorderLayout.CENTER);
        return card;
    }

    // ── Actions ──────────────────────────────────────────────────────
    private void chonFile() {
        JFileChooser fc = new JFileChooser();
        fc.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("Excel (*.xlsx)", "xlsx"));
        if (fc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION)
            txtFile.setText(fc.getSelectedFile().getAbsolutePath());
    }

    private void guiServer() {
        String filePath = txtFile.getText().trim();
        if (filePath.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "Vui lòng chọn file Excel!", "Thiếu thông tin", JOptionPane.WARNING_MESSAGE);
            return;
        }
        File f = new File(filePath);
        if (!f.exists() || !f.isFile()) {
            JOptionPane.showMessageDialog(this,
                    "File không tồn tại:\n" + filePath, "Lỗi file", JOptionPane.ERROR_MESSAGE);
            return;
        }

        mdlCoiThi.setRowCount(0);
        mdlGiamSat.setRowCount(0);
        btnXuat.setEnabled(false);

        new Thread(() -> {
            try {
                // 1. Đọc toàn bộ file xlsx thành bytes
                log("▶ Đọc file Excel: " + f.getName() + " (" + f.length() + " bytes)...");
                byte[] fileBytes = Files.readAllBytes(f.toPath());
                log("✔ Đọc xong file (" + fileBytes.length + " bytes).");

                int tongSoCanBo = (int) spinTongCanBo.getValue();
                int soPhongThi  = (int) spinPhongThi.getValue();
                int soCa        = (int) spinCa.getValue();

                // 2. Đóng gói yêu cầu
                SharedModels.YeuCau yeuCau = new SharedModels.YeuCau(fileBytes, tongSoCanBo, soPhongThi, soCa);

                String serverIP = txtServerIP.getText().trim().isEmpty() ? "localhost" : txtServerIP.getText().trim();
                log("▶ Kết nối Server " + serverIP + ":8888...");

                try (Socket sk = new Socket(serverIP, 8888);
                     ObjectOutputStream oos = new ObjectOutputStream(sk.getOutputStream())) {
                    oos.flush();
                    ObjectInputStream ois = new ObjectInputStream(sk.getInputStream());

                    // 3. Gửi yêu cầu
                    oos.writeObject(yeuCau);
                    oos.flush();
                    log("✔ Đã gửi yêu cầu – Tổng số CB: " + tongSoCanBo
                            + " | Phòng thi: " + soPhongThi + " | Ca: " + soCa + " – chờ kết quả...");

                    // 4. Nhận kết quả
                    @SuppressWarnings("unchecked")
                    List<SharedModels.KetQuaPhanCong> result =
                            (List<SharedModels.KetQuaPhanCong>) ois.readObject();
                    lastResult = result;
                    log("✔ Nhận kết quả: " + result.size() + " bản ghi.");
                    SwingUtilities.invokeLater(() -> hienThiKetQua(lastResult));
                }
            } catch (Exception ex) {
                log("✘ LỖI: " + ex.getMessage());
                ex.printStackTrace();
            }
        }).start();
    }

    private void hienThiKetQua(List<SharedModels.KetQuaPhanCong> ds) {
        mdlCoiThi.setRowCount(0);
        mdlGiamSat.setRowCount(0);

        List<SharedModels.KetQuaPhanCong> coiThi = ds.stream()
                .filter(k -> k.isCoiThi)
                .collect(Collectors.toList());
        List<SharedModels.KetQuaPhanCong> giamSat = ds.stream()
                .filter(k -> !k.isCoiThi)
                .collect(Collectors.toList());

        for (SharedModels.KetQuaPhanCong k : coiThi)
            mdlCoiThi.addRow(new Object[]{
                    k.caThi, k.phongThi, k.maGV, k.hoTen,
                    k.giamThi1 ? "Giám thị 1" : "Giám thị 2"});

        for (SharedModels.KetQuaPhanCong k : giamSat)
            mdlGiamSat.addRow(new Object[]{k.caThi, k.maGV, k.hoTen});

        lblCoiThiCount.setText(coiThi.size() + " người");
        lblGiamSatCount.setText(giamSat.size() + " người");
        btnXuat.setEnabled(true);
        log("✔ Hiển thị xong: " + coiThi.size() + " coi thi | " + giamSat.size() + " giám sát.");
    }

    private void xuatExcel() {
        JFileChooser fc = new JFileChooser();
        fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        fc.setDialogTitle("Chọn thư mục lưu file");
        if (fc.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;
        String dir = fc.getSelectedFile().getAbsolutePath();

        new Thread(() -> {
            try {
                List<SharedModels.KetQuaPhanCong> coiThi = lastResult.stream()
                        .filter(k -> k.isCoiThi).collect(Collectors.toList());
                List<SharedModels.KetQuaPhanCong> giamSat = lastResult.stream()
                        .filter(k -> !k.isCoiThi).collect(Collectors.toList());

                String f1 = dir + File.separator + "CoiThi_PhanCong.xlsx";
                String f2 = dir + File.separator + "GiamSat_PhanCong.xlsx";
                xuatFileCoiThi(coiThi, f1);
                xuatFileGiamSat(giamSat, f2);
                log("✔ Đã xuất:\n   • " + f1 + "\n   • " + f2);
                JOptionPane.showMessageDialog(this, "Xuất file thành công!\n• " + f1 + "\n• " + f2);
            } catch (Exception ex) {
                log("✘ LỖI xuất: " + ex.getMessage());
                ex.printStackTrace();
            }
        }).start();
    }

    // ── Excel xuất Coi Thi ─────────────────────────────────────────
    private void xuatFileCoiThi(List<SharedModels.KetQuaPhanCong> ds, String path) throws Exception {
        try (Workbook wb = new XSSFWorkbook(); FileOutputStream fos = new FileOutputStream(path)) {
            int perPage = 20;
            int pages   = Math.max(1, (int) Math.ceil((double) ds.size() / perPage));
            org.apache.poi.ss.usermodel.Font bold = wb.createFont(); bold.setBold(true);

            CellStyle csBoldCenter = wb.createCellStyle();
            csBoldCenter.setFont(bold);
            csBoldCenter.setAlignment(HorizontalAlignment.CENTER);

            CellStyle csHdr = wb.createCellStyle();
            csHdr.setFont(bold);
            csHdr.setBorderBottom(BorderStyle.MEDIUM);
            csHdr.setFillForegroundColor(IndexedColors.INDIGO.getIndex());
            csHdr.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            csHdr.setAlignment(HorizontalAlignment.CENTER);

            for (int p = 0; p < pages; p++) {
                Sheet sh = wb.createSheet("Trang " + (p + 1));
                writeQuocHieu(sh, csBoldCenter);
                Row titleRow = sh.createRow(4);
                Cell tc = titleRow.createCell(0);
                tc.setCellValue("DANH SÁCH CÁN BỘ COI THI (TRANG " + (p + 1) + "/" + pages + ")");
                tc.setCellStyle(csBoldCenter);
                sh.addMergedRegion(new CellRangeAddress(4, 4, 0, 5));

                String[] cols = {"STT", "Ca", "Phòng thi", "Mã GV", "Họ và tên", "Nhiệm vụ"};
                Row hdr = sh.createRow(5);
                for (int i = 0; i < cols.length; i++) {
                    Cell c = hdr.createCell(i);
                    c.setCellValue(cols[i]);
                    c.setCellStyle(csHdr);
                }

                int start = p * perPage, end = Math.min(start + perPage, ds.size()), ri = 6;
                for (int i = start; i < end; i++) {
                    SharedModels.KetQuaPhanCong k = ds.get(i);
                    Row row = sh.createRow(ri++);
                    row.createCell(0).setCellValue(i + 1);
                    row.createCell(1).setCellValue(k.caThi);
                    row.createCell(2).setCellValue(k.phongThi);
                    row.createCell(3).setCellValue(k.maGV);
                    row.createCell(4).setCellValue(k.hoTen);
                    row.createCell(5).setCellValue(k.giamThi1 ? "Giám thị 1" : "Giám thị 2");
                }
                for (int i = 0; i < cols.length; i++) sh.autoSizeColumn(i);
            }
            wb.write(fos);
        }
    }

    // ── Excel xuất Giám Sát ────────────────────────────────────────
    private void xuatFileGiamSat(List<SharedModels.KetQuaPhanCong> ds, String path) throws Exception {
        try (Workbook wb = new XSSFWorkbook(); FileOutputStream fos = new FileOutputStream(path)) {
            org.apache.poi.ss.usermodel.Font bold = wb.createFont(); bold.setBold(true);
            CellStyle csBoldCenter = wb.createCellStyle();
            csBoldCenter.setFont(bold);
            csBoldCenter.setAlignment(HorizontalAlignment.CENTER);

            CellStyle csHdr = wb.createCellStyle();
            csHdr.setFont(bold);
            csHdr.setBorderBottom(BorderStyle.MEDIUM);
            csHdr.setFillForegroundColor(IndexedColors.SEA_GREEN.getIndex());
            csHdr.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            csHdr.setAlignment(HorizontalAlignment.CENTER);

            int perPage = 20, pages = Math.max(1, (int) Math.ceil((double) ds.size() / perPage));
            for (int p = 0; p < pages; p++) {
                Sheet sh = wb.createSheet("Trang " + (p + 1));
                writeQuocHieu(sh, csBoldCenter);
                Row titleRow = sh.createRow(4);
                Cell tc = titleRow.createCell(0);
                tc.setCellValue("DANH SÁCH CÁN BỘ GIÁM SÁT HÀNH LANG (TRANG " + (p + 1) + "/" + pages + ")");
                tc.setCellStyle(csBoldCenter);
                sh.addMergedRegion(new CellRangeAddress(4, 4, 0, 3));

                String[] cols = {"STT", "Ca", "Mã GV", "Họ và tên"};
                Row hdr = sh.createRow(5);
                for (int i = 0; i < cols.length; i++) {
                    Cell c = hdr.createCell(i);
                    c.setCellValue(cols[i]);
                    c.setCellStyle(csHdr);
                }

                int start = p * perPage, end = Math.min(start + perPage, ds.size()), ri = 6;
                for (int i = start; i < end; i++) {
                    SharedModels.KetQuaPhanCong k = ds.get(i);
                    Row row = sh.createRow(ri++);
                    row.createCell(0).setCellValue(i + 1);
                    row.createCell(1).setCellValue(k.caThi);
                    row.createCell(2).setCellValue(k.maGV);
                    row.createCell(3).setCellValue(k.hoTen);
                }
                for (int i = 0; i < cols.length; i++) sh.autoSizeColumn(i);
            }
            wb.write(fos);
        }
    }

    private void writeQuocHieu(Sheet sh, CellStyle cs) {
        String[] lines = {
                "CỘNG HÒA XÃ HỘI CHỦ NGHĨA VIỆT NAM",
                "Độc lập – Tự do – Hạnh phúc",
                "────────────────────"};
        for (int i = 0; i < lines.length; i++) {
            Row r = sh.createRow(i);
            Cell c = r.createCell(0);
            c.setCellValue(lines[i]);
            c.setCellStyle(cs);
            sh.addMergedRegion(new CellRangeAddress(i, i, 0, 5));
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────
    private void log(String msg) {
        SwingUtilities.invokeLater(() -> {
            txtLog.append("[" + new java.text.SimpleDateFormat("HH:mm:ss").format(new Date()) + "] " + msg + "\n");
            txtLog.setCaretPosition(txtLog.getDocument().getLength());
        });
    }

    private JLabel label(String t) {
        JLabel l = new JLabel(t);
        l.setForeground(TXT_DIM);
        l.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        return l;
    }

    private JLabel countLabel() {
        JLabel l = new JLabel("0 người");
        l.setFont(new Font("Segoe UI", Font.BOLD, 12));
        l.setForeground(TXT_DIM);
        return l;
    }

    private JTextField styledField(int cols) {
        JTextField f = new JTextField(cols);
        f.setBackground(new Color(50, 50, 72));
        f.setForeground(TXT);
        f.setCaretColor(TXT);
        f.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(80, 80, 110), 1),
                new EmptyBorder(4, 8, 4, 8)));
        f.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        return f;
    }

    private void styleSpinner(JSpinner sp) {
        sp.setBackground(new Color(50, 50, 72));
        sp.setForeground(TXT);
        sp.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        sp.setBorder(BorderFactory.createLineBorder(new Color(80, 80, 110), 1));
        ((JSpinner.DefaultEditor) sp.getEditor()).getTextField().setBackground(new Color(50, 50, 72));
        ((JSpinner.DefaultEditor) sp.getEditor()).getTextField().setForeground(TXT);
    }

    private JButton accentBtn(String text, Color bg) {
        JButton b = new JButton(text);
        b.setOpaque(true);
        b.setContentAreaFilled(true);
        b.setBackground(bg);
        b.setForeground(Color.WHITE);
        b.setFont(new Font("Segoe UI", Font.BOLD, 12));
        b.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(bg.darker(), 1),
                new EmptyBorder(5, 13, 5, 13)));
        b.setFocusPainted(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) {
                b.setBackground(bg.brighter());
                b.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(bg, 1), new EmptyBorder(5, 13, 5, 13)));
            }
            public void mouseExited(MouseEvent e) {
                b.setBackground(bg);
                b.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(bg.darker(), 1), new EmptyBorder(5, 13, 5, 13)));
            }
        });
        return b;
    }

    private DefaultTableModel tableModel(String[] cols) {
        return new DefaultTableModel(cols, 0) {
            public boolean isCellEditable(int r, int c) { return false; }
        };
    }

    private JTable styledTable(DefaultTableModel model, Color accent) {
        JTable t = new JTable(model);
        t.setBackground(new Color(22, 22, 36));
        t.setForeground(TXT);
        t.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        t.setRowHeight(26);
        t.setShowGrid(false);
        t.setIntercellSpacing(new Dimension(0, 0));
        t.setSelectionBackground(accent.darker());
        t.setSelectionForeground(Color.WHITE);

        JTableHeader th = t.getTableHeader();
        th.setBackground(new Color(40, 40, 65));
        th.setForeground(accent);
        th.setFont(new Font("Segoe UI", Font.BOLD, 12));
        th.setBorder(BorderFactory.createLineBorder(accent, 1));

        t.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            public Component getTableCellRendererComponent(
                    JTable tbl, Object val, boolean sel, boolean foc, int row, int col) {
                super.getTableCellRendererComponent(tbl, val, sel, foc, row, col);
                setBackground(sel ? accent.darker() : (row % 2 == 0 ? new Color(22, 22, 36) : ROW_ALT));
                setForeground(sel ? Color.WHITE : TXT);
                setBorder(new EmptyBorder(0, 8, 0, 8));
                return this;
            }
        });
        return t;
    }

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {}
        SwingUtilities.invokeLater(() -> new ClientApp().setVisible(true));
    }
}