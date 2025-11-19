package com.bookhub.util;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.util.Base64;

@Service
public class QRCodeService {

    private static final int QR_SIZE = 200; // Kích thước mã QR (pixels)

    /**
     * Sinh mã QR Code dưới dạng chuỗi Base64
     * @param content Nội dung chuỗi sẽ được mã hóa (ví dụ: URL truy cập)
     * @return Chuỗi Base64 đại diện cho ảnh PNG (có tiền tố data URI)
     */
    public String generateQRCodeBase64(String content) {
        try {
            // Khởi tạo bộ ghi QR Code
            QRCodeWriter qrCodeWriter = new QRCodeWriter();

            // Mã hóa nội dung thành ma trận bit
            BitMatrix bitMatrix = qrCodeWriter.encode(content, BarcodeFormat.QR_CODE, QR_SIZE, QR_SIZE);

            // Ghi ma trận bit ra định dạng PNG
            ByteArrayOutputStream pngOutputStream = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(bitMatrix, "PNG", pngOutputStream);
            byte[] pngData = pngOutputStream.toByteArray();

            // Chuyển đổi mảng byte sang Base64 và thêm tiền tố Data URI
            return "data:image/png;base64," + Base64.getEncoder().encodeToString(pngData);

        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Lỗi khi sinh mã QR", e);
        }
    }
}