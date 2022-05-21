package com.pandacorp.taskui.ui.deleted_tasks;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Canvas;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.BaseTransientBottomBar;
import com.google.android.material.snackbar.Snackbar;
import com.pandacorp.taskui.DBHelper;
import com.pandacorp.taskui.R;
import com.pandacorp.taskui.ui.Adapter.CustomAdapter;
import com.pandacorp.taskui.ui.Adapter.ListItem;
import com.pandacorp.taskui.ui.Adapter.RecyclerItemTouchHelper;

import java.util.ArrayList;

import it.xabaras.android.recyclerview.swipedecorator.RecyclerViewSwipeDecorator;

public class DeletedTasksFragment extends Fragment implements RecyclerItemTouchHelper.RecyclerItemTouchHelperListener {
    private final String TAG = "MyLogs";

    private RecyclerView recyclerView;
    public CustomAdapter adapter;
    private ArrayList<String> itemList= new ArrayList<>();
    private ArrayList<ListItem> arrayItemList = new ArrayList<>();

    private FloatingActionButton clear_fab_deleted;

    private View root;

    private DBHelper dbHelper;
    private SQLiteDatabase database;
    private Cursor cursor;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        root = inflater.inflate(R.layout.fragment_deleted_tasks, container, false);

        initViews();

        return root;
    }

    private void initViews() {
        itemList = new ArrayList();

        dbHelper = new DBHelper(getContext());
        database = dbHelper.getWritableDatabase();
        cursor = database.query(DBHelper.DELETED_TASKS_TABLE_NAME, null, null, null, null, null, null);

        clear_fab_deleted = root.findViewById(R.id.clear_fab_deleted);
        clear_fab_deleted.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dbHelper = new DBHelper(getContext());
                database = dbHelper.getWritableDatabase();
                setDeletedTasksValues();
                database.delete(DBHelper.DELETED_TASKS_TABLE_NAME, null, null);
                databaseGetTasks();
                fillArrayItemList();
                adapter.notifyDataSetChanged();
                //Очистка текста.
            }
        });

        setRecyclerView();

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
        recyclerView = root.findViewById(R.id.deleted_rv);
        recyclerView.setHasFixedSize(false);
        recyclerView.setAdapter(adapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        enableSwipe();
        registerForContextMenu(recyclerView);

    }

    private void databaseGetTasks() {
        itemList.clear();
        //Here is recreating DataBase objects for getting new tasks that came from SetTaskActivity
        //from user.
        dbHelper = new DBHelper(getContext());
        database = dbHelper.getWritableDatabase();
        cursor = database.query(DBHelper.DELETED_TASKS_TABLE_NAME, null, null, null, null, null, null);

        if (cursor.moveToFirst()) {
            int idIndex = cursor.getColumnIndex(DBHelper.KEY_ID);
            int keyTaskIndex = cursor.getColumnIndex(DBHelper.KEY_TASK_TEXT);
            do {
                Log.d("MyLogs", "ID = " + cursor.getInt(idIndex) +
                        ", name = " + cursor.getString(keyTaskIndex));
                itemList.add(cursor.getString(keyTaskIndex));
            } while (cursor.moveToNext());
        } else
            Log.d("mLog", "0 rows");


    }

    private void fillArrayItemList() {
        arrayItemList.clear();
        for (int i = 0; i < itemList.size(); i++) {
            if (cursor.moveToFirst()) {
                int idIndex = cursor.getColumnIndex(DBHelper.KEY_ID);
                int keyTaskIndex = cursor.getColumnIndex(DBHelper.KEY_TASK_TEXT);
                do {
                    Log.d("MyLogs", "ID = " + cursor.getInt(idIndex) +
                            ", name = " + cursor.getString(keyTaskIndex));
                    itemList.add(cursor.getString(keyTaskIndex));
                    ListItem current = new ListItem(itemList.get(i));
                    arrayItemList.add(current);

                } while (cursor.moveToNext());
            } else
                Log.d("mLog", "0 rows");

        }
    }
    private void enableSwipe() {
        //Attached the ItemTouchHelper
        recyclerView.addItemDecoration(new DividerItemDecoration(recyclerView.getContext(), DividerItemDecoration.VERTICAL));

        //Attached the ItemTouchHelper
        ItemTouchHelper.SimpleCallback itemTouchHelperCallback = new RecyclerItemTouchHelper(0, ItemTouchHelper.LEFT, this);
        new ItemTouchHelper(itemTouchHelperCallback).attachToRecyclerView(recyclerView);
    }



    @Override
    public void onDestroy() {
        super.onDestroy();
        cursor.close();
        database.close();
    }

    @Override
    public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction, int position) {
        Log.d(TAG, "onSwiped: onSwiped");
        if (viewHolder instanceof CustomAdapter.ViewHolder) {
            final ListItem deletedModel = arrayItemList.get(position);
            final int deletedPosition = position;
            adapter.removeItem(position, deletedModel, DBHelper.DELETED_TASKS_TABLE_NAME);
            // deleting database item
            final SQLiteDatabase WritableDatabase = dbHelper.getWritableDatabase();
            WritableDatabase.delete(DBHelper.DELETED_TASKS_TABLE_NAME, DBHelper.KEY_TASK_TEXT + "=?", new String[]{deletedModel.getMainText()});

            // showing snack bar with Undo option
            Snackbar snackbar = Snackbar.make(getActivity().getWindow().getDecorView().getRootView(), getResources().getText(R.string.snackbar_removed), Snackbar.LENGTH_LONG);
            snackbar.setAnchorView(R.id.fabs_constraintLayout_deleted);
            snackbar.setAnimationMode(BaseTransientBottomBar.ANIMATION_MODE_SLIDE);
            snackbar.setAction(getResources().getText(R.string.snackbar_undo), new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    // undo is selected, restore the deleted item
                    adapter.restoreItem(deletedModel, deletedPosition);
                    ContentValues contentValues = new ContentValues();
                    contentValues.put(DBHelper.KEY_TASK_TEXT, deletedModel.getMainText());
                    database.insert(DBHelper.COMPLETED_TASKS_TABLE_NAME, null, contentValues);
                }
            });
            snackbar.show();
        }


    }
}