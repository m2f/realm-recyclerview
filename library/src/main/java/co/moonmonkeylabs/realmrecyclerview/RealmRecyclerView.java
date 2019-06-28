package co.moonmonkeylabs.realmrecyclerview;

import android.content.Context;
import android.content.res.TypedArray;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;
import androidx.recyclerview.widget.ItemTouchHelper;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewStub;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.tonicartos.superslim.LayoutManager;

import io.realm.RealmBasedRecyclerViewAdapter;

/**
 * A recyclerView that has a few extra features.
 * - Automatic empty state
 * - Pull-to-refresh
 * - LoadMore
 */
public class RealmRecyclerView extends FrameLayout {

    public interface OnRefreshListener {
        void onRefresh();
    }

    public interface OnLoadMoreListener {
        void onLoadMore(Object lastItem);
    }

    private enum Type {
        LinearLayout,
        Grid,
        LinearLayoutWithHeaders,
	StaggeredGridLayout
    }

    private enum Orientation {
        Vertical,
        Horizontal
    }

    private SwipeRefreshLayout swipeRefreshLayout;
    private RecyclerView recyclerView;
    private ViewStub emptyContentContainer;
    private RealmBasedRecyclerViewAdapter adapter;
    private RealmSimpleItemTouchHelperCallback realmSimpleItemTouchHelperCallback;
    private boolean hasLoadMoreFired;
    private boolean showShowLoadMore;

    // Attributes
    private boolean isRefreshable;
    private int emptyViewId;
    private String emptyMessage;
    private Type type;
    private int gridSpanCount;
    private int gridWidthPx;
    private boolean swipeToDelete;
    private int bufferItems = 3;
    private Orientation orientation;
    private boolean reverseLayout;
    private boolean stackFromEnd;

    private TextView emptyMessageTv = null;

    private StaggeredGridLayoutManager staggeredGridManager;
    private GridLayoutManager gridManager;
    private int lastMeasuredWidth = -1;

    // State
    private boolean isRefreshing;

    // Listener
    private OnRefreshListener onRefreshListener;
    private OnLoadMoreListener onLoadMoreListener;

    public RealmRecyclerView(Context context) {
        super(context);
        init(context, null);
    }

