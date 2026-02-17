package com.example.gestipork_v3_sincronizacion.ui.lotes;

import android.content.Intent;
import android.graphics.drawable.GradientDrawable;
import android.view.Menu;
import android.view.MenuItem;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.view.LayoutInflater;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.ArrayAdapter;


import androidx.appcompat.app.AppCompatActivity;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;

import com.example.gestipork_v3_sincronizacion.R;
import com.example.gestipork_v3_sincronizacion.base.ColorUtils;
import com.example.gestipork_v3_sincronizacion.data.db.DBHelper;
import com.example.gestipork_v3_sincronizacion.data.models.Itaca;
import com.example.gestipork_v3_sincronizacion.data.models.Lote;
import com.example.gestipork_v3_sincronizacion.data.repo.ItacaRepository;
import com.example.gestipork_v3_sincronizacion.data.repo.LoteRepository;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.appbar.MaterialToolbar;

public class DetalleLoteActivity extends AppCompatActivity {

    private String idLote;
    private String idExplotacion;
    private Lote loteActual;

    // UI
    private MaterialToolbar toolbar;
    private TextView txtNombreLote, txtNAnimales, txtRazaEdad;
    private TextView txtBellota, txtCeboCampo, txtCebo;
    private MaterialButton btnCambiarAlimentacion;
    private MaterialCardView cardAcciones, cardSalidas;
    private View viewColorLote;

    // DB
    private DBHelper dbh;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detalle_lote);

        dbh = new DBHelper(this);

        recibirIntent();
        bindViews();
        setupToolbar();
        cargarDatos();

        //setupListeners();
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            // El título definitivo lo ponemos cuando cargamos el lote
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> onBackPressed());
    }

    private void recibirIntent() {
        idLote = getIntent().getStringExtra("id_lote");
        idExplotacion = getIntent().getStringExtra("id_explotacion");

        if (idLote == null) {
            Toast.makeText(this, "Error: id_lote no recibido", Toast.LENGTH_LONG).show();
            finish();
        }
    }

    private void bindViews() {

        // Toolbar
        toolbar = findViewById(R.id.toolbar_estandar);

        // Header
        txtNombreLote = findViewById(R.id.text_nombre_lote);
        txtNAnimales  = findViewById(R.id.text_n_animales);
        txtRazaEdad   = findViewById(R.id.text_raza_edad);
        viewColorLote = findViewById(R.id.view_color_lote);

        // Alimentación
        txtBellota   = findViewById(R.id.text_bellota);
        txtCeboCampo = findViewById(R.id.text_cebo_campo);
        txtCebo      = findViewById(R.id.text_cebo);
        btnCambiarAlimentacion = findViewById(R.id.btn_cambiar_alimentacion);

        // Cards de gestión
        cardAcciones = findViewById(R.id.card_acciones);
        cardSalidas  = findViewById(R.id.card_salidas);
    }

    /** Carga lote, itaca y alimentación */
    private void cargarDatos() {

        // Repositorios
        LoteRepository loteRepo = new LoteRepository(dbh);
        ItacaRepository itacaRepo = new ItacaRepository(dbh);

        // Lote base
        Lote lote = loteRepo.findById(idLote);
        if (lote == null) {
            Toast.makeText(this, "Error cargando el lote", Toast.LENGTH_LONG).show();
            return;
        }
        loteActual = lote;
        // Itaca (datos adicionales) – de momento no lo usamos, pero ya lo tienes
        Itaca it = itacaRepo.findByLote(idLote);

        // HEADER
        txtNombreLote.setText(lote.getNombre_lote());

        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(lote.getNombre_lote());
        }

        txtNAnimales.setText(lote.getnDisponibles() + " animales");

        String raza = lote.getRaza() != null ? lote.getRaza() : "Sin raza";
        txtRazaEdad.setText(raza);  // luego añadimos edad si quieres

        // Color del lote
        if (lote.getColor() != null) {
            applyColorToCircle(lote.getColor());
        }

        // ALIMENTACIÓN
        cargarAlimentacion();
    }

    /** Asigna el color configurado en el lote al círculo */
    private void applyColorToCircle(String colorName) {
        int colorInt = ColorUtils.mapColorNameToHex(this, colorName);

        GradientDrawable bg = (GradientDrawable) getDrawable(R.drawable.circle_background);
        if (bg != null) {
            bg.setColor(colorInt);
            viewColorLote.setBackground(bg);
        }
    }

    /** Carga las cifras de alimentación */
    private void cargarAlimentacion() {

        // Placeholder: más adelante lo sustituiremos por consulta real a la tabla "alimentacion"
        txtBellota.setText("0");
        txtCeboCampo.setText("0");
        txtCebo.setText("0");

        // TODO: reemplazar con consulta real:
        // SELECT tipo, cantidad FROM alimentacion WHERE id_lote=? AND id_explotacion=?
    }
