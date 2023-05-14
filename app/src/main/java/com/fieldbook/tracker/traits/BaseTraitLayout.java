package com.fieldbook.tracker.traits;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;

import androidx.annotation.Nullable;

import com.fieldbook.tracker.R;
import com.fieldbook.tracker.activities.CollectActivity;
import com.fieldbook.tracker.database.DataHelper;
import com.fieldbook.tracker.database.models.ObservationModel;
import com.fieldbook.tracker.interfaces.CollectController;
import com.fieldbook.tracker.objects.RangeObject;
import com.fieldbook.tracker.objects.TraitObject;
import com.fieldbook.tracker.preferences.GeneralKeys;
import com.fieldbook.tracker.views.CollectInputView;
import com.fieldbook.tracker.views.RepeatedValuesView;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

public abstract class BaseTraitLayout extends LinearLayout {

    //tracks if data can be entered or not
    //references the collect activity locked state (locked, unlocked or frozen)
    protected boolean isLocked = false;

    protected CollectController controller;

    public BaseTraitLayout(Context context) {
        super(context);
        initController(context);
    }

    public BaseTraitLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        initController(context);
    }

    public BaseTraitLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initController(context);
    }

    private void initController(Context context) {
        if (context instanceof CollectController) {
            this.controller = (CollectController) context;
        }
    }

    public abstract int layoutId();

    public abstract String type();  // return trait type

    public String decodeValue(String value) { return value; }

    public boolean isTraitType(String trait) {
        return trait.equals(type());
    }

    public abstract void init(Activity act);

    /**
     * Override to block multi-measure navigation with specific condition
     */
    public boolean block() { return false; }

    /**
     * Useful function when traits have listeners.
     * @param onNew
     */
    public void refreshLayout(Boolean onNew) {

        getCollectInputView().getRepeatView().refresh(onNew);

    }

    public void loadLayout() {

        ((CollectActivity) getContext()).refreshRepeatedValuesToolbarIndicator();

        //right now text entry is disabled in the camera and photo traits
        //uris are too long to be nicely displayed in the current editTexts
        if (type().equals(PhotoTraitLayout.type)
                || type().equals(UsbCameraTraitLayout.type)
                || isTraitType(LabelPrintTraitLayout.type)
                || type().equals(AudioTraitLayout.type)) {
            toggleVisibility(View.GONE);
        } else {
            toggleVisibility(View.VISIBLE);
        }

        CollectActivity act = (CollectActivity) getContext();
        isLocked = act.isFrozen() || act.isLocked();

        ObservationModel[] observations = getDatabase().getRepeatedValues(
                act.getStudyId(),
                act.getObservationUnit(),
                act.getTraitName()
        );

        //clear old list of repeated values each time a new trait is loaded
        act.getInputView().getRepeatView().clear();

        act.getInputView().getRepeatView().setDisplayColor(Color.parseColor(getDisplayColor()));

        if (observations.length > 0) {

            String value = observations[observations.length - 1].getValue();

            if (!value.isEmpty()) {

                for (ObservationModel m : observations) {
                    if (!m.getValue().isEmpty()) {
                        m.setValue(decodeValue(m.getValue()));
                    }
                }

                act.getInputView().setTextColor(Color.parseColor(getDisplayColor()));

                act.getInputView().prepareObservationsExistMode(Arrays.asList(observations));

            }

            afterLoadExists(act, value);

        } else {

            act.getInputView().prepareEmptyObservationsMode();

            getCollectInputView().setTextColor(Color.BLACK);

            checkDefaultValue();
        }
    }

    private void checkDefaultValue() {

        TraitObject trait = getCurrentTrait();
        CollectActivity act = (CollectActivity) getContext();

        if (trait.getDefaultValue() != null && !trait.getDefaultValue().isEmpty()) {

            String defaultValue = trait.getDefaultValue();
            getCollectInputView().setText(defaultValue);
            updateObservation(trait.getTrait(), trait.getFormat(), defaultValue);
            afterLoadDefault(act);

        } else {
            act.getInputView().setHasData(false);
            afterLoadNotExists(act);
        }
    }

    public void afterLoadExists(CollectActivity act, @Nullable String value) {
        //lock data if frozen or locked state
        isLocked = act.isFrozen() || act.isLocked();
    }

    public void afterLoadDefault(CollectActivity act) {
        //unlock data only if frozen
        isLocked = act.isLocked();
    }

    public void afterLoadNotExists(CollectActivity act) {
        //unlock data only if frozen
        isLocked = act.isLocked();
    }

    public void onExit() {}

    /**
     * Handles the repeated value view list state.
     * If this feature is enabled, the list will be modified and updated.
     */
    public void deleteTraitListener() {
        CollectInputView inputView = getCollectInputView();
        if (inputView.isRepeatEnabled()) {
            inputView.getRepeatView().userDeleteCurrentRep();
        }
    }

    public abstract void setNaTraitsText();

    public void refreshLock() {
        //((CollectActivity) getContext()).traitLockData();
    }

    public Map<String, String> getNewTraits() {
        return ((CollectActivity) getContext()).getNewTraits();
    }

    public TraitObject getCurrentTrait() {
        return ((CollectActivity) getContext()).getCurrentTrait();
    }

    public SharedPreferences getPrefs() {
        return getContext().getSharedPreferences(GeneralKeys.SHARED_PREF_FILE_NAME, Context.MODE_PRIVATE);
    }

    public CollectActivity getCollectActivity() {
        return (CollectActivity) getContext();
    }

    public RangeObject getCurrentRange() {
        return ((CollectActivity) getContext()).getCRange();
    }

    public CollectInputView getCollectInputView() {
        return ((CollectActivity) getContext()).getCollectInputView();
    }

    public String getDisplayColor() {
        return String.format("#%06X", (0xFFFFFF & getContext().getSharedPreferences(GeneralKeys.SHARED_PREF_FILE_NAME, Context.MODE_PRIVATE)
                .getInt(GeneralKeys.SAVED_DATA_COLOR, resolveThemeColor(R.attr.fb_value_saved_color))));
    }

    public int getButtonTextColor() {
        return resolveThemeColor(R.attr.fb_button_text_color);
    }

    public int getButtonBackgroundColor() {
        return resolveThemeColor(R.attr.fb_button_color_normal);
    }

    public int getButtonPressedColor() {
        return resolveThemeColor(R.attr.fb_trait_categorical_button_press_color);
    }

    public int getTextColor() {
        return resolveThemeColor(R.attr.fb_color_text_dark);
    }

    public int getValueAlteredColor() {
        return resolveThemeColor(R.attr.fb_value_altered_color);
    }

    private int resolveThemeColor(int resid) {
        TypedValue value = new TypedValue();
        getContext().getTheme().resolveAttribute(resid, value, true);
        return value.data;
    }

    /**
     * Function that toggles visibility of the edit text or repeated values view based on preferences
     */
    protected void toggleVisibility(int visibility) {

        CollectInputView inputView = getCollectInputView();

        inputView.setVisibility(visibility);

        RepeatedValuesView repeatView = inputView.getRepeatView();
        EditText editText = inputView.getEditText();

        // Clear hint for NA since a focus change doesn't happen for the numeric trait layout
        if (inputView.isRepeatEnabled()) {
            repeatView.setVisibility(visibility);
        } else {
            editText.setVisibility(visibility);
            editText.setHint("");

            if (isTraitType(TextTraitLayout.type)
                    || isTraitType(AudioTraitLayout.type)
                    || isTraitType(PhotoTraitLayout.type)) {
                editText.setVisibility(View.GONE);
            } else {
                editText.setVisibility(View.VISIBLE);
            }
        }
    }

    /**
     * Calls the collect activities db function to insert an observation row.
     * @param traitName the name of the trait s.a "My Height Trait", "Height" (defined by user / FB)
     * @param traitType the type of trait s.a "Numeric", "Categorical" (defined by FB)
     * @param value the Text value to be saved in the row
     */
    public void updateObservation(String traitName, String traitType, String value) {
        ((CollectActivity) getContext()).updateObservation(traitName, traitType, value, null);
    }

    public void removeTrait(String parent) {
        ((CollectActivity) getContext()).removeTrait(parent);
    }

    public void triggerTts(String text) {
        ((CollectActivity) getContext()).triggerTts(text);
    }

    protected List<ObservationModel> getObservations() {
        CollectActivity act = getCollectActivity();
        return Arrays.asList(getDatabase().getRepeatedValues(act.getStudyId(), act.getObservationUnit(), act.getTraitName()));
    }

    protected ObservationModel getCurrentObservation() {
        String rep = getCollectInputView().getRep();
        List<ObservationModel> models = getObservations();
        for (ObservationModel m : models) {
            if (rep.equals(m.getRep())) {
                return m;
            }
        }
        return null;
    }

    protected DataHelper getDatabase() { return controller.getDatabase(); }
}