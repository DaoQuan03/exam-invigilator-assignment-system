import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.List;

public class ServerApp extends JFrame {

    static final Color BG     = new Color(12, 12, 22);
    static final Color PANEL  = new Color(20, 20, 36);
    static final Color CARD   = new Color(30, 30, 50);
    static final Color GREEN  = new Color(16, 185, 129);
    static final Color YELLOW = new Color(245, 158, 11);
    static final Color RED    = new Color(239, 68, 68);
    static final Color BLUE   = new Color(99, 102, 241);
    static final Color TXT    = new Color(230, 230, 255);
    static final Color DIMTXT = new Color(130, 130, 160);
    static final Color ROWALT = new Color(36, 36, 56);

    private static final int PORT          = 8888;
    private static final int SO_TIMEOUT_MS = 60_000; // 60 giây

    private JTextArea         txtLog;
    private JLabel            lblStatus, lblTotal, lblActive, lblIP;
    private DefaultTableModel tblModel;
    private JTable            tblClients;
    private int               totalConnections  = 0;
    private int               activeConnections = 0;

    private ServerSocket serverSocket;
    private Thread       serverThread;
    private volatile boolean running = false;

    public ServerApp() {
        setTitle("Server – Phân Công Coi Thi | Port " + PORT);
        setSize(940, 640);
        setMinimumSize(new Dimension(720, 500));
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        getContentPane().setBackground(BG);
        setLayout(new BorderLayout(0, 0));
        buildUI();
        startServer();
    }

    private void buildUI() {
        add(buildHeader(), BorderLayout.NORTH);
        add(buildCenter(), BorderLayout.CENTER);
        add(buildLog(),    BorderLayout.SOUTH);
    }

    // ── Header ────────────────────────────────────────────────────────
    private JPanel buildHeader() {
        JPanel p = new JPanel(new BorderLayout(12, 0));
        p.setBackground(PANEL);
        p.setBorder(new EmptyBorder(14, 20, 14, 20));

        JPanel left = new JPanel(new GridLayout(2, 1, 0, 2));
        left.setBackground(PANEL);

        JLabel title = new JLabel("🖥  Server Phân Công Coi Thi");
        title.setFont(new Font("Segoe UI", Font.BOLD, 20));
        title.setForeground(TXT);

        lblIP = new JLabel("🌐  Đang lấy địa chỉ IP...");
        lblIP.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lblIP.setForeground(DIMTXT);

        left.add(title);
        left.add(lblIP);

        JPanel stats = new JPanel(new FlowLayout(FlowLayout.RIGHT, 20, 0));
        stats.setBackground(PANEL);

        lblStatus = statLabel("● ĐANG CHẠY", GREEN);
        lblActive  = statLabel("Kết nối hiện tại: 0", YELLOW);
        lblTotal   = statLabel("Tổng xử lý: 0", BLUE);

        JButton btnStop = new JButton("⏹ Dừng");
        btnStop.setBackground(RED);
        btnStop.setForeground(Color.WHITE);
        btnStop.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnStop.setBorder(new EmptyBorder(6, 14, 6, 14));
        btnStop.setFocusPainted(false);
        btnStop.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnStop.addActionListener(e -> stopServer());

        stats.add(lblStatus);
        stats.add(lblActive);
        stats.add(lblTotal);
        stats.add(btnStop);

        p.add(left,  BorderLayout.WEST);
        p.add(stats, BorderLayout.EAST);
        return p;
    }

    // ── Center (connection history table) ────────────────────────────
    private JPanel buildCenter() {
        JPanel p = new JPanel(new BorderLayout(0, 0));
        p.setBackground(BG);
        p.setBorder(new EmptyBorder(10, 10, 0, 10));

        JLabel lbl = new JLabel("  📋  Lịch sử kết nối");
        lbl.setFont(new Font("Segoe UI", Font.BOLD, 13));
        lbl.setForeground(BLUE);
        lbl.setBorder(new EmptyBorder(0, 0, 6, 0));

        // Cột: #, Thời gian, IP Client, CB trong file, CB coi thi, Phòng thi, Số ca, Trạng thái
        String[] cols = {"#", "Thời gian", "IP Client", "CB trong file",
                          "CB coi thi", "Phòng thi", "Số ca", "Trạng thái"};
        tblModel = new DefaultTableModel(cols, 0) {
            public boolean isCellEditable(int r, int c) { return false; }
        };
        tblClients = styledTable(tblModel);

        int[] widths = {35, 80, 120, 90, 80, 70, 50, 110};
        for (int i = 0; i < widths.length; i++)
            tblClients.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);

