package com.fxn.adapters;

import android.content.Context;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;

import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.RequestManager;
import com.bumptech.glide.load.resource.bitmap.CenterCrop;
import com.bumptech.glide.load.resource.bitmap.FitCenter;
import com.bumptech.glide.request.RequestOptions;
import com.fxn.interfaces.OnSelectionListener;
import com.fxn.modals.Img;
import com.fxn.pix.R;
import com.fxn.utility.Utility;

import java.util.ArrayList;

/**
 * Created by akshay on 17/03/18.
 */

public class InstantImageAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private Context context;
    private ArrayList<Img> list;
    private OnSelectionListener onSelectionListener;
    private RequestManager glide;
    private RequestOptions options;

    private boolean mIsLoading;
    private int mNextPage = 1;

    public InstantImageAdapter(Context context) {
        this.context = context;
        this.list = new ArrayList<>();

        glide = Glide.with(context);
        options = new RequestOptions().override(256).transform(new CenterCrop()).transform(new FitCenter());
    }

    public void addOnSelectionListener(OnSelectionListener onSelectionListener) {
        this.onSelectionListener = onSelectionListener;
    }

    public void addImageList(ArrayList<Img> images) {
        if (images == null){
            return;
        }
        int size = list.size();
        list.addAll(images);
        notifyItemRangeChanged(size, images.size());
    }

    public void clearList() {
        list.clear();
    }

    public void clearSelection(){
        Img img;
        for (int i = 0; i < list.size(); i++){
            img = list.get(i);
            if (img.getSelected()){
                img.setSelected(false);
                notifyItemChanged(i);
            }
        }
    }

    public void select(Img img, boolean selection){
        int pos = list.indexOf(img);
        if (pos >= 0){
            list.get(pos).setSelected(selection);
            notifyItemChanged(pos);
        }
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == MainImageAdapter.HEADER) {
            View view = LayoutInflater.from(parent.getContext()).
                    inflate(R.layout.inital_image, parent, false);
            return new HolderNone(view);
        } else {
            View view = LayoutInflater.from(parent.getContext()).
                    inflate(R.layout.inital_image, parent, false);
            return new Holder(view);
        }
    }

    @Override
    public int getItemViewType(int position) {
        Img image = list.get(position);
        return (image.getContentUrl().isEmpty()) ? MainImageAdapter.HEADER : MainImageAdapter.ITEM;
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        Img image = list.get(position);
        if (holder instanceof Holder) {
            Holder imageHolder = (Holder) holder;
            int margin = 2;
            float size = Utility.convertDpToPixel(72, context) - 2;
            FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams((int) size, (int) size);
            layoutParams.setMargins(margin, margin, margin, margin);
            imageHolder.itemView.setLayoutParams(layoutParams);
            int padding = (int) (size / 3.5);
            imageHolder.selection.setPadding(padding, padding, padding, padding);
            imageHolder.preview.setLayoutParams(layoutParams);

            glide.load(image.getContentUrl()).apply(options).into(imageHolder.preview);

            imageHolder.selection.setVisibility(image.getSelected() ? View.VISIBLE : View.GONE);
        } else {
            HolderNone noneHolder = (HolderNone) holder;
            FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(0, 0);
            noneHolder.itemView.setLayoutParams(layoutParams);
            noneHolder.itemView.setVisibility(View.GONE);
        }
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    public boolean isLoading() {
        return mIsLoading;
    }

    public void setLoading(boolean loading) {
        mIsLoading = loading;
    }

    public int getNextPage() {
        return mNextPage;
    }

    public void setNextPage(int nextPage) {
        mNextPage = nextPage;
    }

    public class Holder extends RecyclerView.ViewHolder implements View.OnClickListener {
        ImageView preview;
        ImageView selection;

        Holder(View itemView) {
            super(itemView);
            preview = itemView.findViewById(R.id.preview);
            selection = itemView.findViewById(R.id.selection);
            itemView.setOnClickListener(this);
        }

        @Override
        public void onClick(View view) {
            int id = this.getLayoutPosition();
            onSelectionListener.onClick(list.get(id));
        }
    }

    public class HolderNone extends RecyclerView.ViewHolder {
        HolderNone(View itemView) {
            super(itemView);
        }
    }
}
