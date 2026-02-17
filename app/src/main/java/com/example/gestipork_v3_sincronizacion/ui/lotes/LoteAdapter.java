package com.example.gestipork_v3_sincronizacion.ui.lotes;

import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.graphics.drawable.DrawableCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.gestipork_v3_sincronizacion.R;
import com.example.gestipork_v3_sincronizacion.base.ColorUtils;

import java.util.ArrayList;
import java.util.List;

public class LoteAdapter extends RecyclerView.Adapter<LoteAdapter.VH> {

    public interface OnLoteClickListener {
        void onLoteClick(LoteItem item);
    }

    private final List<LoteItem> lotes = new ArrayList<>();
    private OnLoteClickListener listener;

    public void setLotes(List<LoteItem> nuevos) {
        lotes.clear();
        if (nuevos != null) lotes.addAll(nuevos);
        notifyDataSetChanged();
    }

    public void setOnLoteClickListener(OnLoteClickListener l) {
        this.listener = l;
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_lote, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int pos) {
        h.bind(lotes.get(pos), listener);
    }

    @Override
    public int getItemCount() {
        return lotes.size();
    }

    static class VH extends RecyclerView.ViewHolder {

        View viewColor;
        TextView txtIdLote, txtCodItaca, txtRaza, txtNDisp;

        VH(View v) {
            super(v);
            viewColor = v.findViewById(R.id.view_color);
            txtIdLote = v.findViewById(R.id.txt_id_lote);
            txtCodItaca = v.findViewById(R.id.txt_cod_itaca);
            txtRaza = v.findViewById(R.id.txt_raza);
            txtNDisp = v.findViewById(R.id.txt_n_disponibles);
        }

        void bind(LoteItem item, OnLoteClickListener listener) {

            txtIdLote.setText(item.nombre == null ? "-" : item.nombre);

            txtCodItaca.setText(
                    item.dcer == null || item.dcer.trim().isEmpty()
                            ? "-"
                            : item.dcer
            );

            txtRaza.setText(item.raza == null ? "-" : item.raza);

            txtNDisp.setText(String.valueOf(item.disponibles));

            applyColor(viewColor, item.color);

            itemView.setOnClickListener(v -> {
                if (listener != null) listener.onLoteClick(item);
            });
        }

        private void applyColor(View v, String color) {
            Drawable bg = v.getBackground();
            if (bg == null) return;

            Drawable wrap = DrawableCompat.wrap(bg.mutate());
            int colorInt = ColorUtils.mapColorNameToHex(v.getContext(), color);
            DrawableCompat.setTint(wrap, colorInt);
        }
    }
}
