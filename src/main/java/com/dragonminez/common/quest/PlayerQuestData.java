package com.dragonminez.common.quest;

import lombok.Getter;
import lombok.Setter;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Per-player quest progress for all quest types (saga, sidequest, daily, event).
 * <p>
 * All quests are keyed by their string ID. Saga quests use the composite key
 * {@code "sagaId:numericId"} (e.g. {@code "saiyan_saga:1"}). Side-quests use their
 * natural string ID (e.g. {@code "roshi_basic_training"}).
 * Serialized to/from NBT under the {@code "PlayerQuestData"} key in the player's stats compound.
 *
 * @since 2.0
 * @see QuestRegistry
 */
public class PlayerQuestData {

    // ========================================================================================
    // Progress Status
    // ========================================================================================

    /**
     * The status of a quest for a given player.
     * Can return NOT_STARTED (default), ACCEPTED (in progress), or COMPLETED.
     */
    public enum QuestStatus {
        /** Quest has not been started. */
        NOT_STARTED,
        /** Quest has been accepted and is in progress. */
        ACCEPTED,
        /** Quest has been completed. */
        COMPLETED
    }

    // ========================================================================================
    // Internal State
    // ========================================================================================

    /** All quest progress, keyed by string quest ID. */
    private final Map<String, QuestProgress> quests = new LinkedHashMap<>();

    /** Tracks which sagas the player has unlocked */
    private final Map<String, Boolean> sagaUnlockState = new HashMap<>();

    /** Tracks selected branch path by key "sagaId|branchGroup". */
    private final Map<String, String> branchSelections = new HashMap<>();

    /** Per-quest anchors used by elapsed start requirements. */
    private final Map<String, QuestStartRequirementTiming> startRequirementTimings = new LinkedHashMap<>();

    /** Current tracked quest key (saga:questId or sidequestId) shown in client HUD. */
    @Getter
    private String trackedQuestId = null;

    /** True once the player has seen the first-time "Press V" story prompt. */
    @Setter
    @Getter
    private boolean introPromptShown = false;

    /** Active party identifier for synchronized story progress. */
    @Getter
    private UUID activePartyId = null;

    /** Leader that owns the current synchronized quest state. */
    @Getter
    private UUID partyLeaderId = null;

    /** Snapshot of the current online party composition for the local player UI. */
    private final List<UUID> partyMemberIds = new ArrayList<>();

    /** Pending invitation shown in the quest screen. */
    @Setter
    private PartyInviteData pendingPartyInvite = null;

    /** Personal quest state to restore when leaving a synchronized party. */
    private CompoundTag partyQuestBackup = null;

    // ========================================================================================
    // Quest Progress - Accept / Complete / Reset
    // ========================================================================================

    /**
     * Accepts a quest, marking it as in-progress.
     *
     * @param questId the string quest ID
     */
    public void acceptQuest(String questId) {
        getOrCreateProgress(questId).setStatus(QuestStatus.ACCEPTED);
        clearStartRequirementTiming(questId);
    }

    /**
     * Completes a quest, marking it as finished.
     *
     * @param questId the string quest ID
     */
    public void completeQuest(String questId) {
        getOrCreateProgress(questId).setStatus(QuestStatus.COMPLETED);
        clearStartRequirementTiming(questId);
    }

    /**
     * Returns whether the given quest has been accepted (in progress or completed).
     */
    public boolean isQuestAccepted(String questId) {
        QuestProgress progress = quests.get(questId);
        return progress != null && progress.getStatus() != QuestStatus.NOT_STARTED;
    }

    /**
     * Returns whether the given quest has been completed.
     */
    public boolean isQuestCompleted(String questId) {
        QuestProgress progress = quests.get(questId);
        return progress != null && progress.getStatus() == QuestStatus.COMPLETED;
    }

    /**
     * Returns the status of a given quest.
     */
    public QuestStatus getQuestStatus(String questId) {
        QuestProgress progress = quests.get(questId);
        return progress != null ? progress.getStatus() : QuestStatus.NOT_STARTED;
    }

    /**
     * Resets all progress for a given quest.
     */
    public void resetQuest(String questId) {
        quests.remove(questId);
        clearStartRequirementTiming(questId);
    }

