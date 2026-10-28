package com.wejava.security.service;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.security.SecureRandom;
import java.util.Base64;
import javax.imageio.ImageIO;
import org.springframework.stereotype.Component;

/**
 * 验证码绘图。只负责「把一串字符画成图片」，不碰存储、不碰校验。
 */
@Component
public class CaptchaRenderer {

    private static final int WIDTH = 120;
    private static final int HEIGHT = 40;

    private final SecureRandom random = new SecureRandom();

    /** @return data:image/png;base64,... */
    public String render(String text) {
        BufferedImage image = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setColor(new Color(245, 247, 250));
            g.fillRect(0, 0, WIDTH, HEIGHT);

            g.setStroke(new BasicStroke(1.2f));
            for (int i = 0; i < 6; i++) {
                g.setColor(randomColor(150, 220));
                g.drawLine(
                        random.nextInt(WIDTH), random.nextInt(HEIGHT), random.nextInt(WIDTH), random.nextInt(HEIGHT));
            }

            g.setFont(new Font("SansSerif", Font.BOLD, 26));
            for (int i = 0; i < text.length(); i++) {
                g.setColor(randomColor(20, 120));
                double theta = (random.nextDouble() - 0.5) * 0.5;
                int x = 12 + i * 26;
                int y = 30 + random.nextInt(6);
                g.rotate(theta, x, y);
                g.drawString(String.valueOf(text.charAt(i)), x, y);
                g.rotate(-theta, x, y);
            }
        } finally {
            g.dispose();
        }

        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            ImageIO.write(image, "png", out);
            return "data:image/png;base64," + Base64.getEncoder().encodeToString(out.toByteArray());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private Color randomColor(int min, int max) {
        int bound = max - min;
        return new Color(min + random.nextInt(bound), min + random.nextInt(bound), min + random.nextInt(bound));
    }
}
