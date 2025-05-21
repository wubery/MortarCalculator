package com.example.mortarcalculator;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;
import android.content.DialogInterface;

public class WeatherSettingsDialog extends DialogFragment {
    private OnWeatherSettingsListener listener;
    private BallisticCalculator.WeatherConditions currentWeather;
    private static final String TAG = "WeatherSettingsDialog";
    
    // Добавляем поля для EditText
    private EditText temperatureEdit;
    private EditText pressureEdit;
    private EditText humidityEdit;
    private EditText windSpeedEdit;
    private Spinner windDirectionSpinner;

    private static final String[] WIND_DIRECTIONS = {
        "Север (0°)",
        "Северо-восток (45°)",
        "Восток (90°)",
        "Юго-восток (135°)",
        "Юг (180°)",
        "Юго-запад (225°)",
        "Запад (270°)",
        "Северо-запад (315°)"
    };

    private static final double[] WIND_DIRECTION_ANGLES = {
        0.0, 45.0, 90.0, 135.0, 180.0, 225.0, 270.0, 315.0
    };

    public interface OnWeatherSettingsListener {
        void onWeatherSettingsChanged(BallisticCalculator.WeatherConditions weather);
    }

    public static WeatherSettingsDialog newInstance(BallisticCalculator.WeatherConditions weather) {
        WeatherSettingsDialog dialog = new WeatherSettingsDialog();
        dialog.currentWeather = weather;
        return dialog;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        try {
            listener = (OnWeatherSettingsListener) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(context.toString() + " must implement OnWeatherSettingsListener");
        }
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireActivity());
        LayoutInflater inflater = requireActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.dialog_weather_settings, null);

        // Инициализируем поля класса
        temperatureEdit = view.findViewById(R.id.weather_temperature);
        pressureEdit = view.findViewById(R.id.weather_pressure);
        humidityEdit = view.findViewById(R.id.weather_humidity);
        windSpeedEdit = view.findViewById(R.id.weather_wind_speed);
        windDirectionSpinner = view.findViewById(R.id.weather_wind_direction);

        // Настраиваем Spinner для направления ветра
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
            requireContext(),
            android.R.layout.simple_spinner_item,
            WIND_DIRECTIONS
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        windDirectionSpinner.setAdapter(adapter);

        // Устанавливаем текущие значения
        if (currentWeather != null) {
            temperatureEdit.setText(String.format("%.1f", currentWeather.temperature));
            pressureEdit.setText(String.format("%.2f", currentWeather.pressure));
            humidityEdit.setText(String.format("%.1f", currentWeather.humidity));
            windSpeedEdit.setText(String.format("%.1f", currentWeather.windSpeed));
            
            // Устанавливаем текущее направление ветра в Spinner
            double currentDirection = currentWeather.windDirection;
            int closestIndex = 0;
            double minDiff = Double.MAX_VALUE;
            for (int i = 0; i < WIND_DIRECTION_ANGLES.length; i++) {
                double diff = Math.abs(WIND_DIRECTION_ANGLES[i] - currentDirection);
                if (diff < minDiff) {
                    minDiff = diff;
                    closestIndex = i;
                }
            }
            windDirectionSpinner.setSelection(closestIndex);
            
            Log.d(TAG, String.format(
                "Initial weather values set:\n" +
                "Temperature: %.1f°C\n" +
                "Pressure: %.1f hPa\n" +
                "Humidity: %.1f%%\n" +
                "Wind: %.1f m/s @ %.1f°",
                currentWeather.temperature, currentWeather.pressure, currentWeather.humidity,
                currentWeather.windSpeed, currentWeather.windDirection));
        }

        builder.setView(view)
               .setTitle("Метеоусловия")
               .setPositiveButton("OK", null)
               .setNegativeButton("Отмена", (dialog, id) -> {
                   Log.d(TAG, "Dialog cancelled");
               });

        AlertDialog dialog = builder.create();
        
        dialog.setOnShowListener(dialogInterface -> {
            Button button = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            button.setOnClickListener(v -> {
                try {
                    String tempStr = temperatureEdit.getText().toString().replace(',', '.');
                    String pressStr = pressureEdit.getText().toString().replace(',', '.');
                    String humidStr = humidityEdit.getText().toString().replace(',', '.');
                    String windSpeedStr = windSpeedEdit.getText().toString().replace(',', '.');
                    
                    Log.d(TAG, String.format(
                        "Raw input values:\n" +
                        "Temperature: %s\n" +
                        "Pressure: %s\n" +
                        "Humidity: %s\n" +
                        "Wind Speed: %s\n" +
                        "Wind Direction: %s",
                        tempStr, pressStr, humidStr, windSpeedStr, 
                        WIND_DIRECTIONS[windDirectionSpinner.getSelectedItemPosition()]));

                    double temperature = Double.parseDouble(tempStr);
                    // Проверяем температуру
                    if (temperature < -70.0 || temperature > 70.0) {
                        temperatureEdit.setError("Температура должна быть от -70°C до +70°C");
                        return;
                    }
                    
                    double pressure = Double.parseDouble(pressStr);
                    // Проверяем давление
                    if (pressure < 800.0 || pressure > 1100.0) {
                        pressureEdit.setError("Давление должно быть от 800 до 1100 гПа");
                        return;
                    }
                    
                    double humidity = Double.parseDouble(humidStr);
                    // Проверяем влажность
                    if (humidity < 0.0 || humidity > 100.0) {
                        humidityEdit.setError("Влажность должна быть от 0% до 100%");
                        return;
                    }
                    
                    double windSpeed = Double.parseDouble(windSpeedStr);
                    // Проверяем скорость ветра
                    if (windSpeed < 0.0 || windSpeed > 100.0) {
                        windSpeedEdit.setError("Скорость ветра должна быть от 0 до 100 м/с");
                        return;
                    }
                    
                    double windDirection = WIND_DIRECTION_ANGLES[windDirectionSpinner.getSelectedItemPosition()];

                    Log.d(TAG, String.format(
                        "Parsed weather values:\n" +
                        "Temperature: %.1f°C\n" +
                        "Pressure: %.1f hPa\n" +
                        "Humidity: %.1f%%\n" +
                        "Wind: %.1f m/s @ %.1f°",
                        temperature, pressure, humidity, windSpeed, windDirection));

                    BallisticCalculator.WeatherConditions weather = 
                        new BallisticCalculator.WeatherConditions(
                            temperature, pressure, humidity, windSpeed, windDirection);
                    listener.onWeatherSettingsChanged(weather);
                    dialog.dismiss();
                } catch (NumberFormatException e) {
                    Log.e(TAG, "Error parsing weather values: " + e.getMessage());
                    // Показываем сообщение об ошибке
                    temperatureEdit.setError("Введите корректное число");
                    pressureEdit.setError("Введите корректное число");
                    humidityEdit.setError("Введите корректное число");
                    windSpeedEdit.setError("Введите корректное число");
                }
            });
        });

        return dialog;
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        super.onDismiss(dialog);
        // Убираем обработку значений из onDismiss, так как они уже обработаны в onPositiveButton
    }
} 