    /**
     * Resets all quest progress.
     */
    public void resetAll() {
        quests.clear();
        sagaUnlockState.clear();
        branchSelections.clear();
        startRequirementTimings.clear();
        trackedQuestId = null;
    }

    /**
     * Resets all quest progress for quests belonging to the given saga (keys starting with "sagaId:").
     */
    public void resetSaga(String sagaId) {
        String prefix = sagaId + ":";
        quests.keySet().removeIf(key -> key.startsWith(prefix));
        startRequirementTimings.keySet().removeIf(key -> key.startsWith(prefix));
        if (trackedQuestId != null && trackedQuestId.startsWith(prefix)) trackedQuestId = null;
        String branchPrefix = sagaId + "|";
        branchSelections.keySet().removeIf(key -> key.startsWith(branchPrefix));
    }

    public void setTrackedQuestId(String trackedQuestId) {
        if (trackedQuestId == null || trackedQuestId.isBlank()) {
            this.trackedQuestId = null;
            return;
        }
        this.trackedQuestId = trackedQuestId;
    }

    public QuestStartRequirementTiming getStartRequirementTiming(String questId) {
        return questId == null ? null : startRequirementTimings.get(questId);
    }

    public boolean ensureStartRequirementTiming(String questId, long gameTimeStarted, long realTimeStartedMs) {
        if (questId == null || questId.isBlank()) return false;
        if (startRequirementTimings.containsKey(questId)) return false;
        startRequirementTimings.put(questId, new QuestStartRequirementTiming(gameTimeStarted, realTimeStartedMs));
        return true;
    }

    public void clearStartRequirementTiming(String questId) {
        if (questId == null || questId.isBlank()) return;
        startRequirementTimings.remove(questId);
    }

    /**
     * Returns the set of all quest IDs that have been accepted (in progress).
     */
    public Set<String> getAcceptedQuestIds() {
        Set<String> accepted = new LinkedHashSet<>();
        for (Map.Entry<String, QuestProgress> entry : quests.entrySet()) {
            if (entry.getValue().getStatus() == QuestStatus.ACCEPTED) {
                accepted.add(entry.getKey());
            }
        }
        return accepted;
    }

    /**
     * Returns the set of all quest IDs that have been completed.
     */
    public Set<String> getCompletedQuestIds() {
        Set<String> completed = new LinkedHashSet<>();
        for (Map.Entry<String, QuestProgress> entry : quests.entrySet()) {
            if (entry.getValue().getStatus() == QuestStatus.COMPLETED) {
                completed.add(entry.getKey());
            }
        }
        return completed;
    }

    // ========================================================================================
    // Objective Progress
    // ========================================================================================

    /**
     * Sets the progress value for a specific objective within a quest.
     */
    public void setObjectiveProgress(String questId, int objectiveIndex, int progress) {
        getOrCreateProgress(questId).setObjectiveProgress(objectiveIndex, progress);
    }

    /**
     * Returns the progress value for a specific objective within a quest.
     */
    public int getObjectiveProgress(String questId, int objectiveIndex) {
        QuestProgress progress = quests.get(questId);
        return progress != null ? progress.getObjectiveProgress(objectiveIndex) : 0;
    }

    // ========================================================================================
    // Reward Claims
    // ========================================================================================

    /**
     * Marks a reward as claimed for a given quest.
     */
    public void claimReward(String questId, int rewardIndex) {
        getOrCreateProgress(questId).claimReward(rewardIndex);
    }

    /**
     * Returns whether a reward has been claimed for a given quest.
     */
    public boolean isRewardClaimed(String questId, int rewardIndex) {
        QuestProgress progress = quests.get(questId);
        return progress != null && progress.isRewardClaimed(rewardIndex);
    }

    // ========================================================================================
    // Saga Unlock State
    // ========================================================================================

    /**
     * Sets the unlock state for a saga.
     */
    public void setSagaUnlocked(String sagaId, boolean unlocked) {
        sagaUnlockState.put(sagaId, unlocked);
    }

