package com.example.mortarcalculator;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

public class MortarSettingsDialog extends DialogFragment {
    private OnMortarSettingsListener listener;
    private int mortarIndex;
    private double elevation;

    public interface OnMortarSettingsListener {
        void onMortarSettingsChanged(int mortarIndex, double elevation);
        void onMortarDeleted();
    }

    public static MortarSettingsDialog newInstance(int mortarIndex, double elevation) {
        MortarSettingsDialog dialog = new MortarSettingsDialog();
        Bundle args = new Bundle();
        args.putInt("mortarIndex", mortarIndex);
        args.putDouble("elevation", elevation);
        dialog.setArguments(args);
        return dialog;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        try {
            listener = (OnMortarSettingsListener) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(context.toString() + " must implement OnMortarSettingsListener");
        }
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireActivity());
        LayoutInflater inflater = requireActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.dialog_mortar_settings, null);

        mortarIndex = getArguments().getInt("mortarIndex", 0);
        elevation = getArguments().getDouble("elevation", 0.0);

        Spinner mortarSpinner = view.findViewById(R.id.mortar_type_spinner);
        EditText elevationEdit = view.findViewById(R.id.mortar_elevation);

        // Настраиваем спиннер с типами минометов
        ArrayAdapter<MortarType> adapter = new ArrayAdapter<>(
            requireContext(),
            android.R.layout.simple_spinner_item,
            MortarType.PREDEFINED_MORTARS
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mortarSpinner.setAdapter(adapter);
        mortarSpinner.setSelection(mortarIndex);

        // Устанавливаем текущую высоту
        elevationEdit.setText(String.format("%.1f", elevation));

        builder.setView(view)
               .setTitle("Настройки миномета")
               .setPositiveButton("OK", (dialog, id) -> {
                   int newMortarIndex = mortarSpinner.getSelectedItemPosition();
                   double newElevation;
                   try {
                       newElevation = Double.parseDouble(elevationEdit.getText().toString());
                   } catch (NumberFormatException e) {
                       newElevation = 0.0;
                   }
                   listener.onMortarSettingsChanged(newMortarIndex, newElevation);
               })
               .setNegativeButton("Отмена", (dialog, id) -> {
                   // Ничего не делаем при отмене
               })
               .setNeutralButton("Удалить", (dialog, id) -> {
                   listener.onMortarDeleted();
               });

        return builder.create();
    }
} 