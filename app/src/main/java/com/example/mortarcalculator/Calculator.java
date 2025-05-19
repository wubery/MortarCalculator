package com.example.mortarcalculator;

import android.util.Log;

public class Calculator {
    private static final double MIN_RANGE = 50; // минимальная дальность в метрах
    private static final double MAX_RANGE = 1500; // максимальная дальность в метрах

    public static class MortarResult {
        public final double highAngle;
        public final double lowAngle;
        public final double distance;
        public final double timeOfFlight;

        public MortarResult(double highAngle, double lowAngle, double distance, double timeOfFlight) {
            this.highAngle = highAngle;
            this.lowAngle = lowAngle;
            this.distance = distance;
            this.timeOfFlight = timeOfFlight;
        }
    }

    public static MortarResult calculateMortar(double x, double y, double z) {
        final double g = 9.81;
        final double v0 = 211.0; // Начальная скорость для 82мм миномета
        
        double distance = Math.sqrt(x * x + y * y);

        Log.d("MortarCalc", String.format(
            "Calculator input: x=%.1fm, y=%.1fm, z=%.1fm, distance=%.1fm",
            x, y, z, distance
        ));

        // Проверка минимальной и максимальной дистанции
        if (distance < MIN_RANGE || distance > MAX_RANGE) {
            Log.d("MortarCalc", String.format(
                "Distance check failed: %.1fm not in range %.1f-%.1f",
                distance, MIN_RANGE, MAX_RANGE
            ));
            return null;
        }

        // Корректируем коэффициент сопротивления воздуха в зависимости от дистанции
        double k;
        if (distance < 300) {
            k = 0.0002;
        } else if (distance < 600) {
            k = 0.0003;
        } else if (distance < 900) {
            k = 0.0004;
        } else {
            k = 0.0005;
        }

        double effectiveDistance = distance * (1 + k * distance / 1000);

        // Модифицированный расчет углов для лучшей работы на всех дистанциях
        double baseAngle = 45.0; // Базовый угол для средней дистанции
        
        // Корректируем базовый угол в зависимости от дистанции
        if (distance < 300) {
            baseAngle = 60.0;
        } else if (distance < 600) {
            baseAngle = 55.0;
        } else if (distance < 900) {
            baseAngle = 50.0;
        } else if (distance < 1200) {
            baseAngle = 45.0;
        } else {
            baseAngle = 40.0;
        }

        // Конвертируем в радианы
        double baseAngleRad = Math.toRadians(baseAngle);
        
        // Рассчитываем высокий и низкий углы относительно базового
        double deltaAngle = Math.toRadians(20.0); // Разница между высоким и низким углом
        double highAngleRad = baseAngleRad + deltaAngle;
        double lowAngleRad = baseAngleRad - deltaAngle;

        // Корректируем углы с учетом возвышения цели
        if (z != 0) {
            double elevationAngle = Math.atan(z / distance);
            highAngleRad += elevationAngle;
            lowAngleRad += elevationAngle;
        }

        // Конвертируем обратно в градусы
        double highAngleDeg = Math.toDegrees(highAngleRad);
        double lowAngleDeg = Math.toDegrees(lowAngleRad);
        
        Log.d("MortarCalc", String.format(
            "Angles calculated: base=%.1f°, high=%.1f°, low=%.1f°",
            baseAngle, highAngleDeg, lowAngleDeg
        ));

        // Константы для углов
        final double MAX_ANGLE = 87.0;
        final double MIN_ANGLE = 4.0;

        // Проверяем углы
        if (highAngleDeg > MAX_ANGLE || lowAngleDeg < MIN_ANGLE) {
            Log.d("MortarCalc", String.format(
                "Angle check failed: high=%.1f° (max %.1f°), low=%.1f° (min %.1f°)",
                highAngleDeg, MAX_ANGLE, lowAngleDeg, MIN_ANGLE
            ));
            return null;
        }

        // Расчет времени полета (приближенная формула)
        double avgAngleRad = (highAngleRad + lowAngleRad) / 2;
        double timeOfFlight = Math.sqrt((2 * effectiveDistance * Math.tan(avgAngleRad)) / g);

        return new MortarResult(
            highAngleDeg,
            lowAngleDeg,
            distance,
            timeOfFlight
        );
    }
}