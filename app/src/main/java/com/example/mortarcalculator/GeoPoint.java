package com.example.mortarcalculator;

public class GeoPoint {
    private final double latitude;
    private final double longitude;
    private double elevation;
    private MortarType mortarType; // только для точек минометов
    private AmmoType ammoType; // тип боеприпаса

    public GeoPoint(double latitude, double longitude) {
        this(latitude, longitude, 0.0);
    }

    public GeoPoint(double latitude, double longitude, double elevation) {
        this.latitude = latitude;
        this.longitude = longitude;
        this.elevation = elevation;
        this.mortarType = MortarType.PREDEFINED_MORTARS[0]; // По умолчанию первый тип
        this.ammoType = AmmoType.PREDEFINED_AMMO_82MM[0]; // По умолчанию первый тип боеприпаса
    }

    public double getLatitude() {
        return latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public double getElevation() {
        return elevation;
    }

    public void setElevation(double elevation) {
        this.elevation = elevation;
    }

    public MortarType getMortarType() {
        return mortarType;
    }

    public void setMortarType(MortarType mortarType) {
        // Запоминаем был ли раньше установлен осколочно-фугасный снаряд (ОФ)
        boolean wasHE = this.ammoType != null && this.ammoType.getName().contains("ОФ-");
        
        // Текущий калибр
        int oldCaliber = this.mortarType != null ? this.mortarType.getCaliber() : -1;
        int newCaliber = mortarType.getCaliber();
        
        this.mortarType = mortarType;
        
        // Если калибр изменился, нужно подобрать подходящий тип боеприпаса с соответствующим индексом
        if (oldCaliber != newCaliber) {
            // Проверяем, нужно ли установить осколочно-фугасный (ОФ) или обычный снаряд
            if (newCaliber == 82) {
                // Для калибра 82мм выбираем соответствующий тип снаряда
                this.ammoType = wasHE ? 
                    AmmoType.PREDEFINED_AMMO_82MM[1] :  // ОФ-снаряд (индекс 1)
                    AmmoType.PREDEFINED_AMMO_82MM[0];   // Обычный снаряд (индекс 0)
            } else {
                // Для калибра 120мм выбираем соответствующий тип снаряда
                this.ammoType = wasHE ? 
                    AmmoType.PREDEFINED_AMMO_120MM[1] : // ОФ-снаряд (индекс 1)
                    AmmoType.PREDEFINED_AMMO_120MM[0];  // Обычный снаряд (индекс 0)
            }
            
            android.util.Log.d("GeoPoint", String.format(
                "Automatically updating ammo type after mortar type change:\n" +
                "Old caliber: %d, New caliber: %d\n" +
                "Was HE: %b\n" +
                "New ammo: %s", 
                oldCaliber, newCaliber, wasHE, this.ammoType.getName()
            ));
        }
    }

    public AmmoType getAmmoType() {
        return ammoType;
    }

    public void setAmmoType(AmmoType ammoType) {
        this.ammoType = ammoType;
    }
} 