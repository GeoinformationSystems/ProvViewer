import java.awt.image.BufferedImage;

public class Constants {
    public static BufferedImage EMPTY_IMAGE;

    static {
        try {
            EMPTY_IMAGE = new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB);
        } catch (Exception e) {
        }
    }

    public static final int MAX_REQUEST_SIZE = 10485760;

    public static final int MAX_AREA = 10000 * 10000;

}