    /**
     * Returns whether a saga is locked or not
     */
    public boolean isSagaLocked(String sagaId) {
        return sagaUnlockState.getOrDefault(sagaId, false);
    }

    // ========================================================================================
    // Saga Quest Keys
    // ========================================================================================

    /**
     * Builds the composite key used for saga quests: {@code "sagaId:numericId"}.
     *
     * @param sagaId  the saga identifier
     * @param questId the numeric quest ID within the saga
     * @return the composite string key
     */
    public static String sagaQuestKey(String sagaId, int questId) {
        return sagaId + ":" + questId;
    }

    public static String branchSelectionKey(String sagaId, String branchGroup) {
        return sagaId + "|" + branchGroup;
    }

    public void setBranchSelection(String sagaId, String branchGroup, String branchPath) {
        if (sagaId == null || branchGroup == null || branchGroup.isBlank()) return;
        String key = branchSelectionKey(sagaId, branchGroup);
        if (branchPath == null || branchPath.isBlank()) {
            branchSelections.remove(key);
        } else {
            branchSelections.put(key, branchPath);
        }
    }

    public String getBranchSelection(String sagaId, String branchGroup) {
        if (sagaId == null || branchGroup == null || branchGroup.isBlank()) return null;
        return branchSelections.get(branchSelectionKey(sagaId, branchGroup));
    }

    // ---- Saga convenience overloads (auto-build composite key) ----

    /** Checks if a saga quest is completed. Builds composite key from saga + numeric quest ID. */
    public boolean isQuestCompleted(String sagaId, int questId) {
        return isQuestCompleted(sagaQuestKey(sagaId, questId));
    }

    /** Completes a saga quest. Builds composite key from saga + numeric quest ID. */
    public void completeQuest(String sagaId, int questId) {
        completeQuest(sagaQuestKey(sagaId, questId));
    }

    /** Gets objective progress for a saga quest. */
    public int getObjectiveProgress(String sagaId, int questId, int objectiveIndex) {
        return getObjectiveProgress(sagaQuestKey(sagaId, questId), objectiveIndex);
    }

    /** Sets objective progress for a saga quest. */
    public void setObjectiveProgress(String sagaId, int questId, int objectiveIndex, int progress) {
        setObjectiveProgress(sagaQuestKey(sagaId, questId), objectiveIndex, progress);
    }

    /** Checks if a reward is claimed for a saga quest. Usually done by packets. Can be forced with this. */
    public boolean isRewardClaimed(String sagaId, int questId, int rewardIndex) {
        return isRewardClaimed(sagaQuestKey(sagaId, questId), rewardIndex);
    }

    /** Claims a reward for a saga quest. Usually done automatically by packets. Can be forced with this. */
    public void claimReward(String sagaId, int questId, int rewardIndex) {
        claimReward(sagaQuestKey(sagaId, questId), rewardIndex);
    }

    // ========================================================================================
    // Party State
    // ========================================================================================

	public List<UUID> getPartyMemberIds() {
        return Collections.unmodifiableList(partyMemberIds);
    }

    public boolean isInParty() {
        return activePartyId != null;
    }

    public boolean isPartyLeader(UUID playerId) {
        return playerId != null && playerId.equals(partyLeaderId);
    }

    public void setPartyState(UUID partyId, UUID leaderId, Collection<UUID> members) {
        this.activePartyId = partyId;
        this.partyLeaderId = leaderId;
        this.partyMemberIds.clear();

        if (leaderId != null) {
            this.partyMemberIds.add(leaderId);
        }

        if (members != null) {
            for (UUID memberId : members) {
                if (memberId == null || this.partyMemberIds.contains(memberId)) continue;
                this.partyMemberIds.add(memberId);
            }
        }
    }

    public void clearPartyState() {
        this.activePartyId = null;
        this.partyLeaderId = null;
        this.partyMemberIds.clear();
    }

    public PartyInviteData getPendingPartyInviteData() {
        return pendingPartyInvite;
    }

    public boolean hasPendingPartyInvite() {
        return pendingPartyInvite != null;
    }

	public void clearPendingPartyInvite() {
        this.pendingPartyInvite = null;
    }

