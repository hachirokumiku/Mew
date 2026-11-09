package com.sharkie.wobblekit;

/** Maps image coordinates into the visible wallpaper viewport. */
final class ImageFrame {
    final float width, height, left, top;

    ImageFrame(float imageWidth, float imageHeight, float viewportLeft, float viewportTop,
               float viewportWidth, float viewportHeight, boolean crop, float zoom,
               float positionX, float positionY) {
        float scale = crop
                ? Math.max(viewportWidth / imageWidth, viewportHeight / imageHeight) * zoom
                : Math.min(viewportWidth / imageWidth, viewportHeight / imageHeight);
        width = imageWidth * scale;
        height = imageHeight * scale;
        left = viewportLeft + (crop ? -(width - viewportWidth) * positionX : (viewportWidth - width) / 2);
        top = viewportTop + (crop ? -(height - viewportHeight) * positionY : (viewportHeight - height) / 2);
    }

    float imageX(float viewX) { return (viewX - left) / width; }
    float imageY(float viewY) { return (viewY - top) / height; }
}