/*
    private void setupListeners() {

        btnCambiarAlimentacion.setOnClickListener(v -> {
            Intent i = new Intent(this, CambiarAlimentacionActivity.class);
            i.putExtra("id_lote", idLote);
            startActivity(i);
        });

        cardAcciones.setOnClickListener(v -> {
            Intent i = new Intent(this, AccionesActivity.class);
            i.putExtra("id_lote", idLote);
            i.putExtra("id_explotacion", idExplotacion);
            startActivity(i);
        });

        cardSalidas.setOnClickListener(v -> {
            Intent i = new Intent(this, SalidasActivity.class);
            i.putExtra("id_lote", idLote);
            i.putExtra("id_explotacion", idExplotacion);
            startActivity(i);
        });
    }
*/
    // =========================
    //  MENÚ SUPERIOR TOOLBAR
    // =========================

    private void editarLote() {
        if (loteActual == null) {
            Toast.makeText(this, "No se ha podido cargar el lote", Toast.LENGTH_SHORT).show();
            return;
        }

        LayoutInflater inflater = LayoutInflater.from(this);
        View view = inflater.inflate(R.layout.dialog_nuevo_lote, null);

        EditText edtNombreLote = view.findViewById(R.id.edtNombreLote);
        Spinner spinnerRaza = view.findViewById(R.id.spinnerRaza);

        // Misma lista de razas que al crear
        String[] razas = new String[] { "Selecciona raza" ,"Ibérico 100%", "Cruzado 50%", "Cruzado 75%" };
        ArrayAdapter<String> razaAdapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_dropdown_item,
                razas
        );
        spinnerRaza.setAdapter(razaAdapter);

        // Rellenar con los datos actuales
        edtNombreLote.setText(loteActual.getNombre_lote());

        String razaActual = loteActual.getRaza();
        if (razaActual != null) {
            for (int i = 0; i < razas.length; i++) {
                if (razaActual.equalsIgnoreCase(razas[i])) {
                    spinnerRaza.setSelection(i);
                    break;
                }
            }
        }

        new AlertDialog.Builder(this)
                .setTitle("Editar lote")
                .setView(view)
                .setPositiveButton("Guardar", (dialog, which) -> {
                    String nombreLote = edtNombreLote.getText() != null
                            ? edtNombreLote.getText().toString().trim()
                            : "";
                    int posRaza = spinnerRaza.getSelectedItemPosition();

                    if (nombreLote.isEmpty()) {
                        Toast.makeText(this, "El nombre no puede estar vacío", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if (posRaza == 0) {
                        Toast.makeText(this, "Selecciona una raza", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    String razaSel = (String) spinnerRaza.getItemAtPosition(posRaza);

                    // Actualizamos el modelo en memoria
                    loteActual.setNombre_lote(nombreLote);
                    loteActual.setRaza(razaSel);

                    // Guardar en BD
                    LoteRepository repo = new LoteRepository(dbh);
                    boolean ok = repo.actualizarNombreYRaza(loteActual);

                    if (ok) {
                        Toast.makeText(this, "Lote actualizado", Toast.LENGTH_SHORT).show();
                        cargarDatos(); // recarga los datos en pantalla
                    } else {
                        Toast.makeText(this, "Error al actualizar el lote", Toast.LENGTH_LONG).show();
                    }
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_detalle_lote, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();

        if (id == android.R.id.home) {
            onBackPressed();
            return true;
        }

        if (id == R.id.menu_editar_lote) {
            Toast.makeText(this, "PULSADO EDITAR LOTE", Toast.LENGTH_SHORT).show(); // 👈 DEBUG
            editarLote();
            return true;
        } else if (id == R.id.menu_eliminar_lote) {
            eliminarLote();
            return true;
        } else if (id == R.id.menu_ver_cubricion) {
            verCubricion();
            return true;
        } else if (id == R.id.menu_ver_paridera) {
            verParidera();
            return true;
        } else if (id == R.id.menu_ver_itaca) {
            verItaca();
            return true;
        } else if (id == R.id.menu_ver_historial_pesajes) {
            verHistorialPesajes();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }


    private void eliminarLote() {

        if (loteActual == null) {
            Toast.makeText(this, "No se ha cargado el lote correctamente", Toast.LENGTH_LONG).show();
            return;
        }

        new AlertDialog.Builder(this)
                .setTitle("Eliminar lote")
                .setMessage("¿Seguro que quieres eliminar este lote y todos sus datos asociados?\n" +
                        "La eliminación es definitiva a nivel funcional.")
                .setPositiveButton("Eliminar", (dialog, which) -> {

                    String idLoteReal = loteActual.getId();   // 👈 usamos el id del objeto
                    String idExplotacionReal = idExplotacion; // viene del Intent

                    LoteRepository repo = new LoteRepository(dbh);
                    boolean ok = repo.eliminarLoteYHijosLogicamente(idLoteReal, idExplotacionReal);

                    if (ok) {
                        Toast.makeText(this, "Lote eliminado correctamente", Toast.LENGTH_SHORT).show();
                        finish(); // volvemos a la lista
                    } else {
                        Toast.makeText(this, "Error al eliminar el lote", Toast.LENGTH_LONG).show();
                    }

                })
                .setNegativeButton("Cancelar", null)
                .show();
    }



    private void verCubricion() {
        // Próximo paso: abrir pantalla / diálogo con la cubrición asociada a este lote
        Toast.makeText(this, "Ver Cubrición (pendiente de implementar)", Toast.LENGTH_SHORT).show();
    }

    private void verParidera() {
        // Próximo paso: abrir pantalla / diálogo con la paridera asociada a este lote
        Toast.makeText(this, "Ver Paridera (pendiente de implementar)", Toast.LENGTH_SHORT).show();
    }

    private void verItaca() {
        if (loteActual == null) {
            Toast.makeText(this, "Lote no cargado", Toast.LENGTH_SHORT).show();
            return;
        }

        Intent i = new Intent(this, ItacaActivity.class);
        i.putExtra("id_lote", loteActual.getId());      // ✅ id real (UUID)
        i.putExtra("id_explotacion", idExplotacion);    // ✅ explotación
        startActivity(i);
    }



    private void verHistorialPesajes() {
        // Próximo paso: abrir pantalla con el historial de pesajes de este lote
        Toast.makeText(this, "Historial de pesajes (pendiente de implementar)", Toast.LENGTH_SHORT).show();
    }

}