    public void savePartyQuestBackup() {
        this.partyQuestBackup = serializeCoreQuestState();
    }

    public boolean hasPartyQuestBackup() {
        return partyQuestBackup != null && !partyQuestBackup.isEmpty();
    }

    public void restorePartyQuestBackup() {
        if (hasPartyQuestBackup()) {
            deserializeCoreQuestState(partyQuestBackup);
        }
    }

    public void clearPartyQuestBackup() {
        this.partyQuestBackup = null;
    }

    public void copyQuestStateFrom(PlayerQuestData other) {
        if (other == null) return;
        deserializeCoreQuestState(other.serializeCoreQuestState());
    }

    // ========================================================================================
    // Internal Helpers
    // ========================================================================================

    private QuestProgress getOrCreateProgress(String questId) {
        return quests.computeIfAbsent(questId, QuestProgress::new);
    }

    private CompoundTag serializeCoreQuestState() {
        CompoundTag tag = new CompoundTag();

        ListTag questList = new ListTag();
        for (QuestProgress progress : quests.values()) {
            questList.add(progress.serializeNBT());
        }
        tag.put("quests", questList);

        CompoundTag sagaUnlocks = new CompoundTag();
        for (Map.Entry<String, Boolean> entry : sagaUnlockState.entrySet()) {
            sagaUnlocks.putBoolean(entry.getKey(), entry.getValue());
        }
        tag.put("sagaUnlocks", sagaUnlocks);

        CompoundTag branchTag = new CompoundTag();
        for (Map.Entry<String, String> entry : branchSelections.entrySet()) {
            branchTag.putString(entry.getKey(), entry.getValue());
        }
        tag.put("branchSelections", branchTag);

        if (!startRequirementTimings.isEmpty()) {
            CompoundTag timingTag = new CompoundTag();
            for (Map.Entry<String, QuestStartRequirementTiming> entry : startRequirementTimings.entrySet()) {
                timingTag.put(entry.getKey(), entry.getValue().serializeNBT());
            }
            tag.put("startRequirementTimings", timingTag);
        }

        if (trackedQuestId != null && !trackedQuestId.isBlank()) {
            tag.putString("trackedQuestId", trackedQuestId);
        }
        tag.putBoolean("introPromptShown", introPromptShown);

        return tag;
    }

    private void deserializeCoreQuestState(CompoundTag tag) {
        quests.clear();
        sagaUnlockState.clear();
        branchSelections.clear();
        startRequirementTimings.clear();
        trackedQuestId = null;
        introPromptShown = false;

        ListTag questList = tag.getList("quests", Tag.TAG_COMPOUND);
        for (int i = 0; i < questList.size(); i++) {
            CompoundTag questTag = questList.getCompound(i);
            QuestProgress progress = QuestProgress.deserialize(questTag);
            quests.put(progress.getQuestId(), progress);
        }

        if (tag.contains("sagaUnlocks")) {
            CompoundTag sagaUnlocks = tag.getCompound("sagaUnlocks");
            for (String key : sagaUnlocks.getAllKeys()) {
                sagaUnlockState.put(key, sagaUnlocks.getBoolean(key));
            }
        }

        if (tag.contains("branchSelections")) {
            CompoundTag branchTag = tag.getCompound("branchSelections");
            for (String key : branchTag.getAllKeys()) {
                String path = branchTag.getString(key);
                if (!path.isBlank()) {
                    branchSelections.put(key, path);
                }
            }
        }

        if (tag.contains("startRequirementTimings", Tag.TAG_COMPOUND)) {
            CompoundTag timingTag = tag.getCompound("startRequirementTimings");
            for (String key : timingTag.getAllKeys()) {
                if (!timingTag.contains(key, Tag.TAG_COMPOUND)) continue;
                startRequirementTimings.put(key, QuestStartRequirementTiming.deserialize(timingTag.getCompound(key)));
            }
        }

        if (tag.contains("trackedQuestId", Tag.TAG_STRING)) {
            String tracked = tag.getString("trackedQuestId");
            if (!tracked.isBlank()) trackedQuestId = tracked;
        }

        if (tag.contains("introPromptShown", Tag.TAG_BYTE)) {
            introPromptShown = tag.getBoolean("introPromptShown");
        }
    }

