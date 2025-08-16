package com.voiceassistant.app.databinding;
import com.voiceassistant.app.R;
import com.voiceassistant.app.BR;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import android.view.View;
@SuppressWarnings("unchecked")
public class ActivityMainBindingImpl extends ActivityMainBinding  {

    @Nullable
    private static final androidx.databinding.ViewDataBinding.IncludedLayouts sIncludes;
    @Nullable
    private static final android.util.SparseIntArray sViewsWithIds;
    static {
        sIncludes = null;
        sViewsWithIds = new android.util.SparseIntArray();
        sViewsWithIds.put(R.id.cameraPreview, 1);
        sViewsWithIds.put(R.id.statusCard, 2);
        sViewsWithIds.put(R.id.stateIndicator, 3);
        sViewsWithIds.put(R.id.stateText, 4);
        sViewsWithIds.put(R.id.faceCountText, 5);
        sViewsWithIds.put(R.id.faceConfidenceText, 6);
        sViewsWithIds.put(R.id.permissionStatus, 7);
        sViewsWithIds.put(R.id.controlButtons, 8);
        sViewsWithIds.put(R.id.freeModeButton, 9);
        sViewsWithIds.put(R.id.freeModeIndicator, 10);
        sViewsWithIds.put(R.id.interruptButton, 11);
        sViewsWithIds.put(R.id.conversationCard, 12);
        sViewsWithIds.put(R.id.conversationTitle, 13);
        sViewsWithIds.put(R.id.conversationRecyclerView, 14);
        sViewsWithIds.put(R.id.bottomButtons, 15);
        sViewsWithIds.put(R.id.clearHistoryButton, 16);
        sViewsWithIds.put(R.id.testVoiceButton, 17);
    }
    // views
    @NonNull
    private final androidx.constraintlayout.widget.ConstraintLayout mboundView0;
    // variables
    // values
    // listeners
    // Inverse Binding Event Handlers

    public ActivityMainBindingImpl(@Nullable androidx.databinding.DataBindingComponent bindingComponent, @NonNull View root) {
        this(bindingComponent, root, mapBindings(bindingComponent, root, 18, sIncludes, sViewsWithIds));
    }
    private ActivityMainBindingImpl(androidx.databinding.DataBindingComponent bindingComponent, View root, Object[] bindings) {
        super(bindingComponent, root, 0
            , (android.widget.LinearLayout) bindings[15]
            , (androidx.camera.view.PreviewView) bindings[1]
            , (com.google.android.material.button.MaterialButton) bindings[16]
            , (android.widget.LinearLayout) bindings[8]
            , (com.google.android.material.card.MaterialCardView) bindings[12]
            , (androidx.recyclerview.widget.RecyclerView) bindings[14]
            , (android.widget.TextView) bindings[13]
            , (android.widget.TextView) bindings[6]
            , (android.widget.TextView) bindings[5]
            , (com.google.android.material.button.MaterialButton) bindings[9]
            , (android.view.View) bindings[10]
            , (com.google.android.material.button.MaterialButton) bindings[11]
            , (android.widget.TextView) bindings[7]
            , (android.view.View) bindings[3]
            , (android.widget.TextView) bindings[4]
            , (com.google.android.material.card.MaterialCardView) bindings[2]
            , (com.google.android.material.button.MaterialButton) bindings[17]
            );
        this.mboundView0 = (androidx.constraintlayout.widget.ConstraintLayout) bindings[0];
        this.mboundView0.setTag(null);
        setRootTag(root);
        // listeners
        invalidateAll();
    }

    @Override
    public void invalidateAll() {
        synchronized(this) {
                mDirtyFlags = 0x2L;
        }
        requestRebind();
    }

    @Override
    public boolean hasPendingBindings() {
        synchronized(this) {
            if (mDirtyFlags != 0) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean setVariable(int variableId, @Nullable Object variable)  {
        boolean variableSet = true;
        if (BR.viewModel == variableId) {
            setViewModel((com.voiceassistant.app.presentation.main.MainViewModel) variable);
        }
        else {
            variableSet = false;
        }
            return variableSet;
    }

    public void setViewModel(@Nullable com.voiceassistant.app.presentation.main.MainViewModel ViewModel) {
        this.mViewModel = ViewModel;
    }

    @Override
    protected boolean onFieldChange(int localFieldId, Object object, int fieldId) {
        switch (localFieldId) {
        }
        return false;
    }

    @Override
    protected void executeBindings() {
        long dirtyFlags = 0;
        synchronized(this) {
            dirtyFlags = mDirtyFlags;
            mDirtyFlags = 0;
        }
        // batch finished
    }
    // Listener Stub Implementations
    // callback impls
    // dirty flag
    private  long mDirtyFlags = 0xffffffffffffffffL;
    /* flag mapping
        flag 0 (0x1L): viewModel
        flag 1 (0x2L): null
    flag mapping end*/
    //end
}