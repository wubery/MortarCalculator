package com.example.mortarcalculator;

public class MortarType {
    private final String name;
    private final int caliber; // в миллиметрах
    private final double muzzleVelocity; // начальная скорость м/с
    private final double minRange; // минимальная дальность в метрах
    private final double maxRange; // максимальная дальность в метрах
    private final double minElevation; // минимальный угол возвышения
    private final double maxElevation; // максимальный угол возвышения
    private final double barrelLength; // длина ствола в мм
    private AmmoType ammoType; // текущий тип боеприпаса

    public MortarType(String name, int caliber, double muzzleVelocity, double minRange, 
                     double maxRange, double minElevation, double maxElevation, 
                     double barrelLength) {
        this.name = name;
        this.caliber = caliber;
        this.muzzleVelocity = muzzleVelocity;
        this.minRange = minRange;
        this.maxRange = maxRange;
        this.minElevation = minElevation;
        this.maxElevation = maxElevation;
        this.barrelLength = barrelLength;
        // Устанавливаем боеприпас по умолчанию в зависимости от калибра
        this.ammoType = caliber == 82 ? AmmoType.PREDEFINED_AMMO_82MM[0] : AmmoType.PREDEFINED_AMMO_120MM[0];
    }

    // Предопределенные типы минометов
    public static final MortarType[] PREDEFINED_MORTARS = {
        new MortarType(
            "2Б14 Поднос (82мм)",
            82,
            211.0,
            50.0,
            4000.0,
            1.0,
            89.0,
            1220.0
        ),
        new MortarType(
            "M252 (81мм)",
            81,
            200.0,
            70.0,
            5935.0,
            1.0,
            89.0,
            1310.0
        ),
        new MortarType(
            "2Б11 Сани (120мм)",
            120,
            272.0,
            100.0,
            7100.0,
            1.0,
            89.0,
            1862.0
        )
    };

    // Геттеры
    public String getName() { return name; }
    public int getCaliber() { return caliber; }
    public double getMuzzleVelocity() { return muzzleVelocity; }
    public double getMinRange() { return minRange; }
    public double getMaxRange() { return maxRange; }
    public double getMinElevation() { return minElevation; }
    public double getMaxElevation() { return maxElevation; }
    public double getBarrelLength() { return barrelLength; }
    
    // Методы для работы с типом боеприпаса
    public AmmoType getAmmoType() { return ammoType; }
    public void setAmmoType(AmmoType ammoType) { 
        AmmoType oldAmmoType = this.ammoType;
        
        if ((caliber == 82 && ammoType == AmmoType.PREDEFINED_AMMO_82MM[0]) ||
            (caliber == 82 && ammoType == AmmoType.PREDEFINED_AMMO_82MM[1]) ||
            (caliber == 120 && ammoType == AmmoType.PREDEFINED_AMMO_120MM[0]) ||
            (caliber == 120 && ammoType == AmmoType.PREDEFINED_AMMO_120MM[1])) {
            
            this.ammoType = ammoType;
            
            // Проверяем, действительно ли изменился тип боеприпаса
            boolean ammoTypeChanged = this.ammoType != oldAmmoType;
            
            android.util.Log.d("MortarType", String.format(
                "Ammo type updated for %s:\n" +
                "Old: %s\n" +
                "New: %s\n" +
                "Changed: %b\n" +
                "Is HE (with correction): %b",
                this.name,
                oldAmmoType != null ? oldAmmoType.getName() : "null",
                this.ammoType.getName(),
                ammoTypeChanged,
                this.ammoType.getName().contains("ОФ-")
            ));
        } else {
            android.util.Log.w("MortarType", String.format(
                "Invalid ammo type for %s (caliber %d mm): %s",
                this.name, this.caliber,
                ammoType != null ? ammoType.getName() : "null" 
            ));
        }
    }

    @Override
    public String toString() {
        return name;
    }
} 