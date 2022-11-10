/*
 * Copyright (C) 2021 The Android Open Source Project
 *
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

package android.safetycenter;

import static android.os.Build.VERSION_CODES.TIRAMISU;
import static android.os.Build.VERSION_CODES.UPSIDE_DOWN_CAKE;

import static com.android.internal.util.Preconditions.checkArgument;

import static java.util.Collections.unmodifiableList;
import static java.util.Objects.requireNonNull;

import android.annotation.IntDef;
import android.annotation.NonNull;
import android.annotation.Nullable;
import android.annotation.SuppressLint;
import android.annotation.SystemApi;
import android.annotation.TargetApi;
import android.app.PendingIntent;
import android.os.Build;
import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;

import androidx.annotation.RequiresApi;

import com.android.modules.utils.build.SdkLevel;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Data for a safety source issue in the Safety Center page.
 *
 * <p>An issue represents an actionable matter relating to a particular safety source.
 *
 * <p>The safety issue will contain localized messages to be shown in UI explaining the potential
 * threat or warning and suggested fixes, as well as actions a user is allowed to take from the UI
 * to resolve the issue.
 *
 * @hide
 */
@SystemApi
@RequiresApi(TIRAMISU)
public final class SafetySourceIssue implements Parcelable {

    /** Indicates that the risk associated with the issue is related to a user's device safety. */
    public static final int ISSUE_CATEGORY_DEVICE = 100;

    /** Indicates that the risk associated with the issue is related to a user's account safety. */
    public static final int ISSUE_CATEGORY_ACCOUNT = 200;

    /** Indicates that the risk associated with the issue is related to a user's general safety. */
    public static final int ISSUE_CATEGORY_GENERAL = 300;

    /**
     * All possible issue categories.
     *
     * <p>An issue's category represents a specific area of safety that the issue relates to.
     *
     * <p>An issue can only have one associated category. If the issue relates to multiple areas of
     * safety, then choose the closest area or default to {@link #ISSUE_CATEGORY_GENERAL}.
     *
     * @hide
     * @see Builder#setIssueCategory(int)
     */
    @IntDef(
            prefix = {"ISSUE_CATEGORY_"},
            value = {
                ISSUE_CATEGORY_DEVICE,
                ISSUE_CATEGORY_ACCOUNT,
                ISSUE_CATEGORY_GENERAL,
            })
    @Retention(RetentionPolicy.SOURCE)
    public @interface IssueCategory {}

    /** Value signifying that the source has not specified a particular notification behavior. */
    @RequiresApi(UPSIDE_DOWN_CAKE)
    public static final int NOTIFICATION_BEHAVIOR_UNSPECIFIED = 0;

    /** An issue which Safety Center should never notify the user about. */
    @RequiresApi(UPSIDE_DOWN_CAKE)
    public static final int NOTIFICATION_BEHAVIOR_NEVER = 100;

    /**
     * An issue which Safety Center may notify the user about after a delay if it has not been
     * resolved. Safety Center does not provide any guarantee about the duration of the delay.
     */
    @RequiresApi(UPSIDE_DOWN_CAKE)
    public static final int NOTIFICATION_BEHAVIOR_DELAYED = 200;

    /** An issue which Safety Center may notify the user about immediately. */
    @RequiresApi(UPSIDE_DOWN_CAKE)
    public static final int NOTIFICATION_BEHAVIOR_IMMEDIATELY = 300;

    /**
     * All possible notification behaviors.
     *
     * <p>The notification behavior of a {@link SafetySourceIssue} determines if and when Safety
     * Center should notify the user about it.
     *
     * @hide
     * @see Builder#setNotificationBehavior(int)
     */
    @IntDef(
            prefix = {"NOTIFICATION_BEHAVIOR_"},
            value = {
                NOTIFICATION_BEHAVIOR_UNSPECIFIED,
                NOTIFICATION_BEHAVIOR_NEVER,
                NOTIFICATION_BEHAVIOR_DELAYED,
                NOTIFICATION_BEHAVIOR_IMMEDIATELY
            })
    @Retention(RetentionPolicy.SOURCE)
    @TargetApi(UPSIDE_DOWN_CAKE)
    public @interface NotificationBehavior {}

