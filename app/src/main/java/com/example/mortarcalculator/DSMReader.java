package com.example.mortarcalculator;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import java.io.InputStream;
import java.io.IOException;

public class DSMReader {
    private static Bitmap elevationBitmap;
    private static final double LAT_MIN = 30.4;
    private static final double LAT_MAX = 30.6;
    private static final double LON_MIN = 47.7;
    private static final double LON_MAX = 47.9;
    private static final double ELEVATION_SCALE = 0.3;  // метров на единицу цвета
    private static final double MIN_ELEVATION = 0.0;    // минимальная высота
    private static final double MAX_ELEVATION = 100.0;  // максимальная высота в метрах

    public static void loadElevationData(InputStream inputStream, Context context) throws IOException {
        // Загружаем изображение как обычный bitmap
        elevationBitmap = BitmapFactory.decodeStream(inputStream);
        if (elevationBitmap == null) {
            throw new IOException("Failed to decode elevation data");
        }
    }

    /**
     * Получает высоту местности в указанной точке
     * @param lat широта
     * @param lon долгота
     * @return высота в метрах или 0.0 если точка вне карты
     */
    public static double getElevation(double lat, double lon) {
        if (elevationBitmap == null) return 0.0;

        // Проверка границ карты
        if (lat < LAT_MIN || lat > LAT_MAX || lon < LON_MIN || lon > LON_MAX) {
            return 0.0;
        }

        int width = elevationBitmap.getWidth();
        int height = elevationBitmap.getHeight();

        // Преобразование координат в пиксели
        double xd = (lon - LON_MIN) / (LON_MAX - LON_MIN) * (width - 1);
        double yd = (lat - LAT_MIN) / (LAT_MAX - LAT_MIN) * (height - 1);

        // Целые координаты для интерполяции
        int x0 = (int) Math.floor(xd);
        int y0 = (int) Math.floor(yd);
        int x1 = Math.min(x0 + 1, width - 1);
        int y1 = Math.min(y0 + 1, height - 1);

        // Веса для интерполяции
        double wx = xd - x0;
        double wy = yd - y0;

        // Получение высот в четырех ближайших точках
        double h00 = getPixelElevation(x0, y0);
        double h10 = getPixelElevation(x1, y0);
        double h01 = getPixelElevation(x0, y1);
        double h11 = getPixelElevation(x1, y1);

        // Билинейная интерполяция
        double h0 = h00 * (1 - wx) + h10 * wx;
        double h1 = h01 * (1 - wx) + h11 * wx;
        double elevation = h0 * (1 - wy) + h1 * wy;

        return Math.max(MIN_ELEVATION, Math.min(MAX_ELEVATION, elevation * ELEVATION_SCALE));
    }

    private static double getPixelElevation(int x, int y) {
        int pixel = elevationBitmap.getPixel(x, y);
        // Используем среднее значение RGB как высоту
        return (Color.red(pixel) + Color.green(pixel) + Color.blue(pixel)) / 3.0;
    }
}