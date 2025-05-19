package com.example.mortarcalculator;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import java.io.IOException;
import java.io.InputStream;

public class MainActivity extends AppCompatActivity 
        implements MortarSettingsDialog.OnMortarSettingsListener,
                   TargetSettingsDialog.OnTargetSettingsListener {

    private static final String TAG = "MainActivity";
    private ImageView mapImageView;
    private TouchableImageView touchableImageView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mapImageView = findViewById(R.id.mapImageView);
        touchableImageView = findViewById(R.id.touchableImageView);
        
        // Устанавливаем правильный ScaleType для карты
        mapImageView.setScaleType(ImageView.ScaleType.MATRIX);
        
        touchableImageView.setMapImageView(mapImageView);

        TextView targetAnglesText = findViewById(R.id.targetAnglesText);
        Button resetButton = findViewById(R.id.resetButton);
        touchableImageView.setTargetAnglesText(targetAnglesText);

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
}