    public RealmRecyclerView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    public RealmRecyclerView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs);
    }
    
    public RealmRecyclerView(Context context, AttributeSet attrs, int defStyleAttr, int bufferItems) {
        super(context, attrs, defStyleAttr);
        if (bufferItems <= 0) bufferItems = 0;
        this.bufferItems = bufferItems;
        init(context, attrs);
    }

    @Override
    protected void onMeasure(int widthSpec, int heightSpec) {
        super.onMeasure(widthSpec, heightSpec);
        if (gridWidthPx != -1 && gridManager != null && lastMeasuredWidth != getMeasuredWidth()) {
            int spanCount = Math.max(1, getMeasuredWidth() / gridWidthPx);
            gridManager.setSpanCount(spanCount);
            lastMeasuredWidth = getMeasuredWidth();
        }
    }

    private void init(Context context, AttributeSet attrs) {
        inflate(context, R.layout.realm_recycler_view, this);
        initAttrs(context, attrs);

        swipeRefreshLayout = (SwipeRefreshLayout) findViewById(R.id.rrv_swipe_refresh_layout);
        recyclerView = (RecyclerView) findViewById(R.id.rrv_recycler_view);
        emptyContentContainer = (ViewStub) findViewById(R.id.rrv_empty_content_container);

        swipeRefreshLayout.setEnabled(isRefreshable);
        if (isRefreshable) {
            swipeRefreshLayout.setOnRefreshListener(recyclerViewRefreshListener);
        }

        if(emptyMessage != null) {
            emptyContentContainer.setLayoutResource(R.layout.empty_message_view);
            View inflated = emptyContentContainer.inflate();
            emptyMessageTv = (TextView) inflated.findViewById(R.id.empty_message_tv);
            emptyMessageTv.setText(emptyMessage);
        } else if (emptyViewId != 0) {
            emptyContentContainer.setLayoutResource(emptyViewId);
            emptyContentContainer.inflate();
        }

        if (type == null) {
            throw new IllegalStateException("A type has to be specified via XML attribute");
        }
        switch (type) {
            case LinearLayout:
                LinearLayoutManager manager = new LinearLayoutManager(getContext(),
                        getLinearLayoutManagerOrientation(), reverseLayout);
                manager.setStackFromEnd(stackFromEnd);
                recyclerView.setLayoutManager(manager);
                break;

            case Grid:
                throwIfSwipeToDeleteEnabled();
                if (gridSpanCount == -1 && gridWidthPx == -1) {
                    throw new IllegalStateException(
                            "For GridLayout, a span count or item width has to be set");
                } else if(gridSpanCount != -1 && gridWidthPx != -1) {
                    // This is awkward. Both values are set. Instead of picking one, throw an error.
                    throw new IllegalStateException(
                            "For GridLayout, a span count and item width can not both be set");
                }
                // Uses either the provided gridSpanCount or 1 as a placeholder what will be
                // calculated based on gridWidthPx in onMeasure.
                int spanCount = gridSpanCount == -1 ? 1 : gridSpanCount;
                gridManager = new GridLayoutManager(getContext(), spanCount);
                recyclerView.setLayoutManager(gridManager);
                break;

            case LinearLayoutWithHeaders:
                throwIfSwipeToDeleteEnabled();
                LayoutManager managerHeader = new LayoutManager(getContext());
                recyclerView.setLayoutManager(managerHeader);
                break;

            case StaggeredGridLayout:
                int staggeredSpanCount = gridSpanCount == -1 ? 1 : gridSpanCount;
                staggeredGridManager = new StaggeredGridLayoutManager(staggeredSpanCount, StaggeredGridLayoutManager.VERTICAL);
                recyclerView.setLayoutManager(staggeredGridManager);
                break;

            default:
                throw new IllegalStateException("The type attribute has to be set.");
        }
        recyclerView.setHasFixedSize(true);

        recyclerView.addOnScrollListener(
                new RecyclerView.OnScrollListener() {
                    @Override
                    public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                        super.onScrolled(recyclerView, dx, dy);
                    }
                }
        );

        recyclerView.addOnScrollListener(
                new RecyclerView.OnScrollListener() {
                    @Override
                    public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                        super.onScrollStateChanged(recyclerView, newState);
                    }

                    @Override
                    public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                        maybeFireLoadMore();
                    }
                }
        );

        if (swipeToDelete) {
            realmSimpleItemTouchHelperCallback = new RealmSimpleItemTouchHelperCallback();
            new ItemTouchHelper(realmSimpleItemTouchHelperCallback)
                    .attachToRecyclerView(recyclerView);
        }
    }

    public int getLinearLayoutManagerOrientation() {
        return orientation == Orientation.Horizontal ?
                LinearLayoutManager.HORIZONTAL :
                LinearLayoutManager.VERTICAL;
    }

    /**
     * Sets the orientation of the layout. {@link LinearLayoutManager}
     * will do its best to keep scroll position.
     */
    public void setOrientation(int orientation) {
        if (gridManager != null) {
            gridManager.setOrientation(orientation);
        } else if (staggeredGridManager != null) {
            staggeredGridManager.setOrientation(orientation);
        } else {
            throw new IllegalStateException("Error init of your LayoutManager");
        }
    }

    private void throwIfSwipeToDeleteEnabled() {
        if (!swipeToDelete) {
            return;
        }
        throw new IllegalStateException(
                "SwipeToDelete not supported with this layout type: " + type.name());
    }

    public void setOnLoadMoreListener(OnLoadMoreListener onLoadMoreListener) {
        this.onLoadMoreListener = onLoadMoreListener;
    }

    public void enableShowLoadMore() {
        showShowLoadMore = true;
        ((RealmBasedRecyclerViewAdapter) recyclerView.getAdapter()).addLoadMore();
    }

    public void disableShowLoadMore() {
        showShowLoadMore = false;
        ((RealmBasedRecyclerViewAdapter) recyclerView.getAdapter()).removeLoadMore();
    }

    private void maybeFireLoadMore() {
        if (hasLoadMoreFired) {
            return;
        }
        if (!showShowLoadMore) {
            return;
        }

        final RecyclerView.LayoutManager layoutManager = recyclerView.getLayoutManager();
        int visibleItemCount = layoutManager.getChildCount();
        int totalItemCount = layoutManager.getItemCount();
        int firstVisibleItemPosition = findFirstVisibleItemPosition();

        if (totalItemCount == 0) {
            return;
        }

        if (firstVisibleItemPosition + visibleItemCount + bufferItems > totalItemCount) {
            if (onLoadMoreListener != null) {
                hasLoadMoreFired = true;
                onLoadMoreListener.onLoadMore(adapter.getLastItem());
            }
        }
    }

    public int findFirstVisibleItemPosition() {
        switch (type) {
            case LinearLayout:
                return ((LinearLayoutManager) recyclerView.getLayoutManager())
                        .findFirstVisibleItemPosition();
            case Grid:
                return ((GridLayoutManager) recyclerView.getLayoutManager())
                        .findFirstVisibleItemPosition();
            case LinearLayoutWithHeaders:
                return ((LayoutManager) recyclerView.getLayoutManager())
                        .findFirstVisibleItemPosition();
            case StaggeredGridLayout:
                return ((StaggeredGridLayoutManager) recyclerView.getLayoutManager())
                        .findFirstVisibleItemPositions(null)[0];
            default:
                throw new IllegalStateException("Type of layoutManager unknown." +
                        "In this case this method needs to be overridden");
        }
    }

    private void initAttrs(Context context, AttributeSet attrs) {
        TypedArray typedArray =
                context.obtainStyledAttributes(attrs, R.styleable.RealmRecyclerView);

        isRefreshable = typedArray.getBoolean(R.styleable.RealmRecyclerView_rrvIsRefreshable, false);
        emptyMessage = typedArray.getString(R.styleable.RealmRecyclerView_rrvEmptyMessage);
        emptyViewId = typedArray.getResourceId(R.styleable.RealmRecyclerView_rrvEmptyLayoutId, 0);
        gridSpanCount = typedArray.getInt(R.styleable.RealmRecyclerView_rrvGridLayoutSpanCount, -1);
        gridWidthPx = typedArray.getDimensionPixelSize(R.styleable.RealmRecyclerView_rrvGridLayoutItemWidth, -1);
        swipeToDelete = typedArray.getBoolean(R.styleable.RealmRecyclerView_rrvSwipeToDelete, false);
        stackFromEnd = typedArray.getBoolean(R.styleable.RealmRecyclerView_rrvStackFromEnd, false);
        reverseLayout = typedArray.getBoolean(R.styleable.RealmRecyclerView_rrvReverseLayout, false);
        int typeValue = typedArray.getInt(R.styleable.RealmRecyclerView_rrvLayoutType, -1);
        if (typeValue != -1) {
            type = Type.values()[typeValue];
        }
        int orientationValue = typedArray.getInt(R.styleable.RealmRecyclerView_rrvOrientation, -1);
        if (orientationValue != -1) {
            orientation = Orientation.values()[orientationValue];
        }
        typedArray.recycle();
    }

    public void addItemDecoration(RecyclerView.ItemDecoration decor) {
        recyclerView.addItemDecoration(decor);
    }

    public void addItemDecoration(RecyclerView.ItemDecoration decor, int index) {
        recyclerView.addItemDecoration(decor, index);
    }

    public void removeItemDecoration(RecyclerView.ItemDecoration decor) {
        recyclerView.removeItemDecoration(decor);
    }

    public void setAdapter(final RealmBasedRecyclerViewAdapter adapter) {
        this.adapter = adapter;
        recyclerView.setAdapter(adapter);
        setEmptyStubVisibility(null == adapter || adapter.isEmpty());
        if (swipeToDelete) {
            realmSimpleItemTouchHelperCallback.setAdapter(adapter);
        }

        if (adapter != null) {
            adapter.registerAdapterDataObserver(
                    new RecyclerView.AdapterDataObserver() {
                        @Override
                        public void onItemRangeMoved(
                                int fromPosition,
                                int toPosition,
                                int itemCount) {
                            super.onItemRangeMoved(fromPosition, toPosition, itemCount);
                            update();
                        }

                        @Override
                        public void onItemRangeRemoved(int positionStart, int itemCount) {
                            super.onItemRangeRemoved(positionStart, itemCount);
                            update();
                        }

                        @Override
                        public void onItemRangeInserted(int positionStart, int itemCount) {
                            super.onItemRangeInserted(positionStart, itemCount);
                            update();
                        }

                        @Override
                        public void onItemRangeChanged(int positionStart, int itemCount) {
                            super.onItemRangeChanged(positionStart, itemCount);
                            update();
                        }

                        @Override
                        public void onChanged() {
                            super.onChanged();
                            update();
                        }

                        private void update() {
                            if(stackFromEnd) smoothScrollToPosition(adapter.getItemCount());
                            setEmptyStubVisibility(adapter.isEmpty());
                        }
                    }
            );
        }
    }

    public void setEmptyStubVisibility(boolean visible) {
        if(null != emptyMessageTv) {
            emptyMessageTv.setVisibility( visible ? View.VISIBLE : View.GONE);
        } else if (emptyViewId != 0) {
            emptyContentContainer.setVisibility( visible ? View.VISIBLE : View.GONE);
        }
    }

    public void setItemViewCacheSize(int size) {
        recyclerView.setItemViewCacheSize(size);
    }

    public void smoothScrollToPosition(int position) {
        recyclerView.smoothScrollToPosition(position);
    }

    public void scrollToPosition(int position) {
        recyclerView.scrollToPosition(position);
    }
    
    //
    // Expose public RecycleView
    
    public RecyclerView getRecycleView(){
        return recyclerView;
    }

    //
    // Pull-to-refresh
    //

    public void setOnRefreshListener(OnRefreshListener onRefreshListener) {
        this.onRefreshListener = onRefreshListener;
    }

    public void setRefreshing(boolean refreshing) {
        if (!isRefreshable) {
            return;
        }
        isRefreshing = refreshing;
        swipeRefreshLayout.setRefreshing(refreshing);
    }
    
    public void resetHasLoadMoreFired() {
        hasLoadMoreFired = false;
    }

    // Expose method to change the preloaded items
    public void setBufferItems(int bufferItems){
        if (bufferItems <= 0) bufferItems = 0;
        this.bufferItems = bufferItems;
    }

    private SwipeRefreshLayout.OnRefreshListener recyclerViewRefreshListener =
            new SwipeRefreshLayout.OnRefreshListener() {
                @Override
                public void onRefresh() {
                    if (!isRefreshing && onRefreshListener != null) {
                        onRefreshListener.onRefresh();
                    }
                    isRefreshing = true;
                }
            };
}