        JScrollPane sp = new JScrollPane(tblClients);
        sp.setBorder(BorderFactory.createLineBorder(CARD, 1));
        sp.getViewport().setBackground(new Color(16, 16, 28));

        p.add(lbl, BorderLayout.NORTH);
        p.add(sp,  BorderLayout.CENTER);
        return p;
    }

    // ── Log panel ────────────────────────────────────────────────────
    private JPanel buildLog() {
        JPanel p = new JPanel(new BorderLayout());
        p.setBackground(BG);
        p.setBorder(new EmptyBorder(8, 10, 10, 10));

        txtLog = new JTextArea(7, 0);
        txtLog.setEditable(false);
        txtLog.setBackground(new Color(8, 8, 16));
        txtLog.setForeground(new Color(100, 220, 140));
        txtLog.setFont(new Font("Consolas", Font.PLAIN, 12));
        txtLog.setBorder(new EmptyBorder(6, 10, 6, 10));

        JScrollPane sp = new JScrollPane(txtLog);
        sp.setBorder(BorderFactory.createLineBorder(CARD, 1));
        p.add(sp, BorderLayout.CENTER);
        return p;
    }

    // ── Server logic ─────────────────────────────────────────────────
    private void startServer() {
        running = true;
        serverThread = new Thread(() -> {
            try {
                serverSocket = new ServerSocket(PORT, 50, InetAddress.getByName("0.0.0.0"));

                String localIP = getLocalIP();
                log("✔ Server khởi động tại PORT " + PORT);
                log("✔ IP LAN của máy server: " + localIP);
                log("✔ Client cần kết nối vào: " + localIP + ":" + PORT);
                log("⚠  Đảm bảo firewall đã mở port " + PORT + " (TCP Inbound)");
                log("─────────────────────────────────────────────────");

                SwingUtilities.invokeLater(() -> {
                    lblIP.setText("🌐  IP LAN: " + localIP + "  |  Port: " + PORT);
                    lblIP.setForeground(GREEN);
                    setTitle("Server – Phân Công Coi Thi  |  " + localIP + ":" + PORT);
                });

                while (running) {
                    Socket client = serverSocket.accept();
                    client.setSoTimeout(SO_TIMEOUT_MS);
                    new Thread(() -> handleClient(client)).start();
                }
            } catch (IOException e) {
                if (running) log("✘ Lỗi Server: " + e.getMessage());
            }
        });
        serverThread.setDaemon(true);
        serverThread.start();
    }

    private void stopServer() {
        running = false;
        try {
            if (serverSocket != null && !serverSocket.isClosed())
                serverSocket.close();
        } catch (IOException ignored) {}
        SwingUtilities.invokeLater(() -> {
            lblStatus.setText("● ĐÃ DỪNG");
            lblStatus.setForeground(RED);
        });
        log("⏹ Server đã dừng.");
    }

    private void handleClient(Socket socket) {
        String ip   = socket.getInetAddress().getHostAddress();
        String time = new SimpleDateFormat("HH:mm:ss").format(new Date());
        updateActive(+1);
        int row = addTableRow(time, ip, "–", "–", "–", "–", "⏳ Xử lý...");
        log("▶ Kết nối mới từ: " + ip + ":" + socket.getPort());

        try (
            ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream())
        ) {
            oos.flush(); // gửi OOS header trước để tránh deadlock

            ObjectInputStream ois = new ObjectInputStream(socket.getInputStream());

            // ── Nhận YeuCau từ client ──
            SharedModels.YeuCau yc = (SharedModels.YeuCau) ois.readObject();
            log("   ✔ Nhận yêu cầu: tongSoCanBo=" + yc.tongSoCanBo
                    + " | soPhongThi=" + yc.soPhongThi + " | soCa=" + yc.soCa
                    + " | fileBytes=" + yc.fileBytes.length + " bytes");

            // ── Đọc xlsx từ bytes ──
            List<SharedModels.CanBo> listCB = docCanBoTuBytes(yc.fileBytes);
            log("   ✔ Đọc file: " + listCB.size() + " cán bộ tổng cộng trong file");

            if (listCB.isEmpty()) {
                log("   ✘ Không có dữ liệu cán bộ trong file – hủy phân công!");
                finishRow(row, "✘ File rỗng");
                return;
            }

            // ── Tạo danh sách phòng thi tự động ──
            List<SharedModels.PhongThi> listPT = new ArrayList<>();
            for (int i = 1; i <= yc.soPhongThi; i++)
                listPT.add(new SharedModels.PhongThi("Phòng " + String.format("%03d", i)));

            // ── Cập nhật bảng ──
            updateRow(row,
                    String.valueOf(listCB.size()),
                    String.valueOf(yc.soPhongThi * 2),
                    String.valueOf(yc.soPhongThi),
                    String.valueOf(yc.soCa));

            // ── Phân công ──
            List<SharedModels.KetQuaPhanCong> result =
                    phanCong(yc.soCa, listCB, listPT, yc.tongSoCanBo);
            log("   ✔ Phân công xong: " + result.size() + " bản ghi");

            // ── Gửi kết quả ──
            oos.writeObject(result);
            oos.flush();
            log("   ✔ Đã gửi kết quả về " + ip);
            finishRow(row, "✔ Hoàn tất");

        } catch (SocketTimeoutException e) {
            log("   ✘ Timeout – client " + ip + " không phản hồi trong " + (SO_TIMEOUT_MS / 1000) + "s");
            finishRow(row, "✘ Timeout");
        } catch (EOFException e) {
            log("   ✘ Client " + ip + " ngắt kết nối đột ngột");
            finishRow(row, "✘ Mất kết nối");
        } catch (ClassNotFoundException e) {
            log("   ✘ Lỗi deserialize: " + e.getMessage());
            finishRow(row, "✘ Lỗi dữ liệu");
        } catch (Exception e) {
            log("   ✘ Lỗi xử lý: " + e.getMessage());
            e.printStackTrace();
            finishRow(row, "✘ Lỗi");
        } finally {
            updateActive(-1);
            try { socket.close(); } catch (IOException ignored) {}
        }
    }

    /**
     * Đọc danh sách cán bộ từ mảng bytes của file xlsx.
     * Đọc từ sheet đầu tiên (sheet 0), bỏ qua dòng tiêu đề (row 0).
     * Cột 0 hoặc 1 = Mã GV, Cột 1 hoặc 2 = Họ tên
     * (tự detect: nếu cột 0 là số STT thì mã ở cột 1, tên ở cột 2;
     *  nếu cột 0 là mã thì tên ở cột 1)
     */
    private List<SharedModels.CanBo> docCanBoTuBytes(byte[] fileBytes) throws Exception {
        List<SharedModels.CanBo> list = new ArrayList<>();
        DataFormatter fmt = new DataFormatter();
        try (ByteArrayInputStream bais = new ByteArrayInputStream(fileBytes);
             Workbook wb = new XSSFWorkbook(bais)) {

            Sheet sheet = wb.getSheetAt(0);
            // Kiểm tra header để xác định vị trí cột
            Row headerRow = sheet.getRow(0);
            int colMa = 1, colTen = 2; // mặc định: cột 0 là STT, 1 là mã, 2 là tên
            if (headerRow != null) {
                String h0 = fmt.formatCellValue(headerRow.getCell(0)).trim().toLowerCase();
                // Nếu header cột 0 trông như mã GV (không phải "stt", "số tt", ...)
                if (!h0.isEmpty() && !h0.contains("stt") && !h0.contains("số") && !h0.contains("tt")) {
                    colMa = 0; colTen = 1;
                }
            }

            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row r = sheet.getRow(i);
                if (r == null) continue;
                String ma  = fmt.formatCellValue(r.getCell(colMa)).trim();
                String ten = fmt.formatCellValue(r.getCell(colTen)).trim();
                if (!ma.isEmpty())
                    list.add(new SharedModels.CanBo(ma, ten));
            }
        }
        return list;
    }

    /**
     * Thuật toán phân công:
     *  - soCanBoCoiThi cán bộ đầu tiên (theo thứ tự trong file) → làm cán bộ CÓI THI
     *    (mỗi phòng cần 2 người: giám thị 1 và giám thị 2)
     *  - Phần còn lại → GIÁM SÁT HÀNH LANG (mỗi ca được phân 1 lần)
     *
     * Với mỗi ca:
     *  - Lấy soCanBoCoiThi người đầu, xáo trộn, ghép đôi vào từng phòng (vòng tròn)
     *  - Người ngoài soCanBoCoiThi → giám sát hành lang
     */
    private static List<SharedModels.KetQuaPhanCong> phanCong(
            int soCa,
            List<SharedModels.CanBo> listCB,
            List<SharedModels.PhongThi> listPT,
            int tongSoCanBo) {

        List<SharedModels.KetQuaPhanCong> all = new ArrayList<>();

        // Giới hạn tổng số lượng cán bộ được sử dụng
        int actualTotal = Math.min(tongSoCanBo, listCB.size());
        List<SharedModels.CanBo> selectedCB = new ArrayList<>(listCB.subList(0, actualTotal));

        // Tính số lượng cán bộ coi thi cần thiết (2 người / phòng)
        int soPhong = listPT.size();
        int canThietCoiThi = soPhong * 2;

        // Tách thành 2 nhóm
        int nCoiThi = Math.min(canThietCoiThi, selectedCB.size());
        List<SharedModels.CanBo> nhomCoiThi = new ArrayList<>(selectedCB.subList(0, nCoiThi));
        List<SharedModels.CanBo> nhomGiamSat = new ArrayList<>(selectedCB.subList(nCoiThi, selectedCB.size()));

        Map<SharedModels.CanBo, Set<String>>             histRooms    = new HashMap<>();
        Map<SharedModels.CanBo, Set<SharedModels.CanBo>> histPartners = new HashMap<>();
        for (SharedModels.CanBo cb : nhomCoiThi) {
            histRooms.put(cb, new HashSet<>());
            histPartners.put(cb, new HashSet<>());
        }

        for (int ca = 1; ca <= soCa; ca++) {
            // ── Phân công cán bộ coi thi ──
            List<SharedModels.CanBo> avail = new ArrayList<>(nhomCoiThi);
            Collections.shuffle(avail);
            Map<SharedModels.CanBo, String[]> assigned = new HashMap<>();

            for (SharedModels.PhongThi pt : listPT) {
                SharedModels.CanBo b1 = null, b2 = null;
                // Tìm cặp tốt nhất (không trùng phòng, không trùng cặp trước đó)
                outer:
                for (int i = 0; i < avail.size(); i++) {
                    SharedModels.CanBo u1 = avail.get(i);
                    if (histRooms.get(u1).contains(pt.tenPhong)) continue;
                    for (int j = i + 1; j < avail.size(); j++) {
                        SharedModels.CanBo u2 = avail.get(j);
                        if (!histRooms.get(u2).contains(pt.tenPhong)
                                && !histPartners.get(u1).contains(u2)) {
                            b1 = u1; b2 = u2;
                            break outer;
                        }
                    }
                }
                // Nếu không tìm được cặp lý tưởng, thử ghép đơn giản (vẫn avail)
                if (b1 == null && avail.size() >= 2) {
                    b1 = avail.get(0);
                    b2 = avail.get(1);
                }

                if (b1 != null) {
                    avail.remove(b1); avail.remove(b2);
                    histRooms.get(b1).add(pt.tenPhong); histRooms.get(b2).add(pt.tenPhong);
                    histPartners.get(b1).add(b2);       histPartners.get(b2).add(b1);
                    assigned.put(b1, new String[]{"1", pt.tenPhong});
                    assigned.put(b2, new String[]{"2", pt.tenPhong});
                }
            }

            // Ghi kết quả cán bộ coi thi (isCoiThi = true)
            for (SharedModels.CanBo cb : nhomCoiThi) {
                String[] info  = assigned.get(cb);
                boolean  gt1   = info != null && info[0].equals("1");
                boolean  gt2   = info != null && info[0].equals("2");
                String   phong = info != null ? info[1] : "Chưa phân phòng";
                // isCoiThi=true dù có được phân phòng hay không
                all.add(new SharedModels.KetQuaPhanCong(ca, cb.maGV, cb.hoTen, phong, gt1, gt2, true));
            }

            // Ghi kết quả cán bộ giám sát hành lang (isCoiThi = false)
            for (SharedModels.CanBo cb : nhomGiamSat) {
                all.add(new SharedModels.KetQuaPhanCong(
                        ca, cb.maGV, cb.hoTen, "Giám sát hành lang", false, false, false));
            }
        }
        return all;
    }

    // ── Lấy IP LAN ────────────────────────────────────────────────────
    private String getLocalIP() {
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                NetworkInterface ni = interfaces.nextElement();
                if (!ni.isUp() || ni.isLoopback() || ni.isVirtual()) continue;
                Enumeration<InetAddress> addresses = ni.getInetAddresses();
                while (addresses.hasMoreElements()) {
                    InetAddress addr = addresses.nextElement();
                    if (addr instanceof Inet4Address && !addr.isLoopbackAddress())
                        return addr.getHostAddress();
                }
            }
        } catch (Exception e) {
            log("⚠  Không lấy được IP LAN: " + e.getMessage());
        }
        return "127.0.0.1";
    }

    // ── Table row helpers ─────────────────────────────────────────────
    // Cột: #, Thời gian, IP Client, CB trong file, CB coi thi, Phòng thi, Số ca, Trạng thái
    private int addTableRow(String time, String ip,
                            String cbFile, String cbCoiThi,
                            String phong, String ca, String status) {
        final int[] idx = {-1};
        try {
            SwingUtilities.invokeAndWait(() -> {
                totalConnections++;
                updateStatLabels();
                tblModel.addRow(new Object[]{totalConnections, time, ip, cbFile, cbCoiThi, phong, ca, status});
                idx[0] = tblModel.getRowCount() - 1;
            });
        } catch (Exception ignored) {}
        return idx[0];
    }

    private void updateRow(int row, String cbFile, String cbCoiThi, String phong, String ca) {
        SwingUtilities.invokeLater(() -> {
            if (row >= 0 && row < tblModel.getRowCount()) {
                tblModel.setValueAt(cbFile,   row, 3);
                tblModel.setValueAt(cbCoiThi, row, 4);
                tblModel.setValueAt(phong,    row, 5);
                tblModel.setValueAt(ca,       row, 6);
            }
        });
    }

    private void finishRow(int row, String status) {
        SwingUtilities.invokeLater(() -> {
            if (row >= 0 && row < tblModel.getRowCount())
                tblModel.setValueAt(status, row, 7);
        });
    }

    private void updateActive(int delta) {
        SwingUtilities.invokeLater(() -> {
            activeConnections = Math.max(0, activeConnections + delta);
            updateStatLabels();
        });
    }

    private void updateStatLabels() {
        lblActive.setText("Kết nối hiện tại: " + activeConnections);
        lblTotal.setText("Tổng xử lý: " + totalConnections);
    }

    // ── Helpers ──────────────────────────────────────────────────────
    private void log(String msg) {
        SwingUtilities.invokeLater(() -> {
            txtLog.append("[" + new SimpleDateFormat("HH:mm:ss").format(new Date()) + "] " + msg + "\n");
            txtLog.setCaretPosition(txtLog.getDocument().getLength());
        });
    }

    private JLabel statLabel(String text, Color color) {
        JLabel l = new JLabel(text);
        l.setFont(new Font("Segoe UI", Font.BOLD, 12));
        l.setForeground(color);
        return l;
    }

    private JTable styledTable(DefaultTableModel model) {
        JTable t = new JTable(model);
        t.setBackground(new Color(16, 16, 28));
        t.setForeground(TXT);
        t.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        t.setRowHeight(26);
        t.setShowGrid(false);
        t.setIntercellSpacing(new Dimension(0, 0));
        t.setSelectionBackground(BLUE.darker());
        t.setSelectionForeground(Color.WHITE);

        JTableHeader th = t.getTableHeader();
        th.setBackground(new Color(36, 36, 58));
        th.setForeground(BLUE);
        th.setFont(new Font("Segoe UI", Font.BOLD, 12));

        t.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            public Component getTableCellRendererComponent(
                    JTable tbl, Object val, boolean sel, boolean foc, int r, int c) {
                super.getTableCellRendererComponent(tbl, val, sel, foc, r, c);
                Color bg = sel ? BLUE.darker() : (r % 2 == 0 ? new Color(16, 16, 28) : ROWALT);
                setBackground(bg);
                setForeground(TXT);
                setBorder(new EmptyBorder(0, 8, 0, 8));
                // Cột trạng thái = cột 7
                if (c == 7 && val != null) {
                    String s = val.toString();
                    setForeground(s.startsWith("✔") ? GREEN
                                : s.startsWith("✘") ? RED
                                : YELLOW);
                }
                return this;
            }
        });
        return t;
    }

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {}
        SwingUtilities.invokeLater(() -> new ServerApp().setVisible(true));
    }
}