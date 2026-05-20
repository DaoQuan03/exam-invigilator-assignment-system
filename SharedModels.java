import java.io.Serializable;
import java.util.Objects;

public class SharedModels {

    public static class CanBo implements Serializable {
        private static final long serialVersionUID = 1L;
        public String maGV, hoTen;

        public CanBo(String maGV, String hoTen) {
            this.maGV = maGV;
            this.hoTen = hoTen;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            CanBo canBo = (CanBo) o;
            return Objects.equals(maGV, canBo.maGV);
        }

        @Override
        public int hashCode() { return Objects.hash(maGV); }
    }

    public static class PhongThi implements Serializable {
        private static final long serialVersionUID = 1L;
        public String tenPhong;

        public PhongThi(String tenPhong) { this.tenPhong = tenPhong; }
    }

    public static class KetQuaPhanCong implements Serializable {
        private static final long serialVersionUID = 1L;
        public int caThi;
        public String maGV, hoTen, phongThi;
        public boolean giamThi1, giamThi2;
        /** true = thuộc nhóm coi thi; false = thuộc nhóm giám sát hành lang */
        public boolean isCoiThi;

        public KetQuaPhanCong(int caThi, String maGV, String hoTen, String phongThi,
                              boolean giamThi1, boolean giamThi2, boolean isCoiThi) {
            this.caThi    = caThi;
            this.maGV     = maGV;
            this.hoTen    = hoTen;
            this.phongThi = phongThi;
            this.giamThi1 = giamThi1;
            this.giamThi2 = giamThi2;
            this.isCoiThi = isCoiThi;
        }
    }

    /**
     * Đối tượng yêu cầu Client gửi lên Server.
     *  - fileBytes       : nội dung file xlsx (toàn bộ byte)
     *  - soCanBoCoiThi   : số lượng cán bộ dùng làm cán bộ coi thi
     *  - soPhongThi      : số lượng phòng thi
     *  - soCa            : số ca thi
     */
    public static class YeuCau implements Serializable {
        private static final long serialVersionUID = 1L;
        public byte[] fileBytes;
        public int tongSoCanBo;
        public int soPhongThi;
        public int soCa;

        public YeuCau(byte[] fileBytes, int tongSoCanBo, int soPhongThi, int soCa) {
            this.fileBytes       = fileBytes;
            this.tongSoCanBo     = tongSoCanBo;
            this.soPhongThi      = soPhongThi;
            this.soCa            = soCa;
        }
    }
}