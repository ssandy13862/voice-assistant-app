package com.voiceassistant.app.presentation.adapter;

/**
 * 对话历史适配器
 */
@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000,\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0010\u0002\n\u0002\b\u0002\n\u0002\u0010\b\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\u0004\u0018\u00002\u000e\u0012\u0004\u0012\u00020\u0002\u0012\u0004\u0012\u00020\u00030\u0001:\u0002\u000e\u000fB\u0005\u00a2\u0006\u0002\u0010\u0004J\u0018\u0010\u0005\u001a\u00020\u00062\u0006\u0010\u0007\u001a\u00020\u00032\u0006\u0010\b\u001a\u00020\tH\u0016J\u0018\u0010\n\u001a\u00020\u00032\u0006\u0010\u000b\u001a\u00020\f2\u0006\u0010\r\u001a\u00020\tH\u0016\u00a8\u0006\u0010"}, d2 = {"Lcom/voiceassistant/app/presentation/adapter/ConversationAdapter;", "Landroidx/recyclerview/widget/ListAdapter;", "Lcom/voiceassistant/app/domain/model/ConversationItem;", "Lcom/voiceassistant/app/presentation/adapter/ConversationAdapter$ConversationViewHolder;", "()V", "onBindViewHolder", "", "holder", "position", "", "onCreateViewHolder", "parent", "Landroid/view/ViewGroup;", "viewType", "ConversationDiffCallback", "ConversationViewHolder", "app_debug"})
public final class ConversationAdapter extends androidx.recyclerview.widget.ListAdapter<com.voiceassistant.app.domain.model.ConversationItem, com.voiceassistant.app.presentation.adapter.ConversationAdapter.ConversationViewHolder> {
    
    public ConversationAdapter() {
        super(null);
    }
    
    @java.lang.Override
    @org.jetbrains.annotations.NotNull
    public com.voiceassistant.app.presentation.adapter.ConversationAdapter.ConversationViewHolder onCreateViewHolder(@org.jetbrains.annotations.NotNull
    android.view.ViewGroup parent, int viewType) {
        return null;
    }
    
    @java.lang.Override
    public void onBindViewHolder(@org.jetbrains.annotations.NotNull
    com.voiceassistant.app.presentation.adapter.ConversationAdapter.ConversationViewHolder holder, int position) {
    }
    
    @kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000\u0018\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0010\u000b\n\u0002\b\u0004\b\u0002\u0018\u00002\b\u0012\u0004\u0012\u00020\u00020\u0001B\u0005\u00a2\u0006\u0002\u0010\u0003J\u0018\u0010\u0004\u001a\u00020\u00052\u0006\u0010\u0006\u001a\u00020\u00022\u0006\u0010\u0007\u001a\u00020\u0002H\u0016J\u0018\u0010\b\u001a\u00020\u00052\u0006\u0010\u0006\u001a\u00020\u00022\u0006\u0010\u0007\u001a\u00020\u0002H\u0016\u00a8\u0006\t"}, d2 = {"Lcom/voiceassistant/app/presentation/adapter/ConversationAdapter$ConversationDiffCallback;", "Landroidx/recyclerview/widget/DiffUtil$ItemCallback;", "Lcom/voiceassistant/app/domain/model/ConversationItem;", "()V", "areContentsTheSame", "", "oldItem", "newItem", "areItemsTheSame", "app_debug"})
    static final class ConversationDiffCallback extends androidx.recyclerview.widget.DiffUtil.ItemCallback<com.voiceassistant.app.domain.model.ConversationItem> {
        
        public ConversationDiffCallback() {
            super();
        }
        
        @java.lang.Override
        public boolean areItemsTheSame(@org.jetbrains.annotations.NotNull
        com.voiceassistant.app.domain.model.ConversationItem oldItem, @org.jetbrains.annotations.NotNull
        com.voiceassistant.app.domain.model.ConversationItem newItem) {
            return false;
        }
        
        @java.lang.Override
        public boolean areContentsTheSame(@org.jetbrains.annotations.NotNull
        com.voiceassistant.app.domain.model.ConversationItem oldItem, @org.jetbrains.annotations.NotNull
        com.voiceassistant.app.domain.model.ConversationItem newItem) {
            return false;
        }
    }
    
    @kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000$\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\u0018\u00002\u00020\u0001B\r\u0012\u0006\u0010\u0002\u001a\u00020\u0003\u00a2\u0006\u0002\u0010\u0004J\u000e\u0010\u0007\u001a\u00020\b2\u0006\u0010\t\u001a\u00020\nR\u000e\u0010\u0002\u001a\u00020\u0003X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0005\u001a\u00020\u0006X\u0082\u0004\u00a2\u0006\u0002\n\u0000\u00a8\u0006\u000b"}, d2 = {"Lcom/voiceassistant/app/presentation/adapter/ConversationAdapter$ConversationViewHolder;", "Landroidx/recyclerview/widget/RecyclerView$ViewHolder;", "binding", "Lcom/voiceassistant/app/databinding/ItemConversationBinding;", "(Lcom/voiceassistant/app/databinding/ItemConversationBinding;)V", "dateFormat", "Ljava/text/SimpleDateFormat;", "bind", "", "item", "Lcom/voiceassistant/app/domain/model/ConversationItem;", "app_debug"})
    public static final class ConversationViewHolder extends androidx.recyclerview.widget.RecyclerView.ViewHolder {
        @org.jetbrains.annotations.NotNull
        private final com.voiceassistant.app.databinding.ItemConversationBinding binding = null;
        @org.jetbrains.annotations.NotNull
        private final java.text.SimpleDateFormat dateFormat = null;
        
        public ConversationViewHolder(@org.jetbrains.annotations.NotNull
        com.voiceassistant.app.databinding.ItemConversationBinding binding) {
            super(null);
        }
        
        public final void bind(@org.jetbrains.annotations.NotNull
        com.voiceassistant.app.domain.model.ConversationItem item) {
        }
    }
}