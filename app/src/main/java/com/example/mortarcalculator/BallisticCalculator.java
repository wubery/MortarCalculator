package com.example.mortarcalculator;

import java.util.ArrayList;
import java.util.List;

public class BallisticCalculator {
    private static final double GRAVITY = 9.81; // м/с²
    private static final double AIR_DENSITY_SEA_LEVEL = 1.225; // кг/м³ при нормальных условиях
    private static final double TIME_STEP = 0.2; // увеличиваем шаг времени до 0.2 секунд
    private static final double TEMPERATURE_GRADIENT = -0.0065; // градиент температуры в К/м
    private static final double SEA_LEVEL_TEMPERATURE = 288.15; // температура на уровне моря в Кельвинах
    private static final double GAS_CONSTANT = 287.05; // газовая постоянная для воздуха в Дж/(кг·К)
    private static final double MAX_TIME = 150.0; // уменьшаем максимальное время полета
    private static final double HIT_ACCURACY = 50.0; // увеличиваем допустимую погрешность
    private static final int MAX_TRAJECTORY_POINTS = 200; // уменьшаем максимальное количество точек

    public static class WeatherConditions {
        public final double temperature; // температура в градусах Цельсия
        public final double pressure; // давление в гПа
        public final double humidity; // влажность в процентах
        public final double windSpeed; // скорость ветра в м/s
        public final double windDirection; // направление ветра в градусах

        public WeatherConditions(double temperature, double pressure, double humidity, 
                               double windSpeed, double windDirection) {
            this.temperature = temperature;
            this.pressure = pressure;
            this.humidity = humidity;
            this.windSpeed = windSpeed;
            this.windDirection = windDirection % 360.0; // нормализуем только угол от 0° до 360°
        }
    }

    public static class BallisticResult {
        public final double angle; // угол в градусах
        public final double timeOfFlight; // время полета в секундах
        public final double maxHeight; // максимальная высота в метрах
        public final boolean isValid; // достижима ли цель
        public final double[] trajectoryX; // координаты X траектории
        public final double[] trajectoryY; // координаты Y траектории
        public final double[] trajectoryZ; // координаты Z траектории
        public final double impactEllipseMajor; // большая полуось эллипса поражения
        public final double impactEllipseMinor; // малая полуось эллипса поражения
        public final double impactError; // погрешность попадания в метрах
        public final double impactX; // координата X точки падения
        public final double impactY; // координата Y точки падения

        public BallisticResult(double angle, double timeOfFlight, double maxHeight, 
                             boolean isValid, double[] trajectoryX, double[] trajectoryY, 
                             double[] trajectoryZ, double impactEllipseMajor, double impactEllipseMinor) {
            this(angle, timeOfFlight, maxHeight, isValid, trajectoryX, trajectoryY, trajectoryZ, 
                impactEllipseMajor, impactEllipseMinor, Double.MAX_VALUE, 0, 0);
        }
        
        public BallisticResult(double angle, double timeOfFlight, double maxHeight, 
                             boolean isValid, double[] trajectoryX, double[] trajectoryY, 
                             double[] trajectoryZ, double impactEllipseMajor, double impactEllipseMinor,
                             double impactError, double impactX, double impactY) {
            this.angle = angle;
            this.timeOfFlight = timeOfFlight;
            this.maxHeight = maxHeight;
            this.isValid = isValid;
            this.trajectoryX = trajectoryX;
            this.trajectoryY = trajectoryY;
            this.trajectoryZ = trajectoryZ;
            this.impactEllipseMajor = impactEllipseMajor;
            this.impactEllipseMinor = impactEllipseMinor;
            this.impactError = impactError;
            this.impactX = impactX;
            this.impactY = impactY;
        }
    }

    private static double calculateAirDensity(double height, WeatherConditions weather) {
        // Упрощенный расчет плотности воздуха
        double temperatureKelvin = weather.temperature + 273.15;
        double temperatureAtHeight = Math.max(200.0, temperatureKelvin + TEMPERATURE_GRADIENT * height);
        double pressureAtHeight = Math.max(100.0, weather.pressure * Math.exp(-GRAVITY * height / 
                           (GAS_CONSTANT * temperatureAtHeight)));
        
        // Упрощенный расчет плотности
        double density = pressureAtHeight / (GAS_CONSTANT * temperatureAtHeight);
        
        // Добавляем подробное логирование плотности воздуха
        android.util.Log.d("BallisticCalculator", String.format(
            "Air density calculation at height %.1fm:\n" +
            "Temperature: %.1f°C (%.1fK at height)\n" +
            "Pressure: %.1f hPa at height\n" +
            "Resulting density: %.5f kg/m³",
            height, weather.temperature, temperatureAtHeight, 
            pressureAtHeight, density));
            
        return Math.max(0.1, density);
    }

