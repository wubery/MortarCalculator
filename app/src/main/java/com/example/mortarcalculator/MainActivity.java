package com.example.mortarcalculator;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import java.io.IOException;
import java.io.InputStream;

public class MainActivity extends AppCompatActivity 
        implements MortarSettingsDialog.OnMortarSettingsListener,
                   TargetSettingsDialog.OnTargetSettingsListener,
                   WeatherSettingsDialog.OnWeatherSettingsListener {

    private static final String TAG = "MainActivity";
    private ImageView mapImageView;
    private TouchableImageView touchableImageView;
    private TextView targetAnglesText;
    private BallisticCalculator.WeatherConditions currentWeather;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Установка полноэкранного режима
        getWindow().setFlags(
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
        );
        
        // Скрытие системных UI элементов
        getWindow().getDecorView().setSystemUiVisibility(
            View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
            | View.SYSTEM_UI_FLAG_FULLSCREEN
            | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        );
        
        setContentView(R.layout.activity_main);

        mapImageView = findViewById(R.id.mapImageView);
        touchableImageView = findViewById(R.id.map_view);
        
        // Устанавливаем правильный ScaleType для карты
        mapImageView.setScaleType(ImageView.ScaleType.MATRIX);
        
        touchableImageView.setMapImageView(mapImageView);

        targetAnglesText = findViewById(R.id.targetAnglesText);
        touchableImageView.setTargetAnglesText(targetAnglesText);

        // Инициализация метеоусловий по умолчанию
        currentWeather = new BallisticCalculator.WeatherConditions(0.0, 1013.25, 50.0, 0.0, 0.0);

        // Добавляем кнопку настроек погоды
        Button weatherButton = findViewById(R.id.weather_button);
        weatherButton.setOnClickListener(v -> showWeatherSettingsDialog());

        Button resetButton = findViewById(R.id.resetButton);
        resetButton.setOnClickListener(v -> touchableImageView.reset());

        loadMapImage();
    }

    private void loadMapImage() {
        try {
            Log.d(TAG, "Starting to load map image");
            InputStream inputStream = getAssets().open("al_basrah/map.png");
            Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
            
            if (bitmap != null) {
                Log.d(TAG, "Map image loaded successfully. Size: " + bitmap.getWidth() + "x" + bitmap.getHeight());
                mapImageView.setImageBitmap(bitmap);
                
                // Ждем пока view получит размеры
                mapImageView.post(() -> {
                    // Инициализируем начальное положение и масштаб карты
                    Matrix matrix = new Matrix();
                    float scale = Math.min(
                        (float) mapImageView.getWidth() / bitmap.getWidth(),
                        (float) mapImageView.getHeight() / bitmap.getHeight()
                    );
                    
                    // Устанавливаем начальный масштаб немного меньше, чтобы видеть всю карту
                    scale *= 0.9f;
                    matrix.setScale(scale, scale);
                    
                    // Центрируем карту
                    float dx = (mapImageView.getWidth() - bitmap.getWidth() * scale) / 2;
                    float dy = (mapImageView.getHeight() - bitmap.getHeight() * scale) / 2;
                    matrix.postTranslate(dx, dy);
                    
                    mapImageView.setImageMatrix(matrix);
                    touchableImageView.setInitialMatrix(matrix);
                });
            } else {
                Log.e(TAG, "Failed to decode map image - bitmap is null");
            }
            
            inputStream.close();
        } catch (IOException e) {
            Log.e(TAG, "Error loading map image: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void showWeatherSettingsDialog() {
        Log.d(TAG, String.format(
            "Showing weather settings dialog with current values:\n" +
            "Temperature: %.1f°C\n" +
            "Pressure: %.1f hPa\n" +
            "Humidity: %.1f%%\n" +
            "Wind: %.1f m/s @ %.1f°",
            currentWeather.temperature, currentWeather.pressure, currentWeather.humidity,
            currentWeather.windSpeed, currentWeather.windDirection));
            
        WeatherSettingsDialog dialog = WeatherSettingsDialog.newInstance(currentWeather);
        dialog.show(getSupportFragmentManager(), "weather_settings");
    }

    @Override
    public void onMortarSettingsChanged(int mortarIndex, double elevation) {
        touchableImageView.updateMortarSettings(mortarIndex, elevation);
    }

    @Override
    public void onTargetSettingsChanged(double elevation) {
        touchableImageView.updateTargetSettings(elevation);
    }

    @Override
    public void onMortarDeleted() {
        touchableImageView.deleteMortar();
    }

    @Override
    public void onWeatherSettingsChanged(BallisticCalculator.WeatherConditions weather) {
        Log.d(TAG, String.format(
            "Weather settings changed in MainActivity:\n" +
            "Temperature: %.1f°C\n" +
            "Pressure: %.1f hPa\n" +
            "Humidity: %.1f%%\n" +
            "Wind: %.1f m/s @ %.1f°",
            weather.temperature, weather.pressure, weather.humidity,
            weather.windSpeed, weather.windDirection));
        
        currentWeather = weather;
        touchableImageView.onWeatherSettingsChanged(weather);
    }
}