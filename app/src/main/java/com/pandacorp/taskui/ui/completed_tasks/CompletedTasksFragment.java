package com.pandacorp.taskui.ui.completed_tasks;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.BaseTransientBottomBar;
import com.google.android.material.snackbar.Snackbar;
import com.pandacorp.taskui.Adapter.CustomAdapter;
import com.pandacorp.taskui.Adapter.ListItem;
import com.pandacorp.taskui.Adapter.RecyclerItemTouchHelper;
import com.pandacorp.taskui.DBHelper;
import com.pandacorp.taskui.R;
import com.pandacorp.taskui.Widget.WidgetProvider;

import java.util.ArrayList;

public class CompletedTasksFragment extends Fragment implements View.OnClickListener, RecyclerItemTouchHelper.RecyclerItemTouchHelperListener {
    private final String TAG = "MyLogs";
    private final String table = DBHelper.COMPLETED_TASKS_TABLE_NAME;

    private RecyclerView recyclerView;
    public CustomAdapter adapter;
    private ArrayList<String> itemList = new ArrayList<>();
    private ArrayList<String> itemListTime = new ArrayList<>();
    private ArrayList<String> itemListPriority = new ArrayList<>();

    private ArrayList<ListItem> arrayItemList = new ArrayList<>();

    private FloatingActionButton delete_fab_completed;
    private FloatingActionButton delete_forever_fab_completed;

    private View root;

    private DBHelper dbHelper;
    private SQLiteDatabase database;
    private Cursor cursor;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        root = inflater.inflate(R.layout.fragment_completed_tasks, container, false);

        new Handler().post(this::initViews);

        return root;
    }

    private void initViews() {
        itemList = new ArrayList();

        dbHelper = new DBHelper(getContext());
        database = dbHelper.getWritableDatabase();
        cursor = database.query(DBHelper.COMPLETED_TASKS_TABLE_NAME, null, null, null, null, null, null);

        delete_fab_completed = root.findViewById(R.id.delete_fab_completed);
        delete_forever_fab_completed = root.findViewById(R.id.delete_forever_fab_completed);
        delete_fab_completed.setOnClickListener(this);
        delete_forever_fab_completed.setOnClickListener(this);

        setRecyclerView();

    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.delete_fab_completed:
                dbHelper = new DBHelper(getContext());
                database = dbHelper.getWritableDatabase();
                setDeletedTasksValues();
                database.delete(DBHelper.COMPLETED_TASKS_TABLE_NAME, null, null);
                databaseGetTasks();
                fillArrayItemList();
                adapter.notifyDataSetChanged();
                break;
            case R.id.delete_forever_fab_completed:
                dbHelper = new DBHelper(getContext());
                database = dbHelper.getWritableDatabase();
                database.delete(DBHelper.COMPLETED_TASKS_TABLE_NAME, null, null);
                databaseGetTasks();
                fillArrayItemList();
                adapter.notifyDataSetChanged();
                break;
        }
    }

    private void setDeletedTasksValues() {
        //Setting values of contentValue to set it to DELETED_TASKS_DATABASE when clicking clear_fab
        ContentValues contentValues = new ContentValues();
        for (int i = 0; i < itemList.size(); i++) {
            contentValues.put(DBHelper.KEY_TASK_TEXT, itemList.get(i));
            database.insert(DBHelper.DELETED_TASKS_TABLE_NAME, null, contentValues);
        }


    }

    private void setRecyclerView() {
        databaseGetTasks();
        fillArrayItemList();

        adapter = new CustomAdapter(arrayItemList, getActivity());
        recyclerView = root.findViewById(R.id.completed_rv);
        recyclerView.setHasFixedSize(false);
        recyclerView.setAdapter(adapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        enableSwipe();
        registerForContextMenu(recyclerView);

    }

    private void databaseGetTasks() {
        itemList.clear();
        itemListTime.clear();
        itemListPriority.clear();
        //Here is recreating DataBase objects for getting new tasks that came from SetTaskActivity
        //from user.
        dbHelper = new DBHelper(getContext());
        database = dbHelper.getWritableDatabase();
        cursor = database.query(DBHelper.COMPLETED_TASKS_TABLE_NAME, null, null, null, null, null, null);

        if (cursor.moveToFirst()) {
            int idIndex = cursor.getColumnIndex(DBHelper.KEY_ID);
            int keyTaskTextIndex = cursor.getColumnIndex(DBHelper.KEY_TASK_TEXT);
            int keyTaskTimeIndex = cursor.getColumnIndex(DBHelper.KEY_TASK_TIME);
            int keyTaskPriorityIndex = cursor.getColumnIndex(DBHelper.KEY_TASK_PRIORITY);

            do {
                Log.d("MyLogs", "ID = " + cursor.getInt(idIndex) +
                        ", name = " + cursor.getString(keyTaskTextIndex) +
                        ", time = " + cursor.getString(keyTaskTimeIndex) +
                        ", priority = " + cursor.getString(keyTaskPriorityIndex));
                itemList.add(cursor.getString(keyTaskTextIndex));
                itemListTime.add(cursor.getString(keyTaskTimeIndex));
                itemListPriority.add(cursor.getString(keyTaskPriorityIndex));

            } while (cursor.moveToNext());
        } else
            Log.d("mLog", "0 rows");


    }

    private void fillArrayItemList() {
        arrayItemList.clear();
        for (int i = 0; i < itemList.size(); i++) {
            ListItem current = new ListItem(itemList.get(i), itemListTime.get(i), itemListPriority.get(i));
            arrayItemList.add(current);

        }
    }

    private void enableSwipe() {
        //Attached the ItemTouchHelper
        recyclerView.addItemDecoration(new DividerItemDecoration(recyclerView.getContext(), DividerItemDecoration.VERTICAL));

        //Attached the ItemTouchHelper
        ItemTouchHelper.SimpleCallback itemTouchHelperCallback = new RecyclerItemTouchHelper(0, ItemTouchHelper.RIGHT, this);

        new ItemTouchHelper(itemTouchHelperCallback).attachToRecyclerView(recyclerView);
    }

    @Override
    public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction, int position) {
        Log.d(TAG, "onSwiped: onSwiped");
        if (viewHolder instanceof CustomAdapter.ViewHolder) {
            final ListItem deletedListItem = arrayItemList.get(position);
            adapter.removeItem(position);
            dbHelper.removeById(DBHelper.COMPLETED_TASKS_TABLE_NAME, position);
            dbHelper.add(DBHelper.DELETED_TASKS_TABLE_NAME, deletedListItem);

            WidgetProvider.Companion.sendRefreshBroadcast(getContext());

            // showing snack bar with Undo option
            Snackbar snackbar = Snackbar.make(getActivity().getWindow().getDecorView().getRootView(), getResources().getText(R.string.snackbar_removed), Snackbar.LENGTH_LONG);
            snackbar.setAnchorView(R.id.fabs_constraintLayout_completed);
            snackbar.setAnimationMode(BaseTransientBottomBar.ANIMATION_MODE_SLIDE);
            snackbar.setAction(getResources().getText(R.string.snackbar_undo), new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    // undo is selected, restore the deleted item
                    adapter.restoreItem(deletedListItem, DBHelper.COMPLETED_TASKS_TABLE_NAME);

                }
            });
            snackbar.show();
        }


    }
}