    @NonNull
    public static final Creator<SafetySourceIssue> CREATOR =
            new Creator<SafetySourceIssue>() {
                @Override
                public SafetySourceIssue createFromParcel(Parcel in) {
                    String id = in.readString();
                    CharSequence title = TextUtils.CHAR_SEQUENCE_CREATOR.createFromParcel(in);
                    CharSequence subtitle = TextUtils.CHAR_SEQUENCE_CREATOR.createFromParcel(in);
                    CharSequence summary = TextUtils.CHAR_SEQUENCE_CREATOR.createFromParcel(in);
                    int severityLevel = in.readInt();
                    int issueCategory = in.readInt();
                    List<Action> actions = requireNonNull(in.createTypedArrayList(Action.CREATOR));
                    PendingIntent onDismissPendingIntent =
                            in.readTypedObject(PendingIntent.CREATOR);
                    String issueTypeId = in.readString();
                    Builder builder =
                            new Builder(id, title, summary, severityLevel, issueTypeId)
                                    .setSubtitle(subtitle)
                                    .setIssueCategory(issueCategory)
                                    .setOnDismissPendingIntent(onDismissPendingIntent);
                    for (int i = 0; i < actions.size(); i++) {
                        builder.addAction(actions.get(i));
                    }
                    if (SdkLevel.isAtLeastU()) {
                        builder.setCustomNotification(in.readTypedObject(Notification.CREATOR));
                        builder.setNotificationBehavior(in.readInt());
                        builder.setAttributionTitle(
                                TextUtils.CHAR_SEQUENCE_CREATOR.createFromParcel(in));
                        builder.setDeduplicationId(in.readString());
                    }
                    return builder.build();
                }

                @Override
                public SafetySourceIssue[] newArray(int size) {
                    return new SafetySourceIssue[size];
                }
            };

    @NonNull private final String mId;
    @NonNull private final CharSequence mTitle;
    @Nullable private final CharSequence mSubtitle;
    @NonNull private final CharSequence mSummary;
    @SafetySourceData.SeverityLevel private final int mSeverityLevel;
    private final List<Action> mActions;
    @Nullable private final PendingIntent mOnDismissPendingIntent;
    @IssueCategory private final int mIssueCategory;
    @NonNull private final String mIssueTypeId;
    @Nullable private final Notification mCustomNotification;
    @NotificationBehavior private final int mNotificationBehavior;
    @Nullable private final CharSequence mAttributionTitle;
    @Nullable private final String mDeduplicationId;

    private SafetySourceIssue(
            @NonNull String id,
            @NonNull CharSequence title,
            @Nullable CharSequence subtitle,
            @NonNull CharSequence summary,
            @SafetySourceData.SeverityLevel int severityLevel,
            @IssueCategory int issueCategory,
            @NonNull List<Action> actions,
            @Nullable PendingIntent onDismissPendingIntent,
            @NonNull String issueTypeId,
            @Nullable Notification customNotification,
            @NotificationBehavior int notificationBehavior,
            @Nullable CharSequence attributionTitle,
            @Nullable String deduplicationId) {
        this.mId = id;
        this.mTitle = title;
        this.mSubtitle = subtitle;
        this.mSummary = summary;
        this.mSeverityLevel = severityLevel;
        this.mIssueCategory = issueCategory;
        this.mActions = actions;
        this.mOnDismissPendingIntent = onDismissPendingIntent;
        this.mIssueTypeId = issueTypeId;
        this.mCustomNotification = customNotification;
        this.mNotificationBehavior = notificationBehavior;
        this.mAttributionTitle = attributionTitle;
        this.mDeduplicationId = deduplicationId;
    }

    /**
     * Returns the identifier for this issue.
     *
     * <p>This id should uniquely identify the safety risk represented by this issue. Safety issues
     * will be deduped by this id to be shown in the UI.
     *
     * <p>On multiple instances of providing the same issue to be represented in Safety Center,
     * provide the same id across all instances.
     */
    @NonNull
    public String getId() {
        return mId;
    }

    /** Returns the localized title of the issue to be displayed in the UI. */
    @NonNull
    public CharSequence getTitle() {
        return mTitle;
    }

    /** Returns the localized subtitle of the issue to be displayed in the UI. */
    @Nullable
    public CharSequence getSubtitle() {
        return mSubtitle;
    }

    /** Returns the localized summary of the issue to be displayed in the UI. */
    @NonNull
    public CharSequence getSummary() {
        return mSummary;
    }

    /**
     * Returns the localized attribution title of the issue to be displayed in the UI.
     *
     * <p>This is displayed in the UI and helps to attribute issue cards to a particular source. If
     * this value is {@code null}, the title of the group that contains the Safety Source will be
     * used.
     */
    @Nullable
    @RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    public CharSequence getAttributionTitle() {
        if (!SdkLevel.isAtLeastU()) {
            throw new UnsupportedOperationException();
        }
        return mAttributionTitle;
    }

