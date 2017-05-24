/*
 * Originally based on io.realm.RealmBaseAdapter
 * =============================================
 * Copyright 2014 Realm Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.realm;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.tonicartos.superslim.GridSLM;
import com.tonicartos.superslim.LinearSLM;

import java.util.ArrayList;
import java.util.List;

import co.moonmonkeylabs.realmrecyclerview.LoadMoreListItemView;
import co.moonmonkeylabs.realmrecyclerview.R;
import co.moonmonkeylabs.realmrecyclerview.RealmRecyclerView;
import io.realm.internal.RealmObjectProxy;

/**
 * The base {@link RecyclerView.Adapter} that includes custom functionality to be used with the
 * {@link RealmRecyclerView}.
 */
public abstract class RealmBasedRecyclerViewAdapter
        <T extends RealmModel, VH extends RealmViewHolder>
        extends RecyclerView.Adapter<RealmViewHolder> {

    public class RowWrapper {

        public final boolean isRealm;
        public final int realmIndex;
        public final int sectionHeaderIndex;
        public final String header;

        public RowWrapper(int realmIndex, int sectionHeaderIndex) {
            this(true, realmIndex, sectionHeaderIndex, null);
        }

        public RowWrapper(int sectionHeaderIndex, String header) {
            this(false, -1, sectionHeaderIndex, header);
        }

        public RowWrapper(boolean isRealm, int realmIndex, int sectionHeaderIndex, String header) {
            this.isRealm = isRealm;
            this.realmIndex = realmIndex;
            this.sectionHeaderIndex = sectionHeaderIndex;
            this.header = header;
        }
    }

    private static final List<Long> EMPTY_LIST = new ArrayList<>(0);

    private Object loadMoreItem;
    private Object footerItem;

    protected final int HEADER_VIEW_TYPE = 100;
    private final int LOAD_MORE_VIEW_TYPE = 101;
    private final int FOOTER_VIEW_TYPE = 102;

    private Context context;
    protected LayoutInflater inflater;
    protected OrderedRealmCollection<T> adapterData;

    private List<RowWrapper> rowWrappers;

    private OrderedRealmCollectionChangeListener listener;
    private boolean automaticUpdate;
    private boolean animateResults;
    private boolean addSectionHeaders;
    private boolean isStickyHeader;
    private long realmHeaderColumnIndex = -1;

    public interface OnRealmDataChange {
        void onDataChange(OrderedRealmCollection<? extends RealmModel> newData);
    }

    private OnRealmDataChange onRealmDataChangeListener;

    public RealmBasedRecyclerViewAdapter(
            Context context,
            OrderedRealmCollection<T> adapterData,
            boolean automaticUpdate,
            boolean animateResults) {
        this(context, adapterData, automaticUpdate, animateResults, false, -1);
    }

    public RealmBasedRecyclerViewAdapter(
            Context context,
            OrderedRealmCollection<T> adapterData,
            boolean automaticUpdate,
            boolean animateResults,
            boolean addSectionHeaders,
            long realmHeaderColumnIndex) {
        this(context, adapterData, automaticUpdate, animateResults, addSectionHeaders, realmHeaderColumnIndex, false);
    }

    public RealmBasedRecyclerViewAdapter(
            Context context,
            OrderedRealmCollection<T> adapterData,
            boolean automaticUpdate,
            boolean animateResults,
            boolean addSectionHeaders,
            long realmHeaderColumnIndex,
            boolean isStickyHeader) {
        if (context == null) {
            throw new IllegalArgumentException("Context cannot be null");
        }

        this.context = context;
        this.automaticUpdate = automaticUpdate;
        this.animateResults = animateResults;
        this.addSectionHeaders = addSectionHeaders;
        this.realmHeaderColumnIndex = realmHeaderColumnIndex;
        this.isStickyHeader = isStickyHeader;
        this.inflater = LayoutInflater.from(context);
        this.listener = (!automaticUpdate) ? null : getRealmChangeListener();

        rowWrappers = new ArrayList<>();
        if(null != adapterData) {
            setAdapterData(adapterData);
        }
    }

    public void setOnRealmDataChangeListener(OnRealmDataChange onRealmDataChangeListener) {
        this.onRealmDataChangeListener = onRealmDataChangeListener;
    }

    public void setAdapterData(OrderedRealmCollection<T> newAdapterData) {
        //clear any listener for previous data set when current dat is null
        if(null == newAdapterData) {
            if (listener != null && isDataValid()) {
                removeListener(adapterData);
                return;
            }
        }
        // If automatic updates aren't enabled, then animateResults should be false as well.
        this.animateResults = (automaticUpdate && animateResults && newAdapterData != null);
        if (addSectionHeaders && realmHeaderColumnIndex == -1) {
            throw new IllegalStateException(
                    "A headerColumnName is required for section headers");
        }

        updateAdapterData(newAdapterData);
    }

    public abstract VH onCreateRealmViewHolder(ViewGroup viewGroup, int viewType);

    public abstract void onBindRealmViewHolder(VH holder, int position);

    public VH onCreateFooterViewHolder(ViewGroup viewGroup) {
        throw new IllegalStateException("Implementation missing");
    }

    public void onBindFooterViewHolder(VH holder, int position) {
        throw new IllegalStateException("Implementation missing");
    }

    public RealmViewHolder onCreateHeaderViewHolder(ViewGroup viewGroup) {
        View view = inflater.inflate(R.layout.header_item, viewGroup, false);
        return new RealmViewHolder((TextView) view);
    }

    public void onBindHeaderViewHolder(RealmViewHolder holder, int position) {
        String header = rowWrappers.get(position).header;
        final GridSLM.LayoutParams layoutParams =
            GridSLM.LayoutParams.from(holder.itemView.getLayoutParams());

        holder.headerTextView.setText(header);
        if (layoutParams.isHeaderInline()) {
            layoutParams.width = ViewGroup.LayoutParams.MATCH_PARENT;
        } else {
            layoutParams.width = ViewGroup.LayoutParams.WRAP_CONTENT;
        }
    }

    public Context getContext() {
        return context;
    }

    /**
     * DON'T OVERRIDE THIS METHOD. Implement onCreateRealmViewHolder instead.
     */
    @Override
    public final RealmViewHolder onCreateViewHolder(ViewGroup viewGroup, int viewType) {
        if (viewType == HEADER_VIEW_TYPE) {
            return onCreateHeaderViewHolder(viewGroup);
        } else if (viewType == LOAD_MORE_VIEW_TYPE) {
            return new RealmViewHolder(new LoadMoreListItemView(viewGroup.getContext()));
        } else if (viewType == FOOTER_VIEW_TYPE) {
            return onCreateFooterViewHolder(viewGroup);
        }
        return onCreateRealmViewHolder(viewGroup, viewType);
    }

    /**
     * DON'T OVERRIDE THIS METHOD. Implement onBindRealmViewHolder instead.
     */
    @Override
    @SuppressWarnings("unchecked")
    public final void onBindViewHolder(RealmViewHolder holder, int position) {
        if (getItemViewType(position) == LOAD_MORE_VIEW_TYPE) {
            holder.loadMoreView.showSpinner();
        } else if (getItemViewType(position) == FOOTER_VIEW_TYPE) {
            onBindFooterViewHolder((VH) holder, position);
        } else {
            if (addSectionHeaders && isStickyHeader) {
                final String header = rowWrappers.get(position).header;
                final GridSLM.LayoutParams layoutParams =
                        GridSLM.LayoutParams.from(holder.itemView.getLayoutParams());
                // Setup the header
                if (header != null) {
                    layoutParams.isHeader = true;
                    onBindHeaderViewHolder(holder,position);
                } else {
                    onBindRealmViewHolder((VH) holder, rowWrappers.get(position).realmIndex);
                }
                layoutParams.setSlm(LinearSLM.ID);
                if (header != null) {
                    layoutParams.setFirstPosition(position);
                } else {
                    layoutParams.setFirstPosition(rowWrappers.get(position).sectionHeaderIndex);
                }
                holder.itemView.setLayoutParams(layoutParams);
            } else {
                onBindRealmViewHolder((VH) holder, position);
            }
        }
    }

    public Object getLastItem() {
        if (addSectionHeaders) {
            return adapterData.get(rowWrappers.get(rowWrappers.size() - 1).realmIndex);
        } else {
            return adapterData.get(adapterData.size() - 1);
        }
    }

    @Override
    public int getItemCount() {
        int extraCount = loadMoreItem == null ? 0 : 1;
        extraCount += footerItem == null ? 0 : 1;

        if (addSectionHeaders) {
            return rowWrappers.size() + extraCount;
        }

        if (!isDataValid()) {
            return extraCount;
        }

        return adapterData.size() + extraCount;
    }

    public boolean isEmpty() {
        return !isDataValid() || adapterData.size() == 0;
    }

    @Override
    public int getItemViewType(int position) {
        if (loadMoreItem != null && position == getItemCount() - 1) {
            return LOAD_MORE_VIEW_TYPE;
        } else if (footerItem != null && position == getItemCount() - 1) {
            return FOOTER_VIEW_TYPE;
        } else if (isStickyHeader && !rowWrappers.isEmpty() && !rowWrappers.get(position).isRealm) {
            return HEADER_VIEW_TYPE;
        }
        return getItemRealmViewType(position);
    }

    public int getItemRealmViewType(int position) {
        return super.getItemViewType(position);
    }

    /**
     * Ensure {@link #close()} is called whenever {@link Realm#close()} is called to ensure that the
     * {@link #adapterData} are invalidated and the change listener removed.
     */
    public void close() {
        updateAdapterData(null);
    }

    /**
     * Update the OrderedRealmCollection associated with the Adapter. Useful when the query has been changed.
     * If the query does not change you might consider using the automaticUpdate feature.
     *
     * @param queryResults the new OrderedRealmCollection coming from the new query.
     */
    public void updateAdapterData(OrderedRealmCollection<T> queryResults) {
        if (listener != null && isDataValid()) {
            removeListener(adapterData);
        }

        adapterData = queryResults;
        if (listener != null && adapterData != null) {
            addListener(adapterData);
        }

        updateRowWrappers();

        if (adapterData != null) {
    	    notifyDataSetChanged();
	    }
    }

    /**
     * Method that creates the header string that should be used. Override this method to have
     * a custom header.
     */
    public String createHeaderFromColumnValue(Object columnValue) {
        String result;
        if (columnValue instanceof String) {
            result = ((String) columnValue).substring(0, 1);
        } else if (columnValue instanceof Long) {
            result = columnValue.toString();
        } else {
            throw new IllegalStateException("columnType not supported");
        }

        return result;

    }

    public String getHeaderAtPosition(int position) {
        for (int i = rowWrappers.size() - 1; i >= 0; i--) {
            RowWrapper rowWrapper = rowWrappers.get(i);
            if (!rowWrapper.isRealm) {
                if (rowWrapper.sectionHeaderIndex <= position) {
                    return rowWrapper.header;
                }
            }
        }

        return null;
    }

    private void updateRowWrappers() {
        if (!isDataValid()) {
            return;
        }
        if (addSectionHeaders) {
            String lastHeader = "";
            int headerCount = 0;
            int sectionFirstPosition = 0;
            rowWrappers.clear();

            int i = 0;
            for (RealmModel result : adapterData) {
                Object rawHeader;
                RealmFieldType fieldType = ((RealmObjectProxy) result)
                        .realmGet$proxyState().getRow$realm().getColumnType(realmHeaderColumnIndex);

                if (fieldType == RealmFieldType.STRING) {
                    rawHeader = ((RealmObjectProxy) result)
                            .realmGet$proxyState().getRow$realm().getString(realmHeaderColumnIndex);

                } else if (fieldType == RealmFieldType.INTEGER) {
                    rawHeader = ((RealmObjectProxy) result)
                            .realmGet$proxyState().getRow$realm().getLong(realmHeaderColumnIndex);
                } else {
                    throw new IllegalStateException("columnValue type not supported");
                }

                String header = createHeaderFromColumnValue(rawHeader);
                if (!TextUtils.equals(lastHeader, header)) {
                    // Insert new header view and update section data.
                    sectionFirstPosition = i + headerCount;
                    lastHeader = header;
                    headerCount += 1;

                    rowWrappers.add(new RowWrapper(sectionFirstPosition, header));
                }
                rowWrappers.add(new RowWrapper(i++, sectionFirstPosition));
            }
        }
    }

    public List<RowWrapper> getRowWrappers() {
        return rowWrappers;
    }

    private OrderedRealmCollectionChangeListener<OrderedRealmCollection<T>> getRealmChangeListener() {
        return new OrderedRealmCollectionChangeListener<OrderedRealmCollection<T>>() {

            @Override
            public void onChange(OrderedRealmCollection<T> elements, OrderedCollectionChangeSet changeSet) {
                // null Changes means the async query returns the first time.
                if (changeSet == null) {
                    notifyDataSetChanged();
                    return;
                }

                //notify of data change if any listener is registered
                if(null != onRealmDataChangeListener) {
                    onRealmDataChangeListener.onDataChange(elements);
                }

                // For deletions, the adapter has to be notified in reverse order.
                OrderedCollectionChangeSet.Range[] deletions = changeSet.getDeletionRanges();
                for (int i = deletions.length - 1; i >= 0; i--) {
                    OrderedCollectionChangeSet.Range range = deletions[i];
                    notifyItemRangeRemoved(range.startIndex, range.length);
                }

                OrderedCollectionChangeSet.Range[] insertions = changeSet.getInsertionRanges();
                for (OrderedCollectionChangeSet.Range range : insertions) {
                    notifyItemRangeInserted(range.startIndex, range.length);
                }

                OrderedCollectionChangeSet.Range[] modifications = changeSet.getChangeRanges();
                for (OrderedCollectionChangeSet.Range range : modifications) {
                    notifyItemRangeChanged(range.startIndex, range.length);
                }
            }
        };
    }

    private void addListener(@NonNull OrderedRealmCollection<T> data) {
        if (data instanceof RealmResults) {
            RealmResults<T> results = (RealmResults<T>) data;
            //noinspection unchecked
            results.addChangeListener(listener);
        } else if (data instanceof RealmList) {
            RealmList<T> list = (RealmList<T>) data;
            //noinspection unchecked
            list.addChangeListener(listener);
        } else {
            throw new IllegalArgumentException("RealmCollection not supported: " + data.getClass());
        }
    }

    private void removeListener(@NonNull OrderedRealmCollection<T> data) {
        if (data instanceof RealmResults) {
            RealmResults<T> results = (RealmResults<T>) data;
            //noinspection unchecked
            results.removeChangeListener(listener);
        } else if (data instanceof RealmList) {
            RealmList<T> list = (RealmList<T>) data;
            //noinspection unchecked
            list.removeChangeListener(listener);
        } else {
            throw new IllegalArgumentException("RealmCollection not supported: " + data.getClass());
        }
    }

    private BaseRealm getRealm(@NonNull OrderedRealmCollection<T> data) {
        if (data instanceof RealmResults) {
            RealmResults<T> results = (RealmResults<T>) data;
            //noinspection unchecked
            return results.realm;
        } else if (data instanceof RealmList) {
            RealmList<T> list = (RealmList<T>) data;
            //noinspection unchecked
            return list.realm;
        } else {
            throw new IllegalArgumentException("RealmCollection not supported: " + data.getClass());
        }
    }

    private boolean isDataValid() {
        return adapterData != null && adapterData.isValid();
    }

    /**
     * Adds the LoadMore item.
     */
    public void addLoadMore() {
        if (loadMoreItem != null || footerItem != null) {
            return;
        }
        loadMoreItem = new Object();
        notifyDataSetChanged();
    }

    /**
     * Removes the LoadMoreItems;
     */
    public void removeLoadMore() {
        if (loadMoreItem == null) {
            return;
        }
        loadMoreItem = null;
        notifyDataSetChanged();
    }

    /**
     * Adds the Footer item.
     */
    public void addFooter() {
        if (footerItem != null || loadMoreItem != null) {
            return;
        }
        footerItem = new Object();
        notifyDataSetChanged();
    }

    /**
     * Removes the Footer;
     */
    public void removeFooter() {
        if (footerItem == null) {
            return;
        }
        footerItem = null;
        notifyDataSetChanged();
    }

    /**
     * Called when an item has been dismissed by a swipe.
     *
     * Only supported with type linearLayout and thus the adapterData can be accessed directly.
     * If it is extended to LinearLayoutWithHeaders, rowWrappers will have to be used.
     */
    public void onItemSwipedDismiss(int position) {
        BaseRealm realm = getRealm(adapterData);
        realm.beginTransaction();
        adapterData.deleteFromRealm(position);
        realm.commitTransaction();
    }
}
