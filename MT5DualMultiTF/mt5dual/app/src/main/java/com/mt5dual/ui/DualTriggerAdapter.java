package com.mt5dual.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.mt5dual.R;
import com.mt5dual.core.Signal;
import com.mt5dual.dual.DualTrigger;

import java.util.ArrayList;
import java.util.List;

public class DualTriggerAdapter extends RecyclerView.Adapter<DualTriggerAdapter.ViewHolder> {
    public static final class SymbolTriggerGroup {
        public final String symbol;
        public final List<DualTrigger> triggers;

        public SymbolTriggerGroup(String symbol, List<DualTrigger> triggers) {
            this.symbol = symbol;
            this.triggers = triggers;
        }
    }

    public interface SilentListener { void onSilent(DualTrigger trigger); }

    private final List<SymbolTriggerGroup> groups = new ArrayList<>();
    private final SilentListener silentListener;

    public DualTriggerAdapter(SilentListener silentListener) {
        this.silentListener = silentListener;
    }

    public void submitGroups(List<SymbolTriggerGroup> newGroups) {
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
        SymbolTriggerGroup group = groups.get(position);
        holder.symbol.setText(group.symbol);
        holder.rows.removeAllViews();

        LayoutInflater inflater = LayoutInflater.from(holder.itemView.getContext());
        for (DualTrigger trigger : group.triggers) {
            View row = inflater.inflate(R.layout.item_dual_trigger, holder.rows, false);
            TextView main = row.findViewById(R.id.triggerMainText);
            TextView prices = row.findViewById(R.id.triggerPriceText);
            Button silent = row.findViewById(R.id.triggerSilentButton);

            Signal ref = trigger.getCombination().get(0);
            Signal incoming = trigger.getCombination().get(1);

            main.setText(holder.itemView.getContext().getString(
                    R.string.trigger_row_main,
                    trigger.getTimeframe().name(), trigger.getDirection().name()));
            prices.setText(holder.itemView.getContext().getString(
                    R.string.trigger_row_prices,
                    String.valueOf(ref.getPrice()), String.valueOf(incoming.getPrice())));

            silent.setVisibility(trigger.isSilenced() ? View.GONE : View.VISIBLE);
            silent.setOnClickListener(v -> {
                if (silentListener != null) silentListener.onSilent(trigger);
                notifyDataSetChanged();
            });

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
