package com.example.mortarcalculator;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PointF;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatImageView;
import java.util.ArrayList;
import java.util.List;
import android.util.Log;
import androidx.fragment.app.FragmentActivity;
import androidx.appcompat.app.AppCompatActivity;

public class TouchableImageView extends AppCompatImageView {
    private ImageView mapImageView;
    private TextView targetAnglesText;
    private final List<GeoPoint> mortars = new ArrayList<>();
    private GeoPoint targetPoint;
    private final Paint paint = new Paint();
    private final Paint circlePaint = new Paint();
    private final int[] mortarColors = {Color.RED, Color.GREEN, Color.YELLOW, Color.CYAN, Color.MAGENTA};
    
    // Константы для размера карты (35км x 35км)
    private static final float MAP_SIZE_METERS = 35000f;
    private static final float MAP_DEGREES = 1.0f; // размер карты в градусах (1 градус ≈ 35км на этой широте)
    private static final float METERS_PER_DEGREE = MAP_SIZE_METERS / MAP_DEGREES; // метров на градус
    private static final int MAP_RESOLUTION = 4096; // разрешение карты в пикселях
    private static final float METERS_PER_PIXEL = MAP_SIZE_METERS / MAP_RESOLUTION;

    // Для обработки масштабирования и перемещения
    private ScaleGestureDetector scaleDetector;
    private GestureDetector gestureDetector;
    private Matrix matrix = new Matrix();
    private float[] matrixValues = new float[9];
    private static final float MIN_ZOOM = 0.1f;
    private static final float MAX_ZOOM = 5.0f;

    private GeoPoint selectedMortar; // Добавляем поле для хранения выбранного миномета

    private BallisticCalculator.WeatherConditions currentWeather;

    private static final String TAG = "TouchableImageView";

    public TouchableImageView(Context context) {
        super(context);
        init(context);
    }

