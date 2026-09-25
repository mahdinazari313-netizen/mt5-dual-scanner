package com.mt5dual.ui;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
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
import com.mt5dual.core.Signal;
import com.mt5dual.core.SignalStateManager;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class DualActiveSignalsFragment extends Fragment {
    private static final long REFRESH_INTERVAL_MILLIS = 30_000L;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable refreshRunnable = new Runnable() {
        @Override
        public void run() {
            refresh();
            handler.postDelayed(this, REFRESH_INTERVAL_MILLIS);
        }
    };

    private RecyclerView recycler;
    private TextView empty;
    private SymbolCardAdapter adapter;
    private SignalStateManager.Listener listener;

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_active_signals, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        recycler = view.findViewById(R.id.activeSignalsRecyclerView);
        empty = view.findViewById(R.id.activeSignalsEmptyText);
        recycler.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new SymbolCardAdapter();
        recycler.setAdapter(adapter);
    }

    @Override
    public void onResume() {
        super.onResume();
        listener = signal -> {
            if (isAdded()) {
                requireActivity().runOnUiThread(this::refresh);
            }
        };
        DualApplication.getInstance().getSignalStateManager().addListener(listener);
        refresh();
        handler.removeCallbacks(refreshRunnable);
        handler.postDelayed(refreshRunnable, REFRESH_INTERVAL_MILLIS);
    }

    @Override
    public void onPause() {
        super.onPause();
        if (listener != null) {
            DualApplication.getInstance().getSignalStateManager().removeListener(listener);
        }
        handler.removeCallbacks(refreshRunnable);
    }

    public void refresh() {
        if (recycler == null || !isAdded()) return;
        List<Signal> all = DualApplication.getInstance()
                .getSignalStateManager().getAllSignals();
        long now = System.currentTimeMillis();
        long windowCandles = DualApplication.getInstance()
                .getSettingsProvider().getDualWindowCandles();
        Map<String, List<Signal>> bySymbol = new LinkedHashMap<>();
        for (Signal signal : all) {
            long expiry = signal.getReceivedAt()
                    + signal.getTimeframe().getBaseMinutes()
                    * windowCandles * 60_000L;
            if (now >= expiry) continue;
            bySymbol.computeIfAbsent(signal.getSymbol(), k -> new ArrayList<>()).add(signal);
        }
        List<SymbolCardAdapter.SymbolGroup> groups = new ArrayList<>();
        for (Map.Entry<String, List<Signal>> entry : bySymbol.entrySet()) {
            List<Signal> signals = entry.getValue();
            signals.sort(Comparator
                    .comparingLong((Signal s) -> s.getTimeframe().getBaseMinutes()).reversed()
                    .thenComparingInt(s -> s.getDirection().name().equals("BUY") ? 0 : 1));
            groups.add(new SymbolCardAdapter.SymbolGroup(entry.getKey(), signals));
        }
        groups.sort(Comparator
                .comparingLong((SymbolCardAdapter.SymbolGroup g) -> latest(g.signals))
                .reversed());
        adapter.submitGroups(groups);
        empty.setVisibility(groups.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private long latest(List<Signal> signals) {
        long result = Long.MIN_VALUE;
        for (Signal signal : signals) {
            result = Math.max(result, signal.getReceivedAt());
        }
        return result;
    }
}
