package app.crossword.yourealwaysbe.view.recycler;

import androidx.recyclerview.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.TextView;

import java.lang.Iterable;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Map;

/**
 * Created by rcooper on 7/8/15.
 */
public class SeparatedRecyclerViewAdapter<
    BodyHolder extends RecyclerView.ViewHolder,
    SectionAdapter extends RemovableRecyclerViewAdapter<BodyHolder>
> extends RecyclerView.Adapter<RecyclerView.ViewHolder> implements Dismissable {
    private static int HEADER = Integer.MIN_VALUE;

    private LinkedHashMap<String, SectionAdapter> sections = new LinkedHashMap<>();
    private final int textViewId;
    private Class<BodyHolder> bodyHolderClass;

    /**
     * Needs BodyHolder class for type safety
     */
    public SeparatedRecyclerViewAdapter(
        int textViewId, Class<BodyHolder> bodyHolderClass
    ) {
        this.textViewId = textViewId;
        this.bodyHolderClass = bodyHolderClass;
    }


    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup viewGroup, int viewType) {
        if(viewType == HEADER){
            TextView view = (TextView) LayoutInflater.from(viewGroup.getContext())
                    .inflate(textViewId, viewGroup, false);
            return new SimpleTextViewHolder(view);
        } else {
            RecyclerView.ViewHolder result = null;
            while(result == null){
                for(SectionAdapter sectionAdapter : sections.values()){
                    try {
                        result = sectionAdapter.onCreateViewHolder(viewGroup, viewType);
                    } catch(Exception e){
                        e.printStackTrace();
                    }
                }
            }
            return result;
        }
    }

    /**
     * Iterable over BodyHolder sub-adapters
     *
     * The item positions of the bodies in the list are relative to the
     * index of the IndexedSectionAdapter. I.e. item i is at position
     * index + i of the SeparatedRecyclerViewAdapter.
     */
    public Iterable<IndexedSectionAdapter> getIndexedSectionAdapters() {
        final Iterator<SectionAdapter> sectionAdapters
            = sections.values().iterator();

        return new Iterable<IndexedSectionAdapter>() {
            public Iterator<IndexedSectionAdapter> iterator() {
                return new Iterator<IndexedSectionAdapter>() {
                    private int startPos = 0;

                    public boolean hasNext() {
                        return sectionAdapters.hasNext();
                    }

                    public IndexedSectionAdapter next() {
                        int pos = startPos + 1;
                        SectionAdapter
                            sectionAdapter = sectionAdapters.next();

                        startPos += 1 + sectionAdapter.getItemCount();

                        return new IndexedSectionAdapter(pos, sectionAdapter);
                    }
                };
            }
        };
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder viewHolder, int position) {
        int sectionPosition = 0;
        for (Map.Entry<String, SectionAdapter> entry : this.sections.entrySet()) {
            int size = entry.getValue().getItemCount() + 1;
            if(position < sectionPosition + size){
                int index = position - sectionPosition;
                if(index == 0){
                    TextView view = (TextView) ((SimpleTextViewHolder) viewHolder).itemView;
                    view.setText(entry.getKey());
                } else {
                    BodyHolder bodyHolder = bodyHolderClass.cast(viewHolder);
                    entry.getValue().onBindViewHolder(bodyHolder, index - 1);
                }
                break;
            }
            sectionPosition += size;
        }
    }

    @Override
    public int getItemCount() {
        int count = 0;
        for(SectionAdapter section : sections.values()){
            count++;
            count += section.getItemCount();
        }
        return count;
    }

    @Override
    public int getItemViewType(int position) {
        int sectionPosition = 0;
        for (Map.Entry<String, SectionAdapter> entry : this.sections.entrySet()) {
            int size = entry.getValue().getItemCount() + 1;
            if(position < sectionPosition + size){
                int index = position - sectionPosition;
                if(index == 0){
                    return HEADER;
                } else {
                    return entry.getValue().getItemViewType(index -1);
                }
            }
            sectionPosition += size;
        };
        throw new RuntimeException("Unable to find anything for position "+position);
    }

    public void addSection(String header, SectionAdapter adapter) {
        this.sections.put(header, adapter);
    }

    @Override
    public void onItemDismiss(int position) {
       int sectionPosition = 0;
        for (Map.Entry<String, SectionAdapter> entry : new LinkedList<>(this.sections.entrySet())) {
            int size = entry.getValue().getItemCount() + 1;
            if (position < sectionPosition + size) {
                int index = position - sectionPosition;
                if (index == 0) {
                    return;
                } else {
                    entry.getValue().remove(index - 1);
                    notifyItemRemoved(position);
                    if(entry.getValue().getItemCount() == 0){
                        this.sections.remove(entry.getKey());
                        notifyItemRemoved(position - 1);
                    }
                    break;
                }
            }
            sectionPosition += size;
        }

    }

    public static class SimpleTextViewHolder extends RecyclerView.ViewHolder {
        public SimpleTextViewHolder(TextView itemView) {
            super(itemView);
        }
    }

    /**
     * Contains a BodyHolder and the item index of its first element
     */
    public class IndexedSectionAdapter {
        private int index;
        private SectionAdapter sectionAdapter;

        public IndexedSectionAdapter(int index, SectionAdapter sectionAdapter) {
            this.index = index;
            this.sectionAdapter = sectionAdapter;
        }

        public int getIndex() { return index; }
        public SectionAdapter getSectionAdapter() { return sectionAdapter; }
    }
}
