package com.example.mortarcalculator;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

public class TargetSettingsDialog extends DialogFragment {
    private OnTargetSettingsListener listener;
    private double elevation;

    public interface OnTargetSettingsListener {
        void onTargetSettingsChanged(double elevation);
    }

    public static TargetSettingsDialog newInstance(double elevation) {
        TargetSettingsDialog dialog = new TargetSettingsDialog();
        Bundle args = new Bundle();
        args.putDouble("elevation", elevation);
        dialog.setArguments(args);
        return dialog;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        try {
            listener = (OnTargetSettingsListener) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(context.toString() + " must implement OnTargetSettingsListener");
        }
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireActivity());
        LayoutInflater inflater = requireActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.dialog_target_settings, null);

        elevation = getArguments().getDouble("elevation", 0.0);

        EditText elevationEdit = view.findViewById(R.id.target_elevation);
        elevationEdit.setText(String.format("%.1f", elevation));

        builder.setView(view)
               .setTitle("Настройки цели")
               .setPositiveButton("OK", (dialog, id) -> {
                   double newElevation;
                   try {
                       newElevation = Double.parseDouble(elevationEdit.getText().toString());
                   } catch (NumberFormatException e) {
                       newElevation = 0.0;
                   }
                   listener.onTargetSettingsChanged(newElevation);
               })
               .setNegativeButton("Отмена", (dialog, id) -> {
                   // Ничего не делаем при отмене
               });

        return builder.create();
    }
} 