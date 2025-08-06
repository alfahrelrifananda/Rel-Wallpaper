package com.example.relwallpaper;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.net.NetworkRequest;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.relwallpaper.databinding.ActivityMainBinding;
import com.example.relwallpaper.ui.auth.LoginActivity;
import com.example.relwallpaper.ui.pages.HelpAndSupportActivity;
import com.example.relwallpaper.ui.pages.SearchResultsActivity;
import com.example.relwallpaper.ui.pages.SettingsActivity;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.search.SearchBar;
import com.google.android.material.search.SearchView;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;
    private SearchBar searchBar;
    private SearchView searchView;
    private OnBackPressedCallback onBackPressedCallback;

    private DrawerLayout drawerLayout;
    private ImageButton hamburgerMenu;
    private ImageButton menuButton;

    private CountDownTimer jwtCountdownTimer;
    private AlertDialog sessionExpiredDialog;
    private AlertDialog noInternetDialog;
    private static final String TAG = "MainActivity";

    private ConnectivityManager connectivityManager;
    private ConnectivityManager.NetworkCallback networkCallback;
    private BroadcastReceiver networkReceiver;

    private static final String PREFS_NAME = "search_history_prefs";
    private static final String SEARCH_HISTORY_KEY = "search_history";
    private static final int MAX_SEARCH_HISTORY = 10;

    private SharedPreferences searchHistoryPrefs;
    private List<String> searchHistory;
    private SearchHistoryAdapter searchHistoryAdapter;
    private RecyclerView searchHistoryRecyclerView;
    private TextView searchHistoryHeader;
    private MaterialButton clearHistoryButton;
    private View emptySearchState;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        initializeSearchHistory();

        drawerLayout = findViewById(R.id.drawerLayout);

        ViewCompat.setOnApplyWindowInsetsListener(binding.getRoot(), (v, windowInsets) -> {
            WindowInsetsCompat insets = windowInsets;
            androidx.core.graphics.Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());

            binding.container.setPadding(0, systemBars.top, 0, 0);

            View bottomNav = findViewById(R.id.nav_view);
            if (bottomNav != null) {
                bottomNav.setPadding(0, 0, 0, systemBars.bottom);
            }

            return WindowInsetsCompat.CONSUMED;
        });

        searchBar = findViewById(R.id.searchBar);
        searchView = findViewById(R.id.searchView);
        searchHistoryHeader = findViewById(R.id.searchHistoryHeader);
        clearHistoryButton = findViewById(R.id.clearHistoryButton);
        emptySearchState = findViewById(R.id.emptySearchState);

        setupSearch();
        setupBackPressHandling();

        BottomNavigationView navView = findViewById(R.id.nav_view);

        AppBarConfiguration appBarConfiguration = new AppBarConfiguration.Builder(
                R.id.navigation_home,
                R.id.navigation_search,
                R.id.navigation_collection,
                R.id.navigation_profile
        ).build();

        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_activity_main);
        NavigationUI.setupWithNavController(binding.navView, navController);

        setupBottomNavigationDoubleTap();
        setupDrawerNavigation();
        setupClearHistoryButton();

        initializeNetworkMonitoring();

        if (!isInternetAvailable()) {
            showNoInternetDialog();
        } else {
            startJwtSessionMonitoring();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!isInternetAvailable()) {
            showNoInternetDialog();
        } else {
            if (noInternetDialog != null && noInternetDialog.isShowing()) {
                noInternetDialog.dismiss();
            }
            checkJwtExpiration();
        }
    }

    private class NetworkCallback extends ConnectivityManager.NetworkCallback {
        @Override
        public void onAvailable(@NonNull Network network) {
            super.onAvailable(network);
            runOnUiThread(() -> {
                if (noInternetDialog != null && noInternetDialog.isShowing()) {
                    noInternetDialog.dismiss();
                    startJwtSessionMonitoring();
                    Toast.makeText(MainActivity.this, "Connection restored", Toast.LENGTH_SHORT).show();
                }
            });
        }

        @Override
        public void onLost(@NonNull Network network) {
            super.onLost(network);
            runOnUiThread(() -> {
                if (!isInternetAvailable()) {
                    showNoInternetDialog();
                }
            });
        }
    }

    private class NetworkReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (ConnectivityManager.CONNECTIVITY_ACTION.equals(intent.getAction())) {
                if (isInternetAvailable()) {
                    if (noInternetDialog != null && noInternetDialog.isShowing()) {
                        noInternetDialog.dismiss();
                        startJwtSessionMonitoring();
                        Toast.makeText(MainActivity.this, "Connection restored", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    showNoInternetDialog();
                }
            }
        }
    }

    private void initializeNetworkMonitoring() {
        connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            networkCallback = new NetworkCallback();
            NetworkRequest.Builder builder = new NetworkRequest.Builder();
            connectivityManager.registerNetworkCallback(builder.build(), networkCallback);
        } else {
            networkReceiver = new NetworkReceiver();
            IntentFilter filter = new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION);
            registerReceiver(networkReceiver, filter);
        }
    }

    private boolean isInternetAvailable() {
        if (connectivityManager == null) {
            return false;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Network network = connectivityManager.getActiveNetwork();
            if (network == null) return false;

            NetworkCapabilities capabilities = connectivityManager.getNetworkCapabilities(network);
            return capabilities != null && (
                    capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
                            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)
            );
        } else {
            NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
            return networkInfo != null && networkInfo.isConnected();
        }
    }

    private void showNoInternetDialog() {
        if (noInternetDialog != null && noInternetDialog.isShowing()) {
            return;
        }

        if (jwtCountdownTimer != null) {
            jwtCountdownTimer.cancel();
        }

        noInternetDialog = new MaterialAlertDialogBuilder(this)
                .setTitle("No Internet Connection")
                .setMessage("This app requires an internet connection to function properly. Please check your network connection and try again.")
                .setIcon(R.drawable.ic_baseline_signal_wifi_off_24)
                .setPositiveButton("Retry", (dialog, which) -> {
                    if (isInternetAvailable()) {
                        dialog.dismiss();
                        startJwtSessionMonitoring();
                        Toast.makeText(MainActivity.this, "Connection restored", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(MainActivity.this, "Still no internet connection", Toast.LENGTH_SHORT).show();
                        showNoInternetDialog();
                    }
                })
                .setNegativeButton("Exit App", (dialog, which) -> {
                    finish();
                })
                .setCancelable(false)
                .create();

        noInternetDialog.show();
    }

    private void startJwtSessionMonitoring() {
        SharedPreferences prefs = getSharedPreferences("app_prefs", MODE_PRIVATE);
        long loginTime = prefs.getLong("login_time", System.currentTimeMillis());

        long oneWeekInMillis = 7 * 24 * 60 * 60 * 1000L;
        long expirationTime = loginTime + oneWeekInMillis;
        long remainingTime = expirationTime - System.currentTimeMillis();

        if (remainingTime <= 0) {
            showSessionExpiredDialog();
            return;
        }

        jwtCountdownTimer = new CountDownTimer(remainingTime, 60000) {
            @Override
            public void onTick(long millisUntilFinished) {
                if (millisUntilFinished <= TimeUnit.MINUTES.toMillis(5)) {
                    if (millisUntilFinished <= TimeUnit.MINUTES.toMillis(1)) {
                        Toast.makeText(MainActivity.this,
                                "Session expires in less than 1 minute",
                                Toast.LENGTH_SHORT).show();
                    }
                }
            }

            @Override
            public void onFinish() {
                showSessionExpiredDialog();
            }
        }.start();
    }

    private void checkJwtExpiration() {
        if (!isInternetAvailable()) {
            showNoInternetDialog();
            return;
        }

        SharedPreferences prefs = getSharedPreferences("app_prefs", MODE_PRIVATE);
        long loginTime = prefs.getLong("login_time", System.currentTimeMillis());

        long oneWeekInMillis = 7 * 24 * 60 * 60 * 1000L;
        long expirationTime = loginTime + oneWeekInMillis;
        long remainingTime = expirationTime - System.currentTimeMillis();

        if (remainingTime <= 0) {
            showSessionExpiredDialog();
        }
    }

    private void showSessionExpiredDialog() {
        if (sessionExpiredDialog != null && sessionExpiredDialog.isShowing()) {
            return;
        }

        if (jwtCountdownTimer != null) {
            jwtCountdownTimer.cancel();
        }

        sessionExpiredDialog = new MaterialAlertDialogBuilder(this)
                .setTitle("Session Expired")
                .setMessage("Your session has expired for security reasons. Please log in again to continue using the app.")
                .setIcon(R.drawable.ic_baseline_warning_24)
                .setPositiveButton("Login Again", (dialog, which) -> {
                    redirectToLogin();
                })
                .setCancelable(false)
                .create();

        sessionExpiredDialog.show();
    }

    private void redirectToLogin() {
        new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
            LoginActivity.logout(this);
        }, 0);
    }

    private void initializeSearchHistory() {
        searchHistoryPrefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        loadSearchHistory();
        setupSearchHistoryRecyclerView();
    }

    private void loadSearchHistory() {
        String historyJson = searchHistoryPrefs.getString(SEARCH_HISTORY_KEY, "[]");
        Gson gson = new Gson();
        Type type = new TypeToken<List<String>>(){}.getType();
        searchHistory = gson.fromJson(historyJson, type);

        if (searchHistory == null) {
            searchHistory = new ArrayList<>();
        }
    }

    private void saveSearchHistory() {
        Gson gson = new Gson();
        String historyJson = gson.toJson(searchHistory);
        searchHistoryPrefs.edit()
                .putString(SEARCH_HISTORY_KEY, historyJson)
                .apply();
    }

    private void addToSearchHistory(String query) {
        if (query == null || query.trim().isEmpty()) {
            return;
        }

        query = query.trim();

        searchHistory.remove(query);

        searchHistory.add(0, query);

        if (searchHistory.size() > MAX_SEARCH_HISTORY) {
            searchHistory = searchHistory.subList(0, MAX_SEARCH_HISTORY);
        }

        saveSearchHistory();

        if (searchHistoryAdapter != null) {
            searchHistoryAdapter.notifyDataSetChanged();
        }

        updateSearchHistoryVisibility();
    }

    private void setupSearchHistoryRecyclerView() {
        searchHistoryRecyclerView = findViewById(R.id.searchHistoryRecyclerView);

        if (searchHistoryRecyclerView != null) {
            searchHistoryRecyclerView.setLayoutManager(new LinearLayoutManager(this));

            searchHistoryAdapter = new SearchHistoryAdapter(searchHistory, new SearchHistoryAdapter.OnHistoryItemClickListener() {
                @Override
                public void onHistoryItemClick(String query) {
                    searchView.getEditText().setText(query);
                    performSearch(query);
                    searchView.hide();
                }

                @Override
                public void onDeleteHistoryItem(String query) {
                    showDeleteHistoryItemDialog(query);
                }
            });

            searchHistoryRecyclerView.setAdapter(searchHistoryAdapter);
        }
    }

    private void setupClearHistoryButton() {
        if (clearHistoryButton != null) {
            clearHistoryButton.setOnClickListener(v -> showClearHistoryDialog());
        }
    }

    private void showClearHistoryDialog() {
        new com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
                .setTitle("Clear search history")
                .setMessage("Are you sure you want to clear all search history? This action cannot be undone.")
                .setPositiveButton("Clear", (dialog, which) -> {
                    clearAllSearchHistory();
                    Toast.makeText(this, "Search history cleared", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showDeleteHistoryItemDialog(String query) {
        new AlertDialog.Builder(this)
                .setTitle("Remove from history")
                .setMessage("Remove \"" + query + "\" from search history?")
                .setPositiveButton("Remove", (dialog, which) -> {
                    searchHistory.remove(query);
                    saveSearchHistory();
                    searchHistoryAdapter.notifyDataSetChanged();
                    updateSearchHistoryVisibility();
                    Toast.makeText(this, "Removed from history", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void updateSearchHistoryVisibility() {
        boolean hasHistory = !searchHistory.isEmpty();

        if (searchHistoryRecyclerView != null) {
            searchHistoryRecyclerView.setVisibility(hasHistory ? View.VISIBLE : View.GONE);
        }

        if (searchHistoryHeader != null) {
            searchHistoryHeader.setVisibility(hasHistory ? View.VISIBLE : View.GONE);
        }

        if (clearHistoryButton != null) {
            clearHistoryButton.setVisibility(hasHistory ? View.VISIBLE : View.GONE);
        }

        if (emptySearchState != null) {
            emptySearchState.setVisibility(hasHistory ? View.GONE : View.VISIBLE);
        }
    }

    private void showPopupMenu(View anchor) {
        PopupMenu popup = new PopupMenu(this, anchor);
        MenuInflater inflater = popup.getMenuInflater();
        inflater.inflate(R.menu.top_menu, popup.getMenu());

        popup.setOnMenuItemClickListener(item -> {
            int id = item.getItemId();
            if (id == R.id.action_settings) {
                Intent intent = new Intent(this, SettingsActivity.class);
                startActivity(intent);
                return true;
            } else if (id == R.id.action_help) {
                Intent intent = new Intent(this, HelpAndSupportActivity.class);
                startActivity(intent);
                return true;
            }
            return false;
        });

        popup.show();
    }

    private void setupSearch() {
        searchBar.setOnClickListener(v -> {
            searchView.show();
            updateSearchSuggestions("");
        });

        searchView.getEditText().setOnEditorActionListener((v, actionId, event) -> {
            String query = searchView.getText().toString().trim();
            if (!query.isEmpty()) {
                addToSearchHistory(query);
                performSearch(query);
                searchView.hide();
            } else {
                Toast.makeText(this, "Please enter a search term", Toast.LENGTH_SHORT).show();
            }
            return true;
        });

        searchView.getEditText().addTextChangedListener(new android.text.TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String query = s.toString().trim();
                updateSearchSuggestions(query);
            }

            @Override
            public void afterTextChanged(android.text.Editable s) {}
        });

        searchView.addTransitionListener((searchView, previousState, newState) -> {
            if (newState == SearchView.TransitionState.HIDING) {
                onBackPressedCallback.setEnabled(false);
            } else if (newState == SearchView.TransitionState.SHOWING) {
                onBackPressedCallback.setEnabled(true);
                updateSearchSuggestions("");
            }
        });
    }

    private void setupBackPressHandling() {
        onBackPressedCallback = new OnBackPressedCallback(false) {
            @Override
            public void handleOnBackPressed() {
                if (sessionExpiredDialog != null && sessionExpiredDialog.isShowing()) {
                    return;
                }

                if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
                    drawerLayout.closeDrawer(GravityCompat.START);
                } else if (searchView != null && searchView.isShowing()) {
                    searchView.hide();
                } else {
                    setEnabled(false);
                    onBackPressed();
                    setEnabled(true);
                }
            }
        };

        getOnBackPressedDispatcher().addCallback(this, onBackPressedCallback);

        onBackPressedCallback.setEnabled(true);
    }

    private void setupBottomNavigationDoubleTap() {
        BottomNavigationView navView = binding.navView;

        final long[] lastTapTime = {0};
        final int[] lastTappedItemId = {-1};
        final long DOUBLE_TAP_TIMEOUT = 300;

        navView.setOnItemSelectedListener(item -> {
            long currentTime = System.currentTimeMillis();
            int itemId = item.getItemId();

            if (itemId == R.id.navigation_collection &&
                    lastTappedItemId[0] == itemId &&
                    (currentTime - lastTapTime[0]) < DOUBLE_TAP_TIMEOUT) {

                if (searchView != null) {
                    searchView.show();
                }
                return true;
            }

            lastTapTime[0] = currentTime;
            lastTappedItemId[0] = itemId;

            NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_activity_main);
            return NavigationUI.onNavDestinationSelected(item, navController);
        });
    }

    private void setupDrawerNavigation() {
        binding.navigationView.setNavigationItemSelectedListener(menuItem -> {
            int itemId = menuItem.getItemId();
            NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_activity_main);

            boolean handled = NavigationUI.onNavDestinationSelected(menuItem, navController);

            if (handled) {
                drawerLayout.closeDrawer(GravityCompat.START);
            }

            return handled;
        });

        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_activity_main);
        NavigationUI.setupWithNavController(binding.navigationView, navController);
    }

    private void updateSearchSuggestions(String query) {
        List<String> filteredHistory = new ArrayList<>();

        if (query.isEmpty()) {
            filteredHistory.addAll(searchHistory);
        } else {
            for (String historyItem : searchHistory) {
                if (historyItem.toLowerCase().contains(query.toLowerCase())) {
                    filteredHistory.add(historyItem);
                }
            }
        }

        if (searchHistoryAdapter != null) {
            searchHistoryAdapter.updateHistory(filteredHistory);
        }

        boolean hasFilteredHistory = !filteredHistory.isEmpty();
        boolean isSearching = !query.isEmpty();

        if (searchHistoryRecyclerView != null) {
            searchHistoryRecyclerView.setVisibility(hasFilteredHistory ? View.VISIBLE : View.GONE);
        }

        if (searchHistoryHeader != null) {
            searchHistoryHeader.setVisibility(!isSearching && hasFilteredHistory ? View.VISIBLE : View.GONE);
        }

        if (clearHistoryButton != null) {
            clearHistoryButton.setVisibility(!isSearching && !searchHistory.isEmpty() ? View.VISIBLE : View.GONE);
        }

        if (emptySearchState != null) {
            emptySearchState.setVisibility(!isSearching && searchHistory.isEmpty() ? View.VISIBLE : View.GONE);
        }
    }

    private void performSearch(String query) {
        if (query == null || query.trim().isEmpty()) {
            Toast.makeText(this, "Please enter a search term", Toast.LENGTH_SHORT).show();
            return;
        }

        Intent intent = new Intent(this, SearchResultsActivity.class);
        intent.putExtra(SearchResultsActivity.EXTRA_SEARCH_QUERY, query.trim());
        startActivity(intent);
    }

    public void navigateToSearch(String query) {
        addToSearchHistory(query);
        performSearch(query);
    }

    public void showSearchView() {
        if (searchView != null) {
            searchView.show();
        }
    }

    public void clearAllSearchHistory() {
        searchHistory.clear();
        saveSearchHistory();
        if (searchHistoryAdapter != null) {
            searchHistoryAdapter.notifyDataSetChanged();
        }
        updateSearchHistoryVisibility();
    }

    @Override
    public boolean onSupportNavigateUp() {
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_activity_main);
        return NavigationUI.navigateUp(navController,
                new AppBarConfiguration.Builder(
                        R.id.navigation_home,
                        R.id.navigation_search,
                        R.id.navigation_collection,
                        R.id.navigation_profile
                ).build()) || super.onSupportNavigateUp();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (jwtCountdownTimer != null) {
            jwtCountdownTimer.cancel();
        }

        if (sessionExpiredDialog != null && sessionExpiredDialog.isShowing()) {
            sessionExpiredDialog.dismiss();
        }

        if (noInternetDialog != null && noInternetDialog.isShowing()) {
            noInternetDialog.dismiss();
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && networkCallback != null) {
            try {
                connectivityManager.unregisterNetworkCallback(networkCallback);
            } catch (Exception e) {
            }
        } else if (networkReceiver != null) {
            try {
                unregisterReceiver(networkReceiver);
            } catch (Exception e) {
            }
        }
    }

    public static class SearchHistoryAdapter extends RecyclerView.Adapter<SearchHistoryAdapter.ViewHolder> {
        private List<String> searchHistory;
        private OnHistoryItemClickListener listener;

        public interface OnHistoryItemClickListener {
            void onHistoryItemClick(String query);
            void onDeleteHistoryItem(String query);
        }

        public SearchHistoryAdapter(List<String> searchHistory, OnHistoryItemClickListener listener) {
            this.searchHistory = new ArrayList<>(searchHistory);
            this.listener = listener;
        }

        public void updateHistory(List<String> newHistory) {
            this.searchHistory.clear();
            this.searchHistory.addAll(newHistory);
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.search_history_item, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            String query = searchHistory.get(position);
            holder.queryText.setText(query);

            holder.itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onHistoryItemClick(query);
                }
            });

            holder.deleteButton.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onDeleteHistoryItem(query);
                }
            });
        }

        @Override
        public int getItemCount() {
            return searchHistory.size();
        }

        public static class ViewHolder extends RecyclerView.ViewHolder {
            TextView queryText;
            ImageButton deleteButton;
            ImageView historyIcon;

            public ViewHolder(@NonNull View itemView) {
                super(itemView);
                queryText = itemView.findViewById(R.id.searchQueryText);
                deleteButton = itemView.findViewById(R.id.deleteButton);
                historyIcon = itemView.findViewById(R.id.historyIcon);
            }
        }
    }
}