package com.example.mortarcalculator;

public class BallisticCalculator {
    private static final double GRAVITY = 9.81; // м/с²
    private static final double AIR_DENSITY = 1.225; // кг/м³
    private static final double TIME_STEP = 0.01; // шаг времени для симуляции в секундах

    public static class BallisticResult {
        public final double angle; // угол в градусах
        public final double timeOfFlight; // время полета в секундах
        public final double maxHeight; // максимальная высота в метрах
        public final boolean isValid; // достижима ли цель

        public BallisticResult(double angle, double timeOfFlight, double maxHeight, boolean isValid) {
            this.angle = angle;
            this.timeOfFlight = timeOfFlight;
            this.maxHeight = maxHeight;
            this.isValid = isValid;
        }
    }

    public static BallisticResult[] calculateTrajectory(MortarType mortar, double distance, double heightDiff) {
        BallisticResult[] results = new BallisticResult[2]; // для навесной и настильной траектории
        
        // Проверяем, находится ли цель в пределах досягаемости
        if (distance > mortar.getMaxRange() || distance < mortar.getMinRange()) {
            // Цель вне досягаемости - возвращаем невалидные результаты
            results[0] = new BallisticResult(0, 0, 0, false);
            results[1] = new BallisticResult(0, 0, 0, false);
            return results;
        }
        
        double v0 = mortar.getMuzzleVelocity();
        
        // Для близких целей используем более пологие углы
        double baseAngle = 45.0;
        if (distance < mortar.getMaxRange() * 0.3) {
            baseAngle = 45.0 - (1.0 - distance / (mortar.getMaxRange() * 0.3)) * 30.0;
        }

        // Корректируем угол с учетом разницы высот
        double angleCorrection = Math.toDegrees(Math.atan2(heightDiff, distance));
        baseAngle += angleCorrection;

        // Рассчитываем время полета
        double flightTime = calculateFlightTime(distance, v0, baseAngle);

        // Рассчитываем максимальную высоту
        double maxHeight = calculateMaxHeight(v0, baseAngle, flightTime);

        // Для навесной траектории увеличиваем угол
        double highAngle = baseAngle + 15.0;
        double highFlightTime = calculateFlightTime(distance, v0, highAngle);
        double highMaxHeight = calculateMaxHeight(v0, highAngle, highFlightTime);

        // Для настильной траектории уменьшаем угол
        double lowAngle = baseAngle - 15.0;
        double lowFlightTime = calculateFlightTime(distance, v0, lowAngle);
        double lowMaxHeight = calculateMaxHeight(v0, lowAngle, lowFlightTime);

        // Проверяем, что углы находятся в допустимых пределах
        boolean highAngleValid = highAngle >= mortar.getMinElevation() && highAngle <= mortar.getMaxElevation();
        boolean lowAngleValid = lowAngle >= mortar.getMinElevation() && lowAngle <= mortar.getMaxElevation();

        results[0] = new BallisticResult(highAngle, highFlightTime, highMaxHeight, highAngleValid);
        results[1] = new BallisticResult(lowAngle, lowFlightTime, lowMaxHeight, lowAngleValid);

        return results;
    }

    private static double calculateFlightTime(double distance, double v0, double angleDegrees) {
        double angleRadians = Math.toRadians(angleDegrees);
        double vx = v0 * Math.cos(angleRadians);
        return distance / vx;
    }

    private static double calculateMaxHeight(double v0, double angleDegrees, double time) {
        double angleRadians = Math.toRadians(angleDegrees);
        double vy = v0 * Math.sin(angleRadians);
        return vy * time / 2.0 - GRAVITY * time * time / 8.0;
    }
} 