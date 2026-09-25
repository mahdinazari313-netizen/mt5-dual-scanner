package com.mt5dual.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.mt5dual.R;
import com.mt5dual.core.Direction;
import com.mt5dual.core.Signal;
import com.mt5dual.core.Timeframe;

import java.util.ArrayList;
import java.util.List;

public class SymbolCardAdapter extends RecyclerView.Adapter<SymbolCardAdapter.ViewHolder> {
    public static final class SymbolGroup {
        public final String symbol;
        public final List<Signal> signals;

        public SymbolGroup(String symbol, List<Signal> signals) {
            this.symbol = symbol;
            this.signals = signals;
        }
    }

    private final List<SymbolGroup> groups = new ArrayList<>();

    public void submitGroups(List<SymbolGroup> newGroups) {
        groups.clear();
        if (newGroups != null) groups.addAll(newGroups);
        notifyDataSetChanged();
    }

    @NonNull @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_symbol_card, parent, false);
        return new ViewHolder(view);
    }

    @Override public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        SymbolGroup group = groups.get(position);
        holder.symbol.setText(group.symbol);
        holder.rows.removeAllViews();

        LayoutInflater inflater = LayoutInflater.from(holder.itemView.getContext());
        for (Signal signal : group.signals) {
            View row = inflater.inflate(R.layout.item_dual_trigger, holder.rows, false);
            TextView first = row.findViewById(R.id.triggerMainText);
            TextView second = row.findViewById(R.id.triggerPriceText);
            android.widget.Button button = row.findViewById(R.id.triggerSilentButton);
            button.setVisibility(View.GONE);

            first.setText(holder.itemView.getContext().getString(
                    R.string.active_signal_row, signal.getTimeframe().name(), signal.getDirection().name()));
            second.setText(holder.itemView.getContext().getString(
                    R.string.active_signal_price, String.valueOf(signal.getPrice())));
            holder.rows.addView(row);
        }
    }

    @Override public int getItemCount() { return groups.size(); }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final TextView symbol;
        final LinearLayout rows;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            symbol = itemView.findViewById(R.id.symbolCardTitle);
            rows = itemView.findViewById(R.id.symbolRowsContainer);
        }
    }
}
