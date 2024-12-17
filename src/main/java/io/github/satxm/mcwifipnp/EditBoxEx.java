package io.github.satxm.mcwifipnp;

import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Predicate;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;

public class EditBoxEx<T> extends EditBox {
  public final static int TEXT_COLOR_HINT = 0x707070;
  public final static int TEXT_COLOR_NORMAL = 0xE0E0E0;
  public final static int TEXT_COLOR_WARN = 0xFFFF55;
  public final static int TEXT_COLOR_ERROR = 0xFF5555;

  protected T defaultValue;
  protected ValidatorResult defaultState;
  protected Function<T, String> valueToStringMap;
  protected Function<String, T> stringToValueMap;
  protected BiConsumer<ValidatorResult, T> responder;

  /**
   * Return null to indicate ok and the tooltip specified in the defaultState will
   * be used.
   * To specify tooltip and text color, return a new custom ValidatorResult().
   */
  protected Function<T, ValidatorResult> validator;
  protected ValidatorResult validatorFailedState;

  public EditBoxEx(Font font, int width, int height, int x, int y, Component name) {
    super(font, width, height, x, y, name);
  }

  public void onTextChanged(String newText) {
    T newValue;
    ValidatorResult newState;

    if (newText.isBlank()) {
      newValue = this.defaultValue;
      newState = this.defaultState;
    } else {
      newValue = null;
      newState = this.validatorFailedState;
      try {
        newValue = this.stringToValueMap.apply(newText);
        if (this.defaultValue.equals(newValue)) {
          newState = this.defaultState;
        } else {
          newState = this.validator.apply(newValue);

          if (newState == null) {
            newState = ValidatorResult.normal(this.defaultState.toolTip());
          }
        }
      } catch (Exception e) {}
    }

    this.setTextColor(newState.textColor());
    this.setTooltip(newState.toolTip());

    this.responder.accept(newState, newState == this.validatorFailedState ? null : newValue);
  }

  @Override
  public void setFocused(boolean newValue) {
    super.setFocused(newValue);

    if (!newValue && this.getValue().isBlank()) {
      this.setValue(this.valueToStringMap.apply(this.defaultValue));
    }
  }

  public static record ValidatorResult(int textColor, Tooltip toolTip, boolean valid, boolean updateBackendValue) {
    public static ValidatorResult normal(Tooltip toolTip) {
      return new ValidatorResult(TEXT_COLOR_NORMAL, toolTip, true, true);
    }
  }

  // Builder functions start
  public static EditBoxEx<Integer> numerical(Font font, int width, int height, int x, int y, Component name) {
    EditBoxEx<Integer> instance = new EditBoxEx<Integer>(font, width, height, x, y, name);
    instance.valueToStringMap = (i) -> Integer.toString(i);
    instance.stringToValueMap = Integer::parseInt;
    return instance;
  }

  public static EditBoxEx<String> text(Font font, int width, int height, int x, int y, Component name) {
    EditBoxEx<String> instance = new EditBoxEx<String>(font, width, height, x, y, name);
    instance.valueToStringMap = Function.identity();
    instance.stringToValueMap = Function.identity();
    return instance;
  }

  public EditBoxEx<T> defaults(T defaultValue, int textColor, Tooltip toolTip) {
    this.defaultValue = defaultValue;
    this.defaultState = new ValidatorResult(textColor, toolTip, true, true);
    this.setValue(this.valueToStringMap.apply(defaultValue));
    this.setTextColor(this.defaultState.textColor());
    this.setTooltip(this.defaultState.toolTip());
    return this;
  }

  public EditBoxEx<T> invalid(int textColor, Tooltip toolTip) {
    this.validatorFailedState = new ValidatorResult(textColor, toolTip, false, false);
    return this;
  }

  public EditBoxEx<T> validator(Function<T, ValidatorResult> validator) {
    this.validator = validator;
    return this;
  }

  /**
   * Use a validator that returns a boolean so that one doesn't need to call
   * defaults(), invalid(), and validator() individually.
   *
   * @param defaultValue
   * @param toolTip this tooltip will always be displayed.
   * @param validator
   * @return
   */
  public EditBoxEx<T> bistate(T defaultValue, Tooltip toolTip, Predicate<T> validator) {
    this.defaults(defaultValue, TEXT_COLOR_HINT, toolTip);
    this.invalid(TEXT_COLOR_ERROR, toolTip);
    this.validator((t) -> {
      return validator.test(t) ? new ValidatorResult(TEXT_COLOR_NORMAL, toolTip, true, true)
          : this.validatorFailedState;
    });
    return this;
  }

  public EditBoxEx<T> maxLength(int len) {
    this.setMaxLength(len);
    return this;
  }

  public EditBoxEx<T> responder(BiConsumer<ValidatorResult, T> responder) {
    this.responder = responder;
    this.setResponder(this::onTextChanged);
    return this;
  }
}