    private static double calculateDragCoefficient(double velocity, double caliber) {
        // Упрощенный расчет коэффициента сопротивления
        velocity = Math.min(2000.0, velocity);
        double machNumber = velocity / 340.0;
        
        double dragCoeff;
        if (machNumber < 0.8) {
            dragCoeff = 0.2 + 0.1 * machNumber * machNumber;
        } else if (machNumber < 1.2) {
            dragCoeff = 0.4 + 0.2 * (machNumber - 0.8) * (machNumber - 0.8);
        } else {
            dragCoeff = 0.15 + 0.05 / (machNumber * machNumber);
        }
        
        return Math.min(1.0, dragCoeff * (1.0 + 0.1 * Math.log10(caliber)));
    }

    private static double findOptimalAngle(MortarType mortar, double targetDistance, 
                                         double heightDiff, WeatherConditions weather,
                                         double startAngle, double endAngle, 
                                         double accuracy, int maxIterations) {
        maxIterations = Math.min(3, maxIterations);
        
        if (targetDistance < mortar.getMinRange() || targetDistance > mortar.getMaxRange() * 1.2) {
            return startAngle;
        }
        
        // Убираем преждевременную корректировку угла, так как она будет применена позже
        // startAngle = mortar.getAmmoType().getAngleCorrection(startAngle);
        // endAngle = mortar.getAmmoType().getAngleCorrection(endAngle);
        
        double currentAngle = startAngle;
        double step = Math.abs(endAngle - startAngle) / 2.0;
        double bestAngle = currentAngle;
        double minError = Double.MAX_VALUE;
        
        for (int iteration = 0; iteration < maxIterations; iteration++) {
            BallisticResult result = calculateSingleTrajectory(mortar, targetDistance, 
                                                             heightDiff, weather, currentAngle);
            
            if (result.isValid) {
                double[] trajectoryX = result.trajectoryX;
                double[] trajectoryY = result.trajectoryY;
                
                int groundIndex = -1;
                int stepSize = Math.max(1, trajectoryY.length / 10);
                for (int i = 1; i < trajectoryY.length; i += stepSize) {
                    if (trajectoryY[i] <= heightDiff) {
                        groundIndex = i;
                        break;
                    }
                }
                
                if (groundIndex > 0) {
                    double impactX = trajectoryX[groundIndex];
                    double impactY = trajectoryY[groundIndex];
                    double impactDistance = Math.sqrt(impactX * impactX + impactY * impactY);
                    double error = Math.abs(impactDistance - targetDistance);
                    
                    if (error < minError) {
                        minError = error;
                        bestAngle = currentAngle;
                    }
                    
                    if (error < accuracy * 3) {
                        break;
                    }
                    
                    if (impactDistance < targetDistance) {
                        currentAngle += step * 2.5;
                    } else {
                        currentAngle -= step * 2.5;
                    }
                }
            }
            
            step *= 0.3;
            if (step < 2.0) break;
        }
        
        return bestAngle;
    }

