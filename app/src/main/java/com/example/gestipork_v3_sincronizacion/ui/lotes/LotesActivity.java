package com.example.gestipork_v3_sincronizacion.ui.lotes;

import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.gestipork_v3_sincronizacion.R;
import com.example.gestipork_v3_sincronizacion.data.db.DBHelper;
import com.example.gestipork_v3_sincronizacion.data.models.Lote;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.List;

public class LotesActivity extends AppCompatActivity {

    private Toolbar toolbar;
    private RecyclerView recyclerLotes;
    private TextView txtLotesVacio;
    private FloatingActionButton fabAddLote;

    private DBHelper dbHelper;
    private String idExplotacion;   // viene del Intent
    private LoteAdapter adapter;
    private String nombreExplotacion;


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_lotes);

        dbHelper = new DBHelper(this);

        // Recuperar id_explotacion del Intent
        Intent intent = getIntent();
        idExplotacion = intent.getStringExtra("id_explotacion");
        nombreExplotacion = cargarNombreExplotacion(idExplotacion);

        if (idExplotacion == null) {
            Toast.makeText(this, "Error: explotación no recibida", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        bindViews();
        setupToolbar();
        setupRecycler();

        loadLotes(idExplotacion);

        fabAddLote.setOnClickListener(v -> showCrearLoteDialog());


        adapter.setOnLoteClickListener(item -> {


            Intent detalleIntent = new Intent(
                    LotesActivity.this,
                    com.example.gestipork_v3_sincronizacion.ui.lotes.DetalleLoteActivity.class
            );
            detalleIntent.putExtra("id_lote", item.id);
            detalleIntent.putExtra("id_explotacion", idExplotacion);
            startActivity(detalleIntent);
        });


    }

    private void bindViews() {
        toolbar = findViewById(R.id.toolbar_estandar);
        recyclerLotes = findViewById(R.id.recyclerLotes);
        txtLotesVacio = findViewById(R.id.txtLotesVacio);
        fabAddLote = findViewById(R.id.fabAddLote);

    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            String titulo = "Lotes";
            if (nombreExplotacion != null && !nombreExplotacion.isEmpty()) {
                titulo += " - " + nombreExplotacion;
            }
            getSupportActionBar().setTitle(titulo);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> onBackPressed());
    }


    private void setupRecycler() {
        adapter = new LoteAdapter();
        recyclerLotes.setLayoutManager(new LinearLayoutManager(this));
        recyclerLotes.setAdapter(adapter);
    }

    private void loadLotes(String idExplotacion) {

        List<LoteItem> items = new ArrayList<>();

        String sql =
                "SELECT l.id, l.nombre_lote, l.raza, l.nDisponibles, " +
                        "       i.dcer, i.color " +
                        "FROM lotes l " +
                        "LEFT JOIN itaca i ON i.id_lote = l.id " +
                        "                  AND i.id_explotacion = l.id_explotacion " +
                        "                  AND i.eliminado = 0 " +
                        "WHERE l.id_explotacion = ? AND l.eliminado = 0 " +
                        "ORDER BY l.nombre_lote ASC";

        Cursor c = dbHelper.getReadableDatabase().rawQuery(sql, new String[]{idExplotacion});

        try {
            while (c.moveToNext()) {
                LoteItem item = new LoteItem();
                item.id          = c.getString(0);
                item.nombre      = c.getString(1);
                item.raza        = c.getString(2);
                item.disponibles = c.isNull(3) ? 0 : c.getInt(3);
                item.dcer        = c.getString(4);
                item.color       = c.getString(5);   // COLOR REAL DE ITACA

                items.add(item);
            }
        } finally {
            c.close();
        }

        adapter.setLotes(items);
    }

    private void showCrearLoteDialog() {
        if (idExplotacion == null || idExplotacion.isEmpty()) {
            Toast.makeText(this, "No se ha recibido la explotación", Toast.LENGTH_SHORT).show();
            return;
        }

        LayoutInflater inflater = LayoutInflater.from(this);
        View view = inflater.inflate(R.layout.dialog_nuevo_lote, null);

        EditText edtNombreLote = view.findViewById(R.id.edtNombreLote);
        Spinner spinnerRaza = view.findViewById(R.id.spinnerRaza);

        // Opciones de raza (ajusta si necesitas más)
        String[] razas = new String[] { "Selecciona raza" ,"Ibérico 100%", "Cruzado 50%", "Cruzado 75%" };
        ArrayAdapter<String> razaAdapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_dropdown_item,
                razas
        );
        spinnerRaza.setAdapter(razaAdapter);

        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Nuevo lote")
                .setView(view)
                .setPositiveButton("Crear", (dialog, which) -> {
                    String nombreLote = edtNombreLote.getText() != null
                            ? edtNombreLote.getText().toString().trim()
                            : "";
                    int posRaza = spinnerRaza.getSelectedItemPosition();
                    if (posRaza == 0) {
                        Toast.makeText(this, "Selecciona una raza", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    String razaSel = (String) spinnerRaza.getItemAtPosition(posRaza);

                    try {
                        dbHelper.crearLoteConHijos(
                                idExplotacion,
                                nombreLote,
                                razaSel
                        );

                        loadLotes(idExplotacion);
                        Toast.makeText(this, "Lote creado correctamente", Toast.LENGTH_SHORT).show();

                    } catch (Exception e) {
                        Toast.makeText(this, "Error al crear el lote: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    }

                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private String cargarNombreExplotacion(String idExplot) {
        if (idExplot == null || idExplot.isEmpty()) return "";

        String nombre = "";
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor c = db.rawQuery(
                "SELECT nombre FROM explotaciones WHERE id = ? LIMIT 1",
                new String[]{idExplot}
        );
        try {
            if (c.moveToFirst()) {
                nombre = c.getString(0);
            }
        } finally {
            c.close();
        }
        return nombre != null ? nombre : "";
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (idExplotacion != null) {
            loadLotes(idExplotacion);
        }
    }



}
