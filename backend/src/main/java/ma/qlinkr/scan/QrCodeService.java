package ma.qlinkr.scan;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * QR codes generated on the server with ZXing. Error correction level H
 * (about 30% of the code can be damaged or covered and still scan), black on
 * white, which matters for codes engraved on wood or printed on coasters.
 */
@Service
public class QrCodeService {

    private static final int QUIET_ZONE = 2;

    private final String publicBaseUrl;

    public QrCodeService(@Value("${qlinkr.public-base-url:http://localhost:5173}") String publicBaseUrl) {
        this.publicBaseUrl = publicBaseUrl.replaceAll("/+$", "");
    }

    public String urlFor(ScanPoint point) {
        return publicBaseUrl + "/s/" + point.getCode();
    }

    public byte[] png(String content, int sizePx) {
        BitMatrix matrix = matrix(content, sizePx);
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            MatrixToImageWriter.writeToStream(matrix, "PNG", out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /** Vector SVG: one path of unit squares, scales to any print size without blur. */
    public String svg(String content) {
        BitMatrix matrix = matrix(content, 0);
        int w = matrix.getWidth();
        int h = matrix.getHeight();
        StringBuilder path = new StringBuilder();
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                if (matrix.get(x, y)) {
                    path.append('M').append(x).append(' ').append(y).append("h1v1h-1z");
                }
            }
        }
        return "<svg xmlns=\"http://www.w3.org/2000/svg\" viewBox=\"0 0 " + w + " " + h
            + "\" shape-rendering=\"crispEdges\"><rect width=\"100%\" height=\"100%\" fill=\"#fff\"/>"
            + "<path fill=\"#000\" d=\"" + path + "\"/></svg>";
    }

    private static BitMatrix matrix(String content, int size) {
        try {
            return new QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, size, size, Map.of(
                EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.H,
                EncodeHintType.MARGIN, QUIET_ZONE,
                EncodeHintType.CHARACTER_SET, "UTF-8"));
        } catch (WriterException e) {
            throw new IllegalArgumentException("Cannot encode QR code", e);
        }
    }
}