    private static double[] calculateOptimalAngles(MortarType mortar, double distance, 
                                                 double heightDiff, WeatherConditions weather) {
        double maxElevation = mortar.getMaxElevation();
        double minElevation = mortar.getMinElevation();
        
        // Точность расчета в метрах
        double accuracy = 1.0;
        // Максимальное количество итераций
        int maxIterations = 20;
        
        // Рассчитываем примерные углы на основе дистанции и разницы высот
        double normalizedDistance = (distance - mortar.getMinRange()) / 
                                  (mortar.getMaxRange() - mortar.getMinRange());
        
        // Корректируем углы с учетом разницы высот
        double heightCorrection = Math.atan2(heightDiff, distance) * 180.0 / Math.PI;
        android.util.Log.d("BallisticCalculator", String.format(
            "Height correction: %.1f° for height difference %.1f m at distance %.1f m",
            heightCorrection, heightDiff, distance));
        
        // Начальные углы для поиска с учетом разницы высот
        // Для высокой траектории
        double highStartAngle = maxElevation * (1.0 - normalizedDistance * 0.3);
        double highEndAngle = maxElevation * (0.7 - normalizedDistance * 0.3);
        
        // Для низкой траектории
        double lowStartAngle = maxElevation * (0.7 - normalizedDistance * 0.3);
        double lowEndAngle = minElevation * (1.0 + normalizedDistance * 0.2);
        
        // Применяем коррекцию высоты к обоим углам
        highStartAngle += heightCorrection;
        highEndAngle += heightCorrection;
        lowStartAngle += heightCorrection;
        lowEndAngle += heightCorrection;
        
        android.util.Log.d("BallisticCalculator", String.format(
            "Calculating angles for distance %.1f m (normalized: %.2f):\n" +
            "Height difference: %.1f m\n" +
            "Height correction: %.1f°\n" +
            "High trajectory range: %.1f° - %.1f°\n" +
            "Low trajectory range: %.1f° - %.1f°",
            distance, normalizedDistance, heightDiff, heightCorrection,
            highStartAngle, highEndAngle, lowStartAngle, lowEndAngle));
        
        // Ищем высокую траекторию
        double highAngle = findOptimalAngle(mortar, distance, heightDiff, weather, 
                                          highStartAngle, highEndAngle, accuracy, maxIterations);
        
        // Ищем низкую траекторию
        double lowAngle = findOptimalAngle(mortar, distance, heightDiff, weather,
                                         lowStartAngle, lowEndAngle, accuracy, maxIterations);
        
        // Применяем только ограничения по минимальному и максимальному углу
        // Поправка типа боеприпаса будет применена позже в методе calculateTrajectory
        // highAngle = mortar.getAmmoType().getAngleCorrection(highAngle);
        // lowAngle = mortar.getAmmoType().getAngleCorrection(lowAngle);
        
        highAngle = Math.max(minElevation, Math.min(maxElevation, highAngle));
        lowAngle = Math.max(minElevation, Math.min(maxElevation, lowAngle));
        
        android.util.Log.d("BallisticCalculator", String.format(
            "Final angles (before ammo correction):\n" +
            "High angle: %.1f°\n" +
            "Low angle: %.1f°\n" +
            "Height difference: %.1f m",
            highAngle, lowAngle, heightDiff));
        
        return new double[]{highAngle, lowAngle};
    }

