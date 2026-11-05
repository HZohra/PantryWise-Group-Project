package com.cp470.groupproject.ui.shoppinglist;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationManagerCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.cp470.groupproject.R;
import com.cp470.groupproject.data.entity.ShoppingItem;
import com.cp470.groupproject.notification.NotificationPermissionHelper;
import com.cp470.groupproject.ui.mealplanner.MealPlannerActivity;
import com.cp470.groupproject.util.Event;
import com.cp470.groupproject.util.ShoppingInputValidator;
import com.cp470.groupproject.util.ShoppingListFormatter;
import com.cp470.groupproject.util.WindowInsetsHelper;
import com.google.android.material.snackbar.Snackbar;

import java.util.List;

/**
 * Shopping list screen and destination of the expiration notification. it only renders state and
 * forwards clicks to {@link ShoppingListViewModel}.
 */
public class ShoppingListActivity extends AppCompatActivity {

    private static final String STATE_NAME = "state_name";
    private static final String STATE_QTY = "state_qty";
    private static final String STATE_UNIT = "state_unit";
    private static final String STATE_STORE = "state_store";

    private ShoppingListViewModel viewModel;
    private ShoppingListAdapter adapter;

    private EditText edtName;
    private EditText edtQty;
    private EditText edtUnit;
    private EditText edtStore;
    private TextView txtSummary;
    private TextView txtEmpty;
    private RecyclerView recycler;
    private View bannerNotifications;
    private Button btnGenerate;

    /** true while a generation runs, so the generate actions are disabled */
    private boolean generating;

    /** @return an explicit intent that opens this screen */
    public static Intent newIntent(Context context) {
        return new Intent(context, ShoppingListActivity.class);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_shopping_list);
        WindowInsetsHelper.applySystemBarPadding(findViewById(R.id.root));

        // the app theme has no action bar, so the layout brings its own toolbar
        setSupportActionBar(findViewById(R.id.toolbar));
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        viewModel = new ViewModelProvider(this).get(ShoppingListViewModel.class);

