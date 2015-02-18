/*
 * Copyright (C) 2013 Peter Gregus for GravityBox Project (C3C076@xda)
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.ceco.lollipop.gravitybox;

import com.ceco.lollipop.gravitybox.managers.StatusBarIconManager;
import com.ceco.lollipop.gravitybox.managers.SysUiManagers;
import com.ceco.lollipop.gravitybox.managers.BatteryInfoManager.BatteryData;
import com.ceco.lollipop.gravitybox.managers.BatteryInfoManager.BatteryStatusListener;
import com.ceco.lollipop.gravitybox.managers.StatusBarIconManager.ColorInfo;
import com.ceco.lollipop.gravitybox.managers.StatusBarIconManager.IconManagerListener;

import de.robv.android.xposed.XSharedPreferences;
import android.animation.Animator;
import android.animation.Animator.AnimatorListener;
import android.animation.ArgbEvaluator;
import android.animation.ValueAnimator;
import android.animation.ValueAnimator.AnimatorUpdateListener;
import android.graphics.Color;
import android.util.TypedValue;
import android.widget.TextView;

public class StatusbarBatteryPercentage implements IconManagerListener, BatteryStatusListener {
    private TextView mPercentage;
    private int mDefaultColor;
    private int mIconColor;
    private String mPercentSign;
    private BatteryData mBatteryData;
    private ValueAnimator mChargeAnim;
    private int mChargingStyle;
    private int mChargingColor;

    public static final int CHARGING_STYLE_NONE = 0;
    public static final int CHARGING_STYLE_STATIC = 1;
    public static final int CHARGING_STYLE_ANIMATED = 2;

    public StatusbarBatteryPercentage(TextView view, XSharedPreferences prefs) {
        mPercentage = view;
        mDefaultColor = mIconColor = mPercentage.getCurrentTextColor();

        initPreferences(prefs);

        if (SysUiManagers.IconManager != null) {
            SysUiManagers.IconManager.registerListener(this);
        }
        if (SysUiManagers.BatteryInfoManager != null) {
            SysUiManagers.BatteryInfoManager.registerListener(this);
        }
    }

    private void initPreferences(XSharedPreferences prefs) {
        setTextSize(Integer.valueOf(prefs.getString(
                GravityBoxSettings.PREF_KEY_BATTERY_PERCENT_TEXT_SIZE, "16")));
        setPercentSign(prefs.getString(
                GravityBoxSettings.PREF_KEY_BATTERY_PERCENT_TEXT_STYLE, "%"));
        setChargingStyle(Integer.valueOf(prefs.getString(
                GravityBoxSettings.PREF_KEY_BATTERY_PERCENT_TEXT_CHARGING, "0")));
        setChargingColor(prefs.getInt(
                GravityBoxSettings.PREF_KEY_BATTERY_PERCENT_TEXT_CHARGING_COLOR, Color.GREEN));
    }

    private boolean startChargingAnimation() {
        if (mChargeAnim == null || !mChargeAnim.isRunning()) {
            mChargeAnim = ValueAnimator.ofObject(new ArgbEvaluator(),
                    mIconColor, mChargingColor);

            mChargeAnim.addUpdateListener(new AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator va) {
                    mPercentage.setTextColor((Integer)va.getAnimatedValue());
                }
            });
            mChargeAnim.addListener(new AnimatorListener() {
                @Override
                public void onAnimationCancel(Animator animation) {
                    mPercentage.setTextColor(mIconColor);
                }

                @Override
                public void onAnimationEnd(Animator animation) {
                    mPercentage.setTextColor(mIconColor);
                }

                @Override
                public void onAnimationRepeat(Animator animation) { }

                @Override
                public void onAnimationStart(Animator animation) { }
            });

            mChargeAnim.setDuration(1000);
            mChargeAnim.setRepeatMode(ValueAnimator.REVERSE);
            mChargeAnim.setRepeatCount(ValueAnimator.INFINITE);
            mChargeAnim.start();
            return true;
        }
        return false;
    }

    private boolean stopChargingAnimation() {
        if (mChargeAnim != null && mChargeAnim.isRunning()) {
            mChargeAnim.end();
            mChargeAnim.removeAllUpdateListeners();
            mChargeAnim.removeAllListeners();
            mChargeAnim = null;
            return true;
        }
        return false;
    }

    public TextView getView() {
        return mPercentage;
    }

    public void setTextColor(int color) {
        mIconColor = color;
        stopChargingAnimation();
        update();
    }

    public void setTextSize(int size) {
        mPercentage.setTextSize(TypedValue.COMPLEX_UNIT_DIP, size);
    }

    public void setPercentSign(String percentSign) {
        mPercentSign = percentSign;
        update();
    }

    public void setChargingStyle(int style) {
        mChargingStyle = style;
        update();
    }

    public void setChargingColor(int color) {
        mChargingColor = color;
        stopChargingAnimation();
        update();
    }

    public void update() {
        if (mBatteryData == null) return;

        mPercentage.setText(mBatteryData.level + mPercentSign);

        if (mBatteryData.charging && mBatteryData.level < 100) {
            if (mChargingStyle == CHARGING_STYLE_STATIC) {
                stopChargingAnimation();
                mPercentage.setTextColor(mChargingColor);
            } else if (mChargingStyle == CHARGING_STYLE_ANIMATED) {
                startChargingAnimation();
            } else {
                stopChargingAnimation();
                mPercentage.setTextColor(mIconColor);
            }
        } else {
            stopChargingAnimation();
            mPercentage.setTextColor(mIconColor);
        }
    }

    public void setVisibility(int visibility) {
        mPercentage.setVisibility(visibility);
    }

    @Override
    public void onIconManagerStatusChanged(int flags, ColorInfo colorInfo) {
        if ((flags & StatusBarIconManager.FLAG_ICON_COLOR_CHANGED) != 0) {
            if (colorInfo.coloringEnabled) {
                setTextColor(colorInfo.iconColor[0]);
            } else {
                setTextColor(mDefaultColor);
            }
        } else if ((flags & StatusBarIconManager.FLAG_ICON_ALPHA_CHANGED) != 0) {
            mPercentage.setAlpha(colorInfo.alphaTextAndBattery);
        }
    }

    @Override
    public void onBatteryStatusChanged(BatteryData batteryData) {
        mBatteryData = batteryData;
        update();
    }
}