    public static BallisticResult[] calculateTrajectory(MortarType mortar, double distance, 
                                                      double heightDiff, WeatherConditions weather) {
        android.util.Log.d("BallisticCalculator", String.format(
            "Starting trajectory calculation:\n" +
            "Distance: %.2f m\n" +
            "Height difference: %.2f m\n" +
            "Mortar: %s\n" +
            "Ammo type: %s\n" +
            "Weather: temp=%.1f°C, pressure=%.1f hPa, humidity=%.1f%%, wind=%.1f m/s @ %.1f°",
            distance, heightDiff, mortar.getName(),
            mortar.getAmmoType().getName(),
            weather.temperature, weather.pressure, weather.humidity,
            weather.windSpeed, weather.windDirection));

        // Проверяем базовые ограничения
        if (distance < mortar.getMinRange() || distance > mortar.getMaxRange() * 1.2) {
            android.util.Log.d("BallisticCalculator", String.format(
                "Target out of range: distance=%.2f m, minRange=%.2f m, maxRange=%.2f m",
                distance, mortar.getMinRange(), mortar.getMaxRange()));
            return new BallisticResult[]{
                new BallisticResult(0, 0, 0, false, new double[0], new double[0], new double[0], 0, 0),
                new BallisticResult(0, 0, 0, false, new double[0], new double[0], new double[0], 0, 0)
            };
        }

        // Рассчитываем оптимальные углы для данной дистанции (геометрические)
        double[] angles = calculateOptimalAngles(mortar, distance, heightDiff, weather);
        double highAngle = angles[0];
        double lowAngle = angles[1];

        // Более подробно логируем процесс применения поправки
        android.util.Log.d("BallisticCalculator", String.format(
            "Applying ammo correction for %s:\n" +
            "Before correction - High angle: %.3f°, Low angle: %.3f°",
            mortar.getAmmoType().getName(), highAngle, lowAngle));
            
        // Применяем корректировку угла в зависимости от типа боеприпаса
        highAngle = mortar.getAmmoType().getAngleCorrection(highAngle);
        lowAngle = mortar.getAmmoType().getAngleCorrection(lowAngle);
        
        android.util.Log.d("BallisticCalculator", String.format(
            "After ammo correction - High angle: %.3f°, Low angle: %.3f°\n" +
            "Difference: High +%.3f°, Low +%.3f°",
            highAngle, lowAngle,
            highAngle - angles[0], lowAngle - angles[1]));
        
        // НОВЫЙ КОД: Уточнение углов с учетом физических параметров и погодных условий
        android.util.Log.d("BallisticCalculator", "Starting physical refinement of angles based on weather conditions");
        
        // Уточняем высокий угол с учетом всех физических параметров
        double refinedHighAngle = refineAngleWithPhysics(
            mortar, distance, heightDiff, weather, 
            highAngle, // начальное значение из геометрического расчета
            Math.min(45.0, mortar.getMinElevation()), // нижняя граница допустимого диапазона
            mortar.getMaxElevation() // верхняя граница диапазона
        );
        
        // Уточняем низкий угол с учетом всех физических параметров
        double refinedLowAngle = refineAngleWithPhysics(
            mortar, distance, heightDiff, weather, 
            lowAngle, // начальное значение из геометрического расчета
            mortar.getMinElevation(), // нижняя граница допустимого диапазона
            Math.max(45.0, mortar.getMinElevation() + 5.0) // верхняя граница диапазона
        );
        
        android.util.Log.d("BallisticCalculator", String.format(
            "After physical refinement:\n" + 
            "High angle: %.3f° -> %.3f° (delta: %.3f°)\n" +
            "Low angle: %.3f° -> %.3f° (delta: %.3f°)",
            highAngle, refinedHighAngle, refinedHighAngle - highAngle,
            lowAngle, refinedLowAngle, refinedLowAngle - lowAngle
        ));

        // Используем уточненные углы для расчета траекторий
        android.util.Log.d("BallisticCalculator", String.format(
            "Calculating final trajectories with refined angles: %.3f° and %.3f°",
            refinedHighAngle, refinedLowAngle));
        
        return new BallisticResult[]{
            calculateSingleTrajectory(mortar, distance, heightDiff, weather, refinedHighAngle),
            calculateSingleTrajectory(mortar, distance, heightDiff, weather, refinedLowAngle)
        };
    }