    private static UUID parseUuid(String raw) {
        if (raw == null || raw.isBlank()) return null;
        try {
            return UUID.fromString(raw);
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    // ========================================================================================
    // NBT Serialization
    // ========================================================================================

    /**
     * Serializes all quest progress to NBT.
     */
    public CompoundTag serializeNBT() {
        CompoundTag tag = serializeCoreQuestState();

        CompoundTag partyTag = new CompoundTag();
        if (activePartyId != null) {
            partyTag.putString("partyId", activePartyId.toString());
        }
        if (partyLeaderId != null) {
            partyTag.putString("leaderId", partyLeaderId.toString());
        }
        if (!partyMemberIds.isEmpty()) {
            ListTag membersTag = new ListTag();
            for (UUID memberId : partyMemberIds) {
                membersTag.add(StringTag.valueOf(memberId.toString()));
            }
            partyTag.put("members", membersTag);
        }
        if (pendingPartyInvite != null) {
            partyTag.put("pendingInvite", pendingPartyInvite.serializeNBT());
        }
        if (partyQuestBackup != null && !partyQuestBackup.isEmpty()) {
            partyTag.put("questBackup", partyQuestBackup.copy());
        }
        if (!partyTag.isEmpty()) {
            tag.put("partyState", partyTag);
        }

        return tag;
    }

    /**
     * Deserializes quest progress from NBT.
     */
    public void deserializeNBT(CompoundTag tag) {
        deserializeCoreQuestState(tag);

        activePartyId = null;
        partyLeaderId = null;
        partyMemberIds.clear();
        pendingPartyInvite = null;
        partyQuestBackup = null;

        if (tag.contains("partyState", Tag.TAG_COMPOUND)) {
            CompoundTag partyTag = tag.getCompound("partyState");

            if (partyTag.contains("partyId", Tag.TAG_STRING)) {
                activePartyId = parseUuid(partyTag.getString("partyId"));
            }
            if (partyTag.contains("leaderId", Tag.TAG_STRING)) {
                partyLeaderId = parseUuid(partyTag.getString("leaderId"));
            }
            if (partyTag.contains("members", Tag.TAG_LIST)) {
                ListTag memberList = partyTag.getList("members", Tag.TAG_STRING);
                for (int i = 0; i < memberList.size(); i++) {
                    UUID memberId = parseUuid(memberList.getString(i));
                    if (memberId != null && !partyMemberIds.contains(memberId)) {
                        partyMemberIds.add(memberId);
                    }
                }
            }
            if (partyLeaderId != null && !partyMemberIds.contains(partyLeaderId)) {
                partyMemberIds.add(0, partyLeaderId);
            }
            if (partyTag.contains("pendingInvite", Tag.TAG_COMPOUND)) {
                pendingPartyInvite = PartyInviteData.deserialize(partyTag.getCompound("pendingInvite"));
            }
            if (partyTag.contains("questBackup", Tag.TAG_COMPOUND)) {
                partyQuestBackup = partyTag.getCompound("questBackup").copy();
            }
        }
    }

    // ========================================================================================
    // Quest Progress Inner Class
    // ========================================================================================

    /**
     * Tracks progress for a single quest: status, per-objective progress, and reward claims.
     */
    public static class QuestProgress {

        @Getter
        private final String questId;

        @Setter
        @Getter
        private QuestStatus status;

        private final Map<Integer, Integer> objectiveProgress = new HashMap<>();
        private final Map<Integer, Boolean> rewardsClaimed = new HashMap<>();

        public QuestProgress(String questId) {
            this.questId = questId;
            this.status = QuestStatus.NOT_STARTED;
        }

        public void setObjectiveProgress(int index, int progress) {
            objectiveProgress.put(index, progress);
        }

        public int getObjectiveProgress(int index) {
            return objectiveProgress.getOrDefault(index, 0);
        }

        public void claimReward(int index) {
            rewardsClaimed.put(index, true);
        }

        public boolean isRewardClaimed(int index) {
            return rewardsClaimed.getOrDefault(index, false);
        }

        public CompoundTag serializeNBT() {
            CompoundTag tag = new CompoundTag();
            tag.putString("questId", questId);
            tag.putString("status", status.name());

            CompoundTag objectivesTag = new CompoundTag();
            for (Map.Entry<Integer, Integer> entry : objectiveProgress.entrySet()) {
                objectivesTag.putInt(String.valueOf(entry.getKey()), entry.getValue());
            }
            tag.put("objectives", objectivesTag);

            CompoundTag rewardsTag = new CompoundTag();
            for (Map.Entry<Integer, Boolean> entry : rewardsClaimed.entrySet()) {
                rewardsTag.putBoolean(String.valueOf(entry.getKey()), entry.getValue());
            }
            tag.put("rewards", rewardsTag);

            return tag;
        }

        public static QuestProgress deserialize(CompoundTag tag) {
            String questId = tag.getString("questId");
            QuestProgress progress = new QuestProgress(questId);

            try {
                progress.status = QuestStatus.valueOf(tag.getString("status"));
            } catch (IllegalArgumentException e) {
                progress.status = QuestStatus.NOT_STARTED;
            }

            CompoundTag objectivesTag = tag.getCompound("objectives");
            for (String key : objectivesTag.getAllKeys()) {
                progress.objectiveProgress.put(Integer.parseInt(key), objectivesTag.getInt(key));
            }

            CompoundTag rewardsTag = tag.getCompound("rewards");
            for (String key : rewardsTag.getAllKeys()) {
                progress.rewardsClaimed.put(Integer.parseInt(key), rewardsTag.getBoolean(key));
            }

            return progress;
        }
    }

    @Getter
    public static class QuestStartRequirementTiming {
        private final long gameTimeStarted;
        private final long realTimeStartedMs;

        public QuestStartRequirementTiming(long gameTimeStarted, long realTimeStartedMs) {
            this.gameTimeStarted = gameTimeStarted;
            this.realTimeStartedMs = realTimeStartedMs;
        }

        public CompoundTag serializeNBT() {
            CompoundTag tag = new CompoundTag();
            tag.putLong("gameTimeStarted", gameTimeStarted);
            tag.putLong("realTimeStartedMs", realTimeStartedMs);
            return tag;
        }

        public static QuestStartRequirementTiming deserialize(CompoundTag tag) {
            return new QuestStartRequirementTiming(
                    tag.getLong("gameTimeStarted"),
                    tag.getLong("realTimeStartedMs")
            );
        }
    }

    // ========================================================================================
    // Party Invite Inner Class
    // ========================================================================================

    @Getter
    public static class PartyInviteData {
        private final UUID inviterUUID;
        private final UUID partyId;
        private final UUID partyLeaderId;
        private final String inviterName;
        private final long expiresAtMs;

        public PartyInviteData(UUID inviterUUID, UUID partyId, UUID partyLeaderId, String inviterName, long expiresAtMs) {
            this.inviterUUID = inviterUUID;
            this.partyId = partyId;
            this.partyLeaderId = partyLeaderId;
            this.inviterName = inviterName == null ? "" : inviterName;
            this.expiresAtMs = expiresAtMs;
        }

        public boolean isExpired() {
            return System.currentTimeMillis() > expiresAtMs;
        }

        public CompoundTag serializeNBT() {
            CompoundTag tag = new CompoundTag();
            if (inviterUUID != null) tag.putString("inviterUUID", inviterUUID.toString());
            if (partyId != null) tag.putString("partyId", partyId.toString());
            if (partyLeaderId != null) tag.putString("partyLeaderId", partyLeaderId.toString());
            if (!inviterName.isBlank()) tag.putString("inviterName", inviterName);
            tag.putLong("expiresAtMs", expiresAtMs);
            return tag;
        }

        public static PartyInviteData deserialize(CompoundTag tag) {
            return new PartyInviteData(
                    parseUuid(tag.getString("inviterUUID")),
                    parseUuid(tag.getString("partyId")),
                    parseUuid(tag.getString("partyLeaderId")),
                    tag.getString("inviterName"),
                    tag.getLong("expiresAtMs")
            );
        }
    }
}
