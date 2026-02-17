// features/lotes/LoteService.java
package com.example.gestipork_v3_sincronizacion.features.lotes;

import com.example.gestipork_v3_sincronizacion.data.models.*;

public interface LoteService {

    // ---- 1:1
    String crearLoteConHijos(String idExplotacion, String nombreLote, String raza);

    Paridera getParidera(String idLote);
    Cubricion getCubricion(String idLote);
    Itaca getItaca(String idLote);

    void updateParidera(Paridera p);
    void updateCubricion(Cubricion c);
    void updateItaca(Itaca i);

    // ---- 1:N (algunas)
    String agregarBaja(String idLote, String idExplotacion, String fechaISO,
                       int cantidad, String causa);

    String agregarSalida(String idLote, String idExplotacion, String fechaISO,
                         int cantidad, String destino, String observaciones,
                         boolean descuentaStock);

    String agregarAccion(Accion a);
    // (Puedes añadir agregarNota, agregarPeso, agregarConteo, etc.)

    // añade al final de la interfaz:
    String agregarNota(String idLote, String idExplotacion, String fechaISO, String texto);
    String agregarPeso(String idLote, String idExplotacion, String fechaISO,
                       int nAnimales, double pesoTotal);
    String agregarConteo(String idLote, String idExplotacion, String fechaISO,
                         int nAnimales, String observaciones);

}