    /** Returns the {@link SafetySourceData.SeverityLevel} of the issue. */
    @SafetySourceData.SeverityLevel
    public int getSeverityLevel() {
        return mSeverityLevel;
    }

    /**
     * Returns the category of the risk associated with the issue.
     *
     * <p>The default category will be {@link #ISSUE_CATEGORY_GENERAL}.
     */
    @IssueCategory
    public int getIssueCategory() {
        return mIssueCategory;
    }

    /**
     * Returns a list of {@link Action}s representing actions supported in the UI for this issue.
     *
     * <p>Each issue must contain at least one action, in order to help the user resolve the issue.
     *
     * <p>In Android {@link android.os.Build.VERSION_CODES#TIRAMISU}, each issue can contain at most
     * two actions supported from the UI.
     */
    @NonNull
    public List<Action> getActions() {
        return mActions;
    }

    /**
     * Returns the optional {@link PendingIntent} that will be invoked when an issue is dismissed.
     *
     * <p>When a safety issue is dismissed in Safety Center page, the issue is removed from view in
     * Safety Center page. This method returns an additional optional action specified by the safety
     * source that should be invoked on issue dismissal. The action contained in the {@link
     * PendingIntent} cannot start an activity.
     */
    @Nullable
    public PendingIntent getOnDismissPendingIntent() {
        return mOnDismissPendingIntent;
    }

    /**
     * Returns the identifier for the type of this issue.
     *
     * <p>The issue type should indicate the underlying basis for the issue, for e.g. a pending
     * update or a disabled security feature.
     *
     * <p>The difference between this id and {@link #getId()} is that the issue type id is meant to
     * be used for logging and should therefore contain no personally identifiable information (PII)
     * (e.g. for account name).
     *
     * <p>On multiple instances of providing the same issue to be represented in Safety Center,
     * provide the same issue type id across all instances.
     */
    @NonNull
    public String getIssueTypeId() {
        return mIssueTypeId;
    }

    /**
     * Returns the optional custom {@link Notification} for this issue which overrides the title,
     * text and actions for any {@link android.app.Notification} generated for this {@link
     * SafetySourceIssue}.
     *
     * <p>Safety Center may still generate a default notification from the other details of this
     * issue when no custom notification has been set. See {@link #getNotificationBehavior()} for
     * details
     *
     * @see Builder#setCustomNotification(android.safetycenter.SafetySourceIssue.Notification
     * @see #getNotificationBehavior()
     */
    @Nullable
    @RequiresApi(UPSIDE_DOWN_CAKE)
    public Notification getCustomNotification() {
        if (!SdkLevel.isAtLeastU()) {
            throw new UnsupportedOperationException();
        }
        return mCustomNotification;
    }

    /**
     * Returns the {@link NotificationBehavior} for this issue which determines if and when Safety
     * Center will post a notification for this issue.
     *
     * <p>Any notification will be based on the {@link #getCustomNotification()} if set, or the
     * other properties of this issue otherwise.
     *
     * <ul>
     *   <li>If {@link #NOTIFICATION_BEHAVIOR_IMMEDIATELY} then Safety Center will immediately
     *       create and post a notification
     *   <li>If {@link #NOTIFICATION_BEHAVIOR_DELAYED} then a notification will only be posted after
     *       a delay, if this issue has not been resolved.
     *   <li>If {@link #NOTIFICATION_BEHAVIOR_UNSPECIFIED} then a notification may or may not be
     *       posted, the exact behavior is defined by Safety Center.
     *   <li>If {@link #NOTIFICATION_BEHAVIOR_NEVER} Safety Center will never post a notification
     *       about this issue. Sources should specify this behavior when they wish to handle their
     *       own notifications. When this behavior is set sources should not set a custom
     *       notification.
     * </ul>
     *
     * @see Builder#setNotificationBehavior(int)
     */
    @NotificationBehavior
    @RequiresApi(UPSIDE_DOWN_CAKE)
    public int getNotificationBehavior() {
        if (!SdkLevel.isAtLeastU()) {
            throw new UnsupportedOperationException();
        }
        return mNotificationBehavior;
    }

