package com.example.mortarcalculator;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

public class MortarSettingsDialog extends DialogFragment {
    private int mortarIndex;
    private double elevation;
    private int ammoIndex;
    private OnSettingsChangedListener listener;

    public interface OnSettingsChangedListener {
        void onSettingsChanged(int mortarIndex, double elevation, int ammoIndex);
    }

    public void setOnSettingsChangedListener(OnSettingsChangedListener listener) {
        this.listener = listener;
    }

    public static MortarSettingsDialog newInstance(int mortarIndex, double elevation, int ammoIndex) {
        MortarSettingsDialog dialog = new MortarSettingsDialog();
        Bundle args = new Bundle();
        args.putInt("mortarIndex", mortarIndex);
        args.putDouble("elevation", elevation);
        args.putInt("ammoIndex", ammoIndex);
        dialog.setArguments(args);
        return dialog;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireActivity());
        LayoutInflater inflater = requireActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.dialog_mortar_settings, null);

        mortarIndex = getArguments().getInt("mortarIndex", 0);
        elevation = getArguments().getDouble("elevation", 0.0);
        ammoIndex = getArguments().getInt("ammoIndex", 0);

        Spinner mortarSpinner = view.findViewById(R.id.mortar_type_spinner);
        Spinner ammoSpinner = view.findViewById(R.id.ammo_type_spinner);
        EditText elevationEdit = view.findViewById(R.id.mortar_elevation);

        // Настраиваем спиннер с типами минометов
        ArrayAdapter<MortarType> mortarAdapter = new ArrayAdapter<>(
            requireContext(),
            android.R.layout.simple_spinner_item,
            MortarType.PREDEFINED_MORTARS
        );
        mortarAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mortarSpinner.setAdapter(mortarAdapter);
        mortarSpinner.setSelection(mortarIndex);

        // Настраиваем спиннер с типами боеприпасов
        updateAmmoSpinner(ammoSpinner, mortarIndex);
        ammoSpinner.setSelection(ammoIndex);

        // Обработчик изменения типа миномета
        mortarSpinner.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                mortarIndex = position;
                updateAmmoSpinner(ammoSpinner, position);
                ammoIndex = 0; // Сбрасываем индекс боеприпаса при смене миномета
            }

            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {}
        });

        // Обработчик изменения типа боеприпаса
        ammoSpinner.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                ammoIndex = position;
            }

            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {}
        });

        // Устанавливаем текущую высоту
        elevationEdit.setText(String.format("%.1f", elevation));

        builder.setView(view)
            .setTitle("Настройки миномета")
            .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int id) {
                    try {
                        elevation = Double.parseDouble(elevationEdit.getText().toString());
                    } catch (NumberFormatException e) {
                        elevation = 0.0;
                    }
                    if (listener != null) {
                        listener.onSettingsChanged(mortarIndex, elevation, ammoIndex);
                    }
                }
            })
            .setNegativeButton("Отмена", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int id) {
                    MortarSettingsDialog.this.getDialog().cancel();
                }
            });

        return builder.create();
    }

    private void updateAmmoSpinner(Spinner ammoSpinner, int mortarIndex) {
        AmmoType[] ammoTypes;
        if (MortarType.PREDEFINED_MORTARS[mortarIndex].getCaliber() == 82) {
            ammoTypes = AmmoType.PREDEFINED_AMMO_82MM;
        } else {
            ammoTypes = AmmoType.PREDEFINED_AMMO_120MM;
        }

        ArrayAdapter<AmmoType> ammoAdapter = new ArrayAdapter<>(
            requireContext(),
            android.R.layout.simple_spinner_item,
            ammoTypes
        );
        ammoAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        ammoSpinner.setAdapter(ammoAdapter);
    }
} 