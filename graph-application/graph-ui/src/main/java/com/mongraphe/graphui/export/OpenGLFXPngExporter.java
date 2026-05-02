package com.mongraphe.graphui.export;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;

import javax.imageio.ImageIO;

import org.lwjgl.BufferUtils;

import com.mongraphe.graphui.rendering.GraphRenderer;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL30.*;

/** Export PNG exécuté dans le callback OpenGLFX/LWJGL courant. */
public final class OpenGLFXPngExporter {

    public void export(GraphRenderer renderer, File file, int width, int height) {
        IntBuffer viewport = BufferUtils.createIntBuffer(4);
        glGetIntegerv(GL_VIEWPORT, viewport);
        int previousFbo = glGetInteger(GL_FRAMEBUFFER_BINDING);

        int fbo = 0;
        int tex = 0;
        int rbo = 0;

        int sourceWidth = Math.max(1, viewport.get(2));
        int sourceHeight = Math.max(1, viewport.get(3));
        int[] renderArea = computeRenderArea(width, height, sourceWidth, sourceHeight);

        try {
            fbo = glGenFramebuffers();
            glBindFramebuffer(GL_FRAMEBUFFER, fbo);

            tex = glGenTextures();
            glBindTexture(GL_TEXTURE_2D, tex);
            glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA8, width, height, 0, GL_RGBA, GL_UNSIGNED_BYTE, (ByteBuffer) null);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
            glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_TEXTURE_2D, tex, 0);

            rbo = glGenRenderbuffers();
            glBindRenderbuffer(GL_RENDERBUFFER, rbo);
            glRenderbufferStorage(GL_RENDERBUFFER, GL_DEPTH_COMPONENT24, width, height);
            glFramebufferRenderbuffer(GL_FRAMEBUFFER, GL_DEPTH_ATTACHMENT, GL_RENDERBUFFER, rbo);

            int status = glCheckFramebufferStatus(GL_FRAMEBUFFER);
            if (status != GL_FRAMEBUFFER_COMPLETE) {
                throw new RuntimeException("FBO incomplet : 0x" + Integer.toHexString(status));
            }

            glViewport(0, 0, width, height);
            glClearColor(1f, 1f, 1f, 0f);
            glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

            glViewport(renderArea[0], renderArea[1], renderArea[2], renderArea[3]);
            renderer.reshape(renderArea[2], renderArea[3]);
            renderer.display();
            glFinish();

            BufferedImage img = readPixels(width, height);

            if (file.toPath().getParent() != null) {
                java.nio.file.Files.createDirectories(file.toPath().getParent());
            }
            boolean written = ImageIO.write(img, "png", file);
            if (!written) {
                throw new IOException("Aucun writer disponible pour le format PNG ou échec d'écriture.");
            }
        } catch (Exception e) {
            throw new RuntimeException("Échec de l'export PNG : " + e.getMessage(), e);
        } finally {
            if (tex != 0) glDeleteTextures(tex);
            if (rbo != 0) glDeleteRenderbuffers(rbo);
            if (fbo != 0) glDeleteFramebuffers(fbo);
            glBindFramebuffer(GL_FRAMEBUFFER, previousFbo);
            glViewport(viewport.get(0), viewport.get(1), viewport.get(2), viewport.get(3));
            renderer.reshape(Math.max(1, viewport.get(2)), Math.max(1, viewport.get(3)));
        }
    }

    private int[] computeRenderArea(int targetWidth, int targetHeight, int sourceWidth, int sourceHeight) {
        double sourceRatio = (double) sourceWidth / (double) sourceHeight;
        double targetRatio = (double) targetWidth / (double) targetHeight;

        int renderWidth;
        int renderHeight;
        if (targetRatio > sourceRatio) {
            renderHeight = targetHeight;
            renderWidth = Math.max(1, (int) Math.round(renderHeight * sourceRatio));
        } else {
            renderWidth = targetWidth;
            renderHeight = Math.max(1, (int) Math.round(renderWidth / sourceRatio));
        }

        int offsetX = Math.max(0, (targetWidth - renderWidth) / 2);
        int offsetY = Math.max(0, (targetHeight - renderHeight) / 2);
        return new int[] { offsetX, offsetY, renderWidth, renderHeight };
    }

    private BufferedImage readPixels(int w, int h) {
        ByteBuffer buffer = BufferUtils.createByteBuffer(w * h * 4);
        glReadBuffer(GL_COLOR_ATTACHMENT0);
        glReadPixels(0, 0, w, h, GL_RGBA, GL_UNSIGNED_BYTE, buffer);

        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        byte[] row = new byte[w * 4];

        for (int y = 0; y < h; y++) {
            buffer.position((h - 1 - y) * w * 4);
            buffer.get(row);
            for (int x = 0; x < w; x++) {
                int i = x * 4;
                int argb = ((row[i + 3] & 0xFF) << 24)
                        | ((row[i] & 0xFF) << 16)
                        | ((row[i + 1] & 0xFF) << 8)
                        | (row[i + 2] & 0xFF);
                img.setRGB(x, y, argb);
            }
        }
        return img;
    }
}
