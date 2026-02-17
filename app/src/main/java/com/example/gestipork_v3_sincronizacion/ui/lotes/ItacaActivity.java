package com.example.gestipork_v3_sincronizacion.ui.lotes;


import android.database.Cursor;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.example.gestipork_v3_sincronizacion.R;
import com.example.gestipork_v3_sincronizacion.data.db.DBHelper;

import android.content.ContentValues;
import android.database.sqlite.SQLiteDatabase;
import android.view.Menu;
import android.view.MenuItem;
import android.view.LayoutInflater;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.ArrayAdapter;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;

import com.example.gestipork_v3_sincronizacion.base.FechaUtils;

public class ItacaActivity extends AppCompatActivity {

    private Toolbar toolbar;

    private TextView txtDcer, txtColor, txtRaza, txtAnimales, txtMadres, txtPadres,
            txtFechaPrimNac, txtFechaUltNac, txtCrotales;

    private DBHelper dbHelper;
    private String idLote;
    private String idExplotacion;

    private String dcer, color, raza, fechaPrim, fechaUlt, crotales;
    private int nAnimales, nMadres, nPadres;


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_itaca);

        dbHelper = new DBHelper(this);

        idLote = getIntent().getStringExtra("id_lote");
        idExplotacion = getIntent().getStringExtra("id_explotacion");

        android.util.Log.d("ItacaActivity", "Extras -> idLote=" + idLote + " idExplotacion=" + idExplotacion);

        if (idLote == null || idExplotacion == null) {
            Toast.makeText(this, "Error: faltan datos de lote/explotación", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        bindViews();
        setupToolbar();

        cargarItaca();
    }

    private void bindViews() {
        toolbar = findViewById(R.id.toolbar_estandar);

        txtDcer = findViewById(R.id.txtDcer);
        txtColor = findViewById(R.id.txtColor);
        txtRaza = findViewById(R.id.txtRaza);

        txtAnimales = findViewById(R.id.txtAnimales);
        txtMadres = findViewById(R.id.txtMadres);
        txtPadres = findViewById(R.id.txtPadres);

        txtFechaPrimNac = findViewById(R.id.txtFechaPrimNac);
        txtFechaUltNac = findViewById(R.id.txtFechaUltNac);

        txtCrotales = findViewById(R.id.txtCrotales);
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("ITACA");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> onBackPressed());
    }

    private void cargarItaca() {

        String sql =
                "SELECT dcer, color, raza, nAnimales, nMadres, nPadres, " +
                        "       fechaPrimerNacimiento, fechaUltimoNacimiento, crotalesSolicitados " +
                        "FROM itaca " +
                        "WHERE id_lote = ? AND id_explotacion = ? AND eliminado = 0 " +
                        "LIMIT 1";

        Cursor c = dbHelper.getReadableDatabase().rawQuery(sql, new String[]{idLote, idExplotacion});

        try {
            if (!c.moveToFirst()) {
                Toast.makeText(this, "No hay datos ITACA para este lote", Toast.LENGTH_LONG).show();
                return;
            }

            // ✅ Guardamos variables para precargar diálogo de edición
            dcer = c.getString(0);
            color = c.getString(1);
            raza = c.getString(2);

            nAnimales = c.isNull(3) ? 0 : c.getInt(3);
            nMadres   = c.isNull(4) ? 0 : c.getInt(4);
            nPadres   = c.isNull(5) ? 0 : c.getInt(5);

            fechaPrim = c.getString(6);
            fechaUlt  = c.getString(7);

            int crotalesInt = c.isNull(8) ? 0 : c.getInt(8);
            crotales = String.valueOf(crotalesInt); // si lo quieres como String para mostrar/precargar

            // ✅ Pintar en pantalla
            txtDcer.setText(nvl(dcer));
            txtColor.setText(nvl(color));
            txtRaza.setText(nvl(raza));

            txtAnimales.setText(String.valueOf(nAnimales));
            txtMadres.setText(String.valueOf(nMadres));
            txtPadres.setText(String.valueOf(nPadres));

            txtFechaPrimNac.setText(nvl(fechaPrim));
            txtFechaUltNac.setText(nvl(fechaUlt));

            txtCrotales.setText(String.valueOf(crotalesInt));

        } finally {
            c.close();
        }
    }


    private String nvl(String s) {
        return (s == null || s.trim().isEmpty()) ? "-" : s.trim();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_itaca, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();

        if (id == android.R.id.home) {
            onBackPressed();
            return true;
        }

        if (id == R.id.menu_editar_itaca) {
            mostrarDialogoEditarItaca();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void mostrarDialogoEditarItaca() {

        LayoutInflater inflater = LayoutInflater.from(this);
        View view = inflater.inflate(R.layout.dialog_editar_itaca, null);

        EditText etNAnimales = view.findViewById(R.id.etNAnimales);
        EditText etNMadres   = view.findViewById(R.id.etNMadres);
        EditText etNPadres   = view.findViewById(R.id.etNPadres);
        EditText etFechaPrim = view.findViewById(R.id.etFechaPrim);
        EditText etFechaUlt  = view.findViewById(R.id.etFechaUlt);
        Spinner spRaza       = view.findViewById(R.id.spRaza);
        Spinner spColor      = view.findViewById(R.id.spColor);
        EditText etCrotales  = view.findViewById(R.id.etCrotales);
        EditText etDcer      = view.findViewById(R.id.etDcer);

        // Opciones
        String[] razas = new String[]{"Ibérico 100%", "Cruzado 50%"};
        String[] colores = new String[]{"azul", "naranja", "rojo", "verde", "rosa"};

        ArrayAdapter<String> adapterRaza = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, razas);
        spRaza.setAdapter(adapterRaza);

        ArrayAdapter<String> adapterColor = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, colores);
        spColor.setAdapter(adapterColor);

        // Precargar valores
        etNAnimales.setText(String.valueOf(nAnimales));
        etNMadres.setText(String.valueOf(nMadres));
        etNPadres.setText(String.valueOf(nPadres));
        etFechaPrim.setText(isoToDisplay(fechaPrim));
        etFechaUlt.setText(isoToDisplay(fechaUlt));
        etCrotales.setText(crotales != null ? crotales : "");
        etDcer.setText(dcer != null ? dcer : "");

        if (raza != null) {
            for (int i = 0; i < razas.length; i++) {
                if (raza.equalsIgnoreCase(razas[i])) { spRaza.setSelection(i); break; }
            }
        }
        if (color != null) {
            for (int i = 0; i < colores.length; i++) {
                if (color.equalsIgnoreCase(colores[i])) { spColor.setSelection(i); break; }
            }
        }

        // ✅ Listeners calendario
        etFechaPrim.setOnClickListener(v -> abrirDatePicker(etFechaPrim));
        etFechaUlt.setOnClickListener(v -> abrirDatePicker(etFechaUlt));

        new AlertDialog.Builder(this)
                .setTitle("Editar ITACA")
                .setView(view)
                .setPositiveButton("Guardar", (dialog, which) -> {

                    int nuevoNAnim = parseIntSafe(etNAnimales.getText().toString());
                    int nuevoNMad  = parseIntSafe(etNMadres.getText().toString());
                    int nuevoNPad  = parseIntSafe(etNPadres.getText().toString());

                    // 👇 El usuario ve dd-MM-yyyy
                    String nuevaFechaPrimDisplay = etFechaPrim.getText().toString().trim();
                    String nuevaFechaUltDisplay  = etFechaUlt.getText().toString().trim();

                    // ✅ Convertimos a yyyy-MM-dd para BD
                    String nuevaFechaPrimIso = displayToIso(nuevaFechaPrimDisplay);
                    String nuevaFechaUltIso  = displayToIso(nuevaFechaUltDisplay);

                    String nuevaRaza = (String) spRaza.getSelectedItem();
                    String nuevoColor = (String) spColor.getSelectedItem();
                    int crotalesInt = parseIntSafe(etCrotales.getText().toString());

                    // ✅ DCER
                    String nuevoDcer = etDcer.getText().toString().trim();

                    // Validación mínima
                    if (nuevoNAnim < 0 || nuevoNMad < 0 || nuevoNPad < 0) {
                        Toast.makeText(this, "Los números no pueden ser negativos", Toast.LENGTH_LONG).show();
                        return;
                    }

                    boolean ok = actualizarItacaEnBD(
                            nuevoNAnim, nuevoNMad, nuevoNPad,
                            nuevaFechaPrimIso, nuevaFechaUltIso,   // ✅ ISO
                            nuevaRaza, nuevoColor,
                            crotalesInt,
                            nuevoDcer                              // ✅ DCER
                    );

                    if (ok) {
                        Toast.makeText(this, "ITACA actualizada", Toast.LENGTH_SHORT).show();
                        cargarItaca();
                    } else {
                        Toast.makeText(this, "Error actualizando ITACA", Toast.LENGTH_LONG).show();
                    }
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private int parseIntSafe(String s) {
        try {
            if (s == null) return 0;
            s = s.trim();
            if (s.isEmpty()) return 0;
            return Integer.parseInt(s);
        } catch (Exception e) {
            return 0;
        }
    }

    private boolean actualizarItacaEnBD(
            int nAnim, int nMad, int nPad,
            String fPrimIso, String fUltIso,
            String razaSel, String colorSel,
            int crotalesInt,
            String dcerSel
    )
    {
        SQLiteDatabase db = dbHelper.getWritableDatabase();

        ContentValues v = new ContentValues();
        v.put("nAnimales", nAnim);
        v.put("nMadres", nMad);
        v.put("nPadres", nPad);
        v.put("fechaPrimerNacimiento", fPrimIso);
        v.put("fechaUltimoNacimiento", fUltIso);
        v.put("raza", razaSel);
        v.put("color", colorSel);
        v.put("crotalesSolicitados", crotalesInt);
        v.put("dcer", dcerSel);

        v.put("fecha_actualizacion", FechaUtils.ahoraIso());
        v.put("sincronizado", 0);

        int filas = db.update(
                "itaca",
                v,
                "id_lote = ? AND id_explotacion = ? AND eliminado = 0",
                new String[]{idLote, idExplotacion}
        );

        db.close();
        return filas > 0;
    }
    private void abrirDatePicker(EditText target) {
        // Si ya hay fecha puesta en dd-MM-yyyy, la usamos como inicial
        java.util.Calendar cal = java.util.Calendar.getInstance();
        String current = target.getText() != null ? target.getText().toString().trim() : "";

        // current es dd-MM-yyyy
        try {
            if (!current.isEmpty()) {
                java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("dd-MM-yyyy", java.util.Locale.getDefault());
                java.util.Date d = sdf.parse(current);
                if (d != null) cal.setTime(d);
            }
        } catch (Exception ignore) {}

        int y = cal.get(java.util.Calendar.YEAR);
        int m = cal.get(java.util.Calendar.MONTH);
        int d = cal.get(java.util.Calendar.DAY_OF_MONTH);

        android.app.DatePickerDialog dp = new android.app.DatePickerDialog(
                this,
                (view, year, month, dayOfMonth) -> {
                    String dd = String.format(java.util.Locale.getDefault(), "%02d", dayOfMonth);
                    String mm = String.format(java.util.Locale.getDefault(), "%02d", (month + 1));
                    String txt = dd + "-" + mm + "-" + year;  // dd-MM-yyyy
                    target.setText(txt);
                },
                y, m, d
        );
        dp.show();
    }

    /** Convierte fecha almacenada (yyyy-MM-dd o ISO) a dd-MM-yyyy para mostrar */
    private String isoToDisplay(String iso) {
        if (iso == null) return "";
        iso = iso.trim();
        if (iso.isEmpty()) return "";

        // Si viene ISO completo, nos quedamos con la parte yyyy-MM-dd
        if (iso.length() >= 10) iso = iso.substring(0, 10);

        try {
            java.text.SimpleDateFormat in = new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault());
            java.text.SimpleDateFormat out = new java.text.SimpleDateFormat("dd-MM-yyyy", java.util.Locale.getDefault());
            java.util.Date d = in.parse(iso);
            return d != null ? out.format(d) : "";
        } catch (Exception e) {
            return "";
        }
    }

    /** Convierte dd-MM-yyyy (UI) a yyyy-MM-dd (BD). Si está vacío, devuelve "" */
    private String displayToIso(String display) {
        if (display == null) return "";
        display = display.trim();
        if (display.isEmpty()) return "";

        try {
            java.text.SimpleDateFormat in = new java.text.SimpleDateFormat("dd-MM-yyyy", java.util.Locale.getDefault());
            java.text.SimpleDateFormat out = new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault());
            java.util.Date d = in.parse(display);
            return d != null ? out.format(d) : "";
        } catch (Exception e) {
            return "";
        }
    }


}