        bindViews();
        restoreForm(savedInstanceState);
        setupList();
        setupForm();
        observeViewModel();
    }

    private void bindViews() {
        edtName = findViewById(R.id.edtName);
        edtQty = findViewById(R.id.edtQty);
        edtUnit = findViewById(R.id.edtUnit);
        edtStore = findViewById(R.id.edtStore);
        txtSummary = findViewById(R.id.txtSummary);
        txtEmpty = findViewById(R.id.txtEmpty);
        recycler = findViewById(R.id.recycler);
        bannerNotifications = findViewById(R.id.bannerNotifications);
        btnGenerate = findViewById(R.id.btnGenerate);
        findViewById(R.id.btnBannerSettings).setOnClickListener(
                v -> NotificationPermissionHelper.openNotificationSettings(this));
    }

    /** checked on every return to the screen, so the banner goes away after fixing it in settings */
    @Override
    protected void onResume() {
        super.onResume();
        boolean enabled = NotificationManagerCompat.from(this).areNotificationsEnabled();
        bannerNotifications.setVisibility(enabled ? View.GONE : View.VISIBLE);
    }

    /**
     * the view model survives a rotation, but not a process kill, so the half-typed form is also
     * kept in the bundle
     */
    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(STATE_NAME, edtName.getText().toString());
        outState.putString(STATE_QTY, edtQty.getText().toString());
        outState.putString(STATE_UNIT, edtUnit.getText().toString());
        outState.putString(STATE_STORE, edtStore.getText().toString());
    }

    private void restoreForm(Bundle state) {
        if (state == null) return;
        edtName.setText(state.getString(STATE_NAME, ""));
        edtQty.setText(state.getString(STATE_QTY, ""));
        edtUnit.setText(state.getString(STATE_UNIT, ""));
        edtStore.setText(state.getString(STATE_STORE, ""));
    }

    private void setupList() {
        adapter = new ShoppingListAdapter(new ShoppingListAdapter.Listener() {
            @Override
            public void onCheckedChanged(ShoppingItem item, boolean checked) {
                viewModel.setChecked(item, checked);
            }

            @Override
            public void onItemClicked(ShoppingItem item) {
                showEditDialog(item);
            }
        });
        recycler.setLayoutManager(new LinearLayoutManager(this));
        recycler.setAdapter(adapter);

        // swipe a row to delete it (the snackbar offers undo)
        new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(0,
                ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {
            @Override
            public boolean onMove(@NonNull RecyclerView list,
                                  @NonNull RecyclerView.ViewHolder holder,
                                  @NonNull RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder holder, int direction) {
                int position = holder.getBindingAdapterPosition();
                if (position != RecyclerView.NO_POSITION) {
                    viewModel.deleteItem(adapter.getCurrentList().get(position));
                }
            }
        }).attachToRecyclerView(recycler);
    }

    private void setupForm() {
        Button add = findViewById(R.id.btnAdd);
        add.setOnClickListener(v -> addFromForm());
        btnGenerate.setOnClickListener(v -> viewModel.generateForNextDays());
        // the "done" key of the keyboard also adds the item
        edtName.setOnEditorActionListener((view, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                addFromForm();
                return true;
            }
            return false;
        });
    }

    private void addFromForm() {
        boolean added = viewModel.addItem(edtName.getText().toString(),
                edtQty.getText().toString(), edtUnit.getText().toString(),
                edtStore.getText().toString());
        if (added) {
            edtName.setText("");
            edtQty.setText("");
            edtUnit.setText("");
            edtStore.setText("");
            edtName.requestFocus();
        }
    }

    private void observeViewModel() {
        viewModel.getItems().observe(this, list -> {
            adapter.submitList(list);
            boolean empty = list == null || list.isEmpty();
            txtEmpty.setVisibility(empty ? View.VISIBLE : View.GONE);
            recycler.setVisibility(empty ? View.GONE : View.VISIBLE);
        });
        viewModel.getSummary().observe(this, counts ->
                txtSummary.setText(getString(R.string.shopping_summary, counts[0], counts[1])));
        viewModel.getMessages().observe(this, this::showMessage);
        viewModel.getGenerating().observe(this, busy -> {
            generating = Boolean.TRUE.equals(busy);
            btnGenerate.setEnabled(!generating);
            invalidateOptionsMenu();
        });
    }

    private void showMessage(Event<UiMessage> event) {
        UiMessage message = event == null ? null : event.getContentIfNotHandled();
        if (message == null) return;
        String text = message.isPlural
                ? getResources().getQuantityString(message.resId, message.quantity, message.args)
                : getString(message.resId, message.args);
        Snackbar snackbar = Snackbar.make(findViewById(R.id.root), text, Snackbar.LENGTH_LONG);
        if (message.offerUndo) {
            snackbar.setAction(R.string.shopping_action_undo, v -> viewModel.undoDelete());
        }
        snackbar.show();
    }

    /** custom dialog: own layout inflated and passed to setView */
    private void showEditDialog(ShoppingItem item) {
        View content = LayoutInflater.from(this)
                .inflate(R.layout.dialog_edit_shopping_item, null);
        EditText name = content.findViewById(R.id.edtEditName);
        EditText qty = content.findViewById(R.id.edtEditQty);
        EditText unit = content.findViewById(R.id.edtEditUnit);
        EditText store = content.findViewById(R.id.edtEditStore);
        name.setText(item.name);
        qty.setText(ShoppingListFormatter.formatQuantity(item.quantity));
        unit.setText(item.unit);
        store.setText(item.store == null ? "" : item.store);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(R.string.shopping_edit_title)
                .setView(content)
                .setPositiveButton(R.string.shopping_action_save, null)
                .setNegativeButton(R.string.shopping_action_cancel, null)
                .create();
        // the save button is wired after show() so an invalid input does not close the dialog
        dialog.setOnShowListener(d -> dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                .setOnClickListener(v -> {
                    ShoppingInputValidator.Error error = viewModel.validate(
                            name.getText().toString(), qty.getText().toString());
                    if (error == ShoppingInputValidator.Error.NAME_REQUIRED) {
                        name.setError(getString(R.string.shopping_error_name));
                    } else if (error == ShoppingInputValidator.Error.QUANTITY_INVALID) {
                        qty.setError(getString(R.string.shopping_error_qty));
                    } else {
                        viewModel.editItem(item, name.getText().toString(),
                                qty.getText().toString(), unit.getText().toString(),
                                store.getText().toString());
                        dialog.dismiss();
                    }
                }));
        dialog.show();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_shopping_list, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        MenuItem generate = menu.findItem(R.id.action_generate);
        if (generate != null) {
            generate.setEnabled(!generating);
        }
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem menuItem) {
        int id = menuItem.getItemId();
        if (id == R.id.action_share) {
            shareList();
            return true;
        }
        if (id == R.id.action_generate) {
            viewModel.generateForNextDays();
            return true;
        }
        if (id == R.id.action_planner) {
            startActivity(MealPlannerActivity.newIntent(this));
            return true;
        }
        if (id == R.id.action_clear) {
            confirmClearCompleted();
            return true;
        }
        return super.onOptionsItemSelected(menuItem);
    }

    /** implicit intent: any app that accepts plain text (messages, mail...) can receive the list */
    private void shareList() {
        List<ShoppingItem> list = viewModel.getItems().getValue();
        if (list == null || list.isEmpty()) {
            Toast.makeText(this, R.string.shopping_share_empty, Toast.LENGTH_SHORT).show();
            return;
        }
        Intent send = new Intent(Intent.ACTION_SEND);
        send.setType("text/plain");
        send.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.shopping_share_subject));
        send.putExtra(Intent.EXTRA_TEXT,
                viewModel.buildShareText(getString(R.string.shopping_share_header)));
        // needs the <queries> entry of the manifest to see other apps on android 11+
        if (send.resolveActivity(getPackageManager()) != null) {
            startActivity(Intent.createChooser(send, getString(R.string.shopping_share_chooser)));
        } else {
            Toast.makeText(this, R.string.shopping_share_no_app, Toast.LENGTH_SHORT).show();
        }
    }

    private void confirmClearCompleted() {
        int completed = viewModel.completedCount();
        if (completed == 0) {
            Toast.makeText(this, R.string.shopping_nothing_completed, Toast.LENGTH_SHORT).show();
            return;
        }
        new AlertDialog.Builder(this)
                .setTitle(R.string.shopping_clear_title)
                .setMessage(getResources().getQuantityString(
                        R.plurals.shopping_clear_message, completed, completed))
                .setPositiveButton(R.string.shopping_menu_clear,
                        (dialog, which) -> viewModel.clearCompleted())
                .setNegativeButton(R.string.shopping_action_cancel, null)
                .show();
    }

    /** the parent declared in the manifest (InventoryActivity) may not exist yet, so just close */
    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
