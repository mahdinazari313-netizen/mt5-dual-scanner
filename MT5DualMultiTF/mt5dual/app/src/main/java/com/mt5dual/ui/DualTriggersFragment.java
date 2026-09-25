package com.mt5dual.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.mt5dual.DualApplication;
import com.mt5dual.R;
import com.mt5dual.dual.DualAlarmListener;
import com.mt5dual.dual.DualTrigger;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class DualTriggersFragment extends Fragment {
    private RecyclerView recycler;
    private TextView empty;
    private DualTriggerAdapter adapter;
    private DualAlarmListener listener;

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_dual_triggers, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        recycler = view.findViewById(R.id.dualTriggersRecyclerView);
        empty = view.findViewById(R.id.dualTriggersEmptyText);
        recycler.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new DualTriggerAdapter(trigger -> {
            DualApplication.getInstance().getDualEngineManager().silence(
                    trigger.getSymbol(), trigger.getTimeframe(), trigger.getDirection());
            refresh();
        });
        recycler.setAdapter(adapter);
    }

    @Override
    public void onResume() {
        super.onResume();
        listener = new DualAlarmListener() {
            @Override public void onNewTrigger(DualTrigger trigger) { postRefresh(); }
            @Override public void onRepeatAlarm(DualTrigger trigger) { postRefresh(); }
            @Override public void onTriggerExpired(DualTrigger trigger) { postRefresh(); }
        };
        DualApplication.getInstance().getDualEngineManager().addGlobalAlarmListener(listener);
        refresh();
    }

    @Override
    public void onPause() {
        super.onPause();
        if (listener != null) {
            DualApplication.getInstance().getDualEngineManager().removeGlobalAlarmListener(listener);
        }
    }

    public void refresh() {
        if (recycler == null || !isAdded()) return;
        List<DualTrigger> all = DualApplication.getInstance().getDualEngineManager().getAllActiveTriggers();
        long now = System.currentTimeMillis();
        int candles = DualApplication.getInstance().getSettingsProvider().getDualWindowCandles();

        Map<String, List<DualTrigger>> grouped = new LinkedHashMap<>();
        for (DualTrigger trigger : all) {
            long elapsed = com.mt5dual.core.WeekdayTimeUtil.weekdayMillisBetween(
                    trigger.getCombination().get(0).getReceivedAt(), now);
            long window = trigger.getTimeframe().getBaseMinutes() * candles * 60_000L;
            if (elapsed >= window) continue;
            grouped.computeIfAbsent(trigger.getSymbol(), k -> new ArrayList<>()).add(trigger);
        }

        List<DualTriggerAdapter.SymbolTriggerGroup> groups = new ArrayList<>();
        for (Map.Entry<String, List<DualTrigger>> entry : grouped.entrySet()) {
            // Sort فقط بر اساس createdAt نزولی (جدیدترین Trigger بالاتر)
            // بدون Sort TF، بدون جداسازی BUY/SELL
            entry.getValue().sort(Comparator
                    .comparingLong(DualTrigger::getCreatedAt).reversed());
            groups.add(new DualTriggerAdapter.SymbolTriggerGroup(entry.getKey(), entry.getValue()));
        }

        groups.sort(Comparator
                .comparingLong((DualTriggerAdapter.SymbolTriggerGroup g) -> latest(g.triggers))
                .reversed());
        adapter.submitGroups(groups);
        empty.setVisibility(groups.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private long latest(List<DualTrigger> triggers) {
        long result = Long.MIN_VALUE;
        for (DualTrigger trigger : triggers) result = Math.max(result, trigger.getCreatedAt());
        return result;
    }

    private void postRefresh() {
        if (!isAdded()) return;
        requireActivity().runOnUiThread(this::refresh);
    }
}