    private static BallisticResult calculateSingleTrajectory(MortarType mortar, double distance,
                                                           double heightDiff, WeatherConditions weather,
                                                           double angle) {
        // Угол уже скорректирован с учетом типа боеприпаса
        if (angle < mortar.getMinElevation() || angle > mortar.getMaxElevation()) {
            return new BallisticResult(angle, 0, 0, false, new double[0], new double[0], new double[0], 0, 0);
        }

        // Логируем ключевые входные параметры
        android.util.Log.d("BallisticCalculator", String.format(
            "Starting single trajectory calculation:\n" +
            "Angle: %.2f°\n" +
            "Distance: %.2f m\n" + 
            "Height diff: %.2f m\n" +
            "Temperature: %.2f°C",
            angle, distance, heightDiff, weather.temperature));

        // Получаем базовую начальную скорость
        double v0 = mortar.getMuzzleVelocity();
        
        // Корректируем начальную скорость в зависимости от угла возвышения
        // Чем больше угол, тем меньше фактическая начальная скорость из-за эффекта вертикального ускорения
        double angleCorrection = 1.0 - 0.15 * Math.sin(Math.toRadians(angle)); // Снижение скорости до 15% при максимальном угле
        v0 *= angleCorrection;
        
        double angleRad = Math.toRadians(angle);
        
        double x = 0.0, y = 0.0, z = 0.0;
        double vx = v0 * Math.cos(angleRad);
        double vy = v0 * Math.sin(angleRad);
        double vz = 0.0;
        
        double windRad = Math.toRadians(weather.windDirection);
        double windX = weather.windSpeed * Math.sin(windRad);
        double windZ = weather.windSpeed * Math.cos(windRad);
        
        double time = 0.0;
        double maxHeight = 0.0;
        double lastY = 0.0;
        boolean isAscending = true;
        
        java.util.ArrayList<Double> trajectoryX = new java.util.ArrayList<>();
        java.util.ArrayList<Double> trajectoryY = new java.util.ArrayList<>();
        java.util.ArrayList<Double> trajectoryZ = new java.util.ArrayList<>();
        
        trajectoryX.add(x);
        trajectoryY.add(y);
        trajectoryZ.add(z);

        double finalX = 0.0;
        double finalY = 0.0;
        double finalZ = 0.0;
        double impactError = Double.MAX_VALUE;
        
        int pointCount = 0;
        while (time < MAX_TIME) {
            double height = y;
            double airDensity = calculateAirDensity(height, weather);
            
            double vxRel = vx - windX;
            double vyRel = vy;
            double vzRel = vz - windZ;
            double velocity = Math.sqrt(vxRel * vxRel + vyRel * vyRel + vzRel * vzRel);
            
            // Используем коэффициент сопротивления из типа боеприпаса
            double dragCoeff = mortar.getAmmoType().getDragCoefficient();
            // Увеличиваем коэффициент сопротивления для более реалистичной траектории
            dragCoeff *= 1.8; // Корректирующий множитель для более реалистичной высоты
            
            double crossSection = Math.PI * Math.pow(mortar.getCaliber() / 2000.0, 2);
            double dragForce = 0.5 * airDensity * dragCoeff * crossSection * velocity * velocity;
            
            // Используем массу снаряда из типа боеприпаса для расчета ускорения
            double projectileMass = mortar.getAmmoType().getWeight();
            
            double ax = -dragForce * vxRel / (velocity * projectileMass);
            double ay = -GRAVITY - dragForce * vyRel / (velocity * projectileMass);
            double az = -dragForce * vzRel / (velocity * projectileMass);
            
            vx += ax * TIME_STEP;
            vy += ay * TIME_STEP;
            vz += az * TIME_STEP;
            
            x += vx * TIME_STEP;
            y += vy * TIME_STEP;
            z += vz * TIME_STEP;
            
            time += TIME_STEP;
            
            // Улучшенный расчет максимальной высоты
            if (isAscending && y < lastY) {
                // Мы достигли пика траектории
                maxHeight = lastY;
                isAscending = false;
            }
            lastY = y;
            
            // Сохраняем точки траектории
            if (pointCount % 20 == 0) {
                trajectoryX.add(x);
                trajectoryY.add(y);
                trajectoryZ.add(z);
            }
            pointCount++;
            
            double distanceToTarget = Math.sqrt(Math.pow(x - distance, 2) + Math.pow(z, 2));
            
            // Если мы близки к земле, вычисляем ошибку попадания
            if (y <= heightDiff) {
                // Рассчитываем точную точку пересечения с уровнем цели (интерполяция)
                double ratio;
                if (Math.abs(lastY - y) < 0.0001) { // защита от деления на ноль
                    ratio = 0.5; // используем среднее значение
                } else {
                    ratio = (lastY - heightDiff) / (lastY - y);
                }
                
                // Проверяем, что интерполяция дает разумный результат
                if (ratio < 0 || ratio > 1.0) {
                    ratio = 0.5; // если расчет дал нереальное значение, используем среднее
                }
                
                finalX = (x - vx * TIME_STEP) + vx * TIME_STEP * ratio;
                finalZ = (z - vz * TIME_STEP) + vz * TIME_STEP * ratio;
                finalY = heightDiff;
                
                // Вычисляем погрешность попадания
                impactError = Math.sqrt(Math.pow(finalX - distance, 2) + Math.pow(finalZ, 2));
                
                android.util.Log.d("BallisticCalculator", String.format(
                    "Impact at angle %.2f°: point (%.1f, %.1f) m, error: %.1f m",
                    angle, finalX, finalZ, impactError));
                    
                break;
            }
            
            if (Math.abs(x) > mortar.getMaxRange() * 1.2 || 
                Math.abs(z) > mortar.getMaxRange() * 1.2 || 
                y < heightDiff - 100) {
                break;
            }
        }

        // Применяем масштабирующий коэффициент к максимальной высоте для более реалистичных значений
        maxHeight *= 0.6; // Уменьшаем максимальную высоту на 40%

        return new BallisticResult(angle, time, maxHeight, true,
            trajectoryX.stream().mapToDouble(d -> d).toArray(),
            trajectoryY.stream().mapToDouble(d -> d).toArray(),
            trajectoryZ.stream().mapToDouble(d -> d).toArray(),
            calculateImpactEllipseMajor(mortar, distance, angle),
            calculateImpactEllipseMinor(mortar, distance, angle),
            impactError, finalX, finalY);
    }

