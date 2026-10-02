package tutorial.images.warehouse;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class ThumbnailImageCreator {

    private final int maxWidth;

    public ThumbnailImageCreator(int maxWidth) {
        this.maxWidth = maxWidth;
    }

    public ByteArrayOutputStream create(InputStream srcImageStream) throws IOException {
        final BufferedImage srcImage = ImageIO.read(srcImageStream);
        if (srcImage != null) {
            final BufferedImage thumbnailImage = resize(srcImage);
            final ByteArrayOutputStream result = new ByteArrayOutputStream(1024);
            final boolean written = ImageIO.write(thumbnailImage, "jpeg", result);
            if (!written) {
                throw new IOException("No JPEG writer available");
            }
            return result;
        } else {
            throw new IOException("Can't read image source");
        }
    }

    private BufferedImage resize(BufferedImage srcImage) {
        final int width = srcImage.getWidth();
        final int height = srcImage.getHeight();

        final int newWidth = Math.min(width, maxWidth);
        final int newHeight = (int) Math.round((double) height * newWidth / width);
        final BufferedImage thumbnail = new BufferedImage(newWidth, newHeight, BufferedImage.TYPE_INT_RGB);

        final Graphics2D graphics = thumbnail.createGraphics();
        try {
            graphics.drawImage(srcImage, 0, 0, newWidth, newHeight, null);
        } finally {
            graphics.dispose();
        }
        return thumbnail;
    }
}
