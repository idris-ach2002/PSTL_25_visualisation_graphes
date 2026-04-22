package com.mongraphe.graphui.export;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import javax.imageio.ImageIO;

import com.jogamp.opengl.GL4;
import com.jogamp.opengl.GLAutoDrawable;
import com.mongraphe.graphui.rendering.GraphRenderer;

public class GraphExport {

    private final GLAutoDrawable drawable;

    public GraphExport(GLAutoDrawable drawable) {
        this.drawable = drawable;
    }

    public void export(String path, int width, int height, GraphRenderer renderer) {
        drawable.invoke(true, glDrawable -> {
            exportToPng(glDrawable.getGL().getGL4(), width, height, path, renderer);
            return true;
        });
    }

    private void exportToPng(GL4 gl, int width, int height, String path, GraphRenderer renderer) {
        int[] viewport = new int[4];
        gl.glGetIntegerv(GL4.GL_VIEWPORT, viewport, 0);

        int[] previousFbo = new int[1];
        gl.glGetIntegerv(GL4.GL_FRAMEBUFFER_BINDING, previousFbo, 0);

        int[] fbo = new int[1];
        int[] tex = new int[1];
        int[] rbo = new int[1];

        int sourceWidth = Math.max(1, viewport[2]);
        int sourceHeight = Math.max(1, viewport[3]);
        int[] renderArea = computeRenderArea(width, height, sourceWidth, sourceHeight);

        try {
            setupFramebuffer(gl, width, height, fbo, tex, rbo);

            gl.glViewport(0, 0, width, height);
            gl.glClearColor(1f, 1f, 1f, 0f);
            gl.glClear(GL4.GL_COLOR_BUFFER_BIT | GL4.GL_DEPTH_BUFFER_BIT);

            gl.glViewport(renderArea[0], renderArea[1], renderArea[2], renderArea[3]);
            renderer.display(drawable);

            gl.glFinish();

            BufferedImage img = readPixels(gl, width, height);

            File outputFile = new File(path);
            boolean written = ImageIO.write(img, "png", outputFile);
            if (!written) {
                throw new IOException("Aucun writer disponible pour le format PNG ou échec d'écriture.");
            }

        } catch (Exception e) {
            throw new RuntimeException("Échec de l'export PNG : " + e.getMessage(), e);
        } finally {
            cleanup(gl, fbo, tex, rbo);
            gl.glBindFramebuffer(GL4.GL_FRAMEBUFFER, previousFbo[0]);
            gl.glViewport(viewport[0], viewport[1], viewport[2], viewport[3]);
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

    private void setupFramebuffer(GL4 gl, int w, int h, int[] fbo, int[] tex, int[] rbo) {
        gl.glGenFramebuffers(1, fbo, 0);
        gl.glBindFramebuffer(GL4.GL_FRAMEBUFFER, fbo[0]);

        gl.glGenTextures(1, tex, 0);
        gl.glBindTexture(GL4.GL_TEXTURE_2D, tex[0]);
        gl.glTexImage2D(GL4.GL_TEXTURE_2D, 0, GL4.GL_RGBA8, w, h, 0, GL4.GL_RGBA, GL4.GL_UNSIGNED_BYTE, null);
        gl.glTexParameteri(GL4.GL_TEXTURE_2D, GL4.GL_TEXTURE_MIN_FILTER, GL4.GL_LINEAR);
        gl.glTexParameteri(GL4.GL_TEXTURE_2D, GL4.GL_TEXTURE_MAG_FILTER, GL4.GL_LINEAR);
        gl.glFramebufferTexture2D(GL4.GL_FRAMEBUFFER, GL4.GL_COLOR_ATTACHMENT0, GL4.GL_TEXTURE_2D, tex[0], 0);

        gl.glGenRenderbuffers(1, rbo, 0);
        gl.glBindRenderbuffer(GL4.GL_RENDERBUFFER, rbo[0]);
        gl.glRenderbufferStorage(GL4.GL_RENDERBUFFER, GL4.GL_DEPTH_COMPONENT24, w, h);
        gl.glFramebufferRenderbuffer(GL4.GL_FRAMEBUFFER, GL4.GL_DEPTH_ATTACHMENT, GL4.GL_RENDERBUFFER, rbo[0]);

        int status = gl.glCheckFramebufferStatus(GL4.GL_FRAMEBUFFER);
        if (status != GL4.GL_FRAMEBUFFER_COMPLETE) {
            String error = switch (status) {
                case GL4.GL_FRAMEBUFFER_INCOMPLETE_ATTACHMENT -> "INCOMPLETE_ATTACHMENT";
                case GL4.GL_FRAMEBUFFER_INCOMPLETE_MISSING_ATTACHMENT -> "MISSING_ATTACHMENT";
                case GL4.GL_FRAMEBUFFER_INCOMPLETE_DIMENSIONS -> "INCOMPLETE_DIMENSIONS";
                case GL4.GL_FRAMEBUFFER_UNSUPPORTED -> "UNSUPPORTED";
                default -> "0x" + Integer.toHexString(status);
            };
            throw new RuntimeException("FBO incomplet : " + error);
        }
    }

    private BufferedImage readPixels(GL4 gl, int w, int h) {
        ByteBuffer buffer = ByteBuffer.allocateDirect(w * h * 4).order(ByteOrder.nativeOrder());
        gl.glReadBuffer(GL4.GL_COLOR_ATTACHMENT0);
        gl.glReadPixels(0, 0, w, h, GL4.GL_RGBA, GL4.GL_UNSIGNED_BYTE, buffer);

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

    private void cleanup(GL4 gl, int[] fbo, int[] tex, int[] rbo) {
        gl.glDeleteTextures(1, tex, 0);
        gl.glDeleteRenderbuffers(1, rbo, 0);
        gl.glDeleteFramebuffers(1, fbo, 0);
    }
}