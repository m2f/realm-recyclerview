package co.moonmonkeylabs.realmexamples.realmrecyclerview;

import android.content.Intent;
import android.os.Bundle;
import androidx.annotation.IdRes;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import android.view.View;
import android.widget.Button;

import co.moonmonkeylabs.realmexamples.R;
import co.moonmonkeylabs.realmexamples.realmsearchview.SampleSearchActivity;

public class LayoutSelectorActivity extends AppCompatActivity {

    Toolbar toolbar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_layout_selector);

        toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        getSupportActionBar().setTitle(R.string.activity_selector_name);

        addOnClickListenerToActivity(R.id.recycler_grid_button, GridExampleActivity.class, "Grid");

        addOnClickListenerToActivity(R.id.recycler_to_do_button, ToDoActivity.class, null);

        addOnClickListenerToActivity(R.id.recycler_linear_button, MainActivity.class, "Linear");

        addOnClickListenerToActivity(R.id.recycler_linear_bulk_button, MainActivity.class, "LinearBulk");

        addOnClickListenerToActivity(R.id.recycler_staggered_button, MainActivity.class, "Staggered");

        addOnClickListenerToActivity(R.id.recycler_linear_with_load_more_button, MainActivity.class, "LinearLoadMore");

        addOnClickListenerToActivity(R.id.recycler_loading_list, LoadingListActivity.class, "Loading List ");

        //TODO: Fix the below use cases
        addOnClickListenerToActivity(R.id.recycler_section_header_button, MainActivity2.class, "Header (SLM) ");

        addOnClickListenerToActivity(R.id.search_view, SampleSearchActivity.class, "Search Activity");
    }

    private Button addOnClickListenerToActivity(@IdRes int viewId, final Class<?> activity, @Nullable final String typeExtra) {
        final Button button = (Button) findViewById(viewId);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(LayoutSelectorActivity.this, activity);
                intent.putExtra("Type", typeExtra);
                startActivity(intent);
            }
        });
        return button;
    }
}