    /**
     * Returns the identifier used to deduplicate this issue against other issues with the same
     * deduplication identifiers.
     *
     * <p>Deduplication identifier will be used to identify duplicate issues. This identifier
     * applies across all safety sources which are part of the same deduplication group.
     * Deduplication groups can be set, for each source, in the SafetyCenter config. Therefore, two
     * issues are considered duplicate if their sources are part of the same deduplication group and
     * they have the same deduplication identifier.
     *
     * <p>Out of all issues that are found to be duplicates, only one will be shown in the UI (the
     * one with the highest severity, or in case of same severities, the one placed highest in the
     * config).
     *
     * <p>Expected usage implies different sources will coordinate to set the same deduplication
     * identifiers on issues that they want to deduplicate.
     *
     * <p>This shouldn't be a default mechanism for deduplication of issues. Most of the time
     * sources should coordinate or communicate to only send the issue from one of them. That would
     * also allow sources to choose which one will be displaying the issue, instead of depending on
     * severity and config order. This API should only be needed if for some reason this isn't
     * possible, for example, when sources can't communicate with each other and/or send issues at
     * different times and/or issues can be of different severities.
     */
    @Nullable
    @RequiresApi(UPSIDE_DOWN_CAKE)
    public String getDeduplicationId() {
        if (!SdkLevel.isAtLeastU()) {
            throw new UnsupportedOperationException();
        }
        return mDeduplicationId;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        dest.writeString(mId);
        TextUtils.writeToParcel(mTitle, dest, flags);
        TextUtils.writeToParcel(mSubtitle, dest, flags);
        TextUtils.writeToParcel(mSummary, dest, flags);
        dest.writeInt(mSeverityLevel);
        dest.writeInt(mIssueCategory);
        dest.writeTypedList(mActions);
        dest.writeTypedObject(mOnDismissPendingIntent, flags);
        dest.writeString(mIssueTypeId);
        if (SdkLevel.isAtLeastU()) {
            dest.writeTypedObject(mCustomNotification, flags);
            dest.writeInt(mNotificationBehavior);
            TextUtils.writeToParcel(mAttributionTitle, dest, flags);
            dest.writeString(mDeduplicationId);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SafetySourceIssue)) return false;
        SafetySourceIssue that = (SafetySourceIssue) o;
        return mSeverityLevel == that.mSeverityLevel
                && TextUtils.equals(mId, that.mId)
                && TextUtils.equals(mTitle, that.mTitle)
                && TextUtils.equals(mSubtitle, that.mSubtitle)
                && TextUtils.equals(mSummary, that.mSummary)
                && mIssueCategory == that.mIssueCategory
                && mActions.equals(that.mActions)
                && Objects.equals(mOnDismissPendingIntent, that.mOnDismissPendingIntent)
                && TextUtils.equals(mIssueTypeId, that.mIssueTypeId)
                && Objects.equals(mCustomNotification, that.mCustomNotification)
                && mNotificationBehavior == that.mNotificationBehavior
                && TextUtils.equals(mAttributionTitle, that.mAttributionTitle)
                && TextUtils.equals(mDeduplicationId, that.mDeduplicationId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                mId,
                mTitle,
                mSubtitle,
                mSummary,
                mSeverityLevel,
                mIssueCategory,
                mActions,
                mOnDismissPendingIntent,
                mIssueTypeId,
                mCustomNotification,
                mNotificationBehavior,
                mAttributionTitle,
                mDeduplicationId);
    }

    @Override
    public String toString() {
        return "SafetySourceIssue{"
                + "mId="
                + mId
                + "mTitle="
                + mTitle
                + ", mSubtitle="
                + mSubtitle
                + ", mSummary="
                + mSummary
                + ", mSeverityLevel="
                + mSeverityLevel
                + ", mIssueCategory="
                + mIssueCategory
                + ", mActions="
                + mActions
                + ", mOnDismissPendingIntent="
                + mOnDismissPendingIntent
                + ", mIssueTypeId="
                + mIssueTypeId
                + ", mCustomNotification="
                + mCustomNotification
                + ", mNotificationBehavior="
                + mNotificationBehavior
                + ", mAttributionTitle="
                + mAttributionTitle
                + ", mDeduplicationId="
                + mDeduplicationId
                + '}';
    }

    /**
     * Data for an action supported from a safety issue {@link SafetySourceIssue} in the Safety
     * Center page.
     *
     * <p>The purpose of the action is to allow the user to address the safety issue, either by
     * performing a fix suggested in the issue, or by navigating the user to the source of the issue
     * where they can be exposed to detail about the issue and further suggestions to resolve it.
     *
     * <p>The user will be allowed to invoke the action from the UI by clicking on a UI element and
     * consequently resolve the issue.
     */
    public static final class Action implements Parcelable {

        @NonNull
        public static final Creator<Action> CREATOR =
                new Creator<Action>() {
                    @Override
                    public Action createFromParcel(Parcel in) {
                        String id = in.readString();
                        CharSequence label = TextUtils.CHAR_SEQUENCE_CREATOR.createFromParcel(in);
                        PendingIntent pendingIntent = in.readTypedObject(PendingIntent.CREATOR);
                        return new Builder(id, label, pendingIntent)
                                .setWillResolve(in.readBoolean())
                                .setSuccessMessage(
                                        TextUtils.CHAR_SEQUENCE_CREATOR.createFromParcel(in))
                                .build();
                    }

                    @Override
                    public Action[] newArray(int size) {
                        return new Action[size];
                    }
                };

        private static void enforceUniqueActionIds(
                @NonNull List<SafetySourceIssue.Action> actions, @NonNull String message) {
            Set<String> actionIds = new HashSet<>();
            for (int i = 0; i < actions.size(); i++) {
                SafetySourceIssue.Action action = actions.get(i);

                String actionId = action.getId();
                checkArgument(!actionIds.contains(actionId), message);
                actionIds.add(actionId);
            }
        }

        @NonNull private final String mId;
        @NonNull private final CharSequence mLabel;
        @NonNull private final PendingIntent mPendingIntent;
        private final boolean mWillResolve;
        @Nullable private final CharSequence mSuccessMessage;

        private Action(
                @NonNull String id,
                @NonNull CharSequence label,
                @NonNull PendingIntent pendingIntent,
                boolean willResolve,
                @Nullable CharSequence successMessage) {
            mId = id;
            mLabel = label;
            mPendingIntent = pendingIntent;
            mWillResolve = willResolve;
            mSuccessMessage = successMessage;
        }

        /**
         * Returns the ID of the action, unique among actions in a given {@link SafetySourceIssue}.
         */
        @NonNull
        public String getId() {
            return mId;
        }

        /**
         * Returns the localized label of the action to be displayed in the UI.
         *
         * <p>The label should indicate what action will be performed if when invoked.
         */
        @NonNull
        public CharSequence getLabel() {
            return mLabel;
        }

        /**
         * Returns a {@link PendingIntent} to be fired when the action is clicked on.
         *
         * <p>The {@link PendingIntent} should perform the action referred to by {@link
         * #getLabel()}.
         */
        @NonNull
        public PendingIntent getPendingIntent() {
            return mPendingIntent;
        }

        /**
         * Returns whether invoking this action will fix or address the issue sufficiently for it to
         * be considered resolved i.e. the issue will no longer need to be conveyed to the user in
         * the UI.
         */
        public boolean willResolve() {
            return mWillResolve;
        }

        /**
         * Returns the optional localized message to be displayed in the UI when the action is
         * invoked and completes successfully.
         */
        @Nullable
        public CharSequence getSuccessMessage() {
            return mSuccessMessage;
        }

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(@NonNull Parcel dest, int flags) {
            dest.writeString(mId);
            TextUtils.writeToParcel(mLabel, dest, flags);
            dest.writeTypedObject(mPendingIntent, flags);
            dest.writeBoolean(mWillResolve);
            TextUtils.writeToParcel(mSuccessMessage, dest, flags);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Action)) return false;
            Action that = (Action) o;
            return mId.equals(that.mId)
                    && TextUtils.equals(mLabel, that.mLabel)
                    && mPendingIntent.equals(that.mPendingIntent)
                    && mWillResolve == that.mWillResolve
                    && TextUtils.equals(mSuccessMessage, that.mSuccessMessage);
        }

        @Override
        public int hashCode() {
            return Objects.hash(mId, mLabel, mPendingIntent, mWillResolve, mSuccessMessage);
        }

        @Override
        public String toString() {
            return "Action{"
                    + "mId="
                    + mId
                    + ", mLabel="
                    + mLabel
                    + ", mPendingIntent="
                    + mPendingIntent
                    + ", mWillResolve="
                    + mWillResolve
                    + ", mSuccessMessage="
                    + mSuccessMessage
                    + '}';
        }

        /** Builder class for {@link Action}. */
        public static final class Builder {

            @NonNull private final String mId;
            @NonNull private final CharSequence mLabel;
            @NonNull private final PendingIntent mPendingIntent;
            private boolean mWillResolve = false;
            @Nullable private CharSequence mSuccessMessage;

            /** Creates a {@link Builder} for an {@link Action}. */
            public Builder(
                    @NonNull String id,
                    @NonNull CharSequence label,
                    @NonNull PendingIntent pendingIntent) {
                mId = requireNonNull(id);
                mLabel = requireNonNull(label);
                mPendingIntent = requireNonNull(pendingIntent);
            }

            /**
             * Sets whether the action will resolve the safety issue. Defaults to {@code false}.
             *
             * @see #willResolve()
             */
            @SuppressLint("MissingGetterMatchingBuilder")
            @NonNull
            public Builder setWillResolve(boolean willResolve) {
                mWillResolve = willResolve;
                return this;
            }

            /**
             * Sets the optional localized message to be displayed in the UI when the action is
             * invoked and completes successfully.
             */
            @NonNull
            public Builder setSuccessMessage(@Nullable CharSequence successMessage) {
                mSuccessMessage = successMessage;
                return this;
            }

            /** Creates the {@link Action} defined by this {@link Builder}. */
            @NonNull
            public Action build() {
                return new Action(mId, mLabel, mPendingIntent, mWillResolve, mSuccessMessage);
            }
        }
    }

    /**
     * Data for Safety Center to use when constructing a system {@link android.app.Notification}
     * about a related {@link SafetySourceIssue}.
     *
     * <p>Safety Center can construct a default notification for any issue, but sources may use
     * {@link Builder#setCustomNotification(android.safetycenter.SafetySourceIssue.Notification)} if
     * they want to override the title, text or actions.
     *
     * @see #getCustomNotification()
     * @see Builder#setCustomNotification(android.safetycenter.SafetySourceIssue.Notification)
     * @see #getNotificationBehavior()
     */
    @RequiresApi(UPSIDE_DOWN_CAKE)
    public static final class Notification implements Parcelable {

        @NonNull
        public static final Creator<Notification> CREATOR =
                new Creator<Notification>() {
                    @Override
                    public Notification createFromParcel(Parcel in) {
                        return new Builder(
                                        TextUtils.CHAR_SEQUENCE_CREATOR.createFromParcel(in),
                                        TextUtils.CHAR_SEQUENCE_CREATOR.createFromParcel(in))
                                .setActions(in.createTypedArrayList(Action.CREATOR))
                                .build();
                    }

                    @Override
                    public Notification[] newArray(int size) {
                        return new Notification[size];
                    }
                };

        @NonNull private final CharSequence mTitle;
        @NonNull private final CharSequence mText;
        @NonNull private final List<Action> mActions;

        private Notification(
                @NonNull CharSequence title,
                @NonNull CharSequence text,
                @NonNull List<Action> actions) {
            mTitle = title;
            mText = text;
            mActions = actions;
        }

        /**
         * Custom title which will be used instead of {@link SafetySourceIssue#getTitle()} when
         * building a {@link android.app.Notification} for this issue.
         */
        @NonNull
        public CharSequence getTitle() {
            return mTitle;
        }

        /**
         * Custom text which will be used instead of {@link SafetySourceIssue#getSummary()} when
         * building a {@link android.app.Notification} for this issue.
         */
        @NonNull
        public CharSequence getText() {
            return mText;
        }

        /**
         * Custom list of {@link Action} instances which will be used instead of {@link
         * SafetySourceIssue#getActions()} when building a {@link android.app.Notification} for this
         * issue.
         *
         * <p>If this list is empty then the resulting {@link android.app.Notification} will have
         * zero action buttons.
         */
        @NonNull
        public List<Action> getActions() {
            return mActions;
        }

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(@NonNull Parcel dest, int flags) {
            TextUtils.writeToParcel(mTitle, dest, flags);
            TextUtils.writeToParcel(mText, dest, flags);
            dest.writeTypedList(mActions);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Notification)) return false;
            Notification that = (Notification) o;
            return TextUtils.equals(mTitle, that.mTitle)
                    && TextUtils.equals(mText, that.mText)
                    && mActions.equals(that.mActions);
        }

        @Override
        public int hashCode() {
            return Objects.hash(mTitle, mText, mActions);
        }

        @Override
        public String toString() {
            return "Notification{"
                    + "mTitle="
                    + mTitle
                    + ", mText="
                    + mText
                    + ", mActions="
                    + mActions
                    + '}';
        }

        /** Builder for {@link SafetySourceIssue.Notification}. */
        public static final class Builder {

            @NonNull private final CharSequence mTitle;
            @NonNull private final CharSequence mText;
            @NonNull private final List<Action> mActions = new ArrayList<>();

            public Builder(@NonNull CharSequence title, @NonNull CharSequence text) {
                mTitle = requireNonNull(title);
                mText = requireNonNull(text);
            }

            /** Adds an {@link Action} to be show on the custom {@link Notification}. */
            @NonNull
            public Builder addAction(@NonNull Action action) {
                mActions.add(requireNonNull(action));
                return this;
            }

            /**
             * Sets the list of {@link Action}s to be show on the custom {@link Notification},
             * removing any which were previously added.
             */
            @NonNull
            public Builder setActions(@NonNull List<Action> actions) {
                mActions.clear();
                mActions.addAll(requireNonNull(actions));
                return this;
            }

            /**
             * Clears all the {@link Action}s that were added to this custom {@link
             * Notification.Builder}.
             */
            @NonNull
            public Builder clearActions() {
                mActions.clear();
                return this;
            }

            /** Builds a {@link Notification} instance. */
            @NonNull
            public Notification build() {
                List<Action> actions = unmodifiableList(new ArrayList<>(mActions));
                Action.enforceUniqueActionIds(
                        actions, "Custom notification cannot have duplicate action ids");
                checkArgument(
                        actions.size() <= 2,
                        "Custom notification must not contain more than 2 actions");
                return new Notification(mTitle, mText, actions);
            }
        }
    }

    /** Builder class for {@link SafetySourceIssue}. */
    public static final class Builder {

        @NonNull private final String mId;
        @NonNull private final CharSequence mTitle;
        @NonNull private final CharSequence mSummary;
        @SafetySourceData.SeverityLevel private final int mSeverityLevel;
        @NonNull private final String mIssueTypeId;
        private final List<Action> mActions = new ArrayList<>();

        @Nullable private CharSequence mSubtitle;
        @IssueCategory private int mIssueCategory = ISSUE_CATEGORY_GENERAL;
        @Nullable private PendingIntent mOnDismissPendingIntent;
        @Nullable private CharSequence mAttributionTitle;
        @Nullable private String mDeduplicationId;

        @Nullable private Notification mCustomNotification = null;

        @SuppressLint("NewApi")
        @NotificationBehavior
        private int mNotificationBehavior = NOTIFICATION_BEHAVIOR_UNSPECIFIED;

        /** Creates a {@link Builder} for a {@link SafetySourceIssue}. */
        public Builder(
                @NonNull String id,
                @NonNull CharSequence title,
                @NonNull CharSequence summary,
                @SafetySourceData.SeverityLevel int severityLevel,
                @NonNull String issueTypeId) {
            this.mId = requireNonNull(id);
            this.mTitle = requireNonNull(title);
            this.mSummary = requireNonNull(summary);
            this.mSeverityLevel = validateSeverityLevel(severityLevel);
            this.mIssueTypeId = requireNonNull(issueTypeId);
        }

        /** Sets the localized subtitle. */
        @NonNull
        public Builder setSubtitle(@Nullable CharSequence subtitle) {
            mSubtitle = subtitle;
            return this;
        }

        /**
         * Sets or clears the optional attribution title for this issue.
         *
         * <p>This is displayed in the UI and helps to attribute an issue to a particular source. If
         * this value is {@code null}, the title of the group that contains the Safety Source will
         * be used.
         */
        @NonNull
        @RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
        public Builder setAttributionTitle(@Nullable CharSequence attributionTitle) {
            if (!SdkLevel.isAtLeastU()) {
                throw new UnsupportedOperationException();
            }
            mAttributionTitle = attributionTitle;
            return this;
        }

        /**
         * Sets the category of the risk associated with the issue.
         *
         * <p>The default category will be {@link #ISSUE_CATEGORY_GENERAL}.
         */
        @NonNull
        public Builder setIssueCategory(@IssueCategory int issueCategory) {
            mIssueCategory = validateIssueCategory(issueCategory);
            return this;
        }

        /** Adds data for an {@link Action} to be shown in UI. */
        @NonNull
        public Builder addAction(@NonNull Action actionData) {
            mActions.add(requireNonNull(actionData));
            return this;
        }

        /** Clears data for all the {@link Action}s that were added to this {@link Builder}. */
        @NonNull
        public Builder clearActions() {
            mActions.clear();
            return this;
        }

        /**
         * Sets an optional {@link PendingIntent} to be invoked when an issue is dismissed from the
         * UI.
         *
         * <p>In particular, if the source would like to be notified of issue dismissals in Safety
         * Center in order to be able to dismiss or ignore issues at the source, then set this
         * field. The action contained in the {@link PendingIntent} must not start an activity.
         *
         * @see #getOnDismissPendingIntent()
         */
        @NonNull
        public Builder setOnDismissPendingIntent(@Nullable PendingIntent onDismissPendingIntent) {
            checkArgument(
                    onDismissPendingIntent == null || !onDismissPendingIntent.isActivity(),
                    "Safety source issue on dismiss pending intent must not start an activity");
            mOnDismissPendingIntent = onDismissPendingIntent;
            return this;
        }

        /**
         * Sets a custom {@link Notification} for this issue.
         *
         * <p>Using a custom {@link Notification} a source may specify a different {@link
         * Notification#getTitle()}, {@link Notification#getText()} and {@link
         * Notification#getActions()} for Safety Center to use when constructing a notification for
         * this issue.
         *
         * <p>Safety Center may still generate a default notification from the other details of this
         * issue when no custom notification has been set, depending on the issue's {@link
         * #getNotificationBehavior()}.
         *
         * @see #getCustomNotification()
         * @see #setNotificationBehavior(int)
         */
        @NonNull
        @RequiresApi(UPSIDE_DOWN_CAKE)
        public Builder setCustomNotification(@Nullable Notification customNotification) {
            if (!SdkLevel.isAtLeastU()) {
                throw new UnsupportedOperationException();
            }
            mCustomNotification = customNotification;
            return this;
        }

        /**
         * Sets the notification behavior of the issue.
         *
         * <p>Must be one of {@link #NOTIFICATION_BEHAVIOR_UNSPECIFIED}, {@link
         * #NOTIFICATION_BEHAVIOR_NEVER}, {@link #NOTIFICATION_BEHAVIOR_DELAYED} or {@link
         * #NOTIFICATION_BEHAVIOR_IMMEDIATELY}. See {@link #getNotificationBehavior()} for details
         * of how Safety Center will interpret each of these.
         *
         * @see #getNotificationBehavior()
         */
        @NonNull
        @RequiresApi(UPSIDE_DOWN_CAKE)
        public Builder setNotificationBehavior(@NotificationBehavior int notificationBehavior) {
            if (!SdkLevel.isAtLeastU()) {
                throw new UnsupportedOperationException();
            }
            mNotificationBehavior = validateNotificationBehavior(notificationBehavior);
            return this;
        }

        /**
         * Sets the deduplication identifier for the issue.
         *
         * @see #getDeduplicationId()
         */
        @NonNull
        @RequiresApi(UPSIDE_DOWN_CAKE)
        public Builder setDeduplicationId(@Nullable String deduplicationId) {
            if (!SdkLevel.isAtLeastU()) {
                throw new UnsupportedOperationException();
            }
            mDeduplicationId = deduplicationId;
            return this;
        }

        /** Creates the {@link SafetySourceIssue} defined by this {@link Builder}. */
        @NonNull
        public SafetySourceIssue build() {
            List<SafetySourceIssue.Action> actions = unmodifiableList(new ArrayList<>(mActions));
            Action.enforceUniqueActionIds(
                    actions, "Safety source issue cannot have duplicate action ids");
            checkArgument(!actions.isEmpty(), "Safety source issue must contain at least 1 action");
            checkArgument(
                    actions.size() <= 2,
                    "Safety source issue must not contain more than 2 actions");
            return new SafetySourceIssue(
                    mId,
                    mTitle,
                    mSubtitle,
                    mSummary,
                    mSeverityLevel,
                    mIssueCategory,
                    actions,
                    mOnDismissPendingIntent,
                    mIssueTypeId,
                    mCustomNotification,
                    mNotificationBehavior,
                    mAttributionTitle,
                    mDeduplicationId);
        }
    }

    @SafetySourceData.SeverityLevel
    private static int validateSeverityLevel(int value) {
        switch (value) {
            case SafetySourceData.SEVERITY_LEVEL_INFORMATION:
            case SafetySourceData.SEVERITY_LEVEL_RECOMMENDATION:
            case SafetySourceData.SEVERITY_LEVEL_CRITICAL_WARNING:
                return value;
            case SafetySourceData.SEVERITY_LEVEL_UNSPECIFIED:
                throw new IllegalArgumentException(
                        "SeverityLevel for SafetySourceIssue must not be "
                                + "SEVERITY_LEVEL_UNSPECIFIED");
            default:
        }
        throw new IllegalArgumentException(
                "Unexpected SeverityLevel for SafetySourceIssue: " + value);
    }

    @IssueCategory
    private static int validateIssueCategory(int value) {
        switch (value) {
            case ISSUE_CATEGORY_DEVICE:
            case ISSUE_CATEGORY_ACCOUNT:
            case ISSUE_CATEGORY_GENERAL:
                return value;
            default:
        }
        throw new IllegalArgumentException(
                "Unexpected IssueCategory for SafetySourceIssue: " + value);
    }

    @NotificationBehavior
    private static int validateNotificationBehavior(int value) {
        switch (value) {
            case NOTIFICATION_BEHAVIOR_UNSPECIFIED:
            case NOTIFICATION_BEHAVIOR_NEVER:
            case NOTIFICATION_BEHAVIOR_DELAYED:
            case NOTIFICATION_BEHAVIOR_IMMEDIATELY:
                return value;
            default:
        }
        throw new IllegalArgumentException(
                "Unexpected NotificationBehavior for SafetySourceIssue: " + value);
    }
}