    public TouchableImageView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    private void init(Context context) {
        paint.setTextSize(30);
        paint.setAntiAlias(true);
        paint.setStyle(Paint.Style.FILL);

        circlePaint.setColor(Color.WHITE);
        circlePaint.setStyle(Paint.Style.STROKE);
        circlePaint.setStrokeWidth(2);
        circlePaint.setAlpha(128);

        // Инициализация метеоусловий по умолчанию
        currentWeather = new BallisticCalculator.WeatherConditions(0.0, 1013.25, 50.0, 0.0, 0.0);

        scaleDetector = new ScaleGestureDetector(context, new ScaleListener());
        gestureDetector = new GestureDetector(context, new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onSingleTapConfirmed(MotionEvent e) {
                handleSingleTap(e.getX(), e.getY());
                return true;
            }

            @Override
            public boolean onDoubleTap(MotionEvent e) {
                handleDoubleTap(e.getX(), e.getY());
                return true;
            }

            @Override
            public void onLongPress(MotionEvent e) {
                handleLongPress(e.getX(), e.getY());
            }

            @Override
            public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
                matrix.postTranslate(-distanceX, -distanceY);
                mapImageView.setImageMatrix(matrix);
                invalidate();
                return true;
            }
        });

        setOnTouchListener((v, event) -> {
            boolean retVal = scaleDetector.onTouchEvent(event);
            retVal = gestureDetector.onTouchEvent(event) || retVal;
            return true;
        });
    }

    private class ScaleListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {
        @Override
        public boolean onScale(ScaleGestureDetector detector) {
            float scaleFactor = detector.getScaleFactor();
            
            // Получаем текущий масштаб
            matrix.getValues(matrixValues);
            float currentScale = matrixValues[Matrix.MSCALE_X];
            
            // Проверяем, не выходит ли за пределы
            if ((currentScale * scaleFactor) > MAX_ZOOM) {
                scaleFactor = MAX_ZOOM / currentScale;
            } else if ((currentScale * scaleFactor) < MIN_ZOOM) {
                scaleFactor = MIN_ZOOM / currentScale;
            }

            // Масштабируем относительно центра жеста
            matrix.postScale(scaleFactor, scaleFactor, detector.getFocusX(), detector.getFocusY());
            mapImageView.setImageMatrix(matrix);
            invalidate();
            return true;
        }
    }

    public void setMapImageView(ImageView imageView) {
        this.mapImageView = imageView;
        this.mapImageView.setScaleType(ImageView.ScaleType.MATRIX);
        this.mapImageView.setImageMatrix(matrix);
    }

    public void setTargetAnglesText(TextView textView) {
        this.targetAnglesText = textView;
    }

    public void reset() {
        mortars.clear();
        targetPoint = null;
        matrix.reset();
        mapImageView.setImageMatrix(matrix);
        if (targetAnglesText != null) {
            targetAnglesText.setText("Цель: не установлена\nМиномёты: 0");
        }
        invalidate();
    }

    private float metersToPixels(float meters) {
        // Преобразуем метры в пиксели экрана
        return (meters / MAP_SIZE_METERS) * mapImageView.getWidth();
    }

    private double[] convertToLocalCoordinates(double targetLat, double targetLon, GeoPoint mortar) {
        // Convert to local coordinates in meters
        double dLon = targetLon - mortar.getLongitude();
        double dLat = targetLat - mortar.getLatitude();
        
        // Convert degree differences to meters using the map scale
        double dx = dLon * (MAP_SIZE_METERS / MAP_DEGREES);
        double dy = dLat * (MAP_SIZE_METERS / MAP_DEGREES);
        
        // Add detailed debug logging
        Log.d("MortarCalc", String.format(
            "Coordinate conversion:\n" +
            "Mortar: (%.6f, %.6f)\n" +
            "Target: (%.6f, %.6f)\n" +
            "dx: %.1fm, dy: %.1fm\n" +
            "Total distance: %.1fm",
            mortar.getLatitude(), mortar.getLongitude(),
            targetLat, targetLon,
            dx, dy,
            Math.sqrt(dx*dx + dy*dy)
        ));

        return new double[]{dx, dy};
    }

    private void handleSingleTap(float x, float y) {
        if (mortars.isEmpty()) return;
        
        // Get coordinates in map space
        Matrix inverse = new Matrix();
        matrix.invert(inverse);
        float[] points = new float[] { x, y };
        inverse.mapPoints(points);
        
        // Convert to normalized coordinates (0-1 range)
        float normalizedX = points[0] / MAP_RESOLUTION;
        float normalizedY = points[1] / MAP_RESOLUTION;
        
        // Clamp coordinates to map bounds
        normalizedX = Math.max(0, Math.min(1, normalizedX));
        normalizedY = Math.max(0, Math.min(1, normalizedY));
        
        // Convert to geographic coordinates
        double lon = 47.7 + normalizedX * MAP_DEGREES; // Обновлено для масштаба 35км
        double lat = 30.4 + (1 - normalizedY) * MAP_DEGREES; // Обновлено для масштаба 35км
        
        Log.d("MortarCalc", String.format(
            "Single tap: screen(%.1f, %.1f) -> map(%.1f, %.1f) -> normalized(%.3f, %.3f) -> geo(%.6f, %.6f)",
            x, y, points[0], points[1], normalizedX, normalizedY, lat, lon
        ));
        
        targetPoint = new GeoPoint(lat, lon, 0.0); // Устанавливаем начальную высоту 0 метров
        calculateAndDisplayResults();
        invalidate();
    }

    private void handleDoubleTap(float x, float y) {
        if (mortars.size() >= 5) return;
        
        // Get coordinates in map space
        Matrix inverse = new Matrix();
        matrix.invert(inverse);
        float[] points = new float[] { x, y };
        inverse.mapPoints(points);
        
        // Convert to normalized coordinates (0-1 range)
        float normalizedX = points[0] / MAP_RESOLUTION;
        float normalizedY = points[1] / MAP_RESOLUTION;
        
        // Clamp coordinates to map bounds
        normalizedX = Math.max(0, Math.min(1, normalizedX));
        normalizedY = Math.max(0, Math.min(1, normalizedY));
        
        // Convert to geographic coordinates
        double lon = 47.7 + normalizedX * MAP_DEGREES; // Обновлено для масштаба 35км
        double lat = 30.4 + (1 - normalizedY) * MAP_DEGREES; // Обновлено для масштаба 35км
        
        Log.d("MortarCalc", String.format(
            "Double tap: screen(%.1f, %.1f) -> map(%.1f, %.1f) -> normalized(%.3f, %.3f) -> geo(%.6f, %.6f)",
            x, y, points[0], points[1], normalizedX, normalizedY, lat, lon
        ));
        
        mortars.add(new GeoPoint(lat, lon));
        updateStatusText();
        if (targetPoint != null) {
            calculateAndDisplayResults();
        }
        invalidate();
    }

    private void handleLongPress(float x, float y) {
        // Получаем координаты в пространстве карты
        Matrix inverse = new Matrix();
        matrix.invert(inverse);
        float[] points = new float[] { x, y };
        inverse.mapPoints(points);

        // Проверяем, попали ли мы в миномет или цель
        selectedMortar = null;
        boolean touchedTarget = false;
        float touchRadius = 40; // радиус касания в пикселях

        // Проверяем минометы
        for (GeoPoint mortar : mortars) {
            float normalizedLon = (float) ((mortar.getLongitude() - 47.7) / MAP_DEGREES);
            float normalizedLat = (float) (1 - (mortar.getLatitude() - 30.4) / MAP_DEGREES);
            
            float baseX = normalizedLon * MAP_RESOLUTION;
            float baseY = normalizedLat * MAP_RESOLUTION;
            
            if (Math.abs(points[0] - baseX) < touchRadius && 
                Math.abs(points[1] - baseY) < touchRadius) {
                selectedMortar = mortar;
                break;
            }
        }

        // Проверяем цель
        if (targetPoint != null) {
            float normalizedLon = (float) ((targetPoint.getLongitude() - 47.7) / MAP_DEGREES);
            float normalizedLat = (float) (1 - (targetPoint.getLatitude() - 30.4) / MAP_DEGREES);
            
            float baseX = normalizedLon * MAP_RESOLUTION;
            float baseY = normalizedLat * MAP_RESOLUTION;
            
            if (Math.abs(points[0] - baseX) < touchRadius && 
                Math.abs(points[1] - baseY) < touchRadius) {
                touchedTarget = true;
            }
        }

        // Показываем соответствующий диалог
        if (selectedMortar != null) {
            showMortarSettingsDialog(selectedMortar);
        } else if (touchedTarget) {
            showTargetSettingsDialog();
        }
    }

    private void showMortarSettingsDialog(GeoPoint mortar) {
        FragmentActivity activity = (FragmentActivity) getContext();
        if (activity != null) {
            int mortarIndex = 0;
            for (int i = 0; i < MortarType.PREDEFINED_MORTARS.length; i++) {
                if (mortar.getMortarType().getName().equals(MortarType.PREDEFINED_MORTARS[i].getName())) {
                    mortarIndex = i;
                    break;
                }
            }
            MortarSettingsDialog dialog = MortarSettingsDialog.newInstance(mortarIndex, mortar.getElevation());
            dialog.show(activity.getSupportFragmentManager(), "mortar_settings");
        }
    }

    private void showTargetSettingsDialog() {
        FragmentActivity activity = (FragmentActivity) getContext();
        if (activity != null && targetPoint != null) {
            TargetSettingsDialog dialog = TargetSettingsDialog.newInstance(targetPoint.getElevation());
            dialog.show(activity.getSupportFragmentManager(), "target_settings");
        }
    }

    private void showWeatherSettingsDialog() {
        WeatherSettingsDialog dialog = WeatherSettingsDialog.newInstance(currentWeather);
        dialog.show(((AppCompatActivity) getContext()).getSupportFragmentManager(), "weather_settings");
    }

    public void onWeatherSettingsChanged(BallisticCalculator.WeatherConditions weather) {
        Log.d(TAG, String.format(
            "Weather settings changed in TouchableImageView:\n" +
            "Temperature: %.1f°C\n" +
            "Pressure: %.1f hPa\n" +
            "Humidity: %.1f%%\n" +
            "Wind: %.1f m/s @ %.1f°",
            weather.temperature, weather.pressure, weather.humidity,
            weather.windSpeed, weather.windDirection));
            
        this.currentWeather = weather;
        if (targetPoint != null) {
            Log.d(TAG, "Recalculating results with new weather conditions");
            calculateAndDisplayResults();
        }
    }

    private void calculateAndDisplayResults() {
        if (mortars.isEmpty() || targetPoint == null || targetAnglesText == null) return;

        StringBuilder results = new StringBuilder("Результаты:\n\n");
        for (int i = 0; i < mortars.size(); i++) {
            GeoPoint mortar = mortars.get(i);
            double[] delta = convertToLocalCoordinates(
                    targetPoint.getLatitude(),
                    targetPoint.getLongitude(),
                    mortar
            );

            double elevationDiff = targetPoint.getElevation() - mortar.getElevation();
            double distance = Math.sqrt(delta[0]*delta[0] + delta[1]*delta[1]);
            
            Log.d("MortarCalc", String.format(
                "Mortar %d:\n" +
                "- Type: %s\n" +
                "- Distance: %.1fm (min: %.1fm, max: %.1fm)\n" +
                "- Delta: dx=%.1fm, dy=%.1fm\n" +
                "- Elevation diff: %.1fm\n" +
                "- Coordinates: mortar(%.6f, %.6f), target(%.6f, %.6f)\n" +
                "- Weather: temp=%.1f°C, pressure=%.1fhPa, humidity=%.1f%%, " +
                "wind=%.1fm/s @ %.1f°",
                i+1,
                mortar.getMortarType().getName(),
                distance,
                mortar.getMortarType().getMinRange(),
                mortar.getMortarType().getMaxRange(),
                delta[0],
                delta[1],
                elevationDiff,
                mortar.getLatitude(), mortar.getLongitude(),
                targetPoint.getLatitude(), targetPoint.getLongitude(),
                currentWeather.temperature,
                currentWeather.pressure,
                currentWeather.humidity,
                currentWeather.windSpeed,
                currentWeather.windDirection
            ));

            BallisticCalculator.BallisticResult[] trajectories = 
                BallisticCalculator.calculateTrajectory(mortar.getMortarType(), distance, 
                                                      elevationDiff, currentWeather);

            String color = String.format("#%06X", (0xFFFFFF & mortarColors[i]));
            results.append(String.format(
                    "M%d (цвет: %s)\n" +
                    "• Тип: %s\n" +
                    "• Азимут: %.1f°\n" +
                    "• Дистанция: %.0fм\n" +
                    "• Превышение: %.1fм\n" +
                    "• Метеоусловия:\n" +
                    "  - Температура: %.1f°C\n" +
                    "  - Давление: %.1f гПа\n" +
                    "  - Влажность: %.1f%%\n" +
                    "  - Ветер: %.1f м/с @ %.1f°\n",
                    i+1,
                    color,
                    mortar.getMortarType().getName(),
                    calculateAzimuth(mortar, targetPoint),
                    distance,
                    elevationDiff,
                    currentWeather.temperature,
                    currentWeather.pressure,
                    currentWeather.humidity,
                    currentWeather.windSpeed,
                    currentWeather.windDirection
            ));

            if (trajectories[0].isValid || trajectories[1].isValid) {
                results.append("• Навесная траектория:\n");
                if (trajectories[0].isValid) {
                    results.append(String.format(
                            "  - Угол: %.1f°\n" +
                            "  - Макс. высота: %.0fм\n" +
                            "  - Время полёта: %.1fс\n",
                            trajectories[0].angle,
                            trajectories[0].maxHeight,
                            trajectories[0].timeOfFlight
                    ));
                } else {
                    results.append("  - Недоступна\n");
                }

                results.append("• Настильная траектория:\n");
                if (trajectories[1].isValid) {
                    results.append(String.format(
                            "  - Угол: %.1f°\n" +
                            "  - Макс. высота: %.0fм\n" +
                            "  - Время полёта: %.1fс\n",
                            trajectories[1].angle,
                            trajectories[1].maxHeight,
                            trajectories[1].timeOfFlight
                    ));
                } else {
                    results.append("  - Недоступна\n");
                }
            } else {
                results.append(String.format(
                        "Цель вне досягаемости (%.0f м)\n" +
                        "Допустимая дальность: %.0f - %.0f м\n",
                        distance,
                        mortar.getMortarType().getMinRange(),
                        mortar.getMortarType().getMaxRange()
                ));
            }
            results.append("\n");
        }
        targetAnglesText.setText(results.toString());
    }

    private void updateStatusText() {
        if (targetAnglesText != null) {
            if (mortars.isEmpty()) {
                targetAnglesText.setText("Дважды коснитесь карты чтобы добавить миномёт\n(максимум 5 минометов)");
            } else if (mortars.size() < 5) {
                targetAnglesText.setText(String.format(
                        "Миномёты: %d\n" +
                        "• Дважды коснитесь для добавления миномёта\n" +
                        "• Один раз для выбора цели",
                        mortars.size()
                ));
            } else {
                targetAnglesText.setText("Коснитесь карты один раз для выбора цели");
            }
        }
    }

    private double calculateAzimuth(GeoPoint from, GeoPoint to) {
        double dLon = Math.toRadians(to.getLongitude() - from.getLongitude());
        double lat1 = Math.toRadians(from.getLatitude());
        double lat2 = Math.toRadians(to.getLatitude());
        double y = Math.sin(dLon) * Math.cos(lat2);
        double x = Math.cos(lat1) * Math.sin(lat2) - Math.sin(lat1) * Math.cos(lat2) * Math.cos(dLon);
        double azimuth = Math.toDegrees(Math.atan2(y, x));
        
        // Корректируем азимут с учетом ветра
        if (currentWeather != null && currentWeather.windSpeed > 0) {
            // Рассчитываем угол между направлением ветра и азимутом
            double windAzimuthDiff = (currentWeather.windDirection - azimuth + 360) % 360;
            
            // Корректируем азимут в зависимости от силы ветра и его направления
            // Чем сильнее ветер и чем больше угол между ветром и азимутом, тем больше коррекция
            double windCorrection = currentWeather.windSpeed * Math.sin(Math.toRadians(windAzimuthDiff)) * 0.5;
            
            // Применяем коррекцию
            azimuth = (azimuth + windCorrection + 360) % 360;
            
            Log.d("MortarCalc", String.format(
                "Azimuth correction:\n" +
                "Base azimuth: %.1f°\n" +
                "Wind direction: %.1f°\n" +
                "Wind speed: %.1f m/s\n" +
                "Wind-azimuth difference: %.1f°\n" +
                "Correction: %.1f°\n" +
                "Final azimuth: %.1f°",
                azimuth - windCorrection,
                currentWeather.windDirection,
                currentWeather.windSpeed,
                windAzimuthDiff,
                windCorrection,
                azimuth
            ));
        }
        
        return (azimuth + 360) % 360;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        
        // Get current scale from matrix
        matrix.getValues(matrixValues);
        float currentScale = matrixValues[Matrix.MSCALE_X];
        
        // Draw mortars and their range circles
        for (int i = 0; i < mortars.size(); i++) {
            GeoPoint mortar = mortars.get(i);
            
            // Convert geographic coordinates to normalized map coordinates
            float normalizedLon = (float) ((mortar.getLongitude() - 47.7) / MAP_DEGREES);
            float normalizedLat = (float) (1 - (mortar.getLatitude() - 30.4) / MAP_DEGREES); // Flip Y coordinate
            
            // Convert to pixel coordinates
            float baseX = normalizedLon * MAP_RESOLUTION;
            float baseY = normalizedLat * MAP_RESOLUTION;
            
            // Apply current transformation matrix
            float[] points = new float[] { baseX, baseY };
            matrix.mapPoints(points);
            float x = points[0];
            float y = points[1];
            
            // Draw maximum range circle using the mortar's actual maximum range
            float maxRange = (float) mortar.getMortarType().getMaxRange();
            float radiusPixels = (maxRange / MAP_SIZE_METERS) * MAP_RESOLUTION;
            
            paint.setColor(mortarColors[i]);
            circlePaint.setColor(mortarColors[i]);
            circlePaint.setStyle(Paint.Style.STROKE);
            circlePaint.setStrokeWidth(2);
            circlePaint.setAlpha(128);
            canvas.drawCircle(x, y, radiusPixels * currentScale, circlePaint);
            
            // Draw mortar icon (fixed size regardless of zoom)
            paint.setStyle(Paint.Style.FILL);
            canvas.drawCircle(x, y, 10, paint);
            canvas.drawText("M" + (i + 1), x + 15, y - 15, paint);
        }

        // Draw target if set
        if (targetPoint != null) {
            // Convert geographic coordinates to normalized map coordinates
            float normalizedLon = (float) ((targetPoint.getLongitude() - 47.7) / MAP_DEGREES);
            float normalizedLat = (float) (1 - (targetPoint.getLatitude() - 30.4) / MAP_DEGREES); // Flip Y coordinate
            
            // Convert to pixel coordinates
            float baseX = normalizedLon * MAP_RESOLUTION;
            float baseY = normalizedLat * MAP_RESOLUTION;
            
            // Apply current transformation matrix
            float[] points = new float[] { baseX, baseY };
            matrix.mapPoints(points);
            float x = points[0];
            float y = points[1];
            
            // Draw target crosshair
            paint.setColor(Color.RED);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(2);
            float crossSize = 20;
            canvas.drawLine(x - crossSize, y, x + crossSize, y, paint);
            canvas.drawLine(x, y - crossSize, x, y + crossSize, paint);
        }
    }

    public void setInitialMatrix(Matrix initialMatrix) {
        matrix.set(initialMatrix);
        invalidate();
    }

    public void updateMortarSettings(int mortarIndex, double elevation) {
        if (selectedMortar != null) {
            selectedMortar.setMortarType(MortarType.PREDEFINED_MORTARS[mortarIndex]);
            selectedMortar.setElevation(elevation);
            calculateAndDisplayResults();
            invalidate();
        }
    }

    public void updateTargetSettings(double elevation) {
        if (targetPoint != null) {
            targetPoint.setElevation(elevation);
            // Пересчитываем результаты с новой высотой цели
            calculateAndDisplayResults();
            invalidate();
        }
    }

    public void deleteMortar() {
        if (selectedMortar != null) {
            mortars.remove(selectedMortar);
            selectedMortar = null;
            updateStatusText();
            if (targetPoint != null) {
                calculateAndDisplayResults();
            }
            invalidate();
        }
    }
}