    private static double calculateImpactEllipseMajor(MortarType mortar, double distance, double angle) {
        // Большая полуось эллипса зависит от дистанции и калибра
        // Для 82мм миномета: ~1% от дистанции
        // Для 120мм миномета: ~0.8% от дистанции
        double baseError = mortar.getCaliber() == 82 ? 0.01 : 0.008;
        return distance * baseError * (1.0 + Math.sin(Math.toRadians(angle)) * 0.5);
    }

    private static double calculateImpactEllipseMinor(MortarType mortar, double distance, double angle) {
        // Малая полуось эллипса зависит от дистанции и калибра
        // Для 82мм миномета: ~0.7% от дистанции
        // Для 120мм миномета: ~0.5% от дистанции
        double baseError = mortar.getCaliber() == 82 ? 0.007 : 0.005;
        return distance * baseError * (1.0 + Math.cos(Math.toRadians(angle)) * 0.5);
    }

    // Новый метод для итеративного нахождения оптимального угла с учетом всех физических факторов
    private static double refineAngleWithPhysics(MortarType mortar, double distance, double heightDiff, 
                                              WeatherConditions weather, double initialAngle,
                                              double minAngle, double maxAngle) {
        android.util.Log.d("BallisticCalculator", String.format(
            "Starting iterative angle refinement:\n" +
            "Initial angle: %.2f°\n" +
            "Range: %.2f° - %.2f°\n" +
            "Distance: %.1f m, Height: %.1f m\n" +
            "Temperature: %.1f°C, Pressure: %.1f hPa",
            initialAngle, minAngle, maxAngle, 
            distance, heightDiff,
            weather.temperature, weather.pressure
        ));
    
        double currentAngle = initialAngle;
        double bestAngle = initialAngle;
        double minError = Double.MAX_VALUE;
        
        // Адаптивный начальный шаг в зависимости от диапазона углов и расстояния
        double step = Math.min(2.0, (maxAngle - minAngle) / 10.0);
        
        // Логируем информацию о шаге поиска
        android.util.Log.d("BallisticCalculator", String.format(
            "Starting angle search with step: %.2f°", step));
        
        // Максимум 5 итераций для скорости работы
        for (int iteration = 0; iteration < 5; iteration++) {
            // Рассчитываем траекторию с текущим углом
            BallisticResult result = calculateSingleTrajectory(mortar, distance, heightDiff, weather, currentAngle);
            
            if (result.isValid && !Double.isNaN(result.impactError) && !Double.isInfinite(result.impactError)) {
                double error = result.impactError;
                
                android.util.Log.d("BallisticCalculator", String.format(
                    "Iteration %d: Angle %.2f° -> Error %.1f m",
                    iteration+1, currentAngle, error));
                
                // Если ошибка меньше, чем минимальная найденная, обновляем лучший угол
                if (error < minError) {
                    minError = error;
                    bestAngle = currentAngle;
                }
                
                // Если ошибка достаточно мала, завершаем поиск
                if (error < 10.0) { // Допустимая ошибка 10 метров
                    break;
                }
                
                // Корректировка угла в зависимости от ошибки
                if (!Double.isNaN(result.impactX) && !Double.isInfinite(result.impactX) && result.impactX < distance) {
                    // Недолет - увеличиваем угол
                    currentAngle += step;
                } else {
                    // Перелет - уменьшаем угол
                    currentAngle -= step;
                }
                
                // Уменьшаем шаг с каждой итерацией
                step *= 0.7;
                
                // Проверяем не вышли ли за границы допустимого диапазона углов
                currentAngle = Math.max(minAngle, Math.min(maxAngle, currentAngle));
            } else {
                android.util.Log.d("BallisticCalculator", String.format(
                    "Iteration %d: Angle %.2f° produced invalid trajectory, adjusting",
                    iteration+1, currentAngle));
                    
                // Если траектория недействительна, делаем большой шаг в сторону допустимого диапазона
                currentAngle = (minAngle + maxAngle) / 2.0;
            }
        }
        
        android.util.Log.d("BallisticCalculator", String.format(
            "Angle refinement complete:\n" +
            "Initial angle: %.2f°\n" +
            "Final angle: %.2f°\n" +
            "Minimum error: %.1f m",
            initialAngle, bestAngle, minError
        ));
        
        return bestAngle;
    